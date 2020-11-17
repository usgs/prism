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

import static SmConstants.SmConfigConstants.DELETE_V0;
import static SmConstants.VFileConstants.DELETE_INPUT_V0;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author jmjones
 */
public class FileRemovalCheck {
    private boolean deleteInputFiles;
    
    public FileRemovalCheck() {
        ConfigReader config = ConfigReader.INSTANCE;
        String deleteV0 = config.getConfigValue(DELETE_V0);
        this.deleteInputFiles = (deleteV0 == null) ? false : 
                                    deleteV0.equalsIgnoreCase(DELETE_INPUT_V0);        
    }
    /**
     * Removes the input file from the input directory, if it exists, and if the
     * Delete Input Files flag in the configuration file is set to 'Yes'. If the
     * configuration file is set to 'No', the file is not deleted.
     * @param source the input file
     * @throws IOException if unable to delete the file
     */
    public void deleteV0Check(File source) throws IOException {
        if (this.deleteInputFiles) {
            Path infile = source.toPath();
            Files.deleteIfExists(infile);
        }
    }
}
