package reconstruction;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.hadoop.shaded.com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.HashMap;

import pythonbridge.PythonBridge;
import reconstruction.containers.ShowerImage;

/**
 * Created by alexey on 12.04.18.
 */
public class TailCutPythonMap extends RichMapFunction<Tuple3<Long, Integer, double[]>, Tuple2<ShowerImage, Integer>> {

    private PythonBridge bridge = null;
    String method = "";

    public TailCutPythonMap(String method) {
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
    public Tuple2<ShowerImage, Integer> map(Tuple3<Long, Integer, double[]> event) throws Exception {
        ImmutableMap<String, Serializable> map = ImmutableMap.of(
                "image", event.f2, "eventId", event.f0, "cameraId", event.f1);
        HashMap<String, Object> result = (HashMap<String, Object>)
                bridge.callMethod(method, map);

        ShowerImage showerImage = new ShowerImage(event.f1, event.f0);
//        List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
//        int numberOfTelescopes = event.array.numTriggeredTelescopes;
//        showerImages.forEach(showerImage -> out.collect(Tuple2.of(showerImage, numberOfTelescopes)));
        return new Tuple2<>(showerImage, event.f1);
    }
}
