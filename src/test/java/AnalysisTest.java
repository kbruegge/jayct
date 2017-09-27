import hexmap.TelescopeArray;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.junit.Test;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 *
 * Created by mackaiver on 09/08/17.
 */
public class AnalysisTest {

    static Logger log = LoggerFactory.getLogger(AnalysisTest.class);


    @Test
    public void testStereoParameters() throws IOException {
        URL url = ImageReader.class.getResource("/images.json.gz");

        ImageReader events = ImageReader.fromURL(url);

        for (ImageReader.Event event : events) {
            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

            ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

            if (reconstrucedEvent.direction.isNaN()){
                continue;
            }

            assertTrue(reconstrucedEvent.direction.getZ() > 0);
        }

    }

//    @Test
//    public void testCycle() throws IOException {
//        URL url = ImageReader.class.getResource("/images.json.gz");
//        ImageReader events = ImageReader.fromURL(url);
//
//        Iterable<ImageReader.Event> cycle = Iterables.cycle(events);
//
//        int N = 300;
//
//        Stopwatch stopwatch = Stopwatch.createStarted();
//
//        cycle.
//        IntStream.range(0, N).forEach(i -> {
//            ImageReader.Event event = cycle.;
//            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
//            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);
//
//            DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);
//        });
//
//
//        Duration duration = stopwatch.elapsed();
//        log.info("Reconstruced {} event in {} seconds. Thats {} events per second", N, duration.getSeconds(), N/duration.getSeconds());
//    }

    @Test
    public void testMomentsStream() throws IOException {
        URL url = ImageReader.class.getResource("/images.json.gz");
        ImageReader events = ImageReader.fromURL(url);



        List<Moments> moments = events.stream()
                .flatMap(TailCut::streamShowerImages)
                .map(HillasParametrization::fromShowerImage)
                .filter(m -> m.size > 0.0)
                .collect(Collectors.toList());

        assertTrue(moments.size() >= 2);

        moments.forEach(m -> {
            assertTrue(m.length > m.width);
        });

    }

    @Test
    public void testPrediction() throws IOException, URISyntaxException {
        ImageReader events = ImageReader.fromURL(ImageReader.class.getResource("/images.json.gz"));

        URL predictorURL = ImageReader.class.getResource("/classifier.json");

        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(predictorURL.toURI()));

        for (ImageReader.Event event : events) {
            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

            int numberOfTelescopes = moments.size();

            double prediction = moments.stream()
                    .map(m ->
                            new Vectorizer().of(
                                    numberOfTelescopes,
                                    m.numberOfPixel,
                                    m.width,
                                    m.length,
                                    m.skewness,
                                    m.kurtosis,
                                    m.phi,
                                    m.miss,
                                    m.size,
                                    TelescopeArray.cta().telescopeFromId(m.telescopeID).telescopeType.ordinal()
                            ).createFloatVector()
                    )
                    .mapToDouble(f ->
                            (double) predictor.predictProba(f)[0]
                    )
                    .average()
                    .orElse(0);
        }

    }

//    @Test
//    public void testStereoStream() throws IOException {
//        URL url = ImageReader.class.getResource("/images.json.gz");
//        ImageReader events = ImageReader.fromURL(url);
//
//        List<DirectionReconstruction.ReconstrucedEvent> reconstrucedEvents = events.stream()
//                .flatMap(TailCut::streamShowerImages)
//                .map(HillasParametrization::fromShowerImage)
//                .filter(m -> m.size > 0.0)
//                .collect(groupingBy(m -> m.eventId))
//                .entrySet().stream()
//                .map(e -> DirectionReconstruction.fromMoments(e.getValue(), 0, 0))
//                .filter(e -> !e.direction.isNaN())
//                .collect(Collectors.toList());
//
//        reconstrucedEvents.forEach(e -> assertFalse(e.direction.isInfinite()));
//    }

}
