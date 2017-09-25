package io;

import com.google.common.base.Joiner;
import reconstruction.containers.ReconstrucedEvent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;


/**
 * A class to write results to a jsonl file.
 * Created by mackaiver on 25/09/17.
 */
public class CSVWriter implements Serializable{
    private PrintWriter writer;
    private String seperator = ",";

    public CSVWriter(File file) throws IOException {
        writer = new PrintWriter(file);
    }

    public void appendReconstructedEvent(ReconstrucedEvent e) throws IOException {

        String s = Joiner.on(seperator).join(
                    e.eventID,
                    e.direction.getX(),
                    e.direction.getY(),
                    e.direction.getZ(),
                    e.impactPosition.getX(),
                    e.impactPosition.getY()
                );
        writer.println(s);
        writer.flush();
    }

    public void appendReconstructedEvents(List<ReconstrucedEvent> events) throws IOException {
        events.forEach( e -> {
            String s = Joiner.on(seperator).join(
                    e.eventID,
                    e.direction.getX(),
                    e.direction.getY(),
                    e.direction.getZ(),
                    e.impactPosition.getX(),
                    e.impactPosition.getY()
            );
            writer.println(s);
        });
        writer.flush();
    }

    public void writeHeader(String... headerKeywords) {
        String h = Joiner.on(seperator).join(headerKeywords);
        writer.println(h);
        writer.flush();
    }
}
