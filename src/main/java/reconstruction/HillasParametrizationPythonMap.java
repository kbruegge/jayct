package reconstruction;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

import java.util.HashMap;

import pythonbridge.PythonBridge;
import reconstruction.containers.Moments;
import reconstruction.containers.ShowerImage;

/**
 * Created by alexey on 11.04.18.
 */
public class HillasParametrizationPythonMap extends RichMapFunction<Tuple2<ShowerImage, Integer>, Tuple2<Moments, Integer>> {

    private PythonBridge bridge = null;
    String method = "";

    public HillasParametrizationPythonMap(String method) {
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
    public Tuple2<Moments, Integer> map(Tuple2<ShowerImage, Integer> value) throws Exception {
        HashMap<String, Double> result = (HashMap<String, Double>) bridge.callMethod(method, value.f0.toMap());
        Moments moments = new Moments(value.f0.eventId, value.f0.cameraId, value.f0.cameraId,
                value.f0.signalPixels.size(),
                result.get("width"),
                result.get("length"),
                result.get("psi"),
                result.get("skewness"),
                result.get("kurtosis"),
                result.get("phi"),
                result.get("miss"),
                result.get("r"),
                result.get("cen_x"),
                result.get("cen_y"),
                result.get("size"));
        return Tuple2.of(moments, value.f1);
    }
}
