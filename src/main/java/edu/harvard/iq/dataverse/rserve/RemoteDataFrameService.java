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
import edu.harvard.iq.dataverse.dataaccess.DataAccessObject;
import edu.harvard.iq.dataverse.dataaccess.DataAccessRequest;
import java.io.*;
import java.util.*;
import java.util.logging.*;

import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;

import org.apache.commons.lang.*;
import org.apache.commons.lang.builder.*;

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
    
    private static Logger dbgLog = Logger.getLogger(RemoteDataFrameService.class.getPackage().getName());


    private static String TMP_DATA_FILE_NAME = "dataverseTabData_";
    private static String RWRKSP_FILE_PREFIX = "dataverseDataFrame_";
    private static String PREPROCESS_FILE_PREFIX = "dataversePreprocess_";

    private static String TMP_TABDATA_FILE_EXT = ".tab";
    private static String TMP_RDATA_FILE_EXT = ".RData";

    private static String RSERVE_HOST = null;
    private static String RSERVE_USER = null;
    private static String RSERVE_PWD = null;    
    private static int    RSERVE_PORT = -1;
        
    private static String DATAVERSE_R_FUNCTIONS = "scripts/dataverse_r_functions.R";
    private static String DATAVERSE_R_PREPROCESSING = "scripts/preprocess.R";
                    
    public static String LOCAL_TEMP_DIR = System.getProperty("java.io.tmpdir");
    public static String RSERVE_TMP_DIR=null;
    
    public String PID = null;
    public String tempFileNameIn = null;
    public String tempFileNameOut = null;
 
    static {
    
        RSERVE_TMP_DIR = System.getProperty("dataverse.rserve.tempdir");
        
        if (RSERVE_TMP_DIR == null){
            RSERVE_TMP_DIR = "/tmp/";            
        }
        
        RSERVE_HOST = System.getProperty("dataverse.rserve.host");
        if (RSERVE_HOST == null){
            RSERVE_HOST= "localhost";
        }
        
        RSERVE_USER = System.getProperty("dataverse.rserve.user");
        if (RSERVE_USER == null){
            RSERVE_USER= "rserve";
        }
        
        RSERVE_PWD = System.getProperty("dataverse.rserve.password");
        if (RSERVE_PWD == null){
            RSERVE_PWD= "rserve";
        }
        

        if (System.getProperty("dataverse.rserve.port") == null ){
            RSERVE_PORT= 6311;
        } else {
            RSERVE_PORT = Integer.parseInt(System.getProperty("dataverse.rserve.port"));
        }

    }

   

    public RemoteDataFrameService() {
        // initialization
        PID = RandomStringUtils.randomNumeric(6);

        tempFileNameIn = RSERVE_TMP_DIR + "/" + TMP_DATA_FILE_NAME
                + "." + PID + TMP_TABDATA_FILE_EXT;

        tempFileNameOut = RSERVE_TMP_DIR + "/" + RWRKSP_FILE_PREFIX
                + "." + PID + TMP_RDATA_FILE_EXT;

        dbgLog.fine("tempFileNameIn=" + tempFileNameIn);
        dbgLog.fine("tempFileNameOut=" + tempFileNameOut);

    }


    
    public void setupWorkingDirectory(RConnection c) {
        
        try {
            // check the temp directory; try to create it if it doesn't exist:

            String checkWrkDir = "if (!file_test('-d', '" + RSERVE_TMP_DIR + "')) {dir.create('" + RSERVE_TMP_DIR + "', showWarnings = FALSE, recursive = TRUE);}";

            dbgLog.fine("w permission=" + checkWrkDir);
            c.voidEval(checkWrkDir);

        } catch (RserveException rse) {
            rse.printStackTrace();
        }
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
    
    public Map<String, String> execute(RJobRequest sro) {
        dbgLog.fine("RemoteDataFrameService: execute() starts here.");
    
        // set the return object
        Map<String, String> result = new HashMap<String, String>();
        
        try {
            // TODO: 
            // Split the code below into neat individual methods - for 
            // initializing the connection, loading the remote libraries, 
            // creating remote R vectors for the parameters that will be used 
            // to create the data frame - variable names, labels, etc., and 
            // executing the main request and any necessary post-processing
            // -- L.A. 4.0 alpha 1
            
            // Set up an Rserve connection
            dbgLog.fine("sro dump:\n"+ToStringBuilder.reflectionToString(sro, ToStringStyle.MULTI_LINE_STYLE));
            
            dbgLog.fine("RSERVE_USER="+RSERVE_USER+"[default=rserve]");
            dbgLog.fine("RSERVE_PASSWORD="+RSERVE_PWD+"[default=rserve]");
            dbgLog.fine("RSERVE_PORT="+RSERVE_PORT+"[default=6311]");
            dbgLog.fine("RSERVE_HOST="+RSERVE_HOST);


            RConnection c = new RConnection(RSERVE_HOST, RSERVE_PORT);

            c.login(RSERVE_USER, RSERVE_PWD);
            dbgLog.fine(">" + c.eval("R.version$version.string").asString() + "<");
            
            // check working directories
            // This needs to be done *before* we try to create any files 
            // there!
            setupWorkingDirectory(c);
            


            // send the data file to the Rserve side:
            
            String infile = sro.getTabularDataFileName();
            InputStream inb = new BufferedInputStream(
                    new FileInputStream(infile));

            int bufsize;
            byte[] bffr = new byte[1024];

            RFileOutputStream os = c.createFile(tempFileNameIn);
            while ((bufsize = inb.read(bffr)) != -1) {
                    os.write(bffr, 0, bufsize);
            }
            os.close();
            inb.close();
            
            // Rserve code starts here
            dbgLog.fine("wrkdir="+RSERVE_TMP_DIR);
            
            String RversionLine = "R.Version()$version.string";
            String Rversion = c.eval(RversionLine).asString();
                        
            // We need to initialize our R session:
            // send custom R code library over to the Rserve and load the code:
            
            String rscript = readLocalResource(DATAVERSE_R_FUNCTIONS);
            c.voidEval(rscript);
            
            
            dbgLog.fine("raw variable type="+sro.getVariableTypes());
            c.assign("vartyp", new REXPInteger(sro.getVariableTypes()));
            String [] tmpt = c.eval("vartyp").asStrings();
            dbgLog.fine("vartyp length="+ tmpt.length + "\t " +
                StringUtils.join(tmpt,","));
        
            // variable *formats* - not to be confused with variable *types*!
            // these specify extra, optional format specifications - for example, 
            // String variables may represent date and time values. 
            
            Map<String, String> tmpFmt = sro.getVariableFormats();
            
            dbgLog.fine("tmpFmt="+tmpFmt);
            
            // TODO: figure out what's going on in this awful code fragment below!!
            // -- L.A. 4.0 alpha 1
            
            if (tmpFmt != null){
                Set<String> vfkeys = tmpFmt.keySet();
                String[] tmpfk = (String[]) vfkeys.toArray(new String[vfkeys.size()]);
                String[] tmpfv = getValueSet(tmpFmt, tmpfk);
                c.assign("tmpfk", new REXPString(tmpfk));
                c.assign("tmpfv", new REXPString(tmpfv));
                String fmtNamesLine = "names(tmpfv)<- tmpfk";
                c.voidEval(fmtNamesLine);
                String fmtValuesLine ="varFmt<- as.list(tmpfv)";
                c.voidEval(fmtValuesLine);
            } else {
                String [] varFmtN ={};
                List<String> varFmtV = new ArrayList<String>();
                c.assign("varFmt", new REXPList(new RList(varFmtV, varFmtN)));
            }
            
            // Variable names:
            String [] jvnamesRaw = sro.getVariableNames();
            String [] jvnames = null;
            
            
            if (sro.hasUnsafeVariableNames){
                // create  list
                jvnames =  sro.safeVarNames;
                dbgLog.fine("renamed="+StringUtils.join(jvnames,","));
            } else {
                jvnames = jvnamesRaw;
            }
            
            c.assign("vnames", new REXPString(jvnames));
            
            // confirm:
            
            String [] tmpjvnames = c.eval("vnames").asStrings();
            dbgLog.fine("vnames:"+ StringUtils.join(tmpjvnames, ","));
            
            
            // Parameters for the read.dataverseTabData method executed on the R side:
            
            // file -> tempFileName
            // col.names -> Arrays.deepToString(new REXPString(jvnames)).asStrings())
            // colClassesx -> Arrays.deepToString((new REXPInteger(sro.getVariableTypes())).asStrings())
            // varFormat -> Arrays.deepToString((new REXPString(getValueSet(tmpFmt, tmpFmt.keySet().toArray(new String[tmpFmt.keySet().size()])))).asStrings())

            dbgLog.fine("read.dataverseTabData parameters:");
            dbgLog.fine("col.names = " + Arrays.deepToString((new REXPString(jvnames)).asStrings()));
            dbgLog.fine("colClassesx = " + Arrays.deepToString((new REXPInteger(sro.getVariableTypes())).asStrings()));
            dbgLog.fine("varFormat = " + Arrays.deepToString((new REXPString(getValueSet(tmpFmt, tmpFmt.keySet().toArray(new String[tmpFmt.keySet().size()])))).asStrings()));
            
            String readtableline = "x<-read.dataverseTabData(file='"+tempFileNameIn+
                "', col.names=vnames, colClassesx=vartyp, varFormat=varFmt )";
            dbgLog.fine("readtable="+readtableline);

            c.voidEval(readtableline);
        
            if (sro.hasUnsafeVariableNames){
                dbgLog.fine("unsafeVariableNames exist");
                jvnames = sro.safeVarNames;
                String[] rawNameSet  = sro.renamedVariableArray;
                String[] safeNameSet = sro.renamedResultArray;
                
                c.assign("tmpRN", new REXPString(rawNameSet));
                c.assign("tmpSN", new REXPString(safeNameSet));
                
                String raw2safevarNameTableLine = "names(tmpRN)<- tmpSN";
                c.voidEval(raw2safevarNameTableLine);
                String attrRsafe2rawLine = "attr(x, 'Rsafe2raw')<- as.list(tmpRN)";
                c.voidEval(attrRsafe2rawLine);
            } else {
                String attrRsafe2rawLine = "attr(x, 'Rsafe2raw')<-list();";
                c.voidEval(attrRsafe2rawLine);
            }
            
            // Restore NAs (missign values) in the data frame:
            // (these are encoded as empty strings in dataverse tab files)
            
            String asIsline  = "for (i in 1:dim(x)[2]){ "+
                "if (attr(x,'var.type')[i] == 0) {" +
                "x[[i]]<-I(x[[i]]);  x[[i]][ x[[i]] == '' ]<-NA  }}";
            c.voidEval(asIsline);
            
            String[] varLabels = sro.getVariableLabels();
             
            c.assign("varlabels", new REXPString(varLabels));
            
            String attrVarLabelsLine = "attr(x, 'var.labels')<-varlabels";
            c.voidEval(attrVarLabelsLine);
            
            // Confirm:
            String [] vlbl = c.eval("attr(x, 'var.labels')").asStrings();
            dbgLog.fine("varlabels="+StringUtils.join(vlbl, ","));
        
            // create the VALTABLE
            String vtFirstLine = "VALTABLE<-list()";
            c.voidEval(vtFirstLine);

            // TODO: 
            // Again, the fragment below is being imported from the DVN v2-3
            // implementation... Need to figure out what's going on in the 
            // awfulness there and clean it up!
            // -- L.A. 4.0 alpha 1
            Map<String, Map<String, String>> vltbl = sro.getValueTable();
            String[] variableIds = sro.getVariableIds();

            for (int j = 0; j < variableIds.length; j++) {
                // if this variable has a value-label table,
                // pass its key and value arrays to Rserve;
                // finalize a value-table on the Rserve side:

                String varId = variableIds[j];

                if (vltbl.containsKey(varId)) {

                    Map<String, String> tmp = (HashMap<String, String>) vltbl.get(varId);
                    Set<String> vlkeys = tmp.keySet();
                    String[] tmpk = (String[]) vlkeys.toArray(new String[vlkeys.size()]);
                    String[] tmpv = getValueSet(tmp, tmpk);

                    dbgLog.fine("tmp:k=" + StringUtils.join(tmpk, ","));
                    dbgLog.fine("tmp:v=" + StringUtils.join(tmpv, ","));

                    // index number starts from 1(not 0):
                    int indx = j + 1;
                    dbgLog.fine("index=" + indx);

                    if (tmpv.length > 0) {

                        c.assign("tmpk", new REXPString(tmpk));

                        c.assign("tmpv", new REXPString(tmpv));

                        String namesValueLine = "names(tmpv)<- tmpk";
                        c.voidEval(namesValueLine);

                        String sbvl = "VALTABLE[['" + Integer.toString(indx) + "']]" + "<- as.list(tmpv)";
                        dbgLog.fine("frag=" + sbvl);
                        c.voidEval(sbvl);

                        // confirmation test for j-th variable name
                        REXP jl = c.parseAndEval(sbvl);
                        dbgLog.fine("jl(" + j + ") = " + jl);
                    }
                }
            }

            // debug: confirmation test for value-table
            dbgLog.fine("length of vl=" + c.eval("length(VALTABLE)").asInteger());
            String attrValTableLine = "attr(x, 'val.table')<-VALTABLE";
            c.voidEval(attrValTableLine);
            
            
            // missing-value list: TO DO (DO WE STILL NEED IT?? -- L.A. 4.0 alpha 1)
            /*
                MSVLTBL<-list(); attr(x, 'missval.table')<-MSVLTBL
            */
            String msvStartLine = "MSVLTBL<-list();";
            c.voidEval(msvStartLine);
            // data structure
            String attrMissvalLine = "attr(x, 'missval.table')<-MSVLTBL";
            c.voidEval(attrMissvalLine);
            
            String createVIndexLine = "x<-createvalindex(dtfrm=x, attrname='val.index');";
            c.voidEval(createVIndexLine);
            String createMVIndexLine = "x<-createvalindex(dtfrm=x, attrname='missval.index');";
            c.voidEval(createMVIndexLine);

           
            // invoke the data frame creation method: 
                            
            String dataFileName = "Data." + PID + "." + sro.getFormatRequested();
            
            // data file to be copied back to the dvn
            String dsnprfx = RSERVE_TMP_DIR + "/" + dataFileName;
            
            String dataverseDataFrameCommand = "createDataverseDataFrame(dtfrm=x,"+
                "dwnldoptn='"+sro.getFormatRequested()+"'"+
                ", dsnprfx='"+dsnprfx+"')";
                        
            c.voidEval(dataverseDataFrameCommand);
            
            int wbFileSize = getFileSize(c,dsnprfx);
            
            dbgLog.fine("wbFileSize="+wbFileSize);
            
            // save workspace:
            
            String saveWS = "save('x', file='"+ tempFileNameOut +"')";
            dbgLog.fine("save workspace command: "+saveWS);
            c.voidEval(saveWS);
            
            // Transfer the saved dataframe workspace back to the DVN:
                        
            dbgLog.fine("wrkspFileName="+tempFileNameOut);
            
            int wrkspflSize = getFileSize(c,tempFileNameOut);
            
            File localDataFrameFile = transferRemoteFile(c, tempFileNameOut, RWRKSP_FILE_PREFIX,"RData", wrkspflSize);
            
            result.put("dataFrameFileName",localDataFrameFile.getAbsolutePath());
            
            if (localDataFrameFile != null){
                dbgLog.fine("data frame file name: "+localDataFrameFile.getAbsolutePath());
            } else {
                dbgLog.fine("data frame file is null!");
            }
            
            
            result.put("Rversion", Rversion);
            
            dbgLog.fine("result object (before closing the Rserve):\n"+result);
            
            String deleteLine = "file.remove('"+tempFileNameIn+"')";
            c.eval(deleteLine);
 
            c.close();
        
        } catch (RserveException rse) {
            // RserveException (Rserve is not running maybe?)
            // TODO: *ABSOLUTELY* need more diagnostics here!
            rse.printStackTrace();            
            result.put("RexecError", "true");
            return result;

        } catch (REXPMismatchException mme) {
            mme.printStackTrace();
            result.put("RexecError", "true");
            return result;

        } catch (IOException ie){
            ie.printStackTrace();
            result.put("RexecError", "true");
            return result;
            
        } catch (Exception ex){
            ex.printStackTrace();
            result.put("RexecError", "true");
            return result;
        }
        
        return result;
        
    }
    

    public File runDataPreprocessing(DataFile dataFile) {
        if (!dataFile.isTabularData()) {
            return null;
        }

        File preprocessedDataFile = null; 
        
        try {
            
            // Set up an Rserve connection
            
            RConnection c = new RConnection(RSERVE_HOST, RSERVE_PORT);

            c.login(RSERVE_USER, RSERVE_PWD);            
            // check working directories
            // This needs to be done *before* we try to create any files 
            // there!
            setupWorkingDirectory(c);
            
            // send the tabular data file to the Rserve side:
            
            DataAccessRequest daReq = new DataAccessRequest();
            DataAccessObject accessObject = DataAccess.createDataAccessObject(dataFile, daReq);
            
            if (accessObject == null) {
                return null; 
            }
            
            accessObject.open();
            InputStream is = accessObject.getInputStream();
            if (is == null) {
                return null; 
            }
                    
            // Create the output stream on the remote, R end: 
            
            RFileOutputStream os = c.createFile(tempFileNameIn);   
            
            int bufsize;
            byte[] bffr = new byte[4 * 8192];

            // before writing out any bytes from the input stream, flush
            // any extra content, such as the variable header for the 
            // subsettable files:
            if (accessObject.getVarHeader() != null) {
                os.write(accessObject.getVarHeader().getBytes());
            }

            while ((bufsize = is.read(bffr)) != -1) {
                os.write(bffr, 0, bufsize);
            }

            is.close();
            os.close(); 
            
            // Rserve code starts here
            dbgLog.fine("wrkdir="+RSERVE_TMP_DIR);
            
            // Locate the R code and run it on the temp file we've just 
            // created: 
            
            String loadlib = "library(rjson)";
            c.voidEval(loadlib);
            String rscript = readLocalResource(DATAVERSE_R_PREPROCESSING);
            dbgLog.fine("preprocessing R code: "+rscript.substring(0,64));
            c.voidEval(rscript);
            
            String runPreprocessing = "json<-preprocess(filename=\""+ tempFileNameIn +"\")";
            dbgLog.fine("data preprocessing command: "+runPreprocessing);
            c.voidEval(runPreprocessing);
                        
            // Save the output in a temp file: 
            
            String saveResult = "write(json, file='"+ tempFileNameOut +"')";
            dbgLog.fine("data preprocessing save command: "+saveResult);
            c.voidEval(saveResult);
            
            // Finally, transfer the saved file back on the application side:
            
            int fileSize = getFileSize(c,tempFileNameOut);
            preprocessedDataFile = transferRemoteFile(c, tempFileNameOut, PREPROCESS_FILE_PREFIX, "json", fileSize);
            
            String deleteLine = "file.remove('"+tempFileNameOut+"')";
            c.eval(deleteLine);
            
            c.close();
            
        } catch (RserveException rse) {
            // RserveException (Rserve is not running maybe?)
            // TODO: *ABSOLUTELY* need more diagnostics here!
            rse.printStackTrace();            
            return null;

        } catch (Exception ex){
            ex.printStackTrace();
            return null ;
        }

            
        return preprocessedDataFile;
    }
    
    // utilitiy methods:
    
    /**
     * Returns the array of map values, that corresponds to the order of 
     * the keys provided in the keys array.
     * 
     */

    public static String[] getValueSet(Map<String, String> mp, String[] keys) {
        
        List<String> tmpvl = new ArrayList<String>();
        for (int i=0; i< keys.length; i++){
            tmpvl.add(mp.get(keys[i]));
        }
        String[] tmpv = (String[])tmpvl.toArray(new String[tmpvl.size()]);
        return tmpv;
    }
    
    
    /*
     * the method that does the actual data frame request:
     * (TODO: may not need to be a separate method -- something for the final cleanup ?
     * -- L.A. 4.0 alpha 1)
     */
    public Map<String, String> runDataFrameRequest(RJobRequest sro, RConnection c){
            
        Map<String, String> sr = new HashMap<String, String>();
                
        try {
            String dataFileName = "Data." + PID + "." + sro.getFormatRequested();
            
            // data file to be copied back to the dvn
            String dsnprfx = RSERVE_TMP_DIR + "/" + dataFileName;
            
            String dataverseDataFrameCommand = "createDataverseDataFrame(dtfrm=x,"+
                "dwnldoptn='"+sro.getFormatRequested()+"'"+
                ", dsnprfx='"+dsnprfx+"')";
                        
            c.voidEval(dataverseDataFrameCommand);
            
            int wbFileSize = getFileSize(c,dsnprfx);
            
            dbgLog.fine("wbFileSize="+wbFileSize);
            
        } catch (RserveException rse) {
            rse.printStackTrace();
            sr.put("RexecError", "true");
            return sr;
        }

        sr.put("RexecError", "false");
        return sr;
    }
        
    
    public File transferRemoteFile(RConnection c, String targetFilename,
            String tmpFilePrefix, String tmpFileExt, int fileSize) {

        // set up a local temp file: 
        
        File tmprsltfl = null;
        String resultFile = tmpFilePrefix + PID + "." + tmpFileExt;

        RFileInputStream ris = null;
        OutputStream outbr = null;
        try {
            tmprsltfl = new File(LOCAL_TEMP_DIR, resultFile);
            outbr = new BufferedOutputStream(new FileOutputStream(tmprsltfl));
            // open the input stream
            ris = c.openFile(targetFilename);

            if (fileSize < 1024 * 1024 * 500) {
                int bfsize = fileSize;
                byte[] obuf = new byte[bfsize];
                ris.read(obuf);
                outbr.write(obuf, 0, bfsize);
            }
            ris.close();
            outbr.close();
            return tmprsltfl;
        } catch (FileNotFoundException fe) {
            fe.printStackTrace();
            dbgLog.fine("FileNotFound exception occurred");
            return tmprsltfl;
        } catch (IOException ie) {
            ie.printStackTrace();
            dbgLog.fine("IO exception occurred");
        } finally {
            if (ris != null) {
                try {
                    ris.close();
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
            c.eval(deleteLine);
        } catch (Exception ex) {
            // do nothing.
        }
        
        return tmprsltfl;
    }
    
   
    public int getFileSize(RConnection c, String targetFilename){
        dbgLog.fine("targetFilename="+targetFilename);
        int fileSize = 0;
        try {
            String fileSizeLine = "round(file.info('"+targetFilename+"')$size)";
            fileSize = c.eval(fileSizeLine).asInteger();
        } catch (RserveException rse) {
            rse.printStackTrace();
        } catch (REXPMismatchException mme) {
            mme.printStackTrace();
        }
        return fileSize;
    }
    
    private static String readLocalResource(String path) {
        
        dbgLog.info(String.format("Data Frame Service: readLocalResource: reading local path \"%s\"", path));

        // Get stream
        InputStream resourceStream = RemoteDataFrameService.class.getResourceAsStream(path);
        String resourceAsString = "";

        // Try opening a buffered reader stream
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(resourceStream, "UTF-8"));

            String line = null;
            while ((line = rd.readLine()) != null) {
                resourceAsString = resourceAsString.concat(line + "\n");
            }
            resourceStream.close();
        } catch (IOException ex) {
            dbgLog.warning(String.format("RDATAFileReader: (readLocalResource) resource stream from path \"%s\" was invalid", path));
        }

        // Return string
        return resourceAsString;
    }
}
