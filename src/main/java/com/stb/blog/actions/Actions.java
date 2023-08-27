package com.stb.blog.actions;

import java.util.Date;
import java.util.TimeZone;

public class Actions {
    public static Date getDateInGMT(Date date){
        TimeZone tz = TimeZone.getDefault();
        Date returnDate = new Date(date.getTime() - tz.getRawOffset());
        if(tz.inDaylightTime(returnDate)){
            Date daylightSavingsDate = new Date(returnDate.getTime()-tz.getDSTSavings());
            if(tz.inDaylightTime(daylightSavingsDate)){
                returnDate = daylightSavingsDate;
            }
        }
        return  returnDate;
    }
}
