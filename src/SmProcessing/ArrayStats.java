/*******************************************************************************
 * Name: Java class ArrayStats.java
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

/**
 * The ArrayStats class contains methods to calculate various statistics on an
 * array that is entered during object construction.  The constructor does the
 * computation of mean, peak value, and so on, and various getters provide
 * access to the calculated parameters.  The input array is not modified by
 * this class.  The peak value is the largest value found in the array,
 * regardless of the sign of the value.  Additional methods also calculate the
 * array histogram and determine the approximate most frequently occurring
 * value in the lower range of the array.
 * 
 * @author jmjones
 */
public class ArrayStats {
    private final double[] statArray;
    private final int length;
    private double total;
    private double maxhigh;
    private double maxlow;
    private double maxabs;
    private double mean;
    private int maxhighid;
    private int maxlowid;
    private int maxabsid;
    private double histstep;
    private double range;
    /**
     * The constructor for this class does all the computation on the array for
     * max, min, peak value, mean.  The other methods then retrieve individual
     * statistics on the array. Additional methods are available to calculate
     * standard deviation, histogram, and modal minimum.
     * @param array input array to calculate statistics on
     */
    public ArrayStats( double[] array ) {
        statArray = array;
        length = (array != null) ? array.length : 0;
        total = 0.0;
        maxhigh = Double.MIN_VALUE;
        maxlow = Double.MAX_VALUE;
        maxabs = Double.MIN_VALUE;
        mean = Double.MIN_VALUE;
        maxhighid = -1;
        maxlowid = -1;
        maxabsid = -1;
        histstep = 0.0;
        range = 0.0;
        
        if (length < 2) {
            return;
        }
        
        //Calculate the total value and the mean.
        //Record the highest and lowest values and their indexes
        for (int i = 0; i < length; i++) {
            total = total + statArray[i];
            if (statArray[i] > maxhigh) {
                maxhigh = statArray[i];
                maxhighid = i;
            }
            if (statArray[i] < maxlow) {
                maxlow = statArray[i];
                maxlowid = i;
            }
            mean = total / length;
        }
        range = maxhigh - maxlow;
        
        //Find the peak value and its index.
        if (Math.abs(maxhigh) > Math.abs(maxlow)) {
            maxabs =  maxhigh;
            maxabsid = maxhighid;
        } else {
            maxabs =  maxlow;
            maxabsid = maxlowid;
        }        
    }
    /**
     * Builds a histogram from the array that was given in the constructor call, 
     * with the assumption that the array hasn't been modified since that
     * constructor call (maxhigh and maxlow haven't changed).  The very last
     * value in the array is not added to the histogram (each histogram bin's
     * lowest value is inclusive and highest value is exclusive.)
     * 
     * @param numIntervals the number of intervals to use in the histogram
     * @return array holding the counts of values that fell within each
     * interval of the histogram
     */
    public int[] makeHistogram(int numIntervals) {
        if ((numIntervals <= 0) || (length == 0)) {
            int[] hist = new int[0];
            return hist;
        }
        int[] hist = new int[numIntervals];
        Arrays.fill(hist, 0);
        //determine the width of each bin
        histstep = range / numIntervals;
        //Go through the array, find which bin the current value belongs to, and
        //increment that bin.
        double lowvalue;
        double highvalue;
        for (double val : statArray) {
            for (int i = 0; i < numIntervals; i++ ) {
                //the upper test for a value in
                //a bin is exclusive while the lower test is inclusive (>=).
                lowvalue = maxlow + i * histstep;
                highvalue = maxlow + (i + 1) * histstep;
                if ((val > lowvalue) || (Math.abs(val - lowvalue) < 5*Math.ulp(lowvalue))) {
                    if (val < highvalue) {
                        hist[i] += 1;
                        break;
                    }
                }
            }
        }
        return hist;
    }
    /**
     * Find the most frequently occurring value in the lower and upper range of array values.
     * This is done by making a histogram of the array values and, looking only
     * at the lower (or upper) half of the histogram, find the bin with the highest count.
     * This bin corresponds to a range of values in the array, and the mid point
     * value of the range is calculated and returned as the modal minimum (and maximum).
     * @param numbins the number of bins to use for the histogram
     * @return a 2-element array with the approximate array values representing the most frequently
     * seen low and high range values.  Result[0] = modal minimum and result[1] = modal maximum.
     */
    public double[] getModalMaxAndMin( int numbins ) {
        double[] levels = {Double.MIN_VALUE, Double.MIN_VALUE};
        int[] hist;
        
        hist = makeHistogram( numbins );
        if (hist.length == 0) {
            return levels;
        }
        int startbin = 0;
        int stopbin = 0;
        
        //Find the lowest non-zero histogram bin.  This is the start bin.
        for (int i = 0; i < numbins; i++) {
            if (hist[i] > 0) {
                startbin = i;
                break;
            }
        }
        //Find the highest non-zero histogram bin.  This is the stop bin.
        for (int i = hist.length-1;  i >= 0; i-- ) {
            if (hist[i] > 0) {
                stopbin = i;
                break;
            }
        }
        //Find the bin in the first half of the histogram with the highest
        //count.  This is the modal value for the minimum.
        int mode = 0;
        int modeindex = -1;
        for (int i = startbin; i < ((stopbin-startbin)/2 + 1); i++) {
            if (hist[i] >= mode) {
                mode = hist[i];
                modeindex = i;
            }
        }
        //returns the center point value of the range
        levels[0] = maxlow + (histstep * modeindex) + histstep * 0.5;

        //Find the bin in the 2nd half of the histogram with the highest
        //count.  This is the modal value for the maximum.
        mode = 0;
        modeindex = -1;
        int midbin = startbin + (stopbin - startbin)/2;
        for (int i = (midbin+1); i < (stopbin + 1); i++) {
            if (hist[i] >= mode) {
                mode = hist[i];
                modeindex = i;
            }
        }
        //returns the center point value of the range
        levels[1] = maxlow + (histstep * modeindex) + histstep * 0.5;
        
        return levels;
    }
    /**
     * Getter for the calculated mean of the array
     * @return the mean value
     */
    public double getMean() {return mean;}
    /**
     * Getter for the calculated peak value, which is the largest + or - value
     * found in the array
     * @return the peak value
     */
    public double getPeakVal() {return maxabs;}
    /**
     * Getter for the index of the peak value
     * @return array index for the peak value
     */
    public int getPeakValIndex() {return maxabsid;}
    /**
     * Getter for the minimum array value
     * @return minimum array value
     */
    public double getMinVal() {return maxlow;}
    /**
     * Getter for the minimum array value index
     * @return the index of the minimum array value
     */
    public int getMinValIndex() {return maxlowid;}
    /**
     * Getter for the maximum array value
     * @return maximum array value
     */
    public double getMaxVal() {return maxhigh;}
    /**
     * Getter for the maximum array value
     * @return index for the maximum array value
     */
    public int getMaxValIndex() {return maxhighid;}
    /** 
     * Getter for the range of the array (maxval - minval)
     * @return returns the range of the array
     */
    public double getRange() {return range;}
    /**
     * Getter for the interval size calculated for the histogram, which is
     * (maxval - minval) / numIntervals
     * 
     * @return the calculated histogram interval
     */
    public double getHistogramInterval() {return histstep;}
/**
 * Getter for the most frequently occurring low value in the array
 * @param numbins number of bins to use when creating histogram of array values
 * @return the modal minimum
 */
    public double getModalMin( int numbins ) {
        double[] levels = getModalMaxAndMin( numbins );
        return levels[0];
    }
    /**
     * Getter for the most frequently occurring high value in the array
     * @param numbins number of bins to use when creating histogram of array values
     * @return the modal maximum
     */
    public double getModalMax( int numbins ) {
        double[] levels = getModalMaxAndMin( numbins );
        return levels[1];
    }
}

