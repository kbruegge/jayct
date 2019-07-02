package ml;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * This class can be created from a json file produced from pre-trained sklearn decision trees.
 * It can predict new samples.
 */
public class TreeEnsemblePredictor implements Serializable{
    private final DecisionTree[] trees;



    private class DecisionTree implements Serializable {
        public final String[] names;
        public final float[] thresholds;
        public final int[] attributes;
        public final int[] children_left;
        public final int[] children_right;
        public final float[][] node_distributions;

        private DecisionTree(String[] names, float[] thresholds, int[] attributes, int[] children_left, int[] children_right, float[][] node_distributions) {
            this.names = names;
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
        trees = new GsonBuilder().create().fromJson(reader, t.getType());
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
        trees = new GsonBuilder().create().fromJson(reader, t.getType());
    }


    private float[] createSampleVector(Map<String, Float> sample){
        float[] sampleValues = new float[trees[0].names.length];
        for (int i = 0; i < sampleValues.length; i++) {
            sampleValues[i] = sample.get(trees[0].names[i]);
        }
        return sampleValues;
    }

    public float[] predictProba(Map<String, Float> sample) {
        float[] sampleVector = createSampleVector(sample);
        return predictProba(sampleVector);
    }

    public float[] predictProba(float[] sample) {
        int nClasses = trees[0].getNumberOfClasses();
        float[] distribution = new float[nClasses];
        for (DecisionTree tree : trees) {
            distribution = predictTree(sample, tree);
        }

        float[] predictions= new float[nClasses];
        if (nClasses > 1){
            double n = 0;
            for (float v : distribution) {
                n +=v;
            }


            for (int i = 0; i < predictions.length; i++) {
                predictions[i] = (float) (distribution[i]/n);
            }
        }
        else {
            predictions[0] = distribution[0];
        }
        return predictions;
    }

    /**
     * Predict the class of given sample by averaging over the responses of all trees and returning
     * the class (encoded as int) with the highest probability.
     *
     * @param sample the sample to predict the class of
     * @return return the predicted class
     */
    public int predict(Map<String, Float> sample){
        float[] prediction = predictProba(sample);
        return argmax(prediction);
    }


    public int predict(float[] sample){
        float[] prediction = predictProba(sample);
        return argmax(prediction);
    }

    public int predict(double[] sample){
        float[] floatSample = new float[sample.length];
        for (int i = 0; i < sample.length; i++) {
            floatSample[i] = (float) sample[i];
        }
        float[] prediction = predictProba(floatSample);
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
