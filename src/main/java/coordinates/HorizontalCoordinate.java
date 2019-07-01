package coordinates;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;


/**
 * Represents a coordinate in the horizontal coordinate.
 * <p>
 * Provides a method to transform into the equatorial and camera frame.
 * <p>
 * Created by maxnoe on 22.05.17.
 */
public class HorizontalCoordinate  {

    public final double zenithRad;
    public final double azimuthRad;

    private HorizontalCoordinate(double zenithRad, double azimuthRad) {
        this.zenithRad = zenithRad;
        this.azimuthRad = azimuthRad;
    }

    public static HorizontalCoordinate fromRad(double zenithRad, double azimuthRad) {
        return new HorizontalCoordinate(zenithRad, azimuthRad);
    }

    public static HorizontalCoordinate fromDeg(double zenithDeg, double azimuthDeg) {
        return new HorizontalCoordinate(zenithDeg* Math.PI/180, azimuthDeg* Math.PI/180);
    }


    /**
     * Transform this horizontal coordinate to the camera frame for the given pointing position and focal length.
     *
     * @param pointingPosition Pointing of the telescope
     * @param focalLength      focalLength of the telescope
     * @return coordinate transformed into the camera frame
     */
    public CameraCoordinate toCamera(HorizontalCoordinate pointingPosition, double focalLength) {

        double paz = pointingPosition.getAzimuthRad();
        double pzd = pointingPosition.getZenithRad();
        double saz = this.getAzimuthRad();
        double szd = this.getZenithRad();

        Vector3D vec = new Vector3D(Math.sin(szd) * Math.cos(saz), Math.sin(szd) * Math.sin(saz), Math.cos(szd));

        Rotation rotZAz = new Rotation(new Vector3D(0.0, 0.0, 1.0), -paz, RotationConvention.VECTOR_OPERATOR);
        Rotation rotYZd = new Rotation(new Vector3D(0.0, 1.0, 0.0), -pzd, RotationConvention.VECTOR_OPERATOR);

        Vector3D rotVec = rotYZd.applyTo(rotZAz.applyTo(vec));

        double x = rotVec.getX();
        double y = rotVec.getY();
        double z = rotVec.getZ();

        // rotate camera by 90 degrees to have the following coordinate definition:
        // When looking from the telescope dish onto the camera, x points right, y points up
        CameraCoordinate cameraCoordinate = new CameraCoordinate(-y * (focalLength) / z, x * (focalLength) / z);

        return cameraCoordinate;
    }

    public String toString() {
        return String.format("HorizontalCoordinate(zd=%.4f°, az=%.4f°)", this.getZenithDeg(), this.getAzimuthDeg());
    }

    public double getAltitudeRad() {
        return Math.PI / 2 - zenithRad;
    }

    public double getAltitudeDeg() {
        return -(90 - this.getZenithDeg());
    }

    public double getZenithRad() {
        return zenithRad;
    }

    public double getAzimuthRad() {
        return azimuthRad;
    }

    public double getZenithDeg() {
        return Math.toDegrees(zenithRad);
    }

    public double getAzimuthDeg() {
        return Math.toDegrees(azimuthRad);
    }
}
