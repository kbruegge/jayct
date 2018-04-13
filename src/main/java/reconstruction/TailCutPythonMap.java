package reconstruction;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.hadoop.shaded.com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

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
    public Tuple2<ShowerImage, Integer> map(Tuple3<Long, Integer, double[]> event) {
        // prepare resulting shower image while the signal pixels are added from
        // the python script result
        ShowerImage showerImage = new ShowerImage(event.f1, event.f0);

        // map with the input for python method
        ImmutableMap<String, Serializable> map = ImmutableMap.of(
                "image", event.f2, "eventId", event.f0, "cameraId", event.f1);

        try {
            // call python script
            Object[] result = (Object[]) bridge.callMethod(method, map);

            // cast the results into array
            @SuppressWarnings("unchecked")
            ArrayList<Integer> index = (ArrayList<Integer>) result[0];
            @SuppressWarnings("unchecked")
            ArrayList<Double> weights = (ArrayList<Double>) result[1];

            // add the signal pixels to the shower image
            while (index.size() > 0) {
                Integer ind = index.remove(0);
                Double weight = weights.remove(0);
                showerImage.addPixel(ind, weight);
            }
            return new Tuple2<>(showerImage, event.f1);
        } catch (ClassCastException e) {
            System.out.println("TailCut was not successful. Forward ShowerImage without pixels.");
            return new Tuple2<>(showerImage, event.f1);
        } catch (IOException e) {
            // the case that python bridge has an issue with python script execution
            System.out.println("Python script was not successful. Forward ShowerImage without pixels.");
            e.printStackTrace();
            return new Tuple2<>(showerImage, event.f1);
        }
    }
}
