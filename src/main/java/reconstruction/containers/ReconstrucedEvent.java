package reconstruction.containers;

import com.google.common.base.MoreObjects;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 * Container class containing all information of a (geometrically) reconstruced event.
 *
 * Created by mackaiver on 25/09/17.
 */
public final class ReconstrucedEvent {

    public final long eventID;
    public final double[] altAz;
    public final Vector2D impactPosition;

    public ReconstrucedEvent(long eventID, double[] direction, double[] corePosition) {
        this.eventID = eventID;
        this.altAz= direction;
        this.impactPosition = new Vector2D(corePosition);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventID", eventID)
                .add("direction", altAz)
                .add("impactPosition", impactPosition)
                .toString();
    }
}
