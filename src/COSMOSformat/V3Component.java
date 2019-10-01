/*******************************************************************************
 * Name: Java class V3Component.java
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
import static SmConstants.VFileConstants.*;
import SmConstants.VFileConstants.SmArrayStyle;
import static SmConstants.VFileConstants.V3_DAMPING_VALUES;
import SmException.FormatException;
import SmException.SmException;
import SmProcessing.V3Process;
import SmUtilities.ConfigReader;
import static SmConstants.SmConfigConstants.OUT_ARRAY_FORMAT;
import static SmConstants.SmConfigConstants.PROC_AGENCY_ABBREV;
import static SmConstants.SmConfigConstants.PROC_AGENCY_CODE;
import SmUtilities.SmTimeFormatter;
import java.util.ArrayList;

/**
 * This class extends the COSMOScontentFormat base class to define a V3 record.
 * It defines methods to parse the V3 data arrays, build a new V3 component
 * from V3 processing results, and convert the V3 component into text for writing
 * out to file.
 * @author jmjones
 */
public class V3Component extends COSMOScontentFormat {
    private String V3DampingValues; // holds the damping values
    private ArrayList<VRealArray> V3Data; // a list of VRealArray objects for all the different data sections
    private final V1Component parentV1; // link back to the parent V1 record 
    private final V2Component parentV2; // link back to the parent V2 record
    private final V2Component parentV2vel;
    private final V2Component parentV2dis;
    /** 
     * Use this constructor when the V3 component is read in from a file and
     * filled in with the loadComponent method.  In this case, there is no parentV2
     * associated with this V3.
     * @param procType process level indicator (i.e. "V3")
     */
    public V3Component(String procType) {
        super(procType);
        this.parentV1 = null;
        this.parentV2 = null;
        this.parentV2vel = null;
        this.parentV2dis = null;
    }
    /**
     * Use this constructor when the V3 component is created from processing
     * done on a V2 component.  In this case, the contents of V3 are initialized
     * to the V2 values and updated during the processing.
     * @param procType process level indicator (i.e. "V3")
     * @param pV2 reference to the parent V2Component
     * @param pV2vel reference to the parent V2Component holding velocity
     * @param pV2dis reference to the parent V2Component holding displacement
     */
    public V3Component( String procType, V2Component pV2, V2Component pV2vel,
                                                            V2Component pV2dis) {
        super( procType );
        this.parentV1 = pV2.getParent();
        this.parentV2 = pV2;
        this.parentV2vel = pV2vel;
        this.parentV2dis = pV2dis;
        //Load the text header with parent V1 values.  Leave the update to the V2
        //values to the buildV2 method.
        this.noIntVal = pV2.noIntVal;
        this.noRealVal = pV2.noRealVal;
        this.textHeader = pV2.getTextHeader();
        //Load the headers with parent V2 values.
        //Leave updates for buildV3 method
        this.intHeader = new VIntArray(pV2.intHeader);        
        this.realHeader = new VRealArray(pV2.realHeader);
        this.setChannel(pV2.getChannel());
        this.fileName = pV2.getFileName();
        
        //The buildV2 method fills in these data values, the format line, and
        //the individual params for the real arrays.
        this.V3Data = new ArrayList<>();
        
        this.comments = pV2.getComments(); //leave update for processing, if any
        this.endOfData = pV2.endOfData; //leave update for buildV3
    }
    /**
     * This method defines the steps for parsing a V3 data record, which contains
     * multiple floating point data arrays.
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
        V3DampingValues = infile[current++];
        VRealArray Periods = new VRealArray();
        current = Periods.parseValues(current, infile);
        V3Data.add(Periods);
        VRealArray fftVals = new VRealArray();
        current = fftVals.parseValues(current, infile);
        V3Data.add(fftVals);
        VRealArray spectra;
        for (int i = 0; i < NUM_V3_SPECTRA_ARRAYS; i++) {
            spectra = new VRealArray();
            current = spectra.parseValues(current, infile);
            V3Data.add(spectra);
        }
        return current;
    }
    /**
     * Getter for the length of the data array
     * @return the number of values in the data array
     */
    public int getDataLength() {
        return V3Data.size();
    }
    /**
     * Getter for a copy of the data array reference.  Used to access each array
     * of the V3 array sequence
     * @param arrnum the index of the array to retrieve from the V3 array list
     * @return a copy of the array reference
     */
    public double[] getDataArray(int arrnum) {
        VRealArray varray;
        varray = V3Data.get(arrnum);
        return varray.getRealArray();
    }
    /**
     * This method builds the V3 component from the V3 process object, picking
     * up the data arrays and updating header parameters and format lines.  Once
     * in this method, the V3Process object is no longer needed and its array list
     * is transferred to the V3Component object.
     * @param inVvals the V3Process object
     * @throws SmException if unable to access the header values
     * @throws FormatException if unable to format the numeric values to text
     */
    public void buildV3(V3Process inVvals) throws SmException, FormatException {
        StringBuilder sb = new StringBuilder(MAX_LINE_LENGTH);
        StringBuilder eod = new StringBuilder(MAX_LINE_LENGTH);
        double time;
        String unitsname;
        int unitscode;
        String eodname;
        String line;
        String freqformat42 = "%4.2f";
        String freqformat53 = "%5.3f";

        SmTimeFormatter proctime = new SmTimeFormatter();
        ConfigReader config = ConfigReader.INSTANCE;
        
        this.realHeader.setFieldWidth(DEFAULT_REAL_FIELDWIDTH);
        this.realHeader.buildArrayParams( VFileConstants.SmArrayStyle.PACKED );
        this.setRealHeaderFormatLine();
        
        //Get the processing agency info from the config. data
        String agabbrev = config.getConfigValue(PROC_AGENCY_ABBREV);
        agabbrev = (agabbrev == null) ? DEFAULT_AG_CODE : agabbrev;
        
        String agcode = config.getConfigValue(PROC_AGENCY_CODE);
        int agency_code = (agcode == null) ? 0 : Integer.parseInt(agcode);
        unitsname = GUNITST;

        
        //get real header value 62 (it has already been validated in the processing)
        double delta_t = this.realHeader.getRealValue(DELTA_T);
        //Get the array output format of single column per channel or packed
        String arrformat = config.getConfigValue(OUT_ARRAY_FORMAT);
        arrformat = (arrformat == null) ? DEFAULT_ARRAY_STYLE : arrformat;
        SmArrayStyle packtype = (arrformat.equalsIgnoreCase("singleColumn")) ? 
                              SmArrayStyle.SINGLE_COLUMN : SmArrayStyle.PACKED;
        
        //Make the V3 damping values line
        V3DampingValues = sb.append(String.format("%1$4s", String.valueOf(V3_DAMPING_VALUES.length)))
                .append(" damping values for which spectra are computed:")
                .append(String.format("%1$04.2f,", V3_DAMPING_VALUES[0]))
                .append(String.format("%1$04.2f,", V3_DAMPING_VALUES[1]))
                .append(String.format("%1$04.2f,", V3_DAMPING_VALUES[2]))
                .append(String.format("%1$04.2f,", V3_DAMPING_VALUES[3]))
                .append(String.format("%1$04.2f", V3_DAMPING_VALUES[4]))
                .toString();
        
        //transfer the data arrays, starting with the periods
        VRealArray Periods = new VRealArray();
        Periods.setRealArray(inVvals.getV3Array(0));
        Periods.setNumVals(NUM_T_PERIODS);
        Periods.setPrecision(REAL_PRECISION_V3);
        Periods.setFieldWidth(REAL_FIELDWIDTH_V3);
        Periods.setDisplayType("F");
        Periods.buildArrayParams(packtype);
        line = buildNewDataFormatLine(SECT, SECN, "periods","",0);
        Periods.setFormatLine(line + Periods.getNumberFormat());
        V3Data.add(Periods);
        
        VRealArray fftvals = new VRealArray();
        fftvals.setRealArray(inVvals.getV3Array(1));
        fftvals.setNumVals(NUM_T_PERIODS);
        fftvals.setPrecision(REAL_PRECISION_V3);
        fftvals.setFieldWidth(REAL_FIELDWIDTH_V3);
        fftvals.setDisplayType("E");
        fftvals.buildArrayParams(packtype);
        line = buildNewDataFormatLine(CMSECT, CMSECN, "fft","",0);
        fftvals.setFormatLine(line + fftvals.getNumberFormat());
        V3Data.add(fftvals);
        
        VRealArray sarray;
        int arrcount = 2;
        for (int s = 0; s < V3_DAMPING_VALUES.length; s++) {
            sarray = new VRealArray();
            sarray.setRealArray(inVvals.getV3Array(arrcount++));
            sarray.setNumVals(NUM_T_PERIODS);
            sarray.setPrecision(REAL_PRECISION_V3);
            sarray.setFieldWidth(REAL_FIELDWIDTH_V3);
            sarray.setDisplayType("E");
            sarray.buildArrayParams(packtype);
            line = buildNewDataFormatLine(CMT, CMN, "spectra","Sd",
                                                            V3_DAMPING_VALUES[s]);
            sarray.setFormatLine(line + sarray.getNumberFormat());
            V3Data.add(sarray);

            sarray = new VRealArray();
            sarray.setRealArray(inVvals.getV3Array(arrcount++));
            sarray.setNumVals(NUM_T_PERIODS);
            sarray.setPrecision(REAL_PRECISION_V3);
            sarray.setFieldWidth(REAL_FIELDWIDTH_V3);
            sarray.setDisplayType("E");
            sarray.buildArrayParams(packtype);
            line = buildNewDataFormatLine(CMSECT, CMSECN, "spectra","Sv",
                                                            V3_DAMPING_VALUES[s]);
            sarray.setFormatLine(line + sarray.getNumberFormat());
            V3Data.add(sarray);

            sarray = new VRealArray();
            sarray.setRealArray(inVvals.getV3Array(arrcount++));
            sarray.setNumVals(NUM_T_PERIODS);
            sarray.setPrecision(REAL_PRECISION_V3);
            sarray.setFieldWidth(REAL_FIELDWIDTH_V3);
            sarray.setDisplayType("E");
            sarray.buildArrayParams(packtype);
            line = buildNewDataFormatLine(CMSQSECT, CMSQSECN, "spectra","Sa",
                                                            V3_DAMPING_VALUES[s]);
            sarray.setFormatLine(line + sarray.getNumberFormat());
            V3Data.add(sarray);
        }
        
        //Get the current processing time
        String val = proctime.getGMTdateTime();
        //update values in the text header
        this.textHeader[0] = SPECTRA.concat(this.textHeader[0].substring(END_OF_DATATYPE));
        sb = new StringBuilder(MAX_LINE_LENGTH);
        this.textHeader[10] = sb.append("Processed:").append(val).append(",")
                            .append(agabbrev).append(", SaMax=")
                            .append(String.format(freqformat53,inVvals.getPeakVal()/FROM_G_CONVERSION))
                            .append(unitsname).append(" at ")
                            .append(String.format(freqformat42,inVvals.getPeakPeriod()))
                            .append(" sec prd   5%damping").toString();
        sb.setLength(0);
        //Update the Response Spectrum Parameters in the headers
        this.intHeader.setIntValue(PROCESSING_STAGE_INDEX, V3_STAGE);
        this.intHeader.setIntValue(V_UNITS_INDEX, DEFAULT_NOINTVAL);
        this.intHeader.setIntValue(DATA_PHYSICAL_PARAM_CODE, DEFAULT_NOINTVAL);
        this.intHeader.setIntValue(NUM_SPECTRA_PERIODS, NUM_T_PERIODS);
        this.intHeader.setIntValue(NUM_DAMPING_VALUES, V3_DAMPING_VALUES.length);
        this.realHeader.setRealValue(VALUE_SA_0P2, (inVvals.getSa_0p2()/FROM_G_CONVERSION));
        this.realHeader.setRealValue(VALUE_SA_0P3, (inVvals.getSa_0p3()/FROM_G_CONVERSION));
        this.realHeader.setRealValue(VALUE_SA_1P0, (inVvals.getSa_1p0()/FROM_G_CONVERSION));
        this.realHeader.setRealValue(VALUE_SA_3P0, (inVvals.getSa_3p0()/FROM_G_CONVERSION));
        this.realHeader.setRealValue(MAX_SA_SPECTRUM, (inVvals.getPeakVal()/FROM_G_CONVERSION));
        this.realHeader.setRealValue(PERIOD_OF_MAX, inVvals.getPeakPeriod());
        this.realHeader.setRealValue(TIME_OF_MAX, inVvals.getPeakTime());
        this.realHeader.setRealValue(HOUSNER_INTENSITY, inVvals.getHousnerIntensity());        
        
        //Update the end-of-data line with the new data type
        this.endOfData = this.parentV2.endOfData;
        this.updateEndOfDataLine(SPECTRA, this.parentV2.getChannel());
    }
    /**
     * This method creates a new data format line for the V3 component data arrays.
     * @param units the numeric code for the type of units, COSMOS table 2
     * @param unitsCode code containing the type of units (cm, cm/sec, etc.)
     * @param atype array type of "periods", "fft", or "spectra"
     * @param stype spectra type of "Sa", "Sv", or "Sd"
     * @param damp damping value
     * @return the formatted data format line
     */
    public String buildNewDataFormatLine(String units, int unitsCode, String atype,
                                 String stype, double damp) {
        StringBuilder line = new StringBuilder();
        String datType;
        String outline;
        if (atype.equalsIgnoreCase("periods")) {
            datType = " periods at which spectra computed,      units=";
            outline = line.append(String.format("%1$4s", String.valueOf(NUM_T_PERIODS)))
                        .append(datType)
                        .append(String.format("%1$7s",String.valueOf(units)))
                        .append(String.format("(%1$02d),Format=",unitsCode))
                        .toString();
        } else if (atype.equalsIgnoreCase("fft")) {
            datType = " values of approx Fourier spectrum,      units=";
            outline = line.append(String.format("%1$4s", String.valueOf(NUM_T_PERIODS)))
                        .append(datType)
                        .append(String.format("%1$7s",String.valueOf(units)))
                        .append(String.format("(%1$02d),Format=",unitsCode))
                        .toString();
        } else {
            outline= line.append(String.format("%1$4s values of %2$2s",
                                            String.valueOf(NUM_T_PERIODS),stype))
                        .append(String.format(" for Damping =%1$4s",String.valueOf(damp)))
                        .append(",         units=")
                        .append(String.format("%1$7s",String.valueOf(units)))
                        .append(String.format("(%1$02d),Format=",unitsCode))
                        .toString();
        }
        line.setLength(0);
        return outline;
    }
    /**
     * This method converts the V3 component stored in memory into its text
     * format for writing to a file.
     * @return a text array with the V3 component in COSMOS format for a file
     */
    @Override
    public String[] VrecToText() {
        //add up the length of the text portions of the component, which are
        //the text header, the comments, and the end-of-data line.
        int totalLength;
        int currentLength = 0;
        int textLength = this.textHeader.length + this.comments.length + 2;
        
        //get the header and data arrays as text
        String[] intHeaderText = this.intHeader.numberSectionToText();
        String[] realHeaderText = this.realHeader.numberSectionToText();
        
        String[] varr;
        int datasize = 0;
        ArrayList<String[]> V3DataText = new ArrayList<>();
        for (VRealArray each :  V3Data) {
            varr = each.numberSectionToText();
            V3DataText.add(varr);
            datasize += varr.length;
        }
        //add the array lengths to the text lengths to get the total and declare
        //an array of this length, then build it by combining all the component
        //pieces into a text version of the component.
        totalLength = textLength + intHeaderText.length + realHeaderText.length + 
                                                        datasize;
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
        outText[currentLength] = V3DampingValues;
        currentLength++;
        
        for (String[] each : V3DataText) {
            System.arraycopy(each, 0, outText, currentLength, each.length);
            currentLength += each.length;
        }
        V3DataText.clear();
        outText[totalLength-1] = this.endOfData;
        return outText;
    }
}
