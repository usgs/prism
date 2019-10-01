/*******************************************************************************
 * Name: Java class ProcessStepsRecorder.java
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

package SmUtilities;

import SmConstants.VFileConstants.BaselineType;
import SmConstants.VFileConstants.CorrectionOrder;
import SmConstants.VFileConstants.CorrectionType;
import SmConstants.VFileConstants.V2DataType;
import java.util.ArrayList;

/**
 * This class records the V2 processing steps so they can be included in the
 * comments section of the product files. In addition to event onset time, 
 * resampling rate (if any) and pre-
 * event mean, this also records the baseline correction functions applied to
 * the trace, identifying them by start time, stop time, and function.  This class
 * is a singleton with a private constructor and is instantiated with
 * ProcessStepsRecorder stepRec = ProcessStepsRecorder.INSTANCE.
 * @author jmjones
 */
public class ProcessStepsRecorder2 {
    private double eventOnsetTime;
    private CorrectionType ctype;
    private boolean needsResampling;
    private boolean needsDecimation;
    private double samplerate;
    private double origrate;
    private boolean gotEventOnset;
    private boolean gotTrimmed;
    private int startTrimCount;
    private int endTrimCount;
    private int spikeCount;
    private ArrayList<blcorrect> blist;
    public final static ProcessStepsRecorder2 INSTANCE = new ProcessStepsRecorder2();
    /**
     * Private constructor for the singleton class.
     */
    private ProcessStepsRecorder2(){
        this.eventOnsetTime = 0.0;
        this.ctype = CorrectionType.AUTO;
        this.needsResampling = false;
        this.needsDecimation = false;
        this.gotTrimmed = false;
        this.gotEventOnset = false;
        this.samplerate = 0.0;
        this.origrate = 0.0;
        this.startTrimCount = 0;
        this.endTrimCount = 0;
        this.spikeCount = 0;
        this.blist = new ArrayList<>();
    }
    /**
     * Adds the event onset time to the recorder.
     * @param inonsettime event onset time in seconds
     */
    public void addEventOnset( double inonsettime ) {
        gotEventOnset = true;
        eventOnsetTime = inonsettime;
    }
    /**
     * Adds a flag for the method of correction, such as manual or automatic
     * @param intype the correction type
     */
    public void addCorrectionType( CorrectionType intype ) {
        ctype = intype;
    }
    /**
     * Sets a flag that the sampling rate has changed and records the new rate
     * @param newsamp updated sampling rate
     */
    public void addResampling( double newsamp ) {
        samplerate = newsamp;
        needsResampling = true;
    }
    /**
     * sets a flag the the array has been decimated after resampling
     * @param origsamp the original sampling rate
     */
    public void addDecimation( double origsamp ) {
        origrate = origsamp;
        needsDecimation = true;
    }
    public void addSpikeCount( int inspikes ) {
        spikeCount = inspikes;
    }
    public void addTrimIndicies( int startCount, int endCount ) {
        gotTrimmed = true;
        startTrimCount = startCount;
        endTrimCount = endCount;
    }
    /** 
     * Adds baseline correction information to the recorder.  Baseline corrections
     * are recorded by identifying the start and stop times for the baseline function
     * as well as the start and stop times of the application interval over which
     * baseline correction was applied.  The function itself is identified by its 
     * polynomial order of 1,2, or 3, or by SPLINE if the spline algorithm was used 
     * in adaptive baseline correction.
     * @param fstart start time for baseline function
     * @param fstop stop time for baseline function
     * @param astart start time of application interval baseline correction was applied to
     * @param astop stop time of application interval for baseline correction was applied to
     * @param v2datatype V2 data to which baseline correction was applied
     * @param btype best fit or adaptive
     * @param intype order of polynomial subtracted, or SPLINE if used
     * @param cstep adaptive baseline correction step
     */
    public void addBaselineStep(double fstart, double fstop, double astart, double astop, 
        V2DataType v2datatype, BaselineType btype, CorrectionOrder intype, int cstep) {
        blcorrect entry = new blcorrect(fstart, fstop, astart, astop, v2datatype,
            btype, intype, cstep);
        blist.add(entry);
    }
    /**
     * Clears the steps in the recorder
     */
    public void clearSteps() {
        eventOnsetTime = 0.0;
        ctype = CorrectionType.AUTO;
        needsResampling = false;
        needsDecimation = false;
        samplerate = 0.0;
        origrate = 0.0;
        gotTrimmed = false;
        gotEventOnset = false;
        startTrimCount = 0;
        endTrimCount = 0;
        spikeCount = 0;
        blist.clear();
    }
    /**
     * Formats the processing steps for inclusion in the comments section of the
     * product files. Tags are added in front of each processing step and units are
     * specified.  The returned list of text can be added directly to the end
     * of the comment section.
     * @return a list of text to append to the comment section
     */
    public ArrayList<String> formatSteps() {
        String timeformat = "%9.4f";
        ArrayList<String> outlist = new ArrayList<>();
        outlist.add(String.format("|<PROCESS> %1$s", ctype.name()));
        if (needsResampling) {
            outlist.add(String.format("|<RESAMPLE> Data resampled to %6.2f samples/sec",samplerate));
        }
        if (gotTrimmed) {
            outlist.add(String.format("|<TRIM> %1$d samp. of beginning, %2$d samp. of end of original channel", startTrimCount, endTrimCount));
        }
        if (gotEventOnset) {
            outlist.add(String.format("|<EONSET> event onset(sec)=%1s",
                                        String.format(timeformat,eventOnsetTime)));
        }
        if (spikeCount > 0) {
            outlist.add(String.format("|<DESPIKE> %1$s spikes removed during V1 processing", spikeCount));
        }
        for (blcorrect blc : blist) {
            String dType = blc.getV2DataType().toString().substring(0,1);
            String blTag = (blc.getBaselineType().equals(BaselineType.ABC)) ?
                dType+"BLABC"+blc.getBaselineStep() :
                dType+"BLC";
            
            outlist.add(String.format("|<%1s>SF:%2s, EF:%3s, SA:%4s, EA:%5s, ORDER:%6s",
                blTag,
                String.format(timeformat,blc.getFunctionStart()),
                String.format(timeformat,blc.getFunctionStop()),
                String.format(timeformat,blc.getAppStart()),
                String.format(timeformat,blc.getAppStop()),
                blc.getOrder().name()));
        }
        if (needsDecimation) {
            outlist.add(String.format("|<DECIMATE> Data decimated to %6.2f samples/sec",origrate));
        }
        return outlist;
    }
    /**
     * This private class defines an object to hold one baseline correction entry.
     * It has fields for the start time, stop time, and the polynomial order.
     * Each object is created with the constructor and elements are accessed
     * through the getters.
     */
    private class blcorrect {
        private final double fstart;
        private final double fstop;
        private final double astart;
        private final double astop;
        private final V2DataType v2datatype;
        private final BaselineType btype;
        private final CorrectionOrder order;
        private final int bstep;
        
        public blcorrect( double fstrt, double fstp, double astrt, double astp,
            V2DataType v2datatype, BaselineType btp, CorrectionOrder od, int bstp) {
            this.fstart = fstrt;
            this.fstop = fstp;
            this.astart = astrt;
            this.astop = astp;
            this.v2datatype = v2datatype;
            this.btype = btp;
            this.order = od;
            this.bstep = bstp;
        }
        
        public double getFunctionStart() {return fstart;}
        public double getFunctionStop() {return fstop;}
        public double getAppStart() {return astart;}
        public double getAppStop() {return astop;}
        public V2DataType getV2DataType() {return v2datatype;}
        public BaselineType getBaselineType() {return btype;}
        public CorrectionOrder getOrder() {return order;}
        public int getBaselineStep() {return bstep;}
    }
}
