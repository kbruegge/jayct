package flink;

import hexmap.TelescopeArray;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import picocli.CommandLine;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * /home/kbruegge/jayct/src/main/resources/images.json.gz /home/kbruegge/jayct/src/main/resources/classifier.json
 * Testing the apache flink framework
 * Created by mackaiver on 25/09/17.
 */
@CommandLine.Command(name = "Test Flink", description = "Executes CTA analysis with Apache Flink")
public class DistributeEvents implements Callable<Void>, Serializable {


    @CommandLine.Option(names = {"-p", "--source-parallelism"})
    int sourceParallelism = 4;

    @CommandLine.Option(names = {"-s", "--sink-parallelism"})
    int sinkParallelism = 4;

    @CommandLine.Option(names = {"-m", "--map-parallelism"})
    int mapParallelism = 4;

    @CommandLine.Option(names = {"-l", "--length"}, description = "Number of seconds this stream generates data.")
    int numberOfSecondsToStream= 120;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;

    @CommandLine.Parameters(index = "0", paramLabel = "Input File for the images")
    String inputFile = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Input File for the classifier model")
    String modelFile = " ";



    public static void main (String[] args) throws Exception {

        CommandLine.call(new DistributeEvents(), System.out, args);

    }

    @Override
    public Void call() throws Exception {
        if (helpRequested) {
            CommandLine.usage(this, System.err);
            return null;
        }

        System.out.println("Reading data from file: " + inputFile );
        System.out.println("Reading classifier from file: " +  modelFile);

        StreamExecutionEnvironment env = flinkPlan();
        env.execute();

        return null;
    }
    
    private StreamExecutionEnvironment flinkPlan(){

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.addSource(new InfinteEventSource(inputFile, numberOfSecondsToStream))
            .setParallelism( sourceParallelism)
            .map(new RichMapFunction<ImageReader.Event, Tuple2<ReconstrucedEvent, Double>>() {

                private TreeEnsemblePredictor model;

                @Override
                public void open(Configuration parameters) throws Exception {
                    super.open(parameters);
                    this.model = new TreeEnsemblePredictor(Paths.get(modelFile));
                }

                @Override
                public Tuple2<ReconstrucedEvent, Double> map(ImageReader.Event event) throws Exception {

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
                                    (double) model.predictProba(f)[0]
                            )
                            .average()
                            .orElse(0);

                    ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

                    return Tuple2.of(reconstrucedEvent, prediction);
                }
            })
            .setParallelism(mapParallelism)
            .startNewChain()
            .writeAsCsv("./output.csv", FileSystem.WriteMode.OVERWRITE)
            .setParallelism( sinkParallelism);

        return env;
    }
}
