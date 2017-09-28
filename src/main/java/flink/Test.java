package flink;

import com.google.common.collect.Iterables;
import hexmap.TelescopeArray;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.ParallelSourceFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Testing the apache flink framework
 * Created by mackaiver on 25/09/17.
 */
public class Test {


    public static void main (String[] args) throws Exception {

        if (args.length != 2){
            System.out.println("You must specify a path to an input file and to a classifier");
            System.out.println("Usage:  java - jar jayct.jar <path to images> <path to classifier>");
            return;
        }

        System.out.println("Reading data from file: " + args[0] );
        System.out.println("Reading classifier from file: " + args[1] );


        TreeEnsemblePredictor predictor = new TreeEnsemblePredictor(Paths.get(args[1]));

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<ImageReader.Event> source = env.addSource(new ParallelSourceFunction<ImageReader.Event>() {

            List<ImageReader.Event> events = null;
            Iterator<ImageReader.Event> cycle = null;

            volatile boolean isRunning = true;

            @Override
            public void run(SourceContext<ImageReader.Event> ctx) throws Exception {
                if (events == null){
                    events = ImageReader.fromPathString(args[0]).getListOfRandomEvents(100);
                    cycle = Iterables.cycle(events).iterator();
                }
                while(cycle.hasNext() && isRunning) {
                    ctx.collect(cycle.next());
                }
            }

            @Override
            public void cancel() {
                isRunning = false;
            }
        });


        source
            .setParallelism(8)
            .flatMap(new FlatMapFunction<ImageReader.Event, Tuple2<ShowerImage, Integer>>() {

                @Override
                public void flatMap(ImageReader.Event event, Collector<Tuple2<ShowerImage, Integer>> out) throws Exception {

                    List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
                    int numberOfTelescopes = event.array.numTriggeredTelescopes;
                    showerImages.forEach(i -> out.collect(Tuple2.of(i, numberOfTelescopes)));
                }

            })
            .map(new MapFunction<Tuple2<ShowerImage, Integer>, Tuple2<Moments, Integer>>() {

                @Override
                public Tuple2<Moments, Integer> map( Tuple2<ShowerImage, Integer> value) throws Exception {
                    Moments moments = HillasParametrization.fromShowerImage(value.f0);
                    return  Tuple2.of(moments, value.f1);
                }
            })
            .map(new MapFunction<Tuple2<Moments,Integer>, Tuple2<Moments, Double>>() {
                @Override
                public Tuple2<Moments, Double> map(Tuple2<Moments, Integer> value) throws Exception {

                    Moments m = value.f0;
                    float[] vector = new Vectorizer().of(
                            value.f1,
                            m.numberOfPixel,
                            m.width,
                            m.length,
                            m.skewness,
                            m.kurtosis,
                            m.phi,
                            m.miss,
                            m.size,
                            TelescopeArray.cta().telescopeFromId(m.telescopeID).telescopeType.ordinal()
                    ).createFloatVector();

                    float p = predictor.predictProba(vector)[0];
                    return Tuple2.of(m, (double)p);
                }
            })
            .keyBy(new KeySelector<Tuple2<Moments,Double>, Long>() {
                @Override
                public Long getKey(Tuple2<Moments, Double> value) throws Exception {
                    return value.f0.eventID;
                }
            })
            .timeWindow(Time.seconds(10))
            .aggregate(new AggregateFunction<Tuple2<Moments,Double>, ArrayList<Tuple2<Moments,Double>>, Tuple2<ReconstrucedEvent, Double>>() {
                @Override
                public ArrayList<Tuple2<Moments, Double>> createAccumulator() {
                    return new ArrayList<>();
                }

                @Override
                public void add(Tuple2<Moments, Double> value, ArrayList<Tuple2<Moments, Double>>  accumulator) {
                    accumulator.add(value);
                }

                @Override
                public Tuple2<ReconstrucedEvent, Double> getResult(ArrayList<Tuple2<Moments, Double>>  accumulator) {
                    ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(accumulator.stream().map(v -> v.f0).collect(Collectors.toList()), 1, 2);
                    double avg = accumulator.stream().mapToDouble(v -> v.f1).average().orElse(0);

                    return Tuple2.of(reconstrucedEvent, avg);
                }

                @Override
                public ArrayList<Tuple2<Moments, Double>>  merge(ArrayList<Tuple2<Moments, Double>>  a, ArrayList<Tuple2<Moments, Double>>  b) {
                    ArrayList<Tuple2<Moments, Double>> c = new ArrayList<>();
                    c.addAll(a);
                    c.addAll(b);
                    return c;
                }
            })
            .startNewChain()
            .writeAsCsv("./output.csv", FileSystem.WriteMode.OVERWRITE)
            .setParallelism(1);

        System.out.println(env.getExecutionPlan());

        env.execute("Lecker CTA auf Flink");


    }
}
