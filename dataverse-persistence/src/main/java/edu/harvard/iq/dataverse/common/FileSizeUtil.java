package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.BundleUtil.getCurrentLocale;
import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.lang.Long.numberOfLeadingZeros;
import static java.lang.String.format;

public class FileSizeUtil {

    /*
     * This method turns a number of bytes into a human readable version
     */
    public static String bytesToHumanReadable(final long v) {

        return bytesToHumanReadable(v, 1);
    }

    /*
     * This method turns a number of bytes into a human readable version with figs
     * decimal places
     */
    public static String bytesToHumanReadable(final long v, final int figs) {

        final String B = getStringFromBundle("file.addreplace.error.byte_abrev");

        if (v < 1024) {
            return v + " " + B;
        } else {
            final String formatStr = "%." + figs + "f %s" + B;
            // 63 - because long has 63 binary digits
            final int trailingBin0s = (63 - numberOfLeadingZeros(v)) / 10;
            final char magnitude = " KMGTPE".charAt(trailingBin0s);
            final double roundedValue = (double) v / (1L << (trailingBin0s * 10));

            return format(getCurrentLocale(), formatStr, roundedValue, magnitude);
        }
    }
}
