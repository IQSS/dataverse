/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author Leonid Andreev
 */
public class StoredOriginalFile {
    private static Logger logger = Logger.getLogger(StoredOriginalFile.class.getPackage().getName());


    public static StorageIO<DataFile> retreive(StorageIO<DataFile> storageIO) {

        DataFile dataFile = storageIO.getDataFile();

        if (dataFile == null) {
            return null;
        }
        if (dataFile.getDataTable() == null) {
            return null;
        }

        String originalMimeType = dataFile.getDataTable().getOriginalFileFormat();
        
        
        try {
            storageIO.open();
            long storedOriginalSize = dataFile.getDataTable().getOriginalFileSize() != null ?
                    dataFile.getDataTable().getOriginalFileSize() :
                    storageIO.getAuxObjectSize(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);

            String mimeType = generateOriginalFileMimeType(originalMimeType);
            String fileName = generateFileName(storageIO.getFileName(), originalMimeType);
                    
            InputStream storedOriginalInputStream = storageIO.getAuxFileAsInputStream(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);
            logger.fine("Opened stored original file as Aux " + StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);
            
            return new InputStreamIO(storedOriginalInputStream, storedOriginalSize, mimeType, fileName);

        } catch (IOException ioEx) {
            // The original file not saved, or could not be opened.
            logger.fine("Failed to open stored original file as Aux " + StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION + "!");
            return null;
        }

    }

    private static String generateOriginalFileMimeType(String originalMimeType) {
        String mimeType = StringUtils.defaultIfEmpty(originalMimeType, "application/x-unknown");

        if (mimeType.matches("application/x-dvn-.*-zip")) {
            mimeType = "application/zip";
        }
        return mimeType;
    }

    private static String generateFileName(String storageFilename, String originalMimeType) {
        if (originalMimeType != null) {
            String origFileExtension = generateOriginalExtension(originalMimeType);
            return storageFilename.replaceAll("\\.tab$", origFileExtension);
        } else {
            return storageFilename.replaceAll("\\.tab$", "");
        }
    }

    // TODO: 
    // do what the comment below says - move this code into the file util, 
    // or something like that!
    // -- L.A. 4.0 beta15
    // Shouldn't be here; should be part of the DataFileFormatType, or 
    // something like that... 

    private static String generateOriginalExtension(String fileType) {

        if (fileType.equalsIgnoreCase("application/x-spss-sav")) {
            return ".sav";
        } else if (fileType.equalsIgnoreCase("application/x-spss-por")) {
            return ".por";
        } else if (fileType.equalsIgnoreCase("application/x-stata") || fileType.equalsIgnoreCase("application/x-stata-13") || fileType.equalsIgnoreCase("application/x-stata-14") || fileType.equalsIgnoreCase("application/x-stata-15")) {
            return ".dta";
        } else if (fileType.equalsIgnoreCase("application/x-dvn-csvspss-zip")) {
            return ".zip";
        } else if (fileType.equalsIgnoreCase("application/x-dvn-tabddi-zip")) {
            return ".zip";
        } else if (fileType.equalsIgnoreCase("application/x-rlang-transport")) {
            return ".RData";
        } else if (fileType.equalsIgnoreCase("text/csv") || fileType.equalsIgnoreCase("text/comma-separated-values")) {
            return ".csv";
        } else if (fileType.equalsIgnoreCase("text/tsv") || fileType.equalsIgnoreCase("text/tab-separated-values")) {
            return ".tsv";
        } else if (fileType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ".xlsx";
        }
        logger.severe(fileType + " does not have an associated file extension");
        return "";
    }
}
