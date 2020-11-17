/*******************************************************************************
 * Name: Java class CommentFormatter.java
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

import SmConstants.VFileConstants;
import SmConstants.VFileConstants.CorrectionType;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Handles the updates to the Cosmos comment section as new processing steps are
 * added.  The first line of the comments contains the number of remaining lines.
 * Each time a comment is added this count is increased and the new comment is
 * appended to the end of the input comment section.  This class is a replacement
 * to the ProcessStepsRecorder class for the processing engine.
 * @author jmjones
 */
public class CommentFormatter {
    private final String timeformat = "%9.4f";
    private final ArrayList<String> outlist;
    
    public CommentFormatter() {
        this.outlist = new ArrayList<>();
    }
    // Handles the addition of the new comments to the end of the input comment
    // list and updates the comment count in the first line of the new comment block.
    private String[] updateComments(String[] oldcomments) {
        ArrayList<String> text = new ArrayList<>(Arrays.asList(oldcomments));
        text.addAll(outlist);
        StringBuilder sb = new StringBuilder();
        String start = text.get(0);
        sb.append(String.format("%4d",(text.size()-1)))
                .append(start.substring(4, start.length()));
        text.set(0,sb.toString());
        String[] newcomments = text.toArray(new String[text.size()]);
        outlist.clear();
        text.clear();
        sb.setLength(0);
        return newcomments;
    }
    /**
     * Adds the event onset comment to the end of the comment list
     * @param incomments current comment list
     * @param inonsettime event onset time in seconds
     * @return updated comment list
     */
    public String[] addEventOnset( String[] incomments, double inonsettime ) {
        outlist.add(String.format("|<EONSET> event onset(sec)=%1s",
                                        String.format(timeformat,inonsettime)));
        return updateComments(incomments);
    }
    /**
     * Adds the processing type (auto or manual) to the end of the comment list
     * @param incomments current comment list
     * @param intype processing type (auto or manual)
     * @param version the version number of the prism engine or review tool
     * @return updated comment list
     */
    public String[] addCorrectionType( String[] incomments, VFileConstants.CorrectionType intype, String version ) {
        String outstring = "";
        if (intype.equals(CorrectionType.AUTO)) {
            outstring = String.format("|<PROCESS> Automatically processed using PRISM version %1$s",version);
        } else {
            outstring = String.format("|<PROCESS> Manually processed using PRISM version %1$s",version);
        }
        outlist.add(outstring);
        return updateComments(incomments);
    }
    /**
     * Adds the resampled sample rate to the comment list
     * @param incomments current comment list
     * @param newsamp resampled rate in samples / second
     * @return updated comment list
     */
    public String[] addResampling( String[] incomments, double newsamp ) {
        outlist.add(String.format("|<RESAMPLE> Data resampled to %6.2f samples/sec",newsamp));
        return updateComments(incomments);
    }
    /**
     * Adds the decimated (original)sample rate to the comment list
     * @param incomments current comment list
     * @param origsamp decimated rate in samples / second
     * @return updated comment list
     */
    public String[] addDecimation( String[] incomments, double origsamp ) {
        outlist.add(String.format("|<DECIMATE> Data decimated to %6.2f samples/sec",origsamp));
        return updateComments(incomments);
    }
    /**
     * Adds the spike count found during despiking to the comment list
     * @param incomments current comment list  
     * @param inspikes number of spikes detected
     * @return updated comment list
     */
    public String[] addSpikeCount( String[] incomments, int inspikes ) {
        outlist.add(String.format("|<DESPIKE> %1$s spike(s) removed during V1 processing", inspikes));
        return updateComments(incomments);
    }
    /**
     * Adds the trim indicies to the comment list
     * @param incomments current comment list
     * @param startTrimCount number of samples trimmed from start of array
     * @param endTrimCount number of samples trimmed from end of the array
     * @return updated comment list
     */
    public String[] addTrimIndicies( String[] incomments, int startTrimCount, int endTrimCount ) {
        outlist.add(String.format("|<TRIM> %1$d samp. of beginning, %2$d samp. of end of original channel", 
                                                startTrimCount, endTrimCount));
        return updateComments(incomments);
    }
    /**
     * Adds the baseline correction step to the comment list
     * @param incomments current comment list
     * @param fstart start time of array section used to determine the baseline correction function
     * @param fstop end time of array section used to determine the baseline correction function
     * @param astart start time of array section that correction function is applied to
     * @param astop end time of array section that correction function is applied to
     * @param v2datatype data array that the correction was applied to (acceleration or velocity)
     * @param btype baseline correction process of best fit or adaptive baseline correction
     * @param intype type of correction (mean (y = a), order 1 (y = ax+b), order 2, order 3, spline)
     * @param cstep for ABC processing, this identifies the segment of the array that got the specified correction,
     * with 1 = beginning segment, 2 = middle segment, 3 = end segment
     * @return updated comment list
     */
    public String[] addBaselineStep(String[] incomments, double fstart, double fstop, double astart, double astop, 
                                    VFileConstants.V2DataType v2datatype, 
                                    VFileConstants.BaselineType btype, 
                                    VFileConstants.CorrectionOrder intype, int cstep) {
        String dType = v2datatype.toString().substring(0,1);
        String blTag = (btype.equals(VFileConstants.BaselineType.ABC)) ?
            dType+"BLABC"+cstep :
            dType+"BLC";

        outlist.add(String.format("|<%1s>SF:%2s, EF:%3s, SA:%4s, EA:%5s, ORDER:%6s",
                                blTag,
                                String.format(timeformat,fstart),
                                String.format(timeformat,fstop),
                                String.format(timeformat,astart),
                                String.format(timeformat,astop),
                                intype.name()));
        return updateComments(incomments);
    }
}
