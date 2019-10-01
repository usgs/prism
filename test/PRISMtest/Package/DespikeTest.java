/*******************************************************************************
 * Name: Java class DespikeTest.java
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

import SmProcessing.Despiking;
import SmUtilities.TextFileReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author jmjones
 */
public class DespikeTest {
    public static final int LENGTH = 100;
    public static final double EPSILON = 3.0;
    public static final double SM_EPSILON = 1.0;
    public static double[] inarr;
    static String statein = "/PRISMtest/Data/spikefix_testarray.txt";
    static String stateout = "/PRISMtest/Data/spikefix_result.txt";
    static String[] filecontents;
    static double[] state;
    static double[] despiked;
    int[] locs = {4,5,20,21,77,78,96,97};
    double[] r_array = {3.2058, 15.9867};
    private static double[] nospikes;
    
    public DespikeTest() throws IOException {
        inarr = new double[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            inarr[i] = (2.0 * Math.sin(i));
        }
        nospikes = new double[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            nospikes[i] = 1.0;
        }
        
    }
    @BeforeClass
    public static void setUp() throws URISyntaxException, IOException {
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
    public void testSpikeFix1() throws IOException {
        int nbors = 5;
        int wsize = 25;
        int found;
        double[] testarr = new double[LENGTH];
        Despiking despike = new Despiking(3);
        
        //test location where no spike exists
        testarr = Arrays.copyOf(inarr, state.length);
        found = despike.spikeFix(testarr, 53, nbors, wsize);
        org.junit.Assert.assertEquals(0, found);
        org.junit.Assert.assertArrayEquals(inarr, testarr, EPSILON);
        
        //find the 2 spikes and fix
        testarr = Arrays.copyOf(inarr, LENGTH); 
        testarr[21] = 15.0;
        testarr[78] = 20.0;
        found = despike.spikeFix(testarr, 21, nbors, wsize);
        org.junit.Assert.assertEquals(1, found);
        found = despike.spikeFix(testarr, 78, nbors, wsize);
        org.junit.Assert.assertEquals(1, found);
        org.junit.Assert.assertArrayEquals(inarr, testarr, EPSILON);
        
        //find spikes at the front and end of the array
        testarr = Arrays.copyOf(inarr, LENGTH); 
        testarr[0] = -52.0;
        found = despike.spikeFix(testarr, 0, nbors, wsize);
        org.junit.Assert.assertEquals(1, found);
        org.junit.Assert.assertArrayEquals(inarr, testarr, EPSILON);
        
        testarr = Arrays.copyOf(inarr, LENGTH); 
        testarr[97] = 26.0;
        found = despike.spikeFix(testarr, 97, nbors, wsize);
        org.junit.Assert.assertEquals(1, found);
        org.junit.Assert.assertArrayEquals(inarr, testarr, EPSILON);
        
        //find spikes close to start or end
        testarr = Arrays.copyOf(inarr, LENGTH); 
        testarr[7] = -38.0;
        found = despike.spikeFix(testarr, 7, nbors, wsize);
        org.junit.Assert.assertEquals(1, found);
        org.junit.Assert.assertArrayEquals(inarr, testarr, EPSILON);
        
        testarr = Arrays.copyOf(inarr, LENGTH); 
        testarr[93] = 60.0;
        found = despike.spikeFix(testarr, 94, nbors, wsize);
        org.junit.Assert.assertEquals(1, found);
        org.junit.Assert.assertArrayEquals(inarr, testarr, EPSILON);
    }
    @Test
    public void testSpikefix2() {
        int nbors = 5;
        int wsize = 25;
        double[] test = Arrays.copyOf(state, state.length);
        int found;
        Despiking despike2 = new Despiking(3);
        for (int i=0; i < locs.length; i++) {
            found = despike2.spikeFix(test, locs[i], nbors, wsize);
        }
        org.junit.Assert.assertArrayEquals(despiked, test, EPSILON);
    }
    @Test
    public void testSpikefix3() {
        int nbors = 5;
        int wsize = 25;
        double[] test;
        int found;
        int count = 0;
        Despiking despike3 = new Despiking(3);
        for (int i=0; i < despiked.length; i++) {
            test = Arrays.copyOf(despiked, despiked.length);
            test[i] = -27.0;
            found = despike3.spikeFix(test, i, nbors, wsize);
            count = count + found;
            org.junit.Assert.assertEquals(1, found);
        }
        org.junit.Assert.assertEquals( despiked.length, count);
    }
    @Test
    public void testDespiking1() {
        Despiking despike4 = new Despiking(3);
        double[] test = Arrays.copyOf(state, state.length);
        boolean found = despike4.removeSpikes(test, 0.01);
        org.junit.Assert.assertEquals(true, found);
        org.junit.Assert.assertEquals(19, despike4.getSpikeIndex());
        org.junit.Assert.assertArrayEquals(despiked, test, EPSILON);
    }
    @Test
    public void testDespiking2() throws IOException {
        Despiking despike5 = new Despiking(3);
        double[] test = Arrays.copyOf(nospikes, nospikes.length);
        boolean found = despike5.removeSpikes(test, 0.01);
        org.junit.Assert.assertEquals(false, found);
        org.junit.Assert.assertEquals(0, despike5.getSpikeIndex());
        org.junit.Assert.assertArrayEquals(nospikes, test, 0.001);
    }
    @Test
    public void testDespiking3() throws IOException {
        Despiking despike6 = new Despiking(3);
        double[] test = new double[0];
        boolean found = despike6.removeSpikes(test, 0.01);
        org.junit.Assert.assertEquals(false, found);
        org.junit.Assert.assertEquals(-1, despike6.getSpikeIndex());
    }
    @Test
    public void testGetters() {
        Despiking despike = new Despiking(2);
        org.junit.Assert.assertEquals(2, despike.getNumStd());
        org.junit.Assert.assertEquals(5, despike.getNeighbors());
        org.junit.Assert.assertEquals(4, despike.getNumBins());
        org.junit.Assert.assertEquals(2, despike.getNumPasses());
        org.junit.Assert.assertEquals(25, despike.getWindowSize());
        org.junit.Assert.assertEquals(0, despike.getSpikeIndex());
    }
}
