/*******************************************************************************
 * Name: Java class FilterAndIntegrateProcess.java
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

import SmException.SmException;

/**
 * This class performs the filtering of the acceleration array and its integration
 * to velocity and again to displacement.
 * @author jmjones
 */
public class FilterAndIntegrateProcess {
    private final double lowcut;
    private final double highcut;
    private final int numroll;
    private double[] velocity;
    private double[] displace;
    private double[] paddedaccel;
    private final double taperlength;
    private double calculated_taper;
    private double config_taper;
    private final int startIndex;
    private double initialVel;
    private double initialDis;
    private boolean usefftforint;
    /**
     * Constructor simply initializes filter variables
     * @param lowcut filter low cutoff frequency
     * @param highcut filter high cutoff frequency
     * @param numroll the filter roll off, which is 1/2 the filter order
     * @param tapertime the taper length value from the configuration file
     * @param startInd the array index for the start of the event
     * @param fftint flag for frequency-based integration
     */
    public FilterAndIntegrateProcess( double lowcut, double highcut, int numroll,
                                double tapertime, int startInd, boolean fftint) {
        this.lowcut = lowcut;
        this.highcut = highcut;
        this.numroll = numroll;
        this.taperlength = tapertime;
        this.startIndex = startInd;
        this.usefftforint = fftint;
    }
    /**
     * 
     * @param accel this array is modified during processing, with the final array
     * containing the filtered acceleration values
     * @param dtime the sample time interval (seconds/sample) for the record
     * @throws SmException if unable to calculate valid filter parameters
     */
    public void filterAndIntegrate( double[] accel, double dtime) throws SmException {
        ButterworthFilter filter = new ButterworthFilter();
        boolean valid = filter.calculateCoefficients(lowcut, highcut, 
                                                            dtime, numroll, true);
        if (valid) {
            paddedaccel = filter.applyFilter(accel, taperlength, startIndex);
        } else {
            throw new SmException("Invalid bandpass filter calculated parameters");
        }
        calculated_taper = filter.getTaperlength();
        config_taper = filter.getEndTaperlength();
        //The acceleration array was updated with the filtered values in the 
        //applyFilter call
        double[] paddedvelocity;
        double[] paddeddisplace;
        velocity = new double[accel.length];
        displace = new double[accel.length];
        
        // Integrate padded acceleration to velocity and displacement and unpad
        if (usefftforint) {
            FFTinteDiff fftid = new FFTinteDiff();
            paddedvelocity = fftid.integrate( paddedaccel, dtime);
            paddeddisplace = fftid.integrate( paddedvelocity, dtime);
        } else {
            paddedvelocity = ArrayOps.integrate( paddedaccel, dtime,0.0);
            paddeddisplace = ArrayOps.integrate( paddedvelocity, dtime,0.0);
        }
        System.arraycopy(paddedvelocity, filter.getPadLength(), velocity, 0, velocity.length);
        System.arraycopy(paddeddisplace, filter.getPadLength(), displace, 0, displace.length);
        initialVel = velocity[0];
        initialDis = displace[0];
    }
    /**
     * getter for the velocity array after integration from filtered acceleration
     * @return the velocity array
     */
    public double[] getVelocity() { return velocity;}
    /**
     * getter for the displacement array after 2nd integration from filtered acceleration
     * @return the displacement array
     */
    public double[] getDisplacement() { return displace; }
    /**
     * getter for the padded acceleration array after filtering
     * @return the padded acceleration array
     */
    public double[] getPaddedAccel() { return paddedaccel; }
    /**
     * getter for the initial velocity value
     * @return the initial velocity value
     */
    public double getInitialVel() { return initialVel; }
    /**
     * getter for the initial displacement value
     * @return the initial displacement value
     */
    public double getInitialDis() { return initialDis; }
    /**
     * getter for the taper length calculated for the start of the array
     * @return the initial taper length in seconds
     */
    public double getCalculatedTaper() { return calculated_taper; }
    /**
     * getter for the taper length used at the end of the array
     * @return the final taper length in seconds
     */
    public double getConfigTaper() { return config_taper; }
}
