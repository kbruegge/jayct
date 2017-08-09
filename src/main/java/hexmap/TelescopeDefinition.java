package hexmap;


/**
 * Contains all the information needed for a single telescope.
 * Name of the telescope, Position, Optical properties etc..
 *
 * Could easily be extended to include more dynamic properties like
 * Created by kbruegge on 2/13/17.
 */
public class TelescopeDefinition {

    /**
     * The optical focal length of the telescope in meter
     */
    public final double opticalFocalLength;

    /**
     * The name of the camera as a {@link String}. Might be unique to this specific telescope or to a telescope type.
     */
    public final String cameraName;


    /**
     * The {@link TelescopeType} of this telescope.
     */
    public final TelescopeType telescopeType;

    /**
     * The X position of this telescope in Meter.
     */
    public final double telescopePositionX;

    /**
     * The Y position of this telescope in Meter.
     */
    public final double telescopePositionY;

    /**
     * The Z position of this telescope in Meter.
     */
    public final double telescopePositionZ;

    public TelescopeDefinition(double opticalFocalLength,
                               String cameraName,
                               TelescopeType telescopeType,
                               double telescopePositionX,
                               double telescopePositionY,
                               double telescopePositionZ
    ) {
        this.opticalFocalLength = opticalFocalLength;
        this.cameraName = cameraName;
        this.telescopeType = telescopeType;
        this.telescopePositionX = telescopePositionX;
        this.telescopePositionY = telescopePositionY;
        this.telescopePositionZ = telescopePositionZ;
    }


    /**
     * Enum of telescope types.
     * For CTA we currently have three telescope types. SST, MST, and LST
     */
    public enum TelescopeType {
        SST("SST"),
        MST("MST"),
        LST("LST");

        String type;

        TelescopeType(String type) {
            this.type = type;
        }
    }
}
