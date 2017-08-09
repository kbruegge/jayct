import io.ImageReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by mackaiver on 09/08/17.
 */
public class CleaningTest {

    @Test
    public void testTailCut() throws IOException {
        URL url = ImageReader.class.getResource("/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);
        ImageReader.Event e = events.next();

        List<Shower> showers = TailCut.selectShowersFromEvent(e);

        assertTrue(showers.size() >= 2);

        //each shower should have at least two signal pixels
        for( Shower s : showers){
            assertTrue(s.signalPixels.size() >= 2);
        }
    }
}
