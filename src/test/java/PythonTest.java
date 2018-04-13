import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import pythonbridge.PythonBridge;
import reconstruction.containers.Moments;

public class PythonTest {

    @Test
    public void testBridge() throws Exception {
        PythonBridge bridge = PythonBridge.getInstance();

        assert bridge != null;

        String s1 = "Hello! I pitty the fool.";
        String s2 = (String) bridge.callMethod("ping", s1);

        assert s1.equals(s2);

        bridge.close();
    }


    @Test
    public void testHillas() throws Exception {
        PythonBridge bridge = PythonBridge.getInstance();
        Random r = new Random();
        assert bridge != null;

        double[] b = new double[1855];
        for (int i = 0; i < b.length; i++) {
            b[i] = r.nextDouble() * 50;
        }

        HashMap<String, Double> o = (HashMap<String, Double>) bridge.callMethod("hillas", ImmutableMap.of("image", b, "cameraId", 1, "eventId", 5));
        bridge.close();

        Moments m = new Moments(1, 1, 2, 2,
                o.get("width"),
                o.get("length"),
                o.get("psi"),
                o.get("skewness"),
                o.get("kurtosis"),
                o.get("phi"),
                o.get("miss"),
                o.get("r"),
                o.get("cen_x"),
                o.get("cen_y"),
                o.get("size"));

        assert m.kurtosis == o.get("kurtosis");
    }

    @Test
    public void testTailCut() throws Exception {
        PythonBridge bridge = PythonBridge.getInstance();

        Random r = new Random();
        assert bridge != null;

        double[] b = new double[1855];
        for (int i = 0; i < b.length; i++) {
            b[i] = r.nextDouble() * 50;
        }
        Object[] result = (Object[]) bridge.callMethod("tail_cut", ImmutableMap.of("image", b, "cameraId", 1, "eventId", 5));

        // cast the results into array
        @SuppressWarnings("unchecked")
        ArrayList<Integer> index = (ArrayList<Integer>) result[0];
        @SuppressWarnings("unchecked")
        ArrayList<Double> weights = (ArrayList<Double>) result[1];

        bridge.close();
    }
}
