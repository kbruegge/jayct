/**
 * Test the output modules
 * Created by mackaiver on 25/09/17.
 */
public class WriterTest {


//    @Test
//    public void testEventWriter() throws IOException {
//        URL url = ImageReader.class.getResource("/images.json.gz");
//
//        ImageReader events = ImageReader.fromURL(url);
//
//        File f = new TemporaryFolder().newFile();
//
//        CSVWriter writer = new CSVWriter(f);
//        writer.writeHeader("id","x", "y", "z", "impact_x", "impact_y");
//        for (ImageReader.Event event : events) {
//            List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
//            List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);
//
//            ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);
//
//            if (reconstrucedEvent.direction.isNaN()){
//                continue;
//            }
//            writer.appendReconstructedEvent(reconstrucedEvent);
//        }
//
//    }
}
