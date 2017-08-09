import io.ImageReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertTrue;

/**
 *
 * Created by mackaiver on 09/08/17.
 */
public class AnalysisTest {


    @Test
    public void testStereoParameters() throws IOException {
        URL url = ImageReader.class.getResource("/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);
        ImageReader.Event e = events.next();

        List<Shower> showers = TailCut.collectShowersFromEvent(e);
        List<Moments> moments = showers.stream().map(Moments::fromShower).collect(Collectors.toList());

        Stereo stereo = Stereo.fromMoments(moments, 0, 1);
        assertTrue(stereo.getDirectionZ() > 0);
    }

    @Test
    public void testMomentsStream() throws IOException {
        URL url = ImageReader.class.getResource("/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);

        List<Moments> moments = events.stream()
                .flatMap(TailCut::selectShowersInEvent)
                .map(Moments::fromShower)
                .filter(m -> m.size > 0.0)
                .collect(Collectors.toList());

        assertTrue(moments.size() >= 2);

        moments.forEach(m -> {
            assertTrue(m.length > m.width);
        });

        events.stream()
                .flatMap(TailCut::selectShowersInEvent)
                .map(Moments::fromShower)
                .filter(m -> m.size > 0)
                .collect(Collectors.groupingBy(m -> m.eventId));
    }

    @Test
    public void testStereoStream() throws IOException {
        URL url = ImageReader.class.getResource("/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);

        //TODO: propagate pointing and MC information?
        List<Stereo> stereos = events.stream()
                .flatMap(TailCut::selectShowersInEvent)
                .map(Moments::fromShower)
                .filter(m -> m.size > 0)
                .collect(Collectors.groupingBy(m -> m.eventId))
                .entrySet().stream()
                .map(e -> Stereo.fromMoments(e.getValue(), 0, 0))
                .collect(Collectors.toList());
    }
}
