/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.util.BundleUtil;

/**
 * Convenience methods for checking max. file size
 * @author rmp553
 */
public class FileSizeChecker {

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
			return v +  " "  + BundleUtil.getStringFromBundle("file.addreplace.error.byte_abrev");
		}
		// 63 - because long has 63 binary digits
		int trailingBin0s = (63 - Long.numberOfLeadingZeros(v))/10;
		//String base = "%."+figs+"f %s"+ BundleUtil.getStringFromBundle("file.addreplace.error.byte_abrev");
		return String.format("%."+figs+"f %s"+ BundleUtil.getStringFromBundle("file.addreplace.error.byte_abrev"), (double)v / (1L << (trailingBin0s*10)),
				" KMGTPE".charAt(trailingBin0s));
	}

}
