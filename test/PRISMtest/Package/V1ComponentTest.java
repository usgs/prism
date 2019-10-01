/*******************************************************************************
 * Name: Java class V1ComponentTest.java
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
import COSMOSformat.V1Component;
import static SmConstants.VFileConstants.AVG_VAL;
import static SmConstants.VFileConstants.DELTA_T;
import static SmConstants.VFileConstants.MEAN_ZERO;
import static SmConstants.VFileConstants.MSEC_TO_SEC;
import static SmConstants.VFileConstants.PEAK_VAL;
import static SmConstants.VFileConstants.PEAK_VAL_TIME;
import static SmConstants.VFileConstants.PROCESSING_STAGE_INDEX;
import static SmConstants.VFileConstants.RAWACC;
import static SmConstants.VFileConstants.SERIES_LENGTH;
import static SmConstants.VFileConstants.START_TIME_DAY;
import static SmConstants.VFileConstants.START_TIME_HOUR;
import static SmConstants.VFileConstants.START_TIME_JULDAY;
import static SmConstants.VFileConstants.START_TIME_MIN;
import static SmConstants.VFileConstants.START_TIME_MONTH;
import static SmConstants.VFileConstants.START_TIME_SEC;
import static SmConstants.VFileConstants.START_TIME_YEAR;
import static SmConstants.VFileConstants.UNCORACC;
import static SmConstants.VFileConstants.V1_STAGE;
import SmException.FormatException;
import SmException.SmException;
import SmProcessing.ArrayOps;
import SmProcessing.ArrayStats;
import SmProcessing.V1Process;
import java.time.ZonedDateTime;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author jmjones
 */
public class V1ComponentTest {
    String[] v0file;
    String[] v1file;
    V0Component v0;
    V1Component v1;
    V1Component v1a;
    double delta = 0.0001;
    int arrlen = 14;
    double[] newArray = new double[arrlen];
    double[] saveArray = new double[arrlen];
    double EPSILON = 0.000001;
    
    public V1ComponentTest() {
        this.v0file = new String[48];
        this.v1file = new String[53];
        double start = 1.49999999;
        for (int i=0; i < arrlen; i++) {
            newArray[i] = start;
            saveArray[i] = start;
            start += 3.6556;
        }
    }
    @Before
    public void setUp() throws FormatException, SmException {
        v0file[0] = "Raw acceleration counts   (Format v01.20 with 13 text lines) Src: 921az039.evt";
        v0file[1] = "Rcrd of Wed Jan 15, 2014 01:35:00.0 PST";
        v0file[2] = "Hypocenter: To be determined    H=   km       ML=     Mw= ";
        v0file[3] = "Origin: To be determined ";
        v0file[4] = "Statn No: 05- 13921 Code:CE-13921  CGS  Riverside - Limonite & Downey";
        v0file[5] = "Coords: 33.975  -117.487   Site geology:  ";
        v0file[6] = "Recorder: Etna   s/n 1614 ( 3 Chns of   3 at Sta) Sensor: FBA ";
        v0file[7] = "Rcrd start time: 1/15/2014 09:35:23.152 UTC (Q=5) RcrdId: 13921-L1614-14015.39";
        v0file[8] = "Sta Chan  1: 360 deg (Rcrdr Chan  1)";
        v0file[9] = "Raw record length =  56.000 sec, Uncor max =    20108 counts, at   25.205 sec.";
        v0file[10]= "Processed: 01/15/14  (k2vol0 v0.1 CSMIP)";
        v0file[11]= "Record not filtered.";
        v0file[12]= "Values used when parameter or data value is unknown/unspecified:   -999, -999.0";
        v0file[13]= " 100 Integer-header values follow on  10 lines, Format= (10I8)";
        v0file[14]= "       0       1      50     120       1    -999    -999   13921    -999    -999";
        v0file[15]= "       5       5       5       5    -999       1    -999    -999       6     360";
        v0file[16]= "    -999       1       3    -999    -999    -999    -999    -999       1     109";
        v0file[17]= "       3    1614       3       3      24      18    -999      39       1    2014";
        v0file[18]= "      15       1      15       9      35       5       5    -999    -999       1";
        v0file[19]= "       1       4    -999     360    -999    -999    -999    -999    -999    -999";
        v0file[20]= "    -999    -999    -999    -999       0    -999    -999    -999    -999    -999";
        v0file[21]= "    -999    -999    -999    -999       0       0       1    -999    -999       1";
        v0file[22]= "     560       0       0       0       0       0       0       0     222       0";
        v0file[23]= "       0     303    -999    -999    -999    -999    -999    -999    -999    -999";
        v0file[24]= " 100 Real-header values follow on  17 lines, Format= (6F13.6)";
        v0file[25]= "    33.975300  -117.486500   213.000000   371.000000  -999.000000  -999.000000";
        v0file[26]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[27]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[28]= "  -999.000000  -999.000000  -999.000000      .298023     2.500000    25.000000";
        v0file[29]= "    30.000000  -999.000000  -999.000000  -999.000000  -999.000000      .000000";
        v0file[30]= "  -999.000000      .000000  -999.000000      .005000    56.000000  -999.000000";
        v0file[31]= "  -999.000000  -999.000000  -999.000000   100.400000      .660000      .627000";
        v0file[32]= "     2.500000     4.000000  -999.000000  -999.000000     1.000000  -999.000000";
        v0file[33]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[34]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[35]= "  -999.000000     5.000000    56.000000 20108.000000    25.205000  3304.483000";
        v0file[36]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[37]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[38]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[39]= "  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[40]= "      .000000    10.000000  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[41]= "  -999.000000  -999.000000  -999.000000  -999.000000";
        v0file[42]= "   1 Comment line(s) follow, each starting with a \"|\":";
        v0file[43]= "|";
        v0file[44]= "      19 acceleration pts, approx  56 secs, units=counts (50),Format=(10I8)";
        v0file[45]= "    3284    3334    3296    3284    3308    3242    3236    3324    3322    3262";
        v0file[46]= "    3300    3334    3302    3266    3322    3336    3312    3298    3254";
        v0file[47]= "End-of-data for Chan  1 acceleration";
        
        v0 = new V0Component(RAWACC);
        v0.loadComponent(0, v0file);

        v1file[0] = "Uncorrected acceleration   (Format v01.20 with 13 text lines) Src: 15481673";
        v1file[1] = "Record of Earthquake of Sat Mar 29 04:09:42 2014";
        v1file[2] = "Hypocenter: To be determined    H=   km       ML=     Mw= ";
        v1file[3] = "Origin: To be determined ";
        v1file[4] = "Statn No: 05- 13921 Code:CE-13921  CGS  Riverside - Limonite & Downey";
        v1file[5] = "Coords: 33.975  -117.487   Site geology:  ";
        v1file[6] = "Recorder: Etna   s/n 1614 ( 3 Chns of   3 at Sta) Sensor: FBA ";
        v1file[7] = "Rcrd start time:03/29/2014, 04:09:48.968 UTC (Q=5)  RcrdID: 15481673.AZ.FRD.HNN";
        v1file[8] = "Sta Chan  2: 360 deg (Rcrdr Chan  N) Location:";
        v1file[9] = "Raw record length = 307.190 sec. Uncor Max = 13643.000 c, at   34.830 sec.";
        v1file[10]= "Processed:2014-09-03 20:39:15 GMT, USGS, Max =    0.837 cm/sec2 at   33.350 sec";
        v1file[11]= "Record not filtered.";
        v1file[12]= "Values used when parameter or data value is unknown/unspecified:   -999, -999.0";
        v1file[13]= " 100 Integer-header values follow on  10 lines, Format= (10I8)";
        v1file[14]= "       1       1       4     120       1    -999    -999       0    -999    -999";
        v1file[15]= "    -999    -999    -999       2    -999       1    -999    -999       3    -999";
        v1file[16]= "    -999    -999       3    -999       6    -999    -999       6    -999    -999";
        v1file[17]= "    -999    -999       3       3      24      24    -999    -999    -999    2014";
        v1file[18]= "      62       9       3      20      39       5       5    -999    -999       2";
        v1file[19]= "       2       0       0     360    -999    -999    -999    -999    -999       0";
        v1file[20]= "    -999    -999    -999    -999    -999    -999    -999    -999    -999    -999";
        v1file[21]= "    -999    -999    -999    -999    -999    -999    -999    -999    -999    -999";
        v1file[22]= "    -999    -999    -999    -999    -999    -999    -999    -999    -99915481673";
        v1file[23]= "    -999    -999    -999    -999    -999    -999    -999    -999    -999    -999";
        v1file[24]= " 100 Real-header values follow on  20 lines, Format= (5E15.6)";
        v1file[25]= "   3.349470e+01  -1.166022e+02   1.164000e+03  -9.990000e+02  -9.990000e+02";
        v1file[26]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02   3.393250e+01";
        v1file[27]= "  -1.179172e+02   4.770000e+00  -9.990000e+02  -9.990000e+02   5.100000e+00";
        v1file[28]= "  -9.990000e+02   1.306240e+02   3.381199e+02  -9.990000e+02  -9.990000e+02";
        v1file[29]= "  -9.990000e+02   2.380000e+00  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[30]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02   4.896830e+01";
        v1file[31]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02   1.000000e-02   3.071900e+02";
        v1file[32]= "  -1.711632e+00  -9.990000e+02  -9.990000e+02  -9.990000e+02   0.000000e+00";
        v1file[33]= "   0.000000e+00   1.277920e+01   0.000000e+00   0.000000e+00  -9.990000e+02";
        v1file[34]= "  -9.990000e+02   1.000000e+00  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[35]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[36]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[37]= "  -9.990000e+02   1.000000e+05   3.071900e+02   8.369725e-01   3.335000e+01";
        v1file[38]= "  -4.653877e-14  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[39]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[40]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[41]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[42]= "  -9.990000e+02  -9.990000e+02   1.826392e-04  -9.990000e+02   5.369411e+00";
        v1file[43]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[44]= "  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02  -9.990000e+02";
        v1file[45]= "   2 Comment line(s) follow, each starting with a \"|\":";
        v1file[46]= "|";
        v1file[47]= "|<PROCESS> AUTO";
        v1file[48]= "      16 acceleration pts, approx  307 secs, units=cm/sec2(04), Format=(6E12.3)";
        v1file[49]= "   1.197e-04   3.024e-04   6.676e-04   4.850e-04   4.850e-04   8.503e-04";
        v1file[50]= "   1.033e-03   6.676e-04   4.850e-04   6.676e-04   4.850e-04   6.676e-04";
        v1file[51]= "   1.216e-03   1.216e-03   3.024e-04   1.033e-03";
        v1file[52]= "End-of-data for Chan  2 acceleration";
        
    }
    
    @Test
    public void constructorTest() throws FormatException, SmException {
        v1a = new V1Component(UNCORACC);
        org.junit.Assert.assertEquals(null, v1a.getParent());
        v1a.loadComponent(0, v1file);
        org.junit.Assert.assertEquals(16,v1a.getDataLength());
        double[] test = v1a.getDataArray();
        org.junit.Assert.assertEquals(16, test.length);
         
        v1 = new V1Component(UNCORACC, v0);
        org.junit.Assert.assertEquals(0, v1.getDataLength());
        org.junit.Assert.assertEquals(v0, v1.getParent());
        org.junit.Assert.assertEquals(v0.getEndOfData(), v1.getEndOfData());
     }
    
    @Test
    public void buildV1Test() throws SmException, FormatException {
        V1Process v1val = new V1Process(v0);
        v1val.processV1Data();
        V1Component v1rec = new V1Component( UNCORACC, v0);
        v1rec.buildV1(v1val);        
        String[] text = v1rec.getTextHeader();
        org.junit.Assert.assertEquals(true, text[0].startsWith(UNCORACC));
        org.junit.Assert.assertEquals(V1_STAGE, v1rec.getIntHeaderValue(PROCESSING_STAGE_INDEX));
        String test1 = v1rec.getEndOfData();
        String test2 = v0.getEndOfData();
        org.junit.Assert.assertEquals(true, test1.equalsIgnoreCase(test2));
    }
    
    @Test
    public void V1toTextTest() throws SmException, FormatException {
        V1Process v1val = new V1Process(v0);
        v1val.processV1Data();
        V1Component v1rec = new V1Component( UNCORACC, v0);
        v1rec.buildV1(v1val);        
        String[] text = v1rec.VrecToText();
        org.junit.Assert.assertEquals(69, text.length); //for double formatting change
        org.junit.Assert.assertEquals(v0file[17],text[17]);
        org.junit.Assert.assertEquals(true, text[0].startsWith(UNCORACC));
        org.junit.Assert.assertEquals(v0file[v0file.length-1],(text[text.length-1]));
    }
    
    @Test
    public void BuildNewDataFormatLineTest() throws SmException, FormatException {
        V1Process v1val = new V1Process(v0);
        v1val.processV1Data();
        V1Component v1rec = new V1Component( UNCORACC, v0);
        v1rec.buildV1(v1val);
        v1rec.buildNewDataFormatLine("xxyyz", 25);
        String test = v1rec.getDataFormatLine();
        CharSequence seq = "xxyyz(25)";
        org.junit.Assert.assertEquals(true, test.contains(seq) );
    }
    
    @Test
    public void updateDataTest() throws FormatException, SmException {
        ZonedDateTime newtime = ZonedDateTime.parse("2015-03-29T12:26:10Z[UTC]");
        ZonedDateTime extratime = newtime.plusNanos(123456000);
        String teststring = "2015/03/29 12:26:10.123 UTC";
        v1a = new V1Component(UNCORACC);
        v1a.loadComponent(0, v1file);
        String checkline = v1a.updateArray(newArray, extratime, 10, 100);
        
        org.junit.Assert.assertEquals(arrlen,v1a.getDataLength());
        double[] test = v1a.getDataArray();
        double[] testnomean = new double[newArray.length];
        System.arraycopy(saveArray,0,testnomean,0,saveArray.length);
        double meanToZero = ArrayOps.findAndRemoveMean(testnomean);
        org.junit.Assert.assertArrayEquals(testnomean, test, EPSILON);
        
        ArrayStats stat = new ArrayStats( testnomean );
        double avgVal = stat.getMean();
        double peakVal = stat.getPeakVal();
        int peakIndex = stat.getPeakValIndex();
        double delta_t = v1a.getRealValue(DELTA_T);
        double seriesLength = delta_t * test.length * MSEC_TO_SEC;
        double ptime = peakIndex * MSEC_TO_SEC * delta_t;
        
        org.junit.Assert.assertEquals( avgVal, v1a.getRealValue(AVG_VAL), EPSILON);
        org.junit.Assert.assertEquals( peakVal, v1a.getRealValue(PEAK_VAL), EPSILON);
        org.junit.Assert.assertEquals( ptime, v1a.getRealValue(PEAK_VAL_TIME), EPSILON);
        org.junit.Assert.assertEquals( meanToZero, v1a.getRealValue(MEAN_ZERO), EPSILON);
        org.junit.Assert.assertEquals( seriesLength, v1a.getRealValue(SERIES_LENGTH), EPSILON);
        
        org.junit.Assert.assertEquals( 2015, v1a.getIntValue(START_TIME_YEAR));
        org.junit.Assert.assertEquals( 88, v1a.getIntValue(START_TIME_JULDAY));
        org.junit.Assert.assertEquals( 3, v1a.getIntValue(START_TIME_MONTH));
        org.junit.Assert.assertEquals( 29, v1a.getIntValue(START_TIME_DAY));
        org.junit.Assert.assertEquals( 12, v1a.getIntValue(START_TIME_HOUR));
        org.junit.Assert.assertEquals( 26, v1a.getIntValue(START_TIME_MIN));
        org.junit.Assert.assertEquals( 10.123456, v1a.getRealValue(START_TIME_SEC),EPSILON);
    }
}
