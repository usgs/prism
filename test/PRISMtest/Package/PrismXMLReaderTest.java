/*******************************************************************************
 * Name: Java class PrismXMLReaderTest.java
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
import SmUtilities.PrismXMLReader;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author jmjones
 */
public class PrismXMLReaderTest {
    ConfigReader config = ConfigReader.INSTANCE;
    String filename = "/PRISMtest/Data/prism_config.xml";
    
    public PrismXMLReaderTest() {
    }
    @Test
    public void TestReadFile() throws ParserConfigurationException, IOException, SAXException {
        InputStream ins = PrismXMLReaderTest.class.getResourceAsStream(filename);
        PrismXMLReader xml = new PrismXMLReader();
        xml.readFile(ins);
        org.junit.Assert.assertEquals("2", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyCode"));
        org.junit.Assert.assertEquals("U.S. Geological Survey", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyFullName"));
        org.junit.Assert.assertEquals("USGS", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyAbbreviation"));
        org.junit.Assert.assertEquals("NP", config.getConfigValue("PRISM/ProcessingAgency/StrongMotionNetworkCode/AgencyIRISCode"));
        org.junit.Assert.assertEquals("04", config.getConfigValue("PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitCode"));
        org.junit.Assert.assertEquals("cm/sec2", config.getConfigValue("PRISM/DataUnitsForCountConversion/DataUnitCodes/DataUnitName"));    

        org.junit.Assert.assertEquals("SingleColumn", config.getConfigValue("PRISM/OutputArrayFormat"));
        org.junit.Assert.assertEquals("0.1", config.getConfigValue("PRISM/QCparameters/InitialVelocity"));
        org.junit.Assert.assertEquals("0.1", config.getConfigValue("PRISM/QCparameters/ResidualVelocity"));
        org.junit.Assert.assertEquals("0.1", config.getConfigValue("PRISM/QCparameters/ResidualDisplacement"));
        org.junit.Assert.assertEquals("4", config.getConfigValue("PRISM/BandPassFilterParameters/BandPassFilterOrder"));
        org.junit.Assert.assertEquals("3.0", config.getConfigValue("PRISM/BandPassFilterParameters/BandPassTaperLength"));    

        org.junit.Assert.assertEquals("20.0", config.getConfigValue("PRISM/BandPassFilterParameters/BandPassFilterCutoff/CutoffHigh"));
        org.junit.Assert.assertEquals("0.1", config.getConfigValue("PRISM/BandPassFilterParameters/BandPassFilterCutoff/CutoffLow"));
        org.junit.Assert.assertEquals("5", config.getConfigValue("PRISM/StrongMotionThreshold"));
        org.junit.Assert.assertEquals("0.0", config.getConfigValue("PRISM/EventOnsetBufferAmount"));
        org.junit.Assert.assertEquals("PWD", config.getConfigValue("PRISM/EventDetectionMethod"));
        org.junit.Assert.assertEquals("No", config.getConfigValue("PRISM/DeleteInputV0"));    

        org.junit.Assert.assertEquals("On", config.getConfigValue("PRISM/DebugToLog"));
        org.junit.Assert.assertEquals("Off", config.getConfigValue("PRISM/WriteBaselineFunction"));
        org.junit.Assert.assertEquals("1", config.getConfigValue("PRISM/AdaptiveBaselineCorrection/FirstPolyOrder/LowerLimit"));
        org.junit.Assert.assertEquals("2", config.getConfigValue("PRISM/AdaptiveBaselineCorrection/FirstPolyOrder/UpperLimit"));
        org.junit.Assert.assertEquals("1", config.getConfigValue("PRISM/AdaptiveBaselineCorrection/ThirdPolyOrder/LowerLimit"));
        org.junit.Assert.assertEquals("3", config.getConfigValue("PRISM/AdaptiveBaselineCorrection/ThirdPolyOrder/UpperLimit")); 
        
        org.junit.Assert.assertEquals("Yes", config.getConfigValue("PRISM/DespikeInput"));
        org.junit.Assert.assertEquals("4", config.getConfigValue("PRISM/DespikingStdevLimit"));
        org.junit.Assert.assertEquals("Table", config.getConfigValue("PRISM/FilterCornerMethod"));
        org.junit.Assert.assertEquals("3", config.getConfigValue("PRISM/SignalToNoiseRatio"));
        org.junit.Assert.assertEquals("No", config.getConfigValue("PRISM/PGAcheck"));
        org.junit.Assert.assertEquals("0.5", config.getConfigValue("PRISM/PGAThreshold"));
        
        org.junit.Assert.assertEquals("5", config.getConfigValue("PRISM/StrongMotionThreshold"));
        org.junit.Assert.assertEquals("Freq", config.getConfigValue("PRISM/IntegrationMethod"));
        org.junit.Assert.assertEquals("5", config.getConfigValue("PRISM/DifferentiationOrder"));
        org.junit.Assert.assertEquals("Yes", config.getConfigValue("PRISM/DecimateResampledOutput"));
        org.junit.Assert.assertEquals("Full", config.getConfigValue("PRISM/ApktableSaValues"));
    }
    
    @Rule public ExpectedException expectedEx = ExpectedException.none();
    @Test
    public void TestExceptionOnRead()  throws ParserConfigurationException, 
                                            IOException, SAXException,SAXParseException {
        expectedEx.expect(IllegalArgumentException.class);
        String badfilename = "/PRISMtest/Data/bad_name.xml";
        InputStream ins = PrismXMLReaderTest.class.getResourceAsStream(badfilename);
        PrismXMLReader xml = new PrismXMLReader();
        xml.readFile(ins);
    }
    @Test
    public void TestExceptionOnSchema()  throws ParserConfigurationException, 
                                    IOException, SAXException,SAXParseException {
        expectedEx.expect(IOException.class);
        String badfilename = "/PRISMtest/Data/bad_schema.xml";
        InputStream ins = PrismXMLReaderTest.class.getResourceAsStream(badfilename);
        PrismXMLReader xml = new PrismXMLReader();
        xml.readFile(ins);
    }
    @Test
    public void TestEmptyOnRead()  throws ParserConfigurationException, IOException, 
                                                    SAXException,SAXParseException {
        org.junit.Assert.assertEquals(null, config.getConfigValue("NoSuchTagInTheFile"));    
    }    
}
