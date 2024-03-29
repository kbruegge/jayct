package reconstruction.containers;

import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import hexmap.TelescopeArray;

/**
 * This class holds information about the shower. The set of selected signal pixels from the
 * telescopes image. The pixel class holds all required information to perform simple geometric
 * algorithms on the signal. Like the Hillas parameters for example. Each signal pixel also holds
 * the Ids of its neighbouring pixels. This is useful for dilating the shower.
 *
 * Created by kaibrugge on 13.02.17.
 */
public class ShowerImage implements Serializable {

    private static final TelescopeArray mapping = TelescopeArray.cta();

    public final HashSet<SignalPixel> signalPixels = new HashSet<>();

    public final int cameraId;
    public final long eventId;

    /**
     * Each camera (in one event) can have exactly one shower object.
     *
     * @param cameraId the id of the camera which recorded the image.
     * @param eventId  the unique event id this shower belongs to.
     */
    public ShowerImage(int cameraId, long eventId) {
        this.cameraId = cameraId;
        this.eventId = eventId;
    }

    /**
     * Transform this object into a simple HashMap that can be passed to a python script.
     */
    public HashMap<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("cameraId", cameraId);
        map.put("eventId", eventId);
        double[] pixelWeights = new double[TelescopeArray.cta().cameraFromId(cameraId).numberOfPixel];
        for (SignalPixel signalPixel : signalPixels) {
            pixelWeights[signalPixel.pixelId] = signalPixel.weight;
        }
        map.put("image", pixelWeights);
        return map;
    }

    /**
     * A Shower holds a number of SignalPixels objects defined in this class. Each SignalPixel knows
     * its neighbouring pixel for fast dilation operations.
     */
    public static class SignalPixel implements Serializable {
        final int cameraId;
        final int pixelId;
        final public double weight;
        final public double xPositionInMM;
        final public double yPositionInMM;
        final int[] neighbours;


        private SignalPixel(int cameraId, int pixelId, double xPositionInM,
                            double yPositionInM, double weight, int[] neighbours) {
            this.cameraId = cameraId;
            this.pixelId = pixelId;
            this.xPositionInMM = xPositionInM;
            this.yPositionInMM = yPositionInM;
            this.weight = weight;
            this.neighbours = neighbours;
        }

        /**
         * This methid creates a SignalPixel from the ids and the weight of that pixel.
         *
         * @param cameraId the id of the camera/telescope in which this pixel is located
         * @param pixelId  the id of the pixel
         * @param weight   the weight of the pixel. (like estimated number of photons or similar)
         * @return an instance of a SignalPixel with the given valuess.
         */
        static SignalPixel create(int cameraId, int pixelId, double weight) {
            double x = mapping.cameraFromId(cameraId).pixelXPositions[pixelId];
            double y = mapping.cameraFromId(cameraId).pixelYPositions[pixelId];

            int[] neighbours = mapping.cameraFromId(cameraId).neighbours[pixelId];
            return new SignalPixel(cameraId, pixelId, x, y, weight, neighbours);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SignalPixel pixel = (SignalPixel) o;

            return cameraId == pixel.cameraId && pixelId == pixel.pixelId;
        }

        @Override
        public int hashCode() {
            int result = cameraId;
            result = 31 * result + pixelId;
            return result;
        }
    }

    public void addPixel(int pixelId, double weight) {
        signalPixels.add(SignalPixel.create(cameraId, pixelId, weight));
    }

    /**
     * This method adds pixels to this Shower instance by selecting pixels adjacent to the already
     * selected pixels which have a value which is greater or equal to the given threshold.
     *
     * @param image     the camera image in estimated number of photons.
     * @param threshold the threshold to select pixel which are added to the shower
     */
    public void dilate(double[] image, double threshold) {
        HashSet<SignalPixel> ids = Sets.newHashSet();

        for (SignalPixel pix : signalPixels) {
            for (int n : pix.neighbours) {
                if (image[n] > threshold) {
                    ids.add(SignalPixel.create(cameraId, n, image[n]));
                }
            }
        }

        signalPixels.addAll(ids);
    }

}
