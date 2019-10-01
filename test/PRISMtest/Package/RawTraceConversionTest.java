/*******************************************************************************
 * Name: Java class RawTraceConversionTest.java
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

import SmProcessing.RawTraceConversion;
import org.junit.Test;

/**
 *
 * @author jmjones
 */
public class RawTraceConversionTest {
    private final double EPSILON = 0.000001;
    
    public RawTraceConversionTest() {
    }
    

    @Test
    public void testCountToG() {
        double result = RawTraceConversion.countToG(500000.0, 2.0);
        org.junit.Assert.assertEquals(result, 0.25, EPSILON);
    }
    public void testCountToCMS() {
        double result = RawTraceConversion.countToCMS(500000.0, 2.0, 5.0);
        org.junit.Assert.assertEquals(result, 1.25, EPSILON);
    }
}
