package flink;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import io.ImageReader;
import org.apache.flink.streaming.api.functions.source.ParallelSourceFunction;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An event source which produces data for the specified amount fo seconds.
 */
public class InfinteEventSource implements ParallelSourceFunction<ImageReader.Event>{

    List<ImageReader.Event> events = null;
    Iterator<ImageReader.Event> cycle = null;
    volatile boolean isRunning = true;

    final String inputFile;
    private final int numberOfSecondsToStream;

    public InfinteEventSource(String inputFile, int numberOfSecondsToStream) {
        this.inputFile = inputFile;
        this.numberOfSecondsToStream = numberOfSecondsToStream;
    }

    @Override
        public void run(SourceContext<ImageReader.Event> ctx) throws Exception {
            if (events == null){
                events = ImageReader.fromPathString(inputFile).getListOfRandomEvents(100);
                cycle = Iterables.cycle(events).iterator();
            }
            Stopwatch stopwatch = Stopwatch.createStarted();
            long i = 0;
            while(cycle.hasNext() && isRunning) {
                ImageReader.Event event = cycle.next();
                event.eventId += i;
                ctx.collect(event);
                i++;
                if (stopwatch.elapsed(TimeUnit.SECONDS) > numberOfSecondsToStream){
                    isRunning = false;
                }
            }
        }

        @Override
        public void cancel() {
            isRunning = false;
        }
}
