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

package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;



import java.io.*;
import java.nio.*;
import java.util.logging.*;

import java.util.*;
import java.util.regex.*;
import java.text.*;


import org.apache.commons.lang.*;
import org.apache.commons.codec.binary.Hex;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.datavariable.VariableFormatType;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;

import edu.harvard.iq.dataverse.ingest.plugin.spi.*;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;


/**
 * ingest plugin for Stata DTA file format.
 *
 * This reader plugin has been fully re-implemented for the DVN 4.0;
 * It is still borrows heavily from, and builds on the basis of the 
 * old implementation by Akio Sone, that was in use in the versions 
 * 2-3 of the DVN.
 * 
 * @author Akio Sone at UNC-Odum
 * @author landreev
 */

public class DTAFileReader extends TabularDataFileReader{
    @Inject
    VariableServiceBean varService;
    // static fields, STATA-specific constants, etc. 
    // (should it all be isolated in some other class?) 

    private static Map<Integer, String> STATA_RELEASE_NUMBER = 
            new HashMap<Integer, String>();
    private static Map<String, Integer> release105type = new LinkedHashMap<String, Integer>();
    private static Map<String, Integer> release111type = new LinkedHashMap<String, Integer>();

    private static Map<Integer, Map<String, Integer>> CONSTATNT_TABLE =
            new LinkedHashMap<Integer, Map<String, Integer>>();

    private static Map<String, Integer> release104constant =
                                        new LinkedHashMap<String, Integer>();
                                        
    private static Map<String, Integer> release105constant =
                                        new LinkedHashMap<String, Integer>();
                                        
    private static Map<String, Integer> release108constant =
                                        new LinkedHashMap<String, Integer>();
                                        
    private static Map<String, Integer> release110constant =
                                        new LinkedHashMap<String, Integer>();
                                        
    private static Map<String, Integer> release111constant =
                                        new LinkedHashMap<String, Integer>();
                                        
    private static Map<String, Integer> release113constant =
                                        new LinkedHashMap<String, Integer>();
                                        
    private static Map<String, Integer> release114constant =
                                        new LinkedHashMap<String, Integer>();
      
    private static Map<String, Integer> release115constant =
                                        new LinkedHashMap<String, Integer>();
    
    private static Map<Byte, Integer> byteLengthTable105 = 
                                        new HashMap<Byte, Integer>();
    private static Map<Byte, Integer> byteLengthTable111 = 
                                        new HashMap<Byte, Integer>();
                                        
    private static Map<Byte, String> variableTypeTable105 = 
                                        new LinkedHashMap<Byte, String>();
    private static Map<Byte, String> variableTypeTable111 = 
                                        new LinkedHashMap<Byte, String>();
    
    private static Map<String, Integer> variableTypeMap =
        new LinkedHashMap<String, Integer>();

    private static final int[] LENGTH_HEADER = {60, 109};
    private static final int[] LENGTH_LABEL = {32, 81};
    private static final int[] LENGTH_NAME = {9, 33};
    private static final int[] LENGTH_FORMAT_FIELD = {7, 12, 49};
    private static final int[] LENGTH_EXPANSION_FIELD ={0, 2, 4};
    private static final int[] DBL_MV_PWR = {333, 1023};
 
    static {
        
        STATA_RELEASE_NUMBER.put(104, "rel_3");
        STATA_RELEASE_NUMBER.put(105, "rel_4or5");
        STATA_RELEASE_NUMBER.put(108, "rel_6");
        STATA_RELEASE_NUMBER.put(110, "rel_7first");
        STATA_RELEASE_NUMBER.put(111, "rel_7scnd");
        STATA_RELEASE_NUMBER.put(113, "rel_8_or_9");
        STATA_RELEASE_NUMBER.put(114, "rel_10");    // reading stata docs suggests
                                                    // 114 means release 11 - ?
                                                    // confused. -- L.A.
        STATA_RELEASE_NUMBER.put(115, "rel_12");
        
        release105type.put("STRING",  127);
        release105type.put("BYTE",     98);
        release105type.put("INT",     105);
        release105type.put("LONG",    108);
        release105type.put("FLOAT",   102);
        release105type.put("DOUBLE0", 100);
        
        release111type.put("STRING",   0);
        release111type.put("BYTE",   -5);
        release111type.put("INT",    -4);
        release111type.put("LONG",   -3);
        release111type.put("FLOAT",  -2);
        release111type.put("DOUBLE", -1);

        
        
        release104constant.put("HEADER",     LENGTH_HEADER[0]);
        release104constant.put("LABEL",     LENGTH_LABEL[0]);
        release104constant.put("NAME",      LENGTH_NAME[0]);
        release104constant.put("FORMAT",    LENGTH_FORMAT_FIELD[0]);
        release104constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[0]);
        release104constant.put("DBL_MV_PWR",DBL_MV_PWR[0]);
        CONSTATNT_TABLE.put(104, release104constant);

        release105constant.put("HEADER",     LENGTH_HEADER[0]);
        release105constant.put("LABEL",     LENGTH_LABEL[0]);
        release105constant.put("NAME",      LENGTH_NAME[0]);
        release105constant.put("FORMAT",    LENGTH_FORMAT_FIELD[1]);
        release105constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[1]);
        release105constant.put("DBL_MV_PWR",DBL_MV_PWR[0]);
        CONSTATNT_TABLE.put(105, release105constant);
        
        release108constant.put("HEADER",     LENGTH_HEADER[1]);
        release108constant.put("LABEL",     LENGTH_LABEL[1]);
        release108constant.put("NAME",      LENGTH_NAME[0]);
        release108constant.put("FORMAT",    LENGTH_FORMAT_FIELD[1]);
        release108constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[1]);
        release108constant.put("DBL_MV_PWR",DBL_MV_PWR[1]);
        CONSTATNT_TABLE.put(108, release108constant);
        
        release110constant.put("HEADER",     LENGTH_HEADER[1]);
        release110constant.put("LABEL",     LENGTH_LABEL[1]);
        release110constant.put("NAME",      LENGTH_NAME[1]);
        release110constant.put("FORMAT",    LENGTH_FORMAT_FIELD[1]);
        release110constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[2]);
        release110constant.put("DBL_MV_PWR",DBL_MV_PWR[1]);
        CONSTATNT_TABLE.put(110, release110constant);
        
        release111constant.put("HEADER",     LENGTH_HEADER[1]);
        release111constant.put("LABEL",     LENGTH_LABEL[1]);
        release111constant.put("NAME",      LENGTH_NAME[1]);
        release111constant.put("FORMAT",    LENGTH_FORMAT_FIELD[1]);
        release111constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[2]);
        release111constant.put("DBL_MV_PWR",DBL_MV_PWR[1]);
        CONSTATNT_TABLE.put(111, release111constant);
        
        release113constant.put("HEADER",     LENGTH_HEADER[1]);
        release113constant.put("LABEL",     LENGTH_LABEL[1]);
        release113constant.put("NAME",      LENGTH_NAME[1]);
        release113constant.put("FORMAT",    LENGTH_FORMAT_FIELD[1]);
        release113constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[2]);
        release113constant.put("DBL_MV_PWR",DBL_MV_PWR[1]);
        CONSTATNT_TABLE.put(113, release113constant);
        
        release114constant.put("HEADER",     LENGTH_HEADER[1]);
        release114constant.put("LABEL",     LENGTH_LABEL[1]);
        release114constant.put("NAME",      LENGTH_NAME[1]);
        release114constant.put("FORMAT",    LENGTH_FORMAT_FIELD[2]);
        release114constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[2]);
        release114constant.put("DBL_MV_PWR",DBL_MV_PWR[1]);
        CONSTATNT_TABLE.put(114, release114constant);
        
        release115constant.put("HEADER",     LENGTH_HEADER[1]);
        release115constant.put("LABEL",     LENGTH_LABEL[1]);
        release115constant.put("NAME",      LENGTH_NAME[1]);
        release115constant.put("FORMAT",    LENGTH_FORMAT_FIELD[2]);
        release115constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[2]);
        release115constant.put("DBL_MV_PWR",DBL_MV_PWR[1]);
        CONSTATNT_TABLE.put(115, release115constant);
        
        byteLengthTable105.put((byte) 98,1);
        byteLengthTable105.put((byte)105,2);
        byteLengthTable105.put((byte)108,4);
        byteLengthTable105.put((byte)102,4);
        byteLengthTable105.put((byte)100,8);
        
        byteLengthTable111.put((byte)-5,1);
        byteLengthTable111.put((byte)-4,2);
        byteLengthTable111.put((byte)-3,4);
        byteLengthTable111.put((byte)-2,4);
        byteLengthTable111.put((byte)-1,8);

       
        variableTypeTable105.put((byte) 98,"Byte");
        variableTypeTable105.put((byte)105,"Integer");
        variableTypeTable105.put((byte)108,"Long");
        variableTypeTable105.put((byte)102,"Float");
        variableTypeTable105.put((byte)100,"Double");
        
        variableTypeTable111.put((byte)-5,"Byte");
        variableTypeTable111.put((byte)-4,"Integer");
        variableTypeTable111.put((byte)-3,"Long");
        variableTypeTable111.put((byte)-2,"Float");
        variableTypeTable111.put((byte)-1,"Double");


        variableTypeMap.put("Byte",   -5);
        variableTypeMap.put("Integer",-4);
        variableTypeMap.put("Long",   -3);
        variableTypeMap.put("Float",  -2);
        variableTypeMap.put("Double", -1);
        variableTypeMap.put("String",  0);
        

    }

   
    private static String[] MIME_TYPE = {"application/x-stata"};



    /** format-related constants */

    private static final int DTA_MAGIC_NUMBER_LENGTH = 4;
    private static final int NVAR_FIELD_LENGTH       = 2;
    private static final int NOBS_FIELD_LENGTH       = 4;
    private static final int TIME_STAMP_LENGTH      = 18;
    private static final int VAR_SORT_FIELD_LENGTH   = 2;
    private static final int VALUE_LABEL_HEADER_PADDING_LENGTH = 3;


 
   
    private static String unfVersionNumber = "5";

    private static int MISSING_VALUE_BIAS = 26;

    private byte BYTE_MISSING_VALUE = Byte.MAX_VALUE;
    private short INT_MISSIG_VALUE = Short.MAX_VALUE;
    private int LONG_MISSING_VALUE = Integer.MAX_VALUE;
    
  

    private static final List<Float> FLOAT_MISSING_VALUES = Arrays.asList(
        0x1.000p127f, 0x1.001p127f, 0x1.002p127f, 0x1.003p127f,
        0x1.004p127f, 0x1.005p127f, 0x1.006p127f, 0x1.007p127f,
        0x1.008p127f, 0x1.009p127f, 0x1.00ap127f, 0x1.00bp127f,
        0x1.00cp127f, 0x1.00dp127f, 0x1.00ep127f, 0x1.00fp127f,
        0x1.010p127f, 0x1.011p127f, 0x1.012p127f, 0x1.013p127f,
        0x1.014p127f, 0x1.015p127f, 0x1.016p127f, 0x1.017p127f,
        0x1.018p127f, 0x1.019p127f, 0x1.01ap127f);

    private Set<Float> FLOAT_MISSING_VALUE_SET =
        new HashSet<Float>(FLOAT_MISSING_VALUES);

    private static final List<Double> DOUBLE_MISSING_VALUE_LIST = Arrays.asList(
        0x1.000p1023, 0x1.001p1023, 0x1.002p1023, 0x1.003p1023, 0x1.004p1023,
        0x1.005p1023, 0x1.006p1023, 0x1.007p1023, 0x1.008p1023, 0x1.009p1023,
        0x1.00ap1023, 0x1.00bp1023, 0x1.00cp1023, 0x1.00dp1023, 0x1.00ep1023,
        0x1.00fp1023, 0x1.010p1023, 0x1.011p1023, 0x1.012p1023, 0x1.013p1023,
        0x1.014p1023, 0x1.015p1023, 0x1.016p1023, 0x1.017p1023, 0x1.018p1023,
        0x1.019p1023, 0x1.01ap1023);

    private Set<Double> DOUBLE_MISSING_VALUE_SET =
        new HashSet<Double>(DOUBLE_MISSING_VALUE_LIST);

    private static SimpleDateFormat sdf_ymdhmsS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); // sdf


    private static SimpleDateFormat sdf_ymd = new SimpleDateFormat("yyyy-MM-dd"); // sdf2


    private static SimpleDateFormat sdf_hms = new SimpleDateFormat("HH:mm:ss"); // stf


    private static SimpleDateFormat sdf_yw = new SimpleDateFormat("yyyy-'W'ww");



    // stata's calendar
    private static Calendar GCO_STATA = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

    private static String[] DATE_TIME_FORMAT= {
        "%tc", "%td", "%tw", "%tq","%tm", "%th", "%ty", 
        "%d",  "%w",  "%q", "%m",  "h", "%tb"
    };
    // New "business calendar format" has been added in Stata 12. -- L.A. 
    private static String[] DATE_TIME_CATEGORY={
        "time", "date", "date", "date", "date", "date", "date",
        "date", "date", "date", "date", "date", "date"
    };
    private static Map<String, String> DATE_TIME_FORMAT_TABLE=  new LinkedHashMap<String, String>();

    private static long SECONDS_PER_YEAR = 24*60*60*1000L;

    private static long STATA_BIAS_TO_EPOCH;

    static {
     
        sdf_ymdhmsS.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf_ymd.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf_hms.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf_yw.setTimeZone(TimeZone.getTimeZone("GMT"));

        // set stata's calendar
        GCO_STATA.set(1, 1960);// year
        GCO_STATA.set(2, 0); // month
        GCO_STATA.set(5, 1);// day of month
        GCO_STATA.set(9, 0);// AM(0) or PM(1)
        GCO_STATA.set(10, 0);// hh
        GCO_STATA.set(12, 0);// mm
        GCO_STATA.set(13, 0);// ss
        GCO_STATA.set(14, 0); // SS millisecond


        STATA_BIAS_TO_EPOCH  = GCO_STATA.getTimeInMillis(); // =  -315619200000
        
        for (int i=0; i<DATE_TIME_FORMAT.length; i++){
            DATE_TIME_FORMAT_TABLE.put(DATE_TIME_FORMAT[i],DATE_TIME_CATEGORY[i]);
        }

    }
    



    // instance fields -------------------------------------------------------//

    private static Logger dbgLog = Logger.getLogger(DTAFileReader.class.getPackage().getName());

    /* 
     * TODO: figure out what this map is for; (is this for some weird 2-stage
     * value label lookup that the old SMD object uses?)
     * Answer: yes, seems to be the case. look for labelName further down in 
     * the code. 
     * -- L.A. 4.0
     */
    // private Map<String, String> valueLabelSchemeMappingTable = new LinkedHashMap<String, String>();
    /* TODO: add a comment */
    
    private Map<String, Integer> valueLabelsLookupMap = new LinkedHashMap<String, Integer>();
    
    /* StrinLengthTable stores the byte lengths of string variables (these are 
     * the same fixed values for every string column). 
     * -- L.A. 4.0
     */
    private Map<Integer, Integer> StringLengthTable = new LinkedHashMap<Integer, Integer>();
    
    // private Map<String, Map<String, String>> valueLabelTable =
    //         new LinkedHashMap<String, Map<String, String>>();

    private Map<String, Integer> typeOffsetTable ;

    private Map<String, Integer> constantTable ;

    private Map<Byte, Integer> byteLengthTable;

    private Map<Byte, String> variableTypeTable;



    private NumberFormat twoDigitFormatter = new DecimalFormat("00");

    private NumberFormat doubleNumberFormatter = new DecimalFormat();

    //private SDIOMetadata smd = new DTAMetadata();
    TabularDataIngest ingesteddata = new TabularDataIngest();

    /* 
     * stataDataSection was the old "DataTable" - the proprietary object
     * used by DVN-ingest. 
     */
    //private DataTable stataDataSection = new DataTable();
    private DataTable dataTable = new DataTable();

    private int releaseNumber;

    private int headerLength;

    private int dataLabelLength;

    private boolean isLittleEndian = false;

    private int bytes_per_row;

    

    /* variableTypes is a list of string values representing the type of 
     * data values *stored* in the file - "byte", "integer", "float", "string", 
     * etc. We need this information as we're reading the data, to know how
     * many bytes to read for every object type and how to convert the binary
     * data into the proper Java type.
     * It's important to note that these types are *Stata* types - the types
     * of the variables on the DVN side may change (see below).
     * The variableTypesFinal will describe the data values once they have 
     * been read and stored in the tab. file. This is an important distinction: 
     * for example, the time/data values are stored as binary numeric values 
     * in Stata files, but we'll be storing them as strings in the DVN tabular
     * files.
     * TODO: we'll probably be able to get away without variableTypesFinal, since
     * we should be able to store the type information correctly in the actual
     * DataVariable objects. -- L.A. 4.0
    */ 
    private String[] variableTypes=null;
    
    // 4.0 private String[] variableTypesFinal= null;
    
    // 4.0 private boolean[] isDateTimeDatumList = null;

    private int value_label_table_length;
    
    private String fileUnfValue = null;

    /* 
     * TODO: variableNameList should probably be dropped, since we are 
     * adding the names directly to the DataVariable objects that we are 
     * creating. There are few places throughout this class where the 
     * list is used anyway. -- L.A. 4.0
     */
    // private List<String> variableNameList = new ArrayList<String>();

    
    /* 
     * This map was used by the old SMD object to store variable labels.
     * -- L.A. 4.0
     */
    // private Map<String, String> variableLabelMap = new LinkedHashMap<String, String>();
    
    private static final String MissingValueForTabDelimitedFile = "";
  
    // Constructor -----------------------------------------------------------//

    /**
     * Constructs a <code>DTAFileReader</code> instance with a 
     * <code>StatDataFileReaderSpi</code> object.
     * 
     * @param originator a <code>StatDataFileReaderSpi</code> object.
     */
    public DTAFileReader(TabularDataFileReaderSpi originator){
        super(originator);
    }

    // Methods ---------------------------------------------------------------//

    /*
     * This method configures Stata's release-specific parameters:
     */
    private void init() throws IOException {
        //
        if (dbgLog.isLoggable(Level.INFO)) dbgLog.info("release number="+releaseNumber);
        
        if (releaseNumber < 111) {
            typeOffsetTable = release105type;
            variableTypeTable = variableTypeTable105;
            byteLengthTable = byteLengthTable105;
        } else {
            typeOffsetTable = release111type;
            variableTypeTable = variableTypeTable111;
            byteLengthTable = byteLengthTable111;
            BYTE_MISSING_VALUE   -= MISSING_VALUE_BIAS;
            INT_MISSIG_VALUE     -= MISSING_VALUE_BIAS;
            LONG_MISSING_VALUE   -= MISSING_VALUE_BIAS;
        }
        
        if (releaseNumber <= 105){
            value_label_table_length = 2;
        } else {
            value_label_table_length = 4;
        }
        
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("type-offset table to be used:\n"+typeOffsetTable);

        constantTable = CONSTATNT_TABLE.get(releaseNumber);

        headerLength = constantTable.get("HEADER") - DTA_MAGIC_NUMBER_LENGTH;
        
        dataLabelLength = headerLength - (NVAR_FIELD_LENGTH +
            NOBS_FIELD_LENGTH + TIME_STAMP_LENGTH);
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("data_label_length="+dataLabelLength);

        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("constant table to be used:\n"+constantTable);
       

        doubleNumberFormatter.setGroupingUsed(false);
        doubleNumberFormatter.setMaximumFractionDigits(340);
        Context ctx = null; 
        try {
            ctx = new InitialContext();
            varService = (VariableServiceBean) ctx.lookup("java:global/dataverse-4.0/VariableServiceBean");
        } catch (NamingException nex) {
            try {
                ctx = new InitialContext();
                varService = (VariableServiceBean) ctx.lookup("java:global/dataverse/VariableServiceBean");
            } catch (NamingException nex2) {
                if (dbgLog.isLoggable(Level.INFO)) dbgLog.info("Could not look up initial context, or the variable service in JNDI!");
                throw new IOException ("Could not look up initial context, or the variable service in JNDI!"); 
            }
        }
    }

    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException{
        dbgLog.info("***** DTAFileReader: read() start *****");

        // shit ton of diagnostics needed here!!  -- L.A.
        
        if (dataFile != null) {
            throw new IOException ("this plugin does not support external raw data files");
        }

        decodeHeader(stream);
        decodeDescriptors(stream);
        decodeVariableLabels(stream);
        if (releaseNumber!=104) {
            decodeExpansionFields(stream);
        }
        decodeData(stream);
        decodeValueLabels(stream);

        /* The following lines modifying the smd object will need to be removed;
         * All the information describing the variables should have been stored 
         * in the newly-created DataVariables by now. -- L.A. 4.0
         */
        // smd.setVariableFormatName(formatNameTable);
        // smd.setVariableFormatCategory(formatCategoryTable);
        // smd.setValueLabelMappingTable(valueLabelSchemeMappingTable);
        
        // 4.0 smd.setVariableStorageType(variableTypesFinal);

        ingesteddata.setDataTable(dataTable);
        
        dbgLog.info("***** DTAFileReader: read() end *****");
        return ingesteddata;
    }



    private void decodeHeader(BufferedInputStream stream) throws IOException {
        dbgLog.fine("***** decodeHeader(): start *****");

        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        dbgLog.fine("reading the header segument 1: 4 byte\n");
        byte[] magic_number = new byte[DTA_MAGIC_NUMBER_LENGTH];

        int nbytes = stream.read(magic_number, 0, DTA_MAGIC_NUMBER_LENGTH);

        if (nbytes == 0) {
            throw new IOException();
        }

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("hex dump: 1st 4bytes =>"
                    + new String(Hex.encodeHex(magic_number)) + "<-");
        }

        if (magic_number[2] != 1) {
            dbgLog.fine("3rd byte is not 1: given file is not stata-dta type");
            throw new IllegalArgumentException("given file is not stata-dta type");
        } else if ((magic_number[1] != 1) && (magic_number[1] != 2)) {
            dbgLog.fine("2nd byte is neither 0 nor 1: this file is not stata-dta type");
            throw new IllegalArgumentException("given file is not stata-dta type");
        } else if (!STATA_RELEASE_NUMBER.containsKey((int) magic_number[0])) {
            dbgLog.fine("1st byte (" + magic_number[0]
                    + ") is not within the ingestable range [rel. 3-10]:"
                    + "we cannot ingest this Stata file.");
            throw new IllegalArgumentException("given file is not stata-dta type");
        } else {
            releaseNumber = (int) magic_number[0];
            init();

            // smd.getFileInformation().put("releaseNumber", releaseNumber);
            // smd.getFileInformation().put("byteOrder", (int)magic_number[1]);
            // smd.getFileInformation().put("OSByteOrder", ByteOrder.nativeOrder().toString());
            // smd.getFileInformation().put("mimeType", MIME_TYPE[0]);
            // smd.getFileInformation().put("fileFormat", MIME_TYPE[0]);
            dataTable.setOriginalFileFormat(MIME_TYPE[0]);
            /* 
             * releaseNumber: 
             * for storing in the datatable, we are converting the numeric Stata
             * release number into a more user friendly "version number"; 
             * e.g., "release number 115" = "Stata v. 12"
             * -- L.A. 4.0 
             */
            dataTable.setOriginalFormatVersion(STATA_RELEASE_NUMBER.get(releaseNumber));
            dataTable.setUnf("UNF:6:FILEFILEFILEFILE");

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("this file is stata-dta type: "
                        + STATA_RELEASE_NUMBER.get(releaseNumber)
                        + " (that means Stata version " + releaseNumber + ")");
            }
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("Endian(file)(Big: 1; Little:2)=" + magic_number[1]);
            }

            /* 
             * byte order: defined in the second byte of the "magic number": 
             */
            if ((int) magic_number[1] == 2) {
                isLittleEndian = true;
                dbgLog.fine("Reversal of the bytes is necessary to decode "
                        + "multi-byte fields");
            }
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("Endian of this platform:" + ByteOrder.nativeOrder().toString());
            }
        }

        dbgLog.fine("reading the remaining header segument 2: 60 or 109-byte");

        byte[] header = new byte[headerLength];
        nbytes = stream.read(header, 0, headerLength);
        //printHexDump(header, "header:\n");

        // 1. number of variables: short (2 bytes)
        ByteBuffer bbnvar = ByteBuffer.wrap(header, 0, NVAR_FIELD_LENGTH);
        ByteBuffer dupnvar = bbnvar.duplicate();
        short short_nvar = dupnvar.getShort();

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("get original short view(nvar)=" + short_nvar);
        }
        if (isLittleEndian) {
            bbnvar.order(ByteOrder.LITTLE_ENDIAN);

        }

        short shrt_nvar = bbnvar.getShort();
        // smd.getFileInformation().put("varQnty", new Integer(shrt_nvar));
        dataTable.setVarQuantity(new Long(shrt_nvar));
        int nvar = shrt_nvar;
        
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("number of variables(nvar)=" + nvar);
        }

        // 4.0 Initialize variables: 
        List<DataVariable> variableList = new ArrayList<DataVariable>();

        for (int i = 0; i < nvar; i++) {
            DataVariable dv = new DataVariable();
            dv.setInvalidRanges(new ArrayList());
            dv.setSummaryStatistics( new ArrayList() );
            dv.setUnf("UNF:6:XXX");
            dv.setCategories(new ArrayList());
            variableList.add(dv);

            dv.setFileOrder(i);

            dv.setDataTable(dataTable);
        }

        dataTable.setDataVariables(variableList);

        // setup variableTypeList
        variableTypes = new String[nvar];

        // 2. number of observations: int (4 bytes)
        ByteBuffer nobs = ByteBuffer.wrap(header, NVAR_FIELD_LENGTH,
                NOBS_FIELD_LENGTH);
        ByteBuffer dupnobs = nobs.duplicate();
        int int_dupnobs = dupnobs.getInt();
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("raw nobs=" + int_dupnobs);
        }
        if (isLittleEndian) {
            nobs.order(ByteOrder.LITTLE_ENDIAN);
        }
        int int_nobs = nobs.getInt();
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("reversed nobs=" + int_nobs);
        }

        // smd.getFileInformation().put("caseQnty", new Integer(int_nobs));
        dataTable.setCaseQuantity(new Long(int_nobs));

        /* 
         the "data label" - 
         note that we are not using this label for anything 
         (wonder what it is though? can we use it somewhere?)
         but we still need to extract it from the byte stream, 
         since the offsets of the objects stored further up
         are calculated relative to it. -- L.A., 4.0
         */
        // 3. data_label: 32 or 81 bytes
        int dl_offset = NVAR_FIELD_LENGTH + NOBS_FIELD_LENGTH;
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("dl_offset=" + dl_offset);
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("data_label_length=" + dataLabelLength);
        }

        String data_label = new String(Arrays.copyOfRange(header, dl_offset,
                (dl_offset + dataLabelLength)), "ISO-8859-1");

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("data_label_length=" + data_label.length());
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("loation of the null character=" + data_label.indexOf(0));
        }

        String dataLabel = getNullStrippedString(data_label);
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("data_label_length=" + dataLabel.length());
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("data_label=[" + dataLabel + "]");
        }

        // smd.getFileInformation().put("dataLabel", dataLabel);

        /* end of "data label" */
        // 4. time_stamp: ASCII String (18 bytes)
        // added after release 4
        if (releaseNumber > 104) {
            int ts_offset = dl_offset + dataLabelLength;
            String time_stamp = new String(Arrays.copyOfRange(header, ts_offset,
                    ts_offset + TIME_STAMP_LENGTH), "ISO-8859-1");
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("time_stamp_length=" + time_stamp.length());
            }
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("loation of the null character=" + time_stamp.indexOf(0));
            }

            String timeStamp = getNullStrippedString(time_stamp);
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("timeStamp_length=" + timeStamp.length());
            }
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("timeStamp=[" + timeStamp + "]");
            }

            /* All these time/date stamps - I don't think we are using 
             them anywhere. -- L.A. 4.0
             */
            // smd.getFileInformation().put("timeStamp", timeStamp);
            // smd.getFileInformation().put("fileDate", timeStamp);
            // smd.getFileInformation().put("fileTime", timeStamp);
            /* As for the "varformat schema" - storing this information was 
             * largely redundant, since we know that all the variables in 
             * this data table come from a Stata file. -- L.A. 4.0
             */
            // smd.getFileInformation().put("varFormat_schema", "STATA");
        }

        if (dbgLog.isLoggable(Level.FINE)) {
            // dbgLog.fine("smd dump:"+smd.toString());
            dbgLog.fine("***** decodeHeader(): end *****");
        }
    }



    private void decodeDescriptors(BufferedInputStream stream) throws IOException {

        dbgLog.fine("***** decodeDescriptors(): start *****");

        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        //int nvar = (Integer)smd.getFileInformation().get("varQnty");
        int nvar = dataTable.getVarQuantity().intValue();

        // part 1: variable type list
        decodeDescriptorVarTypeList(stream, nvar);

        // part 2: Variable_Name List
        // name length= 9(release 105) or 33 (release 111) each null terminated
        decodeDescriptorVarNameList(stream, nvar);

        // Part 3: variable sort list
        // length of this field = short(2bytes)*(nvar +1)
        decodeDescriptorVarSortList(stream, nvar);

        // Part 4: variable format list
        // VAR_FORMAT_FIELD_LENGTH (7,12, 49 bytes) * navar
        // null-terminated string
        decodeDescriptorVariableFormat(stream, nvar);

        // Part 5: value-label list
        // variable_name * nvar null-terminated String
        decodeDescriptorValueLabel(stream, nvar);

        if (dbgLog.isLoggable(Level.FINE)) {
            //dbgLog.fine("smd dump (Descriptor):\n"+smd.toString());
            dbgLog.fine("***** decodeDescriptors(): end *****");
        }

    }

    private void decodeDescriptorVarTypeList(BufferedInputStream stream, int nvar) throws IOException {
        byte[] typeList = new byte[nvar];

        // note: the offset param of read() is relative to
        // the current position, not absolute position
        int nbytes = stream.read(typeList, 0, nvar);
        //printHexDump(typeList, "variable type list");
        if (nbytes == 0) {
            throw new IOException("reading the descriptior: no byte was read");
        }
        /*
         Stata internal constants representing variable type information; 
         these were kindly provided by Akio:
        111 type
        Type:   b   i   l   f   d (byte, int, long, float, double)
        byte:  -5  -4  -3  -2  -1 (signed byte = java's byte type)
        byte: 251 252 253 254 255 (unsigned byte)
        HEX:  FB  FC  FD  FE  FF

        105 type(type chars correspond to their hex/decimal expressions
        Type:   b   i   l   f   d (byte, int, long, float, double)
        byte:  98 105 108 102 100 (signed byte = java's byte type)
        byte:  98 105 108 102 100 (unsigned byte)
        HEX:  62  69  6C  66  64
         */
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("type_offset_table:\n" + typeOffsetTable);


        bytes_per_row = 0;
        
        for (int i = 0; i < typeList.length; i++) {
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine(i + "-th value=" + typeList[i]);
            
            /*
             * How Stata types correspond to the DVN types: 
             * "Byte", "Integer" and "Long" become Numeric, Discrete (unless date value); 
             * "Float" and "Double" become Numeric, Continuous (unless date value);
             * "String" becomes String;
             * Date/time values stored as numeric types above, are converted into 
             * Strings.
             * -- L.A. 4.0
             */

            if (byteLengthTable.containsKey(typeList[i])) {
                bytes_per_row += byteLengthTable.get(typeList[i]);
                variableTypes[i] = variableTypeTable.get(typeList[i]);
                String typeLabel = variableTypes[i];
                
                if (typeLabel != null) {
                    // TODO: get rid of the service lookups for known format and interval 
                    // types, etc. -- L.A. 4.0
                    VariableFormatType formatTypeNumeric = varService.findVariableFormatTypeByName("numeric");
                    if (formatTypeNumeric == null) {
                        throw new IOException("No numeric format type in the database. (has the db been populated with reference data?)");
                    }
                    dataTable.getDataVariables().get(i).setVariableFormatType(formatTypeNumeric);
                    if (typeLabel.equals("Byte") || typeLabel.equals("Integer") || typeLabel.equals("Long")) {
                        // these are treated as discrete:
                        dataTable.getDataVariables().get(i).setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
                        
                    } else if (typeLabel.equals("Float") || typeLabel.equals("Double")) {
                        // these are treated as contiuous:
                        dataTable.getDataVariables().get(i).setVariableIntervalType(varService.findVariableIntervalTypeByName("continuous"));
                        
                    } else {
                        throw new IOException("Unrecognized type label: "+typeLabel+" for Stata type value byte "+typeList[i]+".");
                    }
                } else {
                    throw new IOException("No entry in the known types table for Stata type value byte "+typeList[i]+".");
                }
            } else {
                // pre-111 string type
                if (releaseNumber < 111) {
                    int stringType = 256 + typeList[i];
                    if (stringType >= typeOffsetTable.get("STRING")) {
                        int string_var_length = stringType - typeOffsetTable.get("STRING");
                        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("string_var_length=" + string_var_length);
                        bytes_per_row += string_var_length;

                        variableTypes[i] = "String";
                        dataTable.getDataVariables().get(i).setVariableFormatType(varService.findVariableFormatTypeByName("character"));
                        dataTable.getDataVariables().get(i).setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
                        StringLengthTable.put(i, string_var_length);


                    } else {
                        throw new IOException(
                                "unknown variable type was detected: reading errors?");
                    }
                } else if (releaseNumber >= 111) {
                    // post-111 string type
                    if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("DTA reader: typeList[" + i + "]=" + typeList[i]);

                    // if the size of strXXX type is less than 128,
                    // the value of typeList[i] will be equal to that;
                    // if however it is >= 128, typeList[i] = (size - 256)
                    // i.e. it'll be a negative value:

                    int stringType = ((typeList[i] > 0) &&
                            (typeList[i] <= 127)) ? typeList[i] : 256 + typeList[i];

                    if (stringType >= typeOffsetTable.get("STRING")) {
                        int string_var_length = stringType - typeOffsetTable.get("STRING");
                        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("DTA reader: string_var_length=" + string_var_length);
                        bytes_per_row += string_var_length;

                        variableTypes[i] = "String";
                        dataTable.getDataVariables().get(i).setVariableFormatType(varService.findVariableFormatTypeByName("character"));
                        dataTable.getDataVariables().get(i).setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
                        StringLengthTable.put(i, string_var_length);


                    } else {
                        throw new IOException(
                                "unknown variable type was detected: reading errors?");
                    }
                } else {
                    throw new IOException("uknown release number ");
                }

            }
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine(i + "=th\t sum=" + bytes_per_row);
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("bytes_per_row(final)=" + bytes_per_row);
            dbgLog.fine("variableTypes:\n" + Arrays.deepToString(variableTypes));
            dbgLog.fine("StringLengthTable=" + StringLengthTable);
        }
        
        /* For now, we are just going to populate the varaibleTypesFinal with 
         * the exact values from variableTypes; later these values may change 
         * (see the comment where variableTypesFinal was declared).
         * TODO: we'll likely be getting rid of the "Final" version. -- L.A. 4.0
         */
        // 4.0 variableTypesFinal = new String[nvar];
        // 4.0 for (int i = 0; i < variableTypes.length; i++) {
        //     variableTypesFinal[i] = new String(variableTypes[i]);
        // }

    }



    private void decodeDescriptorVarNameList(BufferedInputStream stream, int nvar) throws IOException {
        int length_var_name = constantTable.get("NAME");
        int length_var_name_list = length_var_name * nvar;
        
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("length_var_name_list=" + length_var_name_list);

        byte[] variableNameBytes = new byte[length_var_name_list];

        int nbytes = stream.read(variableNameBytes, 0, length_var_name_list);


        if (nbytes == 0) {
            throw new IOException("reading the var name list: no var name was read");
        }
        int offset_start = 0;
        int offset_end = 0;
        for (int i = 0; i < nvar; i++) {
            offset_end += length_var_name;
            String vari = new String(Arrays.copyOfRange(variableNameBytes, offset_start,
                    offset_end), "ISO-8859-1");
            // 4.0 variableNameList.add(getNullStrippedString(vari));
            String varName = getNullStrippedString(vari);
            dataTable.getDataVariables().get(i).setName(varName);
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine(i + "-th name=[" + varName + "]");
            offset_start = offset_end;
        }
        // 4.0 if (dbgLog.isLoggable(Level.FINE)) {
        //    dbgLog.fine("variableNameList=\n" + StringUtils.join(variableNameList, ",\n") + "\n");
        // }

        //smd.setVariableName(variableNameList.toArray(new String[variableNameList.size()]));
    }

    private void decodeDescriptorVarSortList(BufferedInputStream stream, int nvar) throws IOException {
        /* 
         * Whatever this "var sort list" is, we don't seem to be using this 
         * information for any purposes in particular. However, we need to read
         * the bytes, to skip to the next section in the stream, if nothing else. 
         * -- L.A. 4.0
         */
        int length_var_sort_list = VAR_SORT_FIELD_LENGTH * (nvar + 1);
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("length_var_sort_list=" + length_var_sort_list);

        byte[] varSortList = new byte[length_var_sort_list];
        short[] variableSortList = new short[nvar + 1];


        int nbytes = stream.read(varSortList, 0, length_var_sort_list);

        if (nbytes == 0) {
            throw new IOException("reading error: the varSortList");
        }

        int offset_start = 0;
        for (int i = 0; i <= nvar; i++) {


            ByteBuffer bb_varSortList = ByteBuffer.wrap(varSortList,
                    offset_start, VAR_SORT_FIELD_LENGTH);
            if (isLittleEndian) {
                bb_varSortList.order(ByteOrder.LITTLE_ENDIAN);
            }
            variableSortList[i] = bb_varSortList.getShort();

            offset_start += VAR_SORT_FIELD_LENGTH;
        }
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("variableSortList=" + Arrays.toString(variableSortList));

    }


    /* Variable Formats are used exclusively for time and date variables. 
     * TODO: try to get rid of the maps and tables populated here, by properly
     * configuring the corresponding DataVariables. -- L.A. 4.0
     */
    private void decodeDescriptorVariableFormat(BufferedInputStream stream, int nvar) throws IOException {
        int length_var_format = constantTable.get("FORMAT");
        int length_var_format_list = length_var_format * nvar;
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("length_var_format_list=" + length_var_format_list);

        byte[] variableFormatList = new byte[length_var_format_list];
        // 4.0 variableFormats = new String[nvar];
        // 4.0 isDateTimeDatumList = new boolean[nvar];

        int nbytes = stream.read(variableFormatList, 0, length_var_format_list);

        if (nbytes == 0) {
            throw new IOException("reading var formats: no format was read");
        }
        int offset_start = 0;
        int offset_end = 0;
        for (int i = 0; i < nvar; i++) {
            offset_end += length_var_format;
            String vari = new String(Arrays.copyOfRange(variableFormatList, offset_start,
                    offset_end), "ISO-8859-1");
            String variableFormat = getNullStrippedString(vari);
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine(i + "-th format=[" + variableFormat + "]");
                        
            String variableFormatKey = null;
            if (variableFormat.startsWith("%t")) {
                variableFormatKey = variableFormat.substring(0, 3);
            } else {
                variableFormatKey = variableFormat.substring(0, 2);
            }
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine(i + " th variableFormatKey=" + variableFormatKey);

            /* 
             * Now, let's check if this format is a known time or date format. 
             * If so, note that this changes the storage type of the variable!
             * i.e., times and dates are stored as binary numeric values, but on 
             * the DVN side/in the tab files they will become strings. 
             * TODO: it kinda does look like we can get rid of the variableFormats[]
             * list; these formats are only used if this is a recognized 
             * "date/time datum" (see below); so then it looks like we can 
             * extract this info from the DataVariable "formatschemaname". 
             * -- L.A. 4.0
             */
            if (DATE_TIME_FORMAT_TABLE.containsKey(variableFormatKey)) {
                //formatNameTable.put(variableNameList.get(i), variableFormat);
                dataTable.getDataVariables().get(i).setFormatSchemaName(variableFormat);
                //formatCategoryTable.put(variableNameList.get(i), DATE_TIME_FORMAT_TABLE.get(variableFormatKey));
                dataTable.getDataVariables().get(i).setFormatCategory(DATE_TIME_FORMAT_TABLE.get(variableFormatKey));
                // 4.0 isDateTimeDatumList[i] = true;
                if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine(i + "th var: category=" +
                        DATE_TIME_FORMAT_TABLE.get(variableFormatKey));
                // 4.0 variableTypesFinal[i] = "String";
                // 4.0: convert the DVN variable to String/Discrete: 
                dataTable.getDataVariables().get(i).setVariableFormatType(varService.findVariableFormatTypeByName("character"));
                dataTable.getDataVariables().get(i).setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
            } // 4.0 else {
            // 4.0     isDateTimeDatumList[i] = false;
            // 4.0 }

            
            offset_start = offset_end;
        }
        // 4.0 if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("variableFormats=\n" + StringUtils.join(variableFormats, ",\n") + "\n");


    }
    
    private void decodeDescriptorValueLabel(BufferedInputStream stream, int nvar) throws IOException {
        int length_label_name = constantTable.get("NAME");
        int length_label_name_list = length_label_name * nvar;
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("length_label_name=" + length_label_name_list);

        byte[] labelNameList = new byte[length_label_name_list];
        String[] labelNames = new String[nvar];

        int nbytes = stream.read(labelNameList, 0, length_label_name_list);

        if (nbytes == 0) {
            throw new IOException("reading value-label list:: no var name was read");
        }
        int offset_start = 0;
        int offset_end = 0;
        for (int i = 0; i < nvar; i++) {
            offset_end += length_label_name;
            String vari = new String(Arrays.copyOfRange(labelNameList, offset_start,
                    offset_end), "ISO-8859-1");
            labelNames[i] = getNullStrippedString(vari);
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine(i + "-th label=[" + labelNames[i] + "]");
            offset_start = offset_end;
        }
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("labelNames=\n" + StringUtils.join(labelNames, ",\n") + "\n");

        for (int i = 0; i < nvar; i++) {
            if ((labelNames[i] != null) && (!labelNames[i].equals(""))) {
                // valueLabelSchemeMappingTable.put(variableNameList.get(i), labelNames[i]);
                valueLabelsLookupMap.put(labelNames[i], i);
            }
        }
        // 4.0 if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("valueLabelSchemeMappingTable:\n" + valueLabelSchemeMappingTable);
    }


    private void decodeVariableLabels(BufferedInputStream stream) throws IOException {

        dbgLog.fine("***** decodeVariableLabels(): start *****");

        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        // variable label length (32 or 81 bytes)*nvar, each null-terminated
        // int nvar = (Integer)smd.getFileInformation().get("varQnty");
        int nvar = dataTable.getVarQuantity().intValue();

        int length_var_label = constantTable.get("LABEL");
        int length_var_label_list = length_var_label * nvar;

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("length_label_name=" + length_var_label_list);
        }

        byte[] variableLabelBytes = new byte[length_var_label_list];
        int nbytes = stream.read(variableLabelBytes, 0, length_var_label_list);

        //printHexDump(variableLabelList, "variableLabelList");
        if (nbytes == 0) {
            throw new IOException("reading variable label list: no label was read");
        }
        int offset_start = 0;
        int offset_end = 0;
        for (int i = 0; i < nvar; i++) {
            offset_end += length_var_label;
            String vari = new String(Arrays.copyOfRange(variableLabelBytes, offset_start,
                    offset_end), "ISO-8859-1");
            //variableLabelMap.put(variableNameList.get(i), getNullStrippedString(vari));
            String variableLabelParsed = getNullStrippedString(vari);
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine(i + "-th label=[" + variableLabelParsed + "]");
            }
            offset_start = offset_end;

            // 4.0: 
            dataTable.getDataVariables().get(i).setLabel(variableLabelParsed);
        }

        // smd.setVariableLabel(variableLabelMap);
        // if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("smd dump (variable label):\n"+smd.toString());
        dbgLog.fine("***** decodeVariableLabels(): end *****");

    }
    

    /* 
     * We don't seem to be using any of these "expansion fields" - whatever 
     * they are; but we need to read the section, to skip to the next one in 
     * the byte stream, if nothing else. 
     * -- L.A. 4.0
     */
    private void decodeExpansionFields(BufferedInputStream stream) throws IOException {

        dbgLog.fine("***** decodeExpansionFields(): start *****");
        
        if (stream ==null){
            throw new IllegalArgumentException("stream == null!");
        }
        
        // Added since release 105
        // [1-byte byte_field][short(2)/int(4)_field][variable_field whose
        // length is specified by the previous short/int field]
        
        int int_type_expansion_field = constantTable.get("EXPANSION");
        if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("int_type_expansion_field="+int_type_expansion_field);
        while(true){
            byte[] firstByte = new byte[1];
            byte[] lengthBytes = new byte[int_type_expansion_field];
          
            int nbyte = stream.read(firstByte, 0, 1);
            dbgLog.fine("read 1st byte");
            int nbytes = stream.read(lengthBytes, 0, int_type_expansion_field);
            dbgLog.fine("read next integer");

            ByteBuffer bb_field_length = ByteBuffer.wrap(lengthBytes);

            if (isLittleEndian){
                bb_field_length.order(ByteOrder.LITTLE_ENDIAN);
                dbgLog.fine("byte reversed");
            }

            int field_length;

            if (int_type_expansion_field == 2){
                field_length = bb_field_length.getShort();
            } else {
                field_length = bb_field_length.getInt();
            }
            
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("field_length="+field_length);
            if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("firstByte[0]="+firstByte[0]);
            if ((field_length + (int)firstByte[0]) == 0){
                // reached the end of this field
                break;
            } else {
                byte[] stringField = new byte[field_length];
                nbyte = stream.read(stringField, 0, field_length);

              
            }
        }

        dbgLog.fine("***** decodeExpansionFields(): end *****");

    }
    
    /**
     *
     * @param stream
     */
    private void decodeValueLabels(BufferedInputStream stream) throws IOException {

        dbgLog.fine("***** decodeValueLabels(): start *****");

        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        if (stream.available() != 0) {
            if (releaseNumber <= 105) {
                parseValueLabelsRelease105(stream);
            } else {
                parseValueLabelsReleasel108(stream);
            }
        } else {
            dbgLog.fine("no value-label table: end of file");

        }
        dbgLog.fine("***** decodeValueLabels(): end *****");
    }
    
    
    void parseValueLabelsRelease105(BufferedInputStream stream) throws IOException {

        dbgLog.fine("***** parseValueLabelsRelease105(): start *****");

        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        int nvar = dataTable.getVarQuantity().intValue();
        int length_label_name = constantTable.get("NAME") + 1;
        // note: caution +1 as the null character, not 9 byte

        int length_value_label_header = value_label_table_length
                + length_label_name;

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("value_label_table_length=" + value_label_table_length);
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("length_value_label_header=" + length_value_label_header);
        }

        int length_lable_name_field = 8;

        /*
         Seg  field         byte    type
         1-1. no of pairs      2    int  (= m)
         1-2. vlt_name        10    includes char+(\0) == name used in Sec2.part 5
         -----------------------------------
         11
         2-1. values         2*n    int[]
         2-2. labels         8*n    char
         */
        for (int i = 0; i < nvar; i++) {
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("\n\n" + i + "th value-label table header");
            }

            byte[] valueLabelHeader = new byte[length_value_label_header];

            // Part 1: reading the header of a value-label table if exists
            int nbytes = stream.read(valueLabelHeader, 0,
                    length_value_label_header);

            if (nbytes == 0) {
                throw new IOException("reading value label header: no datum");
            }

            // 1.1 number of value-label pairs in this table (= m)
            ByteBuffer bb_value_label_pairs
                    = ByteBuffer.wrap(valueLabelHeader, 0,
                            value_label_table_length);
            if (isLittleEndian) {
                bb_value_label_pairs.order(ByteOrder.LITTLE_ENDIAN);
                //if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("value lable table lenth: byte reversed");
            }
            int no_value_label_pairs = bb_value_label_pairs.getShort();

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("no_value_label_pairs=" + no_value_label_pairs);
            }

            // 1.2 labelName
            String rawLabelName = new String(Arrays.copyOfRange(
                    valueLabelHeader,
                    value_label_table_length,
                    (value_label_table_length + length_label_name)),
                    "ISO-8859-1");

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("rawLabelName(length)=" + rawLabelName.length());
            }
            String labelName = rawLabelName.substring(0, rawLabelName.indexOf(0));

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("label name = " + labelName + "\n");
            }

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine(i + "-th value-label table");
            }
                // Part 2: reading the value-label table
            // the length of the value-label table is: 2*m + 8*m = 10*m
            int length_value_label_table = (value_label_table_length
                    + length_lable_name_field) * no_value_label_pairs;

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("length_value_label_table=" + length_value_label_table);
            }

            byte[] valueLabelTable_i = new byte[length_value_label_table];
            int noBytes = stream.read(valueLabelTable_i, 0,
                    length_value_label_table);
            if (noBytes == 0) {
                throw new IOException("reading value label table: no datum");
            }

                // 2-1. 2-byte-integer array (2*m): value array (sorted)
            short[] valueList = new short[no_value_label_pairs];
            int offset_value = 0;

            for (int k = 0; k < no_value_label_pairs; k++) {

                ByteBuffer bb_value_list
                        = ByteBuffer.wrap(valueLabelTable_i, offset_value,
                                value_label_table_length);
                if (isLittleEndian) {
                    bb_value_list.order(ByteOrder.LITTLE_ENDIAN);
                }
                valueList[k] = bb_value_list.getShort();

                offset_value += value_label_table_length;
            }

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("value_list=" + Arrays.toString(valueList) + "\n");
            }

                // 2-2. 8-byte chars that store label data (m units of labels)
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("current offset_value=" + offset_value);
            }

            int offset_start = offset_value;
            int offset_end = offset_value + length_lable_name_field;
            String[] labelList = new String[no_value_label_pairs];

            for (int l = 0; l < no_value_label_pairs; l++) {

                String string_l = new String(Arrays.copyOfRange(valueLabelTable_i, offset_start,
                        offset_end), "ISO-8859-1");

                int null_position = string_l.indexOf(0);
                if (null_position != -1) {
                    labelList[l] = string_l.substring(0, null_position);
                } else {
                    labelList[l] = string_l;
                }

                offset_start = offset_end;
                offset_end += length_lable_name_field;
            }

            Map<String, String> tmpValueLabelTable = new LinkedHashMap<String, String>();

            int realVarNumber = valueLabelsLookupMap.get(labelName);

            for (int j = 0; j < no_value_label_pairs; j++) {
                if (dbgLog.isLoggable(Level.FINE)) {
                    dbgLog.fine(j + "-th pair:" + valueList[j] + "[" + labelList[j] + "]");
                }
                // tmpValueLabelTable.put(Integer.toString(valueList[j]), labelList[j]);

                /* TODO:
                 * instead, we should be creating Variable categories here. 
                 * -- L.A. 4.0. 
                 * (note the 2-stage lookup mechanism - the labelName below
                 * may not necessarily be the same as the variable name!
                 */
                VariableCategory cat = new VariableCategory();
                cat.setValue(Integer.toString(valueList[j]));
                cat.setLabel(labelList[j]);

                /* cross-link the variable and category to each other: */
                cat.setDataVariable(dataTable.getDataVariables().get(realVarNumber));
                dataTable.getDataVariables().get(realVarNumber).getCategories().add(cat);

            }
            /* TODO: 
             * once the above has been done (variable categories created)
             * the valueLabelTable can be removed...
             */
            // valueLabelTable.put(labelName, tmpValueLabelTable);

            if (stream.available() == 0) {
                    // reached the end of this file
                // do exit-processing
                if (dbgLog.isLoggable(Level.FINE)) {
                    dbgLog.fine("***** reached the end of the file at " + i
                            + "th value-label Table *****");
                }
                break;
            }

        } // for nvar loop

        // 4.0 if (dbgLog.isLoggable(Level.FINE)) {
        //     dbgLog.fine("valueLabelTable:\n" + valueLabelTable);
        // }

        //smd.setValueLabelTable(valueLabelTable);

        dbgLog.fine("***** parseValueLabelsRelease105(): end *****");

    }


    private void parseValueLabelsReleasel108(BufferedInputStream stream) throws IOException {

        dbgLog.fine("***** parseValueLabelsRelease108(): start *****");

        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        // int nvar = (Integer)smd.getFileInformation().get("varQnty");
        int nvar = dataTable.getVarQuantity().intValue();
        int length_label_name = constantTable.get("NAME");
        int length_value_label_header = value_label_table_length
                + length_label_name
                + VALUE_LABEL_HEADER_PADDING_LENGTH;

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("value_label_table_length=" + value_label_table_length);
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("length_value_label_header=" + length_value_label_header);
        }
        /*
         Seg  field         byte    type
         1-1. len_vlt(Seg.2)   4    int
         1-2. vlt_name      9/33    char+(\0) == name used in Sec2.part 5
         1-3. padding          3    byte
         -----------------------------------
         16/40
         2-1. n(# of vls)      4    int
         2-2. m(len_labels)    4    int
         2-3. label_offsets    4*n  int[]
         2-4. values           4*n  int[]
         2-5. labels           m    char
         */

        for (int i = 0; i < nvar; i++) {
            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("\n\n" + i + "th value-label table header");
            }

            byte[] valueLabelHeader = new byte[length_value_label_header];

            // Part 1: reading the header of a value-label table if exists
            int nbytes = stream.read(valueLabelHeader, 0,
                    length_value_label_header);

            if (nbytes == 0) {
                throw new IOException("reading value label header: no datum");
            }

            // 1.1 length_value_label_table
            ByteBuffer bb_value_label_header
                    = ByteBuffer.wrap(valueLabelHeader, 0,
                            value_label_table_length);
            if (isLittleEndian) {
                bb_value_label_header.order(ByteOrder.LITTLE_ENDIAN);
            }
            int length_value_label_table = bb_value_label_header.getInt();

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("length of this value-label table="
                        + length_value_label_table);
            }

            // 1.2 labelName
            String rawLabelName = new String(Arrays.copyOfRange(
                    valueLabelHeader,
                    value_label_table_length,
                    (value_label_table_length + length_label_name)),
                    "ISO-8859-1");
            String labelName = getNullStrippedString(rawLabelName);

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("label name = " + labelName + "\n");
            }

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine(i + "-th value-label table");
            }
            // Part 2: reading the value-label table
            byte[] valueLabelTable_i = new byte[length_value_label_table];
            int noBytes = stream.read(valueLabelTable_i, 0,
                    length_value_label_table);
            if (noBytes == 0) {
                throw new IOException("reading value label table: no datum");
            }

            // 2-1. 4-byte-integer: number of units in this table (n)
            int valueLabelTable_offset = 0;
            ByteBuffer bb_value_label_pairs
                    = ByteBuffer.wrap(valueLabelTable_i, valueLabelTable_offset,
                            value_label_table_length);
            if (isLittleEndian) {
                bb_value_label_pairs.order(ByteOrder.LITTLE_ENDIAN);
            }

            int no_value_label_pairs = bb_value_label_pairs.getInt();

            valueLabelTable_offset += value_label_table_length;

            if (dbgLog.isLoggable(Level.FINE)) {
                dbgLog.fine("no_value_label_pairs=" + no_value_label_pairs);
            }

                // 2-2. 4-byte-integer: length of the label section (m bytes)
            ByteBuffer bb_length_label_segment
                    = ByteBuffer.wrap(valueLabelTable_i, valueLabelTable_offset,
                            value_label_table_length);
            if (isLittleEndian) {
                bb_length_label_segment.order(ByteOrder.LITTLE_ENDIAN);
            }

            int length_label_segment = bb_length_label_segment.getInt();
            valueLabelTable_offset += value_label_table_length;

                // 2-3. 4-byte-integer array (4xm): offset values for the label sec.
                // these "label offsets" actually appear to represent the byte
            // offsets of the label strings, as stored in the next section.
            // as of now, these are not used for anything, and the code
            // below assumes that the labels are already in the same
            // order as the numeric values! -- L.A.
            int[] label_offsets = new int[no_value_label_pairs];
            int byte_offset = valueLabelTable_offset;

            for (int j = 0; j < no_value_label_pairs; j++) {

                // note: 4-byte singed, not java's long
                ByteBuffer bb_label_offset
                        = ByteBuffer.wrap(valueLabelTable_i, byte_offset,
                                value_label_table_length);
                if (isLittleEndian) {
                    bb_label_offset.order(ByteOrder.LITTLE_ENDIAN);
                    dbgLog.fine("label offset: byte reversed");
                }
                label_offsets[j] = bb_label_offset.getInt();
                dbgLog.fine("label offset [" + j + "]: " + label_offsets[j]);

                byte_offset += value_label_table_length;

            }

                // 2-4. 4-byte-integer array (4xm): value array (sorted)
            dbgLog.fine("value array");

            int[] valueList = new int[no_value_label_pairs];
            int offset_value = byte_offset;

            for (int k = 0; k < no_value_label_pairs; k++) {

                ByteBuffer bb_value_list
                        = ByteBuffer.wrap(valueLabelTable_i, offset_value,
                                value_label_table_length);
                if (isLittleEndian) {
                    bb_value_list.order(ByteOrder.LITTLE_ENDIAN);
                }
                valueList[k] = bb_value_list.getInt();

                offset_value += value_label_table_length;

            }

                // 2-5. m-byte chars that store label data (m units of labels)
            String label_segment = new String(
                    Arrays.copyOfRange(valueLabelTable_i,
                            offset_value,
                            (length_label_segment + offset_value)), "ISO-8859-1");

                // L.A. -- 2011.2.25:
            // This assumes that the labels are already stored in the right
            // order: (see my comment for the section 2.3 above)
                //String[] labelList = label_segment.split("\0");
                // Instead, we should be using the offset values obtained in
            // the section 2.3 above, and select the corresponding
            // substrings:
            String[] labelList = new String[no_value_label_pairs];

            for (int l = 0; l < no_value_label_pairs; l++) {
                String lblString = null;
                int lblOffset = label_offsets[l];

                lblString = label_segment.substring(lblOffset);

                int nullIndx = lblString.indexOf('\000');

                if (nullIndx > -1) {
                    lblString = lblString.substring(0, nullIndx);
                }

                labelList[l] = lblString;
            }

                // this should work! -- L.A.
            // (TODO: check the v105 value label parsing method, to see if
            // something similar applies there)
            Map<String, String> tmpValueLabelTable = new LinkedHashMap<String, String>();
            int realVarNumber = valueLabelsLookupMap.get(labelName);

            for (int l = 0; l < no_value_label_pairs; l++) {
                if (dbgLog.isLoggable(Level.FINE)) {
                    dbgLog.fine(l + "-th pair:" + valueList[l] + "[" + labelList[l] + "]");
                }

                tmpValueLabelTable.put(Integer.toString(valueList[l]), labelList[l]);

                VariableCategory cat = new VariableCategory();
                cat.setValue(Integer.toString(valueList[l]));
                cat.setLabel(labelList[l]);

                /* cross-link the variable and category to each other: */
                cat.setDataVariable(dataTable.getDataVariables().get(realVarNumber));
                dataTable.getDataVariables().get(realVarNumber).getCategories().add(cat);
            }

                //valueLabelTable.put(labelName, tmpValueLabelTable);
            if (stream.available() == 0) {
                    // reached the end of this file
                // do exit-processing
                dbgLog.fine("***** reached the end of the file at " + i
                        + "th value-label Table *****");
                break;
            }

        }  // for nvar loop

        // 4.0 if (dbgLog.isLoggable(Level.FINE)) dbgLog.fine("valueLabelTable:\n"+valueLabelTable);
        //smd.setValueLabelTable(valueLabelTable);
        dbgLog.fine("***** parseValueLabelsRelease108(): end *****");
    }

    private void decodeData(BufferedInputStream stream) throws IOException {

        dbgLog.fine("\n***** decodeData(): start *****");

        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        //int nvar = (Integer)smd.getFileInformation().get("varQnty");
        int nvar = dataTable.getVarQuantity().intValue();
        //int nobs = (Integer)smd.getFileInformation().get("caseQnty");
        int nobs = dataTable.getCaseQuantity().intValue();

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("data dimensions[observations x variables] = (" + nobs + "x" + nvar + ")");
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("bytes per row=" + bytes_per_row + " bytes");
        }

        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("variableTypes=" + Arrays.deepToString(variableTypes));
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("StringLengthTable=" + StringLengthTable);
        }

        // create a File object to save the tab-delimited data file
        FileOutputStream fileOutTab = null;
        PrintWriter pwout = null;
        File tabDelimitedDataFile = File.createTempFile("tempTabfile.", ".tab");
        String tabDelimitedDataFileName = tabDelimitedDataFile.getAbsolutePath();

        // save the temp tab-delimited file in the return ingest object:        
        // smd.getFileInformation().put("tabDelimitedDataFileLocation", tabDelimitedDataFileName);
        ingesteddata.setTabDelimitedFile(tabDelimitedDataFile);

        fileOutTab = new FileOutputStream(tabDelimitedDataFile);
        pwout = new PrintWriter(new OutputStreamWriter(fileOutTab, "utf8"), true);

        // data storage
        // Object[][] dataTable = new Object[nobs][nvar];
        // for later variable-wise calculations of statistics
        // dataTable2 sotres cut-out data columnwise
        /*
         commenting out datatable2: one of the goals of reimplementing 
         ingest is to stop storing the entire data file in memory!
         Object[][] dataTable2 = new Object[nvar][nobs];
         */
        /* Similarly, should we lose this dateFormat thing as well? 
         * the UNF should be calculatable on the app side solely from the data
         * stored in the tab file and the type information stored the dataVariable
         * object. 
         * furthermore, the very idea of storing a format entry not just for 
         * every variable, but for every value/observation is a bit strange. 
         * TODO: review and confirm that, in the 3.* implementation, every
         * entry in dateFormat[nvar][*] is indeed the same - except for the 
         * missing value entries. -- L.A. 4.0
         */
        String[][] dateFormat = new String[nvar][nobs];

        for (int i = 0; i < nobs; i++) {
            byte[] dataRowBytes = new byte[bytes_per_row];
            Object[] dataRow = new Object[nvar];

            int nbytes = stream.read(dataRowBytes, 0, bytes_per_row);

            if (nbytes == 0) {
                String errorMessage = "reading data: no data were read at("
                        + i + "th row)";
                throw new IOException(errorMessage);
            }
            // decoding each row
            int byte_offset = 0;
            for (int columnCounter = 0;
                    columnCounter < variableTypes.length; columnCounter++) {

                Integer varType
                        = variableTypeMap.get(variableTypes[columnCounter]);


                // 4.0 Check if this is a time/date variable: 
                boolean isDateTimeDatum = false; 
                String formatCategory = dataTable.getDataVariables().get(columnCounter).getFormatCategory();
                // REMOVED 4.0 boolean isDateTimeDatum = isDateTimeDatumList[columnCounter];
                if (formatCategory != null && (formatCategory.equals("time") || formatCategory.equals("date"))) {
                    isDateTimeDatum = true; 
                }
                // 4.0 String variableFormat = variableFormats[columnCounter];
                // 4.0 variableFormat: used for time and date values:
                String variableFormat = dataTable.getDataVariables().get(columnCounter).getFormatSchemaName();

                switch (varType != null ? varType : 256) {
                    case -5:
                        // Byte case
                        // note: 1 byte signed
                        byte byte_datum = dataRowBytes[byte_offset];

                        if (dbgLog.isLoggable(Level.FINER)) {
                            dbgLog.finer(i + "-th row " + columnCounter
                                    + "=th column byte =" + byte_datum);
                        }
                        if (byte_datum >= BYTE_MISSING_VALUE) {
                            if (dbgLog.isLoggable(Level.FINER)) {
                                dbgLog.finer(i + "-th row " + columnCounter
                                        + "=th column byte MV=" + byte_datum);
                            }
                            dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                            /* REMOVE dataTable2[columnCounter][i] = null;  //use null reference to indicate missing value in data that is passed to UNF */
                        } else {
                            dataRow[columnCounter] = byte_datum;
                            /* REMOVE dataTable2[columnCounter][i] = byte_datum; */
                        }

                        byte_offset++;
                        break;
                    case -4:
                        // Stata-int (=java's short: 2byte) case
                        // note: 2-byte signed int, not java's int
                        ByteBuffer int_buffer
                                = ByteBuffer.wrap(dataRowBytes, byte_offset, 2);
                        if (isLittleEndian) {
                            int_buffer.order(ByteOrder.LITTLE_ENDIAN);

                        }
                        short short_datum = int_buffer.getShort();

                        if (dbgLog.isLoggable(Level.FINER)) {
                            dbgLog.finer(i + "-th row " + columnCounter
                                    + "=th column stata int =" + short_datum);
                        }
                        if (short_datum >= INT_MISSIG_VALUE) {
                            if (dbgLog.isLoggable(Level.FINER)) {
                                dbgLog.finer(i + "-th row " + columnCounter
                                        + "=th column stata long missing value=" + short_datum);
                            }
                            /* REMOVE dataTable2[columnCounter][i] = null;  //use null reference to indicate missing value in data that is passed to UNF */
                            // 4.0 if (isDateTimeDatum) {
                            dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                            // 4.0 } else {
                            // 4.0     dataRow[columnCounter] = MissingValueForTextDataFileNumeric;
                            // 4.0 }
                        } else {

                            if (isDateTimeDatum) {

                                DecodedDateTime ddt = decodeDateTimeData("short", variableFormat, Short.toString(short_datum));
                                if (dbgLog.isLoggable(Level.FINER)) {
                                    dbgLog.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                                }
                                dataRow[columnCounter] = ddt.decodedDateTime;
                                dateFormat[columnCounter][i] = ddt.format;
                                /* REMOVE dataTable2[columnCounter][i] = dataRow[columnCounter]; */

                            } else {
                                /* REMOVE dataTable2[columnCounter][i] = short_datum; */
                                dataRow[columnCounter] = short_datum;
                            }
                        }
                        byte_offset += 2;
                        break;
                    case -3:
                        // stata-Long (= java's int: 4 byte) case
                        // note: 4-byte singed, not java's long
                        //dbgLog.fine("DATreader: stata long");

                        ByteBuffer long_buffer
                                = ByteBuffer.wrap(dataRowBytes, byte_offset, 4);
                        if (isLittleEndian) {
                            long_buffer.order(ByteOrder.LITTLE_ENDIAN);

                        }
                        int int_datum = long_buffer.getInt();

                        if (dbgLog.isLoggable(Level.FINE)) {
                            //dbgLog.fine(i + "-th row " + columnCounter
                            //        + "=th column stata long =" + int_datum);
                        }
                        if (int_datum >= LONG_MISSING_VALUE) {
                            if (dbgLog.isLoggable(Level.FINE)) {
                                //dbgLog.fine(i + "-th row " + columnCounter
                                //        + "=th column stata long missing value=" + int_datum);
                            }
                            /* REMOVE dataTable2[columnCounter][i] = null;  //use null reference to indicate missing value in data that is passed to UNF */
                            // 4.0 if (isDateTimeDatum) {
                            dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                            // 4.0 } else {
                            //     dataRow[columnCounter] = MissingValueForTextDataFileNumeric;
                            // }
                        } else {
                            if (isDateTimeDatum) {
                                DecodedDateTime ddt = decodeDateTimeData("int", variableFormat, Integer.toString(int_datum));
                                if (dbgLog.isLoggable(Level.FINER)) {
                                    dbgLog.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                                }
                                dataRow[columnCounter] = ddt.decodedDateTime;
                                dateFormat[columnCounter][i] = ddt.format;
                                /* REMOVE dataTable2[columnCounter][i] = dataRow[columnCounter]; */

                            } else {
                                /* REMOVE dataTable2[columnCounter][i] = int_datum; */
                                dataRow[columnCounter] = int_datum;
                            }

                        }
                        byte_offset += 4;
                        break;
                    case -2:
                        // float case
                        // note: 4-byte
                        ByteBuffer float_buffer
                                = ByteBuffer.wrap(dataRowBytes, byte_offset, 4);
                        if (isLittleEndian) {
                            float_buffer.order(ByteOrder.LITTLE_ENDIAN);
                        }
                        float float_datum = float_buffer.getFloat();

                        if (dbgLog.isLoggable(Level.FINER)) {
                            dbgLog.finer(i + "-th row " + columnCounter
                                    + "=th column float =" + float_datum);
                        }
                        if (FLOAT_MISSING_VALUE_SET.contains(float_datum)) {
                            if (dbgLog.isLoggable(Level.FINER)) {
                                dbgLog.finer(i + "-th row " + columnCounter
                                        + "=th column float missing value=" + float_datum);
                            }
                            /* REMOVE dataTable2[columnCounter][i] = null;  //use null reference to indicate missing value in data that is passed to UNF */
                            // 4.0 if (isDateTimeDatum) {
                            dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                            // 4.0 } else {
                            //    dataRow[columnCounter] = MissingValueForTextDataFileNumeric;
                            // }

                        } else {

                            if (isDateTimeDatum) {
                                DecodedDateTime ddt = decodeDateTimeData("float", variableFormat, doubleNumberFormatter.format(float_datum));
                                if (dbgLog.isLoggable(Level.FINER)) {
                                    dbgLog.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                                }
                                dataRow[columnCounter] = ddt.decodedDateTime;
                                dateFormat[columnCounter][i] = ddt.format;
                                /* REMOVE dataTable2[columnCounter][i] = dataRow[columnCounter]; */
                            } else {
                                /* REMOVE dataTable2[columnCounter][i] = float_datum;*/
                                dataRow[columnCounter] = float_datum;
                            }

                        }
                        byte_offset += 4;
                        break;
                    case -1:
                        // double case
                        // note: 8-byte
                        ByteBuffer double_buffer
                                = ByteBuffer.wrap(dataRowBytes, byte_offset, 8);
                        if (isLittleEndian) {
                            double_buffer.order(ByteOrder.LITTLE_ENDIAN);
                        }
                        double double_datum = double_buffer.getDouble();

                        if (DOUBLE_MISSING_VALUE_SET.contains(double_datum)) {
                            /* REMOVE dataTable2[columnCounter][i] = null;  //use null reference to indicate missing value in data that is passed to UNF */
                            if (dbgLog.isLoggable(Level.FINER)) {
                                dbgLog.finer(i + "-th row " + columnCounter
                                        + "=th column double missing value=" + double_datum);
                            }
                            // 4.0 if (isDateTimeDatum) {
                            dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                            // 4.0 } else {
                            //     dataRow[columnCounter] = MissingValueForTextDataFileNumeric;
                            // }
                        } else {

                            if (isDateTimeDatum) {
                                DecodedDateTime ddt = decodeDateTimeData("double", variableFormat, doubleNumberFormatter.format(double_datum));
                                if (dbgLog.isLoggable(Level.FINER)) {
                                    dbgLog.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                                }
                                dataRow[columnCounter] = ddt.decodedDateTime;
                                dateFormat[columnCounter][i] = ddt.format;
                                /* REMOVE dataTable2[columnCounter][i] = dataRow[columnCounter]; */
                            } else {
                                /* REMOVE dataTable2[columnCounter][i] = double_datum; */
                                dataRow[columnCounter] = doubleNumberFormatter.format(double_datum);
                            }

                        }
                        byte_offset += 8;
                        break;
                    case 0:
                        // String case
                        int strVarLength = StringLengthTable.get(columnCounter);
                        String raw_datum = new String(Arrays.copyOfRange(dataRowBytes, byte_offset,
                                (byte_offset + strVarLength)), "ISO-8859-1");
                        String string_datum = getNullStrippedString(raw_datum);
                        if (dbgLog.isLoggable(Level.FINER)) {
                            dbgLog.finer(i + "-th row " + columnCounter
                                    + "=th column string =" + string_datum);
                        }
                        if (string_datum.equals("")) {
                            if (dbgLog.isLoggable(Level.FINER)) {
                                dbgLog.finer(i + "-th row " + columnCounter
                                        + "=th column string missing value=" + string_datum);
                            }
                            // TODO: 
                            /* Is this really a missing value case? 
                             * Or is it an honest empty string? 
                             * Is there such a thing as a missing value for a String in Stata?
                             * -- L.A. 4.0
                             */
                            dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                            /* REMOVE dataTable2[columnCounter][i] = null;  //use null reference to indicate missing value in data that is passed to UNF */
                        } else {
                            String escapedString = string_datum.replaceAll("\"", Matcher.quoteReplacement("\\\""));
                            /*
                             * Fixing the bug we've had in the Stata reader for 
                             * a longest time: new lines and tabs need to 
                             * be escaped too - otherwise it breaks our 
                             * TAB file structure! -- L.A. 
                             */
                            escapedString = escapedString.replaceAll("\t", Matcher.quoteReplacement("\\t"));
                            escapedString = escapedString.replaceAll("\n", Matcher.quoteReplacement("\\n"));
                            escapedString = escapedString.replaceAll("\r", Matcher.quoteReplacement("\\r"));
                            // the escaped version of the string will be 
                            // stored in the tab file: 
                            dataRow[columnCounter] = "\"" + escapedString + "\"";
                            /* TODO: figure out what we are going to do with these escaped tabs and newlines
                             * on the application side, when we calculate the UNF for the variable...
                             * Are we going to try and convert these back to the unescaped version?..
                             * (we'll probably want to escape the back slash itself - "\"
                             * before we escape anything else)
                             * -- L.A. 4.0
                             */
                            // but note that the "raw" version of it is 
                            // used for the UNF:
                            /* REMOVE dataTable2[columnCounter][i] = string_datum; */
                        }
                        byte_offset += strVarLength;
                        break;
                    default:
                        dbgLog.fine("unknown variable type found");
                        String errorMessage
                                = "unknow variable Type found at data section";
                        throw new InvalidObjectException(errorMessage);
                } // switch
            } // for-columnCounter

            // dump the row of data to the external file
            pwout.println(StringUtils.join(dataRow, "\t"));

            if (dbgLog.isLoggable(Level.FINE)) {
                //dbgLog.fine(i + "-th row's data={" + StringUtils.join(dataRow, ",") + "};");
            }

        }  // for- i (row)

        pwout.close();

        if (dbgLog.isLoggable(Level.FINER)) {
            /* REMOVE 
             dbgLog.finer("\ndataTable2(variable-wise):\n");
             dbgLog.finer(Arrays.deepToString(dataTable2)); */
            dbgLog.finer("\ndateFormat(variable-wise):\n");
            dbgLog.finer(Arrays.deepToString(dateFormat));
        }
        if (dbgLog.isLoggable(Level.FINE)) {
            dbgLog.fine("variableTypes:\n" + Arrays.deepToString(variableTypes));
            // dbgLog.fine("variableTypesFinal:\n" + Arrays.deepToString(variableTypesFinal));
        }

        dbgLog.fine("DTA Ingest: decodeData(): end.");

    }


    private class DecodedDateTime {
        String format;
        String decodedDateTime;
    }

    private DecodedDateTime decodeDateTimeData(String storageType, String FormatType, String rawDatum) throws IOException {

        if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("(storageType, FormatType, rawDatum)=("+
        storageType +", " +FormatType +", " +rawDatum+")");
        /*
         *         Historical note:
                   pseudofunctions,  td(), tw(), tm(), tq(), and th()
                used to be called     d(),  w(),  m(),  q(), and  h().
                Those names still work but are considered anachronisms.

        */
        
        long milliSeconds;
        String decodedDateTime=null;
        String format = null;

        if (FormatType.matches("^%tc.*")){
            // tc is a relatively new format
            // datum is millisecond-wise

            milliSeconds = Long.parseLong(rawDatum)+ STATA_BIAS_TO_EPOCH;
            decodedDateTime = sdf_ymdhmsS.format(new Date(milliSeconds));
            format = sdf_ymdhmsS.toPattern();
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("tc: result="+decodedDateTime+", format = "+format);
            
        } else if (FormatType.matches("^%t?d.*")){
            milliSeconds = Long.parseLong(rawDatum)*SECONDS_PER_YEAR + STATA_BIAS_TO_EPOCH;
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("milliSeconds="+milliSeconds);
            
            decodedDateTime = sdf_ymd.format(new Date(milliSeconds));
            format = sdf_ymd.toPattern();
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("td:"+decodedDateTime+", format = "+format);

        } else if (FormatType.matches("^%t?w.*")){

            long weekYears = Long.parseLong(rawDatum);
            long left = Math.abs(weekYears)%52L;
            long years;
            if (weekYears < 0L){
                left = 52L - left;
                if (left == 52L){
                    left = 0L;
                }
                //out.println("left="+left);
                years = (Math.abs(weekYears) -1)/52L +1L;
                years *= -1L;
            } else {
                years = weekYears/52L;
            }

            String yearString  = Long.valueOf(1960L + years).toString();
            String dayInYearString = new DecimalFormat("000").format((left*7) + 1).toString();
            String yearDayInYearString = yearString + "-" + dayInYearString;

            Date tempDate = null;
            try {
                tempDate = new SimpleDateFormat("yyyy-DDD").parse(yearDayInYearString);
            } catch (ParseException ex) {
                throw new IOException(ex);
            }
            
            decodedDateTime = sdf_ymd.format(tempDate.getTime());
            format = sdf_ymd.toPattern();

        } else if (FormatType.matches("^%t?m.*")){
            // month 
            long monthYears = Long.parseLong(rawDatum);
            long left = Math.abs(monthYears)%12L;
            long years;
            if (monthYears < 0L){
                left = 12L - left;
                //out.println("left="+left);
                years = (Math.abs(monthYears) -1)/12L +1L;
                years *= -1L;
            } else {
                years = monthYears/12L;
            }

            String month = null;
            if (left == 12L){
                left = 0L;
            }
            Long monthdata = (left+1);
            month = "-"+twoDigitFormatter.format(monthdata).toString()+"-01";
            long year  = 1960L + years;
            String monthYear = Long.valueOf(year).toString() + month;
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("rawDatum="+rawDatum+": monthYear="+monthYear);
            
            decodedDateTime = monthYear;
            format = "yyyy-MM-dd";
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("tm:"+decodedDateTime+", format:"+format);

        } else if (FormatType.matches("^%t?q.*")){
            // quater
            long quaterYears = Long.parseLong(rawDatum);
            long left = Math.abs(quaterYears)%4L;
            long years;
            if (quaterYears < 0L){
                left = 4L - left;
                //out.println("left="+left);
                years = (Math.abs(quaterYears) -1)/4L +1L;
                years *= -1L;
            } else {
                years = quaterYears/4L;
            }

            String quater = null;

            if ((left == 0L) || (left == 4L)){
                //quater ="q1"; //
                quater = "-01-01";
            } else if (left ==1L) {
                //quater = "q2"; //
                quater = "-04-01";
            } else if (left ==2L) {
                //quater = "q3"; //
                quater = "-07-01";
            } else if (left ==3L) {
                //quater = "q4"; //
                quater = "-11-01";
            }

            long year  = 1960L + years;
            String quaterYear = Long.valueOf(year).toString() + quater;
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("rawDatum="+rawDatum+": quaterYear="+quaterYear);

            decodedDateTime = quaterYear;
            format = "yyyy-MM-dd";
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("tq:"+decodedDateTime+", format:"+format);

        } else if (FormatType.matches("^%t?h.*")){
            // half year
            // odd number:2nd half
            // even number: 1st half
            
            long halvesYears = Long.parseLong(rawDatum);
            long left = Math.abs(halvesYears)%2L;
            long years;
            if (halvesYears < 0L){
                years = (Math.abs(halvesYears) -1)/2L +1L;
                years *= -1L;
            } else {
                years = halvesYears/2L;
            }

            String half = null;
            if (left != 0L){
                // odd number => 2nd half: "h2"
                //half ="h2"; //
                half = "-07-01";
            } else {
                // even number => 1st half: "h1"
                //half = "h1"; //
                half = "-01-01";
            }
            long year  = 1960L + years;
            String halfYear = Long.valueOf(year).toString() + half;
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("rawDatum="+rawDatum+": halfYear="+halfYear);
            
            decodedDateTime = halfYear;
            format = "yyyy-MM-dd";
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("th:"+decodedDateTime+", format:"+format);
            
        } else if (FormatType.matches("^%t?y.*")){
            // year type's origin is 0 AD
            decodedDateTime = rawDatum;
            format = "yyyy";
            if (dbgLog.isLoggable(Level.FINER)) dbgLog.finer("th:"+decodedDateTime);
        } else {
            decodedDateTime = rawDatum;
            format=null;
        }
        DecodedDateTime retValue = new DecodedDateTime();
        retValue.decodedDateTime = decodedDateTime;
        retValue.format = format;
        return retValue;
    }

}

