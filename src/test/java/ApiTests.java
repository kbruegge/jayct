import hexmap.TelescopeArray;
import hexmap.TelescopeDefinition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test Array definitions.
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
}
