package flink;

import hexmap.TelescopeArray;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import picocli.CommandLine;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.io.Serializable;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * /home/kbruegge/jayct/src/main/resources/images.json.gz /home/kbruegge/jayct/src/main/resources/classifier.json
 * Testing the apache flink framework
 */
@CommandLine.Command(name = "Test Flink", description = "Executes CTA analysis with Apache Flink")
public class DistributeImages implements Callable<Void>, Serializable {


    @CommandLine.Option(names = {"-p", "--source-parallelism"})
    int sourceParallelism = 4;

    @CommandLine.Option(names = {"-s", "--sink-parallelism"})
    int sinkParallelism = 4;

    @CommandLine.Option(names = {"-l", "--length"}, description = "Number of seconds this stream generates data.")
    int numberOfSecondsToStream= 120;

    @CommandLine.Option(names = {"-m", "--map-parallelism"})
    int mapParallelism = 4;

    @CommandLine.Option(names = {"-w", "--window-parallelism"})
    int windowParallelism = 4;

    @CommandLine.Option(names = {"-c", "--window-size"}, description = "Size of window in seconds.")
    int windowSize = 5;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;

    @CommandLine.Parameters(index = "0", paramLabel = "Input File for the images")
    String inputFile = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Input File for the classifier model")
    String modelFile = " ";

    @CommandLine.Parameters(index = "2", paramLabel = "Output File to be written.")
    String outputFile = " ";



    public static void main (String[] args) throws Exception {

        CommandLine.call(new DistributeImages(), System.out, args);

    }

    @Override
    public Void call() throws Exception {
        if (helpRequested) {
            CommandLine.usage(this, System.err);
            return null;
        }


        System.out.println("Reading data from file: " + inputFile);
        System.out.println("Reading classifier from file: " +  modelFile);
        System.out.println("Writing result to file: " +  outputFile);


        StreamExecutionEnvironment env = flinkPlan();
        env.execute();

        return null;
    }
    
    private StreamExecutionEnvironment flinkPlan(){

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


        DataStreamSource<ImageReader.Event> source = env.addSource(new InfinteEventSource(inputFile, numberOfSecondsToStream));


        source
                .setParallelism( sourceParallelism)
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
                    public Tuple2<Moments, Integer> map( Tuple2<ShowerImage, Integer> value) throws Exception {
                        Moments moments = HillasParametrization.fromShowerImage(value.f0);
                        return  Tuple2.of(moments, value.f1);
                    }
                })
                .filter(new FilterFunction<Tuple2<Moments, Integer>>() {
                    @Override
                    public boolean filter(Tuple2<Moments, Integer> value) throws Exception {
                        return value.f0.numberOfPixel > 4;
                    }
                })
                .map(new RichMapFunction<Tuple2<Moments,Integer>, Tuple2<Moments, Double>>() {

                    private TreeEnsemblePredictor model;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        super.open(parameters);
                        this.model = new TreeEnsemblePredictor(Paths.get(modelFile));
                    }

                    @Override
                    public Tuple2<Moments, Double> map(Tuple2<Moments, Integer> value) throws Exception {

                        Moments m = value.f0;
                        float[] vector = new Vectorizer().of(
                                value.f1,
                                m.numberOfPixel,
                                m.width,
                                m.length,
                                m.skewness,
                                m.kurtosis,
                                m.phi,
                                m.miss,
                                m.size,
                                TelescopeArray.cta().telescopeFromId(m.telescopeID).telescopeType.ordinal()
                        ).createFloatVector();

                        float p = model.predictProba(vector)[0];
                        return Tuple2.of(m, (double)p);
                    }
                })
                .setParallelism(mapParallelism)
                .keyBy(new KeySelector<Tuple2<Moments,Double>, Long>() {
                    @Override
                    public Long getKey(Tuple2<Moments, Double> value) throws Exception {
                        return value.f0.eventID;
                    }
                })
                .timeWindow(Time.seconds(windowSize))
                .aggregate(new AggregateFunction<Tuple2<Moments,Double>, ArrayList<Tuple2<Moments,Double>>, Tuple2<ReconstrucedEvent, Double>>() {
                    @Override
                    public ArrayList<Tuple2<Moments, Double>> createAccumulator() {
                        return new ArrayList<>();
                    }

                    @Override
                    public void add(Tuple2<Moments, Double> value, ArrayList<Tuple2<Moments, Double>>  accumulator) {
                        accumulator.add(value);
                    }

                    @Override
                    public Tuple2<ReconstrucedEvent, Double> getResult(ArrayList<Tuple2<Moments, Double>>  accumulator) {
                        ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(accumulator.stream().map(v -> v.f0).collect(Collectors.toList()), 1, 2);
                        double avg = accumulator.stream().mapToDouble(v -> v.f1).average().orElse(0);

                        return Tuple2.of(reconstrucedEvent, avg);
                    }

                    @Override
                    public ArrayList<Tuple2<Moments, Double>>  merge(ArrayList<Tuple2<Moments, Double>>  a, ArrayList<Tuple2<Moments, Double>>  b) {
                        ArrayList<Tuple2<Moments, Double>> c = new ArrayList<>();
                        c.addAll(a);
                        c.addAll(b);
                        return c;
                    }
                })
                .setParallelism( windowParallelism)
                .rescale()
                .map(new MapFunction<Tuple2<ReconstrucedEvent, Double>, Tuple5<String, Double, Double, Double, Double>>() {
                    @Override
                    public Tuple5<String, Double, Double, Double, Double> map(Tuple2<ReconstrucedEvent, Double> value) throws Exception {
                        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        double x = value.f0.direction.getX();
                        double y = value.f0.direction.getY();
                        double z = value.f0.direction.getZ();
                        return Tuple5.of(timestamp, x, y, z, value.f1);
                    }
                })
                .writeAsCsv(outputFile, FileSystem.WriteMode.OVERWRITE)
                .setParallelism( sinkParallelism);

        return env;
    }
}
