import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import pythonbridge.PythonBridge;
import reconstruction.containers.Moments;

public class PythonTest {
    static PythonBridge bridge;

    @Before
    public void setUp() throws Exception {
        // get the python bridge
        bridge = PythonBridge.getInstance();
        bridge.start();
        assert bridge != null;
    }

    @After
    public void tearDown() throws Exception {
        // close the python bridge
        bridge.close();
    }

    @Test
    public void testBridge() throws Exception {
        String s1 = "Hello! I pitty the fool.";
        String s2 = (String) bridge.callMethod("ping", s1);

        assert s1.equals(s2);
    }


    @Test
    public void testHillas() throws Exception {
        Random r = new Random();

        double[] b = new double[1855];
        for (int i = 0; i < b.length; i++) {
            b[i] = r.nextDouble() * 50;
        }

        ImmutableMap<String, Serializable> input = ImmutableMap.of(
                "image", b, "cameraId", 1, "eventId", 5);

        // call python script
        @SuppressWarnings("unchecked")
        HashMap<String, Double> o = (HashMap<String, Double>) bridge.callMethod("hillas", input);

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
        Random r = new Random();

        double[] b = new double[1855];
        for (int i = 0; i < b.length; i++) {
            b[i] = r.nextDouble() * 50;
        }

        ImmutableMap<String, Serializable> input = ImmutableMap.of(
                "image", b, "cameraId", 1, "eventId", 5);
        Object[] result = (Object[]) bridge.callMethod("tail_cut", input);

        // cast the results into array
        @SuppressWarnings("unchecked")
        ArrayList<Integer> index = (ArrayList<Integer>) result[0];

        @SuppressWarnings("unchecked")
        ArrayList<Double> weights = (ArrayList<Double>) result[1];
    }
}
