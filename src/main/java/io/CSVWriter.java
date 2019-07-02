package io;

import com.google.common.base.Joiner;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;


/**
 * Writes results to a csv file.
 */
public class CSVWriter implements Serializable{
    private PrintWriter writer;
    private String separator = ",";

    /**
     * Create a new CSVWriter for the given File.
     * @param file the file to create.
     * @throws IOException in case the file can not be written to.
     */
    public CSVWriter(File file) throws IOException {
        writer = new PrintWriter(file);
    }

    public void append(ReconstrucedEvent e, double classPrediction, double energyPrediction, double alt, double az, double trueEnergy) throws IOException {

        String s = Joiner.on(separator).join(
                    e.eventID,
                    e.altAz[0],
                    e.altAz[1],
                    e.impactPosition.getX(),
                    e.impactPosition.getY(),
                    classPrediction,
                    energyPrediction,
                    alt,
                    az,
                    trueEnergy
        );
        writer.println(s);
        writer.flush();
    }

    /**
     * Write the given strings as a row to the CSV file. usefull for writing the header.
     * @param headerKeywords the strings to write into the row.
     */
    public void writeHeader(String... headerKeywords) {
        String h = Joiner.on(separator).join(headerKeywords);
        writer.println(h);
        writer.flush();
    }

    public void append(Moments m, double trueEnergy) {
        String s = Joiner.on(separator).join(
                m.eventID,
                m.telescopeID,
                m.length,
                m.width,
                m.kurtosis,
                m.skewness,
                m.size,
                m.numberOfPixel,
                m.r,
                m.delta,
                trueEnergy
        );
        writer.println(s);
        writer.flush();
    }
}
