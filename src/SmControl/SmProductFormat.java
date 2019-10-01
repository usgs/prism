/*******************************************************************************
 * Name: Java class SmProductFormat.java
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

import COSMOSformat.COSMOScontentFormat;
import COSMOSformat.V0Component;
import COSMOSformat.V1Component;
import COSMOSformat.V2Component;
import COSMOSformat.V3Component;
import SmConstants.VFileConstants;
import static SmConstants.VFileConstants.MAX_LINE_LENGTH;
import SmUtilities.RecordIDValidator;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This abstract class defines the shared methods for the prism product generation 
 * between auto prism and the review tool.
 * @author jmjones
 */
abstract class SmProductFormat {
    protected String outFolder;
    protected ArrayList<COSMOScontentFormat> V0List;
    protected ArrayList<COSMOScontentFormat> V1List;
    protected ArrayList<COSMOScontentFormat> V2List;
    protected ArrayList<COSMOScontentFormat> V3List;
    protected final Charset ENCODING = StandardCharsets.UTF_8;
    protected File stationDir;
    protected File eventDir;
    protected File logDir;
    protected ArrayList<String> loglist;
    protected final String V0DIR = "V0";
    protected final String V1DIR = "V1";
    protected final String V2DIR = "V2";
    protected final String V3DIR = "V3";
    
    /**
     * Constructor for the product class.
     */
    public SmProductFormat() {
        this.V0List = new ArrayList<>();
        this.V1List = new ArrayList<>();
        this.V2List = new ArrayList<>();
        this.V3List = new ArrayList<>();
        this.loglist = new ArrayList<>(); 
    }
    /**
     * Method to add a product to the product queue for later writing out to a file.
     * @param newprod the COSMOS object for the queue
     * @param ext the type of object, such as V0, V1, etc.
     */
    public void addProduct(COSMOScontentFormat newprod, String ext ) {
        if (ext.equalsIgnoreCase("V0")) {
            V0Component rec = (V0Component)newprod;
            rec.setStationDir(this.stationDir);
            this.V0List.add(rec);
        } else if (ext.equalsIgnoreCase("V1")) {
            V1Component rec = (V1Component)newprod;
            rec.setStationDir(this.stationDir);
            this.V1List.add(rec);
        } else if (ext.equalsIgnoreCase("V2")) {
            V2Component rec = (V2Component)newprod;
            rec.setStationDir(this.stationDir);
            this.V2List.add(rec);
        } else {
            V3Component rec = (V3Component)newprod;
            rec.setStationDir(this.stationDir);
            this.V3List.add(rec);
        }
    }
    /**
     * Method to set up the directory structure within the output directory.
     * @param rcid extracted from the input file, this defines the folder structure
     * according to the SCNL code
     * @param scnlauth must be present in the V0 for the standard folder structure
     * to be put in place.
     * @param eventMarker event time pulled from the file header in case of Orphan,
     * this event time will be used in place of the station name
     * @param V2result flag for the creation of a Trouble folder
     */
    public void setDirectories(String rcid, String scnlauth, String eventMarker, 
                                                            VFileConstants.V2Status V2result) {
        
        String event = "Orphan";
        String station = eventMarker;
        RecordIDValidator rcdvalid = new RecordIDValidator(rcid);
        if ((!scnlauth.isEmpty()) && (rcdvalid.isValidRcrdID())) {
            event = rcdvalid.getEventID();
            station = rcdvalid.getStationID();
        }
//        //create the log folder !!! logs are now created in the main prism code
//        File logId = Paths.get(this.outFolder, "Prism_Logs").toFile();
//        if (!logId.isDirectory()) {
//            logId.mkdir();
//        }
//        this.logDir = logId;
        
        //create the event and station directories
        File eventId = Paths.get(this.outFolder, event).toFile();
        if (!eventId.isDirectory()) {
            eventId.mkdir();
        }
        this.eventDir = eventId;
        
        File stationId = Paths.get(eventId.toString(), station).toFile();
        if (!stationId.isDirectory()) {
            stationId.mkdir();
        }
        
        if (V2result != VFileConstants.V2Status.GOOD) {
            stationId = Paths.get(eventId.toString(), station, "Trouble").toFile();
            if (!stationId.isDirectory()) {
                stationId.mkdir();
            }
        }
        this.stationDir = stationId;
        
        //Create the V0 - V3 folders
        File V0Id = Paths.get(stationId.toString(), V0DIR).toFile();
        if (!V0Id.isDirectory()) {
            V0Id.mkdir();
        }
        File V1Id = Paths.get(stationId.toString(), V1DIR).toFile();
        if (!V1Id.isDirectory()) {
            V1Id.mkdir();
        }
        if ((V2result == VFileConstants.V2Status.GOOD) || (V2result == VFileConstants.V2Status.FAILQC)) {
            File V2Id = Paths.get(stationId.toString(), V2DIR).toFile();
            if (!V2Id.isDirectory()) {
                V2Id.mkdir();
            }
        }
        if (V2result == VFileConstants.V2Status.GOOD) {  //V3 processing only occurs on valid V2 products
            File V3Id = Paths.get(stationId.toString(), V3DIR).toFile();
            if (!V3Id.isDirectory()) {
                V3Id.mkdir();
            }        
        }
    }
    /**
     * Writes out each of the products to the respective folder, first creating
     * the full path name, then writing out the text file, then adding the name
     * of the file to the log list.
     * @param inDir input directory for the products
     * @return the text file list of file names (log list)
     * @throws IOException if unable to write out the file
     */
    abstract public String[] writeOutProducts(String inDir) throws IOException ;
    
    /**
     * Builds the output filename from a folder path, file name, file extension,
     * channel number, and V2 processing type extension
     * @param outloc the output folder for this file
     * @param localdir the directory for the specific cosmos file type
     * @param fileName the file name
     * @param fileExtension the extension of V1, V2, etc.
     * @param channel the channel id if needed in the filename
     * @param ext an extension for V2s, such as 'acc', 'vel', or 'dis'
     * @return the full file path
     */
    abstract public Path buildFilename(File outloc, String localdir, String fileName,  
                              String fileExtension, String channel, String ext);
    /**
     * Validates the format of the record id before its use in building directory names
     * @param id the record id
     * @return true if record id has a recognized format, false if it doesn't
     */
    public boolean validateRcrdId( String id ) {
        StringBuilder sb = new StringBuilder(80);
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
        return m.matches();
    }
    public File getStationDir() {return this.stationDir;}
    public void setStationDir(File stationid) {this.stationDir = stationid;}
}
