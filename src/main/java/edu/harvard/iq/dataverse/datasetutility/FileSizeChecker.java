/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Convenience methods for checking max. file size
 * @author rmp553
 */
public class FileSizeChecker {

    private static final Logger logger = Logger.getLogger(FileSizeChecker.class.getCanonicalName());

    SystemConfig systemConfig;
           
    /**
     * constructor
     */
    public FileSizeChecker(SystemConfig systemConfig){
        if (systemConfig == null){
            throw new NullPointerException("systemConfig cannot be null");
        }
        this.systemConfig = systemConfig;
    }
    
    public FileSizeResponse isAllowedFileSize(Long filesize){
        
        if (filesize == null){
            throw new NullPointerException("filesize cannot be null");            
            //return new FileSizeResponse(false, "The file size could not be found!");
        }
        
        Long maxFileSize = systemConfig.getMaxFileUploadSize();
        
        // If no maxFileSize in the database, set it to unlimited!
        //
        if (maxFileSize == null){
            return new FileSizeResponse(true, 
                    BundleUtil.getStringFromBundle("file.addreplace.file_size_ok")
            );
        }
    
        // Good size!
        //
        if (filesize <= maxFileSize){
            return new FileSizeResponse(true, 
                    BundleUtil.getStringFromBundle("file.addreplace.file_size_ok")
            );
        }
        
        // Nope!  Sorry! File is too big
        //
        String errMsg = BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit", Collections.singletonList(bytesToHumanReadable(maxFileSize)));
        
        return new FileSizeResponse(false, errMsg);
        
    }
    
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
    
    /**
     * Inner class that can also return an error message
     */
    public class FileSizeResponse{
        
        public boolean fileSizeOK;
        public String userMsg;
        
        public FileSizeResponse(boolean isOk, String msg){
            
            fileSizeOK = isOk;
            userMsg = msg;
        }
        
        public boolean isFileSizeOK(){
            return fileSizeOK;
        }
        
        public String getUserMessage(){
            return userMsg;
        }
        
    } // end inner class
}
