/*******************************************************************************
 * Name: Java class Decimation.java
 * Project: PRISM strong motion record processing using COSMOS data format
 * 
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
import org.apache.commons.math3.complex.Complex;

/**
 * The Decimation class provides the inverse to the Resampling class by reducing
 * the number of samples in the record to the original sampling rate.  It performs 
 * the decimation in the frequency domain, using the fft of the resampled array
 * to decrease the number of samples.
 * 
 * Original algorithm:    Written by Dr. Erol Kalkan, P.E. (ekalkan@usgs.gov)
 * Matlab implementation: $Revision: 1.0 $  $Date: 2017/05/31 14:03:00 $
 * 
 * @author jmjones
 */
public class Decimation {
        private final FFourierTransform fft;
        
    public Decimation() {
        this.fft = new FFourierTransform();
    }
    /**
     * Decimates the input array based on the input factor.  A decimation factor
     * of 2 will reduce the number of samples in the input array by 2.
     * @param yarray input array for decimation
     * @param factor ratio of (input array length) / (output array length)
     * @return the decimated array
     * @throws SmException on invalid input
     */
    public double[] decimateArray( double[] yarray, int factor ) throws SmException {
        if ((yarray == null) || (yarray.length == 0)){
            throw new SmException("Invalid decimation input array");
        }
        if (factor <= 0){
            throw new SmException("Invalid decimation factor " + factor);
        }
        
        //compute fft, shift the array forward (wrapping the end back around
        // to the beginning), and divide each value by the length of the array 
        Complex[] temp = fft.calculateFFTComplexFront( yarray );
        Complex[] zarray = fft.shiftForward( temp );
        for (int i = 0; i < zarray.length; i++) {
            zarray[i] = zarray[i].divide(zarray.length);
        }
        int padlength = zarray.length - yarray.length;
        int val = (padlength % 2 == 0) ? (padlength / factor) : (int)(padlength / factor) + 1;

        // Calculate half the number of total samples. Zarray.length will always
        // be even after the fft, so ((zarray.length * (factor-1)) / 2) will
        // always be an integer
        int nsamp = (int)((zarray.length * (factor-1)) / 2) / factor;
        
        // drop nsamp number of samples from the beginning and end of the spectrum
        Complex[] zshorten = new Complex[zarray.length - 2*nsamp];
        System.arraycopy(zarray,nsamp,zshorten,0,zshorten.length);
        
        // shift the array backwards, wrapping the end back around to the beginning,
        // then multiply by the length of zshorten and get the inverse fft
        temp = fft.shiftBack( zshorten );
        for (int i = 0; i < temp.length; i++) {
            temp[i] = temp[i].multiply(temp.length);
        }
        double[] ytemp = fft.inverseFFTComplex(temp);
        double[] yp = new double[zshorten.length - val];
        System.arraycopy(ytemp,val,yp,0,yp.length);
        return yp;
    }
}
