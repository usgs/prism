/*******************************************************************************
 * Name: Java class V1Component.java
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
import SmProcessing.ArrayOps;
import SmProcessing.ArrayStats;
import SmProcessing.V1Process;
import SmUtilities.ConfigReader;
import SmUtilities.ProcessStepsRecorder2;
import static SmConstants.SmConfigConstants.OUT_ARRAY_FORMAT;
import static SmConstants.SmConfigConstants.PROC_AGENCY_ABBREV;
import static SmConstants.SmConfigConstants.PROC_AGENCY_CODE;
import SmUtilities.CommentFormatter;
import SmUtilities.SmTimeFormatter;
import java.time.ZonedDateTime;

/**
 * This class extends the COSMOScontentFormat base class to define a V1 record.
 * It defines methods to parse the V1 data array, build a new V1 component
 * from V1 processing results, and convert the V1 component into text for writing
 * out to file.
 * @author jmjones
 */
public class V1Component extends COSMOScontentFormat {
    private VRealArray V1Data; // VRealArray object of uncorrected acceleration counts 
    private final V0Component parentV0; // link back to the parent V0 record 

    /** 
     * Use this constructor when the V1 component is read in from a file and
     * filled in with the loadComponent method.  In this case, there is no parentV0
     * associated with this V1
     * @param procType process level indicator (i.e. "V1")
     */
    public V1Component( String procType) {
        super( procType );
        this.parentV0 = null;
    }
    /**
     * Use this constructor when the V1 component is created from processing
     * done on a V0 component.  In this case, the contents of V1 are initialized
     * to the V0 values and updated during the processing.
     * @param procType process level indicator (i.e. "V1")
     * @param pV0 reference to the parent V0Component
     */
    public V1Component( String procType, V0Component pV0) {
        super( procType );
        this.parentV0 = pV0;
        //Load the text header with parent V0 values.  Leave the update to the V1
        //values to the buildV1 method.
        this.noIntVal = pV0.noIntVal;
        this.noRealVal = pV0.noRealVal;
        this.textHeader = pV0.getTextHeader();
        
        //Load the headers with parent V0 values.
        //Leave updates for buildV1 method
        this.intHeader = new VIntArray(pV0.intHeader);        
        this.realHeader = new VRealArray(pV0.realHeader);
        this.setChannel(pV0.getChannel());
        this.fileName = pV0.getFileName();
        this.rcrdId = pV0.getRcrdId();
        this.SCNLauth = pV0.getSCNLauth();
        this.eventID = pV0.getEventID();
        this.SCNLcode = pV0.getSCNLcode();
        
        //The buildV1 method fills in these data values, the format line, and
        //the individual params for the real array.
        this.V1Data = new VRealArray();
        
        this.comments = pV0.getComments(); //leave update for processing, if any
        this.endOfData = pV0.endOfData;
        this.updateEndOfDataLine(UNCORACC, pV0.getChannel());
    }
    /**
     * This method defines the steps for parsing a V1 data record, which contains
     * a floating point data array.
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
        
        V1Data = new VRealArray();
        current = V1Data.parseValues( current, infile);
        return current;
    }
    /**
     * Getter for the parent V0 object
     * @return reference to the parent V0Component
     */
    public V0Component getParent() {
        return this.parentV0;
    }
    /**
     * Getter for the length of the data array
     * @return the number of values in the data array
     */
    public int getDataLength() {
        return V1Data.getNumVals();
    }
    /**
     * Getter for a copy of the data array reference.  Used to access the entire
     * array during data processing.
     * @return a copy of the array reference
     */
    public double[] getDataArray() {
        return V1Data.getRealArray();
    }
    public double getRealValue(int index) {return this.realHeader.getRealValue(index);}
    public int getIntValue(int index) {return this.intHeader.getIntValue(index);}
    /**
     * This method builds the V1 component from the V1 process object, picking
     * up the data array and updating header parameters and format lines.  Once
     * in this method, the V1Process object is no longer needed and its array
     * is transferred to the V1Component object.
     * @param inVvals the V1Process object
     * @throws SmException if unable to access the header values
     * @throws FormatException if unable to format the numeric values to text
     */
    public void buildV1 (V1Process inVvals) throws 
                                                SmException, FormatException {
        Double epsilon = 0.001;
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        StringBuilder eod = new StringBuilder(MAX_LINE_LENGTH);
        String realformat = "%8.3f";

        SmTimeFormatter proctime = new SmTimeFormatter();
        ConfigReader config = ConfigReader.INSTANCE;
        
        this.realHeader.setFieldWidth(DEFAULT_REAL_FIELDWIDTH);
        this.realHeader.buildArrayParams( SmArrayStyle.PACKED );
        this.setRealHeaderFormatLine();
        
        double delta_t = this.realHeader.getRealValue(DELTA_T);
        double ptime = (inVvals.getPeakIndex()) * MSEC_TO_SEC * delta_t;

        //Get the processing agency info from the config. data
        String agabbrev = config.getConfigValue(PROC_AGENCY_ABBREV);
        if (agabbrev == null) {
            agabbrev = DEFAULT_AG_CODE;
        }
        String agcode = config.getConfigValue(PROC_AGENCY_CODE);
        if (agcode != null) {
            int agency_code = Integer.parseInt(agcode);
            this.intHeader.setIntValue(PROCESSING_AGENCY, agency_code);
        }
        
        //Get units info from V1 processing object
        String unitsname = inVvals.getDataUnits();
        int unitscode = inVvals.getDataUnitCode();
        
        //Get the array output format of single column per channel or packed
        String arrformat = config.getConfigValue(OUT_ARRAY_FORMAT);
        arrformat = (arrformat == null) ? DEFAULT_ARRAY_STYLE : arrformat;
        SmArrayStyle packtype = (arrformat.equalsIgnoreCase("singleColumn")) ? 
                              SmArrayStyle.SINGLE_COLUMN : SmArrayStyle.PACKED;

        //Get the current processing time
        String val = proctime.getGMTdateTime();
        //update values in the text header
        this.textHeader[0] = UNCORACC.concat(this.textHeader[0].substring(END_OF_DATATYPE));
        this.textHeader[10] = sb.append("Processed:").append(val).append(", ")
                                .append(agabbrev).append(", Max = ")
                                .append(String.format(realformat,inVvals.getPeakVal()))
                                .append(" ").append(unitsname).append(" at ")
                                .append(String.format(realformat,ptime))
                                .append(" sec").toString();
        
        //transfer the data array and set all array values
        V1Data.setRealArray(inVvals.getV1Array());
        V1Data.setFieldWidth(REAL_FIELDWIDTH_V1);
        V1Data.setPrecision(REAL_PRECISION_V1);
        V1Data.setDisplayType("E");
        V1Data.setNumVals(inVvals.getV1ArrayLength());
        V1Data.buildArrayParams( packtype );
        this.buildNewDataFormatLine(unitsname, unitscode);
        
        //update the headers with the V1 values
        this.intHeader.setIntValue(PROCESSING_STAGE_INDEX, V1_STAGE);
        this.intHeader.setIntValue(V_UNITS_INDEX, unitscode);
        this.intHeader.setIntValue(DATA_PHYSICAL_PARAM_CODE, ACC_PARM_CODE);
        
        this.realHeader.setRealValue(PEAK_VAL, inVvals.getPeakVal());
        this.realHeader.setRealValue(AVG_VAL, inVvals.getAvgVal());
        this.realHeader.setRealValue(PEAK_VAL_TIME, ptime);
        this.realHeader.setRealValue(MEAN_ZERO, inVvals.getMeanToZero());

//        ProcessStepsRecorder2 stepRec = ProcessStepsRecorder2.INSTANCE;
//        ArrayList<String> psteps = stepRec.formatSteps();
//        if (!psteps.isEmpty()) {
//            this.comments = super.updateComments(this.comments, psteps);
//            psteps.clear();
//        }
        setComments(inVvals.getCommentUpdates());
        this.updateEndOfDataLine(UNCORACC, this.parentV0.getChannel());
    }
    /**
     * This method creates a new data format line for the V1 component data array.
     * It calculates the time based on the number of data values and delta t
     * and gets the physical units from the configuration file.
     * @param units the numeric code for the type of units, COSMOS table 2
     * @param unitsCode code containing the type of units (cm, cm/sec, etc.)
     * @throws SmException if unable to access values in the headers
     */
    public void buildNewDataFormatLine(String units, int unitsCode) throws SmException {
        //calculate the time by multiplying the number of data values by delta t
        String line;
        double dtime = this.getRealHeaderValue(DELTA_T);
        int numvals = V1Data.getNumVals();
        double calcTime = dtime * numvals * MSEC_TO_SEC;
        String timeSec = Integer.toString((int)calcTime);
        String datType = "acceleration";
        line = String.format("%1$8s %2$12s pts, approx %3$4s secs, units=%4$7s(%5$02d),Format=",
                                     String.valueOf(numvals),datType,
                                                    timeSec, units, unitsCode);
        V1Data.setFormatLine(line + V1Data.getNumberFormat());
    }
    /**
     * Getter for the data format line
     * @return the data format line
     */
    public String getDataFormatLine() {
        return V1Data.getFormatLine();
    }
    /**
     * This method converts the V1 component stored in memory into its text
     * format for writing to a file.
     * @return a text array with the V1 component in COSMOS format for a file
     */
    @Override
    public String[] VrecToText() {
        //add up the length of the text portions of the component, which are
        //the text header, the comments, and the end-of-data line.
        int totalLength = 0;
        int currentLength = 0;
        int textLength = this.textHeader.length + this.comments.length + 1;
        
        //get the header and data arrays as text
        String[] intHeaderText = this.intHeader.numberSectionToText();
        String[] realHeaderText = this.realHeader.numberSectionToText();
        String[] V1DataText = this.V1Data.numberSectionToText();
        
        //add the array lengths to the text lengths to get the total and declare
        //an array of this length, then build it by combining all the component
        //pieces into a text version of the component.
        totalLength = textLength + intHeaderText.length + realHeaderText.length + 
                                                        V1DataText.length;
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
        System.arraycopy(V1DataText, 0, outText, currentLength, V1DataText.length);
        outText[totalLength-1] = this.endOfData;
        return outText;
    }
    /**
     * This method is used by the GUI review tool during the trim process. After
     * an uncorrected acceleration array is trimmed, the array is updated and
     * new statistics calculated.  Line 10(11) of the text header is updated with
     * the current time and the new statistics, and the data format line is
     * also updated.
     * @param inArray trimmed uncorrected acceleration array.  This array is modified
     * if a non-zero mean is calculated (in this case the mean will be removed
     * from the array).
     * @param newStartTime ZonedDateTime-formatted updated record start time
     * @param startcount number of points removed from the beginning of the array
     * @param endcount number of points removed from the end of the array
     * @return the original data format line, before the trim (optional, for debug)
     * @throws SmException if unable to update the array
     * @throws FormatException if error occurs building the data format line
     */
    public String updateArray(double[] inArray, ZonedDateTime newStartTime, 
            int startcount, int endcount) throws SmException, FormatException{
        double meanToZero, avgVal, peakVal, peakIndex;
        meanToZero = 0.0;
        String realformat = "%8.3f";
        String unitsname, agabbrev;
        int unitscode;
        CommentFormatter formatter = new CommentFormatter();
        comments = formatter.addCorrectionType(comments, CorrectionType.MANUAL);
//        ProcessStepsRecorder2 stepRec  = ProcessStepsRecorder2.INSTANCE;
//        stepRec.clearSteps();
//        stepRec.addCorrectionType(CorrectionType.MANUAL);
        
        //Remove the mean from the array and save for the Real Header
        meanToZero = ArrayOps.findAndRemoveMean(inArray);
        V1Data.setRealArray(inArray);
        V1Data.setFieldWidth(REAL_FIELDWIDTH_V1);
        V1Data.setPrecision(REAL_PRECISION_V1);
        V1Data.setDisplayType("E");
        V1Data.setNumVals(inArray.length);
        SmArrayStyle numStyle = (V1Data.getValsPerLine() > 1) ? SmArrayStyle.PACKED : 
                                                                SmArrayStyle.SINGLE_COLUMN;
        V1Data.buildArrayParams(numStyle);
        
        //Find the new mean (should now be close to zero) and the location and mag. of peak value
        ArrayStats stat = new ArrayStats( inArray );
        avgVal = stat.getMean();
        peakVal = stat.getPeakVal();
        peakIndex = stat.getPeakValIndex();
                
        unitscode = this.intHeader.getIntValue(UNITS_CODE);
        unitsname = ((unitscode >= 0) && (unitscode < SMARRAYUNITS.length)) ? SMARRAYUNITS[unitscode] : UNKNOWN_UNITS;
        String currentline = this.getDataFormatLine();
        
        try {
            agabbrev = this.textHeader[10].substring(AGENCY_ABBR,AGENCY_ABBR+4);
        } catch (IndexOutOfBoundsException err) {
            agabbrev = DEFAULT_AG_CODE;
        }
        this.buildNewDataFormatLine(unitsname, unitscode);
        
        //Update line 8, the record start time, and the real and int header
        //values associated with the time
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        SmTimeFormatter zonetime = new SmTimeFormatter(newStartTime);
        String startformat = zonetime.getUTCdateTime();
        String currenttext = this.textHeader[7];
        sb.append("Rcrd start time:").append(startformat);
        if (currenttext.length() > END_START_TIME) {
            sb.append(currenttext.substring(END_START_TIME,currenttext.length()));
        }
        this.textHeader[7] = sb.toString();
        double delta_t = this.realHeader.getRealValue(DELTA_T);
        double seriesLength = delta_t * inArray.length * MSEC_TO_SEC;
        double ptime = peakIndex * MSEC_TO_SEC * delta_t;
        SmTimeFormatter proctime = new SmTimeFormatter();
        String val = proctime.getGMTdateTime();  //Get the current processing time
        sb = new StringBuilder(MAX_LINE_LENGTH);
        this.textHeader[10] = sb.append("Processed:").append(val).append(", ")
                                .append(agabbrev).append(", Max = ")
                                .append(String.format(realformat,peakVal))
                                .append(" ").append(unitsname).append(" at ")
                                .append(String.format(realformat,ptime))
                                .append(" sec").toString();
        
        // Update the array and the new procesing parameters
        this.realHeader.setRealValue(SERIES_LENGTH, seriesLength);
        this.realHeader.setRealValue(PEAK_VAL, peakVal);
        this.realHeader.setRealValue(AVG_VAL, avgVal);
        this.realHeader.setRealValue(PEAK_VAL_TIME, ptime);
        this.realHeader.setRealValue(MEAN_ZERO, meanToZero);
        
        //Update the date and time values
        this.intHeader.setIntValue(START_TIME_YEAR, zonetime.getUTCyear());
        this.intHeader.setIntValue(START_TIME_JULDAY, zonetime.getUTCjulday());
        this.intHeader.setIntValue(START_TIME_MONTH, zonetime.getUTCmonth());
        this.intHeader.setIntValue(START_TIME_DAY, zonetime.getUTCday());
        this.intHeader.setIntValue(START_TIME_HOUR, zonetime.getUTChour());
        this.intHeader.setIntValue(START_TIME_MIN, zonetime.getUTCminute());
        this.realHeader.setRealValue(START_TIME_SEC, zonetime.getUTCsecond());
        
        //Update the comments to include the trim info
        comments = formatter.addTrimIndicies(comments, startcount, endcount);
//        stepRec.addTrimIndicies(startcount, endcount);
//        newComments = stepRec.formatSteps();
//        this.comments = super.updateComments(this.comments, newComments);
//        newComments.clear();
//        stepRec.clearSteps();
        
        return currentline;
    }
}
