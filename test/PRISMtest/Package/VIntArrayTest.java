/*******************************************************************************
 * Name: Java class VIntArrayTest.java
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

import COSMOSformat.VIntArray;
import SmException.FormatException;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * JUnit test for VIntArray and COSMOSarrayFormat
 * @author jmjones
 */
public class VIntArrayTest {
    String[] header;
    String[] data;
    VIntArray hi;
    VIntArray di;
    String[] headerbits;
    String badFormat;
    String noFormat;
    String badNumbers;
    String[] badData;
    
    public VIntArrayTest() {
        this.header = new String[3];
        this.data = new String[4];
        this.headerbits = new String[20];
        this.badData = new String[2];
        this.badFormat = "";
        this.noFormat = "";
        this.badNumbers = "";
    }
    
    @Before
    public void setUp() throws FormatException {
        header[0] = "  20 Integer-header values follow on  2 lines, Format= (10I8)";
        header[1] = "       0       1      50     120       1    -999    -999   13921    -999    -999";
        header[2] = "       5       5       5       5    -999       1    -999    -999       6     360";
        
        data[0] = "      30 acceleration pts, approx  56 secs, units=counts (50),Format=(10I8)";
        data[1] = "    3284    3334    3296    3284    3308    3242    3236    3324    3322    3262";
        data[2] = "    3300    3334    3302    3266    3322    3336    3312    3298    3254    3346";
        data[3] = "    3318    3258    3364    3290    3216    3302    3304    3310    3318    3256";
        
        badFormat = "      30 acceleration pts, approx  56 secs, units=counts (50),Format=(I8)";
        badData[0] = "      10 acceleration pts, approx  56 secs, units=counts (50),Format=(10I8)";
        badData[1] = "    3284    3334    3296    3284    abcd    3242    3236    3324    3322    3262";
        
        noFormat = "      30 acceleration pts, approx  56 secs, units=counts (50),Format=";
        badNumbers = "      10 acceleration pts, approx  56 secs, units=counts (50),Format=(a5I8)";
        
        headerbits[0] = "       0";
        headerbits[1] = "       1";
        headerbits[2] = "      50";
        headerbits[3] = "     120";
        headerbits[4] = "       1";
        headerbits[5] = "    -999";
        headerbits[6] = "    -999";
        headerbits[7] = "   13921";
        headerbits[8] = "    -999";
        headerbits[9] = "    -999";
        headerbits[10] = "       5";
        headerbits[11] = "       5";
        headerbits[12] = "       5";
        headerbits[13] = "       5";
        headerbits[14] = "    -999";
        headerbits[15] = "       1";
        headerbits[16] = "    -999";
        headerbits[17] = "    -999";
        headerbits[18] = "       6";
        headerbits[19] = "     360";
        hi = new VIntArray();
        di = new VIntArray();
    }
    @Test
    public void testConstructor()  {
        org.junit.Assert.assertEquals(8, hi.getFieldWidth());
        org.junit.Assert.assertEquals("I", hi.getDisplayType());
    }
    
    @Test
    public void testFormatLineH() throws FormatException {
        hi.parseNumberFormatLine(header[0]);
        org.junit.Assert.assertEquals(20, hi.getNumVals());
        org.junit.Assert.assertEquals("(10I8)", hi.getNumberFormat());
        org.junit.Assert.assertEquals(10, hi.getValsPerLine());
        org.junit.Assert.assertEquals(8, hi.getFieldWidth());
    }
    @Test
    public void testFormatLineD() throws FormatException {
        di.parseNumberFormatLine(data[0]);
        org.junit.Assert.assertEquals(30, di.getNumVals());
        org.junit.Assert.assertEquals("(10I8)", di.getNumberFormat());
        org.junit.Assert.assertEquals(10, di.getValsPerLine());
        org.junit.Assert.assertEquals(8, di.getFieldWidth());
    }
    @Test
    public void testParseValuesH() throws FormatException {
        hi.parseValues(0, header);
        org.junit.Assert.assertEquals(20, hi.getNumVals());
        org.junit.Assert.assertEquals("(10I8)", hi.getNumberFormat());
        org.junit.Assert.assertEquals(10, hi.getValsPerLine());
        org.junit.Assert.assertEquals(8, hi.getFieldWidth());
        org.junit.Assert.assertEquals(2, hi.getNumLines());
        org.junit.Assert.assertEquals(0, hi.getIntValue(0));
        org.junit.Assert.assertEquals(360, hi.getIntValue(19));
    }
    @Test
    public void testParseValuesD() throws FormatException {
        di.parseValues(0, data);
        org.junit.Assert.assertEquals(30, di.getNumVals());
        org.junit.Assert.assertEquals("(10I8)", di.getNumberFormat());
        org.junit.Assert.assertEquals(10, di.getValsPerLine());
        org.junit.Assert.assertEquals(8, di.getFieldWidth());
        org.junit.Assert.assertEquals(3, di.getNumLines());
        org.junit.Assert.assertEquals(3284, di.getIntValue(0));
        org.junit.Assert.assertEquals(3256, di.getIntValue(29));
    }
    @Test
    public void testSetIntValue() throws FormatException {
        di.parseValues(0, data);
        di.setIntValue(5, 333);
        org.junit.Assert.assertEquals(333, di.getIntValue(5));
    }
    @Test
    public void testGetIntArray() throws FormatException {
        hi.parseValues(0, header);
        int[] array = new int[hi.getNumVals()];
        array = hi.getIntArray();
        org.junit.Assert.assertEquals(array[7], 13921);
    }
    @Test
    public void testArrayToText() throws FormatException {
        hi.parseNumberFormatLine(header[0]);
        hi.parseValues(0, header);
        ArrayList<String> textList = hi.arrayToText();
        org.junit.Assert.assertArrayEquals(headerbits, textList.toArray(new String[textList.size()]));
    }
    @Test
    public void testCopyConstructor() throws FormatException {
        hi.parseNumberFormatLine(header[0]);
        hi.parseValues(0, header);
        VIntArray test = new VIntArray( hi );
        org.junit.Assert.assertEquals(hi.getNumVals(), test.getNumVals());
        org.junit.Assert.assertEquals(hi.getNumberFormat(), test.getNumberFormat());
        org.junit.Assert.assertEquals(hi.getValsPerLine(), test.getValsPerLine());
        org.junit.Assert.assertEquals(hi.getFieldWidth(), test.getFieldWidth());
        org.junit.Assert.assertEquals(hi.getNumLines(), test.getNumLines());
        org.junit.Assert.assertEquals(hi.getIntValue(0), test.getIntValue(0));
        org.junit.Assert.assertEquals(hi.getIntValue(19), test.getIntValue(19));
    }
    @Test
    public void testNumberSectionToTextH() throws FormatException {
        hi.parseNumberFormatLine(header[0]);
        hi.parseValues(0, header);
        String[] textList = hi.numberSectionToText();
        org.junit.Assert.assertArrayEquals(header, textList);
    }
    @Test
    public void testNumberSectionToTextD() throws FormatException {
        di.parseNumberFormatLine(data[0]);
        di.parseValues(0, data);
        String[] textList = di.numberSectionToText();
        org.junit.Assert.assertArrayEquals(data, textList);
    }
    @Test(expected=FormatException.class)
    public void testBadFormatLine() throws FormatException {
        di.parseNumberFormatLine(badFormat);
    }
    @Test(expected=FormatException.class)
    public void testNoFormatLine() throws FormatException {
        di.parseNumberFormatLine(noFormat);
    }
    @Test(expected=FormatException.class)
    public void testBadNumbersLine() throws FormatException {
        di.parseNumberFormatLine(badNumbers);
    }
    @Test(expected=FormatException.class)
    public void testBadDataValue() throws FormatException {
        di.parseValues(0, badData);
    }
    @Rule public ExpectedException expectedEx = ExpectedException.none();
    @Test
    public void testIndexRangeLow() throws IndexOutOfBoundsException, FormatException {
        expectedEx.expect(IndexOutOfBoundsException.class);
        expectedEx.expectMessage("Integer array index: -1");
        di.parseValues(0, data);
        di.getIntValue(-1);
    }
    @Test(expected=IndexOutOfBoundsException.class)
    public void testIndexRangeHigh() throws IndexOutOfBoundsException, FormatException {
        di.parseValues(0, data);
        di.setIntValue(300,23);
    }
    @Test
    public void testEOF() throws NumberFormatException, FormatException {
        expectedEx.expect(FormatException.class);
        expectedEx.expectMessage("Unexpected EOF encountered at line 6");        
        di.parseValues(6, data);
    }
}
