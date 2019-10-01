/*******************************************************************************
 * Name: Java class SmTimeFormatter.java
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

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class provides the current date and time in GMT time, according to the
 * Gregorian calendar.  Date format is YYYY-MM-DD where January = 01, Feb = 02, etc.
 * and DD is the day of the month.  The time format is HH:MM:SS where the
 * hour is hour-of-day (24-hour clock).
 * @author jmjones
 */
public class SmTimeFormatter {
    String GMT_ZONE = "GMT";
    String UTC_ZONE = "UTC";
    ZonedDateTime datetime;
    int zyear, zjulday, zmonth, zday, zhour, zmin, zsec, znano;
    double fullsec;
/**
 * Default constructor
 */
    public SmTimeFormatter() {
    }
    public SmTimeFormatter(ZonedDateTime newdatetime) {
        datetime = newdatetime;
        zyear = datetime.getYear();
        zjulday = datetime.getDayOfYear();
        zmonth = datetime.getMonthValue();
        zday = datetime.getDayOfMonth();
        zhour = datetime.getHour();
        zmin = datetime.getMinute();
        zsec = datetime.getSecond();
        znano = datetime.getNano();
        fullsec = (double)zsec + (double)znano/(1.0e9);
    }
/**
 * This method returns the formatted date and time, with "GMT" appended
 * @return Text representation of date and time
 */
    public String getGMTdateTime() {
        String result;
        TimeZone zone = TimeZone.getTimeZone("GMT");
        Calendar cal = new GregorianCalendar(zone);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);
        result = String.format("%1$4d-%2$02d-%3$02d %4$02d:%5$02d:%6$02d %7$3s",
                                year,month,day,hour,min,sec,GMT_ZONE);
        return result;
    }
    public String getUTCdateTime() {
        String result;
        result = String.format("%1$4d/%2$02d/%3$02d %4$02d:%5$02d:%6$5.3f %7$3s",
                                zyear,zmonth,zday,zhour,zmin,fullsec,UTC_ZONE);        
        return result;
    }
    public int getUTCyear(){return zyear;}
    public int getUTCjulday() {return zjulday;}
    public int getUTCmonth() {return zmonth;}
    public int getUTCday() {return zday;}
    public int getUTChour() {return zhour;}
    public int getUTCminute() {return zmin;}
    public double getUTCsecond() {return fullsec;}
    public int getUTCjustSecond() {return zsec;}
    public int getUTCjustNano() {return znano;}
}
