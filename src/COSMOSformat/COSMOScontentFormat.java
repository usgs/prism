/*******************************************************************************
 * Name: Java class COSMOSContentFormat.java
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
import SmException.SmException;
import java.io.File;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the base class for the COSMOS Strong Motion Data Format.  It extracts
 * a channel record from the input file and parses it into its various pieces.
 * Each COSMOS file type (V0-3) differs only in the format of its data section.
 * This base class defines the fields and methods common to all COSMOS files and
 * lets the extending classes define their data sections.
 * @author jmjones
 */
public class COSMOScontentFormat {
    protected final String procType; // data file type, such as 'V0'
    protected String channel; // channel number for this record 
    protected String rcrdId; // record id extracted from the cosmos file 
    protected String SCNLauth; // SCNL authorization tag from the cosmos file
    protected String SCNLcode; // just the SCNL code from the SCNLauth line
    protected String eventID; // event id for this record 
    protected int noIntVal;  // NoData value for integer header array 
    protected double noRealVal;  // NoData value for real header array 
    protected String[] textHeader;  // Holds the text header lines 
    protected VIntArray intHeader;  // Holds the integer header array 
    protected VRealArray realHeader;  // Holds the real header array
    protected String[] comments;  // Holds the comment field
    protected String endOfData;  // Holds the end-of-data line 
    protected String fileName; // holds the original file name 
    protected File stationDir; // holds the station directory for this record 
    /**
     * Default constructor
     * @param procType defines the data type of raw accel., uncorrected accel., 
     * etc.,
     */
    public COSMOScontentFormat( String procType){
        this.procType = procType;
        this.noIntVal = DEFAULT_NOINTVAL;  //default values
        this.noRealVal = DEFAULT_NOREALVAL;
        this.channel = "";
        this.rcrdId = "";
        this.SCNLauth = "";
        this.SCNLcode = "";
        this.eventID = "";
        this.fileName = "";
        this.stationDir = null;
    }
    /**
     * This method extracts the current component/channel from the input file. 
     * It is set up to handle multiple components in the same file by starting
     * at the given line number in the file and returning the line number at the
     * end of the current component.
     * @param start starting line in the file contents to parse the channel
     * @param infile array holding all the lines from the input file
     * @return the line number after the end of the current component
     * @throws FormatException if unable to extract expected values from text
     * @throws SmException if unable to convert text to numeric
     */
    public int loadComponent (int start, String[] infile) 
                                throws FormatException, SmException {
        int current = start;
        int channelNum;  //this is no longer used now that the SCNL code is in
                        //place in the comments, so channel will be set to the empty
                        //string in order to have channel info picked up from
                        //the SCNL tag instead.
        
        //Read in text header, look for number of lines and int, real NoData vals
        current = parseTextHeader(current, infile);
         
        //get integer header values !!channelNum no longer used, just set channel to ""
        channel = "";
        intHeader = new VIntArray();    
        current = intHeader.parseValues( current, infile);
        
        //get real header values
        realHeader = new VRealArray();     
        current = realHeader.parseValues( current, infile);
         
        //store commments
        current = parseComments ( current, infile);
        
        //Look for additional info in the comments
        checkForRcrdIdAndAuth();
        
        //get data values
        current = parseDataSection ( current, infile);
        
        //check for last line
        current = parseEndOfData( current, infile );
                
        return (current);
    }
    /**
     * This method must be overridden by each extended class.  Since each COSMOS
     * file differs only in the format of the data sections, each class extending
     * this for a specific V type must define the process for extracting the
     * data from the data arrays.
     * @param startLine line number where data section starts
     * @param infile array containing each line of the input file
     * @return the updated line number, after the data section
     * @throws FormatException if unable to extract parameters from format line
     */
    public int parseDataSection (int startLine, String[] infile) throws 
                                                            FormatException {
        System.err.println("method parseDataSection must be overridden");
        return startLine;
    }
    /**
     * This method extracts the text header to get the number of lines and the
     * NoData values.  It also saves the header for writing out other data products.
     * @param startLine line number where the text header starts
     * @param infile array containing each line of the input file
     * @return the updated line number, after the text header section
     * @throws FormatException if unable to extract expected parameters
     * @throws NumberFormatException if unable to convert text to numeric
     */
    private int parseTextHeader(int startLine, String[] infile) 
                                                        throws FormatException {
        int current = startLine;
        String line;
        String[] numbers;
        int numHeaderLines = 0;
        //look for num. of text lines
        String matchRegex = "(?i)(with \\d\\d text lines)";
        
        try {
            //get the first header line and extract the number of lines in the header
            line = infile[current];
            Pattern regField = Pattern.compile(matchRegex);
            Matcher m = regField.matcher( line );
            if (m.find()) {
                String[] num = m.group().split(" ");
                numHeaderLines = Integer.parseInt(num[1]);
            }
            else {
                throw new FormatException("Unable to find number of text header lines at line " + 
                                                                        (current+1));
            }
            //verify that the header lines are in the array, then extract NoData vals
            if ((numHeaderLines > 0) && (infile.length > (startLine + numHeaderLines))) {
                textHeader = new String[numHeaderLines];
                textHeader = Arrays.copyOfRange( infile, startLine, startLine+numHeaderLines);
                line = textHeader[NODATA_LINE].substring(textHeader[NODATA_LINE].lastIndexOf(":")+1);
                numbers = line.split(",");
                if (numbers.length == 2) {
                    noIntVal = Integer.parseInt(numbers[0].trim());
                    noRealVal = Double.parseDouble(numbers[1].trim());
                } else {
                    throw new FormatException("Unable to extract NoData values at line " + 
                                                        (current + NODATA_LINE + 1));
                }
            }
            else {
                throw new FormatException("Error in text header length of " + numHeaderLines);
            }
        } catch (NumberFormatException err) {
            throw new FormatException("Unable to convert text to numeric in text header");
        }
        return (startLine + textHeader.length);
    }
    /**
     * This method extracts and saves the comments for the channel.
     * @param startLine line number where the comments start
     * @param infile array containing each line of the input file
     * @return the updated line number, after the comment section
     * @throws FormatException if unable to locate expected parameters
     */
    private int parseComments(int startLine, String[] infile) 
                                                        throws FormatException {

        //at start of line, skip over any whitespace and pick up all digits
        String getDigitsRegex = "^((\\s*)(\\d+))";
        String commentRegex = "(?i).*comment.*";
        
        int current = startLine;
        int numComments = 0;
        String line = "";
        
        //get the first header line and extract the number of lines in the header
        if (infile.length > current) {
            line = infile[current];
        } else {
            throw new FormatException("EOF found before comments at line " 
                                                                    + (current+1));
        }
        try {
            //Make sure it's the comment section
            if (line.matches(commentRegex)) {
                //get the number of values at the start of the line
                Pattern regDigits = Pattern.compile( getDigitsRegex );
                Matcher m = regDigits.matcher( line );
                if (m.find(0)){
                    numComments = Integer.parseInt(m.group().trim());
                } else {
                    throw new FormatException("Could not find number of comment lines at " + 
                                                                        (current+1));
                }
            } else {
                throw new FormatException("Could not find comments at " + (current+1));
            }

            //verify that the comment lines are in the array
            if ((numComments > 0) && (infile.length > (current + numComments + 1))) {
                comments = new String[numComments+1];
                comments = Arrays.copyOfRange(infile,current,(current + numComments + 1));
            }
            else {
                throw new FormatException("Error in comment length of " + numComments);
            }
        } catch (NumberFormatException err) {
            throw new FormatException("Unable to convert text to numeric in comment line");
        }
        return (startLine + comments.length);
    }
    /**
     * This method extracts and saves the end-of-data line for the channel.
     * @param startLine line number where the EOD starts
     * @param infile array containing each line of the input file
     * @return the updated line number, after the end-of-data line
     * @throws FormatException if unable to locate expected parameters
     */
    private int parseEndOfData( int startLine, String[] infile) throws FormatException {
        String line;
        int current = startLine;
        //at start of line, skip over any whitespace and look for end-of-data,
        // case insensitive
        String endOfDataRegex = "^((\\s*)(?i)(End-of-data))";

        if (infile.length > current) {
            line = infile[current];        
            Pattern regDigits = Pattern.compile( endOfDataRegex );
            Matcher m = regDigits.matcher( line );
            if (m.find(0)){
                this.endOfData = line;
            } else {
                throw new FormatException("Could not find End-of-data at line " + 
                                                                    (current+1));
            }            
        } else {
            throw new FormatException("End-of-file found before end-of-data at line " + 
                                                                        (current+1));
        }
        return (current + 1);
    }
    /**
     * This  method must be overridden by each extending class.  It is called to
     * format the V component into its text file format for writing out to file.
     * @return the contents of the V record in COSMOS format
     */
    public String[] VrecToText () {
        String[] temp = new String[0];
        System.err.println("method VrecToText must be overridden");
        return temp;
    }
    /**
     * This method checks line 7 of the text header to see if it contains the 
     * record id, and checks the comments for the Authorization tag.  If either
     * of these are found they are saved for use in processing.  If the SCNL
     * tag is found and there is no defined channel number, the channel code
     * from the SCNL tag is saved for the channel identifier.
     */
    public void checkForRcrdIdAndAuth() {
        String line = this.textHeader[7];
        String[] segments;
        
        //Get the record id if available
        String matchRegex = "(RcrdId:)";
        Pattern regField = Pattern.compile(matchRegex);
        Matcher m = regField.matcher( line );
        rcrdId = (m.find()) ? line.substring(m.end()) : "";
        
        String findRegex = "(?i)(see co??m??me??nt)";
//        String findRegex = "(?i)(see comment)";
        Pattern findField = Pattern.compile(findRegex);
        Matcher f = findField.matcher( rcrdId );
        if (f.find()) {
            for (String each : this.comments) {
                m = regField.matcher( each);
                if (m.find()) {
                    rcrdId = each.substring(m.end()).trim();
                    break;
                }
            }
        }
        if (!rcrdId.isEmpty()) {
            segments = rcrdId.split("\\.");
            eventID = (segments.length > 3 ) ? segments[1] : "";
        }
        //Look for the SCNL and Auth tags and save if found
        String authRegex = "(<AUTH>)";
        Pattern authfield = Pattern.compile(authRegex);
        for (String each : this.comments) {
            m = authfield.matcher(each);
            if (m.find()) {
                SCNLauth = each.substring(1);
                break;                
            }
        }
        //Get the channel code if no channel number defined
        String scnlRegex = "(<SCNL>)(\\S+)";
        Pattern scnlfield = Pattern.compile(scnlRegex);
        if (!SCNLauth.isEmpty()) {
            m = scnlfield.matcher( SCNLauth );
            if (m.find()) {
                this.SCNLcode = m.group(2).trim();
                if (channel.isEmpty()) {
                    segments = m.group(2).split("\\.");
                    this.setChannel(segments[1] + "." + segments[3]);
                }
            }
        }
    }
    /**
     * Looks for the station name in either the header or the comments and
     * returns the name.
     * @return the text station name
     */
    public String checkForStationName() {
        String stationname = "";
        String line = this.textHeader[4].substring(40);
       if (line.isEmpty()) {
            return stationname;
        }
        Matcher m;
        String matchRegex = "(Station Name:)";
        Pattern regField = Pattern.compile(matchRegex);
        
        String findRegex = "(?i)(see co??m??me??nt)";
//        String findRegex = "(?i)(see comment)";
        Pattern findField = Pattern.compile(findRegex);
        Matcher f = findField.matcher( line );
        if (f.find()) {
            for (String each : this.comments) {
                m = regField.matcher( each);
                if (m.find()) {
                    stationname = each.substring(m.end()).trim();
                    break;
                }
            }
        } else {
            stationname = line;
        }
        return stationname;
    }
    /**
     * This method updates the End-of-data line to match the current processing
     * type.
     * @param dtype the data type to update the End-of-data line with, such as
     * UNCORACC, CORACC, VELOCITY, or DISPLACE.  If no match, then "response
     * spectra" is used.
     * @param channel the channel identifier
     */
    public void updateEndOfDataLine(String dtype, String channel) {
        String line = this.endOfData;
        String end = "";
        String group = "";
        String result = "";
        StringBuilder sb = new StringBuilder();
        String endOfDataRegex1 = "^(?i)(\\s*End-of-data\\s+for\\s+Chan\\s+\\S+)";
        String endOfDataRegex2 = "^(?i)(\\s*End-of-data\\s+for\\s+\\S+)";
        
        if (dtype.matches(UNCORACC) || (dtype.matches(CORACC))) {
            end = "acceleration";
        } else if (dtype.matches(VELOCITY)) {
            end = "velocity";
        } else if (dtype.matches(DISPLACE)) {
            end = "displacement";
        } else {
            end = "response spectra";
        }
        Pattern reg1 = Pattern.compile( endOfDataRegex1 );
        Pattern reg2 = Pattern.compile( endOfDataRegex2 );
        Matcher m1 = reg1.matcher( line );
        Matcher m2 = reg2.matcher( line );
        if (m1.find(0)){
            group = m1.group(0);
            result = sb.append(group).append(" ").append(end).toString();
        } else if (m2.find(0)) {
            group = m2.group(0);
            result = sb.append(group).append(" ").append(end).toString();
        } else {
            StringBuilder sb2 = new StringBuilder();
            result = sb2.append("End-of-data for Chan ").append(channel)
                                            .append(" ").append(end).toString();
        }
        this.endOfData = result;
    }
    /**
     * Getter for the channel number, which is needed for the output file name
     * @return channel number
     */
    public String getChannel(){
        return channel;
    }
    /**
     * Getter for the NoData value used in the integer header
     * @return integer NoData value
     */
    public int getNoIntVal(){
        return noIntVal;
    }
    /**
     * Getter for the NoData value used in the real header
     * @return real NoData value
     */
    public double getNoRealVal(){
        return noRealVal;
    }
    /**
     * Getter for the sensor location text in the text header.
     * @return the sensor location text
     */
    public String getSensorLocation() {
        String notFound = "";
        String location = textHeader[SENSOR_LOCATION_LINE];
        if (location.length() < SENSOR_LOCATION_START) {
            return notFound;
        }
        return location.substring(SENSOR_LOCATION_START);
    }
    /**
     * Setter for the channel number.
     * @param inChannel Characters to set the channel to
     */
    public void setChannel(String inChannel) {
            channel = inChannel;
    }
    /**
     * Getter for the event date and time from the Integer header values.
     * @return event date and time in string format, starting with UT and containing
     * year, month, day, hour, minute, and second separated by an underscore.
     */
    public String getEventDateTime() {
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        
        String year = String.format("%04d",intHeader.getIntValue(START_TIME_YEAR));
        String month = String.format("%02d",intHeader.getIntValue(START_TIME_MONTH));
        String day = String.format("%02d",intHeader.getIntValue(START_TIME_DAY));
        String hour = String.format("%02d",intHeader.getIntValue(START_TIME_HOUR));
        String min = String.format("%02d",intHeader.getIntValue(START_TIME_MIN));
        String sec = String.format("%02d",(int)Math.round(realHeader.getRealValue(START_TIME_SEC)));
        
        String eventtime = sb.append("UT_").append(year)
                             .append("_").append(month)
                             .append("_").append(day)
                             .append("_").append(hour)
                             .append("_").append(min)
                             .append("_").append(sec).toString();
        return eventtime;
    }
    /**
     * Getter for individual values from the real header.  
     * @param index location in real header to pick up value
     * @return the value from the header
     * @throws SmException if index is outside of real header range
     */
    public double getRealHeaderValue( int index ) throws SmException {
        double val = 0.0;
        try {
            val = realHeader.getRealValue(index);
        } catch (IndexOutOfBoundsException err) {
            throw new SmException("Real header index " + (index+1) + " is out of range");
        }
        return val;
    }
    /**
     * Setter for individual values in the real header.
     * @param index location in real header to update
     * @param value value to use to update header
     * @throws SmException if index is outside of real header range
     */
    public void setRealHeaderValue( int index, double value ) throws SmException {
        try {
            realHeader.setRealValue( index, value );
        } catch (IndexOutOfBoundsException err) {
            throw new SmException("Real header index " + (index+1) + " is out of range");
        }
        
    }
    /**
     * Setter for the real header format line, calls getters to get the number
     * of values, the number of lines, and the number format and updates the
     * format line.
     */
    public void setRealHeaderFormatLine() {
        String numvals = String.valueOf(realHeader.getNumVals());
        String numlines = String.valueOf(realHeader.getNumLines());
        String numberformat = realHeader.getNumberFormat();
        String line = String.format("%1$4s Real-header values follow on %2$3s lines, Format= ", 
                                                                numvals, numlines);
        realHeader.setFormatLine(line + numberformat);
    }
    /**
     * Getter for individual values from the integer header.  
     * @param index location in integer header to pick up value
     * @return the value from the header
     * @throws SmException if index is outside of integer header range
     */
    public int getIntHeaderValue( int index ) throws SmException {
        int val = 0;
        try {
            val = intHeader.getIntValue(index);
        } catch (IndexOutOfBoundsException err) {
            throw new SmException("Integer header index " + (index+1) + " is out of range");
        }
        return val;
    }
    /**
     * Setter for individual values from the integer header.
     * @param index location in integer header to pick up value
     * @param value value to use to update header
     * @throws SmException if index is outside of integer header range
     */
    public void setIntHeaderValue( int index, int value ) throws SmException {
        try {
            intHeader.setIntValue(index, value);
        } catch (IndexOutOfBoundsException err) {
            throw new SmException("Integer header index " + (index+1) + " is out of range");
        }
    }
    /**
     * This method retrieves a copy of the text header for use in creating a
     * new V component.
     * @return a copy of this component's text header
     */
    public String[] getTextHeader() {
        String[] textCopy = new String[textHeader.length];
        System.arraycopy(textHeader, 0 , textCopy, 0, textHeader.length);
        return textCopy;
    }
    /**
     * This method retrieves a copy of the comments for use in creating a
     * new V component.
     * @return a copy of this component's comments
     */
    public String[] getComments() {
        String[] textCopy = new String[comments.length];
        System.arraycopy(comments, 0 , textCopy, 0, comments.length);
        return textCopy;        
    }
    public void setComments( String[] newcomments ) {
        comments = newcomments;
    }
    /**
     * Takes the current list of comments and appends additional comments created
     * during processing.
     * @param comments the set of comments received from the V* file
     * @param lines a list of additional comments to append to the current comments
     * @return the updated comment list
     */
    public String[] updateComments(String[] comments, ArrayList<String> lines) {
        ArrayList<String> text = new ArrayList<>(Arrays.asList(comments));
        text.addAll(lines);
        StringBuilder sb = new StringBuilder();
        String start = text.get(0);
        sb.append(String.format("%4d",(text.size()-1)))
                .append(start.substring(4, start.length()));
        text.set(0,sb.toString());
        comments = new String[text.size()];
        comments = text.toArray(comments);
        lines.clear();
        text.clear();
        sb.setLength(0);
        return comments;
    }
    /**
     * Getter for the End-of-data line
     * @return End-of-data line
     */
    public String getEndOfData() {
        return endOfData;
    }
    /**
     * Getter for the process type of the data, i.e. Raw acceleration
     * @return process type
     */
    public String getProcType() {
        return procType;
    }
    /**
     * Getter for the file name for this record
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }
    /**
     * Setter for the file name for this record
     * @param inName file name
     */
    public void setFileName( String inName ) {
        fileName = inName;
    }
    /**
     * Getter for the station directory for this channel
     * @return station directory
     */
    public File getStationDir() {
        return stationDir;
    }
    /**
     * Setter for the station directory for this channel
     * @param inDir station directory
     */
    public void setStationDir( File inDir ) {
        stationDir = inDir;
    }
    /**
     * Getter for the record id for this channel
     * @return record id
     */
    public String getRcrdId() {
        return rcrdId;
    }
    /**
     * Getter for the SCNL and Authorization line from the comments
     * @return the SCNL and Auth tags and contents as a String
     */
    public String getSCNLauth() {
        return SCNLauth;
    }
    /**
     * Getter for the SCNL code from the comments
     * @return the SCNL as a String
     */
    public String getSCNLcode() {
        return SCNLcode;
    }
    /**
     * Getter for the event ID from either the text header or the comments
     * @return the event ID
     */
    public String getEventID() {
        return eventID;
    }
}
