import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import hexmap.TelescopeArray;
import hexmap.TelescopeDefinition;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;



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
public class Stereo{

    private static final TelescopeArray MAPPING = TelescopeArray.cta();

    /**
     * Convert in-camera coordinates to direction vectors in 3D-space.
     *
     * @param x in-camera coordinates in meter
     * @param y in-camera coordinates in meter
     * @param phi pointing in radians
     * @param theta pointing in radians
     * @param focalLength focal length of the telescope in meter
     * @param cameraRotation rotation of the camera within the telescope housing in radians
     * @return a direction vector, (x,y,z), corresponding to the direction of the given point in the camera
     */
    private static double[] cameraCoordinateToDirectionVector
    (
            double x,
            double y,
            double phi,
            double theta,
            double focalLength,
            double cameraRotation
    ){

        //angle between x-axis of camera and the line connecting 0,0 with x,y
        double alpha = atan2(y, x);

        //distance from center of camera
        double rho = sqrt(pow(x, 2) + pow(y, 2));
        double beta = rho / focalLength;

        double[] telescopeDirection = cartesianFromPolar(phi, theta);

        double[] p = cartesianFromPolar(phi, theta + beta);

        return rotateAroundAxis(p, telescopeDirection, alpha - cameraRotation);

    }

    /**
     * A helper function which rotates a vector around a given axis.
     * @param vector the vector to rotate
     * @param axis the (fixed) axis around which to rotate
     * @param angleInRadians the angle by which to rotate
     * @return an array [x,y,z] encoding the rotated vector.
     */
    private static double[] rotateAroundAxis(double[] vector, double[] axis, double angleInRadians){
        Vector3D v = new Vector3D(vector);
        Vector3D ax = new Vector3D(axis);

        Rotation rotation = new Rotation(ax, angleInRadians, RotationConvention.FRAME_TRANSFORM);
        Vector3D rotatedVector = rotation.applyTo(v);
        return rotatedVector.toArray();
    }

    /**
     * Go from spherical coordinates to cartesian coordinates. Useful for
     * creating a direction vector from the pointing of the telescope.
     * This assumes the typical 'mathematical conventions', or ISO, for naming these angles.
     * (radius r, inclination θ, azimuth φ)
     *
     * The conversion works like this:
     *
     *    [ sin(theta)*cos(phi),
     *      sin(theta)*sin(phi),
     *      cos(theta)         ]
     *
     *
     * @param phi pointing phi angle of a telescope
     * @param theta pointing theta angle of a telescope
     * @return a direction vector of length 1
     */
    private static double[] cartesianFromPolar(double phi, double theta){
        double x = sin(theta) * cos(phi);
        double y = sin(theta) * sin(phi);
        double z = cos(theta);
        return new double[] {x, y, z};
    }

    final double[] estimatedDirection;
    final double[] estimatedImpactPosition;

    private Stereo(double[] estimatedDirection,
                  double[] estimatedImpactPosition) {

        this.estimatedDirection = estimatedDirection;
        this.estimatedImpactPosition = estimatedImpactPosition;
    }

    public double  getDirectionX(){
        return estimatedDirection[0];
    }
    public double  getDirectionY(){
        return estimatedDirection[1];
    }
    public double  getDirectionZ(){
        return estimatedDirection[2];
    }



    public static Stereo fromMoments(List<Moments> parameters, double altitude, double azimuth) {

        //get pointing information from data stream. while these variables have different names
        // I hope.
//        double phi = (double) data.get("mc:az");
//        double theta = (double) data.get("mc:alt");

        List<Plane> planes = parameters.stream()
                .map(p -> new Plane(azimuth, altitude, p))
                .collect(Collectors.toList());

        double[] direction = estimateDirection(planes);

        double[] corePosition = estimateCorePosition(planes);

        return new Stereo(direction, corePosition);
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
            return new double[]{Double.NaN, Double.NaN, Double.NaN};
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
    private static double[] estimateDirection(List<Plane> planes){

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

                    //dont know what happens now. here is the docstring from the python
                    //TODO: Find out what exactly this calculation does. and why it can have two solutions
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
                .map(Vector3D::normalize);

        return direction.orElse(new Vector3D(0, 0, 0)).toArray();
    }


    /**
     * This class describes a plane in [x, y, z] space for a single telescope shower.
     * The plane is aligned with the reconstructed angle of the shower in the camera.
     * One plane for each camera partaking in an event is created. Using these planes both impact position
     * and direction can be reconstructed.
     *
     */
    private static class Plane {
        //the telescope id this reconstructed plane belongs to
        final int telescopeId;
        //the weight given to the plane
        final double weight;
        //two vectors describing the plane
        final double[] planeVector1;
        final double[] planeVector2;

        //the normalvector of that plane
        final double[] normalVector;

        //the position of the telescope on the ground
        final double[] telescopePosition;

        Plane(double phi, double theta, Moments p) {
            this.telescopeId = p.telescopeID;

            TelescopeDefinition tel = MAPPING.telescopeFromId(this.telescopeId);

            //get two points on the shower axis
            double pX = p.meanX + p.length * cos(p.delta);
            double pY = p.meanY + p.length * sin(p.delta);

            double[] pDirection = cameraCoordinateToDirectionVector(pX, pY, phi, theta, tel.opticalFocalLength, 0);
            double[] cogDirection = cameraCoordinateToDirectionVector(p.meanX, p.meanY, phi, theta, tel.opticalFocalLength, 0);

            this.weight = p.size * (p.length / p.width);
            this.planeVector1 = cogDirection;
            this.planeVector2 = pDirection;

            // c  = (v1 X v2) X v1
            Vector3D crossProduct = Vector3D.crossProduct(Vector3D.crossProduct(new Vector3D(planeVector1), new Vector3D(planeVector2)), new Vector3D(planeVector1));
            Vector3D norm = Vector3D.crossProduct(new Vector3D(planeVector1), crossProduct);

            if (Double.isNaN(weight)){
                this.normalVector = new double[]{Double.NaN,Double.NaN,Double.NaN};
            } else {
                this.normalVector = norm.normalize().toArray();
            }

            telescopePosition = new double[]{tel.telescopePositionX, tel.telescopePositionY, tel.telescopePositionZ};
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()){
                return false;
            }
            Plane plane = (Plane) o;
            return telescopeId == plane.telescopeId;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(telescopeId);
        }
    }
}
