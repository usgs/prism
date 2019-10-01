/*******************************************************************************
 * Name: Java class TextFileWriter.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * This class is used to write an array of text out to a file
 * @author jmjones
 */
public class TextFileWriter {
    private final Path outName;
    private final String[] contents;
    private final double[] array;
    private final Charset ENCODING = StandardCharsets.UTF_8;
    /**
     * The constructor stores the output file name and the contents to be
     * written out
     * @param outfilename the output full path filename
     * @param contents the text contents to be written out
     */
    public TextFileWriter( Path outfilename, String[] contents) {
        this.contents = contents;
        this.outName = outfilename;
        this.array = new double[0];
    }
    /**
     * This constructor is used for debug when writing out a data array
     * @param dir the directory to write the file to
     * @param outname the name for the debug file
     * @param array the data array to write out
     */
    public TextFileWriter( String dir, String outname, double[] array) { //debug
        this.array = array;
        this.contents = new String[ array.length ];
        this.outName = Paths.get(dir, outname);
    }
    /**
     * This method writes out the text to the output file, with a newline
     * inserted between each line of text.
     * @throws IOException if unable to write out to file
     */
    public void writeOutToFile() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outName, ENCODING)) {
            for (String line : contents) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
    /**
     * This method is used to append text to the end of an existing log file.  If
     * no file exists, it is created.
     * @throws IOException if unable to write to file
     */
    public void appendToFile() throws IOException {
        if (Files.notExists(outName)) {
            Files.createFile(outName);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outName, ENCODING, 
                                                    StandardOpenOption.APPEND)) {
            for (String line : contents) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
    /**
     * Writes out the data array for debug purposes, converting the numerics to text
     * @throws IOException if unable to write out to the file
     */
    public void writeOutArray( ) throws IOException {  //mainly for debug
        int len = array.length;
        for (int i = 0; i < len; i++) {
            contents[i] = Double.toString(array[i]);
        }
        writeOutToFile();
    }
}

