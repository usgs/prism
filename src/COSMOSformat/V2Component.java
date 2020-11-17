/*******************************************************************************
 * Name: Java class V2Component.java
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

import static SmConstants.SmConfigConstants.BP_FILTER_ORDER;
import static SmConstants.VFileConstants.*;
import SmConstants.VFileConstants.SmArrayStyle;
import SmConstants.VFileConstants.V2DataType;
import SmException.FormatException;
import SmException.SmException;
import SmProcessing.V2Process;
import SmUtilities.ConfigReader;
import SmUtilities.ProcessStepsRecorder2;
import static SmConstants.SmConfigConstants.OUT_ARRAY_FORMAT;
import static SmConstants.SmConfigConstants.PROC_AGENCY_ABBREV;
import static SmConstants.SmConfigConstants.PROC_AGENCY_CODE;
import SmUtilities.CommentFormatter;
import SmUtilities.SmTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class extends the COSMOScontentFormat base class to define a V2 record.
 * It defines methods to parse the V2 data array, build a new V2 component
 * from V2 processing results, and convert the V2 component into text for writing
 * out to file.
 *
 * @author jmjones
 */
public class V2Component extends COSMOScontentFormat {
    private VRealArray V2Data; // VRealArray object holding the data array 
    private final V0Component parentV0; // link back to the parent V0 record
    private final V1Component parentV1; // link back to the parent V1 record 

    /** 
     * Use this constructor when the V2 component is read in from a file and
     * filled in with the loadComponent method.  In this case, there is no parentV1
     *associated with this V2
     * @param procType process level indicator (i.e. "V2")
     */
    public V2Component( String procType){
        super( procType );
        this.parentV0 = null;
        this.parentV1 = null;
    }
    /**
     * Use this constructor when the V2 component is created from processing
     * done on a V1 component.  In this case, the contents of V2 are initialized
     * to the V1 values and updated during the processing.
     * @param procType process level indicator (i.e. "V2")
     * @param pV1 reference to the parent V1Component
     */
    public V2Component( String procType, V1Component pV1) {
        super( procType );
        this.parentV1 = pV1;
        this.parentV0 = pV1.getParent();
        //Load the text header with parent V1 values.  Leave the update to the V2
        //values to the buildV2 method.
        this.noIntVal = pV1.noIntVal;
        this.noRealVal = pV1.noRealVal;
        this.textHeader = pV1.getTextHeader();
        
        //Load the headers with parent V1 values.
        //Leave updates for buildV2 method
        this.intHeader = new VIntArray(pV1.intHeader);        
        this.realHeader = new VRealArray(pV1.realHeader);
        this.setChannel(pV1.getChannel());
        this.fileName = pV1.getFileName();
        this.rcrdId = pV1.getRcrdId();
        this.SCNLauth = pV1.getSCNLauth();
        this.eventID = pV1.getEventID();
        this.SCNLcode = pV1.getSCNLcode();
        
        //The buildV2 method fills in these data values, the format line, and
        //the individual params for the real arrays.
        this.V2Data = new VRealArray();
        
        this.comments = pV1.getComments(); //leave update for processing, if any
        this.endOfData = pV1.endOfData; //leave update for buildV2
    }
    /**
     * This method defines the steps for parsing a V2 data record, which contains
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
        
        V2Data = new VRealArray();
        current = V2Data.parseValues( current, infile);
        return current;
    }
    /**
     * Getter for the parent V1 object
     * @return reference to the parent V1Component
     */
    public V1Component getParent() {
        return this.parentV1;
    }
    /**
     * Getter for the length of the data array
     * @return the number of values in the data array
     */
    public int getDataLength() {
        return V2Data.getNumVals();
    }
    /**
     * Getter for a copy of the data array reference.  Used to access the entire
     * array during data processing.
     * @return a copy of the array reference
     */
    public double[] getDataArray() {
        return V2Data.getRealArray();
    }
    /**
     * This method builds the V2 component from the V2 process object, picking
     * up the data array and updating header parameters and format lines. Once
     * in this method, the V2Process object is no longer needed and its array
     * is transferred to the V2Component object. Use this method when processing
     * V2Processes iteratively. This method includes a parameter representing 
     * an array of process steps that were applied to the relative V2Process 
     * instance (i.e., inVvals). This parameter is used in liu of using the 
     * V2Component's own ProcessStepsRecorder instance to update the comments.
     * The V2Component's ProcessStepsRecorder instance would simply override
     * previous comments with each pass and, thus, would not work correctly
     * when performing iterative processing of V2Processes. Using this method, 
     * however, will ensure that a separate set of comments is generated for 
     * each V2Process iteration.
     * 
     * @param procType ACC, VEL, or DIS for the V2 data type
     * @param inVvals the V2Process object
     * @param processSteps string array of process steps
     * @throws SmException if unable to access the header values
     * @throws FormatException if unable to format the numeric values to text
     */
    public void buildV2( V2DataType procType, V2Process inVvals, ArrayList<String>processSteps) 
                                            throws SmException, FormatException {
        Double epsilon = 0.001;
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        final double MSEC_TO_SEC = 1e-3;
        String realformat = "%8.3f";
        String freqformat5 = "%5.1f";
        String freqformat6 = "%6.2f";
        double time;
        String unitsname;
        int unitscode;

        SmTimeFormatter proctime = new SmTimeFormatter();
        ConfigReader config = ConfigReader.INSTANCE;
        
        double delta_t;
        double dtime;
        if (inVvals.getResampleIndicator()) {
            dtime = 1.0 / inVvals.getSampleRate();
            delta_t = dtime * (1.0 / MSEC_TO_SEC);
        } else {
            delta_t = this.realHeader.getRealValue(DELTA_T);
            if ((Math.abs(delta_t - this.noRealVal) < epsilon) || (delta_t < 0.0)){
                throw new SmException("Real header #62, delta t, is invalid: " + 
                                                                            delta_t);
            }
            dtime = MSEC_TO_SEC * delta_t;
        }
        
        //Get the time that the peak value occurred for the given data type
        if (procType == V2DataType.ACC) {
            time = (inVvals.getPeakIndex(V2DataType.ACC)) * dtime;
            unitsname = inVvals.getDataUnits(V2DataType.ACC);
            unitscode = inVvals.getDataUnitCode(V2DataType.ACC);
        } else if (procType == V2DataType.VEL) {
            time = (inVvals.getPeakIndex(V2DataType.VEL)) * dtime;
            unitsname = inVvals.getDataUnits(V2DataType.VEL);
            unitscode = inVvals.getDataUnitCode(V2DataType.VEL);
        } else {
            time = (inVvals.getPeakIndex(V2DataType.DIS)) * dtime;
            unitsname = inVvals.getDataUnits(V2DataType.DIS);
            unitscode = inVvals.getDataUnitCode(V2DataType.DIS);
        }
        
        //Get the processing agency info from the config. data
        String agabbrev = config.getConfigValue(PROC_AGENCY_ABBREV);
        if (agabbrev == null) {
            agabbrev = DEFAULT_AG_CODE;
        }
        String agcode = config.getConfigValue(PROC_AGENCY_CODE);
        int agency_code = (agcode == null) ? 0 : Integer.parseInt(agcode);
        
        //Get the array output format of single column per channel or packed
        String arrformat = config.getConfigValue(OUT_ARRAY_FORMAT);
        arrformat = (arrformat == null) ? DEFAULT_ARRAY_STYLE : arrformat;
        SmArrayStyle packtype = (arrformat.equalsIgnoreCase("singleColumn")) ? 
                              SmArrayStyle.SINGLE_COLUMN : SmArrayStyle.PACKED;
        
        //Get the current processing time
        String val = proctime.getGMTdateTime();
        
        //update values in the text header
        if (procType == V2DataType.ACC) {
            this.textHeader[0] = CORACC.concat(this.textHeader[0].substring(END_OF_DATATYPE));
            this.textHeader[10] = sb.append("Processed:").append(val).append(", ")
                                .append(agabbrev).append(", Max = ")
                                .append(String.format(realformat,inVvals.getPeakVal(V2DataType.ACC)))
                                .append(" ").append(unitsname).append(" at ")
                                .append(String.format(realformat,time))
                                .append(" sec").toString();
        } else if (procType == V2DataType.VEL) {
            this.textHeader[0] = VELOCITY.concat(this.textHeader[0].substring(END_OF_DATATYPE));
            this.textHeader[10] = sb.append("Processed:").append(val).append(", ")
                                .append(agabbrev).append(", Max = ")
                                .append(String.format(realformat,inVvals.getPeakVal(V2DataType.VEL)))
                                .append(" ").append(unitsname).append(" at ")
                                .append(String.format(realformat,time))
                                .append(" sec").toString();
        } else {
            this.textHeader[0] = DISPLACE.concat(this.textHeader[0].substring(END_OF_DATATYPE));
            this.textHeader[10] = sb.append("Processed:").append(val).append(", ")
                                .append(agabbrev).append(", Max = ")
                                .append(String.format(realformat,inVvals.getPeakVal(V2DataType.DIS)))
                                .append(" ").append(unitsname).append(" at ")
                                .append(String.format(realformat,time))
                                .append(" sec").toString();
        }
        sb = new StringBuilder(MAX_LINE_LENGTH);
        this.textHeader[11] = sb.append("Record filtered below")
                                .append(String.format(freqformat6,inVvals.getLowCut()))
                                .append(" Hz (periods over")
                                .append(String.format(freqformat6,(1.0/inVvals.getLowCut())))
                                .append(" secs), and above")
                                .append(String.format(freqformat5,inVvals.getHighCut()))
                                .append(" Hz")
                                .toString();
        
        //transfer the data array and set all array values
        V2Data.setFieldWidth(REAL_FIELDWIDTH_V2);
        V2Data.setPrecision(REAL_PRECISION_V2);
        V2Data.setDisplayType("E");
        
        if (procType == V2DataType.ACC) {
            V2Data.setRealArray(inVvals.getV2Array(V2DataType.ACC));
            V2Data.setNumVals(inVvals.getV2ArrayLength(V2DataType.ACC));
            V2Data.buildArrayParams( packtype );
            this.buildNewDataFormatLine(unitsname, unitscode, "acceleration", dtime);
        } else if (procType == V2DataType.VEL) {
            V2Data.setRealArray(inVvals.getV2Array(V2DataType.VEL));
            V2Data.setNumVals(inVvals.getV2ArrayLength(V2DataType.VEL));
            this.realHeader.setRealValue(INITIAL_VELOCITY_VAL, V2Data.getRealValue(0));
            V2Data.buildArrayParams( packtype );
            this.buildNewDataFormatLine(unitsname, unitscode, "velocity    ", dtime);
        }else {
            V2Data.setRealArray(inVvals.getV2Array(V2DataType.DIS));
            V2Data.setNumVals(inVvals.getV2ArrayLength(V2DataType.DIS));
            this.realHeader.setRealValue(INITIAL_DISPLACE_VAL, V2Data.getRealValue(0));            
            V2Data.buildArrayParams( packtype );
            this.buildNewDataFormatLine(unitsname, unitscode, "displacement", dtime);            
        }
        
        //update the headers and end-of-data line with the V2 values
        if (inVvals.getResampleIndicator()) {
            this.realHeader.setRealValue(DELTA_T, delta_t);
        }
        this.intHeader.setIntValue(PROCESSING_STAGE_INDEX, V2_STAGE);
        this.realHeader.setRealValue(PEAK_VAL_TIME, time);
        this.intHeader.setIntValue(V_UNITS_INDEX, unitscode);
        this.realHeader.setRealValue(SCALING_FACTOR, FROM_G_CONVERSION);
        this.intHeader.setIntValue(LOW_FREQ_FILTER_TYPE, BUTTER_A_CODE);
        this.intHeader.setIntValue(HIGH_FREQ_FILTER_TYPE, BUTTER_A_CODE);
        this.realHeader.setRealValue(LOW_FREQ_CORNER, inVvals.getLowCut());
        this.realHeader.setRealValue(HIGH_FREQ_CORNER, inVvals.getHighCut());
        this.intHeader.setIntValue(FILTER_DOMAIN_FLAG, TIME_DOMAIN);
        this.realHeader.setRealValue(INITIAL_VELOCITY_VAL, inVvals.getInitialVelocity());
        this.realHeader.setRealValue(INITIAL_DISPLACE_VAL, inVvals.getInitialDisplace());
        this.realHeader.setRealValue(BRACKETED_DURATION, inVvals.getBracketedDuration());
        this.realHeader.setRealValue(DURATION_INTERVAL, inVvals.getDurationInterval());
        this.realHeader.setRealValue(CUMULATIVE_ABS_VEL, inVvals.getCumulativeAbsVelocity());
        this.realHeader.setRealValue(FILTER_DECAY_LOW,Integer.parseInt(config.getConfigValue(BP_FILTER_ORDER)));
        this.realHeader.setRealValue(FILTER_DECAY_HI,Integer.parseInt(config.getConfigValue(BP_FILTER_ORDER)));
        
        //Housner intensity is calculated during V3 processing and appears in V3 product
        this.realHeader.setRealValue(RMS_ACCELERATION, inVvals.getRMSacceleration());
        this.realHeader.setRealValue(ARIAS_INTENSITY, inVvals.getAriasIntensity());
        
        this.endOfData = this.parentV1.endOfData;
        if (procType == V2DataType.ACC) {
            this.intHeader.setIntValue(DATA_PHYSICAL_PARAM_CODE, ACC_PARM_CODE);
            this.realHeader.setRealValue(PEAK_VAL, inVvals.getPeakVal(V2DataType.ACC));
            this.realHeader.setRealValue(AVG_VAL, inVvals.getAvgVal(V2DataType.ACC));
            this.updateEndOfDataLine(CORACC, this.getChannel());
        } else if (procType == V2DataType.VEL) {
            this.intHeader.setIntValue(DATA_PHYSICAL_PARAM_CODE, VEL_PARM_CODE);
            this.realHeader.setRealValue(PEAK_VAL, inVvals.getPeakVal(V2DataType.VEL));
            this.realHeader.setRealValue(AVG_VAL, inVvals.getAvgVal(V2DataType.VEL));
            this.updateEndOfDataLine(VELOCITY, this.getChannel());
        } else {
            this.intHeader.setIntValue(DATA_PHYSICAL_PARAM_CODE, DIS_ABS_PARM_CODE);
            this.realHeader.setRealValue(PEAK_VAL, inVvals.getPeakVal(V2DataType.DIS));
            this.realHeader.setRealValue(AVG_VAL, inVvals.getAvgVal(V2DataType.DIS));            
            this.updateEndOfDataLine(DISPLACE, this.getChannel());
        }
        
        //Update the comments with processing steps
        if (processSteps != null) {
            this.comments = super.updateComments(this.comments, processSteps);
        } else {
            if (inVvals.getQCStatus() == V2Status.GOOD) {
                this.comments = inVvals.getUpdatedComments();
            }
        }
    }
    
    /**
     * This method creates a new data format line for the V2 component data array.
     * It calculates the time based on the number of data values and delta t
     * and gets the physical units from the configuration file.
     * @param units the numeric code for the type of units, COSMOS table 2
     * @param unitscode code containing the type of units (cm, cm/sec, etc.)
     * @param dataType "acceleration", "velocity", or "displacement"
     * @param timestep the time in seconds between samples
     * @throws SmException if unable to access values in the headers
     */
    public void buildNewDataFormatLine(String units, int unitscode, 
                                        String dataType, double timestep) throws SmException {
        //calculate the time by multiplying the number of data values by delta t
        String line;
        int numvals = V2Data.getNumVals();
        double calcTime = timestep * numvals;
        String timeSec = Integer.toString((int)calcTime);
        line = String.format("%1$8s %2$12s pts, approx %3$4s secs, units=%4$7s(%5$02d),Format=",
                                     String.valueOf(numvals),dataType,
                                                    timeSec, units, unitscode);
        V2Data.setFormatLine(line + V2Data.getNumberFormat());
    }
    /**
     * This method converts the V2 component stored in memory into its text
     * format for writing to a file.
     * @return a text array with the V2 component in COSMOS format for a file
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
        String[] V2DataText = this.V2Data.numberSectionToText();
        
        //add the array lengths to the text lengths to get the total and declare
        //an array of this length, then build it by combining all the component
        //pieces into a text version of the component.
        totalLength = textLength + intHeaderText.length + realHeaderText.length + 
                                                        V2DataText.length;
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
        System.arraycopy(V2DataText, 0, outText, currentLength, V2DataText.length);
        outText[totalLength-1] = this.endOfData;
        return outText;
    }
    public double extractEONSETfromComments() throws SmException {
        String matchRegex = "(<EONSET>)";
        double etime = 0.0;
        if (this.comments.length < 2) {
            return etime;
        }
        try {
            for (String each : this.comments) {
                Pattern eField = Pattern.compile(matchRegex);
                Matcher m = eField.matcher( each );
                if (m.find()) {
                    String[] vals = each.split(" ");
                    etime = Double.parseDouble(vals[vals.length-1]);
                    break;
                }
            }
        } catch (NumberFormatException err) {
            return 0.0;
        }
        return etime;
    }
}
