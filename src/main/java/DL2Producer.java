import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import hexmap.TelescopeArray;
import hexmap.TelescopeDefinition;
import io.CSVWriter;
import io.ImageReader;
import me.tongfei.progressbar.ProgressBar;
import picocli.CommandLine;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.ArrayEvent;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static hexmap.TelescopeArray.*;
import static java.util.stream.Collectors.toList;

/**
 * An executable go from images to dl2.
 *
 * Created by mackaiver on 04.12.17.
 */

@CommandLine.Command(name = "DL2Producer", description = "Executes CTA analysis")
public class DL2Producer implements Callable<Void> {

//    static Logger log = LoggerFactory.getLogger(DL3Producer.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;

    @CommandLine.Parameters(index = "0", paramLabel = "Input file for the images")
    String inputFolder = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Output folder for DL2")
    String outputFolder= " ";

    public static void main (String[] args) throws Exception {
        System.out.println(args);
        CommandLine.call(new DL2Producer(), System.out, args);
    }

    @Override
    public Void call() throws Exception {

        if (helpRequested) {
            CommandLine.usage(this, System.err);
            return null;
        }
        List<Path> paths;
        Path input = Paths.get(inputFolder);
        if (Files.isDirectory(input)){
            paths = Files.list(input)
                    .filter(p -> p.toString().endsWith(".json") || p.toString().endsWith(".json.gz"))
                    .sorted()
                    .collect(toList());
        } else {
            paths = Lists.newArrayList();
            paths.add(input);
        }

        CSVWriter runsWriter = new CSVWriter(Paths.get(outputFolder, "runs.csv").toFile());
        CSVWriter arrayEventsWriter = new CSVWriter(Paths.get(outputFolder, "array_events.csv").toFile());
        CSVWriter telescopeEventsWriter = new CSVWriter(Paths.get(outputFolder, "telescope_events.csv").toFile());

        for (Path p : paths) {
            System.out.println("Analyzing file: " + p.toString());
            int currentRun = -1;
            ImageReader events = ImageReader.fromPath(p);
            for (ImageReader.Event event : ProgressBar.wrap(events, p.toString())) {

                if(event.mc.runId != currentRun){
                    runsWriter.append(event.mc);
                    currentRun = event.mc.runId;
                }

                List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);
                ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.mcAlt, event.mc.mcAz);

                arrayEventsWriter.append(event, reconstrucedEvent, new ArrayEvent(event, moments));
                for (Moments moment : moments) {
                    double distance = cta().telescopeFromId(moment.telescopeID).getTelescopePosition2D().distance(reconstrucedEvent.impactPosition);
                    telescopeEventsWriter.append(moment, distance, event.runId);
                }
            }
        }

        return null;
    }

}
