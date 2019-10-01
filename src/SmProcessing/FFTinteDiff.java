/*******************************************************************************
 * Name: Java class FFTintegrationDifferentiation.java
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

import org.apache.commons.math3.complex.Complex;

/**
* <p>
 * The FFTintediff class provides algorithms for integration and differentiation
 * of a discrete time-signal in the frequency domain using omega arithmetic.
 * </p>
 * <p>
 * Based on matlab code written by Dr. Erol Kalkan, P.E. (ekalkan@usgs.gov)
 * </p>
 * <p>
 * The complex number operations use the apache commons math 
 * package.  http://commons.apache.org/proper/commons-math/
 * </p>
 * @author jmjones
 */
public class FFTinteDiff {
    private final double OPS_EPSILON;
    
    public FFTinteDiff() {
        OPS_EPSILON = 0.00001;
    }
    /**
     * This method computes the integration of a discrete time-signal in the 
     * frequency domain by dividing the spectrum with i / w (w = cyclic frequency)
     * Based on matlab code written by Dr. Erol Kalkan, P.E. (ekalkan@usgs.gov)
     * Matlab date: $Revision: 1.0 $  $Date: 2016/09/03 9:22:00 $
     * @param yarray array to be integrated
     * @param dt the time step in seconds (sampling interval)
     * @return new array containing the approximate integral of the input points,
     * or an array of 0 length if input parameters are invalid
     */
    public double[] integrate( double[] yarray, double dt) {
        if ((yarray == null) || (yarray.length == 0) || (Math.abs(dt - 0.0) < OPS_EPSILON)) {
            return new double[0];
        }
        FFourierTransform fft = new FFourierTransform();
        
        // calculate the length of the time record and
        // compute the shifted fft
        Complex[] temp = fft.calculateFFTComplex( yarray );
        Complex[] zarray = fft.shiftForward( temp );
        
        int zlen = zarray.length;
        double timelen = dt * zlen;
        double df = 1.0 / timelen;
        double omega = 2.0 * Math.PI * df;
        
        // compute frequency array iomega
        double[] warray = new double[zlen];
        for (int i=0;i < zlen; i++) {
            warray[i] = omega * (i - (zlen/2));
        }
       // integrate in the frequency domain by dividing the spectrum by iomega
       // First multiply by (0 + 1j). Result: (a + bj)(0 + 1j) = (b + aj)
       // Then divide by scalar omega (b + aj) / w = ((b/w) + (a/w)j)
       // Then take the conjugate of the result: ((b/w) + (a/w)j) -> ((b/w) - (a/w)j)
        Complex[] znew = new Complex[zlen];
        for (int k=0; k<zlen; k++) {
            if (Double.compare(warray[k], 0.0 ) != 0) {
                znew[k] = new Complex((zarray[k].getImaginary() / warray[k]),
                                    ((-1.0) * zarray[k].getReal() / warray[k]));
            } else {
                znew[k] = new Complex(0.0,0.0);
            }
        }        
        // shift again and compute the inverse fft, then removed extra pad
        // lengths at the end if needed
        temp = fft.shiftBack(znew);
        double[] yptemp = fft.inverseFFTComplex(temp);
        double[] yp = new double[yarray.length];
        System.arraycopy(yptemp,0,yp,0,yarray.length);
        
        // Detrend the output array before returning        
        ArrayOps.removeLinearTrend(yp, dt);
        
        return yp;
    }
    /**
     * NOTE: This method is available but not currently in use in prism.
     * The implementation uses the application of a half-cosine taper (window)
     * at the start and end of the array to reduce ringing: "If the input waveform is non-periodic,
     * artifacts appear in the derivatives due to the implicit discontinuities
     * at the endpoints." Stephen Johnson, MIT, 2011, 'Notes on FFT-based differentiation.'
     * 
     * A long taper (half the distance to the event onset) works well with no
     * ringing.  However, if the start and end of the array are not assumed to be
     * at zero, this will distort the input array at the start and end.  The taperlength
     * parameter controls the length of the window.  If set to 0, no tapering is
     * done.  The actual tapering is over 1/2 the number of samples in taperlength
     * (for the 1/2 cosine applied).
     * 
     * Calculates the approximate derivative in the frequency domain of the input array.
     * Adapted for java from matlab code by Dr. Erol Kalkan, P.E. (ekalkan@usgs.gov)
     * matlab code date: $Revision: 2.0 $  $Date: 2016/09/03 9:22:00 $
     * @param yarray the array to be differentiated
     * @param dt the time step in seconds
     * @param taperlength, number of array elements over which to apply a half-cosine
     * taper to reduce ringing at the array start and end
     * @return new array containing the approximate derivative of the input points,
     * or an array of 0 length if input parameters are invalid
     */
    public double[] differentiate( double[] yarray, double dt, int taperlength) {
        if ((yarray == null) || (yarray.length == 0) || (Math.abs(dt - 0.0) < OPS_EPSILON)) {
            return new double[0];
        }
        //window the input array
        ArrayOps.applyCosineTaper(yarray, taperlength, taperlength);
        FFourierTransform fft = new FFourierTransform();
        
        // calculate the length of the time record and
        // compute the shifted fft
        Complex[] temp = fft.calculateFFTComplex( yarray );
        Complex[] zarray = fft.shiftForward( temp );

        int zlen = zarray.length;
        double timelen = dt * zlen;
        double df = 1.0 / timelen;
        double omega = 2.0 * Math.PI * df;
        
        // compute frequency array iomega
        double[] warray = new double[zlen];
        for (int i=0;i < zlen; i++) {
            warray[i] = omega * (i - (zlen/2));
        }
        
       // Differentiate in the frequency domain by multiplying the spectrum by iomega
       // First multiply by (0 - 1j). Result: (a + bj)(0 + 1j) = (-b - aj)
       // Then multiply by scalar omega (b + aj) * w = ((-b*w) + (-a*w)j)
       // Then get the conjugate of the complex number: (a + bj) -> (a - bj)
        Complex[] znew = new Complex[zlen];
        for (int k=0; k<zlen; k++) {
            znew[k] = new Complex(((-1.0) * zarray[k].getImaginary() * warray[k]),
                                  (-1.0)*((-1.0) * zarray[k].getReal() * warray[k]));
        }        
        // shift again
        temp = fft.shiftBack(znew);
        
        // calculate the inverse fft and remove pads if necessary
        double[] yptemp = fft.inverseFFTComplex(temp);
        double[] dy = new double[yarray.length];
        System.arraycopy(yptemp,0,dy,0,yarray.length);
        
        return dy;
    }
    
}
