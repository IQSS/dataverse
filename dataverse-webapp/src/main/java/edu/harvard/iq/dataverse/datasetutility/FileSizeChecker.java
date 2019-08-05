/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.FileSizeUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.Collections;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Convenience methods for checking max. file size
 *
 * @author rmp553
 */
public class FileSizeChecker {

    private static final Logger logger = Logger.getLogger(FileSizeChecker.class.getCanonicalName());

    SettingsServiceBean settingsService;

    /**
     * constructor
     */
    public FileSizeChecker(SettingsServiceBean settingsService) {
        this.settingsService = Objects.requireNonNull(settingsService);
    }

    public FileSizeResponse isAllowedFileSize(Long filesize) {

        if (filesize == null) {
            throw new NullPointerException("filesize cannot be null");
            //return new FileSizeResponse(false, "The file size could not be found!");
        }

        Long maxFileSize = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);

        // If no maxFileSize in the database, set it to unlimited!
        //
        if (maxFileSize == null) {
            return new FileSizeResponse(true,
                                        BundleUtil.getStringFromBundle("file.addreplace.file_size_ok")
            );
        }

        // Good size!
        //
        if (filesize <= maxFileSize) {
            return new FileSizeResponse(true,
                                        BundleUtil.getStringFromBundle("file.addreplace.file_size_ok")
            );
        }

        // Nope!  Sorry! File is too big
        //
        String errMsg = BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit", Collections.singletonList(FileSizeUtil.bytesToHumanReadable(maxFileSize)));

        return new FileSizeResponse(false, errMsg);

    }

    /**
     * Inner class that can also return an error message
     */
    public class FileSizeResponse {

        public boolean fileSizeOK;
        public String userMsg;

        public FileSizeResponse(boolean isOk, String msg) {

            fileSizeOK = isOk;
            userMsg = msg;
        }

        public boolean isFileSizeOK() {
            return fileSizeOK;
        }

        public String getUserMessage() {
            return userMsg;
        }

    } // end inner class
}
