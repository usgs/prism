/*******************************************************************************
 * Name: Java class SmDebugLogger.java
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

import SmConstants.VFileConstants.LogType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is a singleton instance of the prism debug logger.  This logger is used
 * to record the processing flow parameters to be used for debugging.
 * This logger is currently set up to create
 * a new logging file with the time appended to the name.
 * @author jmjones
 */
public class SmDebugLogger {
    private Path logfile;
    private Path troublefile;
    private static boolean logReady = false;
    private final String logname = "DebugLog.txt";
    private final String troublename = "TroubleLog.txt";
    public final static SmDebugLogger INSTANCE = new SmDebugLogger();
    private String finalFolder;
    private File logfolder;
    private String startTime;
    /**
     * Constructor for the logger is private as part of the
     * singleton implementation.  Access to the logger is through the INSTANCE 
     * variable:  SmDebugLogger logger = SmDebugLogger.INSTANCE.
     */
    private SmDebugLogger() {
    }
    /**
     * This method initializes the logger and checks if the log folder exists,
     * and if not it creates the log folder in the top level output folder
     * @param outfolder the top level output folder, where the log folder resides
     * @param time appended at the end of the file name
     * @throws IOException if unable to write out a file
     */
    public void initializeLogger( String outfolder, String time ) throws IOException {
        StringBuilder sb = new StringBuilder();
        finalFolder = outfolder;
        startTime = time.replace("-","_").replace(" ", "_").replace(":","_");
        if (!logReady) {
            logfolder = Paths.get(outfolder, "Prism_Logs").toFile();
            if (!logfolder.isDirectory()) {
                logfolder.mkdir();
             }
            String[] segments = logname.split("\\.");
            sb.append(segments[0]).append("_").append(startTime).append(".").append(segments[1]);
            this.logfile = Paths.get(logfolder.toString(),sb.toString());

            segments = troublename.split("\\.");
            sb = new StringBuilder();
            sb.append(segments[0]).append("_").append(startTime).append(".").append(segments[1]);
            this.troublefile = Paths.get(logfolder.toString(),sb.toString());
            logReady = true;
            sb.setLength(0);
        }
    }
    /**
     * Writes the array of text messages out to the log file, appending to the
     * end of the current file.
     * @param msg the list of messages to be written to the log
     * @param logger the type of log file, either debug or trouble
     * @throws IOException if unable to write to the file
     */
    public void writeToLog( String[] msg, LogType logger ) throws IOException {
        if (logReady) {
            if (logger == LogType.DEBUG) {
                TextFileWriter textfile = new TextFileWriter( logfile, msg);
                textfile.appendToFile();
            } else if (logger == LogType.TROUBLE) {
                TextFileWriter textfile = new TextFileWriter( troublefile, msg);
                textfile.appendToFile();                
            }
        }
    }
    /**
     * This method is used for debug, to write out data arrays as text files
     * for debugging.  The files are written into the topmost folder.
     * @param array the data array to be written out
     * @param name file name to be used for the file
     */
    public void writeOutArray( double[] array, String name) {
        if (logReady) {
            TextFileWriter textout = new TextFileWriter( finalFolder, 
                                                         name, array);
            try {
                textout.writeOutArray();
            } catch (IOException err) {
                //Nothing to do if the error logger has an error.
            }
        }
    }
    public File getLogFolder() { return logfolder; }
}