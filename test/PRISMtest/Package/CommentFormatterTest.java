/*******************************************************************************
 * Name: Java class CommentFormatterTest.java
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

import SmConstants.VFileConstants;
import static SmConstants.VFileConstants.PRISM_ENGINE_VERSION;
import SmUtilities.CommentFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmjones
 */
public class CommentFormatterTest {
    String[] test1;
    String[] test2;
    String[] testresamp, testresampdec;
    String[] testtrim;
    String[] empty;
    String[] incomments;
    String[] despike;
    
    public CommentFormatterTest() {
        incomments = new String[5];
        incomments[0] = "   4 Comment line(s) follow, each starting with a |";
        incomments[1] = "| Station Name: UC Hastings Preserve, Carmel Valley, CA, USA";
        incomments[2] = "| Sensor: Kinemetrics FBA ES-T Accel. (2 g max 10 v/g)";
        incomments[3] = "| RcrdId: NC.72316031.BK.HAST.HNE.00";
        incomments[4] = "|<SCNL>HAST.HNE.BK.00    <AUTH> 2015/03/21 22:41:18.000";
        
        test1 = new String[4];
        test1[0] = "|<PROCESS> Manually processed using PRISM version v1.2.3";
        test1[1] = "|<EONSET> event onset(sec)=  31.9800";
        test1[2] = "|<ABLC>SF:   0.0000, EF:  31.9800, SA:   0.0000, EA: 132.0000, ORDER:  MEAN";
        test1[3] = "|<VBLC>SF:   0.0000, EF: 132.0000, SA:   0.0000, EA: 132.0000, ORDER:ORDER2";
        
        test2 = new String[6];
        test2[0] = "|<PROCESS> Automatically processed using PRISM version v1.2.3";
        test2[1] = "|<EONSET> event onset(sec)=   5.0000";
        test2[2] = "|<ABLC>SF:   0.0000, EF:   5.0000, SA:   0.0000, EA: 124.0000, ORDER:  MEAN";
        test2[3] = "|<VBLABC1>SF:   0.0000, EF:  12.0000, SA:   0.0000, EA:  12.0000, ORDER:ORDER1";
        test2[4] = "|<VBLABC2>SF:  12.0000, EF:  24.0000, SA:  12.0000, EA:  24.0000, ORDER:ORDER2";
        test2[5] = "|<VBLABC3>SF:  24.0000, EF: 124.0000, SA:  24.0000, EA: 124.0000, ORDER:ORDER3";
        
        empty = new String[0];

        testresamp = new String[3];
        testresamp[0] = "|<PROCESS> Automatically processed using PRISM version v1.2.3";
        testresamp[1] = "|<RESAMPLE> Data resampled to 200.00 samples/sec";
        testresamp[2] = "|<EONSET> event onset(sec)=  15.3200";

        testresampdec = new String[4];
        testresampdec[0] = "|<PROCESS> Automatically processed using PRISM version v1.2.3";
        testresampdec[1] = "|<RESAMPLE> Data resampled to 200.00 samples/sec";
        testresampdec[2] = "|<EONSET> event onset(sec)=  15.3200";
        testresampdec[3] = "|<DECIMATE> Data decimated to 100.00 samples/sec";
        
        testtrim = new String[2];
        testtrim[0] = "|<PROCESS> Manually processed using PRISM version v1.2.3";
        testtrim[1] = "|<TRIM> 260 samp. of beginning, 400 samp. of end of original channel";
        
        despike = new String[3];
        despike[0] = "|<PROCESS> Automatically processed using PRISM version v1.2.3";
        despike[1] = "|<DESPIKE> 2 spike(s) removed during V1 processing";
        despike[2] = "|<EONSET> event onset(sec)=  15.3200";
    }
    private String[] mergeStrings( String[] first, String[] second) {
        ArrayList<String> text1 = new ArrayList<>(Arrays.asList(first));
        ArrayList<String> text2 = new ArrayList<>(Arrays.asList(second));
        text1.addAll(text2);
        StringBuilder sb = new StringBuilder();
        String start = text1.get(0);
        sb.append(String.format("%4d",(text1.size()-1)))
                .append(start.substring(4, start.length()));
        text1.set(0,sb.toString());
        String[] merged = text1.toArray(new String[text1.size()]);
        text1.clear();
        text2.clear();
        sb.setLength(0);
        return merged;
    }

    @Test
    public void formatterTest1() {
        String[] result;
        CommentFormatter formatter = new CommentFormatter();
        result = formatter.addCorrectionType(incomments, VFileConstants.CorrectionType.MANUAL, "v1.2.3");
        result = formatter.addEventOnset(result, 31.98);
        result = formatter.addBaselineStep(result, 0.0, 31.98,0.0, 132.00, 
                                VFileConstants.V2DataType.ACC, 
                                VFileConstants.BaselineType.BESTFIT, 
                                VFileConstants.CorrectionOrder.MEAN, 0);
        result = formatter.addBaselineStep(result, 0.0, 132.00, 0.0, 132.00,
                                VFileConstants.V2DataType.VEL,
                                VFileConstants.BaselineType.BESTFIT,
                                VFileConstants.CorrectionOrder.ORDER2, 0);
        String[] expected = mergeStrings(incomments,test1);
        org.junit.Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void formatterTest2() {
        String[] result;
        CommentFormatter formatter = new CommentFormatter();
        result = formatter.addCorrectionType(incomments, VFileConstants.CorrectionType.AUTO, "v1.2.3");
        result = formatter.addEventOnset(result, 5.0);
        result = formatter.addBaselineStep(result, 0.0,5.0,0.0,124.0,
                                VFileConstants.V2DataType.ACC, 
                                VFileConstants.BaselineType.BESTFIT, 
                                VFileConstants.CorrectionOrder.MEAN, 0);
        result = formatter.addBaselineStep(result, 0.0, 12.00,0.0, 12.00,
                                VFileConstants.V2DataType.VEL,
                                VFileConstants.BaselineType.ABC,
                                VFileConstants.CorrectionOrder.ORDER1,1);
        result = formatter.addBaselineStep(result, 12.0, 24.00,12.0, 24.00,
                                 VFileConstants.V2DataType.VEL,
                                 VFileConstants.BaselineType.ABC,
                                VFileConstants.CorrectionOrder.ORDER2,2);
        result = formatter.addBaselineStep(result, 24.0, 124.00,24.0, 124.00,
                               VFileConstants.V2DataType.VEL,
                               VFileConstants.BaselineType.ABC,
                                VFileConstants.CorrectionOrder.ORDER3,3);
        String[] expected = mergeStrings(incomments,test2);
        org.junit.Assert.assertArrayEquals(expected, result);
    }
    @Test
    public void testRecorderResample() {
        String[] result;
        CommentFormatter formatter = new CommentFormatter();
        result = formatter.addCorrectionType(incomments, VFileConstants.CorrectionType.AUTO, "v1.2.3");
        result = formatter.addResampling(result,200.00);
        result = formatter.addEventOnset(result,15.32);
        String[] expected = mergeStrings(incomments,testresamp);
        org.junit.Assert.assertArrayEquals(expected, result);
    }
    @Test
    public void testRecorderResampleDecimate() {
        String[] result;
        CommentFormatter formatter = new CommentFormatter();
        result = formatter.addCorrectionType(incomments, VFileConstants.CorrectionType.AUTO, "v1.2.3");
        result = formatter.addResampling(result,200.00);
        result = formatter.addEventOnset(result,15.32);
        result = formatter.addDecimation(result,100.0);
        String[] expected = mergeStrings(incomments,testresampdec);
        org.junit.Assert.assertArrayEquals(expected, result);
    }
    @Test
    public void testRecorderTrim() {
        String[] result;
        CommentFormatter formatter = new CommentFormatter();
        result = formatter.addCorrectionType(incomments,VFileConstants.CorrectionType.MANUAL, "v1.2.3");
        result = formatter.addTrimIndicies(result,260,400);
        String[] expected = mergeStrings(incomments,testtrim);
        org.junit.Assert.assertArrayEquals(expected, result);
    }
    @Test
    public void testDespike() {
        String[] result;
        CommentFormatter formatter = new CommentFormatter();
        result = formatter.addCorrectionType(incomments, VFileConstants.CorrectionType.AUTO, "v1.2.3");
        result = formatter.addSpikeCount(result, 2);
        result = formatter.addEventOnset(result,15.32);
        String[] expected = mergeStrings(incomments,despike);
        org.junit.Assert.assertArrayEquals(expected, result);
    }
}
