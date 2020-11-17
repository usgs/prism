/*******************************************************************************
 * Name: Java class FilterCutOffThresholdsTest.java
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

import SmConstants.VFileConstants.MagnitudeType;
import SmProcessing.FilterCutOffThresholds;
import SmUtilities.TextFileReader;
import SmUtilities.TextFileWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jmjones
 */
public class FilterCutOffThresholdsTest {
    private final double NOVAL = -999.99;
    private final double EPSILON = 0.00001;
    static String FASin = "/PRISMtest/Data/FAS.txt";
    static String FASout = "/PRISMtest/Data/smoothFAS2.txt";
    static String[] filecontents;
    static double[] origFAS;
    static double[] smoothFAS;
    static String benchmarkin1 = "/PRISMtest/Data/accel_test1.txt";
    static double[] benchmark1;
    static String benchmarkin2 = "/PRISMtest/Data/accel_test2.txt";
    static double[] benchmark2;
    static String benchmarkin3 = "/PRISMtest/Data/accel_test3.txt";
    static double[] benchmark3;
    static String x1in = "/PRISMtest/Data/x1.txt";
    static String y1in = "/PRISMtest/Data/y1.txt";
    static String x2in = "/PRISMtest/Data/x2.txt";
    static String y2in = "/PRISMtest/Data/y2.txt";
    static double[] x1, y1, x2, y2;
    static RealMatrix solution;
    static final double[] sys1 = {-6.4167,5.4167};
    static final double[] sys2 = {27.167,50.833};
    
    public FilterCutOffThresholdsTest() {
        solution = MatrixUtils.createRealMatrix(2,2);
        solution.setRow(0,sys1);
        solution.setRow(1,sys2);
    }
    
    @BeforeClass
    public static void setUp() throws URISyntaxException, IOException {
        File name;
        TextFileReader infile;
        int next = 0;
        URL url = ArrayOpsTest.class.getResource( FASin );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            origFAS = new double[filecontents.length];
            for (String num : filecontents) {
                origFAS[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( FASout );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            smoothFAS = new double[filecontents.length];
            for (String num : filecontents) {
                smoothFAS[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( benchmarkin1 );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            benchmark1 = new double[filecontents.length];
            for (String num : filecontents) {
                benchmark1[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( benchmarkin2 );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            benchmark2 = new double[filecontents.length];
            for (String num : filecontents) {
                benchmark2[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( benchmarkin3 );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            benchmark3 = new double[filecontents.length];
            for (String num : filecontents) {
                benchmark3[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( x1in );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            x1 = new double[filecontents.length];
            for (String num : filecontents) {
                x1[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( y1in );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            y1 = new double[filecontents.length];
            for (String num : filecontents) {
                y1[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( x2in );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            x2 = new double[filecontents.length];
            for (String num : filecontents) {
                x2[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( y2in );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            y2 = new double[filecontents.length];
            for (String num : filecontents) {
                y2[next++] = Double.parseDouble(num);
            }
        }
    }    
    
    @Test
    public void selectMagsTest() {
        MagnitudeType magtype;
        FilterCutOffThresholds threshold = new FilterCutOffThresholds();
    
        magtype = threshold.SelectMagnitude(6.0, NOVAL, NOVAL, NOVAL, NOVAL);
        org.junit.Assert.assertEquals(MagnitudeType.MOMENT, magtype);
        org.junit.Assert.assertEquals(6.0,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getHighCutOff(),EPSILON);

        magtype = threshold.SelectMagnitude(NOVAL, 5.4, NOVAL, NOVAL, NOVAL);
        org.junit.Assert.assertEquals(MagnitudeType.M_LOCAL, magtype);
        org.junit.Assert.assertEquals(5.4,threshold.getMagnitude(),EPSILON);
    
        magtype = threshold.SelectMagnitude(NOVAL, NOVAL, 4.4, NOVAL, NOVAL);
        org.junit.Assert.assertEquals(MagnitudeType.SURFACE, magtype);
        org.junit.Assert.assertEquals(4.4,threshold.getMagnitude(),EPSILON);
    
        magtype = threshold.SelectMagnitude(NOVAL, NOVAL, NOVAL, 3.4, NOVAL);
        org.junit.Assert.assertEquals(MagnitudeType.M_OTHER, magtype);
        org.junit.Assert.assertEquals(3.4,threshold.getMagnitude(),EPSILON);

        magtype = threshold.SelectMagnitude(NOVAL, NOVAL, NOVAL, NOVAL, NOVAL);
        org.junit.Assert.assertEquals(MagnitudeType.INVALID, magtype);
        org.junit.Assert.assertEquals(0.0,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getHighCutOff(),EPSILON);
    }
    @Test
    public void checkSPSTest() {
        MagnitudeType magtype;
        double magnitude;
        double samprate = 100.0;
        FilterCutOffThresholds threshold = new FilterCutOffThresholds();
        
        magtype = threshold.SelectMagnitude(6.0, NOVAL, NOVAL, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.MOMENT, magtype);
        org.junit.Assert.assertEquals(6.0,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.1,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(40.0,threshold.getHighCutOff(),EPSILON);
        
        samprate = 90.0;
        magtype = threshold.SelectMagnitude(6.0, NOVAL, NOVAL, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.MOMENT, magtype);
        org.junit.Assert.assertEquals(6.0,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.1,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(36.0,threshold.getHighCutOff(),EPSILON);

        magtype = threshold.SelectMagnitude(5.4, NOVAL, NOVAL, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.MOMENT, magtype);
        org.junit.Assert.assertEquals(5.4,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.3,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(36.0,threshold.getHighCutOff(),EPSILON);

        magtype = threshold.SelectMagnitude(3.5, NOVAL, NOVAL, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.MOMENT, magtype);
        org.junit.Assert.assertEquals(3.5,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.3,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(36.0,threshold.getHighCutOff(),EPSILON);
    
        samprate = 60.0;
        magtype = threshold.SelectMagnitude(3.5, NOVAL, NOVAL, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.MOMENT, magtype);
        org.junit.Assert.assertEquals(3.5,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.3,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(24.0,threshold.getHighCutOff(),EPSILON);
    
        magtype = threshold.SelectMagnitude(NOVAL, NOVAL, NOVAL, 3.4, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.M_OTHER, magtype);
        org.junit.Assert.assertEquals(3.4,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.5,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(24.0,threshold.getHighCutOff(),EPSILON);

        samprate = 50.0;
        magtype = threshold.SelectMagnitude(NOVAL, 7.1, NOVAL, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.M_LOCAL, magtype);
        org.junit.Assert.assertEquals(7.1,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.1,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(20.0,threshold.getHighCutOff(),EPSILON);

        samprate = 40.0;
        magtype = threshold.SelectMagnitude(NOVAL, NOVAL, 6.2, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.LOWSPS, magtype);
        org.junit.Assert.assertEquals(6.2,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getHighCutOff(),EPSILON);

        samprate = 60.0;
        magtype = threshold.SelectMagnitude(NOVAL, NOVAL, NOVAL, NOVAL, NOVAL);
        magnitude = threshold.getMagnitude();
        magtype = threshold.SelectMagThresholds(magtype,magnitude,samprate);
        org.junit.Assert.assertEquals(MagnitudeType.INVALID, magtype);
        org.junit.Assert.assertEquals(0.0,threshold.getMagnitude(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getLowCutOff(),EPSILON);
        org.junit.Assert.assertEquals(0.0,threshold.getHighCutOff(),EPSILON);
    }
    @Test
    public void FASsmoothTest() throws IOException {
        FilterCutOffThresholds threshold = new FilterCutOffThresholds();
        double[] test = threshold.smoothFAS(origFAS);
        org.junit.Assert.assertArrayEquals( smoothFAS, test, EPSILON);
        
        double[] noarray = null;
        double[] zarray = new double[0];
        test = threshold.smoothFAS(noarray);
        org.junit.Assert.assertArrayEquals( zarray, test, 0.01);
        test = threshold.smoothFAS(zarray);
        org.junit.Assert.assertArrayEquals( zarray, test, 0.01);
    }
    @Test
    public void FindFreqThresholdsTest() throws IOException {
        double samprate = 100;
        double origrate = 100;
        int pick1 = 3078;
        int pick2 = 3344;
        int pick3 = 3171;
        FilterCutOffThresholds threshold = new FilterCutOffThresholds();
        threshold.findFreqThresholds(benchmark1, pick1,samprate,origrate);
        threshold.findFreqThresholds(benchmark2, pick2,samprate,origrate);
        threshold.findFreqThresholds(benchmark3, pick3,samprate,origrate);
        
        double[] noarray = null;
        double[] zarray = new double[0];
        int lowonset = 0;
        int highonset = 22;
        threshold.findFreqThresholds(null, pick3,samprate,origrate);
        org.junit.Assert.assertEquals( 0.0, threshold.getHighCutOff(), EPSILON);
        org.junit.Assert.assertEquals( 0.0, threshold.getLowCutOff(), EPSILON);
        threshold.findFreqThresholds(zarray, pick3,samprate,origrate);
        org.junit.Assert.assertEquals( 0.0, threshold.getHighCutOff(), EPSILON);
        org.junit.Assert.assertEquals( 0.0, threshold.getLowCutOff(), EPSILON);
        threshold.findFreqThresholds(x1, lowonset,samprate,origrate);
        org.junit.Assert.assertEquals( 0.0, threshold.getHighCutOff(), EPSILON);
        org.junit.Assert.assertEquals( 0.0, threshold.getLowCutOff(), EPSILON);
        threshold.findFreqThresholds(x1, highonset,samprate,origrate);
        org.junit.Assert.assertEquals( 0.0, threshold.getHighCutOff(), EPSILON);
        org.junit.Assert.assertEquals( 0.0, threshold.getLowCutOff(), EPSILON);

    }
    @Test
    public void findIntersectionTest() {
        FilterCutOffThresholds threshold = new FilterCutOffThresholds();
        RealMatrix result = threshold.findIntersection(x1, y1, x2, y2);       
        org.junit.Assert.assertArrayEquals( solution.getRow(0), result.getRow(0), 0.001);
        org.junit.Assert.assertArrayEquals( solution.getRow(1), result.getRow(1), 0.001);
        
        double[] noarray = null;
        double[] zarray = new double[3];
        RealMatrix badinput = MatrixUtils.createRealMatrix(2,2);
        result = threshold.findIntersection(null, y1, x2, y2);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );
        result = threshold.findIntersection(zarray, y1, x2, y2);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );


        result = threshold.findIntersection(x1, null, x2, y2);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );
        result = threshold.findIntersection(x1, zarray, x2, y2);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );


        result = threshold.findIntersection(x1, y1, null, y2);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );
        result = threshold.findIntersection(x1, y1, zarray, y2);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );


        result = threshold.findIntersection(x1, y1, x2, null);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );
        result = threshold.findIntersection(x1, y1, x2, zarray);
        org.junit.Assert.assertArrayEquals( badinput.getRow(0), result.getRow(0), EPSILON );
        org.junit.Assert.assertArrayEquals( badinput.getRow(1), result.getRow(1), EPSILON );
    }
}
