import io.ImageReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * test the io
 * Created by mackaiver on 09/08/17.
 */
public class ImageReaderTest {

    @Test
    public void testReader() throws IOException {
        URL url = ImageReader.class.getResource("/data/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);

        for (ImageReader.Event e : events){
            int n = e.array.numTriggeredTelescopes;
            assertTrue("At least 2 telescopes have to trigger in the data", n >= 2);

            assertTrue(
                    "Number of images in the data has to be euqal to the number of triggered telescopes",
                    e.images.size() == n
            );

        }
    }
    
}
