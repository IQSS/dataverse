/*
   Copyright (C) 2005-2013, by the President and Fellows of Harvard College.

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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata;


import java.io.*;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.*;
import java.util.logging.*;
import java.util.*;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

// Rosuda Wrappers and Methods for R-calls to Rserve
import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.*;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;

import edu.harvard.iq.dataverse.ingest.plugin.spi.*;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.rserve.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.ArrayUtils;
/**
 * Dataverse 4.0 implementation of <code>TabularDataFileReader</code> for the 
 * RData Binary Format.
 * 
 * Based on the original implementation for DVN v3.*, by Matt Owen (2012-2013),
 * completed by Leonid Andreev in 2013. 
 * 
 * This version is a serious re-write of the plugin, using the new 4.0 
 * ingest plugin architecture. 
 * 
 * original 
 * @author Matthew Owen
 * @author Leonid Andreev
 
 * This implementation uses external R-Scripts to do the bulk of the processing.
 */
public class RDATAFileReader extends TabularDataFileReader {
    
// Date-time things
  public static final String[] FORMATS = { "other", "date", "date-time", "date-time-timezone" };

  // R-ingest recognition files
  private static final String[] FORMAT_NAMES = { "RDATA", "Rdata", "rdata" };
  private static final String[] EXTENSIONS = { "Rdata", "rdata" };
  private static final String[] MIME_TYPE = { "application/x-rlang-transport" };
  
  // R Scripts
  static private String RSCRIPT_CREATE_WORKSPACE = "";
  static private String RSCRIPT_DATASET_INFO_SCRIPT = "";
  static private String RSCRIPT_GET_DATASET = "";
  static private String RSCRIPT_GET_LABELS = "";
  static private String RSCRIPT_WRITE_DVN_TABLE = "";
  
  // RServe static variables
  private final String RSERVE_HOST;
  private final int RSERVE_PORT;
  private final String RSERVE_USER;
  private final String RSERVE_PASSWORD;
  
  // TODO: 
  // we're not using these time/data formats for anything, are we?
  // DATE FORMATS
  private static SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[] {
    new SimpleDateFormat("yyyy-MM-dd")
  };
  
  // TIME FORMATS
  private static SimpleDateFormat[] TIME_FORMATS = new SimpleDateFormat[] {
    // Date-time up to milliseconds with timezone, e.g. 2013-04-08 13:14:23.102 -0500
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z"),
    // Date-time up to milliseconds, e.g. 2013-04-08 13:14:23.102
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"),
    // Date-time up to seconds with timezone, e.g. 2013-04-08 13:14:23 -0500
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),
    // Date-time up to seconds and no timezone, e.g. 2013-04-08 13:14:23
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  };
  
  // Logger
  private static final Logger LOG = Logger.getLogger(RDATAFileReader.class.getPackage().getName());

 
  TabularDataIngest ingesteddata = new TabularDataIngest();
  private DataTable dataTable = new DataTable();
    
  // Process ID, used partially in the generation of temporary directories
  private String mPID;
  
  // Object containing all the informatin for an R-workspace (including
  // temporary directories on and off server)
  private RWorkspace mRWorkspace;
  
  

  // Number formatter
  NumberFormat doubleNumberFormatter = new DecimalFormat();

  // Builds R Requests for an R-server
  private RRequestBuilder mRequestBuilder;
  /*
   * Initialize Static Variables
   * This is primarily to construct the R-Script
   */
  static {
    // Load R Scripts into memory, so that we can run them via R-serve
    RSCRIPT_WRITE_DVN_TABLE = readLocalResource("scripts/write.table.R");
    RSCRIPT_GET_DATASET = readLocalResource("scripts/get.dataset.R");
    RSCRIPT_CREATE_WORKSPACE = readLocalResource("scripts/create.workspace.R");
    RSCRIPT_GET_LABELS = readLocalResource("scripts/get.labels.R");
    RSCRIPT_DATASET_INFO_SCRIPT = readLocalResource("scripts/dataset.info.script.R");
    
    
    LOG.finer("R SCRIPTS AS STRINGS --------------");
    LOG.finer(RSCRIPT_WRITE_DVN_TABLE);
    LOG.finer(RSCRIPT_GET_DATASET);
    LOG.fine(RSCRIPT_CREATE_WORKSPACE);
    LOG.finer(RSCRIPT_GET_LABELS);
    LOG.finer(RSCRIPT_DATASET_INFO_SCRIPT);
    LOG.finer("END OF R SCRIPTS AS STRINGS -------");
   }
  
  /* 
   * TODO: 
   * Switch to the implementation in iq.dataverse.rserve
   * -- L.A. 4.0 alpha 1
  */
  private class RWorkspace {
    public String mParent, mWeb, mDvn, mDsb;
    public File mDataFile, mCsvDataFile;
    public RRequest mRRequest;
    public BufferedInputStream mInStream;
    /**
     * 
     */
    public RWorkspace () {
      mParent = mWeb = mDvn = mDsb = "";
      mDataFile = null;
      mCsvDataFile = null;
      mInStream = null;
    }
    /**
     * Create the Actual R Workspace
     */
    public void create () {
      try {
        LOG.fine("RDATAFileReader: Creating R Workspace");
        RRequestBuilder scriptBuilder = mRequestBuilder.script(RSCRIPT_CREATE_WORKSPACE);
        LOG.fine("got a sript request builder");
        
        RRequest scriptRequest = scriptBuilder.build();
        LOG.fine("script request built.");
        
        /*
        REXP result = mRequestBuilder
                .script(RSCRIPT_CREATE_WORKSPACE)
                .build()
                .eval();
        */
        REXP result = scriptRequest.eval(); 
        
        LOG.fine("evaluated the script");
        
        RList directoryNames = result.asList();
        
        mParent = null; 
        
        if (directoryNames != null) {
            if (directoryNames.at("parent") != null) {
                mParent = directoryNames.at("parent").asString();
            } else {
                LOG.fine("WARNING: directoryNames at \"parent\" is null!");
                if(directoryNames.isEmpty()) {
                    LOG.fine("WARNING: directoryNames is empty!");
                } else {
                    Set<String> dirKeySet = directoryNames.keySet();
                    Iterator iter = dirKeySet.iterator();
                    String key;

                    while (iter.hasNext()) {
                        key = (String) iter.next();
                        LOG.fine("directoryNames list key: "+key);
                    }
                }
            }
            
        } else {
            LOG.fine("WARNING: directoryNames is null!");
        }
        
        LOG.fine(String.format("RDATAFileReader: Parent directory of R Workspace is %s", mParent));
        
        LOG.fine("RDATAFileReader: Creating file handle");
        
        mDataFile = new File(mParent, "data.Rdata");
      }
      catch (Exception E) {
        LOG.warning("RDATAFileReader: Could not create R workspace");
        mParent = mWeb = mDvn = mDsb = "";
      }
    }
    /**
     * Destroy the Actual R Workspace
     */
    public void destroy () {
      String destroyerScript = new StringBuilder("")
              .append(String.format("unlink(\"%s\", TRUE, TRUE)", mParent))
              .toString();
      
      try {
        LOG.fine("RDATAFileReader: Destroying R Workspace");

        mRRequest = mRequestBuilder
                .script(destroyerScript)
                .build();
        
        mRRequest.eval();
        
        LOG.fine("RDATAFileReader: DESTROYED R Workspace");
      }
      catch (Exception ex) {
        LOG.warning("RDATAFileReader: R Workspace was not destroyed");
        LOG.fine(ex.getMessage());
      }
    }
    /**
     * Create the Data File to Use for Analysis, etc.
     */
    public File dataFile (String target, String prefix, int size) {
      
      String fileName = String.format("DVN.dataframe.%s.Rdata", mPID);
      
      mDataFile = new File(mParent, fileName);
                
      RFileInputStream RInStream = null;
      OutputStream outStream = null;
      
      RRequest req = mRequestBuilder.build();
      
      try {
        outStream = new BufferedOutputStream(new FileOutputStream(mDataFile));
        RInStream = req.getRConnection().openFile(target);
        
        if (size < 1024*1024*500) {
          int bufferSize = size;
          byte [] outputBuffer = new byte[bufferSize];
          RInStream.read(outputBuffer);
          outStream.write(outputBuffer, 0, size);
        }
        
        RInStream.close();
        outStream.close();
        return mDataFile;
      }
      catch (FileNotFoundException exc) {
        exc.printStackTrace();
        LOG.warning("RDATAFileReader: FileNotFound exception occurred");
        return mDataFile;
      }
      catch (IOException exc) {
        exc.printStackTrace();
        LOG.warning("RDATAFileReader: IO exception occurred");
      }

      // Close R input data stream
      if (RInStream != null) {
        try {
          RInStream.close();
        }
        catch (IOException exc) {
        }
      }

      // Close output data stream
      if (outStream != null) {
        try {
          outStream.close();
        }
        catch (IOException ex) {
        }
      }
      
      return mDataFile;
    }
    /**
     * Set the stream
     * @param inStream 
     */
    public void stream (BufferedInputStream inStream) {
      mInStream = inStream;
    }
    /**
     * Save the Rdata File Temporarily
     */
    private File saveRdataFile () {
      LOG.fine("RDATAFileReader: Saving Rdata File from Input Stream");
      
      if (mInStream == null) {
        LOG.fine("RDATAFileReader: No input stream was specified. Not writing file and returning NULL");
        return null;
      }
      
      byte [] buffer = new byte [1024];
      int bytesRead = 0;
      RFileOutputStream outStream = null;
      RConnection rServerConnection = null;
      
      try {
        LOG.fine("RDATAFileReader: Opening R connection");
        rServerConnection = new RConnection(RSERVE_HOST, RSERVE_PORT);
        
        LOG.fine("RDATAFileReader: Logging into R connection");
        rServerConnection.login(RSERVE_USER, RSERVE_PASSWORD);
        
        LOG.fine("RDATAFileReader: Attempting to create file");
        outStream = rServerConnection.createFile(mDataFile.getAbsolutePath());
        
        LOG.fine(String.format("RDATAFileReader: File created on server at %s", mDataFile.getAbsolutePath()));
      }
      catch (IOException ex) {
        LOG.warning("RDATAFileReader: Could not create file on R Server");
      }
      catch (RserveException ex) {
        LOG.warning("RDATAFileReader: Could not connect to R Server");
      }
      
      /*
       * Read stream and write to destination file
       */
      try {
        // Read from local file and write to rserver 1kb at a time
        while (mInStream.read(buffer) != -1) {
          outStream.write(buffer);
          bytesRead++;
        }
      }
      catch (IOException ex) {
        LOG.warning("RDATAFileReader: Could not write to file");
        LOG.fine(String.format("Error message: %s", ex.getMessage()));
      }
      catch (NullPointerException ex) {
        LOG.warning("RDATAFileReader: Data file has not been specified");
      }
      
      // Closing R server connection
      if (rServerConnection != null) {
        LOG.fine("RDATAFileReader: Closing R server connection");
        rServerConnection.close();
      }
      
      return mDataFile;
    }
    private File saveCsvFile () {
      // Specify CSV File Location on Server
      mCsvDataFile = new File(mRWorkspace.getRdataFile().getParent(), "data.csv");

      // 
      String csvScript = new StringBuilder("")
        .append("options(digits.secs=3)")
        .append("\n")
        .append(RSCRIPT_WRITE_DVN_TABLE)
        .append("\n")
        .append(String.format("load(\"%s\")", mRWorkspace.getRdataAbsolutePath()))
        .append("\n")
        .append(RSCRIPT_GET_DATASET)
        .append("\n")
        .append(String.format("write.dvn.table(data.set, file=\"%s\")", mCsvDataFile.getAbsolutePath()))
        .toString();
      
      // 
      RRequest csvRequest = mRequestBuilder.build();
      
      LOG.fine(String.format("RDATAFileReader: Attempting to write table to `%s`", mCsvDataFile.getAbsolutePath()));
      csvRequest.script(csvScript).eval();

      return mCsvDataFile;
    }
    /**
     * Return Rdata File Handle on R Server
     * @return File asdasd 
     */
    public File getRdataFile () {
      return mDataFile;
    }
    /**
     * Return Location of Rdata File on R Server
     * @return the file location as a string on the (potentially) remote R server
     */
    public String getRdataAbsolutePath () {
      return mDataFile.getAbsolutePath();
    }
  }
  /**
   * Constructs a <code>RDATAFileReader</code> instance from its "Spi" Class
   * @param originator a <code>StatDataFileReaderSpi</code> object.
   */
  public RDATAFileReader(TabularDataFileReaderSpi originator) {

    super(originator);
    
    // These settings have sane defaults in resources/META-INF/microprofile-config.properties,
    // ready to be overridden by a sysadmin. Every time a file would be read with this file reader,
    // a new reader will be created, reading from the cached config source settings with minimal overhead.
    this.RSERVE_HOST = JvmSettings.RSERVE_HOST.lookup();
    int port;
    try {
      port = JvmSettings.RSERVE_PORT.lookup(Integer.class);
    } catch (IllegalArgumentException e) {
      LOG.log(Level.SEVERE, "Could not parse value for " + JvmSettings.RSERVE_PORT.getScopedKey() + ", defaulting to 6311", e);
      port = 6311;
    }
    this.RSERVE_PORT = port;
    this.RSERVE_USER = JvmSettings.RSERVE_USER.lookup();
    this.RSERVE_PASSWORD = JvmSettings.RSERVE_PASSWORD.lookup();

    LOG.fine("RDATAFileReader: INSIDE RDATAFileReader");

    // Create request builder.
    // This object is used throughout as an RRequest factory
    mRequestBuilder = new RRequestBuilder()
            .host(RSERVE_HOST)
            .port(RSERVE_PORT)
            .user(RSERVE_USER)
            .password(RSERVE_PASSWORD);
    
    // Create R Workspace
    mRWorkspace = new RWorkspace();
    
    mPID = RandomStringUtils.randomNumeric(6);
  }

  private void init() throws IOException {
    doubleNumberFormatter.setGroupingUsed(false);
    doubleNumberFormatter.setMaximumFractionDigits(340);
    
  }
  
  /**
   * Read the Given RData File
   * @param stream a <code>BufferedInputStream</code>.
   * @param ignored
   * @return an <code>TabularDataIngest</code> object
   * @throws java.io.IOException if a reading error occurs.
   */
    @Override
    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {

        init();

        // Create Request object
        LOG.fine("RDATAFileReader: Creating RRequest object from RRequestBuilder object");

        try {
            // Create R Workspace
            mRWorkspace.stream(stream);
            mRWorkspace.create();
            mRWorkspace.saveRdataFile();
            mRWorkspace.saveCsvFile();

            // Copy CSV file to a local, temporary directory
            // Additionally, this sets the "tabDelimitedDataFile" property of the FileInformation
            File localCsvFile = transferCsvFile(mRWorkspace.mCsvDataFile);

            // Generate and save all the information about data set; this creates all 
            // the DataVariable objects, among other things:
            getDataFrameInformation();

            // Read and parse the TAB-delimited file saved by R, above; do the 
            // necessary post-processinga and filtering, and save the resulting 
            // TAB file as tabFileDestination, below. This is the file we'll be 
            // using to calculate the UNF, and for the storage/preservation of the
            // dataset. 
            // IMPORTANT: this must be done *after* the variable metadata has been 
            // created!
            // - L.A. 
            RTabFileParser csvFileReader = new RTabFileParser('\t');
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(localCsvFile), "UTF-8"));

            File tabFileDestination = File.createTempFile("data-", ".tab");
            PrintWriter tabFileWriter = new PrintWriter(tabFileDestination.getAbsolutePath(), "UTF-8");
        
            int lineCount = csvFileReader.read(localBufferedReader, dataTable, tabFileWriter);

            LOG.fine("RDATAFileReader: successfully read "+lineCount+" lines of tab-delimited data.");
        
            dataTable.setUnf("UNF:pending");
        
            ingesteddata.setTabDelimitedFile(tabFileDestination);
            ingesteddata.setDataTable(dataTable);

            // Destroy R workspace
            mRWorkspace.destroy();
        } catch (Exception ex) {
            throw new IOException ("Unknown exception occured during ingest; "+ex.getMessage());
        }

        LOG.fine("RDATAFileReader: Leaving \"read\" function");

        return ingesteddata;
    }
  
  /**
   * Copy Remote File on R-server to a Local Target
   * @param target a target on the remote r-server
   * @return 
   */
  private File transferCsvFile (File target) {
    File destination;
    FileOutputStream csvDestinationStream;
    
    try {
      destination = File.createTempFile("data", ".csv");
      LOG.fine(String.format("RDATAFileReader: Writing local CSV File to `%s`", destination.getAbsolutePath()));
      csvDestinationStream = new FileOutputStream(destination);
    }
    catch (IOException ex) {
      LOG.warning("RDATAFileReader: Could not create temporary file!");
      return null;
    }
    
    try {
      // Open connection to R-serve
      RConnection rServeConnection = new RConnection(RSERVE_HOST, RSERVE_PORT);
      rServeConnection.login(RSERVE_USER, RSERVE_PASSWORD);
      
      // Open file for reading from R-serve
      RFileInputStream rServeInputStream = rServeConnection.openFile(target.getAbsolutePath());
      
      int b;
      
      LOG.fine("RDATAFileReader: Beginning to write to local destination file");
      
      // Read from stream one character at a time
      while ((b = rServeInputStream.read()) != -1) {
        // Write to the *local* destination file
        csvDestinationStream.write(b);
      }
      
      LOG.fine(String.format("RDATAFileReader: Finished writing from destination `%s`", target.getAbsolutePath()));
      LOG.fine(String.format("RDATAFileReader: Finished copying to source `%s`", destination.getAbsolutePath()));
      
      
      LOG.fine("RDATAFileReader: Closing CSVFileReader R Connection");
      rServeConnection.close();
    }
    /*
     * TO DO: Make this error catching more intelligent
     */
    catch (Exception ex) {
    }
    
    return destination;
  }
  
  
    /**
     *
     * Runs an R-script that extracts meta-data from the *original* Rdata
     * object, then parses its output and creates DataVariable objects.
     *
     * @throws IOException if something bad happens?
     */
    private void getDataFrameInformation() {
        LOG.fine("RDATAFileReader: Entering `getDataFrameInformation` function");

        // Store variable names
        String[] variableNames = {};

        String parentDirectory = mRWorkspace.getRdataFile().getParent();

        String fileInfoScript = new StringBuilder("")
                .append(String.format("load(\"%s\")\n", mRWorkspace.getRdataAbsolutePath()))
                .append(String.format("setwd(\"%s\")\n", parentDirectory))
                .append(RSCRIPT_GET_DATASET)
                .append("\n")
                .append(RSCRIPT_DATASET_INFO_SCRIPT)
                .toString();

        try {
            RRequest request = mRequestBuilder.build();
            request.script(fileInfoScript);
            RList fileInformation = request.eval().asList();

            RList metaInfo = fileInformation.at("meta.info").asList();

            int varQnty = 0;
            variableNames = fileInformation.at("varNames").asStrings();

            //mDataTypes = fileInformation.at("dataTypes").asStrings();

            // Initialize variables: 
            List<DataVariable> variableList = new ArrayList<>();

            for (String varName : variableNames) {
                DataVariable dv = new DataVariable(varQnty, dataTable);
                dv.setName(varName);
                dv.setLabel(varName);
                // TODO:
                // Check if variables have real descriptive labels defined, 
                // via the mechanismm provided by that special optional package... 
                // (?) -- L.A.
                variableList.add(dv);

                // variableLabels.put(varName, varName);
                // variableNameList.add(varName);
                varQnty++;
            }

            dataTable.setVarQuantity(new Long(varQnty));
            dataTable.setDataVariables(variableList);
        
            // Get the Variable Meta Data Table while Populating 
            processVariableInfo(metaInfo, dataTable);
      
            
            if (fileInformation.at("caseQnty") != null) {
                int caseQuantity = 0; 
                try {
                    caseQuantity =  fileInformation.at("caseQnty").asInteger();
                } catch (REXPMismatchException rexp) {
                    // bummer! - but not fatal. 
                }
                if (caseQuantity > 0) {
                    dataTable.setCaseQuantity(new Long(caseQuantity));
                }
            }
    }
    catch (REXPMismatchException ex) {
      LOG.warning("RDATAFileReader: Could not put information correctly");
    }
    catch (Exception ex) {
      ex.printStackTrace();
      LOG.warning(ex.getMessage());
    }
    
    
  }

    /**
   * Read a Local Resource and Return Its Contents as a String
   * <code>readLocalResource</code> searches the local path around the class
   * <code>RDATAFileReader</code> for a file and returns its contents as a
   * string.
   * @param path String specifying the name of the local file to be converted
   * into a UTF-8 string.
   * @return a UTF-8 <code>String</code>
   */
    private static String readLocalResource(String path) {
        // Debug
        LOG.fine(String.format("RDATAFileReader: readLocalResource: reading local path \"%s\"", path));

        // Get stream
        InputStream resourceStream = RDATAFileReader.class.getResourceAsStream(path);
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
            LOG.warning(String.format("RDATAFileReader: (readLocalResource) resource stream from path \"%s\" was invalid", path));
        }

        // Return string
        return resourceAsString;
    }

  
    /**
     * Get a HashMap matching column number to meta-data used in re-creating R
     * Objects
     *
     * @param metaInfo an "RList" Object containing indices - type, type.string,
     * class, levels, and format.
     * @param dataTable a dataverse DataTable object
     */
    private void processVariableInfo(RList metaInfo, DataTable dataTable) throws IOException {
        // list(type = 1, type.string = "integer", class = class(values), levels = NULL, format = NULL)
        Integer variableType = -1;
        String variableTypeName = "", variableFormat = "";
        String[] variableLevels = null;


        for (int k = 0; k < metaInfo.size(); k++) {

            try {

                // Meta-data for a column in the data-set
                RList columnMeta = metaInfo.at(k).asList();

                // Extract information from the returned list
                variableType = !columnMeta.at("type").isNull() ? columnMeta.at("type").asInteger() : null;
                variableTypeName = !columnMeta.at("type.string").isNull() ? columnMeta.at("type.string").asString() : null;
                variableLevels = !columnMeta.at("levels").isNull() ? columnMeta.at("levels").asStrings() : new String[0];
                variableFormat = !columnMeta.at("format").isNull() ? columnMeta.at("format").asString() : null;

                LOG.fine("variable type: " + variableType);
                LOG.fine("variable type name: " + variableTypeName);
                LOG.fine("variable format: " + variableFormat);

                for (String variableLevel : variableLevels) {
                    LOG.fine("variable level: " + variableLevel);
                }

                //dataTable.getDataVariables().get(k).setFormatSchema("RDATA");

                if (variableTypeName == null || variableTypeName.equals("character") || variableTypeName.equals("other")) {
                    // This is a String: 
                    dataTable.getDataVariables().get(k).setTypeCharacter();
                    dataTable.getDataVariables().get(k).setIntervalDiscrete();
                    
                } else if (variableTypeName.equals("integer")) {
                    dataTable.getDataVariables().get(k).setTypeNumeric();
                    dataTable.getDataVariables().get(k).setIntervalDiscrete();
                    
                } else if (variableTypeName.equals("numeric") || variableTypeName.equals("double")) {
                    dataTable.getDataVariables().get(k).setTypeNumeric();
                    dataTable.getDataVariables().get(k).setIntervalContinuous();
                    
                } else if (variableTypeName.startsWith("Date")) {
                    dataTable.getDataVariables().get(k).setTypeCharacter();
                    dataTable.getDataVariables().get(k).setIntervalDiscrete();
                    dataTable.getDataVariables().get(k).setFormat(variableFormat);
                    
                    // instead:
                    if (variableTypeName.equals("Date")) {
                        dataTable.getDataVariables().get(k).setFormatCategory("date");
                    } else if (variableTypeName.equals("DateTime")) {
                        dataTable.getDataVariables().get(k).setFormatCategory("time");
                    }
                    
                } else if (variableTypeName.equals("factor")) {
                    
                    // All R factors are *string* factors!
                    dataTable.getDataVariables().get(k).setTypeCharacter();
                    dataTable.getDataVariables().get(k).setIntervalDiscrete();
                    if (variableLevels != null && variableLevels.length > 0) {
                        // yes, this is a factor, with levels defined.
                        LOG.fine("this is a factor.");
                        dataTable.getDataVariables().get(k).setFactor(true);
                        boolean ordered = false; 
                        
                        if (variableFormat != null && variableFormat.equals("ordered")) {
                            LOG.fine("an ordered factor, too");
                            ordered = true;
                        }
                        
                        for (int i = 0; i < variableLevels.length; i++) {
                            VariableCategory cat = new VariableCategory();
                            cat.setValue(variableLevels[i]);
                            // Sadly, R factors don't have descriptive labels;
                            cat.setLabel(variableLevels[i]);
                            
                            if (ordered) {
                                cat.setOrder(i+1);
                            }

                            /* cross-link the variable and category to each other: */
                            cat.setDataVariable(dataTable.getDataVariables().get(k));
                            dataTable.getDataVariables().get(k).getCategories().add(cat);
                        }
                        
                        dataTable.getDataVariables().get(k).setOrderedCategorical(ordered);

                    }

                } // And finally, a special case for logical variables: 
                // For all practical purposes, they are handled as numeric factors
                // with 0 and 1 for the values and "FALSE" and "TRUE" for the labels.
                // (so this can also be used as an example of ingesting a *numeric* 
                // categorical variable - as opposed to *string* categoricals, that
                // we turn R factors into - above.
                else if ("logical".equals(variableTypeName)) {
                    dataTable.getDataVariables().get(k).setFormatCategory("Boolean");
                    
                    dataTable.getDataVariables().get(k).setTypeNumeric();
                    dataTable.getDataVariables().get(k).setIntervalDiscrete();

                    String booleanFactorLabels[] = new String[2];
                    booleanFactorLabels[0] = "FALSE";
                    booleanFactorLabels[1] = "TRUE";

                    String booleanFactorValues[] = new String[2];
                    booleanFactorValues[0] = "0";
                    booleanFactorValues[1] = "1";

                    for (int i = 0; i < 2; i++) {
                        VariableCategory cat = new VariableCategory();
                        cat.setValue(booleanFactorValues[i]);
                        // Sadly, R factors don't have descriptive labels;
                        cat.setLabel(booleanFactorLabels[i]);

                        /* cross-link the variable and category to each other: */
                        cat.setDataVariable(dataTable.getDataVariables().get(k));
                        dataTable.getDataVariables().get(k).getCategories().add(cat);
                    }
                }

                // Store the meta-data in a hashmap (to return later)
            } catch (REXPMismatchException ex) {
                // If something went wrong, then it wasn't meant to be for that column.
                // And you know what? That's okay.
                ex.printStackTrace();
                LOG.fine(String.format("Could not process variable %d of the data frame.", k));
            }
        }
    }
}
