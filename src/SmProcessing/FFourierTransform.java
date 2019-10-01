/*******************************************************************************
 * Name: Java class FFourierTransform.java
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

import java.util.Arrays;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * This class calculates the FFT of an array using the Apache Commons Math 
 * package and returns the result as magnitudes (square root of the squares of
 * the real and imaginary parts).  It first pads the array to the next closest
 * power-of-2 length before calculating the FFT.
 * @author jmjones
 */
public class FFourierTransform {
    private int powerlength;
    private int fftlen;
    private int cpowerlength;
    /**
     * Constructor just initializes variables.
     */
    public FFourierTransform() {
        this.powerlength = 0;
        this.fftlen = 0;
        this.cpowerlength = 0;
    }
    /**
     * Performs the FFT calculations by padding the input array AT THE END to the closest
     * power of 2 gt or eq to the current length, calling the FFT transform method,
     * extracting only the first half of the complex array returned and converting
     * these values to magnitudes.
     * @param array input array for calculating the transform
     * @return the magnitudes of the transformed array
     */
    public double[] calculateFFT( double[] array ) {
        double[] arrpad = padArrayAtEnd( array );
         
        FastFourierTransformer fft = new FastFourierTransformer( DftNormalization.STANDARD);
        Complex[] transfreq = fft.transform(arrpad, TransformType.FORWARD);

        fftlen = (powerlength / 2) + 1;
        double[] mags = new double[fftlen];
        for (int i = 0; i < fftlen; i++) {
            mags[i]= Math.sqrt(Math.pow(transfreq[i].getReal(),2) + 
                                    Math.pow(transfreq[i].getImaginary(),2));
        }
        return mags;
    }
    /**
     * Normalizes the magnitudes after the FFT by dividing each magnitude by the
     * number of elements in the original array input to the FFT (not the length
     * of the magnitude array). The input magnitude array is modified.
     * @param mags magnitudes of the transformed array (real values, not complex)
     * @param original_length length of the real array input to calculateFFT 
     */
    public void normalizeMags( double[] mags, int original_length ) {
        if ((original_length <= 0) || (mags == null) || (mags.length == 0)) {
            return;
        }
        for (int i=0; i<mags.length; i++) {
            mags[i] = mags[i] / original_length;
        }
    }
    /**
     * Performs the FFT calculations by padding the input array AT THE END to the closest
     * power of 2 gt or eq to the current length, calling the FFT transform method,
     * and returning the complex array.
     * @param array input array for calculating the transform
     * @return the transformed array
     */
    public Complex[] calculateFFTComplex( double[] array ) {
        double[] arrpad = padArrayAtEnd( array );
        FastFourierTransformer fft = new FastFourierTransformer( DftNormalization.STANDARD);
        Complex[] transfreq = fft.transform(arrpad, TransformType.FORWARD);
        return transfreq;
    }
    /**
     * Performs the FFT calculations by padding the input array AT THE START to the closest
     * power of 2 gt or eq to the current length, calling the FFT transform method,
     * and returning the complex array.
     * @param array input array for calculating the transform
     * @return the transformed array
     */
    public Complex[] calculateFFTComplexFront( double[] array ) {
        double[] arrpad = padArrayAtStart( array );
        FastFourierTransformer fft = new FastFourierTransformer( DftNormalization.STANDARD);
        Complex[] transfreq = fft.transform(arrpad, TransformType.FORWARD);
        return transfreq;
    }
    /**
     * Calculates the inverse FFT on an input complex array (first padding if
     * needed with complex zeros at the END of the array) and returns an array
     * of doubles containing only the real component of the iFFT result.
     * @param transfreq array of complex frequency values
     * @return an array containing the real components of the iFFT result
     */
    public double[] inverseFFTComplex( Complex[] transfreq ) {
        Complex[] transpad = padArrayComplexAtEnd( transfreq );
        double[] realvals;
        FastFourierTransformer fft = new FastFourierTransformer( DftNormalization.STANDARD);
        Complex[] invvals = fft.transform(transpad, TransformType.INVERSE);
        realvals = new double[invvals.length];
        for (int i = 0; i < invvals.length; i++) {
            realvals[i] = invvals[i].getReal();
        }
        return realvals;
    }
    /**
     * pads the incoming array with zeros to the nearest power of 2 length, pads
     * are added at the end
     * @param array the input array
     * @return the padded real array
     */
    private double[] padArrayAtEnd( double[] array ) {
        powerlength = findPower2Length( array.length);
        if (array.length != powerlength) {
            double[] arrpad = new double[powerlength];
            Arrays.fill(arrpad, 0.0);
            System.arraycopy(array, 0, arrpad, 0, array.length);
            return arrpad;
        } else {
            return array;
        }
    }
    /**
     * pads the incoming array with zeros to the nearest power of 2 length, pads
     * are added at the start
     * @param array the input array
     * @return the padded real array
     */
    private double[] padArrayAtStart( double[] array ) {
        powerlength = findPower2Length( array.length);
        int inlen = array.length;
        if (inlen != powerlength) {
            double[] arrpad = new double[powerlength];
            Arrays.fill(arrpad, 0.0);
            System.arraycopy(array, 0, arrpad, (powerlength-inlen), inlen);
            return arrpad;
        } else {
            return array;
        }
    }
    /**
     * pads the incoming array with zeros to the nearest power of 2 length, pads
     * are added at the end
     * @param array the input array
     * @return the padded real array
     */
    private Complex[] padArrayComplexAtEnd( Complex[] carray ) {
        cpowerlength = findPower2Length( carray.length);
        if (cpowerlength != carray.length) {
            Complex[] arrpad = new Complex[cpowerlength];
            Arrays.fill(arrpad, Complex.ZERO);
            System.arraycopy(carray, 0, arrpad, 0, carray.length);
            return arrpad;
        } else {
            return carray;
        }
    }
    /**
     * finds the nearest power of 2 that is greater than or equal to the input value
     * @param length the length of the input array
     * @return the nearest power of 2 length
     */
    public int findPower2Length( int length ) {
        int powlength = 2;
        int i = 2;
        if (length > 2) {
            while (powlength < length) {
                powlength = (int)Math.pow( 2,i);
                i++;
            }
        }
        return powlength;
    }
    /**
     * shifts the zero frequency to the center of the array
     * @param inarray input complex array to shift
     * @return shifted array
     */
    public Complex[] shiftForward( Complex[] inarray ) {
        int inlen = inarray.length;
        int half = (inlen % 2 == 0) ? inlen / 2 : (int)(inlen / 2) + 1;
        Complex[] shift = new Complex[inlen];
        System.arraycopy(inarray, 0, shift, (inlen-half), half);
        System.arraycopy(inarray, half, shift, 0, (inlen-half));
        return shift;
    }
    /**
     * inverse of shiftforward, returning the zero frequency to the start
     * of the array
     * @param inarray shifted complex array to reset
     * @return un-shifted array
     */
    public Complex[] shiftBack( Complex[] inarray ) {
        int inlen = inarray.length;
        int half = (inlen % 2 == 0) ? inlen / 2 : (int)(inlen / 2);
        Complex[] reverse = new Complex[inlen];
        System.arraycopy(inarray, 0, reverse, (inlen-half), half);
        System.arraycopy(inarray, half, reverse, 0, (inlen-half));        
        return reverse;
    }
    /**
     * Getter for the calculated power of 2 length
     * @return the power of 2 length
     */
    public int getPowerLength() {
        return powerlength;
    }
}
