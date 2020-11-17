/*******************************************************************************
 * Name: Java class FilterCornerReader.java
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a collection to hold the station filter corners.  It
 * performs some basic checks on the input filter corners: (1) both values are
 * greater than 0.0, (2) low corner is less than high corner, (3) key is not null,
 * (4) key length is greater than 0.
 * It wraps a Map collection that uses keys and values to store data and retrieve
 * by key.  It is set up according to the singleton design pattern so the keys
 * and values can be loaded once and accessed by different classes as needed.
 * @author jmjones
 */
public class FilterCornerReader {
    private final Map<String, double[]> contents;
    public final static FilterCornerReader INSTANCE = new FilterCornerReader();
    PrismLogger log = PrismLogger.INSTANCE;
    
    /**
     * Constructor for the filter corner file reader is private as part of the
     * singleton implementation.  Access to the reader is through the INSTANCE 
     * variable:  FilterCornerReader corner = FilterCornerReader.INSTANCE.
     */
    private FilterCornerReader() {
        contents = new HashMap<>(100);
    }
    /**
     * Returns the number of key-value pairs in the reader
     * @return number of key-value pairs
     */
    public int size() {
        return contents.size();
    }
    /**
     * Check for an empty reader
     * @return true if no key-value pairs in the reader
     */
    public boolean isEmpty() {
        return contents.isEmpty();
    }
    /**
     * Check for existence of a key in the reader
     * @param key string key to check for in the reader
     * @return true if key exists in the reader
     */
    public boolean containsKey(String key) {
        return contents.containsKey(key);
    }
    /**
     * Clears the contents of the reader
     */
    public void clear() {
        contents.clear();
    }
    /**
     * Getter for the value stored for the given key.
     * @param key The key associated with the key-value pair
     * @return The value for the given key, which is [low corner, high corner]
     */
    public double[] getCornerValues(String key) {
        double[] empty = {};
        if (contents.containsKey(key)) {
            return contents.get(key);
        } else {
            return empty;
        }
    }
    
    /**
     * Setter for the key-value pair in the hashmap
     * @param key string containing the SNCL code (cannot = null)
     * @param endvals double array with [low corner, high corner], where low and
     * high corner are both greater than 0 and low corner is less than high corner
     * @return true if key-value pair accepted, false if not
     */
    public boolean setCornerValues(String key, double[] endvals){
        boolean check = false;
        if ((key != null) && (key.length() > 0) && (endvals[0] > 0.0) &&  
                             (endvals[1] > 0.0) && (endvals[0] < endvals[1])) {
            contents.put(key, endvals);
            check = true;
        }
        return check;
    }
    /**
     * Reads in the filter corner table from the given filename and loads it
     * into the hashmap.  Expected file format is SNCL code: lowval,highval
     * where all characters up to the semi-colon are used as the hash key.  After
     * the semi-colon there should be 2 real numbers separated by a comma.
     * Lines beginning with # are ignored as comments, and blank lines are ignored.
     * If an individual line can't be parsed, it is skipped over.
     * @param inname text string containing the full path to the corner table file
     * @throws IOException if unable to read in the file
     */
    public void loadFilterCorners(String inname) throws IOException {
        String[] filecontents;
        
        // Write the start and finish of the file parsing in the log file since
        // any lines that fail parsing are written to the log file.
        String[] startlines = new String[]{"Reading station filter corner table"};
        String[] endlines = new String[]{"Filter corner table read complete","\n"};
        
        File infilename = new File(inname);
        TextFileReader reader = new TextFileReader(infilename);
        filecontents = reader.readInTextFile();
        log.writeToLog(startlines);
        for (String line : filecontents) {
            parseline(line);
        }
        log.writeToLog(endlines);
    }
    /**
     * Parses an individual line of the station filter corners table.  Looks for
     * lines that are blank or comments and skips.  Extracts the SNCL key and
     * corner values and enters these as a key-value pair into the hashmap.
     * Conversion errors during text-to-double conversion cause the line to
     * be skipped with a note written to the log file.
     * @param inline an individual line from the corners file
     * @throws IOException if unable to write to the log file
     */
    private void parseline( String inline ) throws IOException {
        String[] loglines = new String[1];
        String line = inline.trim();
        double[] endvals = new double[2];
        
        // Skip over comment lines or blank lines
        if ((line.length() == 0) || (line.startsWith("#"))) {
            return;
        }
        // Expecting format SNCL code: lowval,highval
        String[] sections = line.split(":");
        String key = sections[0];
        String[] corners = sections[1].split(",");
        try {
            endvals[0] = Double.parseDouble(corners[0]);
            endvals[1] = Double.parseDouble(corners[1]);
            setCornerValues(key,endvals);
        } catch (NumberFormatException err) {
            // Just log the line that failed and ignore
            loglines[0] = "Unable to convert text to number in " + inline;
            log.writeToLog(loglines);
        }
    }
}
