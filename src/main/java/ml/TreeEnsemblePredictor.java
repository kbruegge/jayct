package ml;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class can be created from a json file produced from pre-trained sklearn decision trees.
 * It can predict new samples.
 */
public class TreeEnsemblePredictor {
    private final Gson gson = new GsonBuilder().create();
    private final DecisionTree[] trees;



    private class DecisionTree {
        public final float[] thresholds;
        public final int[] attributes;
        public final int[] children_left;
        public final int[] children_right;
        public final float[][] node_distributions;

        private DecisionTree(float[] thresholds, int[] attributes, int[] children_left, int[] children_right, float[][] node_distributions) {
            this.thresholds = thresholds;
            this.attributes = attributes;
            this.children_left = children_left;
            this.children_right = children_right;
            this.node_distributions = node_distributions;
        }

        public int getNumberOfClasses(){
            return this.node_distributions[0].length;
        }
    }

    /**
     * Create an ensemble of decision trees from the path to a json file.
     *
     * @param pathToModel path to json file
     *
     * @throws IOException in case the file cannot be read.
     */
    public TreeEnsemblePredictor(Path pathToModel) throws IOException {
        BufferedReader reader = Files.newBufferedReader(pathToModel);
        TypeToken<DecisionTree[]> t = TypeToken.of(DecisionTree[].class);
        trees = gson.fromJson(reader, t.getType());
    }


    /**
     * Create an ensemble of decision trees from the input stream of some json input.
     *
     * @param in inputstream coming from a some json input
     *
     * @throws IOException in case the file cannot be read.
     */
    public TreeEnsemblePredictor(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        TypeToken<DecisionTree[]> t = TypeToken.of(DecisionTree[].class);
        trees = gson.fromJson(reader, t.getType());
    }

    public float[] predictProba(float[] sample) {
        float[] predictions = new float[trees[0].getNumberOfClasses()];

        for (DecisionTree tree : trees) {
            float[] p = predictTree(sample, tree);

            predictions[argmax(p)] += 1;
        }

        for (int i = 0; i < predictions.length; i++) {
            predictions[i] /= trees.length;
        }

        return predictions;
    }

    /**
     * @see TreeEnsemblePredictor#predict(float[])
     * @param sample the samepl to predict
     * @return the predicted class
     */
    public int predict(double[] sample) {
        float s[] = new float[sample.length];
        for (int i = 0; i < sample.length; i++) {
            s[i] = (float) sample[i];
        }
        return predict(s);
    }

    /**
     * Predict the class of given sample by averaging over the responses of all trees and returning
     * the class (encoded as int) with the highest probability.
     *
     * @param sample the sample to predict the class of
     * @return return the predicted class
     */
    public int predict(float[] sample){
        float[] prediction = predictProba(sample);

        return argmax(prediction);
    }


    private int argmax(float[] array) {
        int maxIndex = 0;

        for (int i = 0; i < array.length; i++) {
            float p = array[i];
            maxIndex = p > array[maxIndex] ? i : maxIndex;
        }

        return maxIndex;
    }



    private float[] predictTree(float[] sample, DecisionTree tree){

        int node_index = 0;

        while (tree.attributes[node_index] >= 0) {
            float value = sample[tree.attributes[node_index]];
            if (value <= tree.thresholds[node_index]) {
                node_index = tree.children_left[node_index];
            } else {
                node_index = tree.children_right[node_index];
            }
        }

        return tree.node_distributions[node_index];
    }

    /**
     * Get the number of trees in the ensemble
     * @return the number of trees
     */
    public int getNumberOfTrees(){
        return trees.length;
    }
}
