/*******************************************************************************
 * Name: Java class V3Process.java
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

import COSMOSformat.V2Component;
import static SmConstants.VFileConstants.CMSQSECT;
import static SmConstants.VFileConstants.DELTA_T;
import static SmConstants.VFileConstants.MSEC_TO_SEC;
import static SmConstants.VFileConstants.NUM_T_PERIODS;
import static SmConstants.VFileConstants.V3_DAMPING_VALUES;
import SmException.FormatException;
import SmException.SmException;
import SmUtilities.SmDebugLogger;
import java.io.IOException;
import java.util.ArrayList;
/**
 * The V3Process class executes the steps necessary to generate a V3 product file
 * from a V2 component object. It calculates the FFT of the corrected acceleration
 * and extracts the values at the periods of interest, and calculates the spectra
 * at each damping value.
 * @author jmjones
 */
public class V3Process {
    private final double EPSILON = 0.0001;
    private ArrayList<double[]> V3Data;
    private double[][][] spectra;
    private double[] T_periods;
    private double dtime;
    private double samplerate;
    private final double noRealVal;
    private double[] paccel;
    private double peakVal;
    private double peakIndex;
    private double peakTime;
    private SpectraResources spec;
    private SmDebugLogger elog;
    private boolean writeArrays;
    private double Sa_0p2;
    private double Sa_0p3;
    private double Sa_1p0;
    private double Sa_3p0;
    private boolean strongMotion;
    private double housnerIntensity;
    /**
     * The constructor reads in the coefficient files and the period file and
     * stores them for use during the calculations.
     * @param v2acc the V2 component with corrected acceleration
     * @param v2val the V2 process object holding the padded acceleration array
     * @throws IOException if unable to read in the coefficient files
     * @throws SmException if the sampling interval in the real header is invalid
     * @throws FormatException if unable to parse the values in the coefficient files
     */
    public V3Process(final V2Component v2acc, V2Process v2val) throws IOException, SmException, 
                                                                FormatException {

        this.elog = SmDebugLogger.INSTANCE;
        writeArrays = false;
        this.paccel = v2val.getPaddedAccel();
        this.strongMotion = v2val.getStrongMotion();
        this.housnerIntensity = 0.0;
        this.peakVal = 0.0;
        this.peakIndex = 0;
        this.Sa_0p2 = 0.0;
        this.Sa_0p3 = 0.0;
        this.Sa_1p0 = 0.0;
        this.Sa_3p0 = 0.0;
        this.V3Data = new ArrayList<>();
        this.noRealVal = v2acc.getNoRealVal();
        double delta_t = v2acc.getRealHeaderValue(DELTA_T);
        if ((Math.abs(delta_t - noRealVal) < EPSILON) || (delta_t < 0.0)){
            throw new SmException("Real header #62, delta t, is invalid: " + 
                                                                       delta_t);
        }
        //Get the periods to compute spectra and coeficients for the input
        //sampling interval.
        dtime = delta_t * MSEC_TO_SEC;  
        samplerate = 1.0 / dtime;
        spectra = new double[V3_DAMPING_VALUES.length][][];
        spec = new SpectraResources();
        T_periods = spec.getTperiods();
        for (int i = 0; i < V3_DAMPING_VALUES.length; i++) {
            spectra[i] = spec.getCoefArray(samplerate, V3_DAMPING_VALUES[i]);
        }
        //Add the T-periods to the V3 data list
        V3Data.add(T_periods);
    }
    /**
     * Performs the V3 data processing of calculating the fft, extracting the
     * values at the 91 periods, and calculating the spectra for each damping value
     * and data type.
     */
    public void processV3Data() {        
        //Calculate FFT for the acceleration array.  
        int ulim;
        int llim;
        double uval;
        double lval;
        double scale;
        
        //Calculate the FFT of the padded acceleration
        FFourierTransform fft = new FFourierTransform();
        double[] accspec = fft.calculateFFT(paccel);
        double delta_f = 1.0 / (fft.getPowerLength() * dtime);
        
        //Normalize the values
        double[] accnorm = new double[accspec.length];
        for (int i = 0; i < accnorm.length; i++) {
            accnorm[i] = accspec[i] * dtime;
        }
        if (writeArrays) {
            elog.writeOutArray(accnorm, "V3accelFFTnorm.txt");
        } 
        //Now use 3-point smoothing to smooth out the frequency response
        double[] accsmooth = ArrayOps.perform3PtSmoothing(accnorm);
        
        //Adjust endpoints of smoothing for frequency wrap-around
        int acclen = accsmooth.length;
        accsmooth[0] = 0.5 * accsmooth[0] + 0.5 * accsmooth[1];
        accsmooth[acclen-1] = 0.5 * accsmooth[acclen-1] + 0.5 * accsmooth[acclen-2];
        
        if (writeArrays) {
            elog.writeOutArray(accsmooth, "V3accsmoothFFT.txt");
        } 
        //Select magnitudes for the given T values only.  (freq = 1/T)
        double[] accfftvals = new double[NUM_T_PERIODS];
        int ctr = 0;
        for (int f = NUM_T_PERIODS-1; f >=0; f--) {
            for (int arr = ctr; arr < acclen; arr++) {
                if ((arr*delta_f > (1.0/T_periods[f])) || 
                        (Math.abs(arr*delta_f - (1.0/T_periods[f])) < EPSILON)) {
                    ctr = arr;
                    break;
                }
            }
            if (ctr == 0) {
                accfftvals[f] = accsmooth[ctr];
            } else if (Math.abs(ctr*delta_f - (1.0/T_periods[f])) < EPSILON){
                accfftvals[f] = accsmooth[ctr];
            } else {
                ulim = ctr;
                llim = ctr - 1;
                uval = accsmooth[ulim];
                lval = accsmooth[llim];
                scale = ((1.0/T_periods[f])-(llim*delta_f)) /(delta_f*(ulim-llim));
                accfftvals[f] = lval + scale * (uval - lval);
            }
        }
        V3Data.add(accfftvals);
        
        //Calculate the spectra for each damping value
        double omega;
        int len = paccel.length;
        double[] sd;
        double[] sv;
        double[] sa;
        double coef_a; double coef_b;
        double coef_c; double coef_d;
        double coef_e; double coef_f;
        
        for (int d = 0; d < V3_DAMPING_VALUES.length; d++) {
            sd = new double[NUM_T_PERIODS];
            sv = new double[NUM_T_PERIODS];
            sa = new double[NUM_T_PERIODS];

            for (int p = 0; p < T_periods.length; p++) {
                coef_a = spectra[d][p][0];
                coef_b = spectra[d][p][1];
                coef_c = spectra[d][p][2];
                coef_d = spectra[d][p][3];
                coef_e = spectra[d][p][4];
                coef_f = spectra[d][p][5];
                omega = (2.0 * Math.PI) / T_periods[p];
                double[][] y = new double[2][len];
                y[0][0] = 0.0;
                y[1][0] = 0.0;
                
                for(int k = 1; k < len; k++) {
                    y[0][k] = coef_a * y[0][k-1] + coef_b * y[1][k-1] + coef_e * paccel[k];
                    y[1][k] = coef_c * y[0][k-1] + coef_d * y[1][k-1] + coef_f * paccel[k];
                }
                //Get the relative displacement (cm)
                double[] disp = y[0];
                ArrayStats stat = new ArrayStats(disp);
                sd[p] = Math.abs(stat.getPeakVal());
                sv[p] = sd[p] * omega;
                sa[p] = sv[p] * omega;
            }
            //get the max value for 5% damping
            if (Math.abs(V3_DAMPING_VALUES[d] - 0.05) < EPSILON) {
                if (strongMotion) {
                    ComputedParams hi = new ComputedParams();
                    housnerIntensity = hi.calculateHousnerIntensity(sv, T_periods);
                }
                ArrayStats stat = new ArrayStats( sa );
                peakVal = stat.getPeakVal();
                int index = stat.getPeakValIndex();
                peakIndex = T_periods[index];
                peakTime = 1.0 / peakIndex;
                for (int idx = 0; idx < T_periods.length; idx++) {
                    if (Math.abs(T_periods[idx] - 0.2) < EPSILON) {
                        Sa_0p2 = sa[idx];
                    } else if (Math.abs(T_periods[idx] - 0.3) < EPSILON) {
                        Sa_0p3 = sa[idx];
                    } else if (Math.abs(T_periods[idx] - 1.0) < EPSILON){
                        Sa_1p0 = sa[idx];
                    } else if (Math.abs(T_periods[idx] - 3.0) < EPSILON) {
                        Sa_3p0 = sa[idx];
                    }
                }
            }
            V3Data.add(sd);
            V3Data.add(sv);
            V3Data.add(sa);
        }
    }
    /**
     * Getter for one of the calculated arrays, the order of the list: the 91 periods,
     * the fft array, sd, sv, sa repeated for each of the 5 damping values. 
     * @param arrnum the number of the array to retrieve
     * @return the V3 array
     */
    public double[] getV3Array(int arrnum) {
        return V3Data.get(arrnum);
    }
    /**
     * Getter for the number of arrays created during V3 processing
     * @return the larray length
     */
    public int getV3ListLength() {
        return V3Data.size();
    }
    /**
     * Getter for the maximum value of the Sa spectrum (real header #74)
     * @return the max value
     */
    public double getPeakVal() {
        return peakVal;
    }
    /**
     * Getter for the period at which maximum Sa occurs (real header #75)
     * @return the max period
     */
    public double getPeakPeriod() {
        return peakIndex;
    }
    /**
     * Getter for the time in the record at which the maximum occurs (real header #76)
     * @return the peak time
     */
    public double getPeakTime() {
        return peakTime;
    }
    /**
     * Getter for the value of Sa at 0.3 seconds period (real header #71)
     * @return the value of Sa at 0.3 seconds
     */
    public double getSa_0p3() {
        return Sa_0p3;
    }
    /**
     * Getter for the value of Sa at 0.2 seconds period (real header #70)
     * @return the value of Sa at 0.2 seconds
     */
    public double getSa_0p2() {
        return Sa_0p2;
    }
    /**
     * Getter for the value of Sa at 1.0 seconds period (real header #72)
     * @return the value of Sa at 1.0 seconds
     */
    public double getSa_1p0() {
        return Sa_1p0;
    }
    /**
     * Getter for the value of Sa at 3.0 seconds period (real header #73)
     * @return the value of Sa at 3.0 seconds
     */
    public double getSa_3p0() {
        return Sa_3p0;
    }
    /**
     * Getter for the data units of the acceleration array
     * @return the data units
     */
    public String getDataUnits() {
        return CMSQSECT;
    }
    /**
     * Getter for the Strong Motion indicator
     * @return true if current record exceeded the strong motion threshold
     * set in the configuration file, false if not
     */
    public boolean getStrongMotion() {
        return strongMotion;
    }
    /**
     * Getter for the Housner Intensity value for the real header
     * @return the Housner Intensity
     */
    public double getHousnerIntensity() {
        return housnerIntensity;
    }
}
