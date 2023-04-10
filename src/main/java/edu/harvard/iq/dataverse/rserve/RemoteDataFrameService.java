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
package edu.harvard.iq.dataverse.rserve;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.DataAccessRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.apache.commons.io.IOUtils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;

/**
 * 
 * @author Leonid Andreev
 * (the name is still tentative!)
 * parts of the code are borrowed from Akio Sone's DvnRforeignFileConversionServiceImpl,
 * developed for DVN v.2.*
 * 
 * original author:
 * @author Akio Sone
 */

public class RemoteDataFrameService {

    // ----------------------------------------------------- static filelds
    
    private static Logger logger = Logger.getLogger(RemoteDataFrameService.class.getPackage().getName());


    private static String TMP_DATA_FILE_NAME = "dataverseTabData_";
    private static String RWRKSP_FILE_PREFIX = "dataverseDataFrame_";
    private static String PREPROCESS_FILE_PREFIX = "dataversePreprocess_";

    private static String TMP_TABDATA_FILE_EXT = ".tab";
    private static String TMP_RDATA_FILE_EXT = ".RData";
    
    // These settings have sane defaults in resources/META-INF/microprofile-config.properties,
    // ready to be overridden by a sysadmin
    private final String RSERVE_HOST;
    private final String RSERVE_USER;
    private final String RSERVE_PWD;
    private final int    RSERVE_PORT;
    private final String RSERVE_TMP_DIR;
        
    private static String DATAVERSE_R_FUNCTIONS = "scripts/dataverse_r_functions.R";
    private static String DATAVERSE_R_PREPROCESSING = "scripts/preprocess.R";
    
    public String PID = null;
    public String tempFileNameIn = null;
    public String tempFileNameOut = null;

    public RemoteDataFrameService() {
        // These settings have sane defaults in resources/META-INF/microprofile-config.properties,
        // ready to be overridden by a sysadmin. Config sources have their own caches, so adding
        // these here means the setting can be changed dynamically without too much overhead.
        this.RSERVE_HOST = JvmSettings.RSERVE_HOST.lookup();
        this.RSERVE_USER = JvmSettings.RSERVE_USER.lookup();
        this.RSERVE_PWD = JvmSettings.RSERVE_PASSWORD.lookup();
        this.RSERVE_PORT = JvmSettings.RSERVE_PORT.lookup(Integer.class);
        this.RSERVE_TMP_DIR = JvmSettings.RSERVE_TEMPDIR.lookup();
        
        
        // initialization
        PID = RandomStringUtils.randomNumeric(6);

        tempFileNameIn = RSERVE_TMP_DIR + "/" + TMP_DATA_FILE_NAME
                + "." + PID + TMP_TABDATA_FILE_EXT;

        tempFileNameOut = RSERVE_TMP_DIR + "/" + RWRKSP_FILE_PREFIX
                + "." + PID + TMP_RDATA_FILE_EXT;

        logger.fine("tempFileNameIn=" + tempFileNameIn);
        logger.fine("tempFileNameOut=" + tempFileNameOut);

    }
    
    public Map<String, String> directConvert(File originalFile, String fmt){
        
        Map<String, String> result = new HashMap<>();
        try {
            RConnection connection = setupConnection();
            // send the data file to the Rserve side:
            InputStream inFile = new BufferedInputStream(new FileInputStream(originalFile));

            RFileOutputStream rOutFile = connection.createFile(tempFileNameIn);
            copyWithBuffer(inFile, rOutFile, 1024);
                        
            // We need to initialize our R session:
            // send custom R code library over to the Rserve and load the code:
            String rscript = readLocalResource(DATAVERSE_R_FUNCTIONS);
            connection.voidEval(rscript);
            
            String dataFileName = "Data." + PID + ".RData";
            
            // data file to be copied back to the dvn
            String dsnprfx = RSERVE_TMP_DIR + "/" + dataFileName;
            
            String command = "direct_export(file='"+tempFileNameIn+"'," +
                             "fmt='" + fmt + "'" + ", dsnprfx='" + dsnprfx + "')";
                        
            connection.voidEval(command);
            
            int wbFileSize = getFileSize(connection, dsnprfx);
            File localDataFrameFile = transferRemoteFile(connection, dsnprfx, RWRKSP_FILE_PREFIX,"RData", wbFileSize);
            
            if (localDataFrameFile != null){
                logger.fine("data frame file name: "+localDataFrameFile.getAbsolutePath());
                result.put("dataFrameFileName",localDataFrameFile.getAbsolutePath());
            } else {
                logger.fine("data frame file is null!");
                // throw an exception??
            }
            
            result.put("Rversion", connection.eval("R.Version()$version.string").asString());
            
            logger.fine("result object (before closing the Rserve):\n"+result);
            
            String deleteLine = "file.remove('"+tempFileNameIn+"')";
            connection.eval(deleteLine);
 
            connection.close();
        
        } catch (IOException | REXPMismatchException | RserveException e) {
            logger.severe(e.getMessage());
            result.put("RexecError", "true");
        }
        
        return result;
    }
    
    /*
     * Execute a data frame creation process:
     * 
     * (TODO: describe the process here; -- L.A. 4.0 alpha 1)
     *
     * @param sro    a RJobRequest object that contains various parameters
     * @return    a Map that contains various information about results
    
     * TODO: replace this Map with a dedicated RJobResult object; -- L.A. 4.0 alpha 1
     */    
    
    public Map<String, String> execute(RJobRequest jobRequest) {
        logger.fine("RemoteDataFrameService: execute() starts here.");
    
        Map<String, String> result = new HashMap<>();
        
        try {
            RConnection connection = setupConnection();
            // send the data file to the Rserve side:
            InputStream inFile = new BufferedInputStream(new FileInputStream(
                                     jobRequest.getTabularDataFileName()));

            RFileOutputStream rOutFile = connection.createFile(tempFileNameIn);
            copyWithBuffer(inFile, rOutFile, 1024);
            
            // Rserve code starts here
            logger.fine("wrkdir="+RSERVE_TMP_DIR);
                        
            // We need to initialize our R session:
            // send custom R code library over to the Rserve and load the code:
            String rscript = readLocalResource(DATAVERSE_R_FUNCTIONS);
            connection.voidEval(rscript);
            logger.fine("raw variable type="+Arrays.toString(jobRequest.getVariableTypes()));
            connection.assign("vartyp", new REXPInteger(jobRequest.getVariableTypes()));
        
            // variable *formats* - not to be confused with variable *types*!
            // these specify extra, optional format specifications - for example, 
            // String variables may represent date and time values. 
            
            Map<String, String> varFormat = jobRequest.getVariableFormats();
            
            logger.fine("tmpFmt="+varFormat);
            
            // In the fragment below we create an R list varFrmt storing 
            // these format specifications: 
            
            if (varFormat != null){
                String[] formatKeys = varFormat.keySet().toArray(new String[varFormat.size()]);
                String[] formatValues = getValueSet(varFormat, formatKeys);
                connection.assign("tmpfk", new REXPString(formatKeys));
                connection.assign("tmpfv", new REXPString(formatValues));
                String fmtNamesLine = "names(tmpfv)<- tmpfk";
                connection.voidEval(fmtNamesLine);
                String fmtValuesLine ="varFmt<- as.list(tmpfv)";
                connection.voidEval(fmtValuesLine);
            } else {
                connection.assign("varFmt", new REXPList(new RList(new ArrayList<>(),
                                                                   new String[]{})));
            }
            
            // Variable names:
            String [] jvnamesRaw = jobRequest.getVariableNames();
            String [] jvnames;
            
            if (jobRequest.hasUnsafeVariableNames){
                // create  list
                jvnames =  jobRequest.safeVarNames;
                logger.fine("renamed="+StringUtils.join(jvnames,","));
            } else {
                jvnames = jvnamesRaw;
            }
            
            connection.assign("vnames", new REXPString(jvnames));
            
            // confirm:
            
            String [] tmpjvnames = connection.eval("vnames").asStrings();
            logger.fine("vnames:"+ StringUtils.join(tmpjvnames, ","));
            
           
            // read.dataverseTabData method, from dataverse_r_functions.R, 
            // uses R's standard scan() function to read the tabular data we've 
            // just transfered over and turn it into a dataframe. It adds some 
            // custom post-processing too - restores missing values, converts 
            // strings representing dates and times into R date and time objects, 
            // and more. 
            
            // Parameters for the read.dataverseTabData method executed on the R side:
            
            // file -> tempFileName
            // col.names -> Arrays.deepToString(new REXPString(jvnames)).asStrings())
            // colClassesx -> Arrays.deepToString((new REXPInteger(sro.getVariableTypes())).asStrings())
            // varFormat -> Arrays.deepToString((new REXPString(getValueSet(tmpFmt, tmpFmt.keySet().toArray(new String[tmpFmt.keySet().size()])))).asStrings())

            logger.fine("read.dataverseTabData parameters:");
            logger.fine("col.names = " + Arrays.deepToString((new REXPString(jvnames)).asStrings()));
            logger.fine("colClassesx = " + Arrays.deepToString((new REXPInteger(jobRequest.getVariableTypes())).asStrings()));
            logger.fine("varFormat = " + Arrays.deepToString((new REXPString(getValueSet(varFormat, varFormat.keySet().toArray(new String[varFormat.keySet().size()])))).asStrings()));
            
            String readtableline = "x<-read.dataverseTabData(file='"+tempFileNameIn+
                "', col.names=vnames, colClassesx=vartyp, varFormat=varFmt )";
            logger.fine("readtable="+readtableline);

            connection.voidEval(readtableline);
        
            if (jobRequest.hasUnsafeVariableNames){
                logger.fine("unsafeVariableNames exist");
                jvnames = jobRequest.safeVarNames;
                String[] rawNameSet  = jobRequest.renamedVariableArray;
                String[] safeNameSet = jobRequest.renamedResultArray;
                
                connection.assign("tmpRN", new REXPString(rawNameSet));
                connection.assign("tmpSN", new REXPString(safeNameSet));
                
                String raw2safevarNameTableLine = "names(tmpRN)<- tmpSN";
                connection.voidEval(raw2safevarNameTableLine);
                String attrRsafe2rawLine = "attr(x, 'Rsafe2raw')<- as.list(tmpRN)";
                connection.voidEval(attrRsafe2rawLine);
            } else {
                String attrRsafe2rawLine = "attr(x, 'Rsafe2raw')<-list();";
                connection.voidEval(attrRsafe2rawLine);
            }
            
            // Restore NAs (missign values) in the data frame:
            // (these are encoded as empty strings in dataverse tab files)
            // Why are we doing it here? And not in the dataverse_r_functions.R 
            // fragment? 
            
            String asIsline  = "for (i in 1:dim(x)[2]){ "+
                "if (attr(x,'var.type')[i] == 0) {" +
                "x[[i]]<-I(x[[i]]);  x[[i]][ x[[i]] == '' ]<-NA  }}";
            connection.voidEval(asIsline);
            
            String[] varLabels = jobRequest.getVariableLabels();
             
            connection.assign("varlabels", new REXPString(varLabels));
            
            String attrVarLabelsLine = "attr(x, 'var.labels')<-varlabels";
            connection.voidEval(attrVarLabelsLine);
            
            // Confirm:
            String [] vlbl = connection.eval("attr(x, 'var.labels')").asStrings();
            logger.fine("varlabels="+StringUtils.join(vlbl, ","));
        
            // create the VALTABLE and VALORDER lists:
            connection.voidEval("VALTABLE<-list()");
            connection.voidEval("VALORDER<-list()");

            // In the fragment below, we'll populate the VALTABLE list that we've
            // just created with the actual values and labels of our categorical varaibles.
            // TODO: 
            // This code has been imported from the DVN v2-3
            // implementation. I keep wondering if there is a simpler way to
            // achive this - to pass these maps of values and labels to R 
            // in fewer steps/with less code - ?
            // -- L.A. 4.3
            
            Map<String, Map<String, String>> valueTable = jobRequest.getValueTable();
            Map<String, List<String>> orderedCategoryValues = jobRequest.getCategoryValueOrders();
            String[] variableIds = jobRequest.getVariableIds();

            for (int j = 0; j < variableIds.length; j++) {
                // if this variable has a value-label table,
                // pass its key and value arrays to Rserve;
                // finalize a value-table on the Rserve side:

                String varId = variableIds[j];

                if (valueTable.containsKey(varId)) {

                    Map<String, String> tmp = valueTable.get(varId);
                    Set<String> variableKeys = tmp.keySet();
                    String[] tmpk = variableKeys.toArray(new String[variableKeys.size()]);
                    String[] tmpv = getValueSet(tmp, tmpk);

                    logger.fine("tmp:k=" + StringUtils.join(tmpk, ","));
                    logger.fine("tmp:v=" + StringUtils.join(tmpv, ","));

                    if (tmpv.length > 0) {
                        connection.assign("tmpk", new REXPString(tmpk));
                        connection.assign("tmpv", new REXPString(tmpv));

                        String namesValueLine = "names(tmpv)<- tmpk";
                        connection.voidEval(namesValueLine);

                        // index number starts from 1(not 0):
                        String sbvl = "VALTABLE[['" + (j + 1) + "']]" + "<- as.list(tmpv)";
                        logger.fine("frag=" + sbvl);
                        connection.voidEval(sbvl);

                        // confirmation test for j-th variable name
                        REXP jl = connection.parseAndEval(sbvl);
                        logger.fine("jl(" + j + ") = " + jl);
                    }
                }
                
                // If this is an ordered categorical value (and that means,
                // it was produced from an ordered factor, from an ingested 
                // R data frame, since no other formats we support have 
                // ordered categoricals), we'll also supply a list of these
                // ordered values:
                
                
                if (orderedCategoryValues != null && orderedCategoryValues.containsKey(varId)) {
                    List<String> orderList = orderedCategoryValues.get(varId);
                    if (orderList != null) {
                        String[] ordv = (String[]) orderList.toArray(new String[orderList.size()]);
                        logger.fine("ordv="+ StringUtils.join(ordv,","));
                        connection.assign("ordv", new REXPString(ordv));
                        String sbvl = "VALORDER[['"+ Integer.toString(j + 1)+"']]" + "<- as.list(ordv)";
                        logger.fine("VALORDER[...]="+sbvl);
                        connection.voidEval(sbvl);
                    } else {
                        logger.fine("NULL orderedCategoryValues list.");
                    }
                }
            }

            // And now we store the VALTABLE and MSVLTBL as attributes of the 
            // dataframe we are cooking:
            logger.fine("length of vl=" + connection.eval("length(VALTABLE)").asInteger());
            String attrValTableLine = "attr(x, 'val.table')<-VALTABLE";
            connection.voidEval(attrValTableLine);
 
            String msvStartLine = "MSVLTBL<-list();";
            connection.voidEval(msvStartLine);
            String attrMissvalLine = "attr(x, 'missval.table')<-MSVLTBL";
            connection.voidEval(attrMissvalLine);
            
            // But we are not done, with these value label maps... We now need
            // to call these methods from the dataverse_r_functions.R script
            // to further process the lists. Among other things, they will 
            // create these new lists - value index and missing value index, that 
            // simply indicate which variables have any of the above; these will 
            // also be saved as attributes of the data frame, val.index and 
            // missval.index respectively. But, also, the methods will reprocess
            // and overwite the val.table and missval.table attributes already stored in 
            // the dataframe. I don't fully understand why that is necessary, or what it is
            // that we are actually adding to the lists there... Another TODO: ? 
            
            
            String createVIndexLine = "x<-createvalindex(dtfrm=x, attrname='val.index');";
            connection.voidEval(createVIndexLine);
            String createMVIndexLine = "x<-createvalindex(dtfrm=x, attrname='missval.index');";
            connection.voidEval(createMVIndexLine);

           
            // And now we'll call the last method from the R script - createDataverseDataFrame();
            // It should probably be renamed. The dataframe has already been created. 
            // what this method does, it goes through the frame, and changes the 
            // vectors representing categorical variables to R factors. 
            // For example, if this tabular file was produced from a Stata file 
            // that had a categorical in which "Male" and "Female" were represented 
            // with 0 and 1. In the Dataverse datbase, the string values "Male" and 
            // "Female" are now stored as "categorical value labels". And the column 
            // in the tab file has numeric 1 and 0s. That's what the R
            // dataframe was created from, so it now has a numeric vector of 1s and 0s
            // representing this variable. So in this step we are going 
            // to change this vector into a factor, using the labels and values 
            // that we already passed over via Rserve and stored in the val.table, above. 
            
            // TODO: 
            // I'm going to propose that we go back to what we used to do back in 
            // DVN 2-3.* - instead of giving the user a single dataframe (.RData) 
            // file, provide a zip file, with the data frame, and also a README 
            // file with some documentation explaining how the data frame was 
            // created, and pointing out some potential issues stemming from the 
            // conversion between formats. Converting Stata categoricals into 
            // R factors is one of such issues (if nothing else, do note that 
            // the UNF of the datafile with the column described in the example 
            // above will change, if the resulting R dataframe is reingested! See 
            // the UNF documentation for more info...). We may also make this 
            // download interactive - giving the user some options for how 
            // to handle the conversion (so, another choice would be to convert 
            // the above to a factor of "0" and "1"s), etc. 
            // -- L.A. 4.3
                            
            String dataFileName = "Data." + PID + "." + jobRequest.getFormatRequested();
            
            // data file to be copied back to the dvn
            String dsnprfx = RSERVE_TMP_DIR + "/" + dataFileName;
            
            String dataverseDataFrameCommand = "createDataverseDataFrame(dtfrm=x,"+
                "dwnldoptn='"+jobRequest.getFormatRequested()+"'"+
                ", dsnprfx='"+dsnprfx+"')";
                        
            connection.voidEval(dataverseDataFrameCommand);
            
            int wbFileSize = getFileSize(connection,dsnprfx);
            
            logger.fine("wbFileSize="+wbFileSize);
            
            result.putAll(buildResult(connection, dsnprfx, wbFileSize, result));
        } catch (Exception e) {
            logger.severe(e.getMessage());
            result.put("RexecError", "true");
        }
        
        return result;
        
    }

    private Map<String, String> buildResult(RConnection connection, String dsnprfx, int wbFileSize, Map<String, String> result) throws RserveException, REXPMismatchException {
        // If the above succeeded, the dataframe has been saved on the
        // Rserve side as an .Rdata file. Now we can transfer it back to the
        // dataverse side:
        File localDataFrameFile = transferRemoteFile(connection, dsnprfx, RWRKSP_FILE_PREFIX,"RData", wbFileSize);
        
        if (localDataFrameFile != null){
            logger.fine("data frame file name: "+localDataFrameFile.getAbsolutePath());
            result.put("dataFrameFileName",localDataFrameFile.getAbsolutePath());
        } else {
            logger.warning("data frame file is null!");
            // throw an exception??
        }
        
        result.put("Rversion", connection.eval("R.Version()$version.string").asString());
        
        logger.fine("result object (before closing the Rserve):\n"+result);
        
        String deleteLine = "file.remove('"+tempFileNameIn+"')";
        connection.eval(deleteLine);
        connection.close();
        return result;
    }

    private RConnection setupConnection() throws REXPMismatchException, RserveException {
        // Set up an Rserve connection
        logger.fine("RSERVE_USER="+RSERVE_USER+"[default=rserve]");
        logger.fine("RSERVE_PASSWORD="+RSERVE_PWD+"[default=rserve]");
        logger.fine("RSERVE_PORT="+RSERVE_PORT+"[default=6311]");
        logger.fine("RSERVE_HOST="+RSERVE_HOST);
        RConnection connection = new RConnection(RSERVE_HOST, RSERVE_PORT);
        connection.login(RSERVE_USER, RSERVE_PWD);
        logger.fine(">" + connection.eval("R.version$version.string").asString() + "<");
        // check working directories
        // This needs to be done *before* we try to create any files
        // there!
        setupWorkingDirectory(connection);
        return connection;
    }
    
    public void setupWorkingDirectory(RConnection connection) {
        
        try {
            // check the temp directory; try to create it if it doesn't exist:

            String checkWrkDir = "if (!file_test('-d', '" + RSERVE_TMP_DIR + "')) {dir.create('" + RSERVE_TMP_DIR + "', showWarnings = FALSE, recursive = TRUE);}";

            logger.fine("w permission=" + checkWrkDir);
            connection.voidEval(checkWrkDir);

        } catch (RserveException rse) {
            rse.printStackTrace();
        }
    }
    

    public File runDataPreprocessing(DataFile dataFile) {
        if (!dataFile.isTabularData()) {
            return null;
        }

        File preprocessedDataFile = null; 
        
        try {
            
            // Set up an Rserve connection
            
            RConnection connection = new RConnection(RSERVE_HOST, RSERVE_PORT);

            connection.login(RSERVE_USER, RSERVE_PWD);            
            // check working directories
            // This needs to be done *before* we try to create any files 
            // there!
            setupWorkingDirectory(connection);
            
            // send the tabular data file to the Rserve side:
            
            StorageIO<DataFile> accessObject = DataAccess.getStorageIO(dataFile,
                                                        new DataAccessRequest());
            
            if (accessObject == null) {
                return null; 
            }
            
            accessObject.open();
            InputStream is = accessObject.getInputStream();
            if (is == null) {
                return null; 
            }
                    
            // Create the output stream on the remote, R end: 
            
            RFileOutputStream rOutStream = connection.createFile(tempFileNameIn);   
            

            // before writing out any bytes from the input stream, flush
            // any extra content, such as the variable header for the 
            // subsettable files:
            if (accessObject.getVarHeader() != null) {
                rOutStream.write(accessObject.getVarHeader().getBytes());
            }

            copyWithBuffer(is, rOutStream, 4*8192); 
            
            // Rserve code starts here
            logger.fine("wrkdir="+RSERVE_TMP_DIR);
            
            // Locate the R code and run it on the temp file we've just 
            // created: 
            
            connection.voidEval("library(rjson)");
            String rscript = readLocalResource(DATAVERSE_R_PREPROCESSING);
            logger.fine("preprocessing R code: "+rscript.substring(0,64));
            connection.voidEval(rscript);
            
            String runPreprocessing = "json<-preprocess(filename=\""+ tempFileNameIn +"\")";
            logger.fine("data preprocessing command: "+runPreprocessing);
            connection.voidEval(runPreprocessing);
                        
            // Save the output in a temp file: 
            
            String saveResult = "write(json, file='"+ tempFileNameOut +"')";
            logger.fine("data preprocessing save command: "+saveResult);
            connection.voidEval(saveResult);
            
            // Finally, transfer the saved file back on the application side:
            
            int fileSize = getFileSize(connection,tempFileNameOut);
            preprocessedDataFile = transferRemoteFile(connection, tempFileNameOut, PREPROCESS_FILE_PREFIX, "json", fileSize);
            
            String deleteLine = "file.remove('"+tempFileNameOut+"')";
            connection.eval(deleteLine);
            
            connection.close();
        } catch (Exception ex){
            ex.printStackTrace();
            return null ;
        }

            
        return preprocessedDataFile;
    }

    private void copyWithBuffer(InputStream is, RFileOutputStream rOutStream, int bufSize) throws IOException {
        byte[] buffer = new byte[bufSize];
       bufSize = is.read(buffer);
        while (bufSize != -1) {
            rOutStream.write(buffer, 0, bufSize);
            bufSize = is.read(buffer);
        }
        
        is.close();
        rOutStream.close();
    }
    
    // utilitiy methods:
    
    /**
     * Returns the array of map values, that corresponds to the order of 
     * the keys provided in the keys array.
     * 
     */
    public static String[] getValueSet(Map<String, String> map, String[] keys) {
        String[] result = new String[keys.length];
        for (int i = 0; i<keys.length; i++){
            result[i] = map.get(keys[i]);
        }
        return result;
    }
    
    
    /*
     * the method that does the actual data frame request:
     * (TODO: may not need to be a separate method -- something for the final cleanup ?
     * -- L.A. 4.0 alpha 1)
     */
    public Map<String, String> runDataFrameRequest(RJobRequest jobRequest, RConnection connection){
            
        Map<String, String> sr = new HashMap<>();
                
        try {
            String dataFileName = "Data." + PID + "." + jobRequest.getFormatRequested();
            
            // data file to be copied back to the dvn
            String dsnprfx = RSERVE_TMP_DIR + "/" + dataFileName;
            
            String dataverseDataFrameCommand = "createDataverseDataFrame(dtfrm=x,"+
                "dwnldoptn='"+jobRequest.getFormatRequested()+"'"+
                ", dsnprfx='"+dsnprfx+"')";
                        
            connection.voidEval(dataverseDataFrameCommand);
            
            int wbFileSize = getFileSize(connection,dsnprfx);
            
            logger.fine("wbFileSize="+wbFileSize);
            
        } catch (RserveException rse) {
            rse.printStackTrace();
            sr.put("RexecError", "true");
            return sr;
        }

        sr.put("RexecError", "false");
        return sr;
    }
        
    
    public File transferRemoteFile(RConnection connection, String targetFilename,
            String tmpFilePrefix, String tmpFileExt, int fileSize) {

        // set up a local temp file:
        File tmpResultFile = null;
        RFileInputStream rInStream = null;
        OutputStream outbr = null;
        try {
            tmpResultFile = File.createTempFile(tmpFilePrefix + PID, "."+tmpFileExt);
            outbr = new BufferedOutputStream(new FileOutputStream(tmpResultFile));
            // open the input stream
            rInStream = connection.openFile(targetFilename);
            if (fileSize < 1024 * 1024 * 500) {
                byte[] obuf = new byte[fileSize];
                rInStream.read(obuf);
                outbr.write(obuf, 0, fileSize);
            }
            rInStream.close();
            outbr.close();
            return tmpResultFile;
        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
            logger.fine("FileNotFound exception occurred");
            return tmpResultFile;
        } catch (IOException ie) {
            ie.printStackTrace();
            logger.fine("IO exception occurred");
        } finally {
            if (rInStream != null) {
                try {
                    rInStream.close();
                } catch (IOException e) {

                }
            }

            if (outbr != null) {
                try {
                    outbr.close();
                } catch (IOException e) {

                }
            }

        }
        
        // delete remote file: 
        
        try {
            String deleteLine = "file.remove('"+targetFilename+"')";
            connection.eval(deleteLine);
        } catch (Exception ex) {
            // do nothing.
        }
        
        return tmpResultFile;
    }
    
   
    public int getFileSize(RConnection connection, String targetFilename){
        logger.fine("targetFilename="+targetFilename);
        int fileSize = 0;
        try {
            String fileSizeLine = "round(file.info('"+targetFilename+"')$size)";
            fileSize = connection.eval(fileSizeLine).asInteger();
        } catch (RserveException | REXPMismatchException ex) {
            ex.printStackTrace();
        }
        return fileSize;
    }
    
    private static String readLocalResource(String path) {
        
        logger.fine(String.format("Data Frame Service: readLocalResource: reading local path \"%s\"", path));

        // Get stream
        InputStream resourceStream = RemoteDataFrameService.class.getResourceAsStream(path);
        String resourceAsString = "";

        // Try opening a buffered reader stream
        try {
            resourceAsString = IOUtils.toString(resourceStream, "UTF-8");
            resourceStream.close();
        } catch (IOException ex) {
            logger.warning(String.format("RDATAFileReader: (readLocalResource) resource stream from path \"%s\" was invalid", path));
        }
        return resourceAsString;
    }
}
