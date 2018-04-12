package reconstruction;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

import pythonbridge.PythonBridge;
import pythonbridge.Utils;
import reconstruction.containers.Moments;
import reconstruction.containers.ShowerImage;

/**
 * Created by alexey on 11.04.18.
 */
public class HillasParametrizationPythonMap extends RichMapFunction<Tuple2<ShowerImage, Integer>, Tuple2<Moments, Integer>> {

    private PythonBridge bridge = null;
    String path = "";
    String method = "";

    public HillasParametrizationPythonMap(String path, String method) {
        this.path = path;
        this.method = method;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        if (!path.equals("")) {
            bridge = Utils.initPythonBridge(path);
        }
    }

    @Override
    public void close() throws Exception {
        bridge.close();
        super.close();
    }

    @Override
    public Tuple2<Moments, Integer> map(Tuple2<ShowerImage, Integer> value) throws Exception {
        //Object result = bridge.callMethod(method, );
        Moments moments = HillasParametrization.fromShowerImage(value.f0);
        return Tuple2.of(moments, value.f1);
    }
}
