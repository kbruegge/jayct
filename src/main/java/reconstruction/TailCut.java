package reconstruction;

import io.ImageReader;
import reconstruction.containers.ShowerImage;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static List<ShowerImage> onImagesInEvent(ImageReader.Event event){
        return streamShowerImages(event).collect(Collectors.toList());
    }
    public static<A, R> R onImagesInEvent(ImageReader.Event event, Collector<ShowerImage, A, R> col){
        return streamShowerImages(event).collect(col);
    }


    public static Stream<ShowerImage> streamShowerImages(ImageReader.Event event){
        return event.images
                .entrySet()
                .stream()
                .map(entry -> selectShowerFromImage(event.eventId, entry.getKey(), entry.getValue()));
    }

    public static ShowerImage selectShowerFromImage(long eventId, int cameraId, double[] image) {

        ShowerImage showerImage = new ShowerImage(cameraId, eventId);

        //add the pixels over the first threshold
        for (int pixelId = 0; pixelId < image.length; pixelId++) {
            double weight = image[pixelId];
            if (weight > levels[0]) {
                showerImage.addPixel(pixelId, weight);
            }
        }

        // dilate the shower
        for (int l = 1; l < levels.length; l++) {
            showerImage.dilate(image, levels[l]);
        }

        return showerImage;
    }
}
