import io.CSVWriter;
import io.ImageReader;
import org.junit.Test;
import reconstruction.*;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by mackaiver on 25/09/17.
 */
public class IoTest {


    @Test
    public void testEventWriter() throws IOException {
        URL url = ImageReader.class.getResource("/images.json.gz");

        ImageReader events = ImageReader.fromURL(url);

//        File f = new TemporaryFolder().newFile();
        File f = new File("./test.csv");
        CSVWriter writer = new CSVWriter(f);
        writer.writeHeader("#x", "y", "z", "impact_x", "impact_y");
        for (ImageReader.Event event : events) {
            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

            ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

            if (reconstrucedEvent.direction.isNaN()){
                continue;
            }
            writer.appendReconstructedEvent(reconstrucedEvent);
        }



    }
}
