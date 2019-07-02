package reconstruction.containers;

import com.google.common.base.MoreObjects;

import java.util.HashMap;


/**
 * Stores all the important (statistical) moments that can be calculated on the (cleaned) images.
 *
 * Created by mackaiver on 22/09/17.
 */
public class Moments {

    public Moments(long eventID, int telescopeID, int numberOfPixel, double width,
                   double length, double delta, double skewness, double kurtosis,
                   double r, double meanX, double meanY, double size) {
        this.eventID = eventID;
        this.telescopeID = telescopeID;
        this.numberOfPixel = numberOfPixel;
        this.width = width;
        this.length = length;
        this.delta = delta;
        this.skewness = skewness;
        this.kurtosis = kurtosis;
        this.r = r;
        this.meanX = meanX;
        this.meanY = meanY;
        this.size = size;
    }

    public HashMap<String, Float> toFeatureMap(){
        HashMap<String, Float> map = new HashMap<>();
        map.put("number_of_pixel", (float) numberOfPixel);
        map.put("width", (float) width);
        map.put("length", (float) length);
        map.put("delta", (float) delta);
        map.put("skewness", (float) skewness);
        map.put("kurtosis", (float) kurtosis);
        map.put("r", (float) r);
        map.put("x", (float) meanX);
        map.put("y", (float) meanY);
        map.put("size", (float) size);
        return map;
    }

    public final long eventID;
    public final int telescopeID;

    public final int numberOfPixel;

    public final double width;
    public final double length;
    public final double delta;
    public final double skewness;
    public final double kurtosis;
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
                .add("r", r)
                .add("meanX", meanX)
                .add("meanY", meanY)
                .add("size", size)
                .toString();
    }

}
