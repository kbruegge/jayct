package reconstruction;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.List;

import io.ImageReader;
import pythonbridge.PythonBridge;
import reconstruction.containers.ShowerImage;

/**
 * Created by alexey on 12.04.18.
 */
public class TailCutPythonFlatMap extends RichFlatMapFunction<ImageReader.Event, Tuple2<ShowerImage, Integer>> {

    private PythonBridge bridge = null;
    String method = "";

    public TailCutPythonFlatMap(String method) {
        this.method = method;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        bridge = PythonBridge.getInstance();
    }

    @Override
    public void close() throws Exception {
        bridge.close();
        super.close();
    }

    @Override
    public void flatMap(ImageReader.Event event, Collector<Tuple2<ShowerImage, Integer>> out) throws Exception {
        HashMap<String, Object> map = event.toMap();
        //bridge.callMethod(method, map);
        List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
        int numberOfTelescopes = event.array.numTriggeredTelescopes;
        showerImages.forEach(showerImage -> out.collect(Tuple2.of(showerImage, numberOfTelescopes)));
    }
}
