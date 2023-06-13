/*
 * As this file uses some logic from UNF library, it inherits its license:
 *
 * Dataverse Network - A web application to distribute, share and
 * analyze quantitative data.
 * Copyright (C) 2008
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 *  along with this program; if not, see http://www.gnu.org/licenses
 * or write to the Free Software Foundation,Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package edu.harvard.iq.dataverse.ingest;

import org.apache.commons.lang3.StringUtils;
import org.dataverse.unf.UnfClass;
import org.dataverse.unf.UnfCons;
import org.dataverse.unf.UnfDateFormatter;
import org.dataverse.unf.UnfDigest;
import org.dataverse.unf.UnfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

public class OptimizedUNFUtil {
    private static final Logger logger = LoggerFactory.getLogger(OptimizedUNFUtil.class);

    private static String DATE_FORMAT = "yyyy-MM-dd";
    private static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static String calculateUNF(Number[] vector) throws IOException, UnfException {
        if (vector == null) {
            return null;
        }
        UnfDigest.setTrnps(false);
        UnfClass signature = null;
        if (UnfDigest.getUnfObj()) {
            signature = new UnfClass(UnfCons.DEF_CDGTS, UnfCons.DEF_NDGTS, UnfCons.DEF_HSZ);
        }
        String unf = UnfDigest.unfV(vector, UnfCons.DEF_NDGTS, signature);
        new UnfDigest().getFingerprint().clear();
        return unf;
    }

    public static String calculateUNF(final String[] vector)
            throws IOException, UnfException {
        if (vector == null) {
            return null;
        }
        if (vector[0] != null && vector[0].startsWith("UNF:") && vector[0].split(":").length >= 3) {
            return UnfDigest.addUNFs(vector);
        }
        UnfDigest.setTrnps(false);
        UnfClass signature = null;
        if (UnfDigest.getUnfObj()) {
            signature = new UnfClass(UnfCons.DEF_CDGTS, UnfCons.DEF_NDGTS, UnfCons.DEF_HSZ);
        }
        String unf = UnfDigest.unfV(vector, UnfCons.DEF_CDGTS, signature);
        new UnfDigest().getFingerprint().clear();
        return unf;
    }

    /**
     * WARNING: this method alters contents of input vector!
     */
    public static String calculateDateUNF(String[] vector, String savedDateFormat)
            throws  IOException, UnfException {
        if (vector == null) {
            return null;
        }
        if (vector[0] != null && vector[0].startsWith("UNF:") && vector[0].split(":").length >= 3) {
            return UnfDigest.addUNFs(vector);
        }

        UnfDigest.setTrnps(false);
        String dateFormat = StringUtils.isNotEmpty(savedDateFormat) ? savedDateFormat : DATE_FORMAT;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        UnfDateFormatter unfDateFormatter = new UnfDateFormatter(dateFormat);
        SimpleDateFormat simpleUnfDateFormat = new SimpleDateFormat(unfDateFormatter.getUnfFormatString().toString());
        simpleUnfDateFormat.setTimeZone(unfDateFormatter.isTimeZoneSpecified()
                ? TimeZone.getTimeZone("UTC") : simpleUnfDateFormat.getTimeZone());
        boolean containsMilliseconds = dateFormat.contains("S");

        for (int i = 0; i < vector.length; i++) {
            String value = vector[i];
            if (value == null) {
                continue;
            }

            try {
                Date date = simpleDateFormat.parse(value);
                value = simpleUnfDateFormat.format(date);
                // remove any trailing 0s from milliseconds
                if (containsMilliseconds && value.endsWith("0")) {
                    while (value.endsWith("0")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    // if all trailing milliseconds were 0s, there will now be a trailing . to remove
                    if (value.endsWith(".")) {
                        value = value.substring(0, value.length() - 1);
                    }
                }
            } catch (ParseException ex) {
                logger.warn("Parsing exception", ex);
            }
            vector[i] = value;
        }
        UnfClass signature = null;
        if (UnfDigest.getUnfObj()) {
            signature = new UnfClass(UnfCons.DEF_CDGTS, UnfCons.DEF_NDGTS, UnfCons.DEF_HSZ);
        }
        String unf = UnfDigest.unfV(vector, UnfCons.DEF_CDGTS, signature);
        new UnfDigest().getFingerprint().clear();
        return unf;
    }

    /**
     * WARNING: this method alters contents of input vector!
     * WARNING 2: As the original implementation in Harvard's Dataverse has a
     * bug, we added a flag to reproduce it in order to retain compatibility.
     */
    public static String calculateTimeUNF(String[] vector, String savedDateTimeFormat, boolean harvardCompatible)
            throws  IOException, UnfException {
        if (vector == null) {
            return null;
        }
        if (vector[0] != null && vector[0].startsWith("UNF:") && vector[0].split(":").length >= 3) {
            return UnfDigest.addUNFs(vector);
        }

        String timeFormat = StringUtils.isNotEmpty(savedDateTimeFormat) ? savedDateTimeFormat : DATE_TIME_FORMAT;
        String simplifiedFormat;

        SimpleDateFormat strictFullFormatParser = null;
        SimpleDateFormat strictSimplifiedFormatParser = null;

        SimpleDateFormat standardFullFormatParser = new SimpleDateFormat(timeFormat);

        UnfDateFormatter unfDateFormatter = new UnfDateFormatter(timeFormat);
        SimpleDateFormat finalFormat = new SimpleDateFormat(unfDateFormatter.getUnfFormatString().toString());
        finalFormat.setTimeZone(unfDateFormatter.isTimeZoneSpecified() ? TimeZone.getTimeZone("UTC") : finalFormat.getTimeZone());

        SimpleDateFormat finalSimplifiedFormat = null;

        boolean containsMilliseconds = timeFormat.contains("S");

        if (timeFormat.matches(".*\\.SSS z$")) {
            simplifiedFormat = timeFormat.replace(".SSS", "");

            strictFullFormatParser = new SimpleDateFormat(timeFormat);
            strictFullFormatParser.setLenient(false);

            strictSimplifiedFormatParser = new SimpleDateFormat(simplifiedFormat);
            strictSimplifiedFormatParser.setLenient(false);

            UnfDateFormatter simplifiedUnfDateFormatter = new UnfDateFormatter(simplifiedFormat);
            finalSimplifiedFormat = new SimpleDateFormat(simplifiedUnfDateFormatter.getUnfFormatString().toString());
            finalSimplifiedFormat.setTimeZone(simplifiedUnfDateFormatter.isTimeZoneSpecified()
                    ? TimeZone.getTimeZone("UTC") : finalSimplifiedFormat.getTimeZone());
        }

        boolean simplified = false;
        boolean bypass = false;

        for (int i = 0; i < vector.length; i++) {
            String value = vector[i];
            if (value == null) {
                continue;
            }
            Date date;
            simplified = harvardCompatible && simplified;
            if (strictSimplifiedFormatParser != null) {
                // first, try to parse the value against the "full" format (with the milliseconds part):
                try {
                    date = strictFullFormatParser.parse(vector[i]);
                } catch (ParseException pe) {
                    // try the simplified format instead:
                    try {
                        date = strictSimplifiedFormatParser.parse(vector[i]);
                        simplified = true;
                        bypass = false;
                    } catch (ParseException pex) {
                        throw new IOException("no parsable format found for time value [" + i + "]: " + vector[i]);
                    }
                }
            } else {
                try {
                    date = standardFullFormatParser.parse(value);
                } catch (ParseException pe) {
                    logger.trace("Exception while parsing", pe);
                    continue;
                }
            }

            if (!bypass) {
                SimpleDateFormat unfFormat = simplified ? finalSimplifiedFormat : finalFormat;
                value = unfFormat.format(date);
                // remove any trailing 0s from milliseconds
                if (!simplified && containsMilliseconds && value.endsWith("0")) {
                    while (value.endsWith("0")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    // if all trailing milliseconds were 0s, there will now be a trailing . to remove
                    if (value.endsWith(".")) {
                        value = value.substring(0, value.length() - 1);
                    }
                }
                bypass = harvardCompatible && simplified;
            }
            vector[i] = value;
        }
        UnfDigest.setTrnps(false);
        UnfClass signature = null;
        if (UnfDigest.getUnfObj()) {
            signature = new UnfClass(UnfCons.DEF_CDGTS, UnfCons.DEF_NDGTS, UnfCons.DEF_HSZ);
        }
        System.out.println(Arrays.toString(vector));
        String unf = UnfDigest.unfV(vector, UnfCons.DEF_CDGTS, signature);
        new UnfDigest().getFingerprint().clear();
        return unf;
    }
}
