package io;

import com.google.common.base.MoreObjects;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

/**
 * Opens cta events stored in JSON files (or json.gz)
 * Uses GSON to map the events stored in JSON format Maps Java classes.
 * Created by mackaiver on 09/08/17.
 */
public class ImageReader implements Iterable<ImageReader.Event>, Iterator<ImageReader.Event> {

    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private JsonReader reader;

    @Override
    public Iterator<Event> iterator() {
        return this;
    }

    @Override
    public void forEach(Consumer<? super Event> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<Event> spliterator() {
        return Iterable.super.spliterator();
    }

    @Override
    public boolean hasNext() {
        //check whether the end of the file has been reached
        if (reader != null) {
            JsonToken token;
            try {
                token = reader.peek();
                return token != JsonToken.END_ARRAY;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * One CTA event contains MC information, Array information and of course the images
     * for each camera.
     * The classes below mirror the structure of the JSON file which contains the CTA events.
     * By using this intermediate class structure we can simplify the reading of the json to one
     * single line. Because GSON is pretty nice.
     */
    public class Event{
        public Map<Integer, double[]> images;
        public MC mc;
        public Array array;
        public long eventId;
        public String timestamp;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("ID", eventId)
                    .add("Num Triggered Tels", images.size())
                    .add("MC", mc)
                    .add("timestamp", timestamp)
                    .toString();
        }
    }
    /**
     * The Monte-Carlo information in the data contains the true values for direction and energy.
     * Saving the type of the primary particle might also be useful.
     */
    public class MC {
        public double energy, alt, az, coreY, coreX;
        public String type;

        @Override
        public String toString() {
            return  MoreObjects.toStringHelper(this)
                    .add("type", type)
                    .add("energy", energy)
                    .add("alt", alt)
                    .add("az", az)
                    .add("impact x", coreX)
                    .add("impact y", coreY)
                    .toString();
        }
    }
    /**
     * Information about the event which is not specific to one single camera but to
     * the whole array at once. At some point this should include a Timestamp I suppose.
     * The CTA monte-carlo does not have unique ids or timestamps from what I can see.
     */
    public class Array {
        public int[] triggeredTelescopes;
        public int numTriggeredTelescopes;
    }

    /**
     * Creates an ImageReader from a {@link Path} object pointing to a file somewhere in the filesystem
     * @param path the path to open
     * @return the imagereader
     * @throws IOException in case the file cannot be accessed/read
     */
    public static ImageReader fromPath(Path path) throws IOException {
        return new ImageReader(Files.newInputStream(path));
    }


    /**
     * Creates an ImageReader from a string encoding a path somewhere on the filesystem
     * @param path the path to open
     * @return the imagereader
     * @throws IOException in case the file cannot be accessed/read
     */
    public static ImageReader fromPathString(String path) throws IOException {
        return new ImageReader(Files.newInputStream(Paths.get(path)));
    }

    /**
     * Creates an ImageReader from an URL
     * @param url the url to open
     * @return the imagereader
     * @throws IOException in case the url cannot be accessed/read
     */
    public static ImageReader fromURL(URL url) throws IOException {
        return new ImageReader(url.openStream());
    }

    public Stream<Event> stream(){
        return StreamSupport.stream(this.spliterator(), false);
    }


    ImageReader(InputStream inputStream) throws IOException {
        //see https://stackoverflow.com/questions/4818468/how-to-check-if-inputstream-is-gzipped
        PushbackInputStream pb = new PushbackInputStream(inputStream, 2 ); //we need a pushbackstream to look ahead

        byte [] signature = new byte[2];//read the signature
        int len  = pb.read( signature );
        pb.unread( signature, 0, len); //push back the signature to the stream

        //check if matches standard gzip magic number
        // see https://en.wikipedia.org/wiki/Gzip#File_format
        if( signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b ) {
            inputStream = new GZIPInputStream(pb);
        }

        InputStreamReader streamReader = new InputStreamReader(inputStream, "UTF-8");

        reader = new JsonReader(streamReader);
        reader.beginArray();
    }

    @Override
    public Event next() {
        return gson.fromJson(reader, Event.class);
    }



}
