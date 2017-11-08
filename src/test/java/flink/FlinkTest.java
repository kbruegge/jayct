package flink;

import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.junit.Test;

import java.net.URL;

public class FlinkTest {
    public class DummyContext<Event> implements SourceFunction.SourceContext<Event> {


        @Override
        public void collect(Event element) {

        }

        @Override
        public void collectWithTimestamp(Event element, long timestamp) {

        }

        @Override
        public void emitWatermark(Watermark mark) {

        }

        @Override
        public void markAsTemporarilyIdle() {

        }

        @Override
        public Object getCheckpointLock() {
            return null;
        }

        @Override
        public void close() {

        }
    }

    @Test
    public void sourceTest() throws Exception {
        URL resource = InfinteEventSource.class.getResource("/images.json.gz");
        new InfinteEventSource(resource.getFile(), 5).run(new DummyContext<>());
    }
}
