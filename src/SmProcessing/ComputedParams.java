/*******************************************************************************
 * Name: Java class ComputedParams.java
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

import static SmConstants.VFileConstants.FROM_G_CONVERSION;
import static SmConstants.VFileConstants.TO_G_CONVERSION;
import static SmProcessing.ArrayOps.convertArrayUnits;
import java.util.Arrays;

/**
 * The computed parameters class calculates the computed parameters for records
 * where the acceleration exceeds a threshold (default is 5% g) at any point.  
 * The values computed are bracketed duration, duration interval, Arias intensity, 
 * Housner intensity, channel RMS, and cumulative absolute velocity.
 * @author jmjones
 */
public class ComputedParams {
    private final double[] acc;
    private final double[] gaccsq;
    private double[] gacc;
    private final double dt;
    private int brackstart;
    private int brackend;
    private final int len;
    private final double threshold;
    
    private double sumGaccsq;
    private double bracketedDuration;
    private double ariasIntensity;
    private double housnerIntensity;
    private double RMSacceleration;
    private double durationInterval;
    private double CAV;
    private double durstart;
    private double durend;
    /**
     * Constructor for the Computed Parameters class - this method initializes
     * variables and calculates arrays for acceleration in g, squared acceleration
     * in g, and the sum/integration of squared acceleration in g.
     * @param inAcc acceleration in cm/sq.sec
     * @param dtime time interval between samples in seconds
     * @param inThreshold percentage of g to qualify as strong motion record
     */
    public ComputedParams(final double[] inAcc, double dtime, double inThreshold) {
        this.dt = dtime;
        this.acc = inAcc;
        this.len = inAcc.length;
        this.gacc = new double[len];
        this.gaccsq = new double[len];
        this.bracketedDuration = 0.0;
        this.ariasIntensity = 0.0;
        this.housnerIntensity = 0.0;
        this.RMSacceleration = 0.0;
        this.durationInterval = 0.0;
        this.CAV = 0.0;
        this.brackstart = 0;
        this.brackend = 0;
        this.threshold = inThreshold / 100.0; //change from % to value
        this.durstart = 0.0;
        this.durend = 0.0;
        
        //Get the acceleration in g for calculations
        gacc = convertArrayUnits(acc, TO_G_CONVERSION);
        
        //square acceleration in g
        for (int i = 0; i < len; i++) {
            gaccsq[i] = Math.pow(gacc[i],2);
        }
        //Get sum/integration of gaccsq, which is Ea
        this.sumGaccsq = 0.5 * (gaccsq[0] + gaccsq[len-1]) * dt;
        for (int i = 1; i < len-1; i++) {
            sumGaccsq = sumGaccsq + gaccsq[i]*dt;
        }
    }
    /**
     * This is an alternate constructor for use when calculating Housner intensity.
     * These computed parameters are calculated during V3 processing and uses the
     * velocity spectrum at 5% damping for its input array.  There is no check
     * here for the computed parameters threshold, and it's assumed that Housner 
     * intensity will only be calculated on records that met the 
     * computed parameters threshold.
     */
    public ComputedParams() {
        this.dt = -1;
        this.acc = new double[0];
        this.len = 0;
        this.gacc = new double[0];
        this.gaccsq = new double[0];
        this.bracketedDuration = 0.0;
        this.ariasIntensity = 0.0;
        this.housnerIntensity = 0.0;
        this.RMSacceleration = 0.0;
        this.durationInterval = 0.0;
        this.CAV = 0.0;
        this.brackstart = 0;
        this.brackend = 0;
        this.threshold = 0;
        this.durstart = 0.0;
        this.durend = 0.0;
    }
    /**
     * This method performs the calculations for all computed parameters except
     * Housner Intensity.  It 
     * returns true if the bracketed duration calculation found at least one
     * value greater than the input threshold (5%g default), which indicates that 
     * the calculations were performed.  If
     * the return is false, all computed parameters are set at 0.
     * @return true if calculations performed, false if no strong motion detected
     */
    public boolean calculateComputedParameters() {
        
        // Bracketed Duration (secs over 5% g)
        boolean strongMotion = calculateBracketedDuration();
        if (!strongMotion) {
            return false;
        }       
        //Duration interval, (sec at 95% Arias I. - sec at 5% Arias I.)
        calculateDurationInterval();
        
        // Arias Intensity, units of m/sec, damping = 0.05
        ariasIntensity = (sumGaccsq * Math.PI / 2.0) * FROM_G_CONVERSION * 0.01;

        // Housner Intensity, units of g*g
        //Housner intensity now has a separate calculation using velocity
        //spectrum at 5% damping, and is meant to be called during V3 processing
//        calculateHousnerIntensity();
        
        // RMS of channel, units of g
        calculateRMSacceleration();
        
        //Cumulative absolute velocity, CAV (m/s)
        calculateCumulativeAbsVelocity();
        
        return true;
    }
    /**
     * Calculates the number of seconds that the acceleration is greater than the threshold.
     * Determines the difference in time between the first moment that 
     * acceleration is greater than threshold and the last moment that acc. is greater than threshold.  
     * If no values
     * in the array are greater than threshold, return is set to false and no computed 
     * parameters are calculated.
     * @return true if at least 1 value is greater than threshold, 
     * false if no values greater than threshold
     */
    private boolean calculateBracketedDuration() {
        //Check if any value is greater than strong motion threshold.  If not, no need to compute
        //parameters
        ArrayStats test = new ArrayStats(gacc);
        if (Math.abs(test.getPeakVal()) < threshold) {
            return false;
        }
        //Find the index of the first value > threshold
        for (int i = 0; i < len; i++) {
            if (Math.abs(gacc[i]) > threshold) {
                brackstart = i;
                break;
            }
        }
        
        //Now find the index where the last value < threshold
        for (int j = len-1; j >= brackstart; j--) {
            if (Math.abs(gacc[j]) > threshold) {
                brackend = j;
                break;
            }
        }
        bracketedDuration = (brackend - brackstart) * dt;
        return true;
    }
    /**
     * Calculates the duration interval between 5-95% of Arias intensity.  Uses
     * the integral of the squared acceleration in g over the whole time period
     * for Arias intensity.  Finds the moment when the Arias intensity is 5% of
     * the total and the moment when the Arias intensity is 95% of the total.
     */
    private void calculateDurationInterval() {
        double IA95 = 0.95 * sumGaccsq;
        double IA05 = 0.05 * sumGaccsq;
        boolean found05 = false;
        boolean found95 = false;
        double t05 = 0.0;
        double t95 = 0.0;
        
        //Walk through the array calculating the running integral at each sample.
        //This is done by starting with 1/2 of the first value and adding in the
        //whole second value as the initial integral.  Now, for each remaining sample
        //in the array, first add 1/2 of the value (integral at that point) and 
        //check if the 5% or 95% value has been reached.  If not, add in another 
        //half of the current value and move to the next value.
        //When the integral reaches 5% of total and again when it reaches 
        //95% of the total set a flag and mark the value.
        double dsum = 0.5 * gaccsq[0] * dt + gaccsq[1] * dt;
        for (int i = 2; i < len; i++) {
            dsum = dsum + 0.5*gaccsq[i]*dt;
            if ((!found05) && (Math.abs(dsum - IA05) < (0.01*IA05))) {
                found05 = true;
                t05 = i * dt;
                durstart = t05;
            } else if ((!found05) && (dsum > IA05)) {
                found05 = true;
                t05 = i * dt;
            }
            if ((!found95) && (Math.abs(dsum - IA95) < (0.01*IA95))) {
                found95 = true;
                t95 = i * dt;
                durend = t95;
            } else if ((!found95) && (dsum > IA95)) {
                found95 = true;
                t95 = i * dt;
            }
            if (found05 && found95) {
                break;
            } else {
                dsum = dsum + 0.5*gaccsq[i]*dt;      
            }
        }
        durationInterval = t95 - t05;
    }
    /**
     * Calculate Housner Intensity, using the 5% damping velocity spectrum created
     * during V3 processing.  The calculation is over the interval from 1 to 2.5
     * seconds.  Since the sv is calculated only at 91 separate periods, the
     * integration is performed on this abbreviated set of values.  The 91 periods
     * includes a value at 1 second but no 2.5 second value, so the 2.4 and 2.6
     * second values are averaged to get a value at 2.5 seconds.
     * @param sv the velocity spectrum at 5% damping
     * @param T the 91 period values as index into the sv array
     * @return the calculated Housner intensity value
     */
    public double calculateHousnerIntensity(final double[] sv, final double[]T) {
        int index_p1_sec = 15;
        int index_2p4_sec = 62;
        int index_2p6_sec = 63;
        int length = index_2p6_sec - index_p1_sec + 1;
        double[] period = new double[length];
        double[] psv = new double[length];
        double[] hi = new double[length];
        double dtt;
//        SmDebugLogger elog = SmDebugLogger.INSTANCE;
        
        //pick up only the values between the 2 time periods for the integration
        for (int i = 0; i < length; i++) {
            psv[i] = sv[i + index_p1_sec];
            period[i] = T[i + index_p1_sec];
        }
        //set the starting value at 0 and average to get a final value of 2.5,
        //updating the ends of the arrays with the final value
        hi[0] = 0.0;
        period[length-1] = 2.5;
        psv[length-1] = (sv[index_2p6_sec] + sv[index_2p4_sec]) / 2.0;
        
        //sum up the area under the velocity spectrum
        for (int i = 1; i < length; i++) {
            dtt = period[i] - period[i-1];
            hi[i] = hi[i-1] + ((psv[i-1] + psv[i])/2.0) * dtt;
        }
//        elog.writeOutArray(hi, "housner.txt");
        housnerIntensity = hi[length-1];
        return housnerIntensity;
    }
    /**
     * Calculate the RMS acceleration.
     * @return the RMS acceleration
     */
    public double calculateRMSacceleration() {
        double interval = durend - durstart;
        double[] gaccint = new double[acc.length];
        double total = 0.0;
        int startd = (int)(durstart/dt);
        int endd = (int)(durend/dt);
        for (int i = startd; i <= endd; i++) {
            gaccint[i] = Math.pow((acc[i]/100.0),2);
            total = total + gaccint[i];
        }
        total = Math.sqrt((1/interval) * total);
        RMSacceleration = total / 10;
        return RMSacceleration;
    }
    /**
     * Calculate the cumulative absolute velocity, using only the 1-second intervals
     * where the abs value of acceleration exceeds 0.025 g at least once in the
     * interval.
     */
    private void calculateCumulativeAbsVelocity() {
        int step = (int)Math.round(1.0 / dt);
        int numsecs = (int)Math.floor(dt*len);
        int length_secs = ((step*numsecs) <= len) ? (step*numsecs) : (step*(numsecs-1));
        int ctr = 0;
        boolean[] intervals = new boolean[numsecs];
        Arrays.fill(intervals, 0, numsecs, false);
        
        //First build an array of boolean flags for each 1-sec interval in the
        //acceleration array.  For each, 1-sec. step, if any value exceeds
        //0.025g, set the flag for that interval to true.
        for (int k = 0; k < length_secs; k = k + step) {
            for (int i = k; i < k+step; i++) {
                if (Math.abs(gacc[i]) > 0.025) {
                    intervals[ctr] = true;
                    break;
                }
            }
            ctr++;
        }
        //Now go through the array again and for each interval where the flag
        //is true, sum/integrate the abs. acceleration to get the velocity total
        //for that interval.  Add the velocity total to the running cumulative
        //total.  Multiplying by 0.01 converts from cm/sq.sec to m/sq.sec.
        ctr = 0;
        double sum = 0.0;
        for (int k = 0; k < length_secs; k = k + step) {
            if (intervals[ctr]) {
                for (int i = k; i < k+step; i++) {
                    sum = sum + Math.abs(acc[i]) * 0.01 * dt;
                }
                CAV = CAV + sum;
                sum = 0;
            }
            ctr++;            
        }
    }
    /**
     * Getter for the bracketed duration
     * @return the bracketed duration
     */
    public double getBracketedDuration() {
        return this.bracketedDuration;
    }
    /**
     * Getter for the Arias intensity
     * @return the Arias intensity
     */
    public double getAriasIntensity() {
        return this.ariasIntensity;
    }
    /**
     * Getter for the Housner intensity
     * @return the Housner intensity
     */
    public double getHousnerIntensity() {
        return this.housnerIntensity;
    }
    /**
     * Getter for the RMS acceleration
     * @return the RMS accel.
     */
    public double getRMSacceleration() {
        return this.RMSacceleration;
    }
    /**
     * Getter for the duration interval
     * @return the duration interval
     */
    public double getDurationInterval() {
        return this.durationInterval;
    }
    /**
     * Getter for the cumulative absolute velocity
     * @return the cumulative absolute velocity
     */
    public double getCumulativeAbsVelocity() {
        return this.CAV;
    }
}