import com.google.common.base.MoreObjects;

import static java.lang.Math.*;

/**
 * This will be a basic translation of the code found in ctapipe. Some reference is in here:
 * <a href="http://adsabs.harvard.edu/abs/1993ApJ...404..206R"> link </a>
 *
 * (Whipple and Reynolds et al. 1993)
 *
 */
public class Moments {


    private Moments(long eventId, int telescopeID, double width, double length, double delta, double skewness, double kurtosis, double phi, double miss, double r, double meanX, double meanY, double size) {
        this.eventId = eventId;
        this.telescopeID = telescopeID;
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

    public static Moments fromShower(Shower shower){
        return calculate(shower);
    }

    public final long eventId;
    public final int telescopeID;

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

    private static Moments calculate(Shower shower) {

        double size = shower.signalPixels.stream().mapToDouble(e -> e.weight).sum();
        double sumX = 0;
        double sumY = 0;

        // find weighted center of the shower pixels.
        for (Shower.SignalPixel pixel : shower.signalPixels) {
            sumX += pixel.xPositionInMM * pixel.weight;
            sumY += pixel.yPositionInMM * pixel.weight;
            size += pixel.weight;
        }

        final double meanX = sumX / size;
        final double meanY = sumY / size;

        //calculate the covariance matrix
        double sxx = 0, syy = 0, sxy = 0;
        for (Shower.SignalPixel p : shower.signalPixels) {
            sxx += p.weight * pow((p.xPositionInMM - meanX), 2);
            syy += p.weight * pow((p.yPositionInMM - meanY), 2);
            sxy += p.weight * (p.xPositionInMM - meanX) * (p.yPositionInMM - meanY);
        }

        sxx /= size;
        syy /= size;
        sxy /= size;

        //now analytically calculate the eigenvalues and vectors.
        double d0 = syy - sxx;
        double d1 = 2 * sxy;
        double d2 = d0 + sqrt(d0 * d0 + d1 * d1);
        double a = d2 / d1;

        //apperently things can get less than zero. just set to  zero then.
        double width = sqrt(max((syy + a * a * sxx - 2 * a * sxy) / (1 + a * a), 0));
        double length = sqrt(max((sxx + a * a * syy - 2 * a * sxy) / (1 + a * a), 0));

        double delta = atan(a);
        double cos_delta = 1 / sqrt(1 + a * a);
        double sin_delta = a * cos_delta;

        //I dont know what this is
        double b = meanY - a * meanX;
        double miss = abs(b / (sqrt(1 + a * a)));
        double r = sqrt(meanX * meanX + meanY * meanY);
        double phi = atan2(meanY, meanX); //wtf?

        //calculate higher order moments
        double skewness_a = 0, skewness_b = 0, kurtosis_a = 0, kurtosis_b = 0;
        for (Shower.SignalPixel p : shower.signalPixels) {
            double sk = cos_delta * (p.xPositionInMM - meanX) + sin_delta * (p.yPositionInMM - meanY);
            skewness_a += p.weight * pow(sk, 3);
            skewness_b += p.weight * pow(sk, 2);

            kurtosis_a += p.weight * pow(sk, 4);
            kurtosis_b += p.weight * pow(sk, 2);
        }

        double skewness = (skewness_a / size) / pow(skewness_b / size, 3.0 / 2.0);
        double kurtosis = (kurtosis_a / size) / pow(kurtosis_b / size, 2);


        return new Moments(shower.eventId, shower.cameraId, width, length, delta, skewness, kurtosis, phi, miss, r, meanX, meanY, size);
    }
}
