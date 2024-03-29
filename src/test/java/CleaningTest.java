import io.ImageReader;
import org.junit.Test;
import reconstruction.containers.ShowerImage;
import reconstruction.TailCut;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by mackaiver on 09/08/17.
 */
public class CleaningTest {

    @Test
    public void testTailCut() throws IOException {
        URL url = ImageReader.class.getResource("/data/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);
        ImageReader.Event e = events.iterator().next();

        List<ShowerImage> showerImages = TailCut.onImagesInEvent(e);

        assertTrue(showerImages.size() >= 2);

        //each shower should have at least two signal pixels
        for( ShowerImage s : showerImages){
            assertTrue(s.signalPixels.size() >= 2);
        }
    }
}
