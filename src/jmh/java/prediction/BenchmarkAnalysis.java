package prediction;

import hexmap.TelescopeArray;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ShowerImage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@State(Scope.Thread)
public class BenchmarkAnalysis {

    TreeEnsemblePredictor predictor;
    Random random;
    List<ImageReader.Event> events;

    @Setup
    public void prepare(){
        try {

            InputStream stream = BenchmarkAnalysis.this.getClass().getResourceAsStream("/classifier_cta.json");
            predictor = new TreeEnsemblePredictor(stream);

            ImageReader r = ImageReader.fromInputStream(BenchmarkAnalysis.this.getClass().getResourceAsStream("/images.json.gz"));
            events = r.stream().collect(Collectors.toList());

            random = new Random();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Benchmark
    public void testPrediction(Blackhole bh) throws IOException, URISyntaxException {

        ImageReader.Event event = events.get(random.nextInt(events.size()));
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

        bh.consume(prediction);

    }


    @Benchmark
    public void testPredictionLoopParallel(Blackhole bh) throws IOException, URISyntaxException {


        events.parallelStream().forEach(event -> {
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

            bh.consume(prediction);
        });


    }
}