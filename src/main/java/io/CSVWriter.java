package io;

import com.google.common.base.Joiner;
import reconstruction.containers.ReconstrucedEvent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;


/**
 * Writes results to a csv file.
 */
public class CSVWriter implements Serializable{
    private PrintWriter writer;
    private String seperator = ",";

    /**
     * Create a new CSVWriter for the given File.
     * @param file the file to create.
     * @throws IOException in case the file can not be written to.
     */
    public CSVWriter(File file) throws IOException {
        writer = new PrintWriter(file);
    }

    /**
     * Appends a row to the CSV file. Containing the values for
     *
     *    e.eventID,
     *    e.direction.getX(),
     *    e.direction.getY(),
     *    e.direction.getZ(),
     *    e.impactPosition.getX(),
     *    e.impactPosition.getY(),
     *    classPrediction
     *
     *
     * @param e the reconstructed event object
     * @param classPrediction the prediction (aka. gammaness)
     * @throws IOException in case the file cannot not be written to.
     */
    public void append(ReconstrucedEvent e, double classPrediction) throws IOException {

        String s = Joiner.on(seperator).join(
                    e.eventID,
                    e.direction.getX(),
                    e.direction.getY(),
                    e.direction.getZ(),
                    e.impactPosition.getX(),
                    e.impactPosition.getY(),
                    classPrediction
                );
        writer.println(s);
        writer.flush();
    }

    /**
     * Write the given strings as a row to the CSV file. usefull for writing the header.
     * @param headerKeywords the strings to write into the row.
     */
    public void writeHeader(String... headerKeywords) {
        String h = Joiner.on(seperator).join(headerKeywords);
        writer.println(h);
        writer.flush();
    }
}
