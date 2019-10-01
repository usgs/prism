/*******************************************************************************
 * Name: Java class ABCSortPairs.java
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

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class provides a sorting mechanism for pairs of numbers, one of which is
 * the value to sort on and the other is an id.  The pairs are added into the
 * sorter and the id numbers are returned in their sorted order.
 * @author jmjones
 */
public class ABCSortPairs {
    private SortedSet<SortVals> sorter;
    /**
     * Constructor for the sorter sets up the structure and defines the sorting
     * mechanism.
     */
    public ABCSortPairs() {
        sorter = new TreeSet<>(new Comparator<SortVals>()
            {
                @Override
                public int compare(SortVals a, SortVals b) {
                    double vala = a.getReal();
                    double valb = b.getReal();
                    return Double.compare(vala,valb);
                }
            });
    }
    /**
     * This method adds a pair to the sorter
     * @param first the double numeric to be sorted on
     * @param second an id or index accompanying the numeric, this value is
     * returned in the sorted list
     */
    public void addPair( double first, int second) {
        sorter.add(new SortVals(first, second));
    }
    /**
     * This method returns the ids in the order of sorted input values
     * @return list of ids in order of sorted input doubles
     */
    public int[] getSortedVals() {
        int[] outint = new int[0];
        if (!sorter.isEmpty()) {
            outint = new int[sorter.size()];
            int idx = 0;
            for (SortVals each : sorter) {
                outint[idx++] = each.getIndex();
            }
        }
        return outint;
    }
    /**
     * This class defines the sorted pair type.
     */
    public class SortVals {
        private final double realval;
        private final int index;
        /**
         * Default constructor
         * @param rval real value for sorting
         * @param idx index for identifying sorted values
         */
        public SortVals( double rval, int idx) {
            this.realval = rval;
            this.index = idx;
        }
        /**
         * Getter for the numeric for sorting on
         * @return the real value used for sorting
         */
        public double getReal() {
            return realval;
        }
        /**
         * Getter for the index associated with each input numeric
         * @return the index for the sorted pair
         */
        public int getIndex() {
            return index;
        }
    }
}
