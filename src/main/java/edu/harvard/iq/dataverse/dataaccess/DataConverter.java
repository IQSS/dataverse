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
import java.nio.file.Path;



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
    private static Logger dbgLog = Logger.getLogger(DataConverter.class.getPackage().getName());
    
    public DataConverter() {
    }
    
    public static String FILE_TYPE_TAB = "tab";
    public static String FILE_TYPE_RDATA = "RData";
    
    public static String SERVICE_REQUEST_CONVERT = "convert";
    
    
    public static FileAccessIO performFormatConversion (DataFile file, FileAccessIO fileDownload, String formatRequested, String formatType) {
        if (!file.isTabularData() || !fileDownload.isLocalFile()) {
            return null; 
        }
        
        Path tabFilePath = null; 
        
        try {
            tabFilePath = fileDownload.getFileSystemPath();
        } catch (IOException ioEx) {
            return null; 
        }
        
        if (tabFilePath == null) {
            return null; 
        }
        
        File tabFile = null; 
        File formatConvertedFile = null;

        String cachedFileSystemLocation = null;

         // initialize the data variables list:

        List<DataVariable> dataVariables = file.getDataTable().getDataVariables();

        // if the format requested is "D00", and it's already a TAB file,
        // we don't need to do anything:
        if (formatRequested.equals(FILE_TYPE_TAB)
                && file.getContentType().equals("text/tab-separated-values")) {

            return fileDownload;
        }

        // We may already have a cached copy of this
        // format:
        cachedFileSystemLocation = tabFilePath.toString()
                + "."
                + formatRequested;

        if (new File(cachedFileSystemLocation).exists()) {
            formatConvertedFile = new File(cachedFileSystemLocation);
        } else {
                // OK, we don't have a cached copy. So we'll have to run
            // conversion again (below). Let's have the
            // tab-delimited file handy:

            tabFile = tabFilePath.toFile();
        }
                        
        
        // Check if the tab file is present and run the conversion:

        if (tabFile != null && (tabFile.length() > 0)) {   
            formatConvertedFile = runFormatConversion (file, tabFile, formatRequested);

            // for local files, cache the result:

            if (formatConvertedFile != null &&
                    formatConvertedFile.exists()) {

                try {
                    File cachedConvertedFile = new File (cachedFileSystemLocation);
                    FileUtil.copyFile(formatConvertedFile,cachedConvertedFile);
                    formatConvertedFile.delete();
                    formatConvertedFile = cachedConvertedFile;
                    
                } catch (IOException ex) {
                    // Whatever. For whatever reason we have failed to cache
                    // the format-converted copy of the file we just produced.
                    // But it's not fatal. So we just carry on.
                }
            }

        }

        // Now check the converted file: 
              
        if (formatConvertedFile != null && formatConvertedFile.exists()) {

            fileDownload.closeInputStream();
            fileDownload.setSize(formatConvertedFile.length());

            try {
                fileDownload.setInputStream(new FileInputStream(formatConvertedFile));
            } catch (IOException ex) {
                return null; 
            }

            fileDownload.releaseConnection();
            fileDownload.setHTTPMethod(null);
            fileDownload.setIsLocalFile(true);

            fileDownload.setMimeType(formatType);
            String dbFileName = fileDownload.getFileName();

            if (dbFileName == null || dbFileName.equals("")) {
                dbFileName = "f" + file.getId().toString();
            }

            fileDownload.setFileName(generateAltFileName(formatRequested, dbFileName));

            if (formatRequested.equals(FILE_TYPE_TAB) && (!fileDownload.noVarHeader())) {

                String varHeaderLine = null;
                List dataVariablesList = file.getDataTable().getDataVariables();
                //TODO://varHeaderLine = generateVariableHeader(dataVariablesList);
                fileDownload.setVarHeader(varHeaderLine);
            } else {
                fileDownload.setNoVarHeader(true);
                fileDownload.setVarHeader(null);
                // (otherwise, since this is a subsettable file, the variable header
                //  will be added to this R/Stata/etc. file -- which would
                //  totally screw things up!)
            }

            //TODO://setDownloadContentHeaders (fileDownload);

           
            return fileDownload; 
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
    // This is mostly Akio Sone's code.

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
            dbgLog.fine("format conversion: variables(getDataVariableForRequest())=" + dataVariables + "\n");
            dbgLog.fine("format conversion: variables(dataVariables)=" + dataVariables + "\n");
            dbgLog.fine("format conversion: value table(vls)=" + vls + "\n");
            RJobRequest sro = new RJobRequest(dataVariables, vls);

            sro.setTabularDataFileName(tabFile.getAbsolutePath());
            sro.setRequestType(SERVICE_REQUEST_CONVERT);
            sro.setFormatRequested(FILE_TYPE_RDATA);

        

            // execute the service
            Map<String, String> resultInfo = dfs.execute(sro);

            //resultInfo.put("offlineCitation", citation);
            dbgLog.fine("resultInfo="+resultInfo+"\n");

            // check whether a requested file is actually created

            if ("true".equals(resultInfo.get("RexecError"))){
                dbgLog.fine("R-runtime error trying to convert a file.");
                return  null;
            } else {
                String dataFrameFileName = resultInfo.get("dataFrameFileName");
                dbgLog.fine("data frame file name: "+dataFrameFileName);

                formatConvertedFile = new File(dataFrameFileName);
            }
        } else if ("prep".equals(formatRequested)) {
            formatConvertedFile = dfs.runDataPreprocessing(file);
        } else {
            dbgLog.warning("Unsupported file format requested: "+formatRequested);
            return null; 
        }
            

        if (formatConvertedFile.exists()) {
            dbgLog.fine("frmtCnvrtdFile:length=" + formatConvertedFile.length());
        } else {
            dbgLog.warning("Format-converted file was not properly created.");
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
