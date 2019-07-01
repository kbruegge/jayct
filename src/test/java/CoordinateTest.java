import coordinates.CameraCoordinate;
import coordinates.HorizontalCoordinate;
import hexmap.TelescopeDefinition;
import io.ImageReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URL;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by mackaiver on 04.12.17.
 */
public class CoordinateTest {

    @Rule
    public TemporaryFolder tempFolder= new TemporaryFolder();


    @Test
    public void testCoordinate() {

        HorizontalCoordinate pointing = HorizontalCoordinate.fromDeg(20, 180);



        double pX = 0;
        double pY = 0;

        CameraCoordinate cc = new CameraCoordinate(pX, pY);
        HorizontalCoordinate result = cc.toHorizontal(pointing, 16);
        System.out.println(result);
        assertEquals(20, result.getZenithDeg(), 0.01);
        assertEquals(180, result.getAzimuthDeg(),  0.01 );
//


        pY = 0.0;
        pX = 0.1;
        cc = new CameraCoordinate(pX, pY);
        result = cc.toHorizontal(pointing, 16);
        System.out.println(result);
        assertEquals(19.641, result.getZenithDeg(), 0.01);
        assertEquals(180, result.getAzimuthDeg(),  0.01 );


        pY = 0.0;
        pX = 0.5;
        cc = new CameraCoordinate(pX, pY);
        result = cc.toHorizontal(pointing, 16);
        System.out.println(result);
        assertEquals(18.21, result.getZenithDeg(), 0.01);
        assertEquals(180, result.getAzimuthDeg(),  0.01 );

        pY = 0.5;
        pX = 0.0;
        cc = new CameraCoordinate(pX, pY);
        result = cc.toHorizontal(pointing, 16);
        System.out.println(result);
        assertEquals(20.0766, result.getZenithDeg(), 0.01);
        assertEquals(185.22, result.getAzimuthDeg(),  0.01 );

        pY = -0.5;
        pX = 0.0;
        cc = new CameraCoordinate(pX, pY);
        result = cc.toHorizontal(pointing, 16);
        System.out.println(result);
        assertEquals(20.0766, result.getZenithDeg(), 0.01);
        assertEquals(174.779, result.getAzimuthDeg(),  0.01 );

        pY = -0.5;
        pX = 0.5;
        cc = new CameraCoordinate(pX, pY);
        result = cc.toHorizontal(pointing, 16);
        System.out.println(result);
        assertEquals(18.294, result.getZenithDeg(), 0.01);
        assertEquals(174.29, result.getAzimuthDeg(),  0.01 );
    }
}
