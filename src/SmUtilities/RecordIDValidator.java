/*******************************************************************************
 * Name: Java class SmProduct.java
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

import static SmConstants.VFileConstants.MAX_LINE_LENGTH;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates the format of the record id before its use in building directory names
 * and recording in the apktable
 */
public class RecordIDValidator {
    private final boolean valid;
    private final String[] sections;
    
    public RecordIDValidator(String id) {
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        String pat = sb.append("^")
                       .append("(\\w+)")
                       .append("(\\.)")
                       .append("(\\w+)")
                       .append("(\\.)")
                       .append("(\\w+)")
                       .append("(\\.)")
                       .append("(\\w+)")
                       .append("(\\.)")
                       .append("(\\w+)")
                       .append("(\\.)")
                       .append("[\\w-]+")
                       .append("$")
                       .toString();
        Pattern officialname = Pattern.compile(pat);
        Matcher m = officialname.matcher(id);
        valid = m.matches();
        sections = (valid) ? id.split("\\.") : null;            
    }
    public boolean isValidRcrdID() {
        return valid;
    }
    public String getEventID() {
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        String event = "";
        if (valid) {
            event = sb.append(sections[0]).append(".").append(sections[1]).toString();
        }
        return event; 
    }
    public String getStationID() {
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        String station = "";
        if (valid) {
            station = sb.append(sections[2]).append(".").append(sections[3]).toString();
        }
        return station;         
    }
}
