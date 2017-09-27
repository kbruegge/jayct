package ml;

import com.google.common.primitives.Doubles;
import org.apache.flink.shaded.com.google.common.primitives.Floats;

import java.util.ArrayList;

/**
 * Helper class to create feature vectors for predictions.
 * Created by mackaiver on 27/09/17.
 */
public class Vectorizer {


    ArrayList<Double> values = new ArrayList<>();


    public Vectorizer of(double a){
        values.add(a);
        return this;
    }


    public Vectorizer of(double... a){
        for (double v : a) {
            values.add(v);
        }
        return this;
    }

    public float[] createFloatVector(){
        return Floats.toArray(values);
    }

    public double[] createVector(){
        return Doubles.toArray(values);
    }


}
