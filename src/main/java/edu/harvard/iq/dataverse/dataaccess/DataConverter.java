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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;



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
    
    
    public static DataFileIO performFormatConversion(DataFile file, DataFileIO dataFileIO, String formatRequested, String formatType) {
        if (!file.isTabularData()) {
            return null;
        }
        
        // if the format requested is "D00", and it's already a TAB file,
        // we don't need to do anything:
        if (formatRequested.equals(FILE_TYPE_TAB) && file.getContentType().equals("text/tab-separated-values")) {

            return dataFileIO;
        }
        
        InputStream convertedFileStream = null;
        long convertedFileSize = 0;
        
        // We may already have a cached copy of this
        // format:
        try {
            convertedFileStream = Channels.newInputStream((ReadableByteChannel) dataFileIO.openAuxChannel(formatRequested));
            convertedFileSize = dataFileIO.getAuxObjectSize(formatRequested);
        } catch (IOException ioex) {
            logger.fine("No cached copy for file format "+formatRequested+", file "+file.getStorageIdentifier());
            convertedFileStream = null;
        }

        // If not cached, run the conversion:
        if (convertedFileStream == null) {

            File tabFile = null;

            boolean tempFilesRequired = false;

            try {
                Path tabFilePath = dataFileIO.getFileSystemPath();
                tabFile = tabFilePath.toFile();
            } catch (UnsupportedDataAccessOperationException uoex) {
                // this means there is no direct filesystem path for this object; it's ok!
                logger.fine("Could not open source file as a local Path - will go the temp file route.");
                tempFilesRequired = true;
            } catch (IOException ioex) {
                // this is likely a fatal condition, as in, the file is unaccessible:
                return null;
            }

            if (tempFilesRequired) {
                ReadableByteChannel tabFileChannel = null;
                try {
                    logger.fine("opening datafFileIO for the source tabular file...");
                    dataFileIO.open();
                    tabFileChannel = dataFileIO.getReadChannel();

                    FileChannel tempFileChannel;
                    tabFile = File.createTempFile("tempTabFile", ".tmp");
                    tempFileChannel = new FileOutputStream(tabFile).getChannel();
                    tempFileChannel.transferFrom(tabFileChannel, 0, dataFileIO.getSize());
                } catch (IOException ioex) {
                    logger.warning("caught IOException trying to store tabular file " + dataFileIO.getDataFile().getStorageIdentifier() + " as a temp file.");

                    return null;
                }
            }

            if (tabFile == null) {
                return null;
            }

            if (tabFile.length() > 0) {
                File formatConvertedFile = runFormatConversion(file, tabFile, formatRequested);

                // cache the result for future use:
                if (formatConvertedFile != null && formatConvertedFile.exists()) {

                    try {
                        dataFileIO.savePathAsAux(Paths.get(formatConvertedFile.getAbsolutePath()), formatRequested);

                    } catch (IOException ex) {
                        logger.warning("failed to save cached format " + formatRequested + " for " + file.getStorageIdentifier());
                        // We'll assume that this is a non-fatal condition.
                    }

                    // re-open the generated file:
                    try {
                        convertedFileStream = new FileInputStream(formatConvertedFile);
                        convertedFileSize = formatConvertedFile.length();
                    } catch (IOException ioex) {
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

            String fileName = dataFileIO.getFileName();
            if (fileName == null || fileName.equals("")) {
                fileName = "f" + file.getId().toString();
            }
            inputStreamIO.setFileName(generateAltFileName(formatRequested, fileName));

            return inputStreamIO;
        }

        return null;
    } // end of performformatconversion();

    // Method for (subsettable) file format conversion.
    // The method needs the subsettable file saved on disk as in the
    // TAB-delimited format.
    // Meaning, if this is a remote subsettable file, it needs to be downloaded
    // and stored locally as a temporary file; and if it's a fixed-field file, it
    // needs to be converted to TAB-delimited, before you can feed the file
    // to this method. (See performFormatConversion() method)
    // The method below takes the tab file and sends it to the R server
    // (possibly running on a remote host) and gets back the transformed copy,
    // providing error-checking and diagnostics in the process.
    // This is mostly Akio Sone's code from DVN3. 
    // (hence some obsolete elements in the comment above: ALL of the tabular
    // data files in Dataverse are saved in tab-delimited format - we no longer
    // support fixed-field files!

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

        File formatConvertedFile = null;
        // create the service instance
        RemoteDataFrameService dfs = new RemoteDataFrameService();
        
        if ("RData".equals(formatRequested)) {
            List<DataVariable> dataVariables = file.getDataTable().getDataVariables();
            Map<String, Map<String, String>> vls = null;

            vls = getValueTableForRequestedVariables(dataVariables);
            logger.fine("format conversion: variables(getDataVariableForRequest())=" + dataVariables + "\n");
            logger.fine("format conversion: variables(dataVariables)=" + dataVariables + "\n");
            logger.fine("format conversion: value table(vls)=" + vls + "\n");
            RJobRequest sro = new RJobRequest(dataVariables, vls);

            sro.setTabularDataFileName(tabFile.getAbsolutePath());
            sro.setRequestType(SERVICE_REQUEST_CONVERT);
            sro.setFormatRequested(FILE_TYPE_RDATA);

        

            // execute the service
            Map<String, String> resultInfo = dfs.execute(sro);

            //resultInfo.put("offlineCitation", citation);
            logger.fine("resultInfo="+resultInfo+"\n");

            // check whether a requested file is actually created

            if ("true".equals(resultInfo.get("RexecError"))){
                logger.fine("R-runtime error trying to convert a file.");
                return  null;
            } else {
                String dataFrameFileName = resultInfo.get("dataFrameFileName");
                logger.fine("data frame file name: "+dataFrameFileName);

                formatConvertedFile = new File(dataFrameFileName);
            }
        } else if ("prep".equals(formatRequested)) {
            formatConvertedFile = dfs.runDataPreprocessing(file);
        } else {
            logger.warning("Unsupported file format requested: "+formatRequested);
            return null; 
        }
            

        if (formatConvertedFile.exists()) {
            logger.fine("frmtCnvrtdFile:length=" + formatConvertedFile.length());
        } else {
            logger.warning("Format-converted file was not properly created.");
            return null;
        }

        return formatConvertedFile;
    }

    private static Map<String, Map<String, String>> getValueTableForRequestedVariables(List<DataVariable> dvs){
        Map<String, Map<String, String>> vls = new LinkedHashMap<String, Map<String, String>>();
        for (DataVariable dv : dvs){
            List<VariableCategory> varCat = new ArrayList<VariableCategory>();
            varCat.addAll(dv.getCategories());
            Map<String, String> vl = new HashMap<String, String>();
            for (VariableCategory vc : varCat){
                if (vc.getLabel() != null){
                    vl.put(vc.getValue(), vc.getLabel());
                }
            }
            if (vl.size() > 0){
                vls.put("v"+dv.getId(), vl);
            }
        }
        return vls;
    }
        
    private static String generateAltFileName(String formatRequested, String xfileId) {
        String altFileName = xfileId;

        if ( altFileName == null || altFileName.equals("")) {
            altFileName = "Converted";
        }

        if ( formatRequested != null ) {
            altFileName = FileUtil.replaceExtension(altFileName, formatRequested);
        }

        return altFileName;
    }
    
}
