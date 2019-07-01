package ml;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

import java.nio.file.Paths;

import hexmap.TelescopeArray;
import reconstruction.containers.Moments;

/**
 * Created by alexey on 11.04.18.
 */
public class TreeEnsemblePredictorRichMap extends RichMapFunction<Tuple2<Moments, Integer>, Tuple2<Moments, Double>> {

    private TreeEnsemblePredictor model;

    String modelFile;

    public TreeEnsemblePredictorRichMap(String modelFile) {
        this.modelFile = modelFile;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.model = new TreeEnsemblePredictor(Paths.get(modelFile));
    }

    @Override
    public Tuple2<Moments, Double> map(Tuple2<Moments, Integer> value) throws Exception {

        Moments moments = value.f0;
        float[] vector = new Vectorizer().of(
                value.f1,
                moments.numberOfPixel,
                moments.width,
                moments.length,
                moments.skewness,
                moments.kurtosis,
                moments.size,
                TelescopeArray.cta().telescopeFromId(moments.telescopeID).telescopeType.ordinal()
        ).createFloatVector();

        float p = model.predictProba(vector)[0];
        return Tuple2.of(moments, (double) p);
    }
}
