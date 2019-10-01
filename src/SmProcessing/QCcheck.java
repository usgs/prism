/*******************************************************************************
 * Name: Java class QCcheck.java
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

import static SmConstants.VFileConstants.DEFAULT_QA_INITIAL_VELOCITY;
import static SmConstants.VFileConstants.DEFAULT_QA_RESIDUAL_DISPLACE;
import static SmConstants.VFileConstants.DEFAULT_QA_RESIDUAL_VELOCITY;
import SmUtilities.ConfigReader;
import static SmConstants.SmConfigConstants.QC_INITIAL_VELOCITY;
import static SmConstants.SmConfigConstants.QC_RESIDUAL_DISPLACE;
import static SmConstants.SmConfigConstants.QC_RESIDUAL_VELOCITY;

/**
 * This class handles the QC checks for velocity and displacement, using the
 * QC limits defined in the configuration file.  If no configuration file is
 * available, then default values are substituted for the limits.
 * @author jmjones
 */
public class QCcheck {
    private double lowcut;
    private int eindex;
    private double qcvelinit;
    private double qcvelres;
    private double qcdisres;
    
    private int window;
    private double velstart;
    private double velend;
    private double disend;
    /**
     * Default constructor
     */
    public QCcheck() {
        this.qcvelinit = -1.0;
        this.qcvelres = -1.0;
        this.qcdisres = -1.0;
    }
    /**
     * Retrieves the QC limits from the configuration file.  If no configuration
     * file is found (or no entries for QC parameters found), then default values
     * are substituted.  If values are retrieved from the configuration file, but
     * they can't be converted to doubles, then this method returns 'false'.
     * @return 'true' if QC limits available, 'false' if unable to correctly 
     * extract them from the configuration file.
     */
    public boolean validateQCvalues() {
        ConfigReader config = ConfigReader.INSTANCE;
        try {
            String qainitvel = config.getConfigValue(QC_INITIAL_VELOCITY);
            this.qcvelinit = (qainitvel == null) ? DEFAULT_QA_INITIAL_VELOCITY : 
                                                    Double.parseDouble(qainitvel);

            String qaendvel = config.getConfigValue(QC_RESIDUAL_VELOCITY);
            this.qcvelres = (qaendvel == null) ? DEFAULT_QA_RESIDUAL_VELOCITY : 
                                                    Double.parseDouble(qaendvel);

            String qaenddis = config.getConfigValue(QC_RESIDUAL_DISPLACE);
            this.qcdisres = (qaenddis == null) ? DEFAULT_QA_RESIDUAL_DISPLACE : 
                                                    Double.parseDouble(qaenddis);
        } catch (NumberFormatException err) {
            return false;
        }
        return true;
    }
    /**
     * Finds the appropriate window length in the array to use for the QC check.
     * The length of the window is the maximum of either the event onset time or
     * (1.0 / lowcutoff frequency) * sample rate).
     * @param lowcutoff the filter lowcutoff frequency
     * @param samprate the sampling rate
     * @param eventIndex the event onset index into the array
     * @return the index for the window length in the array
     */
    public int findWindow(double lowcutoff, double samprate, int eventIndex) {
        this.lowcut = lowcutoff;
        this.eindex = eventIndex;
        
        int lclength = (int)(Math.round(1.0 / lowcut) * samprate);
        window = Math.max(eindex, lclength);
        return window;
    }
    /**
     * Performs the QC check for the velocity array.  The calculated window length
     * is used as a starting point to find the nearest zero crossing in the array.
     * The mean of the array over the adjusted window is used as the value to compare
     * against the QC limit parameter.  Both the initial and residual sections of
     * the velocity arrays are checked.  If no zero crossing occurs over the entire
     * window length, then just the initial or final array value is substituted
     * for comparison against the limit.
     * @param velocity the velocity array for the QC check
     * @return TRUE if velocity passed the QC test, FALSE if not
     */
    public boolean qcVelocity(double[] velocity) {
        boolean pass = false;
        int vellen = velocity.length;
        int velwindowstart;
        int velwindowend;
        if (window > 0) {
            velwindowstart = ArrayOps.findZeroCrossing(velocity, window, 0);
            velstart = (velwindowstart > 0) ? 
                    ArrayOps.findSubsetMean(velocity, 0, velwindowstart+1) : 
                                                                    velocity[0];
            velwindowend = ArrayOps.findZeroCrossing(velocity, vellen-window-1, vellen-1);
            velend = (velwindowend > 0) ? 
                    ArrayOps.findSubsetMean(velocity, velwindowend, vellen) : 
                                                             velocity[vellen-1];
        } else {
            velstart = velocity[0];
            velend = velocity[vellen-1];
        }
        if ((Math.abs(velstart) <= qcvelinit) && (Math.abs(velend) <= qcvelres)){
            pass = true;
        }
        return pass;
    }
    /**
     * Performs the QC check for the displacement array.  The calculated window length
     * is used as a starting point to find the nearest zero crossing in the array.
     * The mean of the array over the adjusted window is used as the value to compare
     * against the QC limit parameter.  Only the residual section of the
     * displacement array is checked.  If no zero crossing occurs over the entire
     * window length, then just the final array value is substituted
     * for comparison against the limit.
     * @param displace the displacement array for the QC check
     * @return TRUE if displacement passed the QC test, FALSE if not
     */
    public boolean qcDisplacement(double[] displace) {
        boolean pass = false;
        int dislen = displace.length;
        int diswindowend;
        if (window > 0) {
            diswindowend = ArrayOps.findZeroCrossing(displace, dislen-window-1, dislen-1);
            disend = (diswindowend > 0) ? 
                    ArrayOps.findSubsetMean(displace, diswindowend, dislen) : 
                                                            displace[dislen-1];
        } else {
           disend = displace[dislen-1];
        }
        if ((Math.abs(disend) <= qcdisres)) {
            pass = true;
        }
        return pass;        
    }
    /**
     * Getter for the calculated initial velocity value used in the QC check
     * @return the initial velocity value
     */
    public double getInitialVelocity() {
        return velstart;
    }
    /**
     * Getter for the calculated residual velocity value used in the QC check
     * @return the residual velocity value
     */
    public double getResidualVelocity() {
        return velend;
    }
    /**
     * Getter for the calculated residual displacement value used in the QC check
     * @return the residual displacement value
     */
    public double getResidualDisplacement() {
        return disend;
    }
    /**
     * Getter for the initial velocity QC parameter
     * @return the initial velocity QC parameter
     */
    public double getInitVelocityQCval() {
        return qcvelinit;
    }
    /**
     * Getter for the residual velocity QC parameter
     * @return the residual velocity QC parameter
     */
    public double getResVelocityQCval() {
        return qcvelres;
    }
    /**
     * Getter for the residual displacement QC parameter
     * @return the residual displacement QC parameter
     */
    public double getResDisplaceQCval() {
        return qcdisres;
    }
    /**
     * Getter for the QC window length (number of samples)
     * @return the QC window length in samples
     */
    public int getQCWindow() {
        return window;
    }
}
