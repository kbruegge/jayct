import io.ImageReader;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A heuristic to find signal pixels in the image. Its based on a ideas from the equivalent
 * fact-tools processors, HESS methods and some things in ctapipe.
 *
 * @author Kai Bruegge on 14.02.17
 */
public class TailCut{

    /**
     * Levels to use for image cleaning.
     */
    public static Double[] levels = {10.0, 8.0, 4.5};

    public static List<Shower> selectShowersFromEvent(ImageReader.Event event){
        return event.images
                .entrySet()
                .parallelStream()
                .map(entry -> selectShowerFromImage(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public static Shower selectShowerFromImage(int cameraId, double[] image) {

        Shower shower = new Shower(cameraId);

        //add the pixels over the first threshold
        for (int pixelId = 0; pixelId < image.length; pixelId++) {
            double weight = image[pixelId];
            if (weight > levels[0]) {
                shower.addPixel(pixelId, weight);
            }
        }

        // dilate the shower
        for (int l = 1; l < levels.length; l++) {
            shower.dilate(image, levels[l]);
        }

        return shower;
    }
}
