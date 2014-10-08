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
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.SummaryStatistic;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.datavariable.VariableFormatType;
import edu.harvard.iq.dataverse.datavariable.VariableRange;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;

import edu.harvard.iq.dataverse.ingest.plugin.spi.*;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;


/**
 * ingest plugin for Stata 13 (117) DTA file format.
 *
 * This ingest plugin has been written from scratch for the DVN 4.0, 
 * since this file format, introduced in STATA 13 is a brand new 
 * development, independent of and incompatible with the old, "classic" 
 * dta format.
 * 
 * For the format documentation, see http://www.stata.com/help.cgi?dta
 * @author Leonid Andreev
 */

public class DTA117FileReader extends TabularDataFileReader{
    @Inject
    VariableServiceBean varService;
    // static fields, STATA-specific constants, etc. 
    
    // SECTION TAGS:
    // 
    // The new STATA format features XML-like section tags - 
    // <stata_dta><header>...</header>...</stata_dta>
    
    // MAIN, TOP-LEVEL FILE SECTION:
    
    private static final String TAG_DTA_117 = "stata_dta";
    
    // HEADER SECTION: 
    
    private static final String TAG_HEADER = "header";
    private static final String TAG_HEADER_FILEFORMATID = "release";
    private static final String TAG_HEADER_BYTEORDER = "byteorder";
    private static final String TAG_HEADER_VARNUMBER = "K";
    private static final String TAG_HEADER_OBSNUMBER = "N";
    private static final String TAG_HEADER_FILELABEL = "label";
    private static final String TAG_HEADER_TIMESTAMP = "timestamp";
    
    // MAP SECTION: 
    
    private static final String TAG_MAP = "map";
    
    // VARIABLE TYPES SECTION: 
    
    private static final String TAG_VARIABLE_TYPES = "variable_types";
    
    // VARIABLE NAMES SECTION: 
    
    private static final String TAG_VARIABLE_NAMES = "varnames";
    
    // VARIABLE SORT ORDER SECTION: 
    
    private static final String TAG_SORT_ORDER = "sortlist";
    
    // VARIABLE DISPLAY FORMATS: 
    
    private static final String TAG_DISPLAY_FORMATS = "formats";
    
    // VALUE LABEL FORMAT NAMES: 
    // (TODO: add a comment)
    
    private static final String TAG_VALUE_LABEL_FORMAT_NAMES = "value_label_names"; 
    
    // VARIABLE LABELS: 
    
    private static final String TAG_VARIABLE_LABELS = "variable_labels";
    
    // "CHARACTERISTICS":
    
    private static final String TAG_CHARACTERISTICS = "characteristics";
    private static final String TAG_CHARACTERISTICS_SUBSECTION = "ch";
    
    // DATA SECTION!
    
    private static final String TAG_DATA = "data";
    
    // STRLs SECTION: 
    
    private static final String TAG_STRLS = "strls";
    
    // VALUE LABELS SECTION:
    
    private static final String TAG_VALUE_LABELS = "value_labels";
    private static final String TAG_VALUE_LABELS_LBL_DEF = "lbl";
    
    // (TODO: should the constants below be isolated in some other class, that 
    // could be shared between the 2 STATA DTA reader plugins?

    private static Map<Integer, String> STATA_RELEASE_NUMBER = 
            new HashMap<Integer, String>();
    
    private static Map<Integer, Map<String, Integer>> CONSTATNT_TABLE =
            new LinkedHashMap<Integer, Map<String, Integer>>();

    private static Map<String, Integer> release117constant =
                                        new LinkedHashMap<String, Integer>();
                                        
    
    private static Map<String, Integer> byteLengthTable117 = 
                                        new HashMap<String, Integer>();
    
    private static Map<Integer, String> variableTypeTable117 = 
                                        new LinkedHashMap<Integer, String>();

    private static final int[] LENGTH_HEADER = {60, 109};
    private static final int[] LENGTH_LABEL = {32, 81};
    private static final int[] LENGTH_NAME = {9, 33};
    private static final int[] LENGTH_FORMAT_FIELD = {7, 12, 49};
    private static final int[] LENGTH_EXPANSION_FIELD ={0, 2, 4};
    private static final int[] DBL_MV_PWR = {333, 1023};
    
    private static final int DTA_MAGIC_NUMBER_LENGTH = 4;
    private static final int NVAR_FIELD_LENGTH       = 2;
    private static final int NOBS_FIELD_LENGTH       = 4;
    private static final int TIME_STAMP_LENGTH      = 18;
    private static final int VAR_SORT_FIELD_LENGTH   = 2;
    private static final int VALUE_LABEL_HEADER_PADDING_LENGTH = 3;
   
    private static int MISSING_VALUE_BIAS = 26;

    private byte BYTE_MISSING_VALUE = Byte.MAX_VALUE;
    private short INT_MISSIG_VALUE = Short.MAX_VALUE;
    private int LONG_MISSING_VALUE = Integer.MAX_VALUE;
    
    // Static initialization: 
 
    static {
        
        STATA_RELEASE_NUMBER.put(117, "v.13");
        
        release117constant.put("HEADER",     LENGTH_HEADER[1]);
        release117constant.put("LABEL",     LENGTH_LABEL[1]);
        release117constant.put("NAME",      LENGTH_NAME[1]);
        release117constant.put("FORMAT",    LENGTH_FORMAT_FIELD[1]);
        release117constant.put("EXPANSION", LENGTH_EXPANSION_FIELD[2]);
        release117constant.put("DBL_MV_PWR",DBL_MV_PWR[1]);
        
        CONSTATNT_TABLE.put(117, release117constant);
        
        // 1, 2 and 4-byte integers: 
        byteLengthTable117.put("Byte",1);
        byteLengthTable117.put("Integer",2);
        byteLengthTable117.put("Long",4);
        // 4 and 8-byte floats: 
        byteLengthTable117.put("Float",4);
        byteLengthTable117.put("Double",8);
        // STRLs are defined in their own section, outside of the 
        // main data. In the <data> section they are referenced 
        // by 2 x 4 byte values, "(v,o)", 8 bytes total.
        byteLengthTable117.put("STRL",8);

        variableTypeTable117.put(65530,"Byte");
        variableTypeTable117.put(65529,"Integer");
        variableTypeTable117.put(65528,"Long");
        variableTypeTable117.put(65527,"Float");
        variableTypeTable117.put(65526,"Double");
        
        //variableTypeTable117.put(32768,"STRL");
    }
    
    private static String[] MIME_TYPE = {
            "application/x-stata", 
            "application/x-stata-13"
        };

    
    private static String unfVersionNumber = "6";

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

    private static long SECONDS_PER_YEAR = 24*60*60*1000L; // TODO: huh?

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
    




    // instance fields //

    private static Logger logger = Logger.getLogger(DTAFileReader.class.getPackage().getName());

    private DataTable dataTable = new DataTable();

    private DTADataMap dtaMap = null; 

    
    // TODO: 
    // add a comment explaining what this table is for: 
    // -- L.A. 4.0
    private String[] valueLabelsLookupTable = null; 
    
    private Map<String, Integer> constantTable ;

    private Map<String, Integer> byteLengthTable;

    private Map<Integer, String> variableTypeTable;



    private NumberFormat twoDigitFormatter = new DecimalFormat("00");

    private NumberFormat doubleNumberFormatter = new DecimalFormat();

    TabularDataIngest ingesteddata = new TabularDataIngest();


    private int releaseNumber;

    private int headerLength;

    private int dataLabelLength;

    private boolean isLittleEndian = false;
    

    // TODO: 
    // rewrite this comment? 
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
     */
    
    private String[] variableTypes=null;
    
    private String[] dateVariableFormats=null; 
          
    private static final String MissingValueForTabDelimitedFile = "";
    
  
    // Constructor -----------------------------------------------------------//

    public DTA117FileReader(TabularDataFileReaderSpi originator){
        super(originator);
    }


    /*
     * This method configures Stata's release-specific parameters:
     */
    // TODO: this method needs to be actually called! 
    private void init() throws IOException {
        //
        logger.info("release number=" + releaseNumber);

        variableTypeTable = variableTypeTable117;

        byteLengthTable = byteLengthTable117;
        BYTE_MISSING_VALUE -= MISSING_VALUE_BIAS;
        INT_MISSIG_VALUE -= MISSING_VALUE_BIAS;
        LONG_MISSING_VALUE -= MISSING_VALUE_BIAS;

        constantTable = CONSTATNT_TABLE.get(releaseNumber);

        headerLength = constantTable.get("HEADER") - DTA_MAGIC_NUMBER_LENGTH;

        dataLabelLength = headerLength - (NVAR_FIELD_LENGTH
                + NOBS_FIELD_LENGTH + TIME_STAMP_LENGTH);
        logger.fine("data_label_length=" + dataLabelLength);

        logger.fine("constant table to be used:\n" + constantTable);

        doubleNumberFormatter.setGroupingUsed(false);
        doubleNumberFormatter.setMaximumFractionDigits(340); // TODO: WTF???
        
        Context ctx = null;
        
        try {
            ctx = new InitialContext();
            varService = (VariableServiceBean) ctx.lookup("java:global/dataverse-4.0/VariableServiceBean");
        } catch (NamingException nex) {
            try {
                ctx = new InitialContext();
                varService = (VariableServiceBean) ctx.lookup("java:global/dataverse/VariableServiceBean");
            } catch (NamingException nex2) {
                logger.info("Could not look up initial context, or the variable service in JNDI!");
                throw new IOException("Could not look up initial context, or the variable service in JNDI!");
            }
        }
    }

    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {
        logger.info("DTA117FileReader: read() start");

        // shit ton of diagnostics (still) needed here!!  -- L.A.
        
        if (dataFile != null) {
            throw new IOException ("this plugin does not support external raw data files");
        }

        DataReader dataReader = null;
        
        try {
            init();

            // create a new instance of DataReader:
            dataReader = new DataReader(stream);
            // and read the opening tag:
            dataReader.readOpeningTag(TAG_DTA_117);
            
            // ...and if we've made this far, we can try 
            // and read the header section: 
            readHeader(dataReader);
            
            // then the map: 
            readMap(dataReader);
            
            // variable types: 
            readVariableTypes(dataReader);
            
            // variable names:
            readVariableNames(dataReader);
            
            // sort order: 
            readSortOrder(dataReader);
            
            // value label formats:
            readValueLabelFormatNames(dataReader); 
            
            // variable labels: 
            readVariableLabels(dataReader); 
            
            // "characteristics" - STATA-proprietary information
            // (we are skipping it)
            readCharacteristics(dataReader); 
            
            // Data!
            readData(dataReader); 
            
            // STRLs: 
            // (potentially) large, (potentially) non-ASCII character strings
            // saved outside the <data>...</data> section, and referenced 
            // in the data rows using (v,o) notation - see the documentation 
            // for more information. 
            readSTRLs(dataReader); 
            
            // finally, Value Labels:
            readValueLabels(dataReader); 
            
            // verify that we've reached the final closing tag:
            dataReader.readClosingTag(TAG_DTA_117);
            
            ingesteddata.setDataTable(dataTable);
        } catch (IllegalArgumentException iaex) {
            throw new IOException(iaex.getMessage());
        }
        
        logger.info("DTA117FileReader: read() end.");
        return ingesteddata;
    }



    private void readHeader(DataReader dataReader) throws IOException {
        logger.info("readHeader(): start");

        if (dataReader == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        logger.info("reading the version header.");
               
        dataReader.readOpeningTag(TAG_HEADER);
        String dtaVersionTag = dataReader.readPrimitiveStringSection(TAG_HEADER_FILEFORMATID, 3);

        if (!"117".equals(dtaVersionTag)) {
            throw new IOException("Unexpected version tag found: "+dtaVersionTag+"; expected value: 117.");
        }
        
        String byteOrderTag = dataReader.readPrimitiveStringSection(TAG_HEADER_BYTEORDER);
        
        logger.info("byte order: "+byteOrderTag);
        
        if ("LSF".equals(byteOrderTag)) {
            dataReader.setLSF(true);
        } else if ("MSF".equals(byteOrderTag)) {
            dataReader.setLSF(false);
        }
        
        int varNumber = dataReader.readIntegerSection(TAG_HEADER_VARNUMBER, 2);
        logger.info("number of variables: " + varNumber);

        int obsNumber = dataReader.readIntegerSection(TAG_HEADER_OBSNUMBER, 4);
        logger.info("number of observations: " + obsNumber);
        
        dataTable.setVarQuantity(new Long(varNumber));
        dataTable.setCaseQuantity(new Long(obsNumber));
        

        dataTable.setOriginalFileFormat(MIME_TYPE[0]);
        dataTable.setOriginalFormatVersion("STATA 13");
        dataTable.setUnf("UNF:pending");

        // The word "dataset" below is used in its STATA parlance meaning, 
        // i.e., this is a label that describes the datafile. 
        String datasetLabel = dataReader.readDefinedStringSection(TAG_HEADER_FILELABEL, 80);
        logger.info("dataset label: "+datasetLabel);
        
        // TODO: 
        // do we want to do anything with this label? Add it to the 
        // filemetadata, similarly to what we do with those auto-generated
        // FITS descriptive labels maybe? 
        // (similarly, what to do with the date stamp, below?)
        // -- L.A. 4.0 beta 8
        
        String datasetTimeStamp = dataReader.readDefinedStringSection(TAG_HEADER_TIMESTAMP, 17);
        logger.info("dataset time stamp: "+datasetTimeStamp);
        
        if (datasetTimeStamp == null ||
                (datasetTimeStamp.length() > 0 && datasetTimeStamp.length() < 17)) {
            throw new IOException("unexpected/invalid length of the time stamp in the DTA117 header.");
        } else {
            // TODO: validate the time stamp found against dd Mon yyyy hh:mm; 
            // ...but first decide if we actually want/need to use it for any 
            // practical purposes...
        }
        
        dataReader.readClosingTag("header");
        logger.info("readHeader(): end");
    }

    /* 
        TODO: add a comment. --L.A. DVN 4.0 beta 8 
    */
    private void readMap(DataReader reader) throws IOException {
        reader.readOpeningTag(TAG_MAP);

        dtaMap = new DTADataMap();

        long dta_offset_stata_data = reader.readLongInteger();
        logger.info("dta_offset_stata_data: " + dta_offset_stata_data);
        dtaMap.setOffset_head(dta_offset_stata_data);
        long dta_offset_map = reader.readLongInteger();
        logger.info("dta_offset_map: " + dta_offset_map);
        dtaMap.setOffset_map(dta_offset_map);
        long dta_offset_variable_types = reader.readLongInteger();
        logger.info("dta_offset_variable_types: " + dta_offset_variable_types);
        dtaMap.setOffset_types(dta_offset_variable_types);
        long dta_offset_varnames = reader.readLongInteger();
        logger.info("dta_offset_varnames: " + dta_offset_varnames);
        dtaMap.setOffset_varnames(dta_offset_varnames);
        long dta_offset_sortlist = reader.readLongInteger();
        logger.info("dta_offset_sortlist: " + dta_offset_sortlist);
        dtaMap.setOffset_srtlist(dta_offset_sortlist);
        long dta_offset_formats = reader.readLongInteger();
        logger.info("dta_offset_formats: " + dta_offset_formats);
        dtaMap.setOffset_fmts(dta_offset_formats);
        long dta_offset_value_label_names = reader.readLongInteger();
        logger.info("dta_offset_value_label_names: " + dta_offset_value_label_names);
        dtaMap.setOffset_vlblnames(dta_offset_value_label_names);
        long dta_offset_variable_labels = reader.readLongInteger();
        logger.info("dta_offset_variable_labels: " + dta_offset_variable_labels);
        dtaMap.setOffset_varlabs(dta_offset_variable_labels);
        long dta_offset_characteristics = reader.readLongInteger();
        logger.info("dta_offset_characteristics: " + dta_offset_characteristics);
        dtaMap.setOffset_characteristics(dta_offset_characteristics);
        long dta_offset_data = reader.readLongInteger();
        logger.info("dta_offset_data: " + dta_offset_data);
        dtaMap.setOffset_data(dta_offset_data);
        long dta_offset_strls = reader.readLongInteger();
        logger.info("dta_offset_strls: " + dta_offset_strls);
        dtaMap.setOffset_strls(dta_offset_strls);
        long dta_offset_value_labels = reader.readLongInteger();
        logger.info("dta_offset_value_labels: " + dta_offset_value_labels);
        dtaMap.setOffset_vallabs(dta_offset_value_labels);
        long dta_offset_data_close = reader.readLongInteger();
        logger.info("dta_offset_data_close: " + dta_offset_data_close);
        dtaMap.setOffset_data_close(dta_offset_data_close);
        long dta_offset_eof = reader.readLongInteger();
        logger.info("dta_offset_eof: " + dta_offset_eof);
        dtaMap.setOffset_eof(dta_offset_eof);

        reader.readClosingTag(TAG_MAP);

    }
    
    /* 
     * Variable type information is stored in the <variable_types>...</variable_types>
     * section, as number_of_variables * 2 byte values. 
     * the type codes are defined as follows: 
     * (TODO: ...)
    */
    
    private void readVariableTypes(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VARIABLE_TYPES);
        
        List<DataVariable> variableList = new ArrayList<DataVariable>();
        // setup variableTypeList
        variableTypes = new String[dataTable.getVarQuantity().intValue()];
            
        
        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            // TODO: create a readShort() method;
            short type = (short)reader.readInteger(2);
            logger.info("variable "+i+": type="+type);
            // TODO: configure DataVariable here.
            DataVariable dv = new DataVariable();
            
            dv.setInvalidRanges(new ArrayList<VariableRange>());
            dv.setSummaryStatistics( new ArrayList<SummaryStatistic>());
            dv.setCategories(new ArrayList<VariableCategory>());
            
            dv.setUnf("UNF:pending");
            dv.setFileOrder(i);
            dv.setDataTable(dataTable);
            
            variableTypes[i] = configureVariableType(dv, type);
            // TODO: 
            // we could also calculate the byte offset table now, rather 
            // then figure it out later... - ?
             
            variableList.add(dv);

        }
        
        
        reader.readClosingTag(TAG_VARIABLE_TYPES);
        dataTable.setDataVariables(variableList);

    }
   
    // TODO: 
    // calculate bytes_per_row while we are here -- ?
    private String configureVariableType(DataVariable dv, short type) throws IOException {
        String typeLabel = null;

        if (variableTypeTable.containsKey(type)) {
            typeLabel = variableTypeTable.get(type);

            // TODO: get rid of the service lookups for known format and interval 
            // types, etc. -- L.A. 4.0
            VariableFormatType formatTypeNumeric = varService.findVariableFormatTypeByName("numeric");
            if (formatTypeNumeric == null) {
                throw new IOException("No numeric format type in the database. (has the db been populated with reference data?)");
            }
            dv.setVariableFormatType(formatTypeNumeric);
            if (typeLabel.equals("Byte") || typeLabel.equals("Integer") || typeLabel.equals("Long")) {
                // these are treated as discrete:
                dv.setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));

            } else if (typeLabel.equals("Float") || typeLabel.equals("Double")) {
                // these are treated as contiuous:
                dv.setVariableIntervalType(varService.findVariableIntervalTypeByName("continuous"));

            } else {
                throw new IOException("Unrecognized type label: " + typeLabel + " for Stata type value (short) " + type + ".");
            }

        } else {
            // String:
            //
            // -32768 - flexible length STRL;
            // 1 ... 2045 - fixed-length STRF;

            if (type == (short) -32768) {
                typeLabel = "STRL";

            } else if (type > 0 && type < 2046) {
                typeLabel = "STR" + type;
            } else {
                throw new IOException("unknown variable type value encountered: " + type);
            }

            dv.setVariableFormatType(varService.findVariableFormatTypeByName("character"));
            dv.setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
            
        }

        return typeLabel;

    }
    
    /* 
     * Variable Names are stored as number_of_variables * 33 byte long
     * (zero-padded and zero-terminated) character vectors. 
    */
    private void readVariableNames(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VARIABLE_NAMES);

        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            String variableName = reader.readString(33);
            logger.info("variable "+i+": type=" + variableName);
            if ((variableName != null) && (!variableName.equals(""))) {
                dataTable.getDataVariables().get(i).setName(variableName);
            } else {
                // TODO: decide if we should throw an exception here. 
            }
        }

        reader.readClosingTag(TAG_VARIABLE_NAMES);
    }
    
    /* 
     * TODO: add a comment
    */
    
    private void readSortOrder(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_SORT_ORDER);
        
        // TODO: initialize DataVariable objects here
        
        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            int order = reader.readInteger(2);
            logger.info("variable "+i+": sort order="+order);
            // TODO: 
            // Double-check that we don't really need this sort order 
            // for any practical purposes. 
            // -- L.A. 4.0 beta 8
        }
        
        reader.readClosingTag(TAG_SORT_ORDER);
    }
    
    /*
     * TODO: add a comment
    */
    /* Variable Formats are used exclusively for time and date variables. 
     * (TODO: but should we be using the decimal formats and such too? -- 4.0 beta 8)
     *      -- L.A. 4.0
     */
    
    private void readDisplayFormats(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_DISPLAY_FORMATS);

        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            String variableFormat = reader.readString(49);
            logger.info("variable "+i+": displayFormat=" + variableFormat);
            // TODO: 
            // Decide what we are doing with these. 
            // (saving them, for archival purposes?)
            
            // this is from the old plugin: 
            // TODO: review!
            
            String variableFormatKey = null;
            if (variableFormat.startsWith("%t")) {
                variableFormatKey = variableFormat.substring(0, 3);
            } else {
                variableFormatKey = variableFormat.substring(0, 2);
            }
            logger.fine(i + " th variableFormatKey=" + variableFormatKey);

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
                // TODO: revisit the whole "formatschemaname" thing; -- L.A. 
                // Instead of populating this field with the Stata's internal 
                // format token (??), we should put the actual format of the 
                // values that we store in the tab file. And the internal 
                // STATA format we'll keep in this array for now: 
                dateVariableFormats[i] = variableFormat; 
                //dataTable.getDataVariables().get(i).setFormatSchemaName(variableFormat);
                dataTable.getDataVariables().get(i).setFormatCategory(DATE_TIME_FORMAT_TABLE.get(variableFormatKey));
                logger.fine(i + "th var: category=" +
                        DATE_TIME_FORMAT_TABLE.get(variableFormatKey));
                dataTable.getDataVariables().get(i).setVariableFormatType(varService.findVariableFormatTypeByName("character"));
                dataTable.getDataVariables().get(i).setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
            } 
        }

        reader.readClosingTag(TAG_DISPLAY_FORMATS);
    }
    
    /*
     * Another fixed-field section
    */
    private void readValueLabelFormatNames(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VALUE_LABEL_FORMAT_NAMES);

        valueLabelsLookupTable = new String[dataTable.getVarQuantity().intValue()];
        
        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            String valueLabelFormat = reader.readString(31);
            logger.info("variable "+i+": value label format=" + valueLabelFormat);
            if ((valueLabelFormat != null) && (!valueLabelFormat.equals(""))) {
                valueLabelsLookupTable[i] = valueLabelFormat;
            }
        }

        reader.readClosingTag(TAG_VALUE_LABEL_FORMAT_NAMES);
        
    }
    
    /* 
     * Another fixed-field section
    */
    private void readVariableLabels(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VARIABLE_LABELS);
        
        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            String variableLabel = reader.readString(81);
            logger.info("variable "+i+": label=" + variableLabel);
            if ((variableLabel != null) && (!variableLabel.equals(""))) {
                dataTable.getDataVariables().get(i).setLabel(variableLabel);
            }
        }

        reader.readClosingTag(TAG_VARIABLE_LABELS);
    }
            
    /* 
     * TODO: add a comment
    */
    private void readCharacteristics(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_CHARACTERISTICS);
        
        reader.skipDefinedSections(TAG_CHARACTERISTICS_SUBSECTION); 
        
        reader.readClosingTag(TAG_CHARACTERISTICS);
    }
    
    private int calculateBytesPerRow(int[] variableByteLengths) throws IOException {
        if (variableByteLengths == null || variableByteLengths.length != dataTable.getVarQuantity()) {
            throw new IOException("<internal variable byte offsets table not properly configured>");
        }
        int bytes_per_row = 0;

        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            if (variableByteLengths[i] < 1) {
                throw new IOException("<bad variable byte offset: " + variableByteLengths[i] + ">");
            }
            bytes_per_row += variableByteLengths[i];
        }

        return bytes_per_row;
    }
    
    private int[] getVariableByteLengths(String[] variableTypes) throws IOException {
        if (variableTypes == null || variableTypes.length != dataTable.getVarQuantity()) {
            throw new IOException("<internal variable types not properly configured>");
        }
        
        int[] variableByteLengths = new int[dataTable.getVarQuantity().intValue()];
        
        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            variableByteLengths[i] = getVariableByteLength(variableTypes[i]);
        }
        
        return variableByteLengths;
    }
    
    private int getVariableByteLength(String variableType) throws IOException {
        int byte_length = 0;

        if (variableType == null || variableType.equals("")) {
            throw new IOException("<empty variable type in attempted byte length lookup.>");
        }
        if (byteLengthTable.containsKey(variableType)) {
            return byteLengthTable.get(variableType);
        }

        if (variableType.matches("^STR[1-9][0-9]*")) {
            String stringLengthToken = variableType.substring(3);
            Integer stringLength = null;
            try {
                stringLength = new Integer(stringLengthToken);
            } catch (NumberFormatException nfe) {
                stringLength = null;
            }
            if (stringLength == null || stringLength.intValue() < 1 || stringLength.intValue() > 2045) {
                throw new IOException("Invalid STRF encountered: " + variableType);
            }
            return stringLength.intValue();
        }
        
        throw new IOException ("Unknown/invalid variable type: "+variableType);
    }
    
    /* 
     * TODO: add comments.
     */
    private void readData(DataReader reader) throws IOException {
        logger.fine("readData(): start");
        // TODO: 
        // check that we are at the right byte offset!

        int nvar = dataTable.getVarQuantity().intValue();
        int nobs = dataTable.getCaseQuantity().intValue();

        int[] variableByteLengths = getVariableByteLengths(variableTypes);
        int bytes_per_row = calculateBytesPerRow(variableByteLengths);

        logger.fine("data dimensions[observations x variables] = (" + nobs + "x" + nvar + ")");
        logger.fine("bytes per row=" + bytes_per_row + " bytes");
        logger.fine("variableTypes=" + Arrays.deepToString(variableTypes));

        // create a File object to save the tab-delimited data file
        FileOutputStream fileOutTab = null;
        PrintWriter pwout = null;
        File tabDelimitedDataFile = File.createTempFile("tempTabfile.", ".tab");

        // save the temp tab-delimited file in the return ingest object:        
        ingesteddata.setTabDelimitedFile(tabDelimitedDataFile);

        fileOutTab = new FileOutputStream(tabDelimitedDataFile);
        pwout = new PrintWriter(new OutputStreamWriter(fileOutTab, "utf8"), true);

        for (int i = 0; i < nobs; i++) {
            //byte[] dataRowBytes = new byte[bytes_per_row];
            Object[] dataRow = new Object[nvar];

            //int nbytes = stream.read(dataRowBytes, 0, bytes_per_row);
            //dataRowBytes = reader.readBytes(bytes_per_row);
            // TODO: 
            // maybe intercept any potential exceptions here, and add more 
            // diagnostic info, before re-throwing...
            // TODO: 
            // Remove all the "islittleendian" logic; use the DataReader
            // functionaliy instead. 
            int byte_offset = 0;
            for (int columnCounter = 0; columnCounter < nvar; columnCounter++) {

                String varType = variableTypes[columnCounter];

                // 4.0 Check if this is a time/date variable: 
                boolean isDateTimeDatum = false;
                // TODO: 
                // make sure the formats are properly set! -- use the old 
                // plugin as a model... 
                String formatCategory = dataTable.getDataVariables().get(columnCounter).getFormatCategory();
                if (formatCategory != null && (formatCategory.equals("time") || formatCategory.equals("date"))) {
                    isDateTimeDatum = true;
                }

                // TODO: 
                // ditto
                String variableFormat = dateVariableFormats[columnCounter];

                if (varType == null || varType.equals("")) {
                    throw new IOException("Undefined variable type encountered in readData()");
                }

                // TODO: 
                // double-check that the missing values constants are still correct!
                if (varType.equals("Byte")) {
                    // (signed) Byte
                    byte byte_datum = reader.readSignedByte();

                    logger.finer(i + "-th row " + columnCounter
                            + "=th column byte =" + byte_datum);
                    if (byte_datum >= BYTE_MISSING_VALUE) {
                        logger.finer(i + "-th row " + columnCounter
                                + "=th column byte MV=" + byte_datum);
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {
                        dataRow[columnCounter] = byte_datum;
                    }

                    byte_offset++;
                } else if (varType.equals("Integer")) {
                    short short_datum = (short) reader.readSignedInteger(2);

                    logger.finer(i + "-th row " + columnCounter
                            + "=th column stata int =" + short_datum);

                    if (short_datum >= INT_MISSIG_VALUE) {
                        logger.finer(i + "-th row " + columnCounter
                                + "=th column stata long missing value=" + short_datum);
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {

                        if (isDateTimeDatum) {

                            DecodedDateTime ddt = decodeDateTimeData("short", variableFormat, Short.toString(short_datum));
                            logger.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                            dataRow[columnCounter] = ddt.decodedDateTime;
                            //dateFormat[columnCounter][i] = ddt.format;
                            dataTable.getDataVariables().get(columnCounter).setFormatSchemaName(ddt.format);

                        } else {
                            dataRow[columnCounter] = short_datum;
                        }
                    }
                    byte_offset += 2;
                } else if (varType.equals("Long")) {
                    // stata-Long (= java's int: 4 byte), signed.

                    int int_datum = reader.readSignedInteger(4);

                    if (int_datum >= LONG_MISSING_VALUE) {
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {
                        if (isDateTimeDatum) {
                            DecodedDateTime ddt = decodeDateTimeData("int", variableFormat, Integer.toString(int_datum));
                            logger.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                            dataRow[columnCounter] = ddt.decodedDateTime;
                            dataTable.getDataVariables().get(columnCounter).setFormatSchemaName(ddt.format);

                        } else {
                            dataRow[columnCounter] = int_datum;
                        }

                    }
                    byte_offset += 4;
                } else if (varType.equals("Float")) {
                    // STATA float 
                    // same as Java float - 4-byte

                    float float_datum = reader.readFloat();

                    logger.finer(i + "-th row " + columnCounter
                            + "=th column float =" + float_datum);
                    if (FLOAT_MISSING_VALUE_SET.contains(float_datum)) {
                        logger.finer(i + "-th row " + columnCounter
                                + "=th column float missing value=" + float_datum);
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;

                    } else {

                        if (isDateTimeDatum) {
                            DecodedDateTime ddt = decodeDateTimeData("float", variableFormat, doubleNumberFormatter.format(float_datum));
                            logger.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                            dataRow[columnCounter] = ddt.decodedDateTime;
                            dataTable.getDataVariables().get(columnCounter).setFormatSchemaName(ddt.format);
                        } else {
                            dataRow[columnCounter] = float_datum;
                            // This may be temporary - but for now (as in, while I'm testing 
                            // 4.0 ingest against 3.* ingest, I need to be able to tell if a 
                            // floating point value was a single, or double float in the 
                            // original STATA file: -- L.A. Jul. 2014
                            dataTable.getDataVariables().get(columnCounter).setFormatSchemaName("float");
                            // ?
                        }

                    }
                    byte_offset += 4;
                } else if (varType.equals("Double")) {
                    // STATA double
                    // same as Java double - 8-byte

                    double double_datum = reader.readDouble();

                    if (DOUBLE_MISSING_VALUE_SET.contains(double_datum)) {
                        logger.finer(i + "-th row " + columnCounter
                                + "=th column double missing value=" + double_datum);
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {

                        if (isDateTimeDatum) {
                            DecodedDateTime ddt = decodeDateTimeData("double", variableFormat, doubleNumberFormatter.format(double_datum));
                            logger.finer(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                            dataRow[columnCounter] = ddt.decodedDateTime;
                            dataTable.getDataVariables().get(columnCounter).setFormatSchemaName(ddt.format);
                        } else {
                            dataRow[columnCounter] = doubleNumberFormatter.format(double_datum);
                        }

                    }
                    byte_offset += 8;
                } else if (varType.matches("^STR[1-9][0-9]*")) {
                    // String case
                    int strVarLength = variableByteLengths[columnCounter];
                    //String raw_datum = new String(Arrays.copyOfRange(dataRowBytes, byte_offset,
                    //        (byte_offset + strVarLength)), "ISO-8859-1");
                    // (old) TODO: 
                    // is it the right thing to do, to default to "ISO-8859-1"?
                    // (it may be; since there's no mechanism for specifying
                    // alternative encodings in Stata, this may be their default;
                    // it just needs to be verified. -- L.A. Jul. 2014)
                    // ACTUALLY, in STATA13, it appears that STRF *MUST*
                    // be limited to ASCII. Binary strings can be stored as 
                    // STRLs. (Oct. 6 2014)

                    //String string_datum = getNullStrippedString(raw_datum);
                    String string_datum = reader.readString(strVarLength);
                    logger.finer(i + "-th row " + columnCounter
                            + "=th column string =" + string_datum);
                    if (string_datum.equals("")) {
                        logger.finer(i + "-th row " + columnCounter
                                + "=th column string missing value=" + string_datum);
                        // TODO: 
                            /* Is this really a missing value case? 
                         * Or is it an honest empty string? 
                         * Is there such a thing as a missing value for a String in Stata?
                         * -- L.A. 4.0
                         */
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {
                        /*
                         * Some special characters, like new lines and tabs need to 
                         * be escaped - otherwise they will break our TAB file 
                         * structure! 
                         * But before we escape anything, all the back slashes 
                         * already in the string need to be escaped themselves.
                         */
                        // TODO: 
                        // replace this with the escape code from the parent class. 
                        // -- L.A. Oct. 6 2014
                        String escapedString = string_datum.replace("\\", "\\\\");
                        // escape quotes: 
                        escapedString = escapedString.replaceAll("\"", Matcher.quoteReplacement("\\\""));
                        // escape tabs and new lines:
                        escapedString = escapedString.replaceAll("\t", Matcher.quoteReplacement("\\t"));
                        escapedString = escapedString.replaceAll("\n", Matcher.quoteReplacement("\\n"));
                        escapedString = escapedString.replaceAll("\r", Matcher.quoteReplacement("\\r"));
                        // the escaped version of the string is stored in the tab file 
                        // enclosed in double-quotes; this is in order to be able 
                        // to differentiate between an empty string (tab-delimited empty string in 
                        // double quotes) and a missing value (tab-delimited empty string). 
                        // Although the question still remains - is it even possible 
                        // to store an empty string, that's not a missing value, in Stata? 
                        // - see the comment in the missing value case above. -- L.A. 4.0
                        dataRow[columnCounter] = "\"" + escapedString + "\"";
                    }
                    byte_offset += strVarLength;
                } else if (varType.equals("STRL")) {
                    throw new IOException("<Support for STRLs not yet implemented>");
                } else {
                    logger.warning("unknown variable type found: " + varType);
                    String errorMessage
                            = "unknow variable type encounted when reading data section: " + varType;
                    throw new InvalidObjectException(errorMessage);

                }
            } // for (columnCounter)

            if (byte_offset != bytes_per_row) {
                throw new IOException("Unexpected number of bytes read for data row " + i + "; " + bytes_per_row + " expected, " + byte_offset + " read.");
            }

            // Dump the row of data to the tab-delimited file:
            pwout.println(StringUtils.join(dataRow, "\t"));

        }  // for (rows)

        pwout.close();

        logger.fine("DTA117 Ingest: readData(): end.");

    }
   
    /* 
     * STRLs: 
     * (simply skipping these, for now)
    */
    
    private void readSTRLs(DataReader reader) throws IOException {
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_STRLS);
        
        // TODO: 
        // just skipping the section, for now. 
        // If there are STRLs defined, we'll have to read this section first, 
        // before reading the data section, somehow....
        reader.readPrimitiveSection(TAG_STRLS);
        
        reader.readClosingTag(TAG_STRLS);
    }
    
    private void readValueLabels(DataReader reader) throws IOException {
        //throw new IOException("Support for value labels not yet implemented!");

        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VALUE_LABELS);

        //reader.skipDefinedSections(TAG_CHARACTERISTICS_SUBSECTION);
        while (reader.checkTag(TAG_VALUE_LABELS_LBL_DEF)) {
            // TODO: checktag should probably *read* the tag, if it is indeed
            // encountered, rather then stop at the beginning of the tag. 
            reader.readOpeningTag(TAG_VALUE_LABELS_LBL_DEF);
            long label_table_length = reader.readInteger(4);
                // TODO: 
            // think of better variable names...
            if (label_table_length < 0) {
                // TODO: 
                // readInteger will eventually be replaced with readUnsignedInteger, 
                // so this would be moot point.
                throw new IOException("<negative number of bytes in readValueLabels?>");
            }

            String label_table_name = reader.readString(33);
            // TODO: 
            // do we need to worry about uniquness? or has Stata already 
            // guaranteed that there are no other category value table 
            // defined under this name?
            reader.readBytes(3); // TODO: skipBytes() instead

            long value_category_offset = 0;

            // read the value_label_table that follows. 
            // should be label_table_length. 
            // TODO: 
            // always use longs when reading unsigned 4 byte integers, 
            // to accomodate values > 2^31 - 1 ? 
            // -- not that it's likely to happen, in this context especially...
            int number_of_categories = reader.readInteger(4);
            int text_length = reader.readInteger(4);

            value_category_offset = 8;

            int[] value_label_offsets = new int[number_of_categories];
            int[] category_values = new int[number_of_categories];
            String[] category_value_labels = new String[number_of_categories];

            for (int i = 0; i < number_of_categories; i++) {
                value_label_offsets[i] = reader.readInteger(4);
                value_category_offset += 4;
            }

            for (int i = 0; i < number_of_categories; i++) {
                // TODO: 
                // can the category values be negative?
                category_values[i] = reader.readInteger(4);
                value_category_offset += 4;
            }

            int total_label_bytes = 0;

            int label_offset = 0;
            int label_end = 0;
            int label_length = 0;

            for (int i = 0; i < number_of_categories; i++) {
                label_offset = value_label_offsets[i];
                label_end = i < number_of_categories - 1 ? value_label_offsets[i + 1] : text_length;
                label_length = label_end - label_offset;

                category_value_labels[i] = reader.readString(label_length);
                total_label_bytes += label_length;
            }

            value_category_offset += total_label_bytes;

            if (total_label_bytes != text_length) {
                throw new IOException("<read mismatch in readLabels()>");
            }

            if (value_category_offset != label_table_length) {
                throw new IOException("<read mismatch in readLabels() 2>");
            }
            reader.readClosingTag(TAG_VALUE_LABELS_LBL_DEF);

            // Find the variables that link to this Category Values Table 
            // and create VariableCategory objects for the corresponding 
            // DataVariables: 
            for (int i = 0; i < dataTable.getVarQuantity(); i++) {
                if (label_table_name.equals(valueLabelsLookupTable[i])) {
                    // it is actually a legit condition - when 
                    // a variable is advertised as linked to a category values
                    // table of a certain name, but no such table exists.
                    // -- L.A.
                    for (int j = 0; j < number_of_categories; j++) {
                        VariableCategory cat = new VariableCategory();

                        int cat_value = category_values[j];
                        String cat_label = category_value_labels[j];
                        
                        cat.setValue(""+cat_value);
                        cat.setLabel(cat_label);

                        /* cross-link the variable and category to each other: */
                        cat.setDataVariable(dataTable.getDataVariables().get(i));
                        dataTable.getDataVariables().get(i).getCategories().add(cat);
                    }
                }
            }
        }

        reader.readClosingTag(TAG_VALUE_LABELS);

    }
    
    private class DecodedDateTime {
        String format;
        String decodedDateTime;
    }

    private DecodedDateTime decodeDateTimeData(String storageType, String FormatType, String rawDatum) throws IOException {

        logger.finer("(storageType, FormatType, rawDatum)=("
                + storageType + ", " + FormatType + ", " + rawDatum + ")");
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
            logger.finer("tc: result="+decodedDateTime+", format = "+format);
            
        } else if (FormatType.matches("^%t?d.*")){
            milliSeconds = Long.parseLong(rawDatum)*SECONDS_PER_YEAR + STATA_BIAS_TO_EPOCH;
            logger.finer("milliSeconds="+milliSeconds);
            
            decodedDateTime = sdf_ymd.format(new Date(milliSeconds));
            format = sdf_ymd.toPattern();
            logger.finer("td:"+decodedDateTime+", format = "+format);

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
            logger.finer("rawDatum="+rawDatum+": monthYear="+monthYear);
            
            decodedDateTime = monthYear;
            format = "yyyy-MM-dd";
            logger.finer("tm:"+decodedDateTime+", format:"+format);

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
            logger.finer("rawDatum="+rawDatum+": quaterYear="+quaterYear);

            decodedDateTime = quaterYear;
            format = "yyyy-MM-dd";
            logger.finer("tq:"+decodedDateTime+", format:"+format);

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
            logger.finer("rawDatum="+rawDatum+": halfYear="+halfYear);
            
            decodedDateTime = halfYear;
            format = "yyyy-MM-dd";
            logger.finer("th:"+decodedDateTime+", format:"+format);
            
        } else if (FormatType.matches("^%t?y.*")){
            // year type's origin is 0 AD
            decodedDateTime = rawDatum;
            format = "yyyy";
            logger.finer("th:"+decodedDateTime);
        } else {
            decodedDateTime = rawDatum;
            format=null;
        }
        DecodedDateTime retValue = new DecodedDateTime();
        retValue.decodedDateTime = decodedDateTime;
        retValue.format = format;
        return retValue;
    }
    
    private class DataReader {
        private BufferedInputStream stream; 
        private int BUFFER_SIZE = 1024;
        private byte[] byte_buffer; 
        private long byte_offset; 
        private int buffer_byte_offset;
        Boolean LSF = null; 
        
        public DataReader(BufferedInputStream stream) throws IOException {
            this(stream, 0);
        }
        
        public DataReader(BufferedInputStream stream, int buffer_size) throws IOException {
            if (buffer_size > 0) {
                this.BUFFER_SIZE = buffer_size;
            }
            this.stream = stream;
            byte_buffer = new byte[BUFFER_SIZE];
            byte_offset = 0; 
            buffer_byte_offset = 0; 
            bufferMoreBytes();
        }
        
        public BufferedInputStream getStream() {
            return stream;
        }
        
        public void setStream(BufferedInputStream stream) {
            this.stream = stream; 
        }
        
        public void setLSF(boolean lsf) {
            LSF = lsf; 
        }
        
        public Boolean isLSF() {
            return LSF; 
        }
        
        public byte[] readPrimitiveSection(String tag) throws IOException {
            readOpeningTag(tag);
            byte[] ret = readPrimitiveSectionBytes();
            readClosingTag(tag);
            return ret; 
        }
        
        public byte[] readPrimitiveSection(String tag, int length) throws IOException {
            readOpeningTag(tag);
            byte[] ret = readBytes(length);
            readClosingTag(tag);
            return ret; 
        }
        
        public String readPrimitiveStringSection(String tag) throws IOException {
            return new String(readPrimitiveSection(tag), "US-ASCII");
        }
        
        public String readPrimitiveStringSection(String tag, int length) throws IOException {
            return new String(readPrimitiveSection(tag, length), "US-ASCII");
        }
        
        /* 
         * This method reads a string section the length of which is *defined*.
         * the format of the section is as follows: 
         * <tag>Lxxxxxx...x</tag>
         * where L is a single byte specifying the length of the enclosed 
         * string; followed by L bytes.
         * L must be within 
         * 0 <= L <= limit
         * (for example, the "dataset label" is limited to 80 characters).
        */
        public String readDefinedStringSection(String tag, int limit) throws IOException {
            readOpeningTag(tag);
            short number = readByte();
            if (number < 0 || number > limit) {
                throw new IOException ("<more than limit characters in the section \"tag\">");
            }
            String ret = new String(readBytes(number), "US-ASCII");
            readClosingTag(tag);
            return ret; 
        }
        
        private byte readSignedByte() throws IOException {
            byte ret; 
            if (buffer_byte_offset > BUFFER_SIZE) {
                throw new IOException ("<buffer overflow>");
            }
            if (buffer_byte_offset < BUFFER_SIZE) {
                ret = byte_buffer[buffer_byte_offset];
                buffer_byte_offset++;
            } else {
                if (bufferMoreBytes() < 1) {
                    throw new IOException("reached the end of data stream prematurely.");
                }
                ret = byte_buffer[0];
                buffer_byte_offset = 1; 
            }
            return ret;
        }
        
        private short readByte() throws IOException {
            short ret = readSignedByte();
            
            if (ret < 0) {
                ret += 128;
            }
            return ret;
        }
        
        public int readIntegerSection(String tag, int n) throws IOException {
            readOpeningTag(tag);
            int number = readInteger(n);
            readClosingTag(tag);
            return number;
        }
        
        public byte[] readBytes(int n) throws IOException {
            if (n <= 0) {
                throw new IOException("DataReader.readBytes called to read zero or negative number of bytes.");
            }
            byte[] bytes = new byte[n];
            
            if (BUFFER_SIZE - buffer_byte_offset >= n) {
                System.arraycopy(byte_buffer, buffer_byte_offset, bytes, 0, n);
                buffer_byte_offset+=n;
            } else {
                int bytes_read = 0; 
                if (BUFFER_SIZE - buffer_byte_offset > 0) {
                    System.arraycopy(byte_buffer, buffer_byte_offset, bytes, 0, BUFFER_SIZE - buffer_byte_offset);
                    buffer_byte_offset = BUFFER_SIZE;
                    bytes_read = BUFFER_SIZE - buffer_byte_offset;
                    bufferMoreBytes(); 
                }
                
                while (n - bytes_read > BUFFER_SIZE) {
                    System.arraycopy(byte_buffer, buffer_byte_offset, bytes, bytes_read, BUFFER_SIZE);
                    bytes_read += BUFFER_SIZE;
                    bufferMoreBytes();
                }
                
                System.arraycopy(byte_buffer, 0, bytes, bytes_read, n - bytes_read);
                buffer_byte_offset = n - bytes_read; 
            }
            
            return bytes;
        }
        
        /* 
         * readString() reads NULL-terminated strings; i.e. it chops the 
         * string at the first zero encountered. 
         * we probably need an alternative, readRawString(), that reads 
         * a String as is. 
         */
        public String readString(int n) throws IOException { 
            // TODO: 
            // double-check if variable names have to be ASCII:
            // (regardless... this method is used for reading *all sorts* 
            // of strings, not just variable names - so we should *not* be
            // defaulting to ascii, yes??)
            // -- L.A. 4.0 beta 8
            
            String ret = new String(readBytes(n), "US-ASCII");
            
            // Remove the terminating and/or padding zero bytes:
            if (ret != null && ret.indexOf(0) > -1) {
                return ret.substring(0, ret.indexOf(0));
            }
            
            return ret;
        }
        
        public int readInteger(int n) throws IOException {
            if (LSF == null) {
                throw new IOException("Byte order not determined for reading numeric values.");
            }
            if (n != 2 && n != 4) {
                throw new IOException("Unsupported number of bytes in an integer: "+n);
            }
            
            byte[] raw_bytes = readBytes(n);
            
            return (int)bytesToInt(raw_bytes);
        }
        
        public long readLongInteger() throws IOException {
            if (LSF == null) {
                throw new IOException("Byte order not determined for reading numeric values.");
            }
            
            byte[] raw_bytes = readBytes(8);
            
            return bytesToLong(raw_bytes);
        }
        
        public int readSignedInteger(int n) throws IOException {
            if (LSF == null) {
                throw new IOException("Byte order not determined for reading numeric values.");
            }
            if (n != 2 && n != 4) {
                throw new IOException("Unsupported number of bytes in an integer: "+n);
            }
            
            byte[] raw_bytes = readBytes(n);
            
            return bytesToSignedInt(raw_bytes);
        }
        
        public short readShortInteger() throws IOException {
            throw new IOException("readShortInteger() not implemented yet.");
        }
        
        public double readDouble() throws IOException {
            ByteBuffer double_buffer = ByteBuffer.wrap(readBytes(8));
            if (isLittleEndian) {
                double_buffer.order(ByteOrder.LITTLE_ENDIAN);
            }
            double ret = double_buffer.getDouble();
            return ret;

        }
        
        public float readFloat() throws IOException {
            ByteBuffer float_buffer = ByteBuffer.wrap(readBytes(4));
            // TODO:
            // this implies that floats are always stored in LSF/little endian...
            // verify that this is still true in STATA 13!
            float_buffer.order(ByteOrder.LITTLE_ENDIAN);
            float ret = float_buffer.getFloat();
            return ret;
        }
        
        private long bytesToInt (byte[] raw_bytes) {
            long ret = 0; 
            
            for (int i = 0; i < raw_bytes.length; i++) {
                if (LSF) {
                    ret += raw_bytes[i] * 2^i; 
                } else {
                    ret += raw_bytes[raw_bytes.length - i - 1] * 2^i;
                }
            }
            
            return ret; 
        }
        
        private int bytesToSignedInt(byte[] raw_bytes) throws IOException {
            int n = raw_bytes.length;
            ByteBuffer byte_buffer
                    = ByteBuffer.wrap(raw_bytes);
            if (LSF) {
                byte_buffer.order(ByteOrder.LITTLE_ENDIAN);

            }
            int int_value;
            if (n == 2) {
                int_value = byte_buffer.getShort();
            } else if (n == 4) {
                int_value = byte_buffer.getInt();
            } else {
                throw new IOException("Unsupported number of bytes for signed integer: "+n);
            }
            return int_value;
        }
        
        private long bytesToLong (byte[] raw_bytes) throws IOException {
            if (raw_bytes.length != 8) {
                throw new IOException("Insufficient number of bytes to convert to Long.");
            }
            
            long ret = 0;             
            
            for (int i = 0; i < raw_bytes.length; i++) {
                if (LSF) {
                    ret += raw_bytes[i] * 2^i; 
                } else {
                    ret += raw_bytes[raw_bytes.length - i - 1] * 2^i;
                }
            }
            
            return ret; 
        }
        
        
        public void skipDefinedSections(String tag) throws IOException {
            while (checkTag(tag)) {
                // TODO: checkTag() should probably *read* the tag, if it is indeed
                // encountered, rather then stop at the beginning of the tag. 
                readOpeningTag(tag);
                long number = readInteger(4);
                if (number < 0) {
                    throw new IOException ("<negative number of bytes in skipDefinedSection(\"tag\")?>");
                }
                // TODO: implement skipBytes() instead:
                readBytes((int)number);
                readClosingTag(tag);

            }            
        }
        
        private boolean checkTag(String tag) throws IOException {
            if (tag == null || tag.equals("")) {
                throw new IOException("opening tag must be a non-empty string.");
            }

            int n = tag.length();
            
            if (BUFFER_SIZE - buffer_byte_offset >= n) {
                return ("<" + tag + ">").equals(new String(Arrays.copyOfRange(byte_buffer, buffer_byte_offset, buffer_byte_offset+n),"US-ASCII"));
            } else {
                throw new IOException("Checking section tags across byte buffers not yet implemented.");
            }
            
        }
        
        public void readOpeningTag(String tag) throws IOException {
            if (tag == null || tag.equals("")) {
                throw new IOException("opening tag must be a non-empty string.");
            }
            
            byte[] openTag = readBytes(tag.length() + 2);
            
            String openTagString = new String (openTag, "US-ASCII");
            if (openTagString == null || !openTagString.equals("<"+tag+">")) {
                throw new IOException("Could not read opening tag <"+tag+">");
            }
        }
        
        public void readClosingTag(String tag) throws IOException {
            if (tag == null || tag.equals("")) {
                throw new IOException("closing tag must be a non-empty string.");
            }
            
            byte[] closeTag = readBytes(tag.length() + 3);
            
            String closeTagString = new String (closeTag, "US-ASCII");
            if (closeTagString == null || !closeTagString.equals("</"+tag+">")) {
                throw new IOException("Could not read closing tag </"+tag+">");
            }
        }
        
        
        
        private byte[] readPrimitiveSectionBytes() throws IOException {
            byte[] cached_bytes = null;
            
            if (buffer_byte_offset > BUFFER_SIZE) {
                throw new IOException("Buffer overflow in DataReader.");
            }
            if (buffer_byte_offset == BUFFER_SIZE) {
                // buffer empty; 
                bufferMoreBytes();
            }
            
            int cached_offset = buffer_byte_offset; 
            
            while (byte_buffer[buffer_byte_offset] != '<') {
                buffer_byte_offset++; 
                
                if (buffer_byte_offset == BUFFER_SIZE) {
                    cached_bytes = mergeCachedBytes(cached_bytes, cached_offset);
                    bufferMoreBytes();
                    cached_offset = 0; 
                }
            }
            
            return mergeCachedBytes(cached_bytes, cached_offset);
            
        }
        
        private byte[] mergeCachedBytes(byte[] cached_bytes, int cached_offset) throws IOException {
            byte[] ret_bytes;
            if (cached_bytes == null) {
                if (buffer_byte_offset - cached_offset < 1) {
                    throw new IOException("<read error in save local buffer>TODO: better exception message");
                }
                /*
                if (buffer_byte_offset - cached_offset == 0) {
                    return null; 
                }
                 */
                ret_bytes = new byte[buffer_byte_offset - cached_offset];
                System.arraycopy(byte_buffer, cached_offset, ret_bytes, 0, buffer_byte_offset - cached_offset);
            } else {
                if (cached_offset != 0) {
                    throw new IOException("<read error in save local buffer>TODO: better exception message");
                }
                ret_bytes = new byte[cached_bytes.length + buffer_byte_offset];
                System.arraycopy(cached_bytes, 0, ret_bytes, 0, cached_bytes.length);
                if (buffer_byte_offset > 0) {
                    System.arraycopy(byte_buffer, 0, ret_bytes, cached_bytes.length, buffer_byte_offset);
                }
            }
            return ret_bytes;
        }
        
        
        
        private int bufferMoreBytes() throws IOException {
            int actual_bytes_read = stream.read(byte_buffer, 0, BUFFER_SIZE);
            if (actual_bytes_read != BUFFER_SIZE) {
                BUFFER_SIZE = actual_bytes_read; 
            }
            byte_offset +=  buffer_byte_offset;  
            buffer_byte_offset = 0; 
            return actual_bytes_read; 
        }
    }

    private class DTADataMap {
        private long dta_offset_stata_data = 0; 
        private long dta_offset_map = 0; 
        private long dta_offset_variable_types = 0; 
        private long dta_offset_varnames = 0; 
        private long dta_offset_sortlist = 0; 
        private long dta_offset_formats = 0; 
        private long dta_offset_value_label_names = 0; 
        private long dta_offset_variable_labels = 0; 
        private long dta_offset_characteristics = 0; 
        private long dta_offset_data = 0; 
        private long dta_offset_strls = 0; 
        private long dta_offset_value_labels = 0; 
        private long dta_offset_data_close = 0; 
        private long dta_offset_eof = 0;
        
        // getters:
        
        public long getOffset_head() {
            return dta_offset_stata_data;
        }
        public long getOffset_map() {
            return dta_offset_map;
        }
        public long getOffset_types() {
            return dta_offset_variable_types;
        }
        public long getOffset_varnames() {
            return dta_offset_varnames;
        }
        public long getOffset_srtlist() {
            return dta_offset_sortlist;
        }
        public long getOffset_fmts() {
            return dta_offset_formats;
        }
        public long getOffset_vlblnames() {
            return dta_offset_value_label_names;
        }
        public long getOffset_varlabs() {
            return dta_offset_variable_labels;
        }
        public long getOffset_characteristics() {
            return dta_offset_characteristics;
        }
        public long getOffset_data() {
            return dta_offset_data;
        }
        public long getOffset_strls() {
            return dta_offset_strls;
        }
        public long getOffset_vallabs() {
            return dta_offset_value_labels;
        }
        public long getOffset_data_close() {
            return dta_offset_data_close;
        }
        public long getOffset_eof() {
            return dta_offset_eof;
        }
        
        // setters: 
        
        public void setOffset_head(long dta_offset_stata_data) {
            this.dta_offset_stata_data = dta_offset_stata_data;
        }
        public void setOffset_map(long dta_offset_map) {
            this.dta_offset_map = dta_offset_map;
        }
        public void setOffset_types(long dta_offset_variable_types) {
            this.dta_offset_variable_types = dta_offset_variable_types;
        }
        public void setOffset_varnames(long dta_offset_varnames) {
            this.dta_offset_varnames = dta_offset_varnames;
        }
        public void setOffset_srtlist(long dta_offset_sortlist) {
            this.dta_offset_sortlist = dta_offset_sortlist;
        }
        public void setOffset_fmts(long dta_offset_formats) {
            this.dta_offset_formats = dta_offset_formats;
        }
        public void setOffset_vlblnames(long dta_offset_value_label_names) {
            this.dta_offset_value_label_names = dta_offset_value_label_names;
        }
        public void setOffset_varlabs(long dta_offset_variable_labels) {
            this.dta_offset_variable_labels = dta_offset_variable_labels;
        }
        public void setOffset_characteristics(long dta_offset_characteristics) {
            this.dta_offset_characteristics = dta_offset_characteristics;
        }
        public void setOffset_data(long dta_offset_data) {
            this.dta_offset_data = dta_offset_data;
        }
        public void setOffset_strls(long dta_offset_strls) {
            this.dta_offset_strls = dta_offset_strls;
        }
        public void setOffset_vallabs(long dta_offset_value_labels) {
            this.dta_offset_value_labels = dta_offset_value_labels;
        }
        public void setOffset_data_close(long dta_offset_data_close) {
            this.dta_offset_data_close = dta_offset_data_close;
        }
        public void setOffset_eof(long dta_offset_eof) {
            this.dta_offset_eof = dta_offset_eof;
        }
    }
}

