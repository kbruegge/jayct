package flink;

import com.google.common.collect.Iterables;
import io.CSVWriter;
import io.ImageReader;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.ParallelSourceFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Testing the apache flink framework
 * Created by mackaiver on 25/09/17.
 */
public class Test {

    private static URL url = ImageReader.class.getResource("/images.json.gz");

    public static void main (String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<ImageReader.Event> source = env.addSource(new ParallelSourceFunction<ImageReader.Event>() {

            ImageReader reader = null;
            Iterator<ImageReader.Event> cycle = null;
            volatile boolean isRunning = true;

            @Override
            public void run(SourceContext<ImageReader.Event> ctx) throws Exception {
                if (reader == null){
                    reader = ImageReader.fromPathString("./src/main/resources/images.json.gz");
                    cycle = Iterables.cycle(reader).iterator();
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
            .setParallelism(2)
            .flatMap(new FlatMapFunction<ImageReader.Event, ShowerImage>() {

                    @Override
                    public void flatMap(ImageReader.Event value, Collector<ShowerImage> out) throws Exception {
                        List<ShowerImage> showerImages = TailCut.onImagesInEvent(value);
                        showerImages.forEach(out::collect);
                    }

                })
            .map(new MapFunction<ShowerImage, Moments>() {
                    @Override
                    public Moments map(ShowerImage value) throws Exception {
                        return HillasParametrization.fromShowerImage(value);
                    }
                })
            .keyBy(new KeySelector<Moments, Long>() {
                    @Override
                    public Long getKey(Moments value) throws Exception {
                        return value.eventId;
                    }
                })
            .timeWindow(Time.seconds(10))
            .aggregate(new AggregateFunction<Moments, ArrayList<Moments>, ReconstrucedEvent>() {

                @Override
                public ArrayList<Moments> createAccumulator() {
                    return new ArrayList<>();
                }

                @Override
                public void add(Moments value, ArrayList<Moments> accumulator) {
                    accumulator.add(value);
                }

                @Override
                public ReconstrucedEvent getResult(ArrayList<Moments> accumulator) {
                    return DirectionReconstruction.fromMoments(accumulator, 2, 1);
                }

                @Override
                public ArrayList<Moments> merge(ArrayList<Moments> a, ArrayList<Moments> b) {
                    a.addAll(b);
                    return a;
                }
            })
            .startNewChain()
            .addSink(new SinkFunction<ReconstrucedEvent>() {
                CSVWriter w = null;
                @Override
                public void invoke(ReconstrucedEvent value) throws Exception {
                    if (w == null){
                        w = new CSVWriter(new File("/Users/mackaiver/test_bla.csv"));
                    }
                    w.appendReconstructedEvent(value);
                }
            })
            .setParallelism(1);

//            .writeUsingOutputFormat(new FileOutputFormat<ReconstrucedEvent>() {
//
//                CSVWriter w = null;
//
//                @Override
//                public void writeRecord(ReconstrucedEvent record) throws IOException {
//                    if (w == null){
//                        w = new CSVWriter(new File("/Users/mackaiver/test_bla.csv"));
//                    }
//                    w.appendReconstructedEvent(record);
//                }
//            });



//        .groupBy(new KeySelector<Moments, Long>() {
//            @Override
//            public Long getKey(Moments value) throws Exception {
//                return value.eventId;
//            }
//        }).combineGroup(new GroupCombineFunction<Moments, ReconstrucedEvent>() {
//            @Override
//            public void combine(Iterable<Moments> values, Collector<ReconstrucedEvent> out) throws Exception {
//                DirectionReconstruction.fromMoments(values, 0, 1);
//            }
//        })


        env.execute("Lecker CTA auf Flink");

    }
}
