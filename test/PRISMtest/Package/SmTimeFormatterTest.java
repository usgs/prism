/*******************************************************************************
 * Name: Java class SmTimeFormatterTest.java
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

import SmUtilities.SmTimeFormatter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmjones
 */
public class SmTimeFormatterTest {
    ZonedDateTime newtime, extratime;
    String teststring;
    
    public SmTimeFormatterTest() {
    }
    
    @Before
    public void setUp() {
        newtime = ZonedDateTime.parse("2015-03-29T12:26:10Z[UTC]");
        extratime = newtime.plusNanos(123456000);
        teststring = "2015/03/29 12:26:10.123 UTC";
    }

    @Test
    public void testGMTtime() {
        SmTimeFormatter gmt = new SmTimeFormatter();
//        System.out.println(gmt.getGMTdateTime());
    }
    @Test
    public void testUTCtime() {
        SmTimeFormatter utc = new SmTimeFormatter(extratime);
        org.junit.Assert.assertEquals(2015,utc.getUTCyear());
        org.junit.Assert.assertEquals(3, utc.getUTCmonth());
        org.junit.Assert.assertEquals(29, utc.getUTCday());
        org.junit.Assert.assertEquals(12, utc.getUTChour());
        org.junit.Assert.assertEquals(26, utc.getUTCminute());
        org.junit.Assert.assertEquals(10, utc.getUTCjustSecond());
        org.junit.Assert.assertEquals(123456000, utc.getUTCjustNano());
        org.junit.Assert.assertEquals(10.123456, utc.getUTCsecond(),0.000001);
        org.junit.Assert.assertEquals(teststring, utc.getUTCdateTime());
    }
}
