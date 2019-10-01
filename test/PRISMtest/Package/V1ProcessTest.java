/*******************************************************************************
 * Name: Java class V1ProcessTest.java
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

import COSMOSformat.V0Component;
import static SmConstants.VFileConstants.FROM_G_CONVERSION;
import static SmConstants.VFileConstants.RAWACC;
import SmException.FormatException;
import SmException.SmException;
import SmProcessing.ArrayOps;
import SmProcessing.ArrayStats;
import SmProcessing.RawTraceConversion;
import SmProcessing.V1Process;
import SmUtilities.ConfigReader;
import SmUtilities.PrismXMLReader;
import static SmConstants.SmConfigConstants.DATA_UNITS_CODE;
import static SmConstants.SmConfigConstants.DATA_UNITS_NAME;
import static SmConstants.SmConfigConstants.DESPIKE_INPUT;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;

/**
 *
 * @author jmjones
 */
public class V1ProcessTest {
    String[] infile;
    V0Component v0;
    V1Process v1p;
    V1Process v2p;
    V1Process v3p;
    V1Process v4p;
    V1Process v5p;
    double EPSILON = 0.001;
    double delta = 0.0001;
    int[] counts = {3284,3334,3296,3284,3308,3242,3236,3324,3322,3262,
                    3300,3334,3302,3266,3322,3336,3312,3298,3254};
    
    double[] cmsvals = new double[19];
    double[] gvals = new double[19];
    
    ConfigReader config = ConfigReader.INSTANCE;
    
    public V1ProcessTest() {
        this.infile = new String[48];
    }
    
    @Before
    public void setUp() throws SmException, FormatException {
        infile[0] = "Raw acceleration counts   (Format v01.20 with 13 text lines) Src: 921az039.evt";
        infile[1] = "Rcrd of Wed Jan 15, 2014 01:35:00.0 PST";
        infile[2] = "Hypocenter: To be determined    H=   km       ML=     Mw= ";
        infile[3] = "Origin: To be determined ";
        infile[4] = "Statn No: 05- 13921 Code:CE-13921  CGS  Riverside - Limonite & Downey";
        infile[5] = "Coords: 33.975  -117.487   Site geology:  ";
        infile[6] = "Recorder: Etna   s/n 1614 ( 3 Chns of   3 at Sta) Sensor: FBA ";
        infile[7] = "Rcrd start time: 1/15/2014, 09:35:  .0 UTC (Q=5) RcrdId: 13921-L1614-14015.39";
        infile[8] = "Sta Chan  1: 360 deg (Rcrdr Chan  1)";
        infile[9] = "Raw record length =  56.000 sec, Uncor max =    20108 counts, at   25.205 sec.";
        infile[10]= "Processed: 01/15/14  (k2vol0 v0.1 CSMIP)";
        infile[11]= "Record not filtered.";
        infile[12]= "Values used when parameter or data value is unknown/unspecified:   -999, -999.0";
        infile[13]= " 100 Integer-header values follow on  10 lines, Format= (10I8)";
        infile[14]= "       0       1      50     120       1    -999    -999   13921    -999    -999";
        infile[15]= "       5       5       5       5    -999       1    -999    -999       6     360";
        infile[16]= "    -999       1       3    -999    -999    -999    -999    -999       1     109";
        infile[17]= "       3    1614       3       3      24      18    -999      39       1    2014";
        infile[18]= "      15       1      15       9      35       5       5    -999    -999      76";
        infile[19]= "       1       4    -999     360    -999    -999    -999    -999    -999    -999";
        infile[20]= "    -999    -999    -999    -999       0    -999    -999    -999    -999    -999";
        infile[21]= "    -999    -999    -999    -999       0       0       1    -999    -999       1";
        infile[22]= "     560       0       0       0       0       0       0       0     222       0";
        infile[23]= "       0     303    -999    -999    -999    -999    -999    -999    -999    -999";
        infile[24]= " 100 Real-header values follow on  17 lines, Format= (6F13.6)";
        infile[25]= "    33.975300  -117.486500   213.000000   371.000000  -999.000000  -999.000000";
        infile[26]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[27]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[28]= "  -999.000000  -999.000000  -999.000000      .298023     2.500000    25.000000";
        infile[29]= "    30.000000  -999.000000  -999.000000  -999.000000  -999.000000      .000000";
        infile[30]= "  -999.000000      .000000  -999.000000      .005000    56.000000  -999.000000";
        infile[31]= "  -999.000000  -999.000000  -999.000000   100.400000      .660000      .627000";
        infile[32]= "     2.500000     4.000000  -999.000000  -999.000000     1.000000  -999.000000";
        infile[33]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[34]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[35]= "  -999.000000     5.000000    56.000000 20108.000000    25.205000  3304.483000";
        infile[36]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[37]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[38]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[39]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[40]= "      .000000    10.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[41]= "  -999.000000  -999.000000  -999.000000  -999.000000";
        infile[42]= "   1 Comment line(s) follow, each starting with a \"|\":";
        infile[43]= "|";
        infile[44]= "      19 acceleration pts, approx  56 secs, units=counts (50),Format=(10I8)";
        infile[45]= "    3284    3334    3296    3284    3308    3242    3236    3324    3322    3262";
        infile[46]= "    3300    3334    3302    3266    3322    3336    3312    3298    3254";
        infile[47]= "End-of-data for Chan  1 acceleration";
        
        v0 = new V0Component(RAWACC);
        
        double lsb = 0.298023;
        double sensitivity = 0.627;
        double conv1 = RawTraceConversion.countToCMS(lsb, sensitivity, FROM_G_CONVERSION);
        double conv2 = RawTraceConversion.countToG(lsb, sensitivity);
        for (int i=0; i < counts.length; i++) {
            cmsvals[i] = counts[i] * conv1;
            gvals[i] = counts[i] * conv2;
        }
        ArrayStats cmsstat = new ArrayStats( cmsvals );
        ArrayOps.removeValue(cmsvals, cmsstat.getMean());
        ArrayStats gstat = new ArrayStats( gvals );
        ArrayOps.removeValue(gvals, gstat.getMean());
    }

    @Test
    public void TestV1ProcessCMS() throws FormatException, SmException {
        v0.loadComponent(0, infile);
        v1p = new V1Process(v0);
        v1p.processV1Data();
        org.junit.Assert.assertArrayEquals(cmsvals, v1p.getV1Array(), EPSILON);
        org.junit.Assert.assertEquals(1.536153578, v1p.getMeanToZero(), EPSILON);
        org.junit.Assert.assertEquals(0.0, v1p.getAvgVal(), EPSILON);
        org.junit.Assert.assertEquals(-0.0277712701279, v1p.getPeakVal(), EPSILON);
        org.junit.Assert.assertEquals(6, v1p.getPeakIndex(), EPSILON);
        org.junit.Assert.assertEquals(4, v1p.getDataUnitCode());
        org.junit.Assert.assertEquals("cm/sec2", v1p.getDataUnits());
        org.junit.Assert.assertEquals(19, v1p.getV1ArrayLength());
    }
    @Test
    public void TestV1ProcessG() throws SmException, FormatException {
        try {
            String filename = "/PRISMtest/Data/prism_config.xml";
            InputStream ins = PrismXMLReaderTest.class.getResourceAsStream(filename);
            PrismXMLReader xml = new PrismXMLReader();
            xml.readFile(ins);
            config.setConfigValue(DATA_UNITS_CODE, "02");
            config.setConfigValue(DATA_UNITS_NAME, "g");
            config.setConfigValue(DESPIKE_INPUT, "No");
        } catch (ParserConfigurationException | SAXException | IOException err) {
            System.out.println("Unable to parse configuration file ");
        }
        v0.loadComponent(0, infile);
        v2p = new V1Process(v0);
        v2p.processV1Data();
        org.junit.Assert.assertArrayEquals(gvals, v2p.getV1Array(), EPSILON);
        org.junit.Assert.assertEquals(0.00156644070914, v2p.getMeanToZero(), EPSILON);
        org.junit.Assert.assertEquals(0.0, v2p.getAvgVal(), EPSILON);
        org.junit.Assert.assertEquals(-2.83188144044e-05, v2p.getPeakVal(), EPSILON);
        org.junit.Assert.assertEquals(6, v2p.getPeakIndex(), EPSILON);
        org.junit.Assert.assertEquals(2, v2p.getDataUnitCode());
        org.junit.Assert.assertEquals("g", v2p.getDataUnits());
        org.junit.Assert.assertEquals(19, v2p.getV1ArrayLength());
    }
    @Rule public ExpectedException expectedEx = ExpectedException.none();
    @Test
    public void TestBadLSBParms() throws SmException, FormatException {
        expectedEx.expect(SmException.class);
        expectedEx.expectMessage("Real header #22, recorder least sig. bit, is invalid: -999.0");
        infile[28]= "  -999.000000  -999.000000  -999.000000  -999.000000     2.500000    25.000000";
        v0.loadComponent(0, infile);
        v3p = new V1Process(v0);
    }
    @Test
    public void TestBadSensitivityParms() throws SmException, FormatException {
        expectedEx.expect(SmException.class);
        expectedEx.expectMessage("Real header #42, sensor sensitivity, is invalid: 0.0");
        infile[31]= "  -999.000000  -999.000000  -999.000000   100.400000      .660000      .000000";
        v0.loadComponent(0, infile);
        v3p = new V1Process(v0);
    }
}
