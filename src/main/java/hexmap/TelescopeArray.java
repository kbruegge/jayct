package hexmap;

import com.google.common.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a singleton providing access to geometrical camera descriptions (aka CameraGeometry
 * objects) by the specified telescope/camera id. These ids and camera descriptions are defined in
 * the json files stored in the resources folder : 'cta_array_definition.json' and
 * 'cta_camera_definition.json'.
 *
 * Created by kbruegge on 2/13/17.
 */
public class TelescopeArray {
    private static Logger log = LoggerFactory.getLogger(TelescopeArray.class);

    //see https://github.com/google/guava/wiki/ReflectionExplained for info
    private static final Type CAMERA_DEF = new TypeToken<HashMap<String, CameraGeometry>>() {}.getType();
    private static final Type ARRAY_DEF = new TypeToken<HashMap<Integer, TelescopeDefinition>>() {}.getType();


    /**
     * Singleton instance containing information about cameras' geometry and telescope definition
     */
    private static TelescopeArray mapping;

    /**
     * Map with camera geometries (value) for different cameras (key).
     */
    private final Map<String, CameraGeometry> cameras;

    /**
     * List of the telescopes that make up the array.
     */
    private final HashMap<Integer, TelescopeDefinition> telescopes;

    /**
     * Retrieve the singleton instance of the camera mapping which contains geometry data for the
     * cameras and definition of the telescopes
     */
    public synchronized static TelescopeArray cta() {
        if (mapping == null) {
            try {
                mapping = new TelescopeArray();
            } catch (FileNotFoundException e) {
                log.error("Could not load array or camera definitions from files. Do they exist?");
                throw new InstantiationError();
            }
        }
        return mapping;
    }

    private TelescopeArray() throws FileNotFoundException {
        Gson gson = new GsonBuilder().
                setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        Class cl = TelescopeArray.class;

        // initialize geometry for the cameras
        final InputStream cameraDefs = cl.getResourceAsStream("/camera_definitions/cta_camera_definition.json");
        InputStreamReader reader = new InputStreamReader(cameraDefs);
        this.cameras = gson.fromJson(reader, CAMERA_DEF);

        // initialize definition for the telescopes
        final InputStream arrayDef = cl.getResourceAsStream("/array_definitions/cta_array_definition.json");
        reader = new InputStreamReader(arrayDef);
        this.telescopes = gson.fromJson(reader, ARRAY_DEF);
    }


    /**
     * Get the camera geometry for the given telescope id.
     *
     * @param telescopeId the id of the telescope to get
     * @return the camera geometry for the telescope
     */
    public CameraGeometry cameraFromTelescopeId(int telescopeId) {
        String name = this.telescopes.get(telescopeId).cameraName;
        return cameras.get(name);
    }

    /**
     * Get the Telescope definition for the given id.
     *
     * @param telescopeId the id of the telescope to get
     * @return the TelescopeDefinition object describing this telescope
     */
    public TelescopeDefinition telescopeFromId(int telescopeId){
        //Telescopes ids start with one. Lists are zero based.
        //Let the bugs flow right out of this!
        return telescopes.get(telescopeId);
    }
}
