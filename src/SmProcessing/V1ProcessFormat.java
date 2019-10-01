/*******************************************************************************
 * Name: Java class V1ProcessFormat.java
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
 * This abstract class defines the data and methods that are common to V1
 * processing / updating between the automated and gui (review tool) versions
 * of prism.
 * 
 * @author jmjones
 */
abstract class V1ProcessFormat {
    protected double[] accel;
    protected double meanToZero;
    protected double peakVal;
    protected int peakIndex;
    protected double avgVal;
    protected String[] commentUpdates;
    
    /**
     * Constructor for abstract class just initializes some variables
     */
    public V1ProcessFormat() {
        this.meanToZero = 0.0;
        this.peakVal = Double.MIN_VALUE;
        this.peakIndex = -1;
        this.avgVal = 0.0;
    }
    /**
     * This abstract method gets implemented by either the auto or gui version
     * of prism.
     */
    public abstract void processV1Data();
    
    /**
     * Getter for the mean value removed from the array.
     * @return the mean that was removed from the array
     */
    public double getMeanToZero() {
        return this.meanToZero;
    }
    /**
     * Getter for the peak value in the array (after mean removal).  This is the
     * largest number in the array, either positive or negative.
     * @return the peak value
     */
    public double getPeakVal() {
        return this.peakVal;
    }
    /** Getter for the index in the array where the peak value occurs.
     * @return the array index of the peak value
     */
    public int getPeakIndex() {
        return this.peakIndex;
    }
    /**
     * Getter for the average value of the final array, which should be 0.0.
     * @return the array mean
     */
    public double getAvgVal() {
        return this.avgVal;
    }
    /**
     * Getter for a reference to the data array
     * @return reference to the data array
     */
    public double[] getV1Array() {
        return this.accel;
    }
    /**
     * Getter for the length of the data array
     * @return length of the data array
     */
    public int getV1ArrayLength() {return this.accel.length;}
    public String[] getCommentUpdates() { return commentUpdates; }
}
