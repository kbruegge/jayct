package pythonbridge;

import java.io.IOException;
import java.net.URL;

/**
 * Created by alexey on 11.04.18.
 */
public class Utils {

    public static PythonBridge initPythonBridge(String path) {
        PythonBridge bridge = null;
        try {
            URL urlPath = Utils.class.getClassLoader().getResource(path);
            if (urlPath != null) {
                bridge = new PythonBridge(urlPath.getPath());
            } else {
                System.out.println("Path to the python file is not right.\nStopping the programm...");
                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bridge;
    }
}
