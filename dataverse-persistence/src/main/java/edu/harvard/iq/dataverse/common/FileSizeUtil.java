package edu.harvard.iq.dataverse.common;

public class FileSizeUtil {
    
    /* This method turns a number of bytes into a human readable version
     */
    public static String bytesToHumanReadable(long v) {
        return bytesToHumanReadable(v, 1);
    }

    /* This method turns a number of bytes into a human readable version
     * with figs decimal places
     */
    public static String bytesToHumanReadable(long v, int figs) {
        if (v < 1024) {
            return v + " " + BundleUtil.getStringFromBundle("file.addreplace.error.byte_abrev");
        }
        // 63 - because long has 63 binary digits
        int trailingBin0s = (63 - Long.numberOfLeadingZeros(v)) / 10;
        //String base = "%."+figs+"f %s"+ BundleUtil.getStringFromBundle("file.addreplace.error.byte_abrev");
        return String.format("%." + figs + "f %s" + BundleUtil.getStringFromBundle("file.addreplace.error.byte_abrev"), (double) v / (1L << (trailingBin0s * 10)),
                             " KMGTPE".charAt(trailingBin0s));
    }
}
