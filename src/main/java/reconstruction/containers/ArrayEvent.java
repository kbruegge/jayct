package reconstruction.containers;

import hexmap.TelescopeDefinition;
import io.ImageReader;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.List;

import static hexmap.TelescopeArray.cta;

public class ArrayEvent{
    public final int numTriggeredLST;
    public final int numTriggeredMST;
    public final int numTriggeredSST;

    public final double totalIntensity;

    public ArrayEvent(ImageReader.Event event, List<Moments> moments){
        numTriggeredLST = countType(event, TelescopeDefinition.TelescopeType.LST);
        numTriggeredMST = countType(event, TelescopeDefinition.TelescopeType.MST);
        numTriggeredSST = countType(event, TelescopeDefinition.TelescopeType.SST);

        totalIntensity = totalIntensity(moments);
    }

    public int countType(ImageReader.Event event, TelescopeDefinition.TelescopeType type){
        int counter = 0;
        for (int telescope : event.array.triggeredTelescopes) {
            if (cta().telescopeFromId(telescope).telescopeType == type) {
                counter++;
            }
        }
        return counter;
    }

    public double totalIntensity(List<Moments> moments){
        return moments.stream().mapToDouble(a -> a.intensity).sum();
    }
}
