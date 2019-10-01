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

package SmControl;

import COSMOSformat.V0Component;
import COSMOSformat.V1Component;
import COSMOSformat.V2Component;
import COSMOSformat.V3Component;
import static SmConstants.VFileConstants.DELETE_INPUT_V0;
import SmUtilities.ConfigReader;
import static SmConstants.SmConfigConstants.DELETE_V0;
import SmUtilities.TextFileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles the writing of products out to their files and the creation
 * of the directory structure for the automated prism code.  
 * This class assumes a file name structure for the V0 files.
 * @author jmjones
 */
public class SmProduct extends SmProductFormat {
    private boolean deleteInputFiles;
    
    /**
     * Constructor for the product class.
     * @param newFolder the output folder, top level
     */
    public SmProduct(final String newFolder) {
        super();
        super.outFolder = newFolder;
        super.stationDir = new File( newFolder );
        super.eventDir = new File( newFolder );
        super.logDir = new File( newFolder );
        ConfigReader config = ConfigReader.INSTANCE;
        String deleteV0 = config.getConfigValue(DELETE_V0);
        this.deleteInputFiles = (deleteV0 == null) ? false : 
                                    deleteV0.equalsIgnoreCase(DELETE_INPUT_V0);
    }
    /**
     * Writes out each of the products to the respective folder, first creating
     * the full path name, then writing out the text file, then adding the name
     * of the file to the log list.
     * @param inDir not used here
     * @return the text file list of file names (log list)
     * @throws IOException if unable to write out the file
     */
    @Override
    public String[] writeOutProducts(String inDir) throws IOException {
        TextFileWriter textout;
        Iterator iter;
        Path outName = null;
        String[] contents;
        String chanvalue;
        //write out V0s
        iter = super.V0List.iterator();
        while (iter.hasNext()) {
            V0Component rec0 = (V0Component)iter.next();
            contents = rec0.VrecToText();
            chanvalue = (super.V0List.size() > 1) ? rec0.getChannel() : "";
            outName = buildFilename(rec0.getStationDir(),super.V0DIR, rec0.getFileName(),
                                                    "V0c", chanvalue, "");
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            super.loglist.add(outName.toString());
        }
        super.V0List.clear();
        //write out V1s
        iter = super.V1List.iterator();
        while (iter.hasNext()) {
            V1Component rec1 = (V1Component)iter.next();
            contents = rec1.VrecToText();
            chanvalue = (super.V1List.size() > 1) ? rec1.getChannel() : "";
            outName = buildFilename(rec1.getStationDir(),super.V1DIR, rec1.getFileName(),
                                                    "V1c", chanvalue, "");
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            super.loglist.add(outName.toString());
        }
        super.V1List.clear();
        //write out V2s
        iter = super.V2List.iterator();
        while (iter.hasNext()) {
            V2Component rec2 = (V2Component)iter.next();
            contents = rec2.VrecToText();
            chanvalue = (super.V2List.size() > 3) ? rec2.getChannel() : "";
            outName = buildFilename(rec2.getStationDir(),super.V2DIR, rec2.getFileName(),
                                                  "V2c", chanvalue, "acc");
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            super.loglist.add(outName.toString());
            
            //get velocity and write to file
            if (iter.hasNext()) {
                rec2 = (V2Component)iter.next();
                outName = buildFilename(rec2.getStationDir(),super.V2DIR, rec2.getFileName(),
                                                    "V2c",chanvalue, "vel");
                contents = rec2.VrecToText();
                textout = new TextFileWriter(outName, contents);
                textout.writeOutToFile();
                super.loglist.add(outName.toString());
            }
            //get displacement and write to file
            if (iter.hasNext()) {
                rec2 = (V2Component)iter.next();
                outName = buildFilename(rec2.getStationDir(),super.V2DIR, rec2.getFileName(),
                                                    "V2c", chanvalue, "dis");
                contents = rec2.VrecToText();
                textout = new TextFileWriter(outName, contents);
                textout.writeOutToFile();
                super.loglist.add(outName.toString());
            }
        }
        super.V2List.clear();
        //write out V3s
        iter = super.V3List.iterator();
        while (iter.hasNext()) {
            V3Component rec3 = (V3Component)iter.next();
            chanvalue = (super.V3List.size() > 1) ? rec3.getChannel() : "";
            outName = buildFilename(rec3.getStationDir(), super.V3DIR, rec3.getFileName(),
                                                   "V3c", chanvalue, "");
            contents = rec3.VrecToText();
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            super.loglist.add(outName.toString());
        }
        super.V3List.clear();
        
        String[] outlist = new String[super.loglist.size()];
        outlist = super.loglist.toArray(outlist);
        super.loglist.clear();
        return outlist;
    }
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
    @Override
    public Path buildFilename(File outloc, String localdir, String fileName,  
                              String fileExtension, String channel, String ext) {
        Path pathname = Paths.get(fileName);
        String name = pathname.getFileName().toString();
        String getExtensionRegex = "\\.(?i)V\\d(?i)c??$";
        Pattern extension = Pattern.compile( getExtensionRegex );
        Matcher matcher = extension.matcher(name);
        StringBuilder sb = new StringBuilder();
        if (!channel.isEmpty()) {
            sb.append(".");
            sb.append(channel);
        }
        if (!ext.isEmpty()) {
            sb.append(".");
            sb.append(ext);
        }
        sb.append(".");
        sb.append(fileExtension);
        name = matcher.replaceFirst(sb.toString());
        Path outName = Paths.get(outloc.toString(),localdir, name);
        return outName;
    }
    /**
     * Removes the input file from the input directory, if it exists, and if the
     * Delete Input Files flag in the configuration file is set to 'Yes'. If the
     * configuration file is set to 'No', the file is not deleted.
     * @param source the input file
     * @throws IOException if unable to delete the file
     */
    public void deleteV0AfterProcessing(File source) throws IOException {
        if (this.deleteInputFiles) {
            Path infile = source.toPath();
            Files.deleteIfExists(infile);
        }
    }
    /**
     * Builds a trouble log from the list of all log files.  If there are no
     * files going to trouble folders, the list returned has size 0.
     * @param inlog the list of all output files
     * @return a list of output files going to trouble folders, or a list of
     * length 0 if no trouble files found.
     */
    public String[] buildTroubleLog(String[] inlog) {
        ArrayList<String> trouble = new ArrayList<>();
        String[] outlist = new String[0];
        for (String name : inlog) {
            if (name.contains("Trouble")) {
                trouble.add(name);
            }
        }
        if (trouble.size() > 0) {
            outlist = new String[trouble.size()];
            outlist = trouble.toArray(outlist);
            trouble.clear();
        }
        return outlist;
    }
}