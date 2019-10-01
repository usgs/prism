/*******************************************************************************
 * Name: Java class Prism.java (program main)
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

package SmControl;

import SmConstants.VFileConstants;
import static SmConstants.VFileConstants.RAWACC;
import SmException.FormatException;
import SmException.SmException;
import SmUtilities.ConfigReader;
import SmUtilities.PrismLogger;
import SmUtilities.PrismXMLReader;
import SmConstants.SmConfigConstants;
import SmUtilities.SmDebugLogger;
import SmUtilities.SmTimeFormatter;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * Main class for the PRISM strong motion analysis tool.  This is the controller
 * for the batch processing mode, to be started from the command line or
 * automatically by other software.  This class takes an input folder as a param.,
 * reads in *.V0 files in the folder and then processes each file in turn.
 * Processing involves reading in the file and parsing into record(s), running
 * the waveform processing algorithms to create the data products, and then
 * writing out the data in the different formats.
 * @author jmjones
 */
public class Prism {
    private final String inFolder;
    private final String outFolder;
    private String logFolder;
    private String configFile;

    // data structures for the controller
    private File[] inVList;
    private SmQueue smqueue;
    private SmProduct Vproduct;
    /**
     * Constructor for PRISM main
     * @param args input arguments
     * @throws SmException error processing a COSMOS file
     */
    public Prism (String[] args) throws SmException {
        this.configFile = "";
        if (args.length > 1) {
            File inDir = new File(args[0]);
            File outDir = new File(args[1]);
            if (inDir.isDirectory() && outDir.isDirectory()) {
                this.inFolder = args[0];
                this.outFolder = args[1];
                this.logFolder = this.outFolder;
            } else {
                throw new SmException("Input and output directories are not recognized.");
            }
            if (args.length > 2) {
                Path configval = Paths.get(args[2]);
                if (Files.isReadable(configval)) {
                    this.configFile = args[2];
                } else {
                    throw new SmException("Unable to read configuration file.");
                }
            }
            if (args.length == 4) {
                File logDir = new File(args[3]);
                if (logDir.isDirectory()) {
                    this.logFolder = args[3];
                }
            }
        } else {
            throw new SmException("Input and output directories must be provided.");
        }
    }
    /**
     * Main method starts the loggers, reads in the configuration file, 
     * reads in all .v0 or .v0c file names in the input folder, processes each file
     * in turn, and then deletes the input file.
     * @param args input string arguments, input folder, output folder, optional
     * configuration file (full path names)
     * @throws SmException if a fatal error occurs during processing
     * @throws IOException if unable to read in the files or file names
     */
    public static void main(String[] args) throws SmException, IOException, Exception { 
        int recordCount = 0;
        try {
            Prism smc = new Prism( args ); 

            SmTimeFormatter timer = new SmTimeFormatter();
            PrismLogger log = PrismLogger.INSTANCE;
            SmDebugLogger errlog = SmDebugLogger.INSTANCE;
            String logtime = timer.getGMTdateTime();
            String[] startLog = new String[2];
            startLog[0] = "\n";
            startLog[1] = "Prism Log Entry: " + logtime;
            try {
                log.initializeLogger(smc.logFolder, logtime);
                log.writeToLog(startLog);
                errlog.initializeLogger(smc.logFolder, logtime);
            } 
            catch (IOException err) {
                throw new SmException("Unable to open the log files: " + err.getMessage());
            }
            //get the list of filenames in the input directory
            try {
                smc.inVList = smc.getFileList( smc.inFolder, "*.[vV]0*" );
            }
            catch (IOException err) {
                throw new SmException("Unable to access V0 file list: " + err.getMessage());
            }
            //get the configuration file
            if ( !smc.configFile.isEmpty()  ) {
                smc.readConfigFile( smc.configFile );
                try {
                    smc.logConfigValues( log );
                }
                catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException err) {
                    throw new SmException("Unable to access configuration file parameters for logging");
                }
            }
            //Get each filename, read in, parse, process, write it out. When  
            //going through the list of input files, report any problems 
            //with an individual file and move directly to the next file.  
            //Attempt to process all the files in the list.
            for (File each: smc.inVList){
                smc.smqueue = new SmQueue( each, logtime, log.getLogFolder() );
                smc.Vproduct = new SmProduct(smc.outFolder);
                try {
                    smc.smqueue.readInFile( each );
                    
                    // parse the raw acceleration file into channel record(s)
                    recordCount = smc.smqueue.parseVFile( RAWACC );
                    
                    //process the records, then write out results
                    smc.smqueue.processQueueContents(smc.Vproduct);

                    String[] outlist = smc.Vproduct.writeOutProducts("");
                    log.writeToLog(outlist);
                    String[] troublelist = smc.Vproduct.buildTroubleLog(outlist);
                    if (troublelist.length > 0) {
                        errlog.writeToLog(troublelist, VFileConstants.LogType.TROUBLE);
                    }
                    smc.Vproduct.deleteV0AfterProcessing(each);
                }
                catch (FormatException | IOException | SmException err) {
                    String[] logtxt = new String[2];
                    logtxt[0] = "Unable to process file " + each.toString();
                    logtxt[1] = "\t" + err.getMessage();
                    log.writeToLog(logtxt);
                }
            }
        } 

        catch (SmException err){
            System.err.println(err.getMessage());
        }


    }
    /**
     * Reads in the configuration file and parses the xml
     * @param filename the configuration file name
     * @throws SmException if unable to read in or parse the file
     */
    public void readConfigFile( String filename ) throws SmException {

        try {
            PrismXMLReader xml = new PrismXMLReader();
            xml.readFile(filename);
        } catch (ParserConfigurationException | SAXException err) {
            throw new SmException("Unable to parse configuration file " + filename);
        } catch (IOException err) {
            throw new SmException("Configuration file error: " + err.getMessage());
        }
    }
    /**
     * This method writes a record of the current configuration file parameters
     * into the prism log, using the SmConfigConstant names for each parameter.
     * @param log the logger for writing
     * @throws IOException if unable to read the configuration file
     * @throws NoSuchFieldException if unable to find a specific parameter name
     * @throws IllegalArgumentException if unable to access a class value
     * @throws IllegalAccessException if unable to access a specific parameter
     */
    public void logConfigValues( PrismLogger log ) throws IOException, NoSuchFieldException, 
                                                IllegalArgumentException, IllegalAccessException {
        ConfigReader config = ConfigReader.INSTANCE;
        
        // Get a class object for the configuration file constants 
        Class<SmConfigConstants> scc = SmConfigConstants.class;
        List<String> cflist = new ArrayList<>();
        cflist.add(" ");
        cflist.add("Configuration file parameters:");
        
        // Pick up the publicly declared field names in the class
        // Add both the name of the field and its string value to the list
        StringBuilder sb = new StringBuilder();
        for (Field cf : scc.getDeclaredFields()) {
            if (!cf.getName().equals("CONFIG_XSD_VALIDATOR")) { // don't want this name
                sb.append(cf.getName()).append(": ")
                        .append(config.getConfigValue((String)cf.get(scc)));
                cflist.add(sb.toString());
                sb.setLength(0);
            }
        }
        cflist.add(" ");
        String[] cfarr = new String[cflist.size()];
        log.writeToLog(cflist.toArray(cfarr));
        cflist.clear();
    }
    /**
     * Gets the list of v0 files in the input folder and returns an array of
     * file names
     * @param filePath directory path to the files
     * @param exten file extension to pick up
     * @return list of file names
     * @throws IOException if the folder is empty
     */
    public File[] getFileList(String filePath, String exten) throws IOException {
        Path dir = Paths.get(filePath);
        ArrayList<File> inList = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                                    Files.newDirectoryStream(dir, exten)) {
            for (Path entry: stream) {
                File name = new File( this.inFolder,entry.getFileName().toString());
                inList.add(name);
            }
            if (inList.isEmpty()) {
                throw new IOException("No files found in directory " + this.inFolder);
            }
            File[] finalList = new File[inList.size()];
            finalList = inList.toArray(finalList);
            inList.clear();
            return finalList;
        }
    }
    /**
     * Gets the input folder name
     * @return input folder name
     */
    public String getInFolder()
    {
        return this.inFolder;
    }
    /**
     * Gets the output folder name
     * @return output folder name
     */
    public String getOutFolder()
    {
        return this.outFolder;
    }
    /**
     * Gets the configuration file name
     * @return the configuration file name
     */
    public String getConfigFile()
    {
        return this.configFile;
    }
    /**
     * Gets the list of input files
     * @return the list of input files
     */
    public File[] getInVList()
    {
        return this.inVList;
    }
    /**
     * Gets the processing queue
     * @return the processing queue
     */
    public SmQueue getSmqueue()
    {
        return this.smqueue;
    }
    /**
     * Sets the input file list to the given list
     * @param inVList list to set as the input file list
     */
    public void setInVList(File[] inVList)
    {
        this.inVList = inVList;
    }
}
