package io;

import com.google.common.base.Joiner;
import reconstruction.containers.ArrayEvent;
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
    private String seperator = ",";

    private boolean headerWritten = false;

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
        if(!headerWritten){
            writeHeader("id", "direction_x", "direction_y", "direction_z", "position_x", "position_z", "prediction");
            headerWritten = true;
        }
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
     * @param mc the montecarlo information for a given run
     * @throws IOException in case the file cannot not be written to.
     */
    public void append(ImageReader.MC mc) throws IOException {
        if(!headerWritten){
            writeHeader(
                    "run_id",
                    "mc_max_energy",
                    "mc_min_energy",
                    "mc_spectral_index",
                    "mc_max_altitude",
                    "mc_min_altitude",
                    "mc_max_azimuth",
                    "mc_min_azimuth",
                    "mc_max_scatter_range",
                    "mc_min_scatter_range",
                    "mc_max_viewcone_radius",
                    "mc_min_viewcone_radius",
                    "mc_num_showers",
                    "mc_num_reuse"
                    );
            headerWritten = true;
        }
        String s = Joiner.on(seperator).join(
                mc.runId,
                mc.mcMaxEnergy,
                mc.mcMinEnergy,
                mc.mcSpectralIndex,
                mc.mcMaxAltitude,
                mc.mcMinAltitude,
                mc.mcMaxAzimuth,
                mc.mcMinAzimuth,
                mc.mcMaxScatterRange,
                mc.mcMinScatterRange,
                mc.mcMaxViewconeRadius,
                mc.mcMinViewconeRadius,
                mc.mcNumShowers,
                mc.mcNumReuse
        );
        writer.println(s);
        writer.flush();
    }


    /**
     * Appends a row to the CSV file. Containing the values for
     *
     *    e.eventID,
     *    mc.energy
     *    mc.impact
     *    e.direction.getX(),
     *    e.direction.getY(),
     *    e.direction.getZ(),
     *    e.impactPosition.getX(),
     *    e.impactPosition.getY(),
     *    classPrediction
     *
     * @param event the event object returned from the ImageReader
     * @param reconstrucedEvent the reconstructed event object
     * @param classPrediction the prediction (aka. gammaness)
     * @throws IOException in case the file cannot not be written to.
     */
    public void append(ImageReader.Event event, ReconstrucedEvent reconstrucedEvent, double classPrediction) throws IOException {
        if(!headerWritten){
            writeHeader("array_event_id", "run_id",  "mc_alt", "mc_az", "mc_energy", "mc_core_x", "mc_core_y",  "alt_prediction", "az_prediction", "core_x_prediction", "core_y_prediction", "num_triggered_telescopes", "rta_gamma_prediction");
            headerWritten = true;
        }
        String s = Joiner.on(seperator).join(
                event.eventId,
                event.runId,
                event.mc.mcAlt,
                event.mc.mcAz,
                event.mc.mcEnergy,
                event.mc.mcCoreX,
                event.mc.mcCoreY,
                reconstrucedEvent.alt,
                reconstrucedEvent.az,
                reconstrucedEvent.impactPosition.getX(),
                reconstrucedEvent.impactPosition.getY(),
                event.array.numTriggeredTelescopes,
                classPrediction
        );
        writer.println(s);
        writer.flush();
    }


    public void append(ImageReader.Event event, ReconstrucedEvent reconstrucedEvent, ArrayEvent arrayEvent) throws IOException {
        if(!headerWritten){
            writeHeader(
                    "array_event_id",
                    "run_id",
                    "mc_alt", "mc_az",
                    "mc_energy",
                    "mc_core_x", "mc_core_y",
                    "alt_prediction", "az_prediction",
                    "core_x_prediction", "core_y_prediction",
                    "num_triggered_telescopes",
                    "num_triggered_lst", "num_triggered_mst", "num_triggered_sst",
                    "total_intensity"
            );
            headerWritten = true;
        }
        String s = Joiner.on(seperator).join(
                event.eventId,
                event.runId,
                event.mc.mcAlt,
                event.mc.mcAz,
                event.mc.mcEnergy,
                event.mc.mcCoreX,
                event.mc.mcCoreY,
                reconstrucedEvent.alt,
                reconstrucedEvent.az,
                reconstrucedEvent.impactPosition.getX(),
                reconstrucedEvent.impactPosition.getY(),
                event.array.numTriggeredTelescopes,
                arrayEvent.numTriggeredLST,
                arrayEvent.numTriggeredMST,
                arrayEvent.numTriggeredSST,
                arrayEvent.totalIntensity
        );
        writer.println(s);
        writer.flush();
    }



    public void append(Moments m, double distance, long runId, String telescopeTypeName) throws IOException {
        if(!headerWritten){
            writeHeader(
                    "array_event_id",
                    "run_id",
                    "camera_id",
                    "intensity",
                    "kurtosis",
                    "skewness",
                    "length",
                    "width",
                    "phi",
                    "r",
                    "x",
                    "y",
                    "miss",
                    "numberOfPixel",
                    "distance_to_core",
                    "telescope_type_name"
            );
            headerWritten = true;
        }
        String s = Joiner.on(seperator).join(
                m.eventID,
                runId,
                m.cameraID,
                m.intensity,
                m.kurtosis,
                m.skewness,
                m.length,
                m.width,
                m.phi,
                m.r,
                m.meanX,
                m.meanY,
                m.miss,
                m.numberOfPixel,
                distance,
                telescopeTypeName
        );
        writer.println(s);
        writer.flush();
    }

    /**
     * Write the given strings as a row to the CSV file. useful for writing the header.
     * @param headerKeywords the strings to write into the row.
     */
    public void writeHeader(String... headerKeywords) {
        String h = Joiner.on(seperator).join(headerKeywords);
        writer.println(h);
        writer.flush();
    }
}
