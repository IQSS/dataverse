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

import java.io.IOException;

import edu.harvard.iq.dataverse.DataFile;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Logger;
/**
 *
 * @author Leonid Andreev
 */
public class StoredOriginalFile {
    private static Logger logger = Logger.getLogger(StoredOriginalFile.class.getPackage().getName());
    
    public StoredOriginalFile () {
        
    }
    
    private static final String SAVED_ORIGINAL_FILENAME_EXTENSION = "orig";
    
    public static StorageIO<DataFile> retreive(StorageIO<DataFile> storageIO) {
        String originalMimeType;

        DataFile dataFile = storageIO.getDataFile();

        if (dataFile == null) {
            return null;
        }

        if (dataFile.getDataTable() != null) {
            originalMimeType = dataFile.getDataTable().getOriginalFileFormat();
        } else {
            return null;
        }

        long storedOriginalSize; 
        InputStreamIO inputStreamIO;
        
        try {
            storageIO.open();
            Channel storedOriginalChannel = storageIO.openAuxChannel(SAVED_ORIGINAL_FILENAME_EXTENSION);
            storedOriginalSize = dataFile.getDataTable().getOriginalFileSize() != null ? 
                    dataFile.getDataTable().getOriginalFileSize() : 
                    storageIO.getAuxObjectSize(SAVED_ORIGINAL_FILENAME_EXTENSION);
            inputStreamIO = new InputStreamIO(Channels.newInputStream((ReadableByteChannel) storedOriginalChannel), storedOriginalSize);
            logger.fine("Opened stored original file as Aux "+SAVED_ORIGINAL_FILENAME_EXTENSION);
        } catch (IOException ioEx) {
            // The original file not saved, or could not be opened.
            logger.fine("Failed to open stored original file as Aux "+SAVED_ORIGINAL_FILENAME_EXTENSION+"!");
            return null;
        }

        if (originalMimeType != null && !originalMimeType.isEmpty()) {
            if (originalMimeType.matches("application/x-dvn-.*-zip")) {
                inputStreamIO.setMimeType("application/zip");
            } else {
                inputStreamIO.setMimeType(originalMimeType);
            }
        } else {
            inputStreamIO.setMimeType("application/x-unknown");
        }

        String fileName = storageIO.getFileName();
        if (fileName != null) {
            if (originalMimeType != null) {
                String origFileExtension = generateOriginalExtension(originalMimeType);
                inputStreamIO.setFileName(fileName.replaceAll(".tab$", origFileExtension));
            } else {
                inputStreamIO.setFileName(fileName.replaceAll(".tab$", ""));
            }
        }

        return inputStreamIO;

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
