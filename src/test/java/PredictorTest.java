import com.google.common.base.Splitter;
import io.ImageReader;
import org.junit.Test;
import ml.TreeEnsemblePredictor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

/**
 * Test the tree predictor by comparing to sklearn prediction.
 * Created by mackaiver on 27/09/17.
 */
public class PredictorTest {

    @Test
    public void testPrediction() throws URISyntaxException, IOException {
        URL url = PredictorTest.class.getResource("/iris_rf.json");

        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(url.toURI()));

        float[] sample_1 = new float[]{5.9f,  3.2f,  4.8f,  1.8f};
        int prediction = predictor.predict(sample_1);
        assertThat(prediction, is(1));
    }


    @Test
    public void testPredictionRow78() throws URISyntaxException, IOException {
        URL url = PredictorTest.class.getResource("/iris_rf.json");

        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(url.toURI()));

        float[] sample_1 = new float[]{6.70f, 3.00f, 5.00f, 1.70f};
        int prediction = predictor.predict(sample_1);

        assertThat(prediction, is(1));
    }


    @Test
    public void testPredictionAllSamples() throws URISyntaxException, IOException {
        URL url_predictor = PredictorTest.class.getResource("/iris_rf.json");
        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(url_predictor.toURI()));

        URL url = PredictorTest.class.getResource("/python_predictions_iris.csv");

        List<String> lines = Files.readAllLines(Paths.get(url.toURI()));
        lines.stream().skip(1).forEach(line -> {

            List<String> values = Splitter.onPattern(",").trimResults().splitToList(line);
            double[] row= values.stream().limit(4).mapToDouble(Float::parseFloat).toArray();

            int label = Integer.parseInt(values.get(4));
            int prediction = predictor.predict(row);
            assertThat(prediction, is(label));
        });

    }

    @Test
    public void testPredictProba() throws URISyntaxException, IOException {
        URL url = ImageReader.class.getResource("/iris_rf.json");

        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(url.toURI()));

        float[] p0 = {1.0f, 0.0f, 0.0f};
        float[] p1 = {0.0f, 0.9f, 0.1f};
        float[] p2 = {0.0f, 0.0f, 1.0f};

        float[] sample_0 = new float[]{5.1f,  3.5f,  1.4f,  0.2f};
        float[] prediction = predictor.predictProba(sample_0);
        assertArrayEquals(p0, prediction, 0.0f);

        float[] sample_1 = new float[]{5.9f,  3.2f,  4.8f,  1.8f};
        prediction = predictor.predictProba(sample_1);
        assertArrayEquals(p1, prediction, 0.0f);

        float[] sample_2 = new float[]{7.4f,  2.8f,  6.1f,  1.9f};
        prediction = predictor.predictProba(sample_2);
        assertArrayEquals(p2, prediction, 0.0f);
    }

}
