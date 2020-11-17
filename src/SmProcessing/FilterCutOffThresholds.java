/*******************************************************************************
 * Name: Java class FilterCutOffThresholds.java
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

import SmConstants.VFileConstants.MagnitudeType;
import static SmConstants.VFileConstants.MagnitudeType.MOMENT;
import static SmConstants.VFileConstants.MagnitudeType.M_LOCAL;
import static SmConstants.VFileConstants.MagnitudeType.M_OTHER;
import static SmConstants.VFileConstants.MagnitudeType.SURFACE;
import SmUtilities.FilterCornerReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
/**
 * This class chooses the Butterworth filter cutoff thresholds to use, based on
 * either the magnitude of the earthquake, the values in the station filter corner
 * table, or the frequency content of the seismic signal.  
 * 
 * When based on earthquake magnitude, it also determines which magnitude from
 * the COSMOS header to use, based on which value is defined in the header.  The
 * order of selection is moment, local, surface, and other.  The header values
 * are determined to be defined if they are not equal to the Real Header NoData
 * value and are non-negative.
 * 
 * When based on station filter corner table values, the SNCL code is used as
 * a key to check for corners read in from the file.  If present, these corners
 * are used instead of EQ magnitude or frequency content.
 * 
 * When based on frequency content, the Fourier amplitude spectra for noise and 
 * signal are calculated and then smoothed.  The intersection points of the smoothed
 * spectra within low-pass and high-pass frequency regions are searched to determine
 * the appropriate corner frequencies to be used for bandpass filtering.
 * 
 * The high-pass region is defined between 0.1 Hz and 1.0 Hz.  If no intersection
 * is found, the default value of 0.1 Hz is used.
 * 
 * The low-pass region is defined between the characteristic frequency of the
 * recording instrument (fc) (often 25 Hz) and Nyquist (half of sampling frequency
 * of the waveform data).  If no intersection point is detected, 80% of Nyquist
 * is used as the low-pass corner.
 * 
 * Based on Matlab code written by Dr. Erol Kalkan, P.E. (kalkan76@gmail.com)
 * URL: www.erolkalkan.com
 * Revision: 1.0.7  Date: 2019/02/11
 * 
 * @author jmjones
 */
public class FilterCutOffThresholds {
    private final double epsilon = 0.001;
    private final double fc = 20.0;  //changed from 25 2020_08_11
    private double f1; //low cutoff
    private double f2; //high cutoff
    private double magnitude;
    private final double lp_upperlim = 1.0;
    private final double lp_lowerlim = 0.1;

    private final double mid = 3.5;
    private final double high = 5.5;
    private final double lowsamp = 50;
/**
 * Constructor just initializes values.  The sample rate is used during the magnitude
 * approach to screen for an appropriate sample rate for the given EQ magnitude.
 * For the frequency approach, it is used for creation of an array of frequency values.
 */
    public FilterCutOffThresholds() {
        this.magnitude = 0.0;
        this.f1 = 0.0;
        this.f2 = 0.0;
    }
    /**
     * Determines the high and low filter cutoff thresholds to use based on
     * the earthquake magnitude.  There are 4 earthquake values that may be
     * defined in the COSMOS header, and the order for selection is moment,
     * local, surface, and other.  If no earthquake parameters are defined, the
     * return value will be set to INVALID.  Earthquake parameters are considered
     * valid if they are greater than zero and not equal to the COSMOS nodata
     * value, given in the noval parameter.  A typical COSMOS nodata value is
     * -999.99.
     * @param mm moment magnitude from the COSMOS header
     * @param lm local magnitude from the COSMOS header
     * @param sm surface magnitude from the COSMOS header
     * @param om other magnitude from the COSMOS header
     * @param noval No Data value for the COSMOS real header
     * @return the magnitude type that would be used to select the high and low 
     * cutoff values, or INVALID if no earthquake values have been defined.
     */
    public MagnitudeType SelectMagnitude( double mm, double lm, double sm,
                                                         double om, double noval) {
        MagnitudeType magtype;
        magnitude = 0.0;
        if ((Math.abs(mm - noval) < epsilon) || (mm < 0.0)){
            if ((Math.abs(lm - noval) < epsilon) || (lm < 0.0)){
                if ((Math.abs(sm - noval) < epsilon) || (sm < 0.0)){
                    if ((Math.abs(om - noval) < epsilon) || (om < 0.0)){
                        magtype = MagnitudeType.INVALID;
                        return magtype;
                    } else {
                        magtype = M_OTHER;
                        magnitude = om;
                    }
                } else {
                    magtype = SURFACE;
                    magnitude = sm;
                }
            } else {
                magtype = M_LOCAL;
                magnitude = lm;
            }
        } else {
            magtype = MOMENT;
            magnitude = mm;
        }
        return magtype;
    }
    /**
     * Checks for entries in the station filters corner table and if a match
     * of the sncl code is found, reads in the low and high corners and sets
     * the low cutoff and high cutoff values to the table entries
     * @param sncl the SNCL code for the current record
     * @return true if the sncl code is in the table and false if not
     */
    public boolean CheckForTableCorners( String sncl ) {
        FilterCornerReader corners = FilterCornerReader.INSTANCE;
        if ((!corners.isEmpty()) && (corners.containsKey(sncl))) {
            double[] cornervalues = corners.getCornerValues(sncl);
            f1 = cornervalues[0];
            f2 = cornervalues[1];
            return true;
        } else {
            return false;
        }
    }
    /**
     * Determines the high and low filter cutoff thresholds to use based on
     * the earthquake magnitude and samplerate.  
     * There is a second check to ensure that the sampling rate is sufficient.
     * 
     * @param intype earthquake source value determined from the cosmos header
     * @param mag magnitude for the given type from the header
     * @param orig_samprate original (not resampled) sample rate of the input waveform
     * @return the same magnitude type, updated to LOWSPS if needed
     */
    public MagnitudeType SelectMagThresholds( MagnitudeType intype, double mag, double orig_samprate) {

        Set<MagnitudeType> check = new HashSet<>(Arrays.asList(MagnitudeType.MOMENT,MagnitudeType.SURFACE,
                                                                    MagnitudeType.M_LOCAL,MagnitudeType.M_OTHER));
        if (!check.contains(intype)) {
            f1 = 0.0;
            f2 = 0.0;
            return intype;
        }
        double samplerate = orig_samprate;
        MagnitudeType magtype = intype;
        if ((magnitude > high) || (Math.abs(magnitude - high) < epsilon)) { 
            f1 = 0.1;
        } else if ((magnitude > mid) || (Math.abs(magnitude - mid) < epsilon)) {
            f1 = 0.3;
        } else {  //magnitude < low
            f1 = 0.5;
        }
        
        if (samplerate >= lowsamp) {
            f2 = Math.min(40.0,((samplerate/2.0) - (samplerate/10.0)));
        } else {
            magtype = MagnitudeType.LOWSPS;
            f1 = 0.0;
            f2 = 0.0;
        }
        return magtype;
    }
    /**
     * Looks at the smoothed frequency content for the acceleration array
     * and uses this to determine the filter cutoff thresholds.  This is accomplished
     * by comparing the frequency signature of the entire acceleration array with
     * the signature of just the noise portion (from the start of the array to 
     * the even onset).  The algorithm looks for the intersection of these 2 signatures
     * between the instrument characteristic frequency and the nyquist frequency 
     * for the high cutoff frequency, and between 0.1 Hz and 1 Hz for the low cutoff frequency.
     * @param array acceleration array for frequency calculation
     * @param eonset event onset index in acceleration array
     * @param current_samprate current sampling rate if record has been resampled
     * @param orig_samprate original sampling rate of the record
     */
    public void findFreqThresholds(double[] array, int eonset, 
                                double current_samprate, double orig_samprate) {
        if ((array == null) || (array.length == 0) || (eonset < 1) || (eonset > array.length-2)) {
            f1 = 0.0;
            f2 = 0.0;
            return;
        }
        double dt = 1.0 / current_samprate;
        double nyquist = 0.5 * orig_samprate;
        FFourierTransform fft = new FFourierTransform();
        
        // get the pre-event subset of the acceleration array, calculate the
        // frequency spectrum and smooth it
        double[] noise = new double[eonset];
        System.arraycopy(array, 0, noise, 0, eonset);
        double[] noiseFAS = fft.calculateFFT(noise);
        fft.normalizeMags( noiseFAS, eonset);
        double[] noiseFASsmooth = smoothFAS(noiseFAS);
        double[] noisefreq = ArrayOps.makeFreqArray(dt, (noiseFAS.length*2));
        
        // do the same with the acceleration array, calculating the frequency
        // spectrum and smoothing it
        double[] signalFAS = fft.calculateFFT(array);
        
        fft.normalizeMags(signalFAS, array.length);
        double[] signalFASsmooth = smoothFAS(signalFAS);
        double[] signalfreq = ArrayOps.makeFreqArray(dt, (signalFAS.length*2));
        
        // Find index in frequency arrays nearest to the characteristic frequency.
        int indexSigFc = ArrayOps.findClosestFreq(signalfreq, fc);
        int indexNoiFc = ArrayOps.findClosestFreq(noisefreq, fc);
        
        // find index in frequency arrays nearest to the nyquist frequency
        int indexSigNy = ArrayOps.findClosestFreq(signalfreq, nyquist);
        int indexNoiNy = ArrayOps.findClosestFreq(noisefreq, nyquist);
        
        // find the intersection of the frequency spectra for signal and noise
        // between the characteristic and nyquist frequencies
        RealMatrix points = findIntersection(Arrays.copyOfRange(signalfreq,indexSigFc,indexSigNy),
                                             Arrays.copyOfRange(signalFASsmooth,indexSigFc,indexSigNy),
                                             Arrays.copyOfRange(noisefreq,indexNoiFc,indexNoiNy),
                                             Arrays.copyOfRange(noiseFASsmooth,indexNoiFc,indexNoiNy));
        
        // pick the frequency closest to (but not less than) the characteristic
        // frequency for the high cutoff estimate.  If no intersection found,
        // use 80% of nyquist
        f2 = 0.8 * nyquist;  //start with 80% of nyquist
        double[] xvalsonly = points.getRow(0);
        Arrays.sort(xvalsonly);
        for (double val : xvalsonly) {
            if (val > fc) {
                f2 = val; //found a better match so use it instead
                break;
            }
        }
        // Find index in frequency arrays nearest to the upper limit for low-pass search.
        int indexSigUp = ArrayOps.findClosestFreq(signalfreq, lp_upperlim);
        int indexNoiUp = ArrayOps.findClosestFreq(noisefreq, lp_upperlim);
        
        // find index in frequency arrays nearest to the lower limit
        int indexSigLo = ArrayOps.findClosestFreq(signalfreq, lp_lowerlim);
        int indexNoiLo = ArrayOps.findClosestFreq(noisefreq, lp_lowerlim);
        
        // find the intersection of the frequency spectra for signal and noise
        // between the lower and upper limit values.
        points = findIntersection(Arrays.copyOfRange(signalfreq,indexSigLo,indexSigUp),
                                  Arrays.copyOfRange(signalFASsmooth,indexSigLo,indexSigUp),
                                  Arrays.copyOfRange(noisefreq,indexNoiLo,indexNoiUp),
                                  Arrays.copyOfRange(noiseFASsmooth,indexNoiLo,indexNoiUp));

        // pick the frequency closest to (but not less than) the lower limit
        // frequency for the low cutoff estimate.  If no intersection found,
        // use the low pass lower limit
        f1 = lp_lowerlim;  //start with lower limit
        xvalsonly = points.getRow(0);
        Arrays.sort(xvalsonly);
        for (double val : xvalsonly) {
            if (val > lp_lowerlim) {
                f1 = val; //found a better match so use it instead
                break;
            }
        }
    }
    /**
     * Uses the Konno-Ohmachi window function to smooth the Fourier amplitude
     * spectra.
     * 
     * Based on Matlab code written by: Dr. Erol Kalkan, P.E. (kalkan76@gmail.com)
     * Revision 1.0.0  Date: 2019/20/06
     * 
     * @param FASamps input Fourier amplitude spectrum to smooth
     * @return the smoothed array
     */
    public double[] smoothFAS(double[] FASamps) {
        if ((FASamps == null) || (FASamps.length == 0)) {
            return new double[0];
        }
        int bcoef = 20;   //b, bandwidth coefficient of konno-ohmachi window
        int window = 39;  //w, width of window function
        int halfw = 20;
        int inlen = FASamps.length;
        double[] result = new double[inlen];
        double[] weight = new double[window];    //W, the weighted window function
        
        // Create the weighted window function (W)
        // W = (sin(b * log10((1:w)/halfw))./(b*log10((1:w)/halfw))).^ 4;
        // Make sure the window function will be 1 at the central value (halfw)
        double logcalc;
        double sumweight = 0.0;
        for (int i = 0; i < window; i++) {
            logcalc = bcoef * Math.log10( (double)(i+1) / (double)halfw );
            if ((Math.abs(logcalc) - 0.0) < 5*Math.ulp(logcalc)) {
                weight[i] = 0.0;
            } else {
                weight[i] = Math.pow( (Math.sin(logcalc) / (logcalc)), 4);
            }
            sumweight = sumweight + weight[i];
        }
        sumweight = sumweight - weight[halfw-1] + 1.0;
        weight[halfw-1] = 1.0;
        
        // Normalize the window function, then flip it
        double[] flipw = new double[window];
        for (int i = 0; i < window; i++) {
            weight[i] = weight[i] / sumweight;
            flipw[window - 1 - i] = weight[i];
        }
        
        // Convolve the input array with the weighted window function.
        // The length of the output is inarray.length + weight.length - 1
        int dlen = window - 1;
        int conlen = inlen + dlen;
        double[] convolve = new double[conlen];
        double[] ypad = new double[2*dlen + inlen];
        
        // Create new arrays for padded FASamps and convolved result
        Arrays.fill(convolve, 0.0);
        Arrays.fill(ypad, 0.0);
        
        // Insert FASamps into the padded array in the center
        System.arraycopy(FASamps, 0, ypad, dlen, inlen);
        
        // Convolve by shifting and multiplying the padded input array and
        // the flipped weighted window
        double wsum;
        for (int i = 0; i < conlen; i++) {
            wsum = 0.0;
            for (int j = 0; j < window; j++) {
                wsum = wsum + flipw[j] * ypad[i + j];
            }
            convolve[i] = wsum;
        }
        
        // Extract the central values of the convolved signal only so that the
        // smoothed array has the same length as the input
        System.arraycopy(convolve, (halfw-1), result, 0, inlen);
        
        return result;
    }
    /**
     * Finds the intersection of 2 curves.  Adapted from curve - intersections - InterX.m
     * written by NS, available at
     * https://www.mathworks.com/matlabcentral/fileexchange/22441-curve-intersections
     * Based on matlab code by: Dr. Erol Kalkan, P.E. (kalkan76@gmail.com)
     * @param x1 x (independent) values for first curve
     * @param y1 y (dependent) values for first curve
     * @param x2 x values for 2nd curve
     * @param y2 y values for 2nd curve
     * @return a matrix containing the x,y intersection pairs, with the x
     * values in row 0 and the corresponding y values in row 1
     */
    public RealMatrix findIntersection(double[] x1, double[] y1, double[] x2, double[] y2) {
        RealMatrix result;
        if ((x1 == null) || (x1.length < 4) || (y1 == null) || (y1.length < 4) || 
                (x2 == null) || (x2.length < 4) || (y2 == null) || (y2.length < 4)) {
            result = MatrixUtils.createRealMatrix(2,2); //initialized with zeroes
            return result;  // send back 'no values found'

        }
        // convert the input arrays to matricies, with x1 and y1 converted to
        // column vectors and x2 and y2 converted to row vectors
        RealMatrix matx1 = MatrixUtils.createRealMatrix(x1.length,1);
        RealMatrix maty1 = MatrixUtils.createRealMatrix(y1.length,1);
        RealMatrix matx2 = MatrixUtils.createRealMatrix(1,x2.length);
        RealMatrix maty2 = MatrixUtils.createRealMatrix(1,y2.length);
        matx1.setColumn(0, x1);
        maty1.setColumn(0, y1);
        matx2.setRow(0, x2);
        maty2.setRow(0,y2);
        
        // differentiate the input arrays arrays and convert to matricies, using the
        // same row and column designations as the input arrays.
        double[] diffx1 = differ( x1 );
        double[] diffy1 = differ( y1 );
        double[] diffx2 = differ( x2 );
        double[] diffy2 = differ( y2 );
        
        RealMatrix matdiffx1 = MatrixUtils.createRealMatrix(diffx1.length,1);
        matdiffx1.setColumn(0,diffx1);
        RealMatrix matdiffy1 = MatrixUtils.createRealMatrix(diffy1.length,1);
        matdiffy1.setColumn(0,diffy1);
        RealMatrix matdiffx2 = MatrixUtils.createRealMatrix(1,diffx2.length);
        matdiffx2.setRow(0,diffx2);
        RealMatrix matdiffy2 = MatrixUtils.createRealMatrix(1,diffy2.length);
        matdiffy2.setRow(0,diffy2);
        
        //determine signed distances between each pair of points and convert to matricies
        double[] sign1 = new double[x1.length-1];
        for (int i=0; i < (sign1.length); i++) {
            sign1[i] = (diffx1[i] * y1[i]) - (diffy1[i] * x1[i]);
        }
        double[] sign2 = new double[x2.length-1];
        for (int i=0; i < (sign2.length); i++) {
            sign2[i] = (diffx2[i] * y2[i]) - (diffy2[i] * x2[i]);
        }
        RealMatrix mats1 = MatrixUtils.createRealMatrix(sign1.length,1);
        RealMatrix mats2 = MatrixUtils.createRealMatrix(1,sign2.length);
        mats1.setColumn(0, sign1);
        mats2.setRow(0, sign2);
        
        // make a matrix with the s1 column repeated across all columns to
        // fill in the matrix for compatibility in operations with larger matricies
        RealMatrix matslrg = MatrixUtils.createRealMatrix(sign1.length,sign2.length);
        for (int i=0; i<sign2.length; i++) {
            matslrg.setColumn(i, sign1);
        }
        //obtain segments where an intersection is expected
        // RealMatrix format is column.multiply(row) to create a row-column matrix
        RealMatrix temp1 = matdiffx1.multiply(maty2);
        RealMatrix temp2 = matdiffy1.multiply(matx2);
        RealMatrix matarg = temp1.subtract(temp2);
        
        temp1 = matarg.getSubMatrix(0,(matarg.getRowDimension()-1), 0, (matarg.getColumnDimension()-2));
        RealMatrix matarg1 = temp1.subtract(matslrg);
        temp2 = matarg.getSubMatrix(0,(matarg.getRowDimension()-1), 1, (matarg.getColumnDimension()-1));
        RealMatrix matarg2 = temp2.subtract(matslrg);
        RealMatrix matc1 = elementMultiply(matarg1,matarg2);
        makeBoolean( matc1 );

        // make a matrix with the s2 row repeated across all rows to
        // fill in the matrix for compatibility in operations with larger matricies
        matslrg = MatrixUtils.createRealMatrix(sign1.length,sign2.length);
        for (int i=0; i<sign1.length; i++) {
            matslrg.setRow(i, sign2);
        }
        RealMatrix matslrgt = matslrg.transpose();
        temp1 = maty1.multiply(matdiffx2);
        temp2 = matx1.multiply(matdiffy2);
        matarg = temp1.subtract(temp2);
        RealMatrix matargt = matarg.transpose();
        
        temp1 = matargt.getSubMatrix(0,(matargt.getRowDimension()-1), 0, (matargt.getColumnDimension()-2));
        matarg1 = temp1.subtract(matslrgt);
        temp2 = matargt.getSubMatrix(0,(matargt.getRowDimension()-1), 1, (matargt.getColumnDimension()-1));
        matarg2 = temp2.subtract(matslrgt);
        RealMatrix matc2 = elementMultiply(matarg1,matarg2);
        makeBoolean( matc2 );
        
        // find the logical AND of matricies C1 and C2; the results contains
        // 1 (true) only where both matricies contain nonzero values
        rowColTuple ijvals = elementANDlocations( matc1, matc2.transpose());
        if (ijvals.isEmpty()) {
            result = MatrixUtils.createRealMatrix(2,2); //initialized to zeroes
            return result;  // send back 'no values found'
        }
        Integer[] rows = ijvals.rowsToArray();
        Integer[] cols = ijvals.colsToArray();
        ijvals.clearTuples();
        double[] arrayL = new double[rows.length];
        double[] sys1 = new double[rows.length];
        double[] sys2 = new double[rows.length];
        int ival, jval;
        double testval;
        ArrayList<Double> checkdup = new ArrayList<>();
        
        // find the common x,y values and eliminate any duplicate x values
        for (int k=0; k<rows.length; k++) {
            ival = rows[k];
            jval = cols[k];
            arrayL[k] = (diffy2[jval] * diffx1[ival]) - (diffy1[ival] * diffx2[jval]);
            if ((Math.abs(arrayL[k]) - 0.0) > 5*Math.ulp(arrayL[k])) { //if arrayL val not equal to zero
                testval = ((diffx2[jval] * sign1[ival]) - (diffx1[ival] * sign2[jval])) / arrayL[k];
                if (!checkdup.contains(testval)) {
                    sys1[k] = testval;
                    sys2[k] = ((diffy2[jval] * sign1[ival]) - (diffy1[ival] * sign2[jval])) / arrayL[k];
                    checkdup.add(testval);
               }
            }
        }
        checkdup.clear();
        result = MatrixUtils.createRealMatrix(2, sys1.length);
        result.setRow(0, sys1);
        result.setRow(1, sys2);        
        return result;
    }
    /**
     * Finds a simple difference between consecutive elements in an array
     * @param array the array to find the difference values
     * @return array containing differences; this array has 1 element less than the input array
     */
    private double[] differ( double[] array ) {
        int newlen = array.length - 1;
        double[] diff = new double[newlen];
        for (int i=0; i<newlen; i++) {
            diff[i] = array[i+1] - array[i];
        }
        return diff;
    }
    /**
     * Replaces the values in the input matrix with a 1 in the element when the matrix
     * is less than or equal to 0, otherwise the return element value is 0
     * @param mat input matrix to test and update
     */
    private void makeBoolean( RealMatrix mat ) {
        double val;
        for (int i=0; i< mat.getRowDimension(); i++) {
            for (int j=0; j< mat.getColumnDimension(); j++) {
                val = (mat.getEntry(i,j) <= 0.0) ? 1.0 : 0.0;
                mat.setEntry(i,j,val);
            }
        }        
    }
    /**
     * Provides an elementwise multiplication of 2 matrices, which are assumed to
     * be of the same shape.
     */
    private RealMatrix elementMultiply( RealMatrix mat1, RealMatrix mat2 ) {
        RealMatrix result = MatrixUtils.createRealMatrix(mat1.getRowDimension(), 
                                                        mat1.getColumnDimension());
        for (int i=0; i<mat1.getRowDimension(); i++) {
            for (int j=0; j<mat1.getColumnDimension(); j++) {
                result.setEntry(i,j,(mat1.getEntry(i,j) * mat2.getEntry(i,j)));
            }
        }
        return result;
    }
    /**
     * Provides an elementwise AND of 2 matricies, returning a list of (i,j) index
     * locations where the AND value is 1.  The AND matrix is not returned.
     * @param mat1
     * @param mat2
     * @return a list of tuples with 2 integer values representing the i,j matrix
     * location where both mat1[i][j] and mat2[i][j] are greater than 0.
     */
    private rowColTuple elementANDlocations( RealMatrix mat1, RealMatrix mat2 ) {
        rowColTuple result = new rowColTuple();
        for (int i=0; i<mat1.getRowDimension(); i++) {
            for (int j=0; j<mat1.getColumnDimension(); j++) {
                if ((mat1.getEntry(i,j) > 0.0) && (mat2.getEntry(i,j) > 0.0)) {
                    result.addPair(i,j);
                }
            }
        }
        return result;        
    }
    /**
     * tuple class to hold i,j values for matrix
     */
    private class rowColTuple {
        private final ArrayList<Integer> rowval = new ArrayList<>();
        private final ArrayList<Integer> colval = new ArrayList<>();
        
        public rowColTuple() {
            this.rowval.clear();
            this.colval.clear();
        }
        public void addPair( int i, int j ) {
            rowval.add(i);
            colval.add(j);
        }
        public Integer[] rowsToArray() {
            Integer[] rows = rowval.toArray(new Integer[rowval.size()]);
            return rows;
        }
        public Integer[] colsToArray() {
            Integer[] cols = colval.toArray(new Integer[colval.size()]);
            return cols;
        }
        public void clearTuples() {
            rowval.clear();
            colval.clear();
        }
        public boolean isEmpty() {
            return rowval.size() <= 0;
        }
    }
    // for debug
    private void showMatrix( RealMatrix mat, String name ) {
        double[][] outarrs = mat.getData();
        System.out.println("\n");
        for (int i=0; i<mat.getRowDimension(); i++) {
            for (int j=0; j<mat.getColumnDimension(); j++) {
                System.out.println(name + ":  " + i + "," + j + "  " + outarrs[i][j]);
            }
        }
    }
    // for debug
    private void showDimensions( RealMatrix mat, String name ) {
        System.out.println(name + ": " + mat.getRowDimension() + " rows, " + 
                                          mat.getColumnDimension() + " columns");
    }
    /**
     * Getter for the filter low cutoff threshold
     * @return the low cutoff threshold
     */
    public double getLowCutOff() {return f1;}
    /**
     * Getter for the filter high cutoff threshold
     * @return the high cutoff threshold
     */
    public double getHighCutOff() {return f2;}
    /**
     * Getter for the earthquake magnitude used for selection
     * @return the earthquake magnitude
     */
    public double getMagnitude() {return magnitude;}
}
