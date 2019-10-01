/*******************************************************************************
 * Name: Java class EventOnsetDetection.java
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

/**
 * <p>
 * This class uses the P-wave Detector method to pick the event onset time.
 * </p><p>
 * Described in:
 * Kalkan, Erol, An Automatic P-Wave Onset Time Detector, USGS, 2015, in review.
 * </p>
 * The method creates a mathematical model of a single-degree-of-freedom
 * oscillator with a short resonant frequency, and
 * high damping ratio.  When the input trace is applied to the model, the output
 * damping energy can be used to detect the arrival of the P-wave.  The damping
 * energy "is zero at the beginning of the signal, zero or near zero before the
 * P-wave arrival, and builds up rapidly with the P-wave." (p.1) This
 * damping energy is binned by a histogram to determine when its state
 * begins to change from 0.  The nearest zero crossing before this time
 * is determined to be the event onset.
 * @author jmjones
 */
public class EventOnsetDetection {
    private static final int NUM_BINS = 200; //used for the histogram
    private static final double XI = 0.6;  //damping ratio
    private static final double TN = 0.01; //vibration period
    private final double omegan;
    private final double const_C;
    private final double dtime;
    private final double coef_a; 
    private final double coef_b; 
    private final double coef_c; 
    private final double coef_d; 
    private final double coef_e; 
    private final double coef_f;
    
    private int eventStart;
    private double bufferVal;
    private int bufferedStart;
    private final int difforder;
    
    /**
     * Constructor gets the event onset coefficients for the input sampling
     * interval for solving the oscillator motion equation.
     * @param dtime the sampling interval in sec/sample
     */
    public EventOnsetDetection(double dtime) {
        this.dtime = dtime;
        omegan= 2.0 * Math.PI / TN;
        const_C = 2.0 * XI * omegan;
        
        eventStart = 0;
        bufferVal = 0.0;
        bufferedStart = 0;
        difforder = 5;

        //Get the matrix coefficients for the specific sampling interval
        EventOnsetCoefs pickCoef = new EventOnsetCoefs();
        double[] Ae;
        double[] AeB;
        Ae = pickCoef.getAeCoefs(dtime);
        AeB = pickCoef.getAeBCoefs(dtime);
        coef_a = Ae[0];
        coef_b = Ae[1];
        coef_c = Ae[2];
        coef_d = Ae[3];
        coef_e = AeB[0];
        coef_f = AeB[1];
    }
    /**
     * Show coefficients retrieved for the input sample interval (for debug)
     * @return the coefficient array
     */
    public double[] showCoefficients() {  //for debug
        double[] coefs = new double[6];
        coefs[0] = coef_a;
        coefs[1] = coef_b;
        coefs[2] = coef_c;
        coefs[3] = coef_d;
        coefs[4] = coef_e;
        coefs[5] = coef_f;
        return coefs;
    }
    /**
     * Finds the event onset by applying the input acceleration array to the 
     * model of the oscillator and calculating the damping energy output.  This
     * damping energy is binned by a histogram to determine when its state
     * begins to change from 0.  The nearest zero crossing before this time
     * is determined to be the event onset.
     * @param accTotal the input acceleration array
     * @return the event onset index
     */
    public int findEventOnset( final double[] accTotal) {
        if ((accTotal == null) || (accTotal.length == 0)) {
            eventStart = -1;
            return eventStart;
        }
        
        // Find the peak value and use only the array from start to the peak
        // value to look for the p-wave arrival.
        ArrayStats accstat = new ArrayStats( accTotal );
        int accpeak = accstat.getPeakValIndex();
        double[] acc = new double[accpeak];
        System.arraycopy(accTotal,0,acc,0,accpeak);
        int len = acc.length;
        int found = 0;
                
        //Calculate the transient response of an oscillator with vibration period
        //TN and damping ratio XI subjected to support acceleration (array acc)
        //and sampled at a step deltaT.
        double[][] y = new double[2][len];
        y[0][0] = 0.0;
        y[1][0] = 0.0;
        
        for(int k = 1; k < len; k++) {
            y[0][k] = coef_a * y[0][k-1] + coef_b * y[1][k-1] + coef_e * acc[k];
            y[1][k] = coef_c * y[0][k-1] + coef_d * y[1][k-1] + coef_f * acc[k];
        }
        
        //Get the relative velocity (m/sec) of mass
        double[] veloc = y[1];
        double[] Edi = new double[len];
        
        //Integrand of viscous damping energy (m^2/sec^3)
        for (int i = 0; i < len; i++) {
            Edi[i] = const_C * Math.pow(veloc[i], 2);
        }
        //Viscous damping energy over mass (m^2/sec^2)
        double[] Edoverm = ArrayOps.integrate(Edi, dtime, 0.0);
        
        //Spectral viscous damping energy over mass (m^2/sec^2)
        //find largest absolute value in array
        double Edoverm_max = Double.MIN_VALUE;
        for (double each : Edoverm) {
            if (Math.abs(each) > Edoverm_max) {
                Edoverm_max = Math.abs(each);
            }
        }
        //normalize the array by dividing all vals by the max
        double[] EIM = new double[Edoverm.length];
        for (int i = 0; i < Edoverm.length; i++) {
            EIM[i] = Edoverm[i] / Edoverm_max;
        }
        //Integrand of normalized damping energy (m^2/sec^3)
        double[] PIM = ArrayOps.differentiate(EIM, dtime, difforder);

        // find the most common value in the lower half of the range of PIM.
        // The value returned is the most frequently-occurring
        // value in the lower half of the array min-max range.
        ArrayStats statPIM = new ArrayStats(PIM);
        double lowerMode = statPIM.getModalMin(NUM_BINS);

        //Now find the index of the first occurrence in the array of a value
        //that is greater than the most frequently-occurring value.
        int peak = 0;
        for (int i = 0; i < len; i++) {
            if (PIM[i] > lowerMode) {
                peak = i;
                break;
            }
        }
        //In the array subset acc[0:peak], start at the end and work back to front
        //to find the index of the first zero-crossing.  This is the start of
        //the event.  The zero-crossing is identified by 2 consecutive values
        //in the array with differing signs.
        for (int k = peak; k > 0; k--) {
            if ((acc[k] * acc[k-1]) < 0.0) {
                found = k-1;
                break;
            }
        }
        //Return the index into the acceleration array that marks the start of
        //the event, adjusted by the buffer amount
        eventStart = found;
        return eventStart;
    }
    /**
     * Applies of buffer of specified time length to the event onset index to
     * move it forward in time (towards the start of the array).
     * @param buffer the length of time in seconds to buffer the event onset
     * @return the buffered index into the array
     */
    public int applyBuffer(double buffer) {
        bufferVal = buffer;
        bufferedStart = eventStart - (int)Math.round(bufferVal/dtime);
        bufferedStart = (bufferedStart < 0) ? 0 : bufferedStart;
        return bufferedStart;
    }
    /**
     * Getter for the event onset index
     * @return the event onset
     */
    public int getEventStart() {
        return this.eventStart;
    }
    /**
     * Getter for the buffer length
     * @return the buffer length
     */
    public double getBufferLength() {
        return this.bufferVal;
    }
    /**
     * Getter for the buffered event start index
     * @return the buffered event start index
     */
    public int getBufferedStart() {
        return this.bufferedStart;
    }
}
