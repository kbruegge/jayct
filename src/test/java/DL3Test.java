import io.ImageReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URL;

/**
 * Created by mackaiver on 04.12.17.
 */
public class DL3Test {

    @Rule
    public TemporaryFolder tempFolder= new TemporaryFolder();


    @Test
    public void testProducer() throws Exception {

        URL folder = ImageReader.class.getResource("/data/");
        URL model= ImageReader.class.getResource("/classifier.json");

        DL3Producer p = new DL3Producer();

        p.inputFolder = folder.getPath();
        p.modelFile = model.getPath();
        p.outputFile = tempFolder.newFile().getPath();

        p.call();
    }
}
