/*******************************************************************************
 * Name: Java class ResamplingTest.java
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

import SmException.SmException;
import SmProcessing.Resampling;
import SmUtilities.TextFileReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jmjones
 */
public class ResamplingTest {

    static String data = "/PRISMtest/Data/resample_in.txt";
    static String results = "/PRISMtest/Data/resample_out.txt";
    static double[] yarray;
    static double[] yparray;
    static String[] filecontents;
    static int SPS = 100;
    static double EPSILON = 0.000001;
    
    public ResamplingTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException, URISyntaxException {
        TextFileReader infile;
        int next = 0;
        File name;
        
        URL url = EventOnsetDetectionTest.class.getResource(data);
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            yarray = new double[filecontents.length];
            for (String num : filecontents) {
                yarray[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = EventOnsetDetectionTest.class.getResource(results);
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            yparray = new double[filecontents.length];
            for (String num : filecontents) {
                yparray[next++] = Double.parseDouble(num);
            }
        }
    }

    @Test
    public void testNeedsResampling() {
        Resampling resamp = new Resampling(200);
        org.junit.Assert.assertEquals(true,resamp.needsResampling(100));
        org.junit.Assert.assertEquals(false,resamp.needsResampling(200));
    }
    @Test
    public void testCalcResamplingRate() {
        Resampling resamp = new Resampling(200);
        org.junit.Assert.assertEquals(200,resamp.calcNewSamplingRate(100));
        org.junit.Assert.assertEquals(2,resamp.getFactor());
        org.junit.Assert.assertEquals(200,resamp.getNewSamplingRate());
        org.junit.Assert.assertEquals(-1,resamp.calcNewSamplingRate(200));
        org.junit.Assert.assertEquals(-1,resamp.calcNewSamplingRate(0));
    }
    @Test
    public void testResampleArray() throws SmException {
        Resampling resamp = new Resampling(200);
        double[] yptest = resamp.resampleArray(yarray, SPS);
        org.junit.Assert.assertArrayEquals(yptest, yparray, EPSILON);
    }
}
