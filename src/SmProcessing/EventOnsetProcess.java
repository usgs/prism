/*******************************************************************************
 * Name: Java class EventOnsetProcess.java
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

import SmConstants.VFileConstants;
import SmException.SmException;

/**
 * This class handles the event onset processing for the larger V2 process work flow.
 * Event onset detection involves removing any linear trends from the input
 * acceleration, filtering, and then running the event onset detection algorithm.
 * @author jmjones
 */
public class EventOnsetProcess {
    private final int numroll;
    private final double taperlength;
    private final double lowcutoff;
    private final double highcutoff;
    private final double ebuffer;
    private int pickIndex;
    private int startIndex;
    private double taperused;
/**
 * The event onset process constructor initializes variables for filtering and
 * buffering of the onset time
 * @param lowcut the filter low cutoff frequency
 * @param highcut the filter high cutoff frequency
 * @param taper the taper length from the configuration file
 * @param numrl the filter roll-off, which is 1/2 the filter order
 * @param ebuf the length of the requested buffer for the event onset
 */    
    public EventOnsetProcess( double lowcut, double highcut, double taper, 
                                int numrl, double ebuf) {
        this.lowcutoff = lowcut;
        this.highcutoff = highcut;
        this.taperlength = taper;
        this.numroll = numrl;
        this.ebuffer =  ebuf;
        this.pickIndex = 0;
        this.startIndex = 0;
        this.taperused = 0.0;
    }
    /**
     * Removes any linear trend in acceleration, then filters and finds the event onset
     * @param acc the acceleration array, this array is modified during processing
     * @param dtime the sampling interval in seconds per sample
     * @param emethod the event onset method to use
     * @throws SmException if unable to calculate the filter parameters
     */
    public void findEventOnset(double[] acc, double dtime, 
                        VFileConstants.EventOnsetType emethod) throws SmException {
        
        ArrayOps.removeLinearTrend( acc, dtime);
        
        //set up the filter coefficients and run
        ButterworthFilter filter = new ButterworthFilter();
        boolean valid = filter.calculateCoefficients(lowcutoff, highcutoff, 
                                                        dtime, numroll, true);
        if (valid) {  //calcSec here is used in place of event onset, which is TBD
            int calcSec = (int)(taperlength * dtime);
            filter.applyFilter(acc, taperlength, calcSec);  //filtered values are returned in acc
            taperused = filter.getTaperlength();
        } else {
            throw new SmException("Invalid bandpass filter input parameters");
        }
        // Find event onset
        if (emethod == VFileConstants.EventOnsetType.PWD) {
            EventOnsetDetection depick = new EventOnsetDetection( dtime );
            pickIndex = depick.findEventOnset(acc);
            startIndex = depick.applyBuffer(ebuffer);
        } else {
            AICEventDetect aicpick = new AICEventDetect();
            pickIndex = aicpick.calculateIndex(acc, "ToPeak");
            startIndex = aicpick.applyBuffer(ebuffer, dtime);
        }
    }
    /**
     * getter for the start index, which is the array index for the start of the event
     * with the buffer amount added
     * @return the buffered pick index
     */
    public int getStartIndex() { return startIndex; }
    /**
     * getter for the event onset index
     * @return the array index for the event onset
     */
    public int getPickIndex() { return pickIndex; }
    /**
     * getter for the length of the taper used at the start of the array during filtering.
     * The amount used at the end of the array is controlled by the taper length
     * value in the configuration file.
     * @return the initial taper length
     */
    public double getTaperlengthAtEventOnset() { return taperused; }
}
