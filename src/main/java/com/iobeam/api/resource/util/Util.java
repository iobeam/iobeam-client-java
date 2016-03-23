package com.iobeam.api.resource.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
                final Calendar c = javax.xml.bind.DatatypeConverter.parseDateTime(dateStr);
                created = c.getTime();
            } catch (IllegalArgumentException e2) {
                throw e;
            }
        }
        return created;
    }
}
