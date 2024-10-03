/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.common;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static edu.harvard.iq.dataverse.common.FileSizeUtil.bytesToHumanReadable;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author oscardssmith
 */
public class FileSizeUtilTest {

    @Test
    public void bytesToHumanReadable2() {
    	
    	String B = getStringFromBundle("file.addreplace.error.byte_abrev");
    	
    	assertEquals("1 " + B, bytesToHumanReadable(1L));
    	assertEquals("1023 " + B, bytesToHumanReadable(1023L));
    	assertEquals("1.9 K" + B, bytesToHumanReadable(1986L));
    	assertEquals("122.8 K" + B, bytesToHumanReadable(125707L));
    	assertEquals("2.6 G" + B, bytesToHumanReadable(2759516000L));
    	assertEquals("10.9 T" + B, bytesToHumanReadable(12039650000000L));
    	
    	
    	assertEquals("1 " + B, bytesToHumanReadable(1L, 2));
    	assertEquals("1023 " + B, bytesToHumanReadable(1023L, 2));
    	assertEquals("1.94 K" + B, bytesToHumanReadable(1986L, 2));
    	assertEquals("122.76 K" + B, bytesToHumanReadable(125707L, 2));
    	assertEquals("2.57 G" + B, bytesToHumanReadable(2759516000L, 2));
    	assertEquals("10.95 T" + B, bytesToHumanReadable(12039650000000L, 2));
    }
}
