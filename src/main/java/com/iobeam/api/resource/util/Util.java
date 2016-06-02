package com.iobeam.api.resource.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Resource utilities.
 */
public class Util {

    public static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    public static Date parseToDate(String dateStr) throws ParseException {
        Date created;
        try {
            created = Util.DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            try {
                final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
                String newDateStr = dateStr.replace("Z", "+00:00");
                newDateStr = newDateStr.substring(0, 19) + newDateStr.substring(19).replace(":", "");
                created = ISO_FORMAT.parse(newDateStr);
            } catch (Exception e2) {
                throw e;
            }
        }
        return created;
    }
}
