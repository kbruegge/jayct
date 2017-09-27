//package prediction;
//
//import com.google.common.base.Splitter;
//import org.dmg.pmml.FieldName;
//import org.dmg.pmml.Model;
//import org.dmg.pmml.PMML;
//import org.jpmml.evaluator.*;
//import org.jpmml.model.ImportFilter;
//import org.jpmml.model.JAXBUtil;
//import org.junit.Before;
//import org.openjdk.jmh.annotations.Scope;
//import org.openjdk.jmh.annotations.Setup;
//import org.openjdk.jmh.annotations.State;
//import org.xml.sax.InputSource;
//import org.xml.sax.SAXException;
//import ml.TreeEnsemblePredictor;
//
//import javax.xml.bind.JAXBException;
//import javax.xml.transform.Source;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.*;
//
//import static java.util.stream.Collectors.toList;
//
//@State(Scope.Thread)
//public class BenchmarkPrediction {
//
//    TreeEnsemblePredictor predictor;
//    private List<double[]> rows;
//    Random r;
//
//    private ModelEvaluator<? extends Model> modelEvaluator;
//    private ArrayList<Map<FieldName, FieldValue>> pmmlRows;
//
//    @Before
//    @Setup
//    public void prepare(){
//        try {
//            r = new Random();
//
//            InputStream stream = BenchmarkPrediction.this.getClass().getResourceAsStream("/iris_rf.json");
//            predictor = new TreeEnsemblePredictor(stream);
//
//            rows = readCSV();
//
//            modelEvaluator = prepareEvaluator();
//
//            pmmlRows = prepareData(rows);
//
//            // see https://github.com/jpmml/jpmml-evaluator/issues/38
////            new PredicateOptimizer().applyTo(pmml);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private ModelEvaluator<? extends Model> prepareEvaluator(){
//        InputStream resourceAsStream = BenchmarkPrediction.this.getClass().getResourceAsStream("/iris_rf.pmml");
//
//        PMML pmml;
//        try (InputStreamReader isr = new InputStreamReader(resourceAsStream)) {
//            Source transformedSource = ImportFilter.apply(new InputSource(isr));
//            pmml = JAXBUtil.unmarshalPMML(transformedSource);
//        } catch (SAXException | IOException | JAXBException ex) {
//            ex.printStackTrace();
//            throw  new RuntimeException(ex);
//        }
//
//        ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
//        return modelEvaluatorFactory.newModelEvaluator(pmml);
//
//    }
//
//
//    private ArrayList<Map<FieldName, FieldValue>> prepareData(List<double[]> rows) {
//
//        ArrayList<Map<FieldName, FieldValue>> list = new ArrayList<>(151);
//
//        for (double[] row : rows){
//            Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
//            int i = 0;
//
//            for(InputField activeField : modelEvaluator.getActiveFields()){
//                FieldValue activeValue = activeField.prepare(row[i]);
//                arguments.put(activeField.getName(), activeValue);
//                i++;
//            }
//            list.add(arguments);
//        }
//
//        return list;
//    }
//
//    private List<double[]> readCSV() {
//        InputStream inputStream = BenchmarkPrediction.this.getClass().getResourceAsStream("/python_predictions_iris.csv");
//
//
//        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
//
//        return br.lines()
//                .skip(1)
//                .map(line -> {
//
//                    List<String> values = Splitter.onPattern(",").trimResults().splitToList(line);
//                    return values.stream().limit(4).mapToDouble(Float::parseFloat).toArray();
//                })
//                .collect(toList());
//    }
//
//
//
////    @org.openjdk.jmh.annotations.Benchmark
////    public int benchmarkPredictionLoop() {
////        int prediction = 0;
////        for (double[] row : rows) {
////           prediction = predictor.predict(row);
////        }
////        return prediction;
////    }
//
//    @org.openjdk.jmh.annotations.Benchmark
//    public int benchmarkPredictionRandomRow() {
//        int index = r.nextInt(rows.size());
//        return predictor.predict(rows.get(index));
//    }
//
//    @org.openjdk.jmh.annotations.Benchmark
//    public int benchmarkPredictionRandomRowPMML() {
//        Map<FieldName, ?> results= modelEvaluator.evaluate(pmmlRows.get(r.nextInt(150)));
//
//        ProbabilityDistribution label = (ProbabilityDistribution) results.get(modelEvaluator.getTargetField().getName());
//        return (int) label.getResult();
//
//    }
//}