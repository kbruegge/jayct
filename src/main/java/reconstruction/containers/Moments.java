package reconstruction.containers;

import com.google.common.base.MoreObjects;


/**
 * Stores all the important (statistical) moments that can be calculated on the (cleaned) images.
 *
 * Created by mackaiver on 22/09/17.
 */
public class Moments {

    public Moments(long eventID, int telescopeID, int cameraID, int numberOfPixel, double width, double length, double delta, double skewness, double kurtosis, double phi, double miss, double r, double meanX, double meanY, double size) {
        this.eventID = eventID;
        this.telescopeID = telescopeID;
        this.cameraID = cameraID;
        this.numberOfPixel = numberOfPixel;
        this.width = width;
        this.length = length;
        this.delta = delta;
        this.skewness = skewness;
        this.kurtosis = kurtosis;
        this.phi = phi;
        this.miss = miss;
        this.r = r;
        this.meanX = meanX;
        this.meanY = meanY;
        this.size = size;
    }


    public final long eventID;
    public final int telescopeID;
    public final int cameraID;
    public final int numberOfPixel;

    public final double width;
    public final double length;
    public final double delta;
    public final double skewness;
    public final double kurtosis;
    public final double phi;
    public final double miss;
    public final double r;
    public final double meanX;
    public final double meanY;
    public final double size;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventID", eventID)
                .add("telescopeID", telescopeID)
                .add("width", width)
                .add("length", length)
                .add("delta", delta)
                .add("skewness", skewness)
                .add("kurtosis", kurtosis)
                .add("phi", phi)
                .add("miss", miss)
                .add("r", r)
                .add("meanX", meanX)
                .add("meanY", meanY)
                .add("size", size)
                .toString();
    }

}
