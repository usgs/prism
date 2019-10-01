/*******************************************************************************
 * Name: Java class FFTinteDiffTest.java
 * Project: PRISM strong motion record processing using COSMOS data format
 * Written by: Jeanne Jones, USGS, jmjones@usgs.gov
 * 
 * This software is in the public domain because it contains materials that 
 * originally came from the United States Geological Survey, an agency of the 
 * United States Department of Interior. For more information, see the official 
 * USGS copyright policy at 
 * https://www.usgs.gov/information-policies-and-instructions/copyrights-and-credits#copyright
 * 
 * This software has been approved for release by the U.S. Geological Survey (USGS). 
 * Although the software has been subjected to rigorous review, the USGS reserves 
 * the right to update the software as needed pursuant to further analysis and 
 * review. No warranty, expressed or implied, is made by the USGS or the U.S. 
 * Government as to the functionality of the software and related material nor 
 * shall the fact of release constitute any such warranty. Furthermore, the 
 * software is released on condition that neither the USGS nor the U.S. Government 
 * shall be held liable for any damages resulting from its authorized or unauthorized use.
 * 
 * Version 1.0 release: Feb. 2015
 * Version 2.0 release: Oct. 2019
 ******************************************************************************/
package PRISMtest.Package;

import SmProcessing.FFTinteDiff;
import SmUtilities.TextFileReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmjones
 */
public class FFTinteDiffTest {
    double deltat = 0.005;
    double delta100 = 0.01;
    double STEP = 1.0;
    static double[] acc;
    static double[] vel;
    static double EPSILON = 0.1;
    double SM_EPSILON = 0.1;
    static String[] filecontents;
    static String[] fileformat;
    
    static String accel = "/PRISMtest/Data/acceleration.txt";
    static String veloc = "/PRISMtest/Data/velocity.txt";
    
    public FFTinteDiffTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException, URISyntaxException {
        File name;
        TextFileReader infile;
        int next;
        next = 0;
        URL url = DecimationTest.class.getResource( accel );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            acc = new double[filecontents.length];
            for (String num : filecontents) {
                acc[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = DecimationTest.class.getResource( veloc );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            vel = new double[filecontents.length];
            for (String num : filecontents) {
                vel[next++] = Double.parseDouble(num);
            }
        }
    }
     @Test
     public void testIntegrateDifferentiate() throws IOException {
         double[] test = new double[0];
         double[] test1 = new double[0];
         double[] test2 = null;
         FFTinteDiff fftid = new FFTinteDiff();
         org.junit.Assert.assertArrayEquals(test1, fftid.differentiate(test, STEP,0), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, fftid.differentiate(test2, STEP,0), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, fftid.differentiate(test2, 0.0,0), EPSILON);
         
         org.junit.Assert.assertArrayEquals(test1, fftid.integrate(test, STEP), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, fftid.integrate(test2, STEP), EPSILON);
         org.junit.Assert.assertArrayEquals(test1, fftid.integrate(test2, 0.0), EPSILON);
         
         org.junit.Assert.assertArrayEquals(vel, fftid.integrate(acc, deltat),  SM_EPSILON);
         org.junit.Assert.assertArrayEquals(acc, fftid.differentiate(vel, deltat, 6000),  SM_EPSILON);
     }
    
}
