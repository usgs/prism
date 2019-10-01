/*******************************************************************************
 * Name: Java class SpectraResources.java
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

import static SmConstants.VFileConstants.NUM_COEF_VALS;
import static SmConstants.VFileConstants.NUM_T_PERIODS;
import static SmConstants.VFileConstants.V3_DAMPING_VALUES;
import static SmConstants.VFileConstants.V3_SAMPLING_RATES;
import SmException.FormatException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This class provides access to the coefficient tables for the different sampling
 * rates and damping values, as well as the file of periods, used during
 * processing of response spectra.
 * @author jmjones
 */
public class SpectraResources {
    private boolean initialized = false;
    private static final double EPSILON = 0.000001;
    private static final String periodFileName = "spectra/T_periods.txt";
    private static final String[] spectraFileNames = 
                                              { "spectra/CoefTable_50_0.txt",
                                                "spectra/CoefTable_50_0.02.txt",
                                                "spectra/CoefTable_50_0.05.txt",
                                                "spectra/CoefTable_50_0.1.txt",
                                                "spectra/CoefTable_50_0.2.txt",
                                                "spectra/CoefTable_100_0.txt",
                                                "spectra/CoefTable_100_0.02.txt",
                                                "spectra/CoefTable_100_0.05.txt",
                                                "spectra/CoefTable_100_0.1.txt",
                                                "spectra/CoefTable_100_0.2.txt",
                                                "spectra/CoefTable_200_0.txt",
                                                "spectra/CoefTable_200_0.02.txt",
                                                "spectra/CoefTable_200_0.05.txt",
                                                "spectra/CoefTable_200_0.1.txt",
                                                "spectra/CoefTable_200_0.2.txt",
                                                "spectra/CoefTable_500_0.txt",
                                                "spectra/CoefTable_500_0.02.txt",
                                                "spectra/CoefTable_500_0.05.txt",
                                                "spectra/CoefTable_500_0.1.txt",
                                                "spectra/CoefTable_500_0.2.txt" };
    private String[] T_periods;
    private String[][] coefs;
    /**
     * The constructor checks to see if the files have been read in already.  If 
     * so, it simply exits.  If not, it reads in the files and stores the contents
     * internally for access through the getter methods.
     * @throws IOException if unable to read in a file
     */
    public SpectraResources() throws IOException {
        //Note that this is not considered thread-safe
        if (!initialized) {
            coefs = new String[spectraFileNames.length][NUM_T_PERIODS];
            //Read in resource tables with periods and coefficients
            T_periods = readInResource(periodFileName);
            for (int i = 0; i < spectraFileNames.length; i++) {
                coefs[i] = readInResource( spectraFileNames[i]);
            }
            initialized = true;
        }
    }
    /**
     * This private method contains the actual read of the file.
     * @param inName file to read in
     * @return data from the file as an array of strings
     * @throws IOException if unable to read a file
     */
    private String[] readInResource( String inName ) throws IOException {
        InputStream stream = SpectraResources.class.getResourceAsStream(inName);
        String nextLine;
        
        ArrayList<String> tempfile = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while ((nextLine = reader.readLine()) != null) {
                tempfile.add(nextLine);
            }
        }
        String[] outarray = new String[tempfile.size()];
        outarray = tempfile.toArray(outarray);
        tempfile.clear();
        return outarray;
    }
    /**
     * Getter for the periods at which spectra are computed.
     * @return the 91 period values
     * @throws FormatException if unable to parse the periods file
     */
    public final double[] getTperiods() throws FormatException {
        int len = T_periods.length;
        double[] tout = new double[len];
        try {
            for (int i = 0; i < len; i++) {
                tout[i] = Double.parseDouble(T_periods[i]);
            }
        } catch (NumberFormatException err) {
            throw new FormatException("Unable to parse spectra periods file");
        }
        return tout;
    }
    /**
     * Getter for the coefficient array identified by sample rate and damping
     * value
     * @param samplerate the sample rate
     * @param damping the damping value
     * @return the array of coefficients
     * @throws FormatException if unable to parse the coefficients file
     */
    public final double[][] getCoefArray( double samplerate, double damping) 
                                                        throws FormatException {
        int index;
        int samp = 0;
        int damp = 0;
        int len = V3_DAMPING_VALUES.length;
        for (int i = 0; i < V3_SAMPLING_RATES.length; i++) {
            if (Math.abs(samplerate-V3_SAMPLING_RATES[i]) < EPSILON) {
                samp = i;
            }
        }
        for (int i = 0; i < V3_DAMPING_VALUES.length; i++) {
            if (Math.abs(damping-V3_DAMPING_VALUES[i]) < EPSILON) {
                damp = i;
            }
        }
        index = (samp*len) + damp;
        return reformatCoefArray(coefs[index],NUM_COEF_VALS);
    }
    /**
     * This private method actually handles the conversion of the array from
     * text to double.
     * @param array the coefficient array as strings
     * @param cols the number of coefficient values
     * @return the array of coefficients as doubles
     * @throws FormatException if unable to parse the coefficients
     */
    private double[][] reformatCoefArray(String[] array, int cols ) 
                                                        throws FormatException {
        double[][] outarray = new double[array.length][cols];
        String[] vals;
        try {
            for (int i = 0; i < array.length; i++) {
                vals = array[i].trim().split("\\s+");
                for(int j = 0; j < cols; j++) {
                    outarray[i][j] = Double.parseDouble(vals[j]);
                }
            }
        } catch (NumberFormatException err) {
            throw new FormatException("Unable to parse spectra coefficients file");
        }
        return outarray;
    }
    /**
     * Getter of the periods file as text, mainly for debug
     * @return the contents of the periods file
     */
    public final String[] getTPeriodsText() {
        return T_periods;
    }
    /**
     * Getter of the coefficient arrays as text, mainly for debug
     * @return the coefficients
     */
    public final String[][] getCoefsText() {
        return coefs;
    }
}
