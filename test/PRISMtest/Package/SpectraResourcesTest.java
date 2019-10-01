/*******************************************************************************
 * Name: Java class SpectraResourcesTest.java
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

import static SmConstants.VFileConstants.NUM_COEF_VALS;
import static SmConstants.VFileConstants.NUM_T_PERIODS;
import static SmConstants.VFileConstants.V3_DAMPING_VALUES;
import static SmConstants.VFileConstants.V3_SAMPLING_RATES;
import SmException.FormatException;
import SmProcessing.SpectraResources;
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
public class SpectraResourcesTest {
    SpectraResources spec;
    String periodfile = "/SmProcessing/spectra/T_periods.txt";
    String dirloc = "/SmProcessing/spectra/";
    String[] PeriodsText;
    Double EPSILON = .000001;
    static final String[] spectraFileNames =  { "CoefTable_50_0.txt",
                                                "CoefTable_50_0.02.txt",
                                                "CoefTable_50_0.05.txt",
                                                "CoefTable_50_0.1.txt",
                                                "CoefTable_50_0.2.txt",
                                                "CoefTable_100_0.txt",
                                                "CoefTable_100_0.02.txt",
                                                "CoefTable_100_0.05.txt",
                                                "CoefTable_100_0.1.txt",
                                                "CoefTable_100_0.2.txt",
                                                "CoefTable_200_0.txt",
                                                "CoefTable_200_0.02.txt",
                                                "CoefTable_200_0.05.txt",
                                                "CoefTable_200_0.1.txt",
                                                "CoefTable_200_0.2.txt",
                                                "CoefTable_500_0.txt",
                                                "CoefTable_500_0.02.txt",
                                                "CoefTable_500_0.05.txt",
                                                "CoefTable_500_0.1.txt",
                                                "CoefTable_500_0.2.txt" };
    String[][] coeftext;
    
    public SpectraResourcesTest() throws IOException {
        spec = new SpectraResources();
    }
    @Before
    public void setUp() throws IOException, URISyntaxException {
        File name;
        TextFileReader reader;
        URL url = SpectraResourcesTest.class.getResource(periodfile);
        if (url != null) {
            name = new File(url.toURI());
            reader = new TextFileReader(name);
            PeriodsText = reader.readInTextFile();
        } else {
            System.out.println("period url null");
        }
        coeftext = new String[spectraFileNames.length][];
        for (int i = 0; i < spectraFileNames.length; i++) {
            url = SpectraResourcesTest.class.getResource(dirloc + spectraFileNames[i]);
            if (url != null) {
                name = new File(url.toURI());
                reader = new TextFileReader(name);
                coeftext[i] = reader.readInTextFile();
            } else {
                System.out.println("coef url null");
            }
        }
    }
    @Test
    public void checkTPeriodText() {
        String[] ttext = spec.getTPeriodsText();
        for (int i = 0; i < ttext.length; i++) {
            org.junit.Assert.assertEquals(PeriodsText[i], ttext[i]);
        }
    }
    @Test
    public void checkTPeriodVals() throws FormatException {
        double[] tvals = spec.getTperiods();
        double testval;
        for (int i = 0; i < tvals.length; i++) {
            testval = Double.parseDouble(PeriodsText[i]);
            org.junit.Assert.assertEquals(testval, tvals[i], EPSILON);
        }
    }
    @Test
    public void checkCoefsText() {
        String[][] ctext = spec.getCoefsText();
        for (int i = 0; i < ctext.length; i++) {
            for (int j = 0; j < NUM_T_PERIODS; j++) {
                org.junit.Assert.assertEquals(coeftext[i][j],ctext[i][j]);
            }
        }
    }
    @Test
    public void checkCoefVals() throws FormatException {
        double[][] cvals;
        String coefline;
        String[] coefpieces = new String[NUM_COEF_VALS];
        double[] expected = new double[NUM_COEF_VALS];
        int len = V3_DAMPING_VALUES.length;
        for (int i = 0; i < V3_SAMPLING_RATES.length; i++) {
            for (int j = 0; j < V3_DAMPING_VALUES.length; j++) {
                cvals = spec.getCoefArray(V3_SAMPLING_RATES[i], V3_DAMPING_VALUES[j]);
                for (int k = 0; k < NUM_T_PERIODS; k++) {
                    coefline = coeftext[(i*len)+j][k];
                    coefpieces = coefline.trim().split("\\s+");
                    for (int each = 0; each < NUM_COEF_VALS; each++) {
                        expected[each] = Double.parseDouble(coefpieces[each]);
                    }
                    org.junit.Assert.assertArrayEquals(expected, cvals[k], EPSILON);
                }
            }
        }
    }
    @Test
    public void checkV3Logic() throws IOException, FormatException {
        double[][][] spectra;
        double[] T_periods;
        double delta_t = 0.01;
        double sampersec = 1.0 / delta_t;    
        spectra = new double[V3_DAMPING_VALUES.length][][];
        spec = new SpectraResources();
        T_periods = spec.getTperiods();
        for (int i = 0; i < V3_DAMPING_VALUES.length; i++) {
            spectra[i] = spec.getCoefArray(sampersec, V3_DAMPING_VALUES[i]);
        }

        double coef_a; double coef_b;
        double coef_c; double coef_d;
        double coef_e; double coef_f;
        
        for (int d = 0; d < V3_DAMPING_VALUES.length; d++) {
            for (int p = 0; p < T_periods.length; p++) {
                coef_a = spectra[d][p][0];
                coef_b = spectra[d][p][1];
                coef_c = spectra[d][p][2];
                coef_d = spectra[d][p][3];
                coef_e = spectra[d][p][4];
                coef_f = spectra[d][p][5];
                
//                System.out.println("\ndamping: " + V3_DAMPING_VALUES[d]);
//                System.out.println("period: " + T_periods[p]);
//                System.out.println("sampersec: " + sampersec);
//                System.out.println("a:" +  coef_a + " b: " + coef_b + " c: " + coef_c);
//                System.out.println("d:" +  coef_d + " e: " + coef_e + " f: " + coef_f);
            }
        }
    }
}
