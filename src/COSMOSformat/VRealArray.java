/*******************************************************************************
 * Name: Java class VRealArray.java
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

import static SmConstants.VFileConstants.*;
import SmException.FormatException;
import java.util.ArrayList;

/**
 * This class defines the fields and methods for real arrays in COSMOS format.
 * It can be used for real headers or real data arrays.  It extracts the
 * array information from the text file, provides access to individual values or
 * the whole array, and converts the array values and header into text for
 * writing out to a file.  The access to the whole array was added to speed up
 * processing of strong motion records, eliminating the need to copy the data  
 * array before using it in processing.
 * @author jmjones
 */
public class VRealArray extends COSMOSarrayFormat {
    private double[] realVals;  // the array of doubles 
    private String displayType; // display type for doubles, such as 'F'
    /**
     * Default constructor
     */    
    public VRealArray(){
        super();
        this.setFieldWidth(DEFAULT_REAL_FIELDWIDTH);
        this.setPrecision(DEFAULT_REAL_PRECISION);
        this.displayType = DEFAULT_REAL_DISPLAYTYPE;
    }
    /**
     * Copy constructor - use this to create a copy of a VRealArray object from
     * another VRealArray object.  Useful during processing to create a COSMOS
     * component at the next processing step.
     * @param source a VRealArray object to be copied
     */
    public VRealArray( VRealArray source ){
        super();
        this.setFieldWidth(source.getFieldWidth());
        this.setPrecision(source.getPrecision());
        this.displayType = source.displayType;
        this.realVals = new double[source.realVals.length];
        System.arraycopy(source.realVals, 0, this.realVals, 0, source.realVals.length);
        this.setNumVals(source.getNumVals());
        this.setFormatLine(source.getFormatLine());
        this.setNumberFormat(source.getNumberFormat());
        this.setValsPerLine(source.getValsPerLine());
        this.setNumLines(source.getNumLines());  
    }
    /**
     * This method overrides the abstract class to handle the extraction of 
     * numeric values for real arrays.  It takes a string array of text lines
     * from the input file and an index into the text where the array section
     * begins, and parses the format line, then extracts each number and stores
     * in a real array
     * @param startLine beginning line in the text file for the array information
     * @param infile string array holding the contents of the COSMOS file
     * @return updated line number, now pointing to the line after the array info.
     * @throws FormatException if unable to find the expected format values or
     * unable to convert text to double
     */
    @Override
    public int parseValues( int startLine, String[] infile) 
                                                    throws FormatException {
        int current = startLine;
        int next = 0;
        ArrayList<String> holdNumbers;
        
        //Check for EOF before parsing format line
        if (infile.length <= current) {
            throw new FormatException("Unexpected EOF encountered at line " + current);
        }
        try {
            super.parseNumberFormatLine(infile[current]);
            String numformat = super.getNumberFormat();
            this.displayType = ((numformat.contains("F")) || (numformat.contains("f"))) ? "F" : "E";
            holdNumbers =  this.extractNumericVals( current, infile);        
            realVals = new double[holdNumbers.size()];
            for (String each: holdNumbers){
                realVals[next++] = Double.parseDouble(each);
            }
            holdNumbers.clear();
        } catch (NumberFormatException err) {
            throw new FormatException("Unable to convert text to numeric in array");
        }
        //add 1 to account for the real header format line
        return (current + calculateNumLines() + 1);
    }
    /**
     * This getter returns a value from the real array at the given index
     * @param index index into the real array
     * @return value from the real array at the index
     * @throws IndexOutOfBoundsException if index not within array index range
     */
    public double getRealValue( int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > realVals.length)) {
            throw new IndexOutOfBoundsException("Real array index: " + index);
        }
        return realVals[index];
    }
    /**
     * This setter sets a value in the real array at the given index to the
     * new value
     * @param index index into the real array
     * @param value value to write into the array at the given index
     * @throws IndexOutOfBoundsException if index not within array index range
     */
    public void setRealValue( int index, double value ) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > realVals.length)) {
            throw new IndexOutOfBoundsException("Real array index: " + index);
        }
        this.realVals[index] = value;
    }
    /**
     * This method returns a reference to the real array.  This is used when
     * processing V(n) data values to V(n+1) data.
     * @return reference to the real array
     */
    public double[] getRealArray(){
        return this.realVals;
    }
    /**
     * This method sets the real array reference to the input array.  This is
     * used when processing V(n) data values to V(n+1) data.
     * @param inArray reference to a real array
     */
    public void setRealArray(double[] inArray){
        this.realVals = inArray;
    }
    /**
     * This method calculates the values for the valsPerLine, numberFormat, and
     * numLines fields based on the current array size and other field values.
     * @param packtype single-column or packed for array output format
     * @throws FormatException if the fieldWidth field is less than or equal to 0
     */
    public void buildArrayParams(SmArrayStyle packtype) throws FormatException {
        if (this.getFieldWidth() > 0 ) {
            if (packtype == SmArrayStyle.PACKED) {
                this.setValsPerLine(MAX_LINE_LENGTH / this.getFieldWidth());
            } else {
                this.setValsPerLine(1);
            }
        } else {
            throw new FormatException("Invalid field width of " + this.getFieldWidth());
        }
        this.setNumberFormat("(" + String.valueOf(this.getValsPerLine()) + 
                this.displayType + String.valueOf(this.getFieldWidth()) + 
                "." + String.valueOf(this.getPrecision()) + ")");
        this.calculateNumLines();
    }
    /**
     * This method takes each numeric value and converts it to its text
     * representation according to the formatting stored for the array.
     * Each text representation of a number is stored in an arrayList
     * @return arrayList of text-formatted numbers
     */
    @Override
    public ArrayList<String> arrayToText() {
        String outType = ("e".equalsIgnoreCase(this.displayType)) ? "e" : "f";
        //!!!update this line for floating pt. format
        String formatting = "%" + String.valueOf(this.getFieldWidth()) + "." +
                                String.valueOf(this.getPrecision())+ outType;
        ArrayList <String> textVals = new ArrayList<>();
        
        for (double each : realVals){
            textVals.add(String.format(formatting, each));
        }
        return textVals;
    }
    /**
     * Getter for the displayType field,i.e. "F", for floating point
     * @return the display type
     */
    public String getDisplayType() {
        return this.displayType;
    }
    /**
     * Setter for the displayType field
     * @param dtype type to set display type to, i.e. "F" or "E"
     */
    public void setDisplayType(String dtype) {
        this.displayType = dtype;
    }
}
