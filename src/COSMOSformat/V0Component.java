/*******************************************************************************
 * Name: Java class V0Component.java
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

package COSMOSformat;

import SmConstants.VFileConstants;
import static SmConstants.VFileConstants.CNTN;
import static SmConstants.VFileConstants.COUNTTEXT;
import static SmConstants.VFileConstants.DEFAULT_ARRAY_STYLE;
import static SmConstants.VFileConstants.DELTA_T;
import static SmConstants.VFileConstants.MSEC_TO_SEC;
import SmException.FormatException;
import SmException.SmException;
import SmUtilities.ConfigReader;
import static SmConstants.SmConfigConstants.OUT_ARRAY_FORMAT;

/**
 * This class extends the COSMOScontentFormat base class to define a V0 record.
 * It defines the method to parse the V0 data array and provides getters to 
 * access the array.
 * @author jmjones
 */
public class V0Component extends COSMOScontentFormat {
    private VIntArray V0Data;  // VIntArray object of raw acceleration counts 
    /**
     * Default constructor
     * @param procType identifies the data type of raw accel., uncorrected 
     * accel., etc. of this component object (see data product names in 
     * VFileConstants)
     */
    public V0Component( String procType){
        super( procType );
    }
    /**
     * This method defines the steps for parsing a V0 data record, which contains
     * an integer data array.
     * @param startLine line number for the start of the data section
     * @param infile contents of the input file, one string per line
     * @return updated line number now pointing to first line after data section
     * @throws FormatException if unable to extract format information or
     * to convert text values to numeric
     */
    @Override
    public int parseDataSection (int startLine, String[] infile) throws 
                                                            FormatException {
        int current = startLine;
        
        V0Data = new VIntArray();
        current = V0Data.parseValues( current, infile);
        return current;
    }
    /**
     * This method converts the internal record to a text format.  Items already
     * in text format are copied in, and numeric tables are converted according
     * to their data type.
     * @return an array of strings (text) of the COSMOS file in output format
     */
    @Override
    public String[] VrecToText() {
        //add up the length of the text portions of the component, which are
        //the text header, the comments, and the end-of-data line.
        int totalLength;
        int currentLength = 0;
        int textLength = this.textHeader.length + this.comments.length + 1;
        
        //get the header and data arrays as text
        String[] intHeaderText = this.intHeader.numberSectionToText();
        String[] realHeaderText = this.realHeader.numberSectionToText();
        String[] V0DataText = this.V0Data.numberSectionToText();
        
        //add the array lengths to the text lengths to get the total and declare
        //an array of this length, then build it by combining all the component
        //pieces into a text version of the component.
        totalLength = textLength + intHeaderText.length + realHeaderText.length + 
                                                        V0DataText.length;
        String[] outText = new String[totalLength];
        System.arraycopy(this.textHeader, 0, outText, currentLength, 
                                                        this.textHeader.length);
        currentLength = currentLength + this.textHeader.length;
        System.arraycopy(intHeaderText, 0, outText, currentLength, 
                                                            intHeaderText.length);
        currentLength = currentLength + intHeaderText.length;
        System.arraycopy(realHeaderText, 0, outText, currentLength, 
                                                          realHeaderText.length);
        currentLength = currentLength + realHeaderText.length;
        System.arraycopy(this.comments, 0, outText, currentLength, this.comments.length);
        currentLength = currentLength + this.comments.length;
        System.arraycopy(V0DataText, 0, outText, currentLength, V0DataText.length);
        outText[totalLength-1] = this.endOfData;
        return outText;
    }
    /**
     * Updates certain parameters for the V0 file to facilitate its rewrite out
     * as single channel.  The array output format is checked in the configuration
     * file and updated for the outgoing V0, the output file name is set and
     * record id and authorization picked up to determine the output directory
     * and file name
     * @param inname the file name for this record
     * @throws FormatException if field width is invalid
     * @throws SmException if unable to build the new format line
     */
    public void updateV0(String inname) throws FormatException, SmException {
        //Get the array output format of single column per channel or packed
        VFileConstants.SmArrayStyle packtype;
        ConfigReader config = ConfigReader.INSTANCE;
        
        String arrformat = config.getConfigValue(OUT_ARRAY_FORMAT);
        arrformat = (arrformat == null) ? DEFAULT_ARRAY_STYLE : arrformat;
        packtype = (arrformat.equalsIgnoreCase("singleColumn")) ? 
                            VFileConstants.SmArrayStyle.SINGLE_COLUMN : 
                                            VFileConstants.SmArrayStyle.PACKED;
        V0Data.buildArrayParams( packtype );
        this.buildNewDataFormatLine(COUNTTEXT, CNTN, "raw accel.  ");
        this.setFileName(inname);
//        this.checkForRcrdIdAndAuth();
        
    }
    /**
     * This method builds a new data format line for writing out the V0 files,
     * and is used when the data packing method has changed to Single Column.
     * @param units output units, which will be counts for V0
     * @param unitscode units code for counts
     * @param dataType for V0 this will be acceleration or raw accel.
     * @throws SmException if unable to correctly format the values
     */
    public void buildNewDataFormatLine(String units, int unitscode, 
                                        String dataType) throws SmException {
        //calculate the time by multiplying the number of data values by delta t
        String line;
        double deltat = this.getRealHeaderValue(DELTA_T);
        int numvals = V0Data.getNumVals();
        double calcTime = deltat * numvals * MSEC_TO_SEC;
        String timeSec = Integer.toString((int)calcTime);
        line = String.format("%1$8s %2$12s pts, approx %3$4s secs, units=%4$7s(%5$02d),Format=",
                                     String.valueOf(numvals),dataType,
                                                    timeSec, units, unitscode);
        V0Data.setFormatLine(line + V0Data.getNumberFormat());
    }
    /**
     * Getter for the length of the data array.
     * @return the length of the data array.
     */
    public int getDataLength() {
        return V0Data.getNumVals();
    }
    /**
     * Getter for a copy of the data array reference.  Used to access the entire
     * array during data processing.
     * @return a copy of the array reference
     */
    public int[] getDataArray() {
        return V0Data.getIntArray();
    }
}
