/*
   Copyright (C) 2005-2014, by the President and Fellows of Harvard College.

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

import java.util.logging.Logger;
import java.util.List; 
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap; 

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.rserve.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;



/**
 * 4.0 implementation of the Data Access "optional service" that offers 
 * access to "subsettable" (tabular) data files in alternative formats. 
 * In reality, there will only be 1 alternative format supported in 4.0: 
 * R data. The module will still provide functionality for supporting 
 * multiple alternative formats. 
 * 
 * @author Leonid Andreev
 */
public class DataConverter {
    private static Logger logger = Logger.getLogger(DataConverter.class.getPackage().getName());
    
    public DataConverter() {
    }
    
    public static String FILE_TYPE_TAB = "tab";
    public static String FILE_TYPE_RDATA = "RData";
    
    public static String SERVICE_REQUEST_CONVERT = "convert";
    
    
    public static StorageIO<DataFile> performFormatConversion(DataFile file, StorageIO<DataFile> storageIO, String formatRequested, String formatType) {
        if (!file.isTabularData()) {
            return null;
        }
        
        // if the format requested is "D00", and it's already a TAB file,
        // we don't need to do anything:
        if (formatRequested.equals(FILE_TYPE_TAB) && file.getContentType().equals("text/tab-separated-values")) {

            return storageIO;
        }
        
        InputStream convertedFileStream = null;
        long convertedFileSize = 0;
        
        // We may already have a cached copy of this
        // format:
        try {
            convertedFileStream = Channels.newInputStream((ReadableByteChannel) storageIO.openAuxChannel(formatRequested));
            convertedFileSize = storageIO.getAuxObjectSize(formatRequested);
        } catch (IOException ioex) {
            logger.fine("No cached copy for file format "+formatRequested+", file "+file.getStorageIdentifier());
            convertedFileStream = null;
        }

        // If not cached, run the conversion:
        if (convertedFileStream == null) {

            File tabFile = downloadFromStorageIO(storageIO);

            if (tabFile == null) {
                return null;
            }

            if (tabFile.length() > 0) {
                File formatConvertedFile = runFormatConversion(file, tabFile, formatRequested);

                // cache the result for future use:
                if (formatConvertedFile != null && formatConvertedFile.exists()) {

                    try {
                        storageIO.savePathAsAux(Paths.get(formatConvertedFile.getAbsolutePath()), formatRequested);

                    } catch (IOException ex) {
                        logger.warning("failed to save cached format " + formatRequested + " for " + file.getStorageIdentifier());
                        // We'll assume that this is a non-fatal condition.
                    }

                    // re-open the generated file:
                    try {
                        convertedFileStream = new FileInputStream(formatConvertedFile);
                        convertedFileSize = formatConvertedFile.length();
                    } catch (FileNotFoundException ioex) {
                        logger.warning("Failed to open generated format " + formatRequested + " for " + file.getStorageIdentifier());
                        return null;
                    }
                }

            }
        }

        // Now check the converted stream, and return the IO object back to the 
        // download API instance writer:
        if (convertedFileStream != null && convertedFileSize > 0) {

            InputStreamIO inputStreamIO = null;
            try {
                inputStreamIO = new InputStreamIO(convertedFileStream, convertedFileSize);
            } catch (IOException ioex) {
                return null;
            }

            inputStreamIO.setMimeType(formatType);

            String fileName = storageIO.getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "f" + file.getId().toString();
            }
            inputStreamIO.setFileName(generateAltFileName(formatRequested, fileName));

            return inputStreamIO;
        }

        return null;
    }

    private static File downloadFromStorageIO(StorageIO<DataFile> storageIO) {
        if (storageIO.isLocalFile()){
            try {
                Path tabFilePath = storageIO.getFileSystemPath();
                return tabFilePath.toFile();
            } catch (IOException ioex) {
                // this is likely a fatal condition, as in, the file is unaccessible:
            }
        } else {
            try {
                storageIO.open();
                return downloadFromByteChannel(storageIO.getReadChannel(), storageIO.getSize());
            } catch (IOException ex) {
                logger.warning("caught IOException trying to store tabular file " + storageIO.getDataFile().getStorageIdentifier() + " as a temp file.");
            }
        }
        return null;
    }

    private static File downloadFromByteChannel(ReadableByteChannel tabFileChannel, long size) {
        try {
            logger.fine("opening datafFileIO for the source tabular file...");
            
            File tabFile = File.createTempFile("tempTabFile", ".tmp");
            FileChannel tempFileChannel = new FileOutputStream(tabFile).getChannel();
            tempFileChannel.transferFrom(tabFileChannel, 0, size);
            return tabFile;
        } catch (IOException ioex) {
            logger.warning("caught IOException trying to store tabular file as a temp file.");
        }
        return null;
    }

    // Method for (subsettable) file format conversion.
    // The method needs the subsettable file saved on disk as in the
    // TAB-delimited format.
    // Meaning, if this is a remote subsettable file, it needs to be downloaded
    // and stored locally as a temporary file (See performFormatConversion() method)
    // The method below takes the tab file and sends it to the R server
    // (possibly running on a remote host) and gets back the transformed copy,
    // providing error-checking and diagnostics in the process.
    // This is mostly Akio Sone's code from DVN3.
    private static File runFormatConversion (DataFile file, File tabFile, String formatRequested) {

        if ( formatRequested.equals (FILE_TYPE_TAB) ) {
            // if the *requested* format is TAB-delimited, we don't
            // need to call R to do any conversions, we can just
            // send back the TAB file we have just produced.
            
            // (OK, so that the assumption is, if this is a fixed-field file -- 
            // from ICPSR or otherwise -- the Access service has already 
            // converted it to tab-delimited... TODO: review this logic; 
            // perhaps fixed-field to tabular should also be handled here? 
            // -- L.A. 4.0 alpha 1)

            return tabFile;
        }

        File formatConvertedFile;
        // create the service instance
        RemoteDataFrameService dfs = new RemoteDataFrameService();
        
        if ("RData".equals(formatRequested)) {
            String origFormat = file.getOriginalFileFormat();
            Map<String, String> resultInfo;
            if (origFormat.contains("stata") || origFormat.contains("spss")){
                if (origFormat.contains("stata")){
                    origFormat = "dta";
                } else if (origFormat.contains("sav")){
                    origFormat = "sav";
                } else if (origFormat.contains("por")){
                    origFormat = "por";
                }
                    
                try {
                    StorageIO<DataFile> storageIO = file.getStorageIO();
                    long size = storageIO.getAuxObjectSize("orig");
                    File origFile = downloadFromByteChannel((ReadableByteChannel) storageIO.openAuxChannel("orig"), size);
                    resultInfo = dfs.directConvert(origFile, origFormat);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return null;
                }
                
            } else{
                List<DataVariable> dataVariables = file.getDataTable().getDataVariables();
                Map<String, Map<String, String>> vls = getValueTableForRequestedVariables(dataVariables);
                logger.fine("format conversion: variables(getDataVariableForRequest())=" + dataVariables + "\n");
                logger.fine("format conversion: variables(dataVariables)=" + dataVariables + "\n");
                logger.fine("format conversion: value table(vls)=" + vls + "\n");
                RJobRequest sro = new RJobRequest(dataVariables, vls);

                sro.setTabularDataFileName(tabFile.getAbsolutePath());
                sro.setRequestType(SERVICE_REQUEST_CONVERT);
                sro.setFormatRequested(FILE_TYPE_RDATA);

                // execute the service
                resultInfo = dfs.execute(sro);
            }

            //resultInfo.put("offlineCitation", citation);
            logger.fine("resultInfo="+resultInfo+"\n");

            // check whether a requested file is actually created

            if ("true".equals(resultInfo.get("RexecError"))){
                logger.fine("R-runtime error trying to convert a file.");
                return  null;
            }
            String dataFrameFileName = resultInfo.get("dataFrameFileName");
            logger.fine("data frame file name: "+dataFrameFileName);

            formatConvertedFile = new File(dataFrameFileName);
        } else if ("prep".equals(formatRequested)) {
            formatConvertedFile = dfs.runDataPreprocessing(file);
        } else {
            logger.warning("Unsupported file format requested: "+formatRequested);
            return null; 
        }
            

        if (formatConvertedFile == null || !formatConvertedFile.exists()) {
            logger.warning("Format-converted file was not properly created.");
            return null;
        }
        logger.fine("frmtCnvrtdFile:length=" + formatConvertedFile.length());
        return formatConvertedFile;
    }

    private static Map<String, Map<String, String>> getValueTableForRequestedVariables(List<DataVariable> dataVariables){
        Map<String, Map<String, String>> allVarLabels = new LinkedHashMap<>();
        for (DataVariable dataVar : dataVariables){
            Map<String, String> varLabels = new HashMap<>();
            for (VariableCategory varCatagory : dataVar.getCategories()){
                if (varCatagory.getLabel() != null){
                    varLabels.put(varCatagory.getValue(), varCatagory.getLabel());
                }
            }
            if (!varLabels.isEmpty()){
                allVarLabels.put("v"+dataVar.getId(), varLabels);
            }
        }
        return allVarLabels;
    }
        
    private static String generateAltFileName(String formatRequested, String xfileId) {
        String altFileName = xfileId;

        if (altFileName == null || altFileName.isEmpty()) {
            altFileName = "Converted";
        }
        // Fixme:" should this be else if?
        if ( formatRequested != null ) {
            altFileName = FileUtil.replaceExtension(altFileName, formatRequested);
        }

        return altFileName;
    }
    
}
