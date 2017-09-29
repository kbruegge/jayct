package flink;

import io.ImageReader;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by mackaiver on 29/09/17.
 */
public class FolderEventSource extends RichParallelSourceFunction<ImageReader.Event> {

    volatile boolean isRunning = true;

    final String inputFolder;

    public FolderEventSource(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    @Override
    public void run(SourceContext<ImageReader.Event> ctx) throws Exception {
        getRuntimeContext().getIndexOfThisSubtask();
        Files.list(Paths.get(inputFolder))
                .filter(f -> f.getFileName().toString().endsWith("json.gz"))
                .forEach( f -> {
                    if (!isRunning){
                        return;
                    }
                    try {
                        ImageReader events = ImageReader.fromPath(f);
                        events.forEach(ctx::collect);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void run(SourceContext<ImageReader.Event> ctx) throws Exception {
        ctx.
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

}
