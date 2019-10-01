/*******************************************************************************
 * Name: Java class PrismLogger.java
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

/**
 * This class is a singleton instance of the prism logger.  This logger is used
 * to record the names of the output products, including their destination folders.
 * Errors that occur during processing are also recorded here.
 * @author jmjones
 */
public class PrismLogger {
    private Path logfile;
    private static boolean logReady = false;
    private final String logname = "PrismLog.txt";
    public final static PrismLogger INSTANCE = new PrismLogger();
    private String finalFolder;
    private File logfolder;
    private String startTime;
    /**
     * Constructor for the logger is private as part of the
     * singleton implementation.  Access to the logger is through the INSTANCE 
     * variable:  PrismLogger logger = PrismLogger.INSTANCE.
     */
    private PrismLogger() {
    }
    /**
     * This method initializes the logger and checks if the log folder exists,
     * and if not it creates the log folder in the top level output folder
     * @param outfolder the top level output folder, where the log folder resides
     * @param time currently not used, but available if log files should be created
     * new and not appended at the end of the existing file
     * @throws IOException if unable to write out a file
     */
    public void initializeLogger( String outfolder, String time ) throws IOException {
        finalFolder = outfolder;
        startTime = time;
        if (!logReady) {
            logfolder = Paths.get(outfolder, "Prism_Logs").toFile();
            if (!logfolder.isDirectory()) {
                logfolder.mkdir();
            }
            this.logfile = Paths.get(logfolder.toString(),logname);
            logReady = true;
        }
    }
    /**
     * Writes the array of text messages out to the log file, appending to the
     * end of the current file.
     * @param msg the list of messages to be written out
     * @throws IOException if unable to write to the file
     */
    public void writeToLog( String[] msg ) throws IOException {
        if (logReady) {
            TextFileWriter textfile = new TextFileWriter( logfile, msg);
            textfile.appendToFile();
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
