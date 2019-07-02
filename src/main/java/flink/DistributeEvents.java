package flink;

import hexmap.TelescopeArray;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.*;
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
import java.time.LocalDateTime;
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

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;

    @CommandLine.Parameters(index = "0", paramLabel = "Input File for the images")
    String inputFile = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Input File for the classifier model")
    String clfFile = " ";

    @CommandLine.Parameters(index = "2", paramLabel = "Input File for the regressor model")
    String rgrFile = " ";

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
        System.out.println("Reading classifier from file: " +  clfFile);
        System.out.println("Reading regressor  file: " +  rgrFile);

        StreamExecutionEnvironment env = flinkPlan();
        env.execute();

        return null;
    }
    
    private StreamExecutionEnvironment flinkPlan(){

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.addSource(new InfiniteEventSource(inputFile))
            .setParallelism( sourceParallelism)
            .map(new RichMapFunction<ImageReader.Event, Tuple5<Double, Double, Double, Double, LocalDateTime>>() {

                private TreeEnsemblePredictor classifier;
                private TreeEnsemblePredictor regressor;

                @Override
                public void open(Configuration parameters) throws Exception {
                    super.open(parameters);
                    this.classifier = new TreeEnsemblePredictor(Paths.get(clfFile));
                    this.regressor= new TreeEnsemblePredictor(Paths.get(rgrFile));
                }

                @Override
                public Tuple5<Double, Double, Double, Double, LocalDateTime> map(ImageReader.Event event) throws Exception {

                    List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                    List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);


                    double prediction = predictType(moments, classifier);
                    double energyPrediction = predictEnergy(moments, regressor);

                    ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

                    return Tuple5.of(reconstrucedEvent.altAz[0], reconstrucedEvent.altAz[1], prediction, energyPrediction, LocalDateTime.now());
                }
            })
            .setParallelism(mapParallelism)
            .startNewChain()
            .writeAsCsv("./output.csv", FileSystem.WriteMode.OVERWRITE)
            .setParallelism(sinkParallelism);

        return env;
    }
    private double predictType(List<Moments> moments, TreeEnsemblePredictor classifier){
        return moments.stream()
                .map(Moments::toFeatureMap
                )
                .mapToDouble(f ->
                        (double) classifier.predictProba(f)[1]
                )
                .average()
                .orElse(Double.NaN);
    }


    private double predictEnergy(List<Moments> moments, TreeEnsemblePredictor regressor){
        return moments.stream()
                .map(Moments::toFeatureMap
                )
                .mapToDouble(f ->
                        (double) regressor.predictProba(f)[0]
                )
                .average()
                .orElse(Double.NaN);
    }
}

