package reconstruction;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import pythonbridge.PythonBridge;
import reconstruction.containers.Moments;

/**
 * Created by alexey on 10.04.18.
 */
public class ReconstructionAggregatePython implements AggregateFunction
        <Tuple2<Moments, Double>, ArrayList<Tuple2<Moments, Double>>,
                Tuple2<HashMap<String, String>, Double>>, Serializable {

    private PythonBridge bridge;

    String method = "";

    public ReconstructionAggregatePython(String method) {
        this.method = method;
    }

    private Object readResolve() {
        bridge = PythonBridge.getInstance();
        bridge.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    bridge.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return this;
    }

    @Override
    public ArrayList<Tuple2<Moments, Double>> createAccumulator() {
        return new ArrayList<>();
    }

    @Override
    public void add(Tuple2<Moments, Double> value, ArrayList<Tuple2<Moments, Double>> accumulator) {
        accumulator.add(value);
    }

    @Override
    public Tuple2<HashMap<String, String>, Double> getResult(ArrayList<Tuple2<Moments, Double>> accumulator) {
        try {
            List<HashMap<String, Object>> hashMaps = accumulator.stream()
                    .map(v -> v.f0.toMap()).collect(Collectors.toList());
            HashMap<String, String> result = (HashMap<String, String>) bridge.callMethod(method, hashMaps);
            double avg = accumulator.stream().mapToDouble(v -> v.f1).average().orElse(0);
            return Tuple2.of(result, avg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Tuple2.of(new HashMap<>(), Double.NaN);
    }

    @Override
    public ArrayList<Tuple2<Moments, Double>> merge(ArrayList<Tuple2<Moments, Double>> a, ArrayList<Tuple2<Moments, Double>> b) {
        ArrayList<Tuple2<Moments, Double>> c = new ArrayList<>();
        c.addAll(a);
        c.addAll(b);
        return c;
    }

}
