/*******************************************************************************
 * Name: Java class SmProductGUI.java
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
import SmConstants.VFileConstants;
import SmUtilities.TextFileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles the writing of products out to their files and the creation
 * of the directory structure for the manual prism review code.  
 * This class assumes a file name structure for the V0 files.
 * @author jmjones
 */
public class SmProductGUI extends SmProductFormat {
    
    public SmProductGUI() {
        super();
    }
    /**
     * Sets the current station directory to the input path and checks to see
     * if the V0-V3 subdirectories are present in the station directory.  If not,
     * it creates them.
     * @param stationId the full path of the station (or UT date-time) directory
     */
    public void updateDirectoriesGUI(File stationId) {
        setStationDir(stationId);
        
        //Create the V0 - V3 folders
        File V0Id = Paths.get(stationId.toString(), "V0").toFile();
        if (!V0Id.isDirectory()) {
            V0Id.mkdir();
        }
        File V1Id = Paths.get(stationId.toString(), "V1").toFile();
        if (!V1Id.isDirectory()) {
            V1Id.mkdir();
        }
        File V2Id = Paths.get(stationId.toString(), "V2").toFile();
        if (!V2Id.isDirectory()) {
            V2Id.mkdir();
        }
        File V3Id = Paths.get(stationId.toString(), "V3").toFile();
        if (!V3Id.isDirectory()) {
            V3Id.mkdir();
        }        
    }
    /**
     * Writes out each of the products to the respective folder, first creating
     * the full path name, then writing out the text file, then adding the name
     * of the file to the log list.
     * @param trashDir the trash directory where overwritten or deleted files will be moved to
     * @return the text file list of file names (log list)
     * @throws IOException if unable to write out the file
     */
    @Override
    public String[] writeOutProducts(String trashDir) throws IOException {
        TextFileWriter textout;
        Iterator iter;
        Path outName = null;
        String[] contents;
        File location;
        String channel;
        String srcFileName;
        String srcFileExt;
        String destFileExt;
        
        super.loglist.clear();
        
        String msg = "Writing output files ...";
        super.loglist.add(msg);
        
        //write out V0s
        iter = super.V0List.iterator();
        while (iter.hasNext()) {
            V0Component rec0 = (V0Component)iter.next();  
            
            contents = rec0.VrecToText();
            location = rec0.getStationDir();
            channel = rec0.getChannel();
            srcFileName = rec0.getFileName();
            srcFileExt = srcFileName.substring(srcFileName.lastIndexOf(".")+1);
            destFileExt = srcFileExt.length() > 2 ? "V0" + srcFileExt.substring(2) : "V0";
            
            outName = buildFilename(location, "", srcFileName, destFileExt, channel, "");
            File outFile = outName.toFile();
            
            // If file exists in destination path, move it to the trash folder.
            if (outFile.exists()) {
                Path pathStation = Paths.get(location.getPath());
                Path pathEvent = pathStation.getParent();
                String event = pathEvent.getFileName().toString();
                String station = pathStation.getFileName().toString();
                Path trashPath = Paths.get(trashDir,event,station);
                Path trashFilePath = buildFilename(trashPath.toFile(),"", srcFileName,
                    destFileExt,channel,"");
                File trashFile = trashFilePath.toFile();
                
                moveFileToTrash(outFile,trashFile);
            }
            
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            
            msg = String.format("V0 File: %s", outName);
            loglist.add(msg);
            
        }
        this.V0List.clear();
        
        //write out V1s
        iter = this.V1List.iterator();
        while (iter.hasNext()) {
            V1Component rec1 = (V1Component)iter.next();
            contents = rec1.VrecToText();
            location = rec1.getStationDir();
            channel = rec1.getChannel();
            srcFileName = rec1.getFileName();
            srcFileExt = srcFileName.substring(srcFileName.lastIndexOf(".")+1);
            destFileExt = srcFileExt.length() > 2 ? "V1" + srcFileExt.substring(2) : "V1";
            
            outName = buildFilename(location, "", srcFileName, destFileExt, channel, "");
            File outFile = outName.toFile();
            
            // If file exists in destination path, move it to the trash folder.
            if (outFile.exists()) {
                Path pathStation = Paths.get(location.getPath());
                Path pathEvent = pathStation.getParent();
                String event = pathEvent.getFileName().toString();
                String station = pathStation.getFileName().toString();
                Path trashPath = Paths.get(trashDir,event,station);
                Path trashFilePath = buildFilename(trashPath.toFile(),"", srcFileName,
                    destFileExt,channel,"");
                File trashFile = trashFilePath.toFile();
                
                moveFileToTrash(outFile,trashFile);
            }
            
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            
            msg = String.format("V1 File: %s", outName);
            loglist.add(msg);
        }
        this.V1List.clear();
        
        //write out V2s
        iter = this.V2List.iterator();
        while (iter.hasNext()) {
            V2Component rec2 = (V2Component)iter.next();
            String textHeader0 = rec2.getTextHeader()[0];
            String v2DataTypeExt = "";
            
            if (textHeader0.matches(String.format("(?i)(^.*)(%s)(.*)", 
                VFileConstants.V2DataType.ACC.toString())))
                v2DataTypeExt = "acc";
            if (textHeader0.matches(String.format("(?i)(^.*)(%s)(.*)", 
                VFileConstants.V2DataType.VEL.toString())))
                v2DataTypeExt = "vel";
            if (textHeader0.matches(String.format("(?i)(^.*)(%s)(.*)", 
                VFileConstants.V2DataType.DIS.toString())))
                v2DataTypeExt = "dis";
            
            contents = rec2.VrecToText();
            location = rec2.getStationDir();
            channel = rec2.getChannel();
            srcFileName = rec2.getFileName();
            srcFileExt = srcFileName.substring(srcFileName.lastIndexOf(".")+1);
            destFileExt = srcFileExt.length() > 2 ? "V2" + srcFileExt.substring(2) : "V2";
            
            outName = buildFilename(location, "", srcFileName, destFileExt, channel, v2DataTypeExt);
            File outFile = outName.toFile();
            
            // If file exists in destination path, move it to the trash folder.
            if (outFile.exists()) {
                Path pathStation = Paths.get(location.getPath());
                Path pathEvent = pathStation.getParent();
                String event = pathEvent.getFileName().toString();
                String station = pathStation.getFileName().toString();
                Path trashPath = Paths.get(trashDir,event,station);
                Path trashFilePath = buildFilename(trashPath.toFile(),"", srcFileName,
                    destFileExt,channel,v2DataTypeExt);
                File trashFile = trashFilePath.toFile();
                
                moveFileToTrash(outFile,trashFile);
            }
            
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            
            msg = String.format("V2.%s File: %s", v2DataTypeExt,outName);
            loglist.add(msg);
        }
        this.V2List.clear();
        
        //write out V3s
        iter = this.V3List.iterator();
        while (iter.hasNext()) {
            V3Component rec3 = (V3Component)iter.next();
            contents = rec3.VrecToText();
            location = rec3.getStationDir();
            channel = rec3.getChannel();
            srcFileName = rec3.getFileName();
            srcFileExt = srcFileName.substring(srcFileName.lastIndexOf(".")+1);
            destFileExt = srcFileExt.length() > 2 ? "V3" + srcFileExt.substring(2) : "V3";
            
            outName = buildFilename(location,"", srcFileName,destFileExt,channel, "");
            File outFile = outName.toFile();
            
            // If file exists in destination path, move it to the trash folder.
            if (outFile.exists()) {
                Path pathStation = Paths.get(location.getPath());
                Path pathEvent = pathStation.getParent();
                String event = pathEvent.getFileName().toString();
                String station = pathStation.getFileName().toString();
                Path trashPath = Paths.get(trashDir,event,station);
                Path trashFilePath = buildFilename(trashPath.toFile(),"", srcFileName,
                    destFileExt,channel,"");
                File trashFile = trashFilePath.toFile();
                
                moveFileToTrash(outFile,trashFile);
            }
            
            textout = new TextFileWriter(outName, contents);
            textout.writeOutToFile();
            
            msg = String.format("V3 File: %s", outName);
            loglist.add(msg);
        }
        this.V3List.clear();
        
        msg = String.format("File writing completed.");
        loglist.add(msg);
        
        String[] outlist = new String[loglist.size()];
        outlist = loglist.toArray(outlist);
        loglist.clear();
            
        return outlist;
    }
    /**
     * Builds the output filename from a folder path, file name, file extension,
     * channel number, and V2 processing type extension
     * @param location the output folder for this file
     * @param fileName the file name
     * @param fileExtension the extension of V1, V2, etc.
     * @param channel the channel id if needed in the filename
     * @param ext an extension for V2s, such as 'acc', 'vel', or 'dis'
     * @return the full file path
     */
    @Override
    public Path buildFilename(File location, String localdir, String fileName,  
                            String fileExtension,String channel, String ext) {
        
        String name;
        
        if (ext.isEmpty()) { // V0, V1, or V3
            String regEx = channel.isEmpty() ? String.format("[\\.]V[\\d][cC]?$") :
                String.format("%s[\\.]V[\\d][cC]?$", channel);
            String replacement = channel.isEmpty() ? "." + fileExtension :
                channel + "." + fileExtension;
            name = fileName.replaceFirst(regEx, replacement);
        }
        else {  //V2
            String regEx = channel.isEmpty() ? String.format("[\\.]V[\\d][cC]?$") :
                String.format("%s[\\.]V[\\d][cC]?$", channel);
            String replacement = channel.isEmpty() ? "." + ext + "." + fileExtension :
                channel + "." + ext + "." + fileExtension;
            name = fileName.replaceFirst(regEx, replacement);
        }
        
        String cosmosFileType = fileExtension.substring(0,2);
        
        Path outName = Paths.get(location.toString(),cosmosFileType, name);
        
        return outName;
    }
    /**
     * 
     * @param srcFile
     * @param trashFile
     * @return
     * @throws IOException 
     */
    private boolean moveFileToTrash(File srcFile, File trashFile) throws IOException {
        try {
            if (srcFile.getPath().isEmpty() || trashFile.getPath().isEmpty())
                return false;
            
            if (srcFile.exists()) {
                Path srcPath = srcFile.toPath();
                Path trashPath = trashFile.toPath();
                
                // Create trash directory if necessary.
                File trashDir = trashFile.getParentFile();
                if (!trashDir.exists())
                    trashDir.mkdirs();

                // Check if trashPath already exists. If so, attach an increment
                // number to the filename. Iterate until first filename with 
                // incremented number is not found.
                if (trashFile.exists()) {
                    String origFileName = trashFile.getName();
                    String rx = "(?i)(^.*)(.V[0123][cC]?$)";
                    Pattern pattern = Pattern.compile(rx);
                    Matcher m = pattern.matcher(origFileName);
                    if (!m.find())
                        return false;

                    int num = 2;
                    while (trashFile.exists()) {
                        String newFileName = String.format("%s(%d)%s", m.group(1),num,m.group(2));
                        trashFile = Paths.get(trashFile.getParent(),newFileName).toFile();
                        trashPath = trashFile.toPath();
                        num++;
                    }
                }
                
                // Move file to trash.
                Files.move(srcPath, trashPath, REPLACE_EXISTING);
                return true;
            }
            else
                return false;
        }
        catch (IOException ex) {throw ex;}
    }  
}
