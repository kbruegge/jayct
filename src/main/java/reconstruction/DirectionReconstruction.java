package reconstruction;

import com.google.common.collect.Lists;
import coordinates.CameraCoordinate;
import coordinates.HorizontalCoordinate;
import hexmap.TelescopeArray;
import hexmap.TelescopeDefinition;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.*;

/**
 * This processor takes the image parameters of an array event, so all image parameters for each
 * telescope in the event and uses them to estimate the position of the shower impact on the ground
 * and the direction the shower originated from.
 *
 * The original implementation was created by Tino Michael. And is called FitGammaHillas
 * within the ctapipe project. https://github.com/cta-observatory/ctapipe
 *
 * Created by Kai on 20.02.17.
 */
public class DirectionReconstruction {


    private static final TelescopeArray MAPPING = TelescopeArray.cta();


    /**
     * Go from spherical coordinates to cartesian coordinates.
     * This assumes the typical 'mathematical conventions', or ISO, for naming these angles.
     * (radius r, inclination θ, azimuth φ)
     *
     * The conversion works like this:
     *
     *    [ r * sin(theta)*cos(phi),
     *      r * sin(theta)*sin(phi),
     *      cos(theta/r)         ]
     *
     *
     * Apache Commons Math also provides a SphericalCoordinates object. It uses a different convention however
     * (phi and theta are switched).
     *
     *
     * @param phi_rad pointing phi angle of a telescope
     * @param theta_rad pointing theta angle of a telescope
     * @return a direction vector of length 1
     */
    private static Vector3D sphericalToCartesian(double radius, double phi_rad, double theta_rad){
        double x = radius * sin(theta_rad) * cos(phi_rad);
        double y = radius * sin(theta_rad) * sin(phi_rad);
        double z = cos(theta_rad/radius);
        return new Vector3D(x, y, z);
    }

    private static double[] cartesianToAltAz(Vector3D r){
        double radius = r.getNorm();

        double theta = Math.acos(r.getZ()/radius);
        double phi= Math.atan2(r.getY(), r.getX());

        double alt = Math.PI/2 - theta;
        double az = phi;
        if (az<0){
            az += Math.PI *2;
        }

        return new double[]{alt, az};
    }



    public static ReconstrucedEvent fromMoments(Iterable<Moments> momentses, double altitude, double azimuth) {

        ArrayList<Moments> parameters = Lists.newArrayList(momentses);

        long eventID = parameters.get(0).eventID;

        List<Plane> planes = parameters.stream()
                .map(p -> new Plane(altitude, azimuth, p))
                .collect(Collectors.toList());

        Vector3D direction = estimateDirection(planes);
        double[] altAz = cartesianToAltAz(direction);
        double[] corePosition = estimateCorePosition(planes);
        return new ReconstrucedEvent(eventID, altAz, corePosition);
    }

    /**
     * Estimate the position x,y of the showers impact from a collection of Plane objects
     * @param planes planes for each telescope in the event
     * @return an array [x, y] giving a point on the surface
     */
    private static double[] estimateCorePosition(List<Plane> planes){
        int n = planes.size();

        double[][] mat = new double[n][2];
        double[] d =  new double[n];

        for (int i = 0; i < n; i++) {
            Plane plane = planes.get(i);
            Vector3D norm = plane.getNormalAsVector();
            double weight = plane.weight;

            Vector2D projection = new Vector2D(norm.getX(), norm.getY());

            mat[i][0] = projection.getX() * weight;
            mat[i][1] = projection.getY() * weight;


            Vector2D telPos = new Vector2D(plane.telescopePosition[0], plane.telescopePosition[1]);

            d[i] = (projection.scalarMultiply(weight)).dotProduct(telPos);
        }

        //Do a linear least square regresssion
        RealMatrix A = MatrixUtils.createRealMatrix(mat);
        try {
            return MatrixUtils.inverse(A.transpose().multiply(A)).multiply(A.transpose()).operate(d);
        } catch (SingularMatrixException e){
            return new double[]{Double.NaN, Double.NaN};
        }

    }

    /**
     * Estimate the direction of the shower from a collection of Plane objects
     * Returns a direction vector [x, y, z] pointing into the direction the shower
     * originated.
     * It does so by calculating a weighted sum of the estimated directions
     * from all pairs of planes in the given as the method argument.
     *
     * @param planes the plane objects for each Telescope
     * @return an array [x, y, z]
     */
    private static Vector3D estimateDirection(List<Plane> planes){

        // get all combinations of size 2 in a rather inelegant way.
        List<List<Plane>> tuples = new ArrayList<>();
        for(Plane p1 : planes){
            for(Plane p2 : planes){
                if(p1 != p2){
                    tuples.add(Lists.newArrayList(p1, p2));
                }
            }
        }

        Optional<Vector3D> direction = tuples.stream()
                .map(l -> {

                    Plane plane1 = l.get(0);
                    Plane plane2 = l.get(1);
                    double[] v1 = plane1.normalVector;
                    double[] v2 = plane2.normalVector;

                    Vector3D product = Vector3D.crossProduct(new Vector3D(v1), new Vector3D(v2));

                    //Here is the original docstring from the python implementation:
                    //
                    // # two great circles cross each other twice (one would be
                    // # the origin, the other one the direction of the gamma) it
                    // # doesn't matter which we pick but it should at least be
                    // # consistent: make sure to always take the "upper" solution.
                    if (product.getZ() < 0) {
                        product = product.scalarMultiply(-1);
                    }

                    return product.scalarMultiply(plane1.weight * plane2.weight);
                })
                .reduce(Vector3D::add)
                .filter(l -> l.getNorm() > 0)
                .map(Vector3D::normalize);

        return direction.orElse(new Vector3D(Double.NaN, Double.NaN, Double.NaN));
    }


    /**
     * This class describes a plane in [x, y, z] space for a single telescope shower.
     * The plane is aligned with the reconstructed angle of the shower in the camera.
     * One plane for each camera partaking in an event is created. Using these planes both impact position
     * and direction can be reconstructed.
     */
    private static class Plane {
        //the telescope id this reconstructed plane belongs to
        final int telescopeId;
        //the weight given to the plane
        final double weight;
        //two vectors describing the plane
        final Vector3D planeVector1;
        final Vector3D planeVector2;

        //the normalvector of that plane
        final double[] normalVector;

        //the position of the telescope on the ground
        final double[] telescopePosition;

        Plane(double alt, double az, Moments p) {
            HorizontalCoordinate pointing = HorizontalCoordinate.fromDeg(90 - alt, az);
            this.telescopeId = p.telescopeID;

            TelescopeDefinition tel = MAPPING.telescopeFromId(this.telescopeId);


            //get two points on the shower axis
            double pX = p.meanX + 0.1 * cos(p.delta);
            double pY = p.meanY + 0.1 * sin(p.delta);

            this.weight = p.size * (p.length / p.width);

            CameraCoordinate cc = new CameraCoordinate(p.meanX, p.meanY);
            HorizontalCoordinate p1 = cc.toHorizontal(pointing, tel.opticalFocalLength);
            this.planeVector1 = sphericalToCartesian(1, p1.getAzimuthRad(), p1.getZenithRad());

            cc = new CameraCoordinate(pX, pY);
            HorizontalCoordinate p2 = cc.toHorizontal(pointing, tel.opticalFocalLength);
            this.planeVector2 = sphericalToCartesian(1, p2.getAzimuthRad(), p2.getZenithRad());


            Vector3D crossProduct = Vector3D.crossProduct(Vector3D.crossProduct(planeVector1, planeVector2), planeVector1);
            Vector3D norm = Vector3D.crossProduct(planeVector1, crossProduct);

            if (Double.isNaN(weight)){
                this.normalVector = new double[]{Double.NaN,Double.NaN,Double.NaN};
            } else {
                this.normalVector = norm.normalize().toArray();
            }

            telescopePosition = new double[]{tel.telescopePositionX, tel.telescopePositionY, tel.telescopePositionZ};
//            System.out.println(telescopePosition[0]+ ", " +  telescopePosition[1] + ", " + telescopePosition[2] );
        }

        /**
         * A small convenience function to return the [x, y, z] normal vector as an actual Vector3d object
         *
         * @return a the normal vector of the plane
         */
        Vector3D getNormalAsVector(){
            return new Vector3D(normalVector);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Plane plane = (Plane) o;

            if (telescopeId != plane.telescopeId) return false;
            if (Double.compare(plane.weight, weight) != 0) return false;
            if (!planeVector1.equals(plane.planeVector1)) return false;
            return planeVector2.equals(plane.planeVector2);

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = telescopeId;
            temp = Double.doubleToLongBits(weight);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + planeVector1.hashCode();
            result = 31 * result + planeVector2.hashCode();
            return result;
        }
    }
}
