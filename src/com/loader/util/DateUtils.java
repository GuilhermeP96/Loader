package com.loader.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    /**
     * Returns the current timestamp formatted as a string.
     * @return A string representing the current timestamp.
     */
    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    // You can add more date-related utility methods here if needed.
}
