package flink;

import com.google.common.collect.Iterables;
import io.ImageReader;
import org.apache.flink.streaming.api.functions.source.ParallelSourceFunction;

import java.util.Iterator;
import java.util.List;

/**
 * Created by mackaiver on 29/09/17.
 */
public class InfiniteEventSource implements ParallelSourceFunction<ImageReader.Event>{

    List<ImageReader.Event> events = null;
    Iterator<ImageReader.Event> cycle = null;
    volatile boolean isRunning = true;

    final String inputFile;

    public InfiniteEventSource(String inputFile) {
        this.inputFile = inputFile;
    }

    @Override
        public void run(SourceContext<ImageReader.Event> ctx) throws Exception {
            if (events == null){
                events = ImageReader.fromPathString(inputFile).getListOfRandomEvents(500);
                cycle = Iterables.cycle(events).iterator();
            }
            long i = 0;
            while(cycle.hasNext() && isRunning) {
                ImageReader.Event event = cycle.next();
                event.eventId += i;
                ctx.collect(event);
                i++;
            }
        }

        @Override
        public void cancel() {
            isRunning = false;
        }

}
