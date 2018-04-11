package pythonbridge;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.ArrayList;
import java.util.stream.Collectors;

import reconstruction.DirectionReconstruction;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;

/**
 * Created by alexey on 10.04.18.
 */
public class ReconstructionAggregatePython implements AggregateFunction<Tuple2<Moments, Double>, ArrayList<Tuple2<Moments, Double>>, Tuple2<ReconstrucedEvent, Double>> {

    private PythonBridge bridge;

    String method;

//    @Override
//    public void open(Configuration parameters) throws Exception {
//        super.open(parameters);
//        bridge = new PythonBridge(parameters.getString("path", ""));
//        method = parameters.getString("method", "");
//        // see python processor and python context
//    }
//
//    @Override
//    public void close() throws Exception {
//        // see python processor and python context
//        bridge.close();
//        super.close();
//    }

    @Override
    public ArrayList<Tuple2<Moments, Double>> createAccumulator() {
        return new ArrayList<>();
    }

    @Override
    public void add(Tuple2<Moments, Double> value, ArrayList<Tuple2<Moments, Double>>  accumulator) {
        accumulator.add(value);
    }

    @Override
    public Tuple2<ReconstrucedEvent, Double> getResult(ArrayList<Tuple2<Moments, Double>>  accumulator) {
        //TODO: call the bridge!
        //bridge.callMethod(method, ...)
        ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(
                accumulator.stream().map(v -> v.f0).collect(Collectors.toList()), 1, 2);
        double avg = accumulator.stream().mapToDouble(v -> v.f1).average().orElse(0);

        return Tuple2.of(reconstrucedEvent, avg);
    }

    @Override
    public ArrayList<Tuple2<Moments, Double>>  merge(ArrayList<Tuple2<Moments, Double>>  a, ArrayList<Tuple2<Moments, Double>>  b) {
        ArrayList<Tuple2<Moments, Double>> c = new ArrayList<>();
        c.addAll(a);
        c.addAll(b);
        return c;
    }

}
