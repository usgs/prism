/*******************************************************************************
 * Name: Java class AICEventDetect.java
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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * <p>
 * This class computes AIC for the input array and picks the P (event) onset.
 * It uses an abbreviated form of the Akaike Information Criterion to locate 
 * the global minimum.  
 * See papers by Maeda, N. (1985). A method for reading
 * and checking phase times in autoprocessing system of seismic wave data, 
 * Zisin Jishin 38, 365-379.
 * </p><p>
 * The result is the index of the event offset in the input array.
 * index = min(AIC(n)) + 1.
 * AIC(n) = k*log(var(x([1,k])) + (n-k-1)*log(var(x[k+1,n])).  
 * Translated into Java by Jeanne Jones (in 2014) from matlab code by Erol Kalkan  
 * (in 2014), from algorithms in Fortran and R by Bill Ellsworth.
 * </p>
 * @author jmjones
 */
public class AICEventDetect {
    private int index;
    private int bufferedIndex;
    private double[] array;
    private double bufferVal;
    /**
     * Constructor for AICEventDetect just initializes variables
     */
    public AICEventDetect() {
        this.index = 0;
        this.bufferedIndex = 0;
        this.bufferVal = 0.0;
    }
    /**
     * This method calculates the event onset index by first removing the median 
     * value from the array. Then the portion of the array to search over
     * (either just to the peak value or the full length) is selected.  This
     * portion is sent to the aicval method, which iterates over the entire
     * length of the portion, calculating the variance before and after the 
     * current iteration index.  aicval returns an array of combined variances 
     * and this method selects the smallest (the global minimum).  The index 
     * into the variance array right after the occurrence of the global minimum 
     * is determined to be the event onset.
     * @param InArray the input array to search for the event onset
     * @param pickrange selects the range of the input array over which to look
     * for the event onset.  If set to 'To_Peak', the portion of the input array
     * from the start to the largest absolute value is used.  If set to 'Full',
     * the entire input array is used.
     * @return the index value for the event onset in the input array
     */
    public int calculateIndex( double[] InArray, String pickrange ) {
        if ((InArray == null) || (InArray.length == 0)) {
            index = -1;
            return index;
        }
        String range = ((pickrange == null) || pickrange.isEmpty() || 
                (!pickrange.equalsIgnoreCase("Full"))) ? "to_peak" : pickrange;
        array = new double[InArray.length];
        double[] arrnew;
        ArrayStats arrstats;
        
        //Make a copy of the array for calculations
        System.arraycopy(InArray, 0, array, 0, InArray.length);
        
        //Remove the median value from the array
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < array.length; i++) {
            stats.addValue(array[i]);
        }
        double median = stats.getPercentile(50);
        ArrayOps.removeValue(array, median);
        
        //window the array based on the pickrange, choosing either the whole
        //array or only the values from the start to the peak absolute value
        if (range.equalsIgnoreCase("to_peak")) {
            arrstats = new ArrayStats( array );
            int indpeak = arrstats.getPeakValIndex();
            arrnew = new double[indpeak];
            System.arraycopy(array, 0 , arrnew, 0, indpeak);
        } else {
            arrnew = array;
        }
        
        //call the method aicval with this windowed array, and it returns an
        //array of calculated variances, with each entry corresponding to the
        //combined variances at that particular index in the windowed array
        double[] temp = aicval(arrnew);
        
        //select the minimum value from this array, get the index (the location
        //in the array of the global minmum) and add 1 to use as the event onset
        arrstats = new ArrayStats( temp );
        index = arrstats.getMinValIndex() + 1;
        return index;
    }
    /**
     * This method handles the iteration over the entire (portion) of the array
     * to calculate the global minimum combined variance.  At each iteration step,
     * the array is divided into 2 sections at the current iteration index and
     * the variance of each section is calculated. The entry into the output array
     * for each iteration step is calculated by (for iteration step i):
     * i * log(var1) + (segment.length-i) * log(var2)
     * @param segment the windowed portion of the input array over which to locate
     * the global minimum
     * @return an array of calculated variances corresponding to the combined
     * variances of the windowed array at each index value
     */
    private double[] aicval( double[] segment ) {
        double[] vararray = new double[segment.length];
        double[] temp;
        double s1;
        double s2;
        SummaryStatistics sumstats;
                
        for (int i = 0; i < segment.length-1; i++) {
            
            //compute the variance in the first part of the array
            temp = new double[i];
            System.arraycopy(segment,0,temp,0,i);
            sumstats = new SummaryStatistics();
            for (double each : temp) {
                sumstats.addValue(each);
            }
            s1 = sumstats.getVariance();
            s1 = (s1 > 0.0) ? Math.log(s1) : 0.0;
            
            //compute the variance in the second part of the array
            temp = new double[segment.length-i];
            System.arraycopy(segment,i,temp,0,segment.length-i);
            sumstats = new SummaryStatistics();
            for (double each : temp) {
                sumstats.addValue(each);
            }
            s2 = sumstats.getVariance();
            s2 = (s2 > 0.0) ? Math.log(s2) : 0.0;
            
            //combine the 2 variances, weighted by the number of entries in
            //each section
            vararray[i] = i * s1 + (segment.length-i) * s2;
        }
        return vararray;
    }
    /**
     * This method subtracts a buffer of a certain length of time to this class's
     * calculated event onset.  The number of samples is calculated by dividing
     * the input buffer time by the number of seconds per sample.  This value
     * is then subtracted from the current event onset index.
     * @param buffer The amount of time in seconds to move the event onset 
     * forward (towards the beginning of the array)
     * @param dtime the number of seconds per sample for this record
     * @return the buffered event onset index
     */
    public int applyBuffer( double buffer, double dtime ) {
        //check for dtime set to 0 and exit before trying divide
        if (Math.abs(dtime - 0.0) < 5*Math.ulp(dtime)) {
            bufferedIndex = -1;
        } else {
            bufferVal = buffer;
            bufferedIndex = index - (int)Math.round(bufferVal/dtime);
            bufferedIndex = (bufferedIndex < 0) ? 0 : bufferedIndex;
        }
        return bufferedIndex;
    }
    /**
     * The getter for the event onset index.
     * @return the event onset
     */
    public int getIndex() {
        return index;
    }
    /**
     * The getter for the buffered event onset index.
     * @return the buffered event onset
     */
    public int getBufferedIndex() {
        return bufferedIndex;
    }
}
