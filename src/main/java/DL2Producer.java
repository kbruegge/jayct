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

import static java.util.stream.Collectors.toList;

/**
 * An executable go from images to dl3 including predictions.
 *
 * Created by mackaiver on 04.12.17.
 */

@CommandLine.Command(name = "DL2Producer", description = "Executes CTA analysis")
public class DL2Producer implements Callable<Void> {

    static Logger log = LoggerFactory.getLogger(DL2Producer.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;

    @CommandLine.Parameters(index = "0", paramLabel = "Input Folder for the images")
    String inputFolder = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Output path for DL2")
    String outputFile= " ";

    public static void main (String[] args) throws Exception {
        CommandLine.call(new DL2Producer(), System.out, args);
    }

    @Override
    public Void call() throws Exception {

        if (helpRequested) {
            CommandLine.usage(this, System.err);
            return null;
        }


        Path file = new File(inputFolder).toPath();

        boolean isDirectory = Files.isDirectory(file);   // Check if it's a directory


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
                "telescope_id",
                "length",
                "width",
                "kurtosis",
                "skewness",
                "size",
                "number_of_pixel",
                "r",
                "delta",
                "mc_energy"
        );
        for (Path p : paths) {

            log.info("Analyzing file: {}", p.toString());

            ImageReader events = ImageReader.fromPath(p);
            for (ImageReader.Event event : events) {
                List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

                for (Moments moment: moments){
                    writer.append(moment, event.mc.energy);
                }


            }
        }

        return null;
    }


}
