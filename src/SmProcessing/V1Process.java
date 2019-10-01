/*******************************************************************************
 * Name: Java class V1Process.java
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

package SmProcessing;

import COSMOSformat.V0Component;
import static SmConstants.VFileConstants.*;
import SmException.SmException;
import SmUtilities.ConfigReader;
import static SmConstants.SmConfigConstants.*;
import SmUtilities.CommentFormatter;

/**
 * The V1Process class executes the steps necessary to generate a V1 product file
 * from a V0 input file.  It retrieves the recorder LSB and sensor sensitivity
 * values from the Real Header and the data units name and code from the 
 * configuration file.  Based on the units specified, the raw trace counts are
 * converted to physical values.  Then the mean of the data array is removed.
 * The mean value that was removed from the array is saved for updating the
 * header, and the final array peak value, peak value index, and new mean (which
 * should be 0.0) are calculated as well.
 * 
 * @author jmjones
 */
public class V1Process extends V1ProcessFormat {
    private final V0Component inV0;
    private final int data_unit_code;
    private final String data_units;
    private final double lsb;
    private final double sensitivity;
    private final boolean despikeflag;
    private final int despikedevs;
    private final double dtime;
    
    /**
     * The extended constructor for V1Process retrieves the recorder LSB (least significant
     * bit) and the sensor sensitivity from the Real Header and screens for
     * invalid values.  It also retrieves the data units name and code from
     * the configuration file if available.  If these values are not defined,
     * default values are used instead.
     * @param v0rec the reference to the parent V0 record
     * @throws SmException if unable to acquire needed real header or configuration
     * parameters
     */
    public V1Process(final V0Component v0rec) throws SmException {
        super();
        double epsilon = 0.0001;
        double nodata = v0rec.getNoRealVal();
        this.inV0 = v0rec;
        this.commentUpdates = inV0.getComments();
        ConfigReader config = ConfigReader.INSTANCE;
        
        //extract needed values from the V0 record and check if defined
        this.lsb = v0rec.getRealHeaderValue(RECORER_LSB);
        this.sensitivity = v0rec.getRealHeaderValue(SENSOR_SENSITIVITY);
        if  ((Math.abs(lsb - 0.0) < epsilon) || (Math.abs(lsb - nodata) < epsilon)){
            throw new SmException("Real header #" + (RECORER_LSB + 1) + 
                            ", recorder least sig. bit, is invalid: " + lsb);
        }
        if ((Math.abs(sensitivity - 0.0) < epsilon) || (Math.abs(sensitivity - nodata) < epsilon)){
            throw new SmException("Real header #" + (SENSOR_SENSITIVITY + 1) + 
                            ", sensor sensitivity, is invalid: " + sensitivity);
        }
        double noRealVal = v0rec.getNoRealVal();
        double delta_t = v0rec.getRealHeaderValue(DELTA_T);
        if ((Math.abs(delta_t - noRealVal) < epsilon) || (delta_t < 0.0)){
            throw new SmException("Real header #62, delta t, is invalid: " + 
                                                                        delta_t);
        }
        dtime = delta_t * MSEC_TO_SEC;    
        
        //Get config values or use defaults if not defined
        try {
            String unitname = config.getConfigValue(DATA_UNITS_NAME);
            this.data_units = (unitname == null) ? CMSQSECT : unitname;
            
            String unitcode = config.getConfigValue(DATA_UNITS_CODE);
            this.data_unit_code = (unitcode == null) ? CMSQSECN : Integer.parseInt(unitcode);

            String despike = config.getConfigValue(DESPIKE_INPUT);
            this.despikeflag = (despike == null) ? false : despike.equalsIgnoreCase(DESPIKE_INPUT_FLAG);
            
            String numdevs = config.getConfigValue(DESPIKING_STDEV_LIMIT);
            this.despikedevs = (numdevs == null) ? DEFAULT_DESPIKEDEV : Integer.parseInt(numdevs);
        } catch (NumberFormatException err) {
            throw new SmException("Error extracting numeric values from configuration file");
        }
    }
    /**
     * This method performs the actual processing by calling methods from the
     * processing api to convert the counts to physical values and remove the
     * mean.
     */
    @Override
    public void processV1Data(){
        //Get the units from the config file and calculate conversion factor
        double conv;
        if (data_unit_code == CMSQSECN) {
            conv = RawTraceConversion.countToCMS(lsb, sensitivity, FROM_G_CONVERSION);
        } else {
            conv = RawTraceConversion.countToG(lsb, sensitivity);
        }

        //convert counts to physical values
        accel = ArrayOps.countsToPhysicalValues(inV0.getDataArray(), conv);
        
        //Flag automated processing run in comments
        CommentFormatter formatter = new CommentFormatter();
        commentUpdates = formatter.addCorrectionType(commentUpdates, CorrectionType.AUTO);

        //Check for despiking flag and run despiking if requested
        if (despikeflag) {
            Despiking despike = new Despiking( despikedevs );
            boolean spikesfound = despike.removeSpikes(accel, dtime);
            if (spikesfound) {
                commentUpdates = formatter.addSpikeCount(commentUpdates, despike.getSpikeIndex());
            }
        }
        
        //Remove the mean from the array and save for the Real Header
        meanToZero = ArrayOps.findAndRemoveMean(accel);
        
        //Find the new mean (should now be zero) and the location and mag. of peak value
        ArrayStats stat = new ArrayStats( accel );
        avgVal = stat.getMean();
        peakVal = stat.getPeakVal();
        peakIndex = stat.getPeakValIndex();
    }
    /**
     * getter for the data unit code used in processing the V1 array
     * @return the data unit code (COSMOS format, Table 2)
     */
    public int getDataUnitCode() {return this.data_unit_code;}
    /**
     * Getter for the data units used in processing the V1 array
     * @return the data units (COSMOS format, Table 2)
     */
    public String getDataUnits() {return this.data_units;}
}
