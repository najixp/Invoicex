package com.bytecodr.invoicing.helper;

import org.json.JSONObject;

import java.math.RoundingMode;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.util.Date;

public class helper_number
{
    //Returns the month name for the month number
    public static String getMonthName(int month) {
        return new DateFormatSymbols().getShortMonths()[month];
    }

    //Rounds double number to 2 decimal
    public static String round(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

        return decimalFormat.format(value);
    }

    //Convert unix timestamp to date
    public static Date unixToDate(int timeStamp)
    {
        return new Date(timeStamp * 1000L);
    }

    public static double optDouble(JSONObject json, String key) {
        // http://code.google.com/p/android/issues/detail?id=13830
        if (json.isNull(key))
            return 0;
        else
            return json.optDouble(key);
    }
}
