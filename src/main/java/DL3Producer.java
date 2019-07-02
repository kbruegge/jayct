import hexmap.TelescopeArray;
import io.CSVWriter;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.Math.PI;
import static java.util.stream.Collectors.toList;

/**
 * An executable go from images to dl3 including predictions.
 *
 * Created by mackaiver on 04.12.17.
 */

@CommandLine.Command(name = "DL3Producer", description = "Executes CTA analysis")
public class DL3Producer implements Callable<Void> {

    static Logger log = LoggerFactory.getLogger(DL3Producer.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;

    @CommandLine.Parameters(index = "0", paramLabel = "Input Folder for the images")
    String inputFolder = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Input File for the classifier model")
    String clfFile = " ";

    @CommandLine.Parameters(index = "2", paramLabel = "Input File for the regressor model")
    String rgrFile = " ";

    @CommandLine.Parameters(index = "3", paramLabel = "Output path for DL3")
    String outputFile= " ";

    public static void main (String[] args) throws Exception {
        CommandLine.call(new DL3Producer(), System.out, args);
    }

    @Override
    public Void call() throws Exception {

        if (helpRequested) {
            CommandLine.usage(this, System.err);
            return null;
        }


        double alt = 70;
        double az= 180;

        Path file = new File(inputFolder).toPath();

        boolean isDirectory = Files.isDirectory(file);   // Check if it's a directory

        TreeEnsemblePredictor classifier = new TreeEnsemblePredictor(Paths.get(clfFile));
        TreeEnsemblePredictor regressor= new TreeEnsemblePredictor(Paths.get(rgrFile));

        List<Path> paths = new ArrayList<>();

        if (isDirectory) {
            paths = Files.list(file)
                    .filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".json.gz"))
                    .sorted()
                    .collect(toList());
        } else {
            paths.add(file);
        }

        CSVWriter writer = new CSVWriter(new File(outputFile));

        writer.writeHeader("event_id",
                "alt",
                "az",
                "impact_x",
                "impact_y",
                "gamma_prediction",
                "energy_prediction",
                "mc_alt",
                "mc_az",
                "mc_energy"
        );
        for (Path p : paths) {

            log.info("Analyzing file: {}", p.toString());

            ImageReader events = ImageReader.fromPath(p);
            for (ImageReader.Event event : events) {
                List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

                ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, alt, az);

                double prediction = predictType(moments, classifier);

                double energyPrediction = predictEnergy(moments, regressor);

                writer.append(reconstrucedEvent, prediction, energyPrediction, event.mc.alt, event.mc.az, event.mc.energy);
            }
        }

        return null;
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
