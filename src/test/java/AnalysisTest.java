import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import hexmap.TelescopeArray;
import io.CSVWriter;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * Created by mackaiver on 09/08/17.
 */
public class AnalysisTest {

    static Logger log = LoggerFactory.getLogger(AnalysisTest.class);


    @Test
    public void testStereoParameters() throws IOException {
        URL url = ImageReader.class.getResource("/data/images.json.gz");

        ImageReader events = ImageReader.fromURL(url);

        for (ImageReader.Event event : events) {
            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

            ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

            assertEquals(2, reconstrucedEvent.altAz.length);
        }

    }



    @Test
    public void testMomentsStream() throws IOException {
        URL url = ImageReader.class.getResource("/data/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);



        List<Moments> moments = events.stream()
                .flatMap(TailCut::streamShowerImages)
                .map(HillasParametrization::fromShowerImage)
                .filter(m -> m.size > 0.0)
                .collect(toList());

        assertTrue(moments.size() >= 2);

        moments.forEach(m -> {
            assertTrue(m.length > m.width);
        });

    }

}
