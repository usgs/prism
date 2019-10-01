/*******************************************************************************
 * Name: Java class ConfigReader.java
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

package SmUtilities;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a collection to hold the configuration file parameters.
 * It wraps a Map collection that uses keys and values to store data and retrieve
 * by key.  It is set up according to the singleton design pattern so the keys
 * and values can be loaded once and accessed by different classes as needed.
 * The actual reading in of the values from the file is done with the 
 * PrismXMLReader, which then loads the keys and values into the ConfigReader.
 * @author jmjones
 */
public class ConfigReader {
    private final Map<String, String> contents;
    public final static ConfigReader INSTANCE = new ConfigReader();
/**
 * Constructor for the configuration file reader is private as part of the
 * singleton implementation.  Access to the reader is through the INSTANCE 
 * variable:  ConfigReader config = ConfigReader.INSTANCE.
 */
    private ConfigReader() {
        contents = new HashMap<>();
    }
/**
 * Getter for the value stored for the given key.
 * @param key The key associated with the key-value pair
 * @return The value for the given key
 */
    public String getConfigValue(String key) {
        return contents.get(key);
    }
/**
 * Setter for a key-value pair
 * @param key The key for storage and retrieval
 * @param value The value to store
 */
    public void setConfigValue(String key, String value) {
        String line = contents.put(key, value);
    }
}
