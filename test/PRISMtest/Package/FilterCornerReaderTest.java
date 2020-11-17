/*******************************************************************************
 * Name: Java class FilterCornerReaderTest.java
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
package PRISMtest.Package;

import SmUtilities.FilterCornerReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmjones
 */
public class FilterCornerReaderTest {
    FilterCornerReader corners = FilterCornerReader.INSTANCE;
    static String cornersfile = "/PRISMtest/Data/corners.txt";
    static String filename;
    static double epsilon = 0.00001;
    
    public FilterCornerReaderTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws URISyntaxException {
        URL url = ArrayOpsTest.class.getResource( cornersfile );
        if (url != null) {
            File name = new File(url.toURI());
            filename = name.toString();
        } else {
            filename = "";
        }
    }
    @Test
    public void testSetGetCorners() {
        double[] test1 = {0.25,3.75};
        double[] test2 = {1.33,54.6};
        double[] test3 = {-1.0,3.2};
        double[] test4 = {2.4,-11};
        double[] test5 = {3.5,3.5};
        double[] result;
        boolean check;
        double[] empty = {};
        check = corners.setCornerValues("abc.def", test1);
        org.junit.Assert.assertEquals(true,check);
        check = corners.setCornerValues("TR#..567", test2);
        org.junit.Assert.assertEquals(true,check);
        check = corners.setCornerValues("badlow", test3);
        org.junit.Assert.assertEquals(false,check);
        check = corners.setCornerValues("badhigh", test4);
        org.junit.Assert.assertEquals(false,check);
        check = corners.setCornerValues("badsame", test5);
        org.junit.Assert.assertEquals(false,check);
        check = corners.setCornerValues(null, test1);
        org.junit.Assert.assertEquals(false,check);
        check = corners.setCornerValues("", test1);
        org.junit.Assert.assertEquals(false,check);
        
        result = corners.getCornerValues("abc.def");
        org.junit.Assert.assertArrayEquals(test1, result, epsilon);
        result = corners.getCornerValues("TR#..567");
        org.junit.Assert.assertArrayEquals(test2, result, epsilon);
        result = corners.getCornerValues("badsame");
        org.junit.Assert.assertArrayEquals(empty, result, epsilon);
    }
    @Test
    public void testStationFileRead() throws IOException {
        String testkey = "COLA.HNZ.IU.20";
        double[] check = {0.02,40.00};
        corners.clear();
        corners.loadFilterCorners(filename);
        org.junit.Assert.assertEquals(6,corners.size());
        org.junit.Assert.assertEquals(true,corners.containsKey(testkey));
        org.junit.Assert.assertArrayEquals(check,corners.getCornerValues(testkey), epsilon);
    }
}
