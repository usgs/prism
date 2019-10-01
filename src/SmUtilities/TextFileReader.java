/*******************************************************************************
 * Name: Java class TextFileReader.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * This class is used to read from a text file and return the contents as an
 * array of strings (text).
 * @author jmjones
 */
public class TextFileReader {
    private File fileName;
    private String[] contents;
    /**
     * Constructor takes the file name and checks that it exists and is readable.
     * @param filename the file name to read
     * @throws IOException if unable to read the file
     */
    public TextFileReader(File filename) throws IOException {
        Path check = filename.toPath();
        if (Files.isReadable(check)) {
            this.fileName = filename;
        } else {
            throw new IOException("Unable to read file " + filename);
        }
        contents = new String[0];
    }
    /**
     * Read in the contents of the file and return as an array of text.
     * @return an array of the text contents of the file
     * @throws IOException if unable to read
     */
    //read in the file into a temp arrayList. If the read was good, copy the
    //arrayList into a regular array and return.
    public String[] readInTextFile() throws IOException{
        String nextLine;
        ArrayList<String> tempfile = new ArrayList<>();

        try (BufferedReader bufReader = new BufferedReader(new FileReader(this.fileName))){
            while ((nextLine = bufReader.readLine()) != null) {
                tempfile.add(nextLine);
            }
            if (tempfile.size() > 0){
                contents = tempfile.toArray(new String[tempfile.size()]);
            } else {
                throw new IOException("Empty file: " + this.fileName);
            }
        }
        tempfile.clear();
        return contents;
    }

}
