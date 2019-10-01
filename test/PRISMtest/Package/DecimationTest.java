/*******************************************************************************
 * Name: Java class DecimationTest.java
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
import SmProcessing.Decimation;
import SmProcessing.FFourierTransform;
import SmUtilities.TextFileReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import org.apache.commons.math3.complex.Complex;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 *
 * @author jmjones
 */
public class DecimationTest {
    static Complex[] shifteven;
    static Complex[] shiftodd;
    static int evenlen = 14;
    static int oddlen = 15;
    static String yfile = "/PRISMtest/Data/decimin.txt";
    static String ypfile = "/PRISMtest/Data/decimout.txt";
    static String[] filecontents;
    static double[] ytest;
    static double[] yptest;
    static int factor = 2;
    static double EPSILON = 0.000001;
    private final FFourierTransform fft;
    
    public DecimationTest() {
        shifteven = new Complex[evenlen];
        shiftodd = new Complex[oddlen];
        this.fft = new FFourierTransform();
    }
    
    @BeforeClass
    public static void setUpClass() throws URISyntaxException, IOException {
        File name;
        TextFileReader infile;

        int next = 0;
        URL url = DecimationTest.class.getResource( yfile );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            ytest = new double[filecontents.length];
            for (String num : filecontents) {
                ytest[next++] = Double.parseDouble(num);
            }
        }
        next = 0;
        url = Decimation.class.getResource( ypfile );
        if (url != null) {
            name = new File(url.toURI());
            infile = new TextFileReader( name );
            filecontents = infile.readInTextFile();
            yptest = new double[filecontents.length];
            for (String num : filecontents) {
                yptest[next++] = Double.parseDouble(num);
            }
        }
    }
    
    @Before
    public void setUp() {
        double real = 1.0;
        double imag = 2.0;
        Complex compnum;
        for (int i=0; i < evenlen; i++) {
            compnum = new Complex((real+i),(imag+i));
            shifteven[i] = compnum;
        }
        for (int i=0; i < oddlen; i++) {
            compnum = new Complex((real+i),(imag+i));
            shiftodd[i] = compnum;
        }
    }
   
    @Test
    public void testShiftForwardAndBack() {
        String[] start = new String[evenlen];
        String[] end = new String[evenlen];
        Complex[] forward = fft.shiftForward(shifteven);
        Complex[] back = fft.shiftBack(forward);
        for (int i=0; i<evenlen; i++){
            start[i] = shifteven[i].toString();
            end[i] = back[i].toString();
        }
        org.junit.Assert.assertArrayEquals(start,end);

        start = new String[oddlen];
        end = new String[oddlen];
        forward = fft.shiftForward(shiftodd);
        back = fft.shiftBack(forward);
        for (int i=0; i<oddlen; i++){
            start[i] = shiftodd[i].toString();
            end[i] = back[i].toString();
        }
        org.junit.Assert.assertArrayEquals(start,end);
    }
    @Test
    public void testDecimation() throws SmException, IOException {
        Decimation dec = new Decimation();
        double[] test = dec.decimateArray(ytest, factor);
        int newlength = (ytest.length % factor == 0) ? (ytest.length/factor) : 
                                                    (int)(ytest.length/factor)+1;
        org.junit.Assert.assertEquals(newlength,test.length);
        
        org.junit.Assert.assertArrayEquals(test, yptest, EPSILON);
    }
    
    @Rule public ExpectedException expectedEx = ExpectedException.none();
    @Test
    public void testInputCheck1() throws SmException, IOException {
        expectedEx.expect(SmException.class);
        expectedEx.expectMessage("Invalid decimation input array");
        Decimation dec = new Decimation();
        double[] bad = new double[0];
        double[] outarr = dec.decimateArray(bad,100);
    }
    @Test
    public void testInputCheck2() throws SmException, IOException {
        expectedEx.expect(SmException.class);
        expectedEx.expectMessage("Invalid decimation input array");
        Decimation dec = new Decimation();
        double[] morebad = null;
        double[] outarr = dec.decimateArray(morebad,100);
    }
    @Test
    public void testInputCheck3() throws SmException, IOException {
        expectedEx.expect(SmException.class);
        expectedEx.expectMessage("Invalid decimation factor 0");
        double[] test = new double[10];
        Arrays.fill(test,2.0);
        Decimation dec = new Decimation();
        double[] outarr = dec.decimateArray(test,0);
    }
    @Test
    public void testInputCheck4() throws SmException, IOException {
        expectedEx.expect(SmException.class);
        expectedEx.expectMessage("Invalid decimation factor -3");
        double[] test = new double[10];
        Arrays.fill(test,2.0);
        Decimation dec = new Decimation();
        double[] outarr = dec.decimateArray(test,-3);
    }
}
