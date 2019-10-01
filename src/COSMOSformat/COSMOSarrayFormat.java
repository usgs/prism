/*******************************************************************************
 * Name: Java class COSMOSarrayFormat.java
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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import SmException.FormatException;

/**
 * This abstract class defines fields and methods to work with numeric arrays 
 * extracted from COSMOS files.  It extracts the format line from the input 
 * text, pulls out and converts formatting text to numbers, and then uses these
 * to parse the numeric header/data values from the text and store in numeric
 * arrays.  It contains methods to allow access to the arrays during processing,
 * and it also converts the numeric arrays back to text for writing out
 * to another COSMOS file.
 * 
 * @author jmjones
 */
abstract class COSMOSarrayFormat {
    private String formatLine;  // save a copy of the line with formatting 
    private int numLines;  // total number of lines for numeric block 
    private int numVals;  // total number of values in numeric block 
    private String numberFormat;  // text of format, i.e. (10I8) 
    private int valsPerLine; // number of values packed per 80-char line 
    private int fieldWidth;  // number of characters for each numeric value
    private int precision;  // number of places after decimal point (reals)
    
    /**
     * Constructor for this class simply initializes the instance variables
     */
    public COSMOSarrayFormat(){
        this.numLines = 0;
        this.numVals = 0;
        this.numberFormat = "";
        this.valsPerLine = 0;
        this.fieldWidth = 0;
        this.precision = 0;
    }
    /**
     * This method is to be defined for each array type, to extract numeric
     * values and formatting information from the input text file.
     * @param startLine The line number in the text file to begin parsing array info.
     * @param infile The input text file, one line per string
     * @return The text file line number updated to the line after the array info.
     * @throws FormatException if unable to extract the formatting values
     */
    public abstract int parseValues( int startLine, String[] infile) 
                                throws FormatException;
    
    /**
     * This method takes the format line before either the header or the data
     * arrays and pulls out the needed values such as the number of values in 
     * the array, the number of values per line and the field width.
     * @param line the line from the text file with array formatting information
     * @throws FormatException if unable to find the necessary values in the line
     * or to convert text to numeric values
     */
    public void parseNumberFormatLine( String line) 
                                throws FormatException {
         
        //at start of line, skip over any whitespace and pick up all digits
        String getDigitsRegex = "^((\\s*)(\\d+))";
        //pick up (xxx) for the number format
        String formatRegex = "\\((\\d+)([A-Za-z]+)(\\d+)(\\.*)(\\d*)\\)";
        //look for groups of digits in the number format
        String fieldRegex = "(\\d+)";
        
        ArrayList<String> formatter;
        
        //set the copy of the header format line
        this.formatLine = line;
        
        try {
        
            //get the number of values at the start of the line
            Pattern regDigits = Pattern.compile( getDigitsRegex );
            Matcher m = regDigits.matcher( line );
            if (m.find(0)){
                this.numVals = Integer.parseInt(m.group().trim());
            } else {
                throw new FormatException("Could not find number of values in " + line);
            }
            //get the format as text
            Pattern regFormat = Pattern.compile(formatRegex);
            m = regFormat.matcher(line);
            if (m.find(0)){
                this.numberFormat = m.group().trim();
            } else {
                throw new FormatException("Could not find number format in " + line);
            }
            //get field width and optional precision (if real vals)
            Pattern regField = Pattern.compile(fieldRegex);
            m = regField.matcher( this.numberFormat );
            formatter = new ArrayList<>();
            while (m.find()) {
                formatter.add(m.group());
            }
            if ((formatter.size() == 2) && 
                    ((this.numberFormat.contains("I")) || (this.numberFormat.contains("i")))){
                this.valsPerLine = Integer.parseInt(formatter.get(0));
                this.fieldWidth = Integer.parseInt(formatter.get(1));
            } else if ((formatter.size() == 3) && 
                                ((this.numberFormat.contains("F")) || 
                                        (this.numberFormat.contains("E")) || 
                                            (this.numberFormat.contains("f")) || 
                                                (this.numberFormat.contains("e")))){
                this.valsPerLine = Integer.parseInt(formatter.get(0));
                this.fieldWidth = Integer.parseInt(formatter.get(1));
                this.precision = Integer.parseInt(formatter.get(2));
            } else {
                throw new FormatException("Could not extract format values in " + line);
            }
            formatter.clear();
        } catch (NumberFormatException err) {
            throw new FormatException("Unable to convert text to numbers in header/data format line");
        }
    }
    /**
     * This method pulls each string representation of a number out of the line
     * and puts it into a string arrayList.  It uses the field width to separate 
     * the values in each line and continues until the total value count is reached. 
     * Another method handles the text to numeric conversion.
     * @param startLine line number in input file contents where array begins
     * @param infile text file contents in an array of strings
     * @return arrayList of numeric values in text form, one value per entry
     * @throws FormatException if unpacked values from the format line are not 
     * valid or if the end-of-file is reached before all values extracted
     */
    public ArrayList<String> extractNumericVals(int startLine, String[] infile) 
                                                        throws FormatException {
        ArrayList<String> holdNumbers;
        int current = startLine;
        String line;
        int total = 0;
        
        //do some initial error checking
        if ((this.numVals <= 0) || (this.fieldWidth <= 0)) {
            throw new FormatException("Invalid number of values: " + this.numVals 
            + " or field width: " + this.fieldWidth);
        }
        holdNumbers = new ArrayList<>();
        while (total < this.numVals){
            current++;
            if (infile.length <= current) {
                throw new FormatException("Unexpected end-of-file at line " + current);
            }
            line = infile[current];
            if (line.length() < this.fieldWidth) {
                throw new FormatException("Could not extract number from line " + current);
            }
            for (int j = 0; j <= line.length()- this.fieldWidth; j = j + this.fieldWidth) {
                String num = line.substring(j, j + this.fieldWidth).trim();
                if ( !num.isEmpty()) {
                    holdNumbers.add(num);
                }
                total++;
            }
        }
        if (this.numVals != holdNumbers.size()) {
            throw new FormatException("Expected " + this.numVals + 
                            " values in array but found " + holdNumbers.size());
        }
        return holdNumbers;
    }
    /**
     * This method uses the number of data values and the number of values per
     * line extracted from the format line to calculate the number of lines of
     * data in the file.  Using integer math, round up the count if there's an
     * unfilled last line.
     * 
     * @return The number of text file lines holding the values of the array
     * @throws FormatException for an invalid value extracted for values per line
     */
    public int calculateNumLines() throws FormatException {
        if (this.valsPerLine > 0) {
            this.numLines = (this.numVals / this.valsPerLine) + 
                              (((this.numVals % this.valsPerLine) > 0) ? 1 : 0);
        } else {
            throw new FormatException("Invalid number of values per input line");
        }      
        return this.numLines;
    }
    /**
     * This method is defined for each array type using this abstract class.  Each
     * array type handles the conversion of each numeric value into text form
     * according to the defined formatting.  The result is an arrayList of strings
     * that can then be packed into lines for text file output.
     * 
     * @return arrayList of strings containing the text representation of each
     * number in the array
     */
    public abstract ArrayList<String> arrayToText();
    /**
     * This method converts the array and its format line into text
     * strings to be written to a file.  It calls arrayToText, which is defined
     * in each individual array type's methods, to convert each numeric value 
     * into a text string according to the output format.  This method then
     * packs each text string into a line.  It puts the format line at the
     * start and returns a string array ready to be appended to the text output.
     * 
     * @return array of strings ready for text file output
     */
    public String[] numberSectionToText() {
        int valsToPack = 0;
        int current = 0;
        int totalLength = 1 + this.numLines;
        String[] newText = new String[totalLength];
        newText[0] = this.getFormatLine();
        ArrayList<String> textVals = this.arrayToText();
        
        //pack each text value into a line according to the values per line
        for (int i=0; i<this.numLines; i++) {
            StringBuilder line = new StringBuilder();
            //calculate the number of vals to pack into a single line
            //use valsPerLine unless the last line has fewer
            valsToPack = ((this.numVals-current) >= this.valsPerLine) ? 
                                this.valsPerLine : (this.numVals - current);
            for (int next=0; next < valsToPack; next++){
                line.append(textVals.get(current));
                current++;
            }
            newText[i+1] = line.toString();
        }
        textVals.clear();
        return newText;
    }
    /**
     * This getter returns the full format line for the array
     * @return string containing the array's format line
     */
    public String getFormatLine(){
        return this.formatLine;
    }  
    /**
     * This setter sets the format line field to the input string
     * @param formatLine complete format line for this array
     */
    public void setFormatLine(String formatLine){
        this.formatLine = formatLine;
    }  
    /**
     * This getter returns the section of the format line holding number format,
     * such as '(10I8)' or '(8F10.3)'
     * @return the number format string
     */
    public String getNumberFormat(){
        return this.numberFormat;
    }  
    /**
     * Setter for the number format field
     * @param numberFormat the number format string, i.e. '(10I8)'
     */
    public void setNumberFormat(String numberFormat){
        this.numberFormat = numberFormat;
    } 
    /**
     * Getter for the numVals field
     * @return the number of values in the array
     */
    public int getNumVals(){
        return this.numVals;
    }    
    /**
     * Setter for the numVals field
     * @param numVals the number of values in the array
     */
    public void setNumVals( int numVals){
        this.numVals = numVals;
    }    
    /**
     * Getter for the number of lines needed to hold the array as text
     * @return the number of lines needed for text output
     */
    public int getNumLines(){
        return this.numLines;
    }    
    /**
     * Setter for the number of lines needed to hold the array as text
     * @param numLines the number of lines needed for text output
     */
    public void setNumLines( int numLines){
        this.numLines = numLines;
    }    
    /**
     * Getter for the number of numeric values (as text) to pack into a text line
     * @return the number of values packed into a line
     */
    public int getValsPerLine() {
        return this.valsPerLine;
    }
    /**
     * Setter for the number of array values to pack into a text line
     * @param valsPerLine the number of values packed into a line
     */
    public void setValsPerLine( int valsPerLine ) {
        this.valsPerLine = valsPerLine;
    }
    /**
     * Getter for the field width for numbers in the text array lines
     * @return the field width
     */
    public int getFieldWidth() {
        return this.fieldWidth;
    }
    /**
     * Setter for the field width for converting numbers to text in the arrays
     * @param fieldWidth field width for converting numbers to text
     */
    public void setFieldWidth( int fieldWidth ) {
        this.fieldWidth = fieldWidth;
    }
    /**
     * Getter for the precision for conversion of doubles to text
     * @return the precision
     */
    public int getPrecision() {
        return this.precision;
    }
    /**
     * Setter for the precision for conversion of doubles to text
     * @param precision the type double precision
     */
    public void setPrecision( int precision ) {
        this.precision = precision;
    }
}
