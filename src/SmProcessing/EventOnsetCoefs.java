/*******************************************************************************
 * Name: Java class EventOnsetCoefs.java
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

import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the coefficients for the PwD event onset detection calculations.
 * The coefficients for the Ae and AeB arrays have been pre-calculated in matlab
 * and are stored here and accessed through this class according to the sample 
 * interval.
 * @author jmjones
 */
public class EventOnsetCoefs {

    private static final int DELTAT_TO_INT = 10000;
    
    private final Map<Integer, double[]> AeB;
    private final Map<Integer, double[]> Ae;
    
    private final double[] aeb_01 = { 0.00000, 
                                     -0.00004};
    
    private final double[] aeb_02 = {0.0000, 
                                     0.0000};
    
    private final double[] aeb_005 = {0.000, 
                                      0.00018};
    
    private final double[] aeb_002 = {0.0000, 
                                      0.00079};
    
    private final double[] ae_01 = {-0.00932, 
                                    -0.00004, 
                                     17.22045, 
                                     0.02357};
    
    private final double[] ae_02 = {-0.00066, 
                                    -0.00000, 
                                     0.24536, 
                                    -0.00020};
    
    private final double[] ae_005 = {-0.05590, 
                                      0.00018, 
                                    -70.09431, 
                                     -0.18977};
    
    private final double[] ae_002 = {0.55004, 
                                     0.00079, 
                                    -311.99721, 
                                    -0.04583};
/**
 * The constructor sets up the data structure for access to the values through
 * the getter methods.
 */    
    public EventOnsetCoefs() {
        AeB = new HashMap<>();
        AeB.put(100, aeb_01);
        AeB.put(200, aeb_02);
        AeB.put(50, aeb_005);
        AeB.put(20, aeb_002);
        
        Ae = new HashMap<>();
        Ae.put(100, ae_01);
        Ae.put(200, ae_02);
        Ae.put(50, ae_005);
        Ae.put(20, ae_002);
    }
    /**
     * Get the array of AeB coefficients that are associated with the input sample interval.
     * @param deltaT the interval in seconds between samples for this record
     * @return the coefficients for the requested array
     */
    public double[] getAeBCoefs(double deltaT) {
        int key = (int)(deltaT * DELTAT_TO_INT);
        return AeB.get(key);
    }
    /**
     * Get the array of Ae coefficients that are associated with the input sample interval.
     * @param deltaT the interval in seconds between samples for this record
     * @return the coefficients for the requested array
     */
    public double[] getAeCoefs(double deltaT) {
        int key = (int)(deltaT * DELTAT_TO_INT);
        return Ae.get(key);
    }
}
