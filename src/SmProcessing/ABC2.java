/*******************************************************************************
 * Name: Java class VFileConstants.java
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

import SmConstants.VFileConstants;
import static SmConstants.VFileConstants.*;
import SmConstants.VFileConstants.V2Status;
import SmException.SmException;
import SmUtilities.ABCSortPairs;
import SmUtilities.ConfigReader;
import static SmConstants.SmConfigConstants.*;
import java.util.ArrayList;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

/**
 * <p>This class performs the adaptive baseline correction to search for
 * a baseline function that corrects the velocity trace with the goal of passing
 * the quality checks and maximizing goodness of fit. It accomplishes this by 
 * breaking the input velocity trace into 3 segments.
 * </p><p>
 * The first segment extends from the array start to the 
 * event onset.  This segment is fitted with polynomials of different order
 * (defined in the configuration file) and the fit with the lowest rms is selected.
 * Then the next 2 segments are selected through an iterative walk through the 
 * part of the array from event onset to near the end.  
 * </p><p>
 * The last
 * portion is fitted with a polynomial while an interpolating spline function is
 * used to create the middle segment.  The rms is calculated for these 2 baseline
 * functions, the input acceleration is corrected by the derivative of the 3 baseline segments, and after
 * filtering and integration the QC checks are recorded for each iteration along
 * with a goodness of fit value. 
 * </p><p>
 * The goodness of fit values are ranked, and the lowest ranking iteration that
 * also passes the QC checks is chosen as the final baseline correction.</p>
 * <p>The solutions returned from ABC reflect the order of corrections determined from
 * the velocity array.  The actual corrections made were the derivative of the baseline
 * correction determined from velocity.  To record the order of the actual corrections
 * made to each segment of acceleration, subtract 1 from each ABC order.</p>
 * @author jmjones
 */
public class ABC2 {
    private final int NUM_SEGMENTS = 3;
    private final int RESULT_PARMS = 14;
    
    private final int MOVING_WINDOW = 200;
    private final int difforder;
    private final boolean usefft;
    private final double dtime;
    private final double lowcut;
    private final double highcut;
    private final int numroll;
    private final int estart;
    private final double taplength;
    private double[] velocity;
    private final double[] velstart;
    private final double[] accstart;
    private double[] displace;
    private double[] accel;
    private double []paddedaccel;
    ButterworthFilter filter;
    private final int degreeP1lo;
    private final int degreeP1hi;
    private final int degreeP3lo;
    private final int degreeP3hi;
    private double[] bnn;
    private double[] derivbnn;
    private double[] b1;
    private int bestfirstdegree;
    private int bestthirddegree;
    private ArrayList<double[]> params;
    private double[] rms;
    private int[] ranking;
    private int solution;
    private int counter;
    private double calculated_taper;
    private double config_taper;
    /**
     * The constructor for ABC validates the low and high ranges for the 1st and
     * 3rd polynomial orders that were defined in the configuration file.
     * @param delttime sampling interval, in seconds/sample
     * @param invel velocity array to find the baseline function for
     * @param inacc acceleration array to remove the baseline function derivative from
     * @param lowcut lowcut filter value to use
     * @param highcut high cut filter value
     * @param numroll filter order / 2
     * @param ppick event onset index
     * @param taplengthtime minimum number of seconds for the filter taper length
     * @throws SmException if polynomial orders are invalid
     */
    public ABC2(double delttime, double[] invel, double[] inacc,
                                      double lowcut,double highcut, int numroll,
                                      int ppick, double taplengthtime) throws SmException {
        this.dtime = delttime;
        this.estart = ppick;
        this.taplength = taplengthtime;
        this.velstart = invel;
        this.accstart = inacc;
        this.lowcut = lowcut;
        this.highcut = highcut;
        this.numroll = numroll;
        this.rms = new double[NUM_SEGMENTS];
        this.calculated_taper = 0.0;
        this.config_taper = 0.0;
        this.solution = 0;
        this.counter = 1;
        this.bestfirstdegree = 0;
        this.bestthirddegree = 0;

        ConfigReader config = ConfigReader.INSTANCE;
        String difford = config.getConfigValue(DIFFERENTIATION_ORDER);
        this.difforder = (difford == null) ? DEFAULT_DIFFORDER : Integer.parseInt(difford);
        
        String fftint = config.getConfigValue(INTEGRATION_METHOD);
        this.usefft = (fftint == null) ? true : 
                                        fftint.equalsIgnoreCase(FFT_FOR_INTEGRATION);

        
        //Get the values out of the configuration file and screen for correctness.
        //First polynomial order        
        this.degreeP1lo = validateConfigParam(FIRST_POLY_ORDER_LOWER, 
                                                DEFAULT_1ST_POLY_ORD_LOWER,
                                                DEFAULT_1ST_POLY_ORD_LOWER,
                                                DEFAULT_1ST_POLY_ORD_UPPER);
        this.degreeP1hi = validateConfigParam(FIRST_POLY_ORDER_UPPER, 
                                                DEFAULT_1ST_POLY_ORD_UPPER,
                                                degreeP1lo,
                                                DEFAULT_1ST_POLY_ORD_UPPER);
        //second polynomial order
        this.degreeP3lo = validateConfigParam(THIRD_POLY_ORDER_LOWER, 
                                                DEFAULT_3RD_POLY_ORD_LOWER,
                                                DEFAULT_3RD_POLY_ORD_LOWER,
                                                DEFAULT_3RD_POLY_ORD_UPPER);
        this.degreeP3hi = validateConfigParam(THIRD_POLY_ORDER_UPPER, 
                                                DEFAULT_3RD_POLY_ORD_UPPER,
                                                degreeP3lo,
                                                DEFAULT_3RD_POLY_ORD_UPPER);
        
        // xml is validated by xsd, but this also does a final check for validity
        if ((this.degreeP1lo < 1) || (this.degreeP1hi < 1) || 
                (this.degreeP3lo < 1) || (this.degreeP3hi < 1)) {
            throw new SmException("Unable to parse the adaptive baseline polynomial"
                    + " order values.");
        }
         if ((this.degreeP1lo > this.degreeP1hi) || (this.degreeP3lo > this.degreeP3hi)) {
            throw new SmException("Adaptive baseline polynomial order values "
                    + "are invalid");
        }
    }
    /**
     * Validates the input configuration parameter against the acceptable upper
     * and lower limits and other value.  If input parm is out of range, it is 
     * flagged as an error.
     * @param configparm configuration parameter to validate
     * @param defval the default value for this parameter
     * @param lower the acceptable lower limit
     * @param upper the acceptable upper limit
     * @return a valid value for the configuration parameter, or 0 if unable to 
     * parse, or -1 if out of range
     */
    public final int validateConfigParam( String configparm, int defval, int lower,
                                                                    int upper) {
        int outval = 0;
        ConfigReader config = ConfigReader.INSTANCE;
        String inval = config.getConfigValue(configparm);
        if (inval == null) {
            outval = defval;
        } else {
            try {
                outval = Integer.parseInt(inval);
                outval = ((outval < lower) || (outval > upper)) ? -1 : outval;
            } catch (NumberFormatException e) {
                outval = 0;
            }
        }
        return outval;
    }
    /**
     * Finds the best fit for the input velocity array.  This method controls the
     * flow of adaptive baseline correction.  It runs through each iteration, 
     * recording the results, then ranks the results based on the rms values.
     * The iterations are checked in the ranking order and the first iteration
     * found that passes QC is used to generate the baseline-corrected velocity.
     * If no iteration passes QC, then a flag is set to indicate failure and the
     * iteration with the lowest ranked rms value is returned for inspection.
     * The status of NOABC would be returned if the length of the iteration 
     * segment never gets long enough to exceed the lower bound for the filter limit.
     * @return processing status of GOOD, FAILQC, or NOABC
     * @throws SmException if unable to calculate valid filter parameters
     */
    public VFileConstants.V2Status findFit() throws SmException {
        int vlen = velstart.length;
        int endval = (int)(0.8 * vlen); //iterate through 80% of the array
        int startval = estart + MOVING_WINDOW;
        boolean success = false;
        boolean NO_FFT = false;
        params = new ArrayList<>();
        double[] onerun;
        VFileConstants.V2Status status = V2Status.NOABC;
        QCcheck qcchecker = new QCcheck();
        qcchecker.validateQCvalues();
        qcchecker.findWindow(lowcut, (1.0/dtime), estart);
        filter = new ButterworthFilter();
        boolean valid = filter.calculateCoefficients(lowcut,highcut,dtime,numroll, true);
        if (!valid) {
            throw new SmException("ABC: Invalid bandpass filter input parameters");
        }
        //Fit done for input velocity from time 0 to event onset.  RMS value
        //returned for best fit, array b1 contains the baseline function, and
        //variable bestFirstDegree contains the degree of the fit.
        rms[0] = findFirstPolynomialFit();
        
        //Iterate to find the 2nd break point which results in the lowest rms
        //for the 3 segments.  For each 3rd polynomial order to try, walk through
        //the array section, increasing the window for the 2nd segment each time.
        //At each iteration, filter, integrate, and differentiate.  Store the
        //QC results for each iteration as well as the rms of the corrected vs. 
        //original segments.
        for (int order3 = degreeP3lo; order3 <= degreeP3hi; order3++) {
            for (int t2 = startval; t2 <= endval; t2 += MOVING_WINDOW) {
                if (((t2-estart)*dtime) >= ((int)1.0/lowcut)) {
                    processTheArrays( t2, order3, NO_FFT);
                    qcchecker.qcVelocity(velocity);
                    qcchecker.qcDisplacement(displace);
                    //store the results in an array for comparison
                    onerun = new double[RESULT_PARMS];
                    onerun[0] = Math.sqrt(Math.pow(rms[0], 2) +
                            Math.pow(rms[1],2) + Math.pow(rms[2],2));
                    onerun[1] = Math.abs(qcchecker.getResidualDisplacement());
                    onerun[2] = Math.abs(qcchecker.getInitialVelocity());
                    onerun[3] = Math.abs(qcchecker.getResidualVelocity());
                    onerun[4] = estart;
                    onerun[5] = t2;
                    onerun[6] = bestfirstdegree;
                    onerun[7] = order3;
                    onerun[8] = counter;
                    onerun[9] = rms[0];
                    onerun[10] = rms[1];
                    onerun[11] = rms[2];
                    onerun[12] = 0;
                    onerun[13]= 0;
                    //Penalty for initial acceleration step
                    ArrayStats accstat = new ArrayStats(accel);
                    if (Math.abs(Math.abs(accel[0]) - Math.abs(accstat.getPeakVal())) < 5*Math.ulp(accel[0])) {
                        onerun[0] = 1000;
                    }
                    params.add(onerun);
                    counter++;
                }
            }
        }
        //exit with error status if no estimates performed
        if (params.isEmpty()) {
            status = V2Status.NOABC;
            return status;
        }
        //Sort the results based on cumulative rms
        int count = 0;
        ABCSortPairs sorter = new ABCSortPairs();
        double[] temp;
        for (int i = 0; i < params.size(); i++) {
            temp = params.get(i);
            sorter.addPair(temp[0], count++);
        }
        ranking = sorter.getSortedVals();
        double[] eachrun;
        
        //check each solution against the QA values and find the first that passes
        for (int idx : ranking) {
            eachrun = params.get(idx);
            success = (eachrun[2] <= qcchecker.getInitVelocityQCval()) && 
                          (eachrun[3] <= qcchecker.getResVelocityQCval()) && 
                                (eachrun[1] <= qcchecker.getResDisplaceQCval());
            if (success) {
                processTheArrays((int)eachrun[5],(int)eachrun[7], usefft);
                status = V2Status.GOOD;
                solution = idx;
                break;
            }
        }
        if (status != V2Status.GOOD) { //just pick the lowest rms run to return
            solution = 0;
            eachrun = params.get(solution);
            processTheArrays((int)eachrun[5],(int)eachrun[7], usefft);
            status = V2Status.FAILQC;
        }
        return status;
    }
    /**
     * Finds the best fit for the first segment (from 0 to event onset) by
     * iterating over the different polynomial orders and choosing the order that
     * produces a fit with the lowest rms error compared to the original segment.
     * @return the rms value for the best fit
     */
    private double findFirstPolynomialFit() {
        double bestrms = Double.MAX_VALUE;
        int bestdegree = 0;
        int len = estart+1;
        double[] bestcoefs = new double[0];
        double[] coefs;
        double[] h1 = new double[len];
        b1 = new double[len];
        double rms1;
        PolynomialFunction poly;
        
        double[] time = ArrayOps.makeTimeArray( dtime, h1.length);
        System.arraycopy(velstart,0,h1,0,h1.length);
        for (int order1 = degreeP1lo; order1 <= degreeP1hi; order1++) {
            //find best fit for 1st polynomial, since its length doesn't change
            coefs = ArrayOps.findPolynomialTrend(h1, order1, dtime);
            poly = new PolynomialFunction( coefs );
            for (int i = 0; i < len; i++) {
                b1[i] = poly.value(time[i]);
            }
            rms1 = ArrayOps.rootMeanSquare(h1, b1);
            if (rms1 < bestrms) {
                bestrms = rms1;
                bestdegree = order1;
                bestcoefs = coefs;
            }
        }
        poly = new PolynomialFunction( bestcoefs );
        for (int i = 0; i < len; i++) {
            b1[i] = poly.value(time[i]);
        }
        bestfirstdegree = bestdegree;
        return bestrms;
    }
    /**
     * Performs the steps of making the correction for segments 2 and 3, then filtering
     * and integrating to obtain the corrected acceleration and velocity
     * @param secondb the second break point
     * @param order the polynomial order for the 3rd segment
     * @throws SmException if unable to calculate valid filter parameters
     */
    private void processTheArrays( int secondb, int order, boolean fftuse) throws SmException {

        //fit a baseline function to segments 2 and 3 and make correction
        //updated results in accel and velocity
        makeCorrection(velstart, accstart, secondb, order, fftuse);
        
        //filter acceleration and integrate to velocity and displacement
        FilterAndIntegrateProcess filterInt = 
                new FilterAndIntegrateProcess(lowcut,highcut,numroll,
                                                            taplength,estart,fftuse);
        filterInt.filterAndIntegrate(accel, dtime);
        paddedaccel = filterInt.getPaddedAccel();
        velocity = filterInt.getVelocity();
        displace = filterInt.getDisplacement();
        calculated_taper = filterInt.getCalculatedTaper();
        config_taper = filterInt.getConfigTaper();
    }
    /**
     * Makes the baseline correction on the input array. It first calculates the
     * baseline function for the 3rd segment based on the input order and beginning
     * at the input break index.  Then it calls the spline method to build the
     * interpolating spline between the baseline functions of the 1st and 3rd 
     * segments. It subtracts the baseline function from the input array and
     * calculates the rms of the 2nd and 3rd segments and adds these to the stored
     * rms values.
     * @param array the input array to correct
     * @param break2 the index to split the array at for the 3rd segment
     * @param order3 the order of the 3rd segment polynomial for correction
     * @return the baseline-corrected input array
     */
    private void makeCorrection( double[] velin, double[] accin, int break2, int order3, boolean fftuse) {
        double[] h2;
        double[] h3;
        int break1 = estart;
        int splinelength = break2-(break1+1);
        accel = new double[accin.length];
        double[] time = ArrayOps.makeTimeArray(dtime, velin.length);
        
        h2 = new double[splinelength];
        double[] b2 = new double[splinelength];
        h3 = new double[velin.length-break2];
        System.arraycopy(velin, break1+1, h2, 0, splinelength);
        System.arraycopy(velin, break2, h3, 0, velin.length-break2);
        FFTinteDiff fftid = new FFTinteDiff();
        
        //Get the best fit baseline function for the 3rd segment
        double[] b3 = find3rdPolyFit(h3, order3);
        
        //Construct the baseline function from the first and 3rd sections
        bnn = new double[time.length];
        for (int i = 0; i < bnn.length; i++) {
            if ( i <= break1) {
                bnn[i] = b1[i];
            } else if ( i >= break2) {
                bnn[i] = b3[i - break2];
            } else {
                bnn[i] = 0.0;
            }
        }
        //Connect the 1st and 3rd segments with the interpolating spline
        getSplineSmooth( bnn, break1, break2, dtime );
        System.arraycopy(bnn,break1+1,b2,0,splinelength);
        
        //differentiate the baseline function and remove the derivative from
        //acceleration
        derivbnn = ArrayOps.differentiate(bnn, dtime, difforder);
        for (int i = 0; i < accin.length; i++) {
            accel[i] = accin[i] - derivbnn[i];
        }
        
        //integrate acceleration to velocity and correct for initial estimate of 0
        if (fftuse) {
            velocity = fftid.integrate(accel, dtime);
        } else {
            velocity = ArrayOps.integrate(accel, dtime, 0.0);
        }
        ArrayOps.correctForZeroInitialEstimate( velocity, estart );
        
        //Compute the rms of original and corrected segments
        rms[1] = ArrayOps.rootMeanSquare(h2,b2);
        rms[2] = ArrayOps.rootMeanSquare(h3,b3);
    }
    /**
     * Finds the 3rd polynomial baseline fit based on the polynomial degree.
     * @param array the input array to fit
     * @param degree the degree of the polynomial to use to fit
     * @return the baseline correction function
     */
    private double[] find3rdPolyFit(double[] array, int degree) {
        double[] result = new double[array.length];
        double[] time = ArrayOps.makeTimeArray(dtime, array.length);
        double[] coefs = ArrayOps.findPolynomialTrend(array, degree, dtime);
        PolynomialFunction poly = new PolynomialFunction( coefs );
        for (int i = 0; i < array.length; i++) {
            result[i] = poly.value(time[i]);
        }
        return result;
    }
    /**
     * Connects the 1st and 3rd segments of the baseline correction fit with
     * "the lowest order (smoothest) polynomial baseline connection that
     * continuously connects the initial and final portions" (p.1) from:
     * <p>
     * Wang, Luo-Jia,
     * EERL 96-04, Earthquake Engineering Research Laboratory, 
     * California Institute of Technology Pasadena, 1996-09, 25 pages,
     * </p>
     * @param vals the input array of values, where the array between break1
     * and break2 will be filled with the calculated spline values
     * @param break1 location of last value of 1st baseline segment
     * @param break2 location of first value of 3rd baseline segment
     * @param intime the time interval between samples
     */
    public void getSplineSmooth( double[] vals, int break1, int break2, double intime ) {
        double start;
        double end;
        double ssq;
        double esq;
        
        int len = vals.length;
        double[] loctime = ArrayOps.makeTimeArray( intime, len);
        double t1 = break1 * intime;
        double t2 = break2 * intime;
        double time12 = intime * 12.0;   //dt12
        double intlen = t2 - t1;        //t21
        
        double a = vals[break1];
        double b = vals[break2];
        double c = (   3.0 * vals[break1-4] 
                    - 16.0 * vals[break1-3] 
                    + 36.0 * vals[break1-2] 
                    - 48.0 * vals[break1-1] 
                    + 25.0 * vals[break1]   )/ time12;
        
        double d = ( -25.0 * vals[break2] 
                    + 48.0 * vals[break2+1] 
                    - 36.0 * vals[break2+2] 
                    + 16.0 * vals[break2+3] 
                    -  3.0 * vals[break2+4]   )/ time12;

        for (int i = break1+1; i < break2; i++) {
            start = loctime[i] - t1;
            end = loctime[i] - t2;
            ssq = Math.pow(start, 2);
            esq = Math.pow(end, 2);
            vals[i] = (1.0 + ((2.0 * start)/intlen)) * esq * a + 
                      (1.0 - ((2.0 * end)/intlen)) * ssq * b +
                      start * esq * c +
                      end * ssq * d;
            vals[i] = vals[i] / Math.pow(intlen, 2);
        }
    }
    /**
     * Getter for the baseline function
     * @return the baseline function
     */
    public double[] getBaselineFunction() {
        return bnn;
    }
    /**
     * Getter for the baseline derivative function
     * @return the baseline derivative function
     */
    public double[] getBaselineDerivativeFunction() {
        return derivbnn;
    }
    /**
     * Getter for the array of ranks
     * @return ranked index array of iteration numbers
     */
    public int[] getRanking() {
        return ranking;
    }
    /**
     * Getter for the index of the iteration identified as the solution
     * @return index to the parameter array holing the solution parameters
     */
    public int getSolution() {
        return solution;
    }
    /**
     * Getter for the entire array list of parameters for every iteration
     * @return array list of parameters
     */
    public ArrayList<double[]> getParameters() {
        return params;
    }
    /**
     * Getter for the individual solution array based on the input index
     * @param sol the index for the solution set of parameters
     * @return the array of solution parameters
     */
    public double[] getSolutionParms(int sol) {
        return params.get(sol);
    }
    /**
     * Getter for the number of iterations performed
     * @return the total number of iterations
     */
    public int getNumRuns() {
        return params.size();
    }
    /**
     * Getter for the length of the moving window, used as a step added to the
     * length of the 2nd segment for each iteration
     * @return the length of the moving window
     */
    public int getMovingWindow() {
        return MOVING_WINDOW;
    }
    /**
     * Getter for the number of iterations, should be same as getNumRuns
     * @return counter of the number of iterations
     */
    public int getCounter() {
        return counter;
    }
    /**
     * Getter for the array of RMS values for the winning or return solution
     * @return array of RMS values, one for each segment
     */
    public double[] getRMSvalues() {
        return rms;
    }
    /**
     * Getter for the corrected velocity array
     * @return the final velocity
     */
    public double[] getABCvelocity() {
        return velocity;
    }
    /**
     * Getter for the final displacement array
     * @return the final displacement
     */
    public double[] getABCdisplacement() {
        return displace;
    }
    /**
     * Getter for the corrected acceleration array
     * @return the final acceleration
     */
    public double[] getABCacceleration() {
        return accel;
    }
    /**
     * Getter for the corrected padded acceleration array
     * @return the padded acceleration
     */
    public double[] getABCpaddedacceleration() {
        return paddedaccel;
    }
    /**
     * Getter for the calculated taper length used during filtering
     * @return the calculated taper length
     */
    public double getCalculatedTaperLength() {
        return this.calculated_taper;
    }
    /**
     * Getter for the config taper length used during filtering
     * @return the config taper length
     */
    public double getConfigTaperLength() {
        return this.config_taper;
    }
    /**
     * Clears the params array to release dynamic memory storage
     */
    public void clearParamsArray() {
        params.clear();
    }
}
