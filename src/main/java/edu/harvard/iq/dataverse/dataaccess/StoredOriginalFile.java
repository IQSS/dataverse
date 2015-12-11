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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import edu.harvard.iq.dataverse.DataFile;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
/**
 *
 * @author Leonid Andreev
 */
public class StoredOriginalFile {
    
    public StoredOriginalFile () {
        
    }
    
    private static final String SAVED_ORIGINAL_FILENAME_EXTENSION = "orig";
    
    public static DataFileIO retreive(DataFileIO dataAccess) {
        String originalMimeType = null;

        DataFile dataFile = dataAccess.getDataFile();

        if (dataFile == null) {
            return null;
        }

        if (dataFile.getDataTable() != null) {
            originalMimeType = dataFile.getDataTable().getOriginalFileFormat();
        } else {
            return null;
        }

        Channel storedOriginalChannel = null;
        try {
            storedOriginalChannel = dataAccess.openAuxChannel(SAVED_ORIGINAL_FILENAME_EXTENSION);
        } catch (IOException ioEx) {
            // The original file not saved, or could not be opened.
            return null;
        }

        if (storedOriginalChannel == null) {
            return null;
        }

        dataAccess.setInputStream(Channels.newInputStream((ReadableByteChannel) storedOriginalChannel));

        // Reset the size, filename and the mime type: 
        
        long origFileSize; 
        try {
            origFileSize = dataAccess.getAuxObjectSize(SAVED_ORIGINAL_FILENAME_EXTENSION);
        } catch (IOException ioEx) {
            return null; 
        }
        
        dataAccess.setSize(origFileSize);
        
        if (originalMimeType != null && !originalMimeType.equals("")) {
            if (originalMimeType.matches("application/x-dvn-.*-zip")) {
                dataAccess.setMimeType("application/zip");
            } else {
                dataAccess.setMimeType(originalMimeType);
            }
        } else {
            dataAccess.setMimeType("application/x-unknown");
        }

        String fileName = dataAccess.getFileName();
        if (fileName != null) {
            if (originalMimeType != null) {
                String origFileExtension = generateOriginalExtension(originalMimeType);
                dataAccess.setFileName(fileName.replaceAll(".tab$", origFileExtension));
            } else {
                dataAccess.setFileName(fileName.replaceAll(".tab$", ""));
            }
        }

        dataAccess.setNoVarHeader(true);
        dataAccess.setVarHeader(null);
        
        return dataAccess;

    }
    
    // This method is deprecated; 
    // The method above should be used instead, as it fully utilises the new generic 
    // DataFileIO framework. 
    // The method below is however left in place temporarily, for backward 
    // compatibility access to the existing stored originals. 
    
    /*
    @Deprecated
    public static FileAccessIO retrieve (DataFile dataFile, FileAccessIO fileDownload) {
        String originalMimeType = null; 
        
        if (dataFile.getDataTable() != null) {
            originalMimeType = dataFile.getDataTable().getOriginalFileFormat();
        } else {
            return null; 
        }
        
        /* 
         * TODO: 
         * This assumes that, and only works if this file is stored locally 
         * on the filesystem!!
         * L.A. 4.0.2
        */
       /*
        String tabularFileName = dataFile.getStorageIdentifier(); 
        Path savedOriginalPath = null; 
        
        if (tabularFileName != null && !tabularFileName.equals("")) {
            savedOriginalPath = Paths.get(dataFile.getOwner().getFileSystemDirectory().toString(), "_"+tabularFileName);
        }     
        

        if (savedOriginalPath != null) {
            
            if (Files.exists(savedOriginalPath)) {
                
                fileDownload.closeInputStream();
                fileDownload.setSize(savedOriginalPath.toFile().length());
                
                try {
                    fileDownload.setInputStream(new FileInputStream(savedOriginalPath.toFile()));
                } catch (IOException ex) {
                    return null; 
                }
                fileDownload.setIsLocalFile(true);

                if (originalMimeType != null && !originalMimeType.equals("")) {
                    if (originalMimeType.matches("application/x-dvn-.*-zip")) {
                        fileDownload.setMimeType("application/zip");
                    } else {
                        fileDownload.setMimeType(originalMimeType);
                    }
                } else {
                    fileDownload.setMimeType("application/x-unknown");
                }

                String fileName = fileDownload.getFileName();
                if (fileName != null) {
                    if ( originalMimeType != null) {
                        String origFileExtension = generateOriginalExtension(originalMimeType);
                        fileDownload.setFileName(fileName.replaceAll(".tab$", origFileExtension));
                    } else {
                        fileDownload.setFileName(fileName.replaceAll(".tab$", ""));
                    }
                }


                // The fact that we have the "original format" file for this data
                // set, means it's a subsettable, tab-delimited file. Which means
                // we've already prepared a variable header to be added to the
                // stream. We don't want to add it to the stream that's no longer
                // tab-delimited -- that would screw it up! -- so let's remove
                // those headers:

                fileDownload.setNoVarHeader(true);
                fileDownload.setVarHeader(null);
                
                return fileDownload;
            }
        }
        
        return null;
    }*/

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
        } else if (fileType.equalsIgnoreCase("application/x-stata") || fileType.equalsIgnoreCase("application/x-stata-13")) {
            return ".dta";
        } else if (fileType.equalsIgnoreCase("application/x-dvn-csvspss-zip")) {
            return ".zip";
        } else if (fileType.equalsIgnoreCase("application/x-dvn-tabddi-zip")) {
            return ".zip";
        } else if (fileType.equalsIgnoreCase("application/x-rlang-transport")) {
            return ".RData";
        } else if (fileType.equalsIgnoreCase("text/csv") || fileType.equalsIgnoreCase("text/comma-separated-values")) {
            return ".csv";
        } else if (fileType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ".xlsx";
        }

        return "";
    }
}
