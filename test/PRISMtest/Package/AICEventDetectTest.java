/*******************************************************************************
 * Name: Java class AICEventDetectTest.java
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

import SmProcessing.AICEventDetect;
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
public class AICEventDetectTest {
    final double EPSILON = 0.001;
    AICEventDetect aicPeak;
//    AICEventDetect aicWhole;
    
    static String picktest = "/PRISMtest/Data/15481673.AZ.FRD.HNN.txt";
    static String[] fileContents;
    
    static double[] hnn;    
    
    public AICEventDetectTest() {
        aicPeak = new AICEventDetect();
//        aicWhole = new AICEventDetect();
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException, URISyntaxException {
        int next = 0;
        URL url = AICEventDetectTest.class.getResource(picktest);
        if (url != null) {
            File name = new File(url.toURI());
            TextFileReader infile = new TextFileReader( name );
            fileContents = infile.readInTextFile();
//            System.out.println("length: " + fileContents.length);
            hnn = new double[fileContents.length];
            for (String num : fileContents) {
                hnn[next++] = Double.parseDouble(num);
            }
        } else {
            System.out.println("url null");
        }
    }
    
     @Test
     public void checkEventDetection() {
         double[] empty = new double[0];
         double[] test = null;
         String test2 = null;
         int pick1 = aicPeak.calculateIndex(hnn, "topeak");
         org.junit.Assert.assertEquals(1472, pick1);
         org.junit.Assert.assertEquals(1472,aicPeak.getIndex());
         
         org.junit.Assert.assertEquals(972,aicPeak.applyBuffer(5, 0.01));
         org.junit.Assert.assertEquals(972,aicPeak.getBufferedIndex());
         org.junit.Assert.assertEquals(0,aicPeak.applyBuffer(15, 0.01));
         org.junit.Assert.assertEquals(-1,aicPeak.applyBuffer(5, 0.0));
         
         org.junit.Assert.assertEquals(-1, aicPeak.calculateIndex(empty, "topeak"));
         org.junit.Assert.assertEquals(-1, aicPeak.calculateIndex(test, "topeak"));
         
         org.junit.Assert.assertEquals(1472, aicPeak.calculateIndex(hnn, ""));
         org.junit.Assert.assertEquals(1472, aicPeak.calculateIndex(hnn, "nnn"));
         org.junit.Assert.assertEquals(1472, aicPeak.calculateIndex(hnn, test2));
         
         
     }
}
