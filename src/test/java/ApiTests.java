import hexmap.TelescopeArray;
import hexmap.TelescopeDefinition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by mackaiver on 09/08/17.
 */
public class ApiTests {

    @Test
    public void testArrayLST(){
        TelescopeArray cta = TelescopeArray.cta();

        TelescopeDefinition lst = cta.telescopeFromId(1);
        assertEquals(TelescopeDefinition.TelescopeType.LST, lst.telescopeType);
        assertEquals(28.0, lst.opticalFocalLength, 0.0);
    }


    @Test
    public void testArrayMST(){
        TelescopeArray cta = TelescopeArray.cta();

        TelescopeDefinition mst = cta.telescopeFromId(10);
        assertEquals(TelescopeDefinition.TelescopeType.MST, mst.telescopeType);
        assertEquals(16.0, mst.opticalFocalLength, 0.0);
    }

    @Test
    public void testArraySST(){
        TelescopeArray cta = TelescopeArray.cta();

        TelescopeDefinition sst = cta.telescopeFromId(60);
        assertEquals(TelescopeDefinition.TelescopeType.SST, sst.telescopeType);
        assertEquals(2.28, sst.opticalFocalLength, 0.01);
    }
}
