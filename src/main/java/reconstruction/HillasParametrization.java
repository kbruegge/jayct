package reconstruction;

import coordinates.CameraCoordinate;
import org.apache.commons.math3.linear.EigenDecomposition;
import reconstruction.containers.Moments;
import reconstruction.containers.ShowerImage;
import statistics.Weighted1dStatistics;
import statistics.Weighted2dStatistics;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static java.util.stream.Collectors.toList;

/**
 * This will be a basic translation of the code found in ctapipe. Some reference is in here:
 * <a href="http://adsabs.harvard.edu/abs/1993ApJ...404..206R"> link </a>
 *
 * (Whipple and Reynolds et al. 1993)
 *
 */
public class HillasParametrization {


    public static List<Moments> fromShowerImages(List<ShowerImage> showerImages){
        return showerImages.stream().map(HillasParametrization::fromShowerImage).collect(toList());
    }

    public static List<Moments> fromShowerImages(ShowerImage... showerImages){
        return Arrays.stream(showerImages).map(HillasParametrization::fromShowerImage).collect(toList());
    }


    /**
     * Transforms camera coordinates (x, y) into longitudinal and transversal
     * ellipse coordinates (l, t). The ellipse coordinate system is defined by
     * the center of gravity (x,y) and the angle between the major axis and the
     * camera x-axis (delta in radians).
     *
     * @param x
     * @param y
     * @param cogX
     * @param cogY
     * @param delta
     * @return an array having two elements {l, t}
     */
    public static double[] transformToEllipseCoordinates(double x, double y, double cogX, double cogY, double delta) {
        double translatedX = x - cogX;
        double translatedY = y - cogY;

        double sinDelta = Math.sin(delta);
        double cosDelta = Math.cos(delta);

        double l = cosDelta * translatedX + sinDelta * translatedY;
        double t = -sinDelta * translatedX + cosDelta * translatedY;

        return new double[]{l, t};
    }

    public static double calculateDelta(EigenDecomposition eig) {
        // calculate the angle between the eigenvector and the camera axis.
        // So basicly the angle between the major-axis of the ellipse and the
        // camrera axis.
        // this will be written in radians.
        double longitudinalComponent = eig.getEigenvector(0).getEntry(0);
        double transverseComponent = eig.getEigenvector(0).getEntry(1);
        return Math.atan(transverseComponent / longitudinalComponent);
    }

    public static Moments fromShowerImage(ShowerImage showerImage) {

        double[] showerWeights = showerImage.signalPixels.stream().mapToDouble((p) -> p.weight).toArray();

        double[] pixelX = showerImage.signalPixels.stream().mapToDouble(p -> p.xPositionInM).toArray();
        double[] pixelY = showerImage.signalPixels.stream().mapToDouble(p -> p.yPositionInM).toArray();

        Weighted2dStatistics stats2d = Weighted2dStatistics.ofArrays(pixelX, pixelY, showerWeights);

        double size = stats2d.weightsSum;

        double x = stats2d.mean[0];
        double y =  stats2d.mean[1];

        if(stats2d.N < 2){
            return new Moments(
                    showerImage.eventId,
                    showerImage.telescopeId,
                    showerImage.signalPixels.size(),
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN
                    );
        }
        EigenDecomposition eig = new EigenDecomposition(stats2d.covarianceMatrix);
        double length = Math.sqrt(eig.getRealEigenvalue(0));
        double width = Math.sqrt(eig.getRealEigenvalue(1));
        double delta = calculateDelta(eig);

        // Calculation of the showers statistical moments (Variance, Skewness, Kurtosis)
        // Rotate the shower by the angle delta in order to have the ellipse
        // main axis in parallel to the Camera-Coordinates X-Axis
        double[] longitudinalCoordinates = new double[showerImage.signalPixels.size()];

        for (int i = 0; i < showerImage.signalPixels.size(); i++) {
            // translate to center
            double[] c = transformToEllipseCoordinates(pixelX[i], pixelY[i],  x, y, delta);
            // fill array of new shower coordinates
            longitudinalCoordinates[i] = c[0];
        }

        Weighted1dStatistics statsLong = Weighted1dStatistics.ofArrays(longitudinalCoordinates, showerWeights);

        double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        return new Moments(
                showerImage.eventId,
                showerImage.telescopeId,
                showerImage.signalPixels.size(),
                width,
                length,
                delta,
                statsLong.skewness,
                statsLong.kurtosis,
                r,
                x,
                y,
                size);
    }
}
