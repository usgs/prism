/*******************************************************************************
 * Name: Java class Despiking.java
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

import SmConstants.VFileConstants.SpikeInterpolationMethod;
import java.util.ArrayList;

/**
 * DESPIKING DISCRETE-TIME SIGNAL USING HISTOGRAM METHOD
 * 
 *   Time series may contain undesired transients and spikes. This function
 *  replaces sudden spike(s) exceeding the threshold value by interpolating
 *  among previous and subsequent data points. The threshold is defined as
 *  the mean +/- a number of standard deviations of windowed data centered at
 *  the spike location(s). 
 * 
 * Caveat: This function is for detecting sudden spikes and may not be effective
 * for detection of long duration spike-like pulses.
 * 
 * Original algorithm:    Written by Dr. Erol Kalkan, P.E. (ekalkan@usgs.gov)
 * Matlab implementation: $Revision: 2.0 $  $Date: 2018/12/10 08:58:00 $
 * 
 * @author jmjones
 */
public class Despiking {
    private final int DIFFORDER = 5;
    private int numpass;
    private int numstd;
    private int numbins;
    private int spikeindex;
    private int windowsize;
    private int neighbors;
    private final SpikeInterpolationMethod method = SpikeInterpolationMethod.LINEAR;;
    private int order;
    /**
     * Despiking constructor takes the number of standard deviations above the mean
     * to be used for defining spikes.  This mean is determined in a window of
     * values around the possible spike.
     * @param numstandarddev number of standard deviations
     */
    public Despiking(int numstandarddev) {
        this.numpass = 2;
        this.numbins = 4;
        this.neighbors = 5;
        this.windowsize = 25;
        if (method == SpikeInterpolationMethod.LINEAR) {
            this.order = 1;
        }
        this.numstd = numstandarddev; 
    }
    /**
     * Looks for possible spikes in the input array by differentiating the array
     * and making a histogram of the magnitudes (absolute values).  Any spike above the
     * midpoint of the topmost bin is examined to look for spikes.  Multiple passes
     * can be made through the spike-corrected array to look for more spikes.
     * @param array input array for spike removal.  This array is modified
     * @param dt delta time for the sampling rate in seconds
     * @return true if any spikes were found
     */
    public boolean removeSpikes( double[] array, double dt ){
        boolean spikesfound = false;
        this.spikeindex = 0;
        if (array.length == 0) {
            spikeindex = -1;
            return spikesfound;
        }
        for (int k = 0; k < numpass; k++) {
            int fixed;
            // calculate the derivative of the input array and convert to
            // absolute value before determining spike locations.  Using
            // derivative here to look for high rate of change that might be a
            // spike.
            double[] diffarr = ArrayOps.differentiate(array, dt, DIFFORDER);
            for (int j=0; j<diffarr.length; j++) {
                diffarr[j] = Math.abs(diffarr[j]);
            }
            
            // define the magnitude threshold for possible spikes via the
            // histogram method, then make a list of indicies in the input
            // array to check for spikes.
            ArrayStats stats = new ArrayStats( diffarr );
            double num = stats.getModalMin(numbins);
            ArrayList<Integer> locations = new ArrayList<>();
            for (int j=0; j<diffarr.length; j++) {
                if (diffarr[j] > num) {
                    locations.add(j);
                }
            }        
            Integer[] spikelocs = locations.toArray(new Integer[locations.size()]);
            locations.clear();

            // fix each spike and keep count of the number fixed
            for (int idx : spikelocs) {
                fixed = spikeFix( array, idx, neighbors, windowsize );
                spikeindex = spikeindex + fixed;
            }
            if (spikeindex > 0) {
                spikesfound = true;
            }
        }
        return spikesfound;
    }
    /**
     * Does the actual spike check and removal. Takes the input array and the index
     * of the possible spike and examines its magnitude compared to its neighbors
     * within a window.  If a spike is detected it is replaced with a linearly
     * interpolated value.
     * @param inarr input array for spike removal - this array is modified
     * @param idx index in input array of possible spike
     * @param nbors number of neighbors on each side to use for interpolation
     * @param wsize size of window around spike to use for mean, stddev calculation
     * @return 1 if spike found, 0 if not
     */
    public int spikeFix( double[] inarr, int idx, int nbors, int wsize ) {
        int found = 0;
        int zone = 3; // number of elements to replace if spike found
        int index = idx;
        double[] subset;
        double substdev = 0.0;
        double submean;
        int halfwindow = (int)Math.floor(wsize / 2.0);
        
        // index of potential spike may be off by 1 after central difference
        // differentiation, so verify that it points to max and adjust 
        // if needed
        if ((index > 0) && (index < (inarr.length-1))) {
            double maxim = inarr[index];
            int newmax = index;
            for (int i = index-1; i < index+2; i++) {
                if (Math.abs(inarr[i]) > Math.abs(maxim)) {
                    maxim = inarr[i];
                    newmax = i;
                }
            }
            index = newmax;
        }
        // If spike occurs at beginning or end of array, replace with the mean
        // of nearby values
        int nwndw = 2*nbors;
        if (index <= nwndw) {
            subset = new double[nwndw];
            System.arraycopy(inarr, (index + 1), subset, 0, nwndw);
            ArrayStats substat = new ArrayStats( subset );
            inarr[index] = substat.getMean();
            found = 1;
        } else if ((index >= (inarr.length - nwndw))) {
            subset = new double[nwndw];
            System.arraycopy(inarr, (index - nwndw), subset, 0, nwndw);
            ArrayStats substat = new ArrayStats( subset );
            inarr[index] = substat.getMean();            
            found = 1;
        } else {            
            // overwrite neighbors width if potential spike occurs within
            // first or last data window
            if (index <= halfwindow) {
                wsize = index * 2;
                halfwindow = (int)Math.floor(wsize / 2.0);
            } else if (index >= (inarr.length - halfwindow)) {
                wsize = inarr.length - index;
                halfwindow = (int)Math.floor(wsize / 2.0);
            }
            
            // overwrite neighbors if it is bigger than half of window size
            if (nbors > halfwindow) {
                nbors = halfwindow - 1;
            }
            // compute mean and standard deviation of data window centered at
            // potential spike while excluding spike
            subset = new double[halfwindow * 2];
            System.arraycopy(inarr, (index - halfwindow), subset, 0, halfwindow);
            System.arraycopy(inarr, (index +1), subset, halfwindow, halfwindow);
            ArrayStats substat = new ArrayStats( subset );
            submean = substat.getMean();
            substdev = ArrayOps.findStandardDev(subset);
//            System.out.println(String.format("i = %d, x(i) = %5.2f, mu_x = %5.2f, std_x = %5.2f",index,inarr[index],submean,substdev));
            
            // check threshold exceedance
            if ((inarr[index] >= (submean + substdev*numstd)) || 
                            (inarr[index] <= (submean - substdev*numstd))) {
                //replace spike region using interpolation of neighboring data points
                double[] startx = new double[2*nbors];
                double[] starty = new double[2*nbors];
                double[] newx = {index-1, index, index+1};
                for (int i = 0; i < nbors; i++) {
                    startx[i] = index - nbors - 1 + i;
                    starty[i] = inarr[index - nbors - 1 + i];
                }
                for (int i = 0; i < nbors; i++) {
                    startx[i + nbors] = index + 2 + i;
                    starty[i + nbors] = inarr[index + 2 + i];
                }
                double[] newy = ArrayOps.interpolate(startx, starty, newx, order);
                System.arraycopy(newy, 0, inarr, index-1, zone);
                found = 1;
            }
        }
//        if (found == 1) {
//            System.out.println(String.format("spike found at: %d and new value is %5.2f",index,inarr[index]));
//        }
        return found;
    }
    /**
     * Provides ability to set a different number of bins for the histogram
     * @param newnum new number of bins
     */
    public void setNumBins( int newnum ) {this.numbins = newnum;}
    /**
     * Provides the ability to adjust the number of standard deviations used
     * when determining what is a spike
     * @param newnum number of standard deviations
     */
    public void setNumStd( int newnum ) {this.numstd = newnum;}
    /**
     * Provides the ability to adjust the number of passes through the despiking
     * algorithm
     * @param newnum number of passes
     */
    public void setNumPasses( int newnum ) {this.numpass = newnum;}
    /**
     * Provides the ability to adjust the window size as an upper bound on the
     * neighborhood
     * @param newnum window size
     */
    public void setWindowSize( int newnum ) {this.windowsize = newnum;}
    /**
     * Provides the ability to adjust the size of the neighborhood used to determine
     * replacement value for spike
     * @param newnum neighborhood size
     */
    public void setNeighbors( int newnum ) {this.neighbors = newnum;}
    /**
     * Getter for the number of bins used in the histogram
     * @return number of histogram bins
     */
    public int getNumBins() {return this.numbins;}
    /**
     * Getter for the number of standard deviations used from the mean to 
     * determine if a spike should be fixed
     * @return number of standard deviations
     */
    public int getNumStd() {return this.numstd;}
    /**
     * Getter for the number of passes through the array to use in order to look
     * for all spikes
     * @return number of passes through spike removal
     */
    public int getNumPasses() {return this.numpass;}
    /**
     * Getter for the window size to use for upper bound for neighborhood
     * @return the window size
     */
    public int getWindowSize() {return this.windowsize;}
    /**
     * Getter for the neighborhood to use for interpolating (or getting mean of)
     * replacement points for a spike
     * @return the neighborhood size
     */
    public int getNeighbors() {return this.neighbors;}
    /** 
     * Getter for the interpolation method used during spike removal
     * (currently only linear)
     * @return the interpolation method
     */
    public SpikeInterpolationMethod getMethod() {return this.method;}
    /**
     * Getter for the number of spikes found after all passes
     * @return number of spike found
     */
    public int getSpikeIndex() {return this.spikeindex;}
}
