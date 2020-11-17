/*******************************************************************************
 * Name: Java class V2Process.java
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

import COSMOSformat.V1Component;
import static SmConstants.VFileConstants.*;
import SmConstants.VFileConstants.EventOnsetType;
import SmConstants.VFileConstants.MagnitudeType;
import SmConstants.VFileConstants.V2DataType;
import SmException.SmException;
import SmUtilities.CSVFileWriter;
import SmUtilities.ConfigReader;
import static SmConstants.SmConfigConstants.*;
import SmUtilities.CommentFormatter;
import SmUtilities.ProcessStepsRecorder2;
import SmUtilities.SmDebugLogger;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
/**
 * This class handles the V2 processing, performing the necessary steps to process
 * the uncorrected acceleration into corrected acceleration, velocity and displacement.
 * It performs 2 QC checks to test the accuracy of the processing.
 * @author jmjones
 */
public class V2Process {
    protected double[] accel;
    protected double ApeakVal;
    protected int ApeakIndex;
    protected double AavgVal;
    protected int acc_unit_code;
    protected String acc_units;
    
    protected double[] velocity;
    protected double VpeakVal;
    protected int VpeakIndex;
    protected double VavgVal;
    protected int vel_unit_code;
    protected String vel_units;
    
    protected double[] displace;
    protected double DpeakVal;
    protected int DpeakIndex;
    protected double DavgVal;
    protected int dis_unit_code;
    protected String dis_units;
    
    protected double initialVel;
    protected double initialDis;
    
    private int inArrayLength;
    protected double[] paddedaccel;
    protected final V1Component inV1;
    protected int data_unit_code;
    protected double dtime;
    protected double samplerate;
    protected double orig_samplerate;
    protected boolean needresampling;
    protected boolean decimate;
    protected boolean usefas;
    protected int sampfactor;
    protected double noRealVal;
    protected double lowcutoff;
    protected double highcutoff;
    protected double lowcutadj;
    protected double highcutadj;
    protected double mmag;
    protected double lmag;
    protected double smag;
    protected double omag;
    protected double magnitude;
    protected double snr;
    protected BaselineType basetype;
    
    private int pickIndex;
    protected int startIndex;
    private double ebuffer;
    private EventOnsetType emethod;
    protected int numroll;  // the filter order is rolloff*2
    protected double taperlength;
    private double preEventMean;
    private int trendRemovalOrder;
    private double calculated_taper;
    private double config_taper;
    private boolean strongMotion;
    protected double smThreshold;
    private final String logtime;
    protected boolean usefft;
    protected double snrvalue;
    protected boolean pgacheck;
    protected double pgathreshold;
    
    private V2Status procStatus;
    private QCcheck qcchecker;
    
    private ArrayList<String> errorlog;
    private boolean writeDebug;
    private boolean writeBaseline;
    private SmDebugLogger elog;
    private String[] logstart;
    private final File V0name;
    private final String channel;
    private final String eventID;
    private double QCvelinitial;
    private double QCvelresidual;
    private double QCdisresidual;
    private int ABCnumparams;
    private int ABCwinrank;
    private int ABCpoly1;
    private int ABCpoly2;
    private int ABCbreak1;
    private int ABCbreak2;
    
    protected double bracketedDuration;
    protected double AriasIntensity;
    protected double HousnerIntensity;
    protected double RMSacceleration;
    protected double durationInterval;
    protected double cumulativeAbsVelocity;
    
    protected ProcessStepsRecorder2 stepRec;
    protected CommentFormatter formatter;
    protected String[] commentUpdates;
    protected String SNCLcode;
    
    /**
     * Constructor gets the necessary header and configuration file parameters
     * and validates them.
     * @param v1rec the V1 component object holding the uncorrected acceleration
     * @param inName the name of the V0 input file
     * @param logtime the processing time
     * @throws SmException if unable to access valid header or configuration file
     * parameters
     */
    public V2Process(final V1Component v1rec, File inName, String logtime) 
                                                            throws SmException {
        double epsilon = 0.000001;
        this.inV1 = v1rec;
        this.lowcutadj = 0.0;
        this.highcutadj = 0.0;
        formatter = new CommentFormatter();
        this.V0name = inName;
        this.channel = inV1.getChannel();
        this.eventID = inV1.getEventID();
        this.logtime = logtime;
        this.basetype = BaselineType.BESTFIT;
        this.needresampling = false;
        this.decimate = false;
        this.sampfactor = 0;
        this.snr = 0.0;
        
        //Get config values to cm/sec2 (acc), cm/sec (vel), cm (dis)
        this.acc_unit_code = CMSQSECN;
        this.vel_unit_code = CMSECN;
        this.dis_unit_code = CMN;
        
        this.acc_units = CMSQSECT;
        this.vel_units = CMSECT;
        this.dis_units = CMT;
        this.pickIndex = 0;
        this.startIndex = 0;
        this.procStatus = V2Status.NOEVENT;
        this.VpeakVal = 0.0;
        this.ApeakVal = 0.0;
        this.DpeakVal = 0.0;
        this.inArrayLength = 0;
        
        this.bracketedDuration = 0.0;
        this.AriasIntensity = 0.0;
        this.HousnerIntensity = 0.0;
        this.RMSacceleration = 0.0;
        this.durationInterval = 0.0;
        this.cumulativeAbsVelocity = 0.0;
        this.initialVel = 0.0;
        this.initialDis = 0.0;
        this.preEventMean = 0.0;
        this.trendRemovalOrder = 0;
        this.calculated_taper = 0.0;
        this.config_taper = 0.0;
        this.strongMotion = false;
        this.smThreshold = 0.0;
        this.commentUpdates = inV1.getComments();
        this.SNCLcode = inV1.getSCNLcode();
        
        this.noRealVal = inV1.getNoRealVal();
        //verify that real header value delta t is defined and valid
        double delta_t = inV1.getRealHeaderValue(DELTA_T);
        if ((Math.abs(delta_t - noRealVal) < epsilon) || (delta_t < 0.0)){
            throw new SmException("Real header #62, delta t, is invalid: " + 
                                                                        delta_t);
        }
        // Determine the delta time and the sample rate, and also capture the
        // initial sample rate since some records will be resampled later.
        boolean match = false;
        dtime = delta_t * MSEC_TO_SEC;    
        samplerate = 1.0 / dtime;
        orig_samplerate = samplerate;
        for (double each : V3_SAMPLING_RATES) {
            if (Math.abs(each - samplerate) < epsilon) {
                match = true;
            }
        }
        if (!match) {
            throw new SmException("Real header #62, delta t value, " + 
                                        delta_t + " is out of expected range");
        }
        //Get the earthquake magnitude from the real header array.
        this.mmag = inV1.getRealHeaderValue(MOMENT_MAGNITUDE);
        this.lmag = inV1.getRealHeaderValue(LOCAL_MAGNITUDE);
        this.smag = inV1.getRealHeaderValue(SURFACE_MAGNITUDE);
        this.omag = inV1.getRealHeaderValue(OTHER_MAGNITUDE);
    }
    /**
     * Controls the flow of automatic V2 processing, starting with event detection,
     * pre-event mean removal, integration to velocity to find trend, 
     * best fit trend derivative removal from acceleration, and the first QC.  If this
     * QC fails, adaptive baseline correction is attempted. If the first QC passed,
     * the baseline-corrected acceleration is filtered, integrated to velocity, and
     * integrated to displacement.  A final QC check is performed
     * in both best fit and adaptive baseline correction and the status of the
     * processing is returned.
     * @return the status of V2 processing, such as GOOD, FAILQC, etc.
     * @throws SmException if unable to perform processing
     * @throws IOException if unable to write out to log files
     */
    public V2Status processV2Data() throws SmException, IOException {
        
        //get parameters from config file and set defaults
        initializeForProcessing();
        
        // if needed, convert array units, resample
        accel = prepareAccelForProcessing();
        double[] accopy = new double[accel.length];
        System.arraycopy( accel, 0, accopy, 0, accel.length);
        writePrePwDdebug(accel.length);
        
        //Find Event Onset
        EventOnsetProcess EventOnset = new EventOnsetProcess(lowcutoff,
                                                    highcutoff, taperlength, 
                                                    numroll, ebuffer);
        EventOnset.findEventOnset(accopy, dtime, emethod);
        boolean successfulEventDetection = checkOnsetStatusAndLog(EventOnset.getPickIndex(),
                                                        EventOnset.getStartIndex(),
                                                        EventOnset.getTaperlengthAtEventOnset());
        if (!successfulEventDetection) {
            return procStatus;   //V2Status.NOEVENT
        }
        // check the SNR of acceleration, using the record from start to event
        // onset for the noise calculation.  If too low, exit.  Also check the
        // acceleration peak value.  If too low, also exit.
        snr  = ArrayOps.calcSignalToNoiseRatio(accopy, EventOnset.getPickIndex());
        boolean passedSNRpeak = checkSNRandPeakVal(accopy);
        if (!passedSNRpeak) {
            return procStatus; // V2Status.FAILINIT
        }
        // Find the filter cutoff thresholds
        boolean passedThresholds = updateThresholds(samplerate, orig_samplerate, 
                                               EventOnset.getPickIndex());
        if (!passedThresholds) {
            return procStatus;  //V2Status.FAILINIT
        }
        
        //Remove trends from acceleration record and integrate to velocity for QC
        TrendRemovalProcess detrend = new TrendRemovalProcess( startIndex, usefft );
        velocity = detrend.removeTrends(accel, dtime);
        preEventMean = detrend.getPreEventMean();
        trendRemovalOrder = detrend.getTrendRemovalOrder();
        logDetrendResults();

        //perform first QA check on velocity copy, check first and last sections of
        //velocity array - should be close to 0.0 with tolerances.  If not,
        //perform adaptive baseline correction.
        qcchecker = new QCcheck();
        qcchecker.validateQCvalues();
        qcchecker.findWindow(lowcutadj, samplerate, startIndex);
        boolean passedQC = qcchecker.qcVelocity(velocity);
        checkFirstQCResultsAndLog(passedQC);
        if ( !passedQC ){
            // Adaptive Baseline Correction
            double[] goodrun = adaptiveCorrection();
            boolean successfulABC = checkABCstatusAndLog(goodrun);
            if (!successfulABC) {
                return procStatus;   //V2Status.NOABC
            }
        } else {
            // Passed first QC, so filter, and integrate
            FilterAndIntegrateProcess filterInt = 
                    new FilterAndIntegrateProcess(lowcutadj,highcutadj,DEFAULT_NUM_ROLL,
                                                        taperlength,startIndex,usefft);
            filterInt.filterAndIntegrate(accel, dtime);
            paddedaccel = filterInt.getPaddedAccel();
            velocity = filterInt.getVelocity();
            displace = filterInt.getDisplacement();
            initialVel = filterInt.getInitialVel();
            initialDis = filterInt.getInitialDis();
            calculated_taper = filterInt.getCalculatedTaper();
            config_taper = filterInt.getConfigTaper();
            LogFilterResults();
            // Second QC Test (also performed in ABC)
            boolean success = qcchecker.qcVelocity(velocity) && 
                                            qcchecker.qcDisplacement(displace);
            procStatus = (success) ? V2Status.GOOD : V2Status.FAILQC;
            QCvelinitial = Math.abs(qcchecker.getInitialVelocity());
            QCvelresidual = Math.abs(qcchecker.getResidualVelocity());
            QCdisresidual = Math.abs(qcchecker.getResidualDisplacement());
        }
        if (procStatus == V2Status.FAILQC) {
            logFailed2ndQCstats();
        }
        //check for resampling done and decimation requested
        if (needresampling && decimate) {
            Decimation dec = new Decimation();
            double[] decaccel = dec.decimateArray(accel, sampfactor);
            double[] decpaddedacc = dec.decimateArray(paddedaccel, sampfactor);
            double[] decvel = dec.decimateArray(velocity, sampfactor);
            double[] decdisp = dec.decimateArray(displace, sampfactor);
            accel = decaccel;
            paddedaccel = decpaddedacc;
            velocity = decvel;
            displace = decdisp;
            initialVel = -999;
            initialDis = -999;
            samplerate = orig_samplerate;
            commentUpdates = formatter.addDecimation(commentUpdates, orig_samplerate);
        }
        //calculate final array params for headers
        ArrayStats statVel = new ArrayStats( velocity );
        VpeakVal = statVel.getPeakVal();
        VpeakIndex = statVel.getPeakValIndex();
        VavgVal = statVel.getMean();

        ArrayStats statDis = new ArrayStats( displace );
        DpeakVal = statDis.getPeakVal();
        DpeakIndex = statDis.getPeakValIndex();
        DavgVal = statDis.getMean();

        ArrayStats statAcc = new ArrayStats( accel );
        ApeakVal = statAcc.getPeakVal();
        ApeakIndex = statAcc.getPeakValIndex();
        AavgVal = statAcc.getMean();

        logFinalStats();
        // strong motion computed parameters done if status is good
        if (procStatus == V2Status.GOOD) {
            ComputedParams cp = new ComputedParams(accel, dtime, smThreshold);
            strongMotion = cp.calculateComputedParameters();
            if (strongMotion) {
                bracketedDuration = cp.getBracketedDuration();
                AriasIntensity = cp.getAriasIntensity();
                HousnerIntensity = cp.getHousnerIntensity();
                RMSacceleration = cp.getRMSacceleration();
                durationInterval = cp.getDurationInterval();
                cumulativeAbsVelocity = cp.getCumulativeAbsVelocity();
                errorlog.add("Strong motion record");
            }
        }
        if ((writeDebug) || (procStatus != V2Status.GOOD)) {
            writeOutErrorDebug();
            makeDebugCSV();
        }
        return procStatus;
    }
    /**
     * Initializes variables for the automatic processing and gets the filter input 
     * variables, event onset type, and logging flags from the configuration file.
     * @throws SmException if unable to extract information from the configuration file
     * or if processing parameters are invalid.
     */
    private void initializeForProcessing() throws SmException {
        this.errorlog = new ArrayList<>();
        this.elog = SmDebugLogger.INSTANCE;
        ConfigReader config = ConfigReader.INSTANCE;
        this.writeDebug = false;
        this.writeBaseline = false;
        this.QCvelinitial = 0.0;
        this.QCvelresidual = 0.0;
        this.QCdisresidual = 0.0;
        this.ABCnumparams = 0;
        this.ABCwinrank = 0;
        this.ABCpoly1 = 0;
        this.ABCpoly2 = 0;
        this.ABCbreak1 = 0;
        this.ABCbreak2 = 0;

        logstart = new String[2];
        logstart[0] = "\n";
        logstart[1] = "Prism Error/Debug Log Entry: " + logtime;
        try {
            String unitcode = config.getConfigValue(DATA_UNITS_CODE);
            this.data_unit_code = (unitcode == null) ? CMSQSECN : Integer.parseInt(unitcode);

            String lowcut = config.getConfigValue(BP_FILTER_CUTOFFLOW);
            this.lowcutoff = (lowcut == null) ? DEFAULT_LOWCUT : Double.parseDouble(lowcut);

            String highcut = config.getConfigValue(BP_FILTER_CUTOFFHIGH);
            this.highcutoff = (highcut == null) ? DEFAULT_HIGHCUT : Double.parseDouble(highcut);

            //The Butterworth filter implementation requires an even number for rolloff
            String filorder = config.getConfigValue(BP_FILTER_ORDER);
            this.numroll = (filorder == null) ? DEFAULT_NUM_ROLL : Integer.parseInt(filorder)/2;

            //The Butterworth filter taper length for the half cosine taper
            //this taperlength is the value in seconds from the configuration file
            String taplen = config.getConfigValue(BP_TAPER_LENGTH);
            this.taperlength = (taplen == null) ? DEFAULT_TAPER_LENGTH : Double.parseDouble(taplen);
            this.taperlength = (this.taperlength < 0.0) ? DEFAULT_TAPER_LENGTH : this.taperlength;  
            
            String pbuf = config.getConfigValue(EVENT_ONSET_BUFFER);
            this.ebuffer = (pbuf == null) ? DEFAULT_EVENT_ONSET_BUFFER : Double.parseDouble(pbuf);
            this.ebuffer = (this.ebuffer < 0.0) ? DEFAULT_EVENT_ONSET_BUFFER : this.ebuffer;
            
            String thold = config.getConfigValue(SM_THRESHOLD);
            this.smThreshold = (thold == null) ? DEFAULT_SM_THRESHOLD : Double.parseDouble(thold);
            this.smThreshold = ((this.smThreshold < 0.0) || (this.smThreshold > 100.0)) ? 
                                                DEFAULT_SM_THRESHOLD : this.smThreshold;

            String snrval = config.getConfigValue(SIGNAL_NOISE_RATIO);
            this.snrvalue = (snrval == null) ? DEFAULT_SNR : Double.parseDouble(snrval);
            
            String pgaflag = config.getConfigValue(PGA_CHECK);
            this.pgacheck = (pgaflag == null) ? false : pgaflag.equalsIgnoreCase(PGA_INPUT_FLAG);
            
            String pgaval = config.getConfigValue(PGA_THRESHOLD);
            this.pgathreshold = (pgaval == null) ? DEFAULT_PGA : Double.parseDouble(pgaval);
            
        } catch (NumberFormatException err) {
            throw new SmException("Error extracting numeric values from configuration file");
        }
        String eventmethod = config.getConfigValue(EVENT_ONSET_METHOD);
        if (eventmethod == null) {
            this.emethod = DEFAULT_EVENT_ONSET_METHOD;
        } else if (eventmethod.equalsIgnoreCase("AIC")) {
            this.emethod = EventOnsetType.AIC;
        } else {
            this.emethod = EventOnsetType.PWD;
        }
        
        String fftint = config.getConfigValue(INTEGRATION_METHOD);
        this.usefft = (fftint == null) ? true : 
                                        fftint.equalsIgnoreCase(FFT_FOR_INTEGRATION);
        
        String filtcorner = config.getConfigValue(FILTER_CORNER_METHOD);
        this.usefas = (filtcorner == null) ? false : 
                                        filtcorner.equalsIgnoreCase(FAS_FOR_CORNERS);
        
        String decval = config.getConfigValue(DECIMATE_AFTER_RESAMPLING);
        this.decimate = (decval == null) ? false : 
                                        decval.equalsIgnoreCase(DECIMATE_OUTPUT);
        
        String debugon = config.getConfigValue(DEBUG_TO_LOG);
        this.writeDebug = (debugon == null) ? false : 
                                        debugon.equalsIgnoreCase(DEBUG_TO_LOG_ON);
        String baselineon = config.getConfigValue(WRITE_BASELINE_FUNCTION);
        this.writeBaseline = (baselineon == null) ? false : 
                                    baselineon.equalsIgnoreCase(BASELINE_WRITE_ON);
        
        qcchecker = new QCcheck();
        if (!qcchecker.validateQCvalues()){
            throw new SmException("Error extracting numeric values from configuration file");
        }
    }
    /**
     * Updates the filter threshold values based on the filter corner table,
     * EQ magnitude or FAS
     */
    private boolean updateThresholds(double samprate, double origrate, 
                                        int eonset) throws SmException, IOException {
        boolean thresholdstat = true;
        FilterCutOffThresholds threshold = new FilterCutOffThresholds();
        MagnitudeType magtype = threshold.SelectMagnitude(mmag, lmag, smag, omag, noRealVal);
        magnitude = threshold.getMagnitude();
        errorlog.add(String.format("Earthquake magnitude is %4.2f and M used is %s",
                                                            magnitude,magtype));
        if (usefas) { // check if fas method requested
            threshold.findFreqThresholds(accel, eonset, samprate, origrate);
        } else {
            // First check for (and get) corners in filter table
            boolean foundintable = threshold.CheckForTableCorners(SNCLcode);
            if (!foundintable) {  //If not in corners table, pick based on EQ and sps
                if (magtype == MagnitudeType.INVALID) {
                    throw new SmException("Earthquake magnitude real header values not valid for filter corner selection.");
                }
                magtype = threshold.SelectMagThresholds(magtype,magnitude,origrate);
                if (magtype == MagnitudeType.LOWSPS) {
                    thresholdstat = false;
                    errorlog.add("Original sample rate too low for given earthquake magnitude");
                    errorlog.add(String.format("  earthquake magnitude is %4.2f and sample rate is %6.2f",magnitude, samprate));
                    procStatus  = V2Status.FAILINIT;
                    errorlog.add("V2process: exit status = " + procStatus);
                    writeOutErrorDebug();
                    makeDebugCSV();                      
                }
            }
        }
        if (thresholdstat) {
            // Update Butterworth filter low and high cutoff thresholds for later
            lowcutadj = threshold.getLowCutOff();
            highcutadj = threshold.getHighCutOff();
            errorlog.add("Acausal bandpass filter:");
            errorlog.add(String.format("  adjusted lowcut: %4.2f and adjusted highcut: %4.2f Hz",
                                                    lowcutadj, highcutadj));
        }
        return thresholdstat;
    }
    /**
     * Gets the raw acceleration from the V1 record and converts units if necessary.
     * Also checks to see if resampling is needed and if so, resamples, updates
     * samples per second, and adds a comment to the comment list.
     * @return the raw acceleration array in units of cm per sq. sec
     * @throws SmException if data units code from the configuration file is invalid
     */
    private double[] prepareAccelForProcessing() throws SmException {
        // Correct units to CMSQSECN, if needed, and make copy of acc array
        double[] accraw = new double[0];
        double[] V1Array = inV1.getDataArray();
        inArrayLength = V1Array.length;

        if (data_unit_code == CMSQSECN) {
            accraw = new double[V1Array.length];
            System.arraycopy( V1Array, 0, accraw, 0, V1Array.length);
        } else if (data_unit_code == GLN) {
            accraw = ArrayOps.convertArrayUnits(V1Array, FROM_G_CONVERSION);
        } else {
            throw new SmException("V1 file units are unsupported for processing");
        }
        //check if the sample rate is below the threshold and if it is, resample
        //to at least 200 sps
        Resampling resamp = new Resampling(SAMPLING_LIMIT);
        needresampling = resamp.needsResampling((int)samplerate);
        if (needresampling) {
            double[] accresamp = resamp.resampleArray(accraw, (int)samplerate);
            samplerate = resamp.getNewSamplingRate();
            dtime = 1.0 / samplerate;
            sampfactor = resamp.getFactor();
            commentUpdates = formatter.addResampling(commentUpdates, samplerate);
            inArrayLength = accresamp.length;
            return accresamp;
        } else {
            return accraw;
        }
    }    
    /**
     * Calls adaptive baseline correction and extracts the results
     * @return the parameter array for the returned solution
     * @throws SmException if unable to get filter coefficients during ABC
     */
    private double[] adaptiveCorrection() throws SmException {
        ABC2 adapt = new ABC2(dtime,velocity,accel, lowcutadj,highcutadj,numroll,
                                                        startIndex,taperlength);
        procStatus = adapt.findFit();
        if (procStatus == V2Status.NOABC) {
            double[] goodrun = new double[0];
            return goodrun;
        }
        basetype = BaselineType.ABC;
        int solution = adapt.getSolution();
        double[] baseline = adapt.getBaselineFunction();
        double[] derivbaseline = adapt.getBaselineDerivativeFunction();
        double[] goodrun = adapt.getSolutionParms(solution);
        calculated_taper = adapt.getCalculatedTaperLength();
        config_taper = adapt.getConfigTaperLength();
        ABCnumparams = adapt.getNumRuns();
        ABCwinrank = (procStatus == V2Status.GOOD) ? (solution + 1) : 0;
        adapt.clearParamsArray();
        accel = adapt.getABCacceleration();
        velocity = adapt.getABCvelocity();
        displace = adapt.getABCdisplacement();
        paddedaccel = adapt.getABCpaddedacceleration();
        initialVel = velocity[0];
        initialDis = displace[0];
        if (writeBaseline) { 
            elog.writeOutArray(baseline, (V0name.getName() + "_" + channel + "_baseline.txt"));
            elog.writeOutArray(derivbaseline, (V0name.getName() + "_" + channel + "_derivbaseline.txt"));
        } 
        return goodrun;
    }
    /**
     * Extracts the statistics from the ABC run and place into the class variables
     * @param goodrun the array containing the ABC parameters
     */
    private boolean checkABCstatusAndLog(double[] goodrun) throws IOException {
        if (procStatus == V2Status.NOABC) {
            errorlog.add("V2process: exit status = " + procStatus);
            writeOutErrorDebug();
            makeDebugCSV();
            return false;
        }
        QCvelinitial = goodrun[2];
        QCvelresidual = goodrun[3];
        QCdisresidual = goodrun[1];
        ABCpoly1 = (int)goodrun[6];
        ABCpoly2 = (int)goodrun[7];
        ABCbreak1 = (int)goodrun[4];
        ABCbreak2 = (int)goodrun[5];
        
        //For each step here, record the order minus 1 and set the data type
        //to acceleration instead of velocity.  The values returned from ABC
        //record the orders of the velocity baseline determination.
        if (ABCpoly1 == 1) {
            commentUpdates = formatter.addBaselineStep(commentUpdates, 0.0, (ABCbreak1*dtime),
                                    0.0, (ABCbreak1*dtime),
                                    V2DataType.ACC, BaselineType.ABC,
                                    CorrectionOrder.MEAN,1);
        } else {
            commentUpdates = formatter.addBaselineStep(commentUpdates, 0.0, (ABCbreak1*dtime),
                                    0.0, (ABCbreak1*dtime),
                                    V2DataType.ACC, BaselineType.ABC,
                                    CorrectionOrder.ORDER1,1);
        }
        commentUpdates = formatter.addBaselineStep(commentUpdates, (ABCbreak1*dtime), (ABCbreak2*dtime),
                                (ABCbreak1*dtime), (ABCbreak2*dtime),
                                V2DataType.ACC, BaselineType.ABC,
                                CorrectionOrder.ORDER2, 2);
        if (ABCpoly2 == 1) {
            commentUpdates = formatter.addBaselineStep(commentUpdates, (ABCbreak2*dtime), (velocity.length*dtime),
                                    (ABCbreak2*dtime), (velocity.length*dtime),
                                    V2DataType.ACC, BaselineType.ABC,
                                    CorrectionOrder.MEAN, 3);
        } else if (ABCpoly2 == 2) {
            commentUpdates = formatter.addBaselineStep(commentUpdates, (ABCbreak2*dtime), (velocity.length*dtime),
                                    (ABCbreak2*dtime), (velocity.length*dtime),
                                    V2DataType.ACC, BaselineType.ABC,
                                    CorrectionOrder.ORDER1, 3);
        } else {
            commentUpdates = formatter.addBaselineStep(commentUpdates, (ABCbreak2*dtime), (velocity.length*dtime),
                                    (ABCbreak2*dtime), (velocity.length*dtime),
                                    V2DataType.ACC, BaselineType.ABC,
                                    CorrectionOrder.ORDER2, 3);
        }
        errorlog.add("    length of ABC params: " + ABCnumparams);
        errorlog.add("    ABC: final status: " + procStatus.name());
        errorlog.add("    ABC: rank: " + ABCwinrank);
        errorlog.add("    ABC: poly1 order: " + (ABCpoly1-1));
        errorlog.add("    ABC: poly2 order: " + (ABCpoly2-1));
        errorlog.add("    ABC: start: " + ABCbreak1 + "  stop: " + ABCbreak2);
        errorlog.add(String.format("    ABC: velstart: %f,  limit %f", 
                        QCvelinitial,qcchecker.getInitVelocityQCval()));
        errorlog.add(String.format("    ABC: velend: %f,  limit %f",QCvelresidual, 
                                    qcchecker.getResVelocityQCval()));
        errorlog.add(String.format("    ABC: disend: %f,  limit %f",QCdisresidual, 
                                        qcchecker.getResDisplaceQCval()));
        errorlog.add(String.format("    ABC: calc. taperlength (zero crossing): %f", 
                                                    (calculated_taper/2.0)));
        errorlog.add(String.format("    ABC: config taperlength (end): %f", 
                                                    (config_taper/2.0)));
        return true;
    }
    /**
     * Writes general debug information out to the error/debug log at the 
     * start of processing
     * @param arlen the length of the acceleration array
     */
    private void writePrePwDdebug(int arlen){
        errorlog.add("Start of V2 processing for " + V0name.toString() + " and channel " + channel);
        errorlog.add(String.format("EventID: %s",eventID));
        errorlog.add(String.format("time per sample in sec %4.3f",dtime));
        errorlog.add(String.format("sample rate (samp/sec): %4.1f",samplerate));
        if (needresampling) {
            errorlog.add(String.format("original sample rate (samp/sec): %4.1f", orig_samplerate));
            errorlog.add(String.format("sampling factor: %d",sampfactor));
        }
        errorlog.add(String.format("length of acceleration array: %d",arlen));
    }
    /**
     * Writes snr debug information out to the error/debug log and checks for too low value
     */
    private boolean checkSNRandPeakVal(double[] array) throws IOException{
        boolean goodSNRpeak;
        goodSNRpeak = (snr >= snrvalue);
        errorlog.add(String.format("acceleration SNR is %4.2f and limit is %4.2f",snr,snrvalue));
        if (pgacheck) {
            ArrayStats stats = new ArrayStats(array);
            double peakval = stats.getPeakVal();
            errorlog.add(String.format("acceleration peak (absolute) value is %4.2f and limit is %4.2f",
                    Math.abs(peakval),Math.abs(pgathreshold)));
            goodSNRpeak = (Math.abs(peakval) >= Math.abs(pgathreshold));
        }
        if (!goodSNRpeak) {
            procStatus  = V2Status.FAILINIT;
            errorlog.add("V2process: exit status = " + procStatus);
            writeOutErrorDebug();
            makeDebugCSV();
        }
        return goodSNRpeak;
    }
    /**
     * Checks the status returned by the EventOnsetProcess and logs critical values,
     * also records the onset with the Process Steps Recorder
     * @param pickInd the event onset array index
     * @param startInd the buffered event onset array index
     * @param tapused the taper length used during event onset filtering
     * @return the success or failure status of event onset detection
     * @throws IOException if unable to write to the debug file
     */
    private boolean checkOnsetStatusAndLog( int pickInd, int startInd, double tapused) throws IOException{
        errorlog.add(String.format("Filtering before event onset detection, taperlength: %8.3f", 
                                                        (tapused/2.0)));
        if (emethod == EventOnsetType.PWD) {
            errorlog.add("Event Detection algorithm: PwD method");
        } else {
            errorlog.add("Event Detection algorithm: modified AIC");
        }
        pickIndex = pickInd;
        startIndex = startInd;
        errorlog.add(String.format("pick index: %d, start index: %d",
                                                        pickIndex,startIndex));
        errorlog.add(String.format("pick time in seconds: %8.3f, buffered time: %8.3f",
                                          (pickIndex*dtime),(startIndex*dtime)));

        if (pickIndex <= 0) { //No pick index detected, so skip all V2 processing
            procStatus  = V2Status.NOEVENT;
            errorlog.add("V2process: exit status = " + procStatus);
            writeOutErrorDebug();
            makeDebugCSV();
            return false;
        } else {
            commentUpdates = formatter.addEventOnset(commentUpdates, startIndex * dtime);
            return true;
        }
    }
    /**
     * Records the results of the Trend Removal Process in the log and with the Process
     * Steps Recorder.
     */
    private void logDetrendResults() {
        if (startIndex > 0) {
            commentUpdates = formatter.addBaselineStep(commentUpdates, 0, startIndex*dtime, 0, inArrayLength*dtime,
                    V2DataType.ACC, BaselineType.BESTFIT, CorrectionOrder.MEAN, 0);
            errorlog.add(String.format("Pre-event mean of %10.6e removed from uncorrected acceleration",preEventMean));
        }
        // trend found in vel was either Order2 or Order1, so derivative of
        // trend (which was removed from acc) will be Order1 or Mean
        if (trendRemovalOrder == 1) { 
            commentUpdates = formatter.addBaselineStep(commentUpdates, 0, inArrayLength*dtime, 0, inArrayLength*dtime,
                    V2DataType.ACC, BaselineType.BESTFIT, CorrectionOrder.ORDER1, 0);            
        } else {
            commentUpdates = formatter.addBaselineStep(commentUpdates, 0, inArrayLength*dtime, 0, inArrayLength*dtime,
                    V2DataType.ACC, BaselineType.BESTFIT, CorrectionOrder.MEAN, 0);
        }
        errorlog.add(String.format("Best fit trend of order %d removed from acceleration", trendRemovalOrder));
        if (writeBaseline) {
            elog.writeOutArray(velocity, V0name.getName() + "_" + channel + "_VelAfterTrendRemovedFromAcc.txt");
            elog.writeOutArray(accel, V0name.getName() + "_" + channel + "_BestFitTrendRemovedAcc.txt");                
        }
    }
    /**
     * Records the results of the first QC test in the error log
     * @param passedQC pass or fail status of the QC test
     */
    private void checkFirstQCResultsAndLog(boolean passedQC){
        if (!passedQC) {
            errorlog.add("Velocity QC1 failed:");
            errorlog.add(String.format("   initial velocity: %f,  limit %f",
                                        Math.abs(qcchecker.getInitialVelocity()),
                                              qcchecker.getInitVelocityQCval()));
            errorlog.add(String.format("   final velocity: %f,  limit %f",
                                  Math.abs(qcchecker.getResidualVelocity()), 
                                            qcchecker.getResVelocityQCval()));
            errorlog.add("Adaptive baseline correction beginning");
        }
    }
     /**
     * Writes out information to the error log after filtering
     */
    private void LogFilterResults() {
        if (writeBaseline) {
           elog.writeOutArray(accel, V0name.getName() + "_" + channel + "_accelAfterFiltering.txt");
           elog.writeOutArray(paddedaccel, V0name.getName() + "_" + channel + "_paddedAccelAfterFiltering.txt");
        }
        if (writeDebug) {
            errorlog.add("Acceleration integrated to velocity integrated to displacement");
        }
    }
    /**
     * Logs the results of the 2nd QC tests in the error log
     */
    private void logFailed2ndQCstats(){
        errorlog.add("Final QC failed - V2 processing unsuccessful:");
        errorlog.add(String.format("   initial velocity: %f, limit %f",
                                    Math.abs(qcchecker.getInitialVelocity()),
                                          qcchecker.getInitVelocityQCval()));
        errorlog.add(String.format("   final velocity: %f, limit %f",
                              Math.abs(qcchecker.getResidualVelocity()), 
                                        qcchecker.getResVelocityQCval()));
        errorlog.add(String.format("   final displacement,: %f, limit %f",
                              Math.abs(qcchecker.getResidualDisplacement()),
                                        qcchecker.getResDisplaceQCval()));
    }
    /**
     * Logs the final V2process status in the error log
     */
    private void logFinalStats() {
        errorlog.add("V2process: exit status = " + procStatus);
        errorlog.add(String.format("Peak Velocity: %f",VpeakVal));
        if (needresampling && decimate) {
            errorlog.add("Final arrays decimated to original sampling rate");
        }
    }
    /**
     * Writes out the error log to file
     * @throws IOException if unable to write to file
     */
    private void writeOutErrorDebug() throws IOException {
        elog.writeToLog(logstart, LogType.DEBUG);
        String[] errorout = new String[errorlog.size()];
        errorout = errorlog.toArray(errorout);
        elog.writeToLog(errorout, LogType.DEBUG);
        errorlog.clear();
    }
   /**
     * Makes the CSV file contents for debug and writes to file
     * @throws IOException if unable to write to file
     */
    private void makeDebugCSV() throws IOException {
        String[] headerline = {"EVENT","MAG","NAME","CHANNEL",
            "ARRAY LENGTH","DELTAT ORIG(SEC)","DELTAT PROC(SEC)","PICK INDEX",
            "PICK TIME(SEC)","SNR","EXIT STATUS","PEAK VEL(CM/SEC)","START TAPER (SEC)","END TAPER (SEC)",
            "PRE-EVENT MEAN (CM/SEC/SEC)","FILTER LO-CUT (HZ)","FILTER HI-CUT (HZ)",
            "1ST BASELINE CORRECTION ORDER",
            "PEAK ACC(G)","STRONG MOTION","VEL INITIAL(CM/SEC)",
            "VEL RESIDUAL(CM/SEC)","DIS RESIDUAL(CM)","BASELINE CORRECTION",
            "ABC POLY1","ABC POLY2","ABC 1ST BREAK","ABC 2ND BREAK",
            "ABC PARM LENGTH","ABC WIN RANK"};
        ArrayList<String> data = new ArrayList<>();
        data.add(eventID);                                  //event id
        data.add(String.format("%4.2f",magnitude));         //event magnitude
        data.add(V0name.getName());                        //record file name
        data.add(channel);                                  //channel number
        data.add(String.format("%d",inArrayLength));      //length of array
        data.add(String.format("%5.3f",(1.0/orig_samplerate))); //original sample interval
        data.add(String.format("%5.3f",dtime));             //processing sample interval
        data.add(String.format("%d",startIndex));            //event onset index
        data.add(String.format("%8.3f", startIndex*dtime));  //event onset time
        data.add(String.format("%8.3f",snr));               //signal-to-noise ratio
        data.add(procStatus.name());                        //final processing status
        data.add(String.format("%8.5f",VpeakVal));          //peak velocity
        data.add(String.format("%8.3f",(calculated_taper/2.0))); //filter taperlength
        data.add(String.format("%8.3f",(config_taper/2.0))); //filter taperlength
        data.add(String.format("%8.6f",preEventMean));      //preevent mean removed
        data.add(String.format("%8.3f",lowcutadj));         //low filter cutoff freq
        data.add(String.format("%8.3f",highcutadj));         //high filter cutoff freq
        data.add(String.format("%d",trendRemovalOrder));  //order of 1st baseline correction
        data.add(String.format("%5.4f",ApeakVal*TO_G_CONVERSION)); //peak acc in g
        if (strongMotion) {
            data.add("YES");
        } else if (procStatus != V2Status.GOOD) {
            data.add("--");
        } else {
            data.add("NO");
        }
        if ((procStatus == V2Status.GOOD) || (procStatus == V2Status.FAILQC)) {
            data.add(String.format("%8.6f",QCvelinitial));     //QC value initial velocity
            data.add(String.format("%8.6f",QCvelresidual));    //QC value residual velocity
            data.add(String.format("%8.6f",QCdisresidual));    //QC value residual displace
            data.add(basetype.name());
            if (basetype == BaselineType.ABC) {
                data.add(String.format("%d",(ABCpoly1-1)));
                data.add(String.format("%d",(ABCpoly2-1)));
                data.add(String.format("%d",ABCbreak1));
                data.add(String.format("%d",ABCbreak2));
                data.add(String.format("%d",ABCnumparams));
                data.add(String.format("%d",ABCwinrank));
            }
        }
        CSVFileWriter csv = new CSVFileWriter(elog.getLogFolder());
        csv.writeToCSV(data, headerline, "ParameterLog.csv", logtime);
        data.clear();
    }
    /**
     * Getter for the peak value for the particular data type
     * @param dType the data type (ACC, VEL, DIS)
     * @return the peak value
     */
    public double getPeakVal(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.ApeakVal;
        } else if (dType == V2DataType.VEL) {
            return this.VpeakVal;
        } else {
            return this.DpeakVal;
        }
    }
    /**
     * Getter for the peak index for the particular data type
     * @param dType the data type (ACC, VEL, DIS)
     * @return the peak index
     */
    public int getPeakIndex(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.ApeakIndex;
        } else if (dType == V2DataType.VEL) {
            return this.VpeakIndex;
        } else {
            return this.DpeakIndex;
        }
    }
    /**
     * Getter for the average value for the particular data type
     * @param dType the data type (ACC, VEL, DIS)
     * @return the average value
     */
    public double getAvgVal(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.AavgVal;
        } else if (dType == V2DataType.VEL) {
            return this.VavgVal;
        } else {
            return this.DavgVal;
        }
    }
    /**
     * Getter for the array for the particular data type
     * @param dType the data type (ACC, VEL, DIS)
     * @return the array reference
     */
    public double[] getV2Array(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.accel;
        } else if (dType == V2DataType.VEL) {
            return this.velocity;
        } else {
            return this.displace;
        }
    }
    /**
     * Getter for the array length for the particular data type
     * @param dType the data type (ACC, VEL, DIS)
     * @return the array length
     */
    public int getV2ArrayLength(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.accel.length;
        } else if (dType == V2DataType.VEL) {
            return this.velocity.length;
        } else {
            return this.displace.length;
        }
    }
    /**
     * Getter for the data unit code for the particular data type
     * @param dType the data type (ACC, VEL, DIS)
     * @return the data unit code
     */
    public int getDataUnitCode(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.acc_unit_code;
        } else if (dType == V2DataType.VEL) {
            return this.vel_unit_code;
        } else {
            return this.dis_unit_code;
        }
    }
    /**
     * Getter for the data units for the particular data type
     * @param dType the data type (ACC, VEL, DIS)
     * @return the data units text name
     */
    public String getDataUnits(V2DataType dType) {
        if (dType == V2DataType.ACC) {
            return this.acc_units;
        } else if (dType == V2DataType.VEL) {
            return this.vel_units;
        } else {
            return this.dis_units;
        }
    }
    /**
     * Getter for the flag indicating that the input data array was resampled
     * @return true if the array was resampled, otherwise false
     */
    public boolean getResampleIndicator() { return this.needresampling; }
    /**
     * Getter for the flag indicating that the input data array was decimated
     * after resampling
     * @return true if the array was decimated, otherwise false
     */
    public boolean getDecimateIndicator() { return this.decimate; }
    /**
     * Getter for the sample rate, which is only updated from the value in the
     * V1 header if the resample indicator is set to true
     * @return the sample rate
     */
    public double getSampleRate() { return this.samplerate; }
    /**
     * Getter for the adjusted filter low cutoff value
     * @return the adjusted low filter cutoff frequency
     */
    public double getLowCut() {return this.lowcutadj;}
    /**
     * Getter for the adjusted filter high cutoff value
     * @return the adjusted high filter cutoff frequency
     */
    public double getHighCut() {return this.highcutadj;}
    /**
     * Getter for the final QC status of the V2 processing
     * @return the QC exit status
     */
    public V2Status getQCStatus() {return this.procStatus;}
    /**
     * Getter for the event onset index
     * @return the event onset index
     */
    public int getPickIndex() {return this.pickIndex;}
    /**
     * Getter for the start index, which is the event index modified by the buffer value
     * @return the start index
     */
    public int getStartIndex() {return this.startIndex;}
    /**
     * Setter for the start index
     * @param instartindex the event start index
     */
    public void setStartIndex(int instartindex) {this.startIndex = instartindex;}
    /**
     * Getter for the Bracketed Duration for the real header
     * @return the bracketed duration
     */
    public double getBracketedDuration() {return bracketedDuration;}
    /**
     * Getter for the Arias Intensity value for the real header
     * @return the Arias Intensity
     */
    public double getAriasIntensity() {return AriasIntensity;}
    /**
     * Getter for the Strong Motion indicator
     * @return true if current record exceeded the strong motion threshold
     * set in the configuration file, false if not
     */
    public boolean getStrongMotion() {return strongMotion;}
    /**
     * Getter for the duration interval for the real header
     * @return the duration interval
     */
    public double getDurationInterval() {return durationInterval;}
    /**
     * Getter for the Cumulative absolute velocity for the real header
     * @return the cumulative absolute velociy
     */
    public double getCumulativeAbsVelocity() {return cumulativeAbsVelocity;}
    /**
     * Getter for the RMS acceleration for the real header
     * @return the RMS acceleration
     */
    public double getRMSacceleration() {return RMSacceleration;}
    /**
     * Getter for the initial velocity value for the real header
     * @return the initial velocity value
     */
    public double getInitialVelocity() {return initialVel;}
    /**
     * Getter for the initial displacement value for the real header
     * @return the initial displacement value
     */
    public double getInitialDisplace() {return initialDis;}
    /**
     * Getter for the padded acceleration array for V3 processing
     * @return reference to the padded acceleration array
     */
    public double[] getPaddedAccel() {return paddedaccel;}
    public String[] getUpdatedComments() {return commentUpdates; }
    public boolean getFFTflag() {return usefft; }
}
