import com.google.common.collect.Lists;
import hexmap.TelescopeArray;
import io.CSVWriter;
import io.ImageReader;
import me.tongfei.progressbar.ProgressBar;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
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
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.stream.Collectors.toList;

/**
 * An executable go from images to dl3 including predictions.
 *
 * Created by mackaiver on 04.12.17.
 */

@CommandLine.Command(name = "DL3Producer", description = "Executes CTA analysis")
public class DL3Producer implements Callable<Void> {

//    static Logger log = LoggerFactory.getLogger(DL3Producer.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits.")
    boolean helpRequested = false;

    @CommandLine.Parameters(index = "0", paramLabel = "Input file for the images")
    String inputFolder = " ";

    @CommandLine.Parameters(index = "1", paramLabel = "Input File for the classifier model")
    String modelFile = " ";

    @CommandLine.Parameters(index = "2", paramLabel = "Output path for DL3")
    String outputFile= " ";

    public static void main (String[] args) throws Exception {
        System.out.println(args);
        CommandLine.call(new DL3Producer(), System.out, args);
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
            paths = Lists.newArrayList(input);
        }
        System.out.println(paths);
        TreeEnsemblePredictor model = new TreeEnsemblePredictor(Paths.get(modelFile));

        CSVWriter writer = new CSVWriter(new File(outputFile));

        for (Path p : paths) {
            System.out.println("Analyzing file: " + p.toString());

            ImageReader events = ImageReader.fromPath(p);
            for (ImageReader.Event event : ProgressBar.wrap(events, p.toString())) {
                List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

                ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

                double prediction = predictParticleType(moments, model);

                writer.append(event, reconstrucedEvent, prediction);

            }
        }

        return null;
    }


    private double predictParticleType(List<Moments> moments, TreeEnsemblePredictor model){
        int numberOfTriggeredTelescopes = moments.size();

        return moments.stream()
                .map(m ->
                        new Vectorizer().of(
                                numberOfTriggeredTelescopes,
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
    }

}
