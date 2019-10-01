/*******************************************************************************
 * Name: Java class BuildAPKtable.java
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

import COSMOSformat.V1Component;
import COSMOSformat.V2Component;
import static SmConstants.SmConfigConstants.FULL_ACC_SPECTRA;
import static SmConstants.VFileConstants.COSMOS_EPICENTRALDIST;
import static SmConstants.VFileConstants.COSMOS_LATITUDE;
import static SmConstants.VFileConstants.COSMOS_LONGITUDE;
import static SmConstants.VFileConstants.COSMOS_STATION_TYPE;
import static SmConstants.VFileConstants.FULL_SA_VALUES;
import static SmConstants.VFileConstants.NUM_T_PERIODS;
import static SmConstants.VFileConstants.PEAK_VAL;
import static SmConstants.VFileConstants.TO_G_CONVERSION;
import static SmConstants.VFileConstants.V_UNITS_INDEX;
import SmException.SmException;
import SmProcessing.V3Process;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The BuildAPKtable class pulls information from the Cosmos headers and data
 * arrays of the processed records and builds a CSV table for output.  The data
 * included in the table are event name, scnl code, station type and name,
 * latitude and longitude, epic, PGA for V1 and V2, PGV and PGD, and
 * some or all of the 5% SA values.  A flag in the configuration file determines
 * how many SA values are included in the output table.
 * @author jmjones
 */
public class BuildAPKtable {
    private final String tablename = "apktable.csv";
    private final boolean fulloutput;
    private final String[] headstart = {"EVENT","SCNL","STATION_TYPE",
         "STATION_NAME","LAT","LON","EPIC","FAULT","PGAV1","PGAV2","PGV","PGD"};
    //V3Data is a 2-D array
    //t-periods,fft,0%(Sd,Sv,Sa),2%(Sd,Sv,Sa),5%(Sd,Sv,Sa),10%(Sd,Sv,Sa),20%(Sd,Sv,Sa),
    private final int T_PERIOD = 0; //V3Data array index for t-periods
    private final int SA_5PC = 10; //V3Data array index for Sa 5%
    /**
     * The BuildAPKtable constructor checks the configuration file parameter to
     * see if partial or full SA values are to be written out.
     */
    public BuildAPKtable() {
        ConfigReader config = ConfigReader.INSTANCE;
        String typeval = config.getConfigValue(FULL_ACC_SPECTRA);
        this.fulloutput = (typeval == null) ? false : 
                                        typeval.equalsIgnoreCase(FULL_SA_VALUES);
    }
    /**
     * This constructor is used by the GUI to enter the user preference.
     * @param SAflag true if the full apktable output is requested
     */
    public BuildAPKtable(boolean SAflag) {
        this.fulloutput = SAflag;
    }
    /**
     * Pulls information from all the input data sources to build the contents
     * of the table
     * @param v3rec V3 processing record
     * @param v1Component V1 output file
     * @param v2ComponentAcc V2 acceleration output file
     * @param v2ComponentVel V2 velocity output file
     * @param v2ComponentDis V2 displacement output file
     * @param csvFolder directory name to hold the csv file
     * @param startTime processing start time to build into the file name
     * @throws Exception if unable to create or write to the table
     */
    public void buildTable(V3Process v3rec, V1Component v1Component,
            V2Component v2ComponentAcc, V2Component v2ComponentVel, 
            V2Component v2ComponentDis, File csvFolder, String startTime) throws Exception {
        try {
            ArrayList<String> headerline = new ArrayList<>();
            headerline.addAll(Arrays.asList(headstart));
            
            ArrayList<String> data = new ArrayList<>();
            //event id
            RecordIDValidator rcdvalid = new RecordIDValidator(v1Component.getRcrdId());
            String event = (rcdvalid.isValidRcrdID()) ? rcdvalid.getEventID() : "not found";
            data.add(event);
            //SCNL code
            String scode = v1Component.getSCNLcode();
            data.add(scode);
            //station type
            int stationtype = v1Component.getIntHeaderValue(COSMOS_STATION_TYPE);
            data.add(String.format("%d", stationtype));
            //station name
            String stationname = v1Component.checkForStationName();
            data.add(stationname.replace(",", " "));
            //station latitude
            double lat = v1Component.getRealHeaderValue(COSMOS_LATITUDE);
            data.add(String.format("%10.5f",lat));
            //station longitude
            double lon = v1Component.getRealHeaderValue(COSMOS_LONGITUDE);
            data.add(String.format("%10.5f",lon));
            //epicentral distance
            double epic = v1Component.getRealHeaderValue(COSMOS_EPICENTRALDIST);
            data.add(String.format("%10.5f",epic));
            //fault
            data.add("( -- )");
            //PGAv1
            int units = v1Component.getIntHeaderValue(V_UNITS_INDEX);
            double pgav1 = v1Component.getRealHeaderValue(PEAK_VAL);
            pgav1 = (units == 2) ? pgav1 : (pgav1 * TO_G_CONVERSION);
            data.add(String.format("%15.6f",pgav1));
            //PGSv2
            
            units = v2ComponentAcc.getIntHeaderValue(V_UNITS_INDEX);
            double pgav2 = v2ComponentAcc.getRealHeaderValue(PEAK_VAL);
            pgav2 = (units == 2) ? pgav2 : (pgav2 * TO_G_CONVERSION);
            data.add(String.format("%15.6f",pgav2));
            //PGV
            double pgv = v2ComponentVel.getRealHeaderValue(PEAK_VAL);
            data.add(String.format("%15.6f",pgv));
            //PGD
            double pgd = v2ComponentDis.getRealHeaderValue(PEAK_VAL);
            data.add(String.format("%15.6f",pgd));
            
            if (!fulloutput) {
                //Sa at period 0.3 sec, 1 sec, 3 sec
                headerline.add("SA_0.3");
                data.add(String.format("%15.6f",v3rec.getSa_0p3()));
                headerline.add("SA_1.0");
                data.add(String.format("%15.6f",v3rec.getSa_1p0()));
                headerline.add("SA_3.0");
                data.add(String.format("%15.6f",v3rec.getSa_3p0()));
            } else {
                double[] t_periods = v3rec.getV3Array(T_PERIOD);
                double[] sa_5percent = v3rec.getV3Array(SA_5PC);
                for (int i=0; i < NUM_T_PERIODS; i++) {
                    headerline.add(String.format("SA_%-5.3f", t_periods[i]));
                    data.add(String.format("%15.6f",sa_5percent[i]));
                }
            }

            CSVFileWriter csvwrite = new CSVFileWriter( csvFolder );
            String[] headerout = new String[headerline.size()];
            headerout = headerline.toArray(headerout);
            csvwrite.writeToCSV(data,headerout,tablename, startTime);
            data.clear();
            headerline.clear();
        }
        catch (SmException ex) {
            throw new Exception("Apktable build Error:\n" + ex.getMessage());
        }
    }
}
