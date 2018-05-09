package reconstruction.containers;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import static java.lang.Math.*;

/**
 * Container class containing all information of a (geometrically) reconstruced event.
 *
 * Created by mackaiver on 25/09/17.
 */
public final class ReconstrucedEvent {

    public final long eventID;
    public final Vector3D direction;
    public final Vector2D impactPosition;
    public double alt, az;

    public ReconstrucedEvent(long eventID, double[] direction, double[] corePosition) {
        this.eventID = eventID;
        this.direction = new Vector3D(direction);
        this.impactPosition = new Vector2D(corePosition);

        double[] alt_az = fromCartesian(this.direction);
        this.alt = alt_az[0];
        this.az= alt_az[1];
    }

    private double[] fromCartesian(Vector3D cartesian){
        double x = cartesian.getX();
        double y = cartesian.getY();
        double z = cartesian.getZ();
        double s = sqrt(pow(x, 2) + pow(y, 2));

        double lon = atan2(y, x);
        double lat = atan2(z, s);

        alt = PI/2 - lat;
        az = lon;
        return new double[]{alt, az};
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventID", eventID)
                .add("direction", direction)
                .add("impactPosition", impactPosition)
                .toString();
    }


    public String toCSVString() {
        return Joiner.on(",").join(eventID, direction.getX(), direction.getY(), direction.getZ(), impactPosition.getX(), impactPosition.getY());
    }
}
