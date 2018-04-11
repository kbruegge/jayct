package flink;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import io.ImageReader;
import ml.TreeEnsemblePredictorRichMap;
import picocli.CommandLine;
import pythonbridge.ReconstructionAggregatePython;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
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
    String modelFile = " ";


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
        System.out.println("Reading classifier from file: " + modelFile);


        StreamExecutionEnvironment env = flinkPlan();
        env.execute();

        return null;
    }

    private StreamExecutionEnvironment flinkPlan() {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


        DataStreamSource<ImageReader.Event> source = env.addSource(new InfiniteEventSource(inputFile));

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
                .map(new TreeEnsemblePredictorRichMap(modelFile))
                .keyBy(new KeySelector<Tuple2<Moments, Double>, Long>() {
                    @Override
                    public Long getKey(Tuple2<Moments, Double> value) throws Exception {
                        return value.f0.eventID;
                    }
                })
                .timeWindow(Time.seconds(windowSize))
                .aggregate(new ReconstructionAggregatePython("python/helloworld.py", "process"))
                .setParallelism(windowParallelism)
                .rescale()
                .writeAsCsv("./output.csv", FileSystem.WriteMode.OVERWRITE)
                .setParallelism(sinkParallelism);

        return env;
    }
}
