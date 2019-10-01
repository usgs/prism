/*******************************************************************************
 * Name: Java class TrendRemovalProcess.java
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
import java.util.Arrays;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * This class removes any pre-event mean from the acceleration array, then integrates to
 * velocity, looks for a trend in velocity and removes the derivative of the trend
 * from the acceleration array.  This corrected acceleration array is then integrated
 * to also return a corrected velocity array.
 * @author jmjones
 */
public class TrendRemovalProcess {
    private final int startIndex;
    private double[] velocity;
    private double preEventMean;
    private int trendRemovalOrder;
    private final boolean usefftforint;
    /**
     * Constructor simply initializes variables.
     * @param startInd the event start index
     * @param fftint flag for freq integration
     */
    public TrendRemovalProcess(int startInd, boolean fftint) {
        this.startIndex = startInd;
        this.usefftforint = fftint;
    }
    /**
     * Performs the trend removal steps of pre-event mean removal from acceleration,
     * integration to velocity, trend detection in velocity, differentiation of trend
     * and removal of trend derivative from acceleration, then integration of 
     * acceleration to corrected velocity
     * @param accel acceleration array, this array is modified during processing
     * and will contain the corrected acceleration after the call.
     * @param dtime the sampling rate in seconds per sample
     * @return the corrected velocity array
     * @throws SmException if unable to remove differentiated trend from acceleration
     */
    public double[] removeTrends(double[] accel, double dtime) throws SmException {
        FFTinteDiff fftid = new FFTinteDiff();
        if (startIndex > 0) {
            double[] subset = Arrays.copyOfRange( accel, 0, startIndex );
            ArrayStats accsub = new ArrayStats( subset );
            preEventMean = accsub.getMean();
            ArrayOps.removeValue(accel, preEventMean);
        }
        //Integrate the acceleration to get velocity, using 0 as first value estimate
        if (usefftforint) {
            velocity = fftid.integrate(accel, dtime);
        } else {
            velocity = ArrayOps.integrate(accel, dtime, 0.0);
        }
        //Now correct for unknown initial value by removing preevent mean (minus first val.)
        ArrayOps.correctForZeroInitialEstimate( velocity, startIndex );

        //Find any linear or 2nd order polynomial trend in velocity
        //Get derivative of trend and remove this from acc
        double[] tcoefs = ArrayOps.findTrendWithBestFit( velocity, dtime);
        PolynomialFunction trendpoly = new PolynomialFunction( tcoefs );
        trendRemovalOrder = trendpoly.degree() - 1; //order of acc's trend
        PolynomialFunction diffPoly = trendpoly.polynomialDerivative();
        boolean trendSuccess = ArrayOps.removePolynomialTrend(accel, 
                                                diffPoly.getCoefficients(), dtime);
        if (!trendSuccess) {
            throw new SmException("Unable to remove best fit differentiated trend from acceleration.");
        } else {
            if (usefftforint) {
                velocity = fftid.integrate(accel, dtime);
            } else {
                velocity = ArrayOps.integrate(accel, dtime, 0.0);
            }
        }
        return velocity;
    }
    /**
     * getter for the pre-event mean
     * @return the pre-event mean that was removed from acceleration
     */
    public double getPreEventMean() { return preEventMean; }
    /**
     * getter for the trend removal order
     * @return the order of the polynomial trend removed from acceleration
     */
    public int getTrendRemovalOrder() { return trendRemovalOrder; }
}
