/*******************************************************************************
 * Name: Java class ArrayStatsTest.java
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

import SmProcessing.ArrayOps;
import SmProcessing.ArrayStats;
import SmUtilities.TextFileReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author jmjones
 */
public class ArrayStatsTest {
    double[] t1 = {1,2,4,4,4,4,4,3,2,9,9,9,9,9,9,9,9,8,8,8};
    double[] a1;
    double[] a2;
    double[] b1;
    ArrayStats stata1;
    ArrayStats statb1;
    ArrayStats stata2;
    ArrayStats statmode;
    ArrayStats t1mode;
    ArrayStats r1mode;
    double EPSILON = 0.001;
    double EPS1 = 0.01;
    double EPS2 = 0.1;
    int[] hista1 = {2,2,2,2,2,2,2,2,2,1};
    int[] histb1 = {2,2,2,2,2,2,2,2,2,1};
    int[] hista2 = {5,0,0,0,0,0,10,0,0,0};
    int[] histt1 = {3,6,0,0,3};
    int[] histmode = {30,10,10,0,10,0,0,30,20,0};
    double[] r1 = {3.24,65.2,-2.22,9.87,-4.65,1.11};
    
    double[] mode = { 3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,
                        5.2,5.2,5.2,5.2,5.2,5.2,5.2,5.2,5.2,5.2,
                        -1.4,-1.4,-1.4,-1.4,-1.4,-1.4,-1.4,-1.4,-1.4,-1.4,
                        7.6,7.6,7.6,7.6,7.6,7.6,7.6,7.6,7.6,7.6,
                        -9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,
                        5.2,5.2,5.2,5.2,5.2,5.2,5.2,5.2,5.2,5.2,
                        -4.9,-4.9,-4.9,-4.9,-4.9,-4.9,-4.9,-4.9,-4.9,-4.9,
                        3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,
                        3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,3.1,
                        -6.6,-6.6,-6.6,-6.6,-6.6,-6.6,-6.6,-6.6,-6.6,-6.6,
                        7.6,7.6,7.6,7.6,7.6,7.6,7.6,7.6,7.6,7.6,
                        -9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,
                        -9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8,-9.8};

    int[] locs = {4,5,20,21,77,78,96,97};
    double[] levels = {3.2058, 15.9867};
    static String statein = "/PRISMtest/Data/spikefix_testarray.txt";
    static String stateout = "/PRISMtest/Data/spikefix_result.txt";
    static String[] filecontents;
    double[] state;
    double[] despiked;
    
    public ArrayStatsTest() {
        this.a1 = new double[20];
        this.b1 = new double[20];
        this.a2 = new double[25];
    }
    
    @Before
    public void setUp() throws URISyntaxException, IOException {
        for (int i = 1; i < 21; i++) {
            a1[i-1] = (double)i;
        }
        stata1 = new ArrayStats( a1 );
        for (int i = 0; i < 20; i++) {
            b1[i] = a1[i] * Math.pow(-1.0, i);
        }
        statb1 = new ArrayStats( b1 );
        
        for (int i = 1; i < 26; i++) {
            a2[i-1] = mode[i-1];
        }
        stata2 = new ArrayStats( a2 );
        
        statmode = new ArrayStats( mode );
        t1mode = new ArrayStats(t1);
        r1mode = new ArrayStats(r1);

        File name;
        TextFileReader infile;
        int next = 0;
        URL url = ArrayOpsTest.class.getResource( statein );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            state = new double[filecontents.length];
            for (String num : filecontents) {
                state[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = ArrayOpsTest.class.getResource( stateout );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            despiked = new double[filecontents.length];
            for (String num : filecontents) {
                despiked[next++] = Double.parseDouble(num);
            }
        }
    }
    
    @Test
    public void testBasicStats() {
        org.junit.Assert.assertEquals(1.0, stata1.getMinVal(), EPSILON);
        org.junit.Assert.assertEquals(20.0, stata1.getMaxVal(), EPSILON);
        org.junit.Assert.assertEquals(20.0, stata1.getPeakVal(), EPSILON);
        org.junit.Assert.assertEquals(19, stata1.getPeakValIndex());
        org.junit.Assert.assertEquals(10.5, stata1.getMean(), EPSILON);
        
        org.junit.Assert.assertEquals(-20.0, statb1.getMinVal(), EPSILON);
        org.junit.Assert.assertEquals(19.0, statb1.getMaxVal(), EPSILON);
        org.junit.Assert.assertEquals(-20.0, statb1.getPeakVal(), EPSILON);
        org.junit.Assert.assertEquals(19, statb1.getPeakValIndex());
        org.junit.Assert.assertEquals(-0.5, statb1.getMean(), EPSILON);
        
        org.junit.Assert.assertEquals(-1.4, stata2.getMinVal(), EPSILON);
        org.junit.Assert.assertEquals(5.2, stata2.getMaxVal(), EPSILON);
        org.junit.Assert.assertEquals(5.2, stata2.getPeakVal(), EPSILON);
        org.junit.Assert.assertEquals(10, stata2.getPeakValIndex());
        org.junit.Assert.assertEquals(3.04, stata2.getMean(), EPSILON);        
        
        org.junit.Assert.assertEquals(1, t1mode.getMinVal(), EPSILON);
        org.junit.Assert.assertEquals(9, t1mode.getMaxVal(), EPSILON);
        org.junit.Assert.assertEquals(9, t1mode.getPeakVal(), EPSILON);
        org.junit.Assert.assertEquals(9, t1mode.getPeakValIndex());
        org.junit.Assert.assertEquals(6.2, t1mode.getMean(), EPSILON);        
        
        org.junit.Assert.assertEquals(-4.65, r1mode.getMinVal(), EPSILON);
        org.junit.Assert.assertEquals(4, r1mode.getMinValIndex());
        org.junit.Assert.assertEquals(65.2, r1mode.getMaxVal(), EPSILON);
        org.junit.Assert.assertEquals(1, r1mode.getMaxValIndex());
        org.junit.Assert.assertEquals(65.2, r1mode.getPeakVal(), EPSILON);
        org.junit.Assert.assertEquals(1, r1mode.getPeakValIndex());
        org.junit.Assert.assertEquals(12.092, r1mode.getMean(), EPSILON);        
    }

    @Test
    public void testHistogram() {
        int[] bad = new int[0];
        org.junit.Assert.assertArrayEquals(hista1, stata1.makeHistogram(10));
        org.junit.Assert.assertEquals(1.9, stata1.getHistogramInterval(), EPSILON);
        org.junit.Assert.assertArrayEquals(histb1, statb1.makeHistogram(10));
        org.junit.Assert.assertEquals(3.9, statb1.getHistogramInterval(), EPSILON);
        org.junit.Assert.assertArrayEquals(hista2, stata2.makeHistogram(10));
        org.junit.Assert.assertEquals(0.66, stata2.getHistogramInterval(), EPSILON);
        org.junit.Assert.assertArrayEquals(histmode, statmode.makeHistogram(10));
        org.junit.Assert.assertEquals(1.74, statmode.getHistogramInterval(), EPSILON);
        org.junit.Assert.assertArrayEquals(histt1, t1mode.makeHistogram(5));
        org.junit.Assert.assertEquals(1.6, t1mode.getHistogramInterval(), EPSILON);
        org.junit.Assert.assertArrayEquals(bad, t1mode.makeHistogram(0));
    }
    @Test
    public void testModalMin() {
        org.junit.Assert.assertEquals(-8.93, statmode.getModalMin(10), EPSILON);
        org.junit.Assert.assertEquals(4.0, t1mode.getModalMin(4), EPSILON);
        org.junit.Assert.assertEquals(3.4, t1mode.getModalMin(5), EPSILON);
    }
    @Test
    public void testModalMax() {
        org.junit.Assert.assertEquals(3.25, statmode.getModalMax(10), EPSILON);
        org.junit.Assert.assertEquals(8.0, t1mode.getModalMax(4), EPSILON);
        org.junit.Assert.assertEquals(8.2, t1mode.getModalMax(5), EPSILON);
    }
    @Test
    public void testModalMaxAndMin() throws IOException {
        double[] diffarr = ArrayOps.differentiate(state, 0.5, 3);
        for (int j=0; j<diffarr.length; j++) {
            diffarr[j] = Math.abs(diffarr[j]);
        }
        ArrayStats spikestat = new ArrayStats(diffarr);
        double highlevel = spikestat.getModalMax(4);
        org.junit.Assert.assertEquals(levels[1],highlevel, EPS1);
        double lowlevel = spikestat.getModalMin(4);
        org.junit.Assert.assertEquals(levels[0],lowlevel, EPS1);
    }
    @Test
    public void testErrorConditions() {
        int[] testhist = new int[0];
        double[] bad = new double[0];
        ArrayStats badstats = new ArrayStats(bad);
        org.junit.Assert.assertEquals(Double.MIN_VALUE,badstats.getMean(),EPSILON);
        org.junit.Assert.assertArrayEquals(testhist, badstats.makeHistogram(100));
        org.junit.Assert.assertEquals(Double.MIN_VALUE, badstats.getModalMin(100),EPSILON);
        org.junit.Assert.assertEquals(-1,badstats.getMaxValIndex());
    }
    @Test
    public void testErrorConditions2() {
        int[] testhist = new int[0];
        double[] bad = null;
        ArrayStats badstats = new ArrayStats(bad);
        org.junit.Assert.assertEquals(Double.MIN_VALUE,badstats.getMean(),EPSILON);
        org.junit.Assert.assertArrayEquals(testhist, badstats.makeHistogram(100));
        org.junit.Assert.assertEquals(Double.MIN_VALUE, badstats.getModalMin(100),EPSILON);
        org.junit.Assert.assertEquals(-1,badstats.getMaxValIndex());
    }
}
