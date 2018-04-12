package reconstruction;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

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
        Object result = bridge.callMethod(method, value.f0.toMap());
        //TODO: use the result in the pipeline and adjust the following function to use it
        //TODO: OR transform the result into moment? (worse performance!?)
        Moments moments = HillasParametrization.fromShowerImage(value.f0);
        return Tuple2.of(moments, value.f1);
    }
}
