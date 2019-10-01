/*******************************************************************************
 * Name: Java class CSVFileWriter.java
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * The CSVFileWriter class creates a new csv file using the data and the headerline
 * if the file with the given filename doesn't exist. If it does exist, the new data
 * line is appended to the end of the file.  If the directory for the csv file
 * doesn't exist, this class will create it.
 * @author jmjones
 */
public class CSVFileWriter {
    private File csvfolder;
    
    /**
     * The CSVFileWriter constructor takes the csv directory path and if the path
     * doesn't exist, creates it.
     * @param infolder directory name for the csv file to be written to
     */
    public CSVFileWriter( File infolder ) {
        csvfolder = infolder;
        if (!csvfolder.isDirectory()) {
            csvfolder.mkdir();
        }
    }
    /**
     * Writes the list out as a CSV file, with the first line containing the column names.
     * If the file already exists, the data line is appended to the file
     * @param msg a list of the parameters for one record
     * @param headerline the column names to write out the first time
     * @param name the name of the file
     * @param time the time of the current processing run
     * @throws IOException if unable to write to the file
     */
    public void writeToCSV( ArrayList<String> msg, String[] headerline, 
                            String name, String time ) throws IOException {
        String[] values;
        String startTime = time.replace("-","_").replace(" ", "_").replace(":","_");
        StringBuilder sbheader = new StringBuilder();
        StringBuilder sbname = new StringBuilder();
        StringBuilder sbmsg = new StringBuilder();
        for (String each : msg) {
            sbmsg.append(each).append(",");
        }
        // Build the CSV file name
        sbmsg.replace(sbmsg.length()-1, sbmsg.length(), "");
        String[] segments = name.split("\\.");
        sbname.append(segments[0]).append("_").append(startTime).append(".").append(segments[1]);
        
        Path outfile = Paths.get(csvfolder.toString(), sbname.toString());
        if (!outfile.toFile().exists()) {
            values = new String[2];
            for (String each : headerline) {
                sbheader.append(each).append(",");
            }
            sbheader.replace(sbheader.length()-1, sbheader.length(), "");
            values[0] = sbheader.toString();
            values[1] = sbmsg.toString();
        } else {
            values = new String[1];
            values[0] = sbmsg.toString();
        }
        TextFileWriter textfile = new TextFileWriter( outfile, values);
        textfile.appendToFile();
        sbheader.setLength(0);
        sbname.setLength(0);
        sbmsg.setLength(0);
    }
}
