/*******************************************************************************
 * Name: Java class RawTraceConversion.java
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
 * This static factory class is used during processing of raw trace counts to
 * physical values.  No object instantiation is used, just call the factory methods
 * with class name and method name, such as RawTraceConversion.countToG().
 * Use this class to convert counts to either units of g or cm/sq.sec.
 * @author jmjones
 */
public class RawTraceConversion {
    private static final double MICRO_TO_VOLT = 1.0e-6;
    /**
     * Private constructor for this static factory class
     */
    private RawTraceConversion(){
    }
    /**
     * Calculates the value to use to convert raw trace counts to physical units
     * of g based on input least significant bit and sensor sensitivity
     * @param inlsb least significant bit value from real header #22
     * @param inSensitivity sensor sensitivity value from real header #42
     * @return conversion value
     */
    public static double countToG(final double inlsb, final double inSensitivity) {
        double result = ( inlsb * MICRO_TO_VOLT) / inSensitivity;
        return result;        
    }
    /**
     * Calculates the value to use to convert raw trace counts to physical units
     * of cm/sq.sec based on input least significant bit and sensor sensitivity
     * @param inlsb least significant bit value from real header #22
     * @param inSensitivity sensor sensitivity value from real header #42
     * @param conversion constant to convert from g to cm/sq.sec (980.665)
     * @return conversion value
     */
    public static double countToCMS(final double inlsb, final double inSensitivity,
                                        double conversion) {
        //sensor calculation of volts per count and cm per sq. sec per volt
        //countToCMS units are cm per sq. sec per count
        //This is multiplied by each count to get the sensor value in cm per sq. sec
        double result = (( inlsb * MICRO_TO_VOLT) / inSensitivity) * conversion;
        return result;
    }
    
}
