package flink;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import io.ImageReader;
import ml.TreeEnsemblePredictor;
import picocli.CommandLine;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

/**
 * /home/kbruegge/jayct/src/main/resources/images.json.gz /home/kbruegge/jayct/src/main/resources/classifier.json
 * Testing the apache flink framework Created by mackaiver on 25/09/17.
 */
@CommandLine.Command(name = "Test Flink", description = "Executes CTA analysis with Apache Flink")
public class DistributeImages implements Callable<Void>, Serializable {


    @CommandLine.Option(names = {"-p", "--source-parallelism"})
    int sourceParallelism = 4;

    @CommandLine.Option(names = {"-s", "--sink-parallelism"})
    int sinkParallelism = 4;

    @CommandLine.Option(names = {"-w", "--window-parallelism"})
    int windowParallelism = 4;

    @CommandLine.Option(names = {"-c", "--window-size"}, description = "Size of window in seconds.")
    int windowSize = 5;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;


    @CommandLine.Parameters(index = "0", paramLabel = "Input File for the images")
    String inputFile = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Input File for the classifier model")
    String clfFile = " ";

    @CommandLine.Parameters(index = "2", paramLabel = "Input File for the regressor model")
    String rgrFile = " ";



    public static void main(String[] args) throws Exception {

        CommandLine.call(new DistributeImages(), System.out, args);

    }

    @Override
    public Void call() throws Exception {
        if (helpRequested) {
            CommandLine.usage(this, System.err);
            return null;
        }


        System.out.println("Reading data from file: " + inputFile);
        System.out.println("Reading classifier from file: " + clfFile);


        StreamExecutionEnvironment env = flinkPlan();
        env.execute();

        return null;
    }

    private StreamExecutionEnvironment flinkPlan() {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


        DataStreamSource<ImageReader.Event> source = env.addSource(new InfiniteEventSource(inputFile));
        env.setParallelism(1);

        source
                .setParallelism(sourceParallelism)
                .rescale()
                .flatMap(new FlatMapFunction<ImageReader.Event, Tuple2<ShowerImage, Integer>>() {

                    @Override
                    public void flatMap(ImageReader.Event event, Collector<Tuple2<ShowerImage, Integer>> out) throws Exception {

                        List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                        int numberOfTelescopes = event.array.numTriggeredTelescopes;
                        showerImages.forEach(i -> out.collect(Tuple2.of(i, numberOfTelescopes)));
                    }

                })
                .map(new MapFunction<Tuple2<ShowerImage, Integer>, Tuple2<Moments, Integer>>() {

                    @Override
                    public Tuple2<Moments, Integer> map(Tuple2<ShowerImage, Integer> value) throws Exception {
                        Moments moments = HillasParametrization.fromShowerImage(value.f0);
                        return Tuple2.of(moments, value.f1);
                    }
                })
                .filter(new FilterFunction<Tuple2<Moments, Integer>>() {
                    @Override
                    public boolean filter(Tuple2<Moments, Integer> value) throws Exception {
                        return value.f0.numberOfPixel > 4;
                    }
                })
                .map(new RichMapFunction<Tuple2<Moments, Integer>, Tuple3<Moments, Double, Double>>() {

                    private TreeEnsemblePredictor classifier;
                    private TreeEnsemblePredictor regressor;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.classifier = new TreeEnsemblePredictor(Paths.get(clfFile));
                        this.regressor= new TreeEnsemblePredictor(Paths.get(rgrFile));
                    }

                    @Override
                    public Tuple3<Moments, Double, Double> map(Tuple2<Moments, Integer> value) throws Exception {
                        Moments m = value.f0;
                        double p = classifier.predictProba(m.toFeatureMap())[1];
                        double energy = regressor.predictProba(m.toFeatureMap())[0];
                        return Tuple3.of(m, p, energy);
                    }
                })
                .keyBy(new KeySelector<Tuple3<Moments, Double, Double>, Long>() {
                    @Override
                    public Long getKey(Tuple3<Moments, Double, Double> value) throws Exception {
                        return value.f0.eventID;
                    }
                })
                .timeWindow(Time.seconds(windowSize))
                .aggregate(new AggregateFunction<Tuple3<Moments, Double, Double>, ArrayList<Tuple3<Moments, Double, Double>>, Tuple2<ReconstrucedEvent, Double>>() {
                    @Override
                    public ArrayList<Tuple3<Moments, Double, Double>> createAccumulator() {
                        return new ArrayList<>();
                    }

                    @Override
                    public void add(Tuple3<Moments, Double, Double> value, ArrayList<Tuple3<Moments, Double, Double>> accumulator) {
                        accumulator.add(value);
                    }

                    @Override
                    public Tuple2<ReconstrucedEvent, Double> getResult(ArrayList<Tuple3<Moments, Double, Double>> accumulator) {
                        ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(accumulator.stream().map(v -> v.f0).collect(Collectors.toList()), 1, 2);
                        double avg = accumulator.stream().mapToDouble(v -> v.f1).average().orElse(0);

                        return Tuple2.of(reconstrucedEvent, avg);
                    }

                    @Override
                    public ArrayList<Tuple3<Moments, Double, Double>> merge(ArrayList<Tuple3<Moments, Double , Double>> a, ArrayList<Tuple3<Moments, Double, Double>> b) {
                        ArrayList<Tuple3<Moments, Double, Double>> c = new ArrayList<>();
                        c.addAll(a);
                        c.addAll(b);
                        return c;
                    }
                })
                .setParallelism(windowParallelism)
                .map(new MapFunction<Tuple2<ReconstrucedEvent,Double>, Tuple2<Double, String>>() {
                    @Override
                    public Tuple2<Double, String> map(Tuple2<ReconstrucedEvent, Double> value) throws Exception {
                        return new Tuple2<Double, String>(value.f1, LocalDateTime.now().toString());
                    }
                })
                .rescale()
                .writeAsCsv("./output-java.csv", FileSystem.WriteMode.OVERWRITE)
                .setParallelism(sinkParallelism);

        return env;
    }
}
