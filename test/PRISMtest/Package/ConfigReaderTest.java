/*******************************************************************************
 * Name: Java class ConfigReaderTest.java
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

import SmUtilities.ConfigReader;
import org.junit.Test;

/**
 * JUnit test class for ConfigReader in SmUtilities
 * @author jmjones
 */
public class ConfigReaderTest {
    ConfigReader config = ConfigReader.INSTANCE;
    
    public ConfigReaderTest() {
    }
        
    @Test
    public void TestSetConfigValue() {
        config.setConfigValue("a", "1");
        config.setConfigValue("b", "2");
        config.setConfigValue("c", "3");
        config.setConfigValue("d", "4");
        org.junit.Assert.assertEquals("1", config.getConfigValue("a"));
        org.junit.Assert.assertEquals("2", config.getConfigValue("b"));
        org.junit.Assert.assertEquals("3", config.getConfigValue("c"));
        org.junit.Assert.assertEquals("4", config.getConfigValue("d"));
    }
    @Test
    public void TestGetConfigValue() {
        config.setConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyCode", "2");
        config.setConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyFullName", "U.S. Geological Survey");
        config.setConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyAbbreviation", "USGS");
        config.setConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyIRISCode", "NP");
        config.setConfigValue("PRISM/OutputFileFormat", "Single");
        config.setConfigValue("PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitCode", "04");
        config.setConfigValue("PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitName", "cm/sec/sec");        

        org.junit.Assert.assertEquals("2", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyCode"));
        org.junit.Assert.assertEquals("U.S. Geological Survey", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyFullName"));
        org.junit.Assert.assertEquals("USGS", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyAbbreviation"));
        org.junit.Assert.assertEquals("NP", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyIRISCode"));
        org.junit.Assert.assertEquals("Single", config.getConfigValue("PRISM/OutputFileFormat"));
        org.junit.Assert.assertEquals("04", config.getConfigValue("PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitCode"));
        org.junit.Assert.assertEquals("cm/sec/sec", config.getConfigValue("PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitName"));
    }
    @Test
    public void TestSingleton() {
        ConfigReader con1 = ConfigReader.INSTANCE;
        ConfigReader con2 = ConfigReader.INSTANCE;
        org.junit.Assert.assertEquals(true, (con1 == con2));
        con1.setConfigValue("a", "1");
        org.junit.Assert.assertEquals("1", con2.getConfigValue("a"));
    }
}
