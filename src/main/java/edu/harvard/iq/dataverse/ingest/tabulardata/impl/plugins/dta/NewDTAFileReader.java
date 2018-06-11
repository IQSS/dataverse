package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import java.io.*;
import java.nio.*;
import java.util.logging.*;

import java.util.*;
import java.text.*;

import org.apache.commons.lang.*;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;

/**
 * ingest plugin for Stata 13-15 (117-119) DTA file format. A copy and paste from
 *
 * - v.13/("dta 117"): https://www.stata.com/help.cgi?dta_117
 *
 * - v.14/("dta 118"): https://www.stata.com/help.cgi?dta
 *
 * - v.14/("dta 119"): https://www.stata.com/help.cgi?dta_119
 *
 */
public class NewDTAFileReader extends TabularDataFileReader {
    //@Inject
    //VariableServiceBean varService;
    // static fields, STATA-specific constants, etc. 

    // SECTION TAGS:
    // 
    // The new STATA format features XML-like section tags - 
    // <stata_dta><header>...</header>...</stata_dta>
    
    // MAIN, TOP-LEVEL FILE SECTION:
    private static final String TAG_DTA = "stata_dta";

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
    private static final String STRL_GSO_HEAD = "GSO";

    // VALUE LABELS SECTION:
    private static final String TAG_VALUE_LABELS = "value_labels";
    private static final String TAG_VALUE_LABELS_LBL_DEF = "lbl";

    private static Map<Integer, String> STATA_RELEASE_NUMBER =
            new HashMap<Integer, String>();

    private static Map<Integer, Map<String, Integer>> CONSTANT_TABLE =
            new LinkedHashMap<Integer, Map<String, Integer>>();

    private static Map<String, Integer> releaseconstant
            = new LinkedHashMap<String, Integer>();

    private static Map<String, Integer> byteLengthTable =
            new HashMap<String, Integer>();

    private static Map<Integer, String> variableTypeTable =
            new LinkedHashMap<Integer, String>();

    private static final int[] LENGTH_HEADER = {60, 109};
    private static final int[] LENGTH_LABEL = {32, 81};
    private static final int[] LENGTH_NAME = {9, 33};
    private static final int[] LENGTH_FORMAT_FIELD = {7, 12, 49};
    private static final int[] LENGTH_EXPANSION_FIELD = {0, 2, 4};
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
        releaseconstant.put("HEADER",     LENGTH_HEADER[1]);
        releaseconstant.put("LABEL",     LENGTH_LABEL[1]);
        releaseconstant.put("NAME",      LENGTH_NAME[1]);
        releaseconstant.put("FORMAT",    LENGTH_FORMAT_FIELD[1]);
        releaseconstant.put("EXPANSION", LENGTH_EXPANSION_FIELD[2]);
        releaseconstant.put("DBL_MV_PWR", DBL_MV_PWR[1]);
        
        // 1, 2 and 4-byte integers: 
        byteLengthTable.put("Byte",1);
        byteLengthTable.put("Integer",2);
        byteLengthTable.put("Long",4);
        // 4 and 8-byte floats: 
        byteLengthTable.put("Float",4);
        byteLengthTable.put("Double",8);
        // STRLs are defined in their own section, outside of the 
        // main data. In the <data> section they are referenced 
        // by 2 x 4 byte values, "(v,o)", 8 bytes total.
        byteLengthTable.put("STRL",8);

        variableTypeTable.put(65530,"Byte");
        variableTypeTable.put(65529,"Integer");
        variableTypeTable.put(65528,"Long");
        variableTypeTable.put(65527,"Float");
        variableTypeTable.put(65526,"Double");
        
        //variableTypeTable117.put(32768,"STRL");
    }
    
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
            new HashSet<>(FLOAT_MISSING_VALUES);

    private static final List<Double> DOUBLE_MISSING_VALUE_LIST = Arrays.asList(
            0x1.000p1023, 0x1.001p1023, 0x1.002p1023, 0x1.003p1023, 0x1.004p1023,
            0x1.005p1023, 0x1.006p1023, 0x1.007p1023, 0x1.008p1023, 0x1.009p1023,
            0x1.00ap1023, 0x1.00bp1023, 0x1.00cp1023, 0x1.00dp1023, 0x1.00ep1023,
            0x1.00fp1023, 0x1.010p1023, 0x1.011p1023, 0x1.012p1023, 0x1.013p1023,
            0x1.014p1023, 0x1.015p1023, 0x1.016p1023, 0x1.017p1023, 0x1.018p1023,
            0x1.019p1023, 0x1.01ap1023);

    private Set<Double> DOUBLE_MISSING_VALUE_SET =
            new HashSet<>(DOUBLE_MISSING_VALUE_LIST);

    private static SimpleDateFormat sdf_ymdhmsS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); // sdf

    private static SimpleDateFormat sdf_ymd = new SimpleDateFormat("yyyy-MM-dd"); // sdf2

    private static SimpleDateFormat sdf_hms = new SimpleDateFormat("HH:mm:ss"); // stf

    private static SimpleDateFormat sdf_yw = new SimpleDateFormat("yyyy-'W'ww");

    // stata's calendar
    private static Calendar GCO_STATA = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

    private static String[] DATE_TIME_FORMAT = {
        "%tc", "%td", "%tw", "%tq", "%tm", "%th", "%ty",
        "%d", "%w", "%q", "%m", "h", "%tb"
    };
    // New "business calendar format" has been added in Stata 12. -- L.A. 
    private static String[] DATE_TIME_CATEGORY = {
        "time", "date", "date", "date", "date", "date", "date",
        "date", "date", "date", "date", "date", "date"
    };
    private static Map<String, String> DATE_TIME_FORMAT_TABLE = new LinkedHashMap<String, String>();

    private static long MILLISECCONDS_PER_DAY = 24 * 60 * 60 * 1000L;

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

    private Map<String, Integer> constantTable;
    
    private Map<String, String> cachedGSOs;

    private NumberFormat twoDigitFormatter = new DecimalFormat("00");

    private NumberFormat doubleNumberFormatter = new DecimalFormat();

    TabularDataIngest ingesteddata = new TabularDataIngest();

    private int DTAVersion;

    private int headerLength;

    private int dataLabelLength;

    private boolean hasSTRLs = false;

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

    private String[] MIME_TYPE = {
        "application/x-stata",
        "application/x-stata-" + (DTAVersion-104)
    };
         
    // Constructor -----------------------------------------------------------//
    public NewDTAFileReader(TabularDataFileReaderSpi originator, int DTAVersion) {
        super(originator);
        
        this.DTAVersion = DTAVersion;
        STATA_RELEASE_NUMBER.put(DTAVersion, "v." + (DTAVersion-104));

        CONSTANT_TABLE.put(DTAVersion, releaseconstant);
    }


    /*
     * This method configures Stata's release-specific parameters:
     */
    // TODO: this method needs to be actually called! 
    private void init() throws IOException {
        //
        logger.fine("release number=" + DTAVersion);

        BYTE_MISSING_VALUE -= MISSING_VALUE_BIAS;
        INT_MISSIG_VALUE -= MISSING_VALUE_BIAS;
        LONG_MISSING_VALUE -= MISSING_VALUE_BIAS;

        constantTable = CONSTANT_TABLE.get(DTAVersion);

        headerLength = constantTable.get("HEADER") - DTA_MAGIC_NUMBER_LENGTH;

        dataLabelLength = headerLength - (NVAR_FIELD_LENGTH
                + NOBS_FIELD_LENGTH + TIME_STAMP_LENGTH);
        logger.fine("data_label_length=" + dataLabelLength);

        logger.fine("constant table to be used:\n" + constantTable);

        doubleNumberFormatter.setGroupingUsed(false);
        doubleNumberFormatter.setMaximumFractionDigits(340); // TODO: WTF???

        /* 
         * No longer necessary to use variable service bean during ingest!
         *
        
        Context ctx = null;
        
        try {
            ctx = new InitialContext();
            varService = (VariableServiceBean) ctx.lookup("java:global/dataverse-4.0/VariableServiceBean");
        } catch (NamingException nex) {
            try {
                ctx = new InitialContext();
                varService = (VariableServiceBean) ctx.lookup("java:global/dataverse/VariableServiceBean");
            } catch (NamingException nex2) {
                logger.severe("Could not look up initial context, or the variable service in JNDI!");
                throw new IOException("Could not look up initial context, or the variable service in JNDI!");
            }
        }
         */
    }

    @Override
    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {
        logger.fine("NewDTAFileReader: read() start");

        // shit ton of diagnostics (still) needed here!!  -- L.A.
        if (dataFile != null) {
            throw new IOException("this plugin does not support external raw data files");
        }

        DataReader dataReader;

        try {
            init();

            // create a new instance of DataReader:
            dataReader = new DataReader(stream);
            // and read the opening tag:
            dataReader.readOpeningTag(TAG_DTA);

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

            // display formats: 
            readDisplayFormats(dataReader);

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
            dataReader.readClosingTag(TAG_DTA);

            ingesteddata.setDataTable(dataTable);
        } catch (IllegalArgumentException iaex) {
            throw iaex;
            //throw new IOException(iaex.getMessage());
        }

        logger.fine("NewDTAFileReader: read() end.");
        return ingesteddata;
    }

    private void readHeader(DataReader dataReader) throws IOException {
        logger.fine("readHeader(): start");

        if (dataReader == null) {
            throw new IllegalArgumentException("stream == null!");
        }

        logger.fine("reading the version header.");

        dataReader.readOpeningTag(TAG_HEADER);
        String dtaVersionTag = dataReader.readPrimitiveStringSection(TAG_HEADER_FILEFORMATID, 3);

        if (!("117".equals(dtaVersionTag)||"118".equals(dtaVersionTag)||"119".equals(dtaVersionTag))) {
            throw new IOException("Unexpected version tag found: " + dtaVersionTag + "; expected value: 117-119.");
        }

        String byteOrderTag = dataReader.readPrimitiveStringSection(TAG_HEADER_BYTEORDER);

        logger.fine("byte order: "+byteOrderTag);

        if ("LSF".equals(byteOrderTag)) {
            dataReader.setLSF(true);
        } else if ("MSF".equals(byteOrderTag)) {
            dataReader.setLSF(false);
        }

        int varNumber = dataReader.readIntegerSection(TAG_HEADER_VARNUMBER, DTAVersion == 119? 4: 2);
        logger.fine("number of variables: " + varNumber);

        /**
         * 5.1.4 N, # of observations
         *
         *
         * N, the number of observations stored in the dataset, is recorded as
         *
         *
         * <N>bbbbbbbb</N>
         * Where N =  is a 4 or 8-byte unsigned integer field recorded
         * according to byteorder.
         */
        int obsNumber = dataReader.readIntegerSection(TAG_HEADER_OBSNUMBER, DTAVersion == 117? 4: 8);
        logger.fine("number of observations: " + obsNumber);

        dataTable.setVarQuantity(new Long(varNumber));
        dataTable.setCaseQuantity(new Long(obsNumber));

        dataTable.setOriginalFileFormat(MIME_TYPE[0]);
        
        dataTable.setOriginalFormatVersion("STATA " + (DTAVersion-104));
        dataTable.setUnf("UNF:pending");

        // The word "dataset" below is used in its STATA parlance meaning, 
        // i.e., this is a label that describes the datafile.
        String datasetLabel;
        if (DTAVersion==117){
            datasetLabel = dataReader.readDefinedStringSection(TAG_HEADER_FILELABEL, 80);
        }else{
            datasetLabel = dataReader.readLabelSection(TAG_HEADER_FILELABEL, 320);
        }
        logger.fine("dataset label: " + datasetLabel);

        // TODO: 
        // do we want to do anything with this label? Add it to the 
        // filemetadata, similarly to what we do with those auto-generated
        // FITS descriptive labels maybe? 
        // (similarly, what to do with the date stamp, below?)
        // -- L.A. 4.0 beta 8
        String datasetTimeStamp = dataReader.readDefinedStringSection(TAG_HEADER_TIMESTAMP, 17);
        logger.fine("dataset time stamp: " + datasetTimeStamp);

        if (datasetTimeStamp == null
                || (datasetTimeStamp.length() > 0 && datasetTimeStamp.length() < 17)) {
            throw new IOException("unexpected/invalid length of the time stamp in the NewDTA header.");
        } else {
            // TODO: validate the time stamp found against dd Mon yyyy hh:mm; 
            // ...but first decide if we actually want/need to use it for any 
            // practical purposes...
        }

        dataReader.readClosingTag("header");
        logger.fine("readHeader(): end");
    }

    /* 
        TODO: add a comment. --L.A. DVN 4.0 beta 8 
     */
    private void readMap(DataReader reader) throws IOException {
        logger.fine("Map section; at offset " + reader.getByteOffset());
        reader.readOpeningTag(TAG_MAP);

        dtaMap = new DTADataMap();

        long dta_offset_stata_data = reader.readLongInteger();
        logger.fine("dta_offset_stata_data: " + dta_offset_stata_data);
        dtaMap.setOffset_head(dta_offset_stata_data);
        long dta_offset_map = reader.readLongInteger();
        logger.fine("dta_offset_map: " + dta_offset_map);
        dtaMap.setOffset_map(dta_offset_map);
        long dta_offset_variable_types = reader.readLongInteger();
        logger.fine("dta_offset_variable_types: " + dta_offset_variable_types);
        dtaMap.setOffset_types(dta_offset_variable_types);
        long dta_offset_varnames = reader.readLongInteger();
        logger.fine("dta_offset_varnames: " + dta_offset_varnames);
        dtaMap.setOffset_varnames(dta_offset_varnames);
        long dta_offset_sortlist = reader.readLongInteger();
        logger.fine("dta_offset_sortlist: " + dta_offset_sortlist);
        dtaMap.setOffset_srtlist(dta_offset_sortlist);
        long dta_offset_formats = reader.readLongInteger();
        logger.fine("dta_offset_formats: " + dta_offset_formats);
        dtaMap.setOffset_fmts(dta_offset_formats);
        long dta_offset_value_label_names = reader.readLongInteger();
        logger.fine("dta_offset_value_label_names: " + dta_offset_value_label_names);
        dtaMap.setOffset_vlblnames(dta_offset_value_label_names);
        long dta_offset_variable_labels = reader.readLongInteger();
        logger.fine("dta_offset_variable_labels: " + dta_offset_variable_labels);
        dtaMap.setOffset_varlabs(dta_offset_variable_labels);
        long dta_offset_characteristics = reader.readLongInteger();
        logger.fine("dta_offset_characteristics: " + dta_offset_characteristics);
        dtaMap.setOffset_characteristics(dta_offset_characteristics);
        long dta_offset_data = reader.readLongInteger();
        logger.fine("dta_offset_data: " + dta_offset_data);
        dtaMap.setOffset_data(dta_offset_data);
        long dta_offset_strls = reader.readLongInteger();
        logger.fine("dta_offset_strls: " + dta_offset_strls);
        dtaMap.setOffset_strls(dta_offset_strls);
        long dta_offset_value_labels = reader.readLongInteger();
        logger.fine("dta_offset_value_labels: " + dta_offset_value_labels);
        dtaMap.setOffset_vallabs(dta_offset_value_labels);
        long dta_offset_data_close = reader.readLongInteger();
        logger.fine("dta_offset_data_close: " + dta_offset_data_close);
        dtaMap.setOffset_data_close(dta_offset_data_close);
        long dta_offset_eof = reader.readLongInteger();
        logger.fine("dta_offset_eof: " + dta_offset_eof);
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
        logger.fine("Type section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_types());
        reader.readOpeningTag(TAG_VARIABLE_TYPES);

        List<DataVariable> variableList = new ArrayList<>();
        // setup variableTypeList
        variableTypes = new String[dataTable.getVarQuantity().intValue()];

        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            int type = reader.readInteger(2);
            logger.fine("variable " + i + ": type=" + type);
            DataVariable dv = new DataVariable();

            dv.setInvalidRanges(new ArrayList<>());
            dv.setSummaryStatistics(new ArrayList<>());
            dv.setCategories(new ArrayList<>());

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
    private String configureVariableType(DataVariable dv, int type) throws IOException {
        String typeLabel = null;

        if (variableTypeTable.containsKey(type)) {
            typeLabel = variableTypeTable.get(type);

            dv.setTypeNumeric();
            if (typeLabel.equals("Byte") || typeLabel.equals("Integer") || typeLabel.equals("Long")) {
                // these are treated as discrete:
                dv.setIntervalDiscrete();

            } else if (typeLabel.equals("Float") || typeLabel.equals("Double")) {
                // these are treated as contiuous:
                dv.setIntervalContinuous();

            } else {
                throw new IOException("Unrecognized type label: " + typeLabel + " for Stata type value (short) " + type + ".");
            }

        } else {
            // String:
            //
            // 32768 - flexible length STRL;
            // 1 ... 2045 - fixed-length STRF;

            if (type == 32768) {
                typeLabel = "STRL";
                hasSTRLs = true;

            } else if (type > 0 && type < 2046) {
                typeLabel = "STR" + type;
            } else {
                throw new IOException("unknown variable type value encountered: " + type);
            }

            dv.setTypeCharacter();
            dv.setIntervalDiscrete();
        }

        return typeLabel;

    }

    /* 
     * Variable Names are stored as number_of_variables * 33 byte long
     * (zero-padded and zero-terminated) character vectors. 
     */
    private void readVariableNames(DataReader reader) throws IOException {
        logger.fine("Variable names section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_varnames());
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VARIABLE_NAMES);

        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            String variableName = reader.readString(DTAVersion == 117? 33: 129);
            logger.fine("variable " + i + ": name=" + variableName);
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
        logger.fine("Sort Order section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_srtlist());
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_SORT_ORDER);

        // TODO: initialize DataVariable objects here
        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            int order = reader.readInteger(DTAVersion == 119? 4: 2);
            logger.fine("variable " + i + ": sort order=" + order);
            // TODO: 
            // Double-check that we don't really need this sort order 
            // for any practical purposes. 
            // -- L.A. 4.0 beta 8
        }

        // Important! 
        // The SORT ORDER section (5.5 in the doc) always contains
        // number_of_variables + 1 short (2-byte) integers!
        int terminatingShort = reader.readInteger(DTAVersion == 119? 4: 2);
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
        logger.fine("Formats section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_fmts());
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_DISPLAY_FORMATS);
        dateVariableFormats = new String[dataTable.getVarQuantity().intValue()];

        for (int i = 0; i < dataTable.getVarQuantity(); i++) { 
            String variableFormat = reader.readString(DTAVersion == 117? 49: 57);
            logger.fine("variable " + i + ": displayFormat=" + variableFormat);
            // TODO: 
            // Decide what we are doing with these. 
            // (saving them, for archival purposes?)

            // this is from the old plugin: 
            // TODO: review!
            String variableFormatKey = null;
            logger.info(variableFormat);
            if (variableFormat.startsWith("%t")) {
                variableFormatKey = variableFormat.substring(0, 3);
            } else {
                //FIXME java.lang.StringIndexOutOfBoundsException here --pdurbin
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
                dateVariableFormats[i] = variableFormat;
                dataTable.getDataVariables().get(i).setFormatCategory(DATE_TIME_FORMAT_TABLE.get(variableFormatKey));
                logger.fine(i + "th var: category="
                        + DATE_TIME_FORMAT_TABLE.get(variableFormatKey));
                dataTable.getDataVariables().get(i).setTypeCharacter();
                dataTable.getDataVariables().get(i).setIntervalDiscrete();
            }
        }

        reader.readClosingTag(TAG_DISPLAY_FORMATS);
    }

    /*
     * Another fixed-field section
     */
    private void readValueLabelFormatNames(DataReader reader) throws IOException {
        logger.fine("Category valuable section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_vlblnames());
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VALUE_LABEL_FORMAT_NAMES);

        valueLabelsLookupTable = new String[dataTable.getVarQuantity().intValue()];

        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            // TODO: 
            // Define all the byte lengths as constants!
            String valueLabelFormat = reader.readString(DTAVersion == 117? 33: 129);
            logger.fine("variable " + i + ": value label format=" + valueLabelFormat);
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
        logger.fine("Variable labels section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_varlabs());
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VARIABLE_LABELS);

        for (int i = 0; i < dataTable.getVarQuantity(); i++) {
            String variableLabel = reader.readString(DTAVersion == 117? 81: 321);
            logger.fine("variable " + i + ": label=" + variableLabel);
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
        logger.fine("Characteristics section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_characteristics());
        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_CHARACTERISTICS);

        reader.skipDefinedSections(TAG_CHARACTERISTICS_SUBSECTION);

        reader.readClosingTag(TAG_CHARACTERISTICS);

    }

    /* 
     * TODO: add comments.
     */
    private void readData(DataReader reader) throws IOException {
        logger.fine("Data section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_data());
        logger.fine("readData(): start");
        reader.readOpeningTag(TAG_DATA);
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

        logger.fine("Beginning to read data stream.");

        for (int i = 0; i < nobs; i++) {
            //byte[] dataRowBytes = new byte[bytes_per_row];
            Object[] dataRow = new Object[nvar];

            //int nbytes = stream.read(dataRowBytes, 0, bytes_per_row);
            //dataRowBytes = reader.readBytes(bytes_per_row);
            // TODO: 
            // maybe intercept any potential exceptions here, and add more 
            // diagnostic info, before re-throwing...
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

                    logger.fine(i + "-th row " + columnCounter
                            + "=th column byte =" + byte_datum);
                    if (byte_datum >= BYTE_MISSING_VALUE) {
                        logger.fine(i + "-th row " + columnCounter
                                + "=th column byte MV=" + byte_datum);
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {
                        dataRow[columnCounter] = byte_datum;
                        logger.fine(i + "-th row " + columnCounter
                                + "-th column byte value=" + byte_datum);
                    }

                    byte_offset++;
                } else if (varType.equals("Integer")) {
                    short short_datum = (short) reader.readShortSignedInteger();

                    logger.fine(i + "-th row " + columnCounter
                            + "=th column stata int =" + short_datum);

                    if (short_datum >= INT_MISSIG_VALUE) {
                        logger.fine(i + "-th row " + columnCounter
                                + "=th column stata long missing value=" + short_datum);
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {

                        if (isDateTimeDatum) {

                            DecodedDateTime ddt = decodeDateTimeData("short", variableFormat, Short.toString(short_datum));
                            logger.fine(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                            dataRow[columnCounter] = ddt.decodedDateTime;
                            //dateFormat[columnCounter][i] = ddt.format;
                            dataTable.getDataVariables().get(columnCounter).setFormat(ddt.format);

                        } else {
                            dataRow[columnCounter] = short_datum;
                            logger.fine(i + "-th row " + columnCounter
                                    + "-th column \"integer\" value=" + short_datum);
                        }
                    }
                    byte_offset += 2;
                } else if (varType.equals("Long")) {
                    // stata-Long (= java's int: 4 byte), signed.

                    int int_datum = reader.readSignedInteger();

                    if (int_datum >= LONG_MISSING_VALUE) {
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;
                    } else {
                        if (isDateTimeDatum) {
                            DecodedDateTime ddt = decodeDateTimeData("int", variableFormat, Integer.toString(int_datum));
                            logger.fine(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                            dataRow[columnCounter] = ddt.decodedDateTime;
                            dataTable.getDataVariables().get(columnCounter).setFormat(ddt.format);

                        } else {
                            dataRow[columnCounter] = int_datum;
                            logger.fine(i + "-th row " + columnCounter
                                    + "-th column \"long\" value=" + int_datum);
                        }

                    }
                    byte_offset += 4;
                } else if (varType.equals("Float")) {
                    // STATA float 
                    // same as Java float - 4-byte

                    float float_datum = reader.readFloat();

                    logger.fine(i + "-th row " + columnCounter
                            + "=th column float =" + float_datum);
                    if (FLOAT_MISSING_VALUE_SET.contains(float_datum)) {
                        logger.fine(i + "-th row " + columnCounter
                                + "=th column float missing value=" + float_datum);
                        dataRow[columnCounter] = MissingValueForTabDelimitedFile;

                    } else {

                        if (isDateTimeDatum) {
                            DecodedDateTime ddt = decodeDateTimeData("float", variableFormat, doubleNumberFormatter.format(float_datum));
                            logger.fine(i + "-th row , decodedDateTime " + ddt.decodedDateTime + ", format=" + ddt.format);
                            dataRow[columnCounter] = ddt.decodedDateTime;
                            dataTable.getDataVariables().get(columnCounter).setFormat(ddt.format);
                        } else {
                            dataRow[columnCounter] = float_datum;
                            logger.fine(i + "-th row " + columnCounter
                                    + "=th column float value:" + float_datum);
                            // This may be temporary - but for now (as in, while I'm testing 
                            // 4.0 ingest against 3.* ingest, I need to be able to tell if a 
                            // floating point value was a single, or double float in the 
                            // original STATA file: -- L.A. Jul. 2014
                            dataTable.getDataVariables().get(columnCounter).setFormat("float");
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
                            dataTable.getDataVariables().get(columnCounter).setFormat(ddt.format);
                        } else {
                            logger.fine(i + "-th row " + columnCounter
                                    + "=th column double value:" + double_datum); //doubleNumberFormatter.format(double_datum));

                            dataRow[columnCounter] = double_datum; //doubleNumberFormatter.format(double_datum);
                        }

                    }
                    byte_offset += 8;
                } else if (varType.matches("^STR[1-9][0-9]*")) {
                    // String case
                    int strVarLength = variableByteLengths[columnCounter];
                    logger.fine(i + "-th row " + columnCounter
                            + "=th column is a string (" + strVarLength + " bytes)");
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
                    if (string_datum.length() < 64) {
                        logger.fine(i + "-th row " + columnCounter
                                + "=th column string =" + string_datum);
                    } else {
                        logger.fine(i + "-th row " + columnCounter
                                + "=th column string =" + string_datum.substring(0, 64) + "... (truncated)");
                    }
                    if (string_datum.equals("")) {

                        logger.fine(i + "-th row " + columnCounter
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
                         */

                        dataRow[columnCounter] = escapeCharacterString(string_datum);
                    }
                    byte_offset += strVarLength;
                } else if (varType.equals("STRL")) {
                    //throw new IOException("<Support for STRLs not yet implemented>");
                    logger.fine("STRL encountered.");

                    if (cachedGSOs == null) {
                        cachedGSOs = new LinkedHashMap<>();
                    }

                    // Reading the (v,o) pair: 
                    long v;
                    long o;
                    
                    if(DTAVersion == 117){
                        v = reader.readInteger();
                        byte_offset += 4;
                        o = reader.readInteger();
                        byte_offset += 4;
                    } else {
                        v = reader.readInteger(2);
                        byte_offset += 2;
                        o = reader.readInteger(6);
                        byte_offset += 6;
                    }
                    // create v,o pair; save, for now:
                    String voPair = v + "," + o;
                    dataRow[columnCounter] = voPair;

                    // TODO: 
                    // Validate v and o? 
                    // Making sure v <= varNum and o < numbObs; 
                    // or, if o == numObs, v <= columnCounter; 
                    // -- per the Stata 13 spec...
                    if (!(v == columnCounter + 1 && o == i + 1)) {
                        if (!cachedGSOs.containsKey(voPair)) {
                            cachedGSOs.put(voPair, "");
                            // this means we need to cache this GSO, when 
                            // we read the STRLS section later on. 
                        }
                    }

                } else {
                    logger.warning("unknown variable type found: " + varType);
                    String errorMessage
                            = "unknown variable type encounted when reading data section: " + varType;
                    //throw new InvalidObjectException(errorMessage);
                    throw new IOException(errorMessage);

                }
            } // for (columnCounter)

            if (byte_offset != bytes_per_row) {
                throw new IOException("Unexpected number of bytes read for data row " + i + "; " + bytes_per_row + " expected, " + byte_offset + " read.");
            }

            // Dump the row of data to the tab-delimited file:
            pwout.println(StringUtils.join(dataRow, "\t"));

            logger.fine("finished reading " + i + "-th row");

        }  // for (rows)

        pwout.close();

        reader.readClosingTag(TAG_DATA);
        logger.fine("NewDTA Ingest: readData(): end.");

    }

    /* 
     * STRLs: 
     * (simply skipping these, for now)
     */
    private void readSTRLs(DataReader reader) throws IOException {
        logger.fine("STRLs section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_strls());
        // TODO: 
        // check that we are at the right byte offset!
        //reader.readOpeningTag(TAG_STRLS);

        if (hasSTRLs) {
            reader.readOpeningTag(TAG_STRLS);

            File intermediateTabFile = ingesteddata.getTabDelimitedFile();
            FileInputStream fileInTab = new FileInputStream(intermediateTabFile);

            Scanner scanner = new Scanner(fileInTab);
            scanner.useDelimiter("\\n");

            File finalTabFile = File.createTempFile("finalTabfile.", ".tab");
            FileOutputStream fileOutTab = new FileOutputStream(finalTabFile);
            PrintWriter pwout = new PrintWriter(new OutputStreamWriter(fileOutTab, "utf8"), true);

            logger.fine("Setting the tab-delimited file to " + finalTabFile.getName());
            ingesteddata.setTabDelimitedFile(finalTabFile);

            int nvar = dataTable.getVarQuantity().intValue();
            int nobs = dataTable.getCaseQuantity().intValue();

            String[] line;

            for (int obsindex = 0; obsindex < nobs; obsindex++) {
                if (scanner.hasNext()) {
                    line = (scanner.next()).split("\t", -1);

                    for (int varindex = 0; varindex < nvar; varindex++) {
                        if ("STRL".equals(variableTypes[varindex])) {
                            // this is a STRL; needs to be re-processed:

                            String voPair = line[varindex];
                            long v;
                            long o;
                            if (voPair == null) {
                                throw new IOException("Failed to read an intermediate v,o Pair for variable "
                                        + varindex + ", observation " + obsindex);
                            }

                            if ("0,0".equals(voPair)) {
                                // This is a code for an empty string - "";
                                // doesn't need to be defined or looked up.

                                line[varindex] = "\"\"";
                            } else {
                                String[] voTokens = voPair.split(",", 2);

                                try {
                                    v = new Long(voTokens[0]);
                                    o = new Long(voTokens[1]);
                                } catch (NumberFormatException nfex) {
                                    throw new IOException("Illegal v,o value: " + voPair + " for variable "
                                            + varindex + ", observation " + obsindex);
                                }

                                if (v == varindex + 1 && o == obsindex + 1) {
                                    // This v,o must be defined in the STRLs section:
                                    line[varindex] = readGSO(reader, v, o);
                                    if (line[varindex] == null) {
                                        throw new IOException("Failed to read GSO value for " + voPair);
                                    }

                                } else {
                                    // This one must have been cached already:
                                    if (cachedGSOs.get(voPair) != null
                                            && !cachedGSOs.get(voPair).equals("")) {
                                        line[varindex] = cachedGSOs.get(voPair);
                                    } else {
                                        throw new IOException("GSO string unavailable for v,o value " + voPair);
                                    }
                                }
                            }
                        }
                    }
                    // Dump the row of data to the tab-delimited file:
                    pwout.println(StringUtils.join(line, "\t"));
                }
            }

            scanner.close();
            pwout.close();

            reader.readClosingTag(TAG_STRLS);
        } else {
            // If this data file doesn't use STRLs, we can just skip 
            // this section, and assume that we are done with the 
            // tabular data file.
            reader.readPrimitiveSection(TAG_STRLS);
        }

        //reader.readClosingTag(TAG_STRLS);
    }

    private String readGSO(DataReader reader, long v, long o) throws IOException {
        if (!reader.checkTag(STRL_GSO_HEAD)) {
            return null;
        }

        // Skipping the GSO header - fixed string "GSO":
        reader.readBytes(STRL_GSO_HEAD.length());

        // Reading the stored (v,o) pair: 
        long vStored = reader.readInteger();
        long oStored = reader.readInteger(DTAVersion == 117? 4: 8);

        String voPair = v + "," + o;

        if (vStored != v || oStored != o) {
            throw new IOException("GSO reading mismatch: expected v,o pair: "
                    + voPair + ", found: " + vStored + "," + oStored);
        }

        short type = reader.readByte();
        boolean binary = false;

        if (type == 129) {
            logger.fine("STRL TYPE: binary");
            binary = true;
        } else if (type == 130) {
            logger.fine("STRL TYPE: ascii");
        } else {
            logger.warning("WARNING: unknown STRL type: " + type);
        }

        long length = reader.readInteger();

        logger.fine("Advertised length of the STRL: " + length);

        // TODO:
        // length can technically be 0 < length < 2^^32;
        // but Java arrays are only [int], i.e., can only have < 2^^31
        // elements; readBytes() allocates and returns a byte[] array.
        // so I should probably check the value of length - if it 
        // can fit into a signed int; not that it's likely to happen 
        // in real life. -- L.A. 4.0 beta 11
        byte[] contents = reader.readBytes((int) length);

        // TODO: 
        // Depending on whether this GSO is advertised as ASCII or binary, 
        // I should probably create the String as either ASCII or UTF8. 
        // (and the trailing zero needs to be chopped, if it's the ASCII kind)
        //  -- L.A. 4.0 beta 11
        String gsoString;
        if (binary) {
            gsoString = new String(contents, "utf8"); // ?
        } else {
            gsoString = new String(contents, 0, (int) length - 1, "US-ASCII");
        }

        logger.fine("GSO " + v + "," + o + ": " + gsoString);

        String escapedGsoString = escapeCharacterString(gsoString);

        if (cachedGSOs.containsKey(voPair)) {
            // We need to cache this GSO: 
            if (!"".equals(cachedGSOs.get(voPair))) {
                throw new IOException("Multiple GSO definitions for v,o " + voPair);
            }
            cachedGSOs.put(voPair, escapedGsoString);
        }

        return escapedGsoString;
    }

    private void readValueLabels(DataReader reader) throws IOException {
        logger.fine("Value Labels section; at offset " + reader.getByteOffset() + "; dta map offset: " + dtaMap.getOffset_vallabs());
        logger.fine("readValueLabels(): start.");

        // TODO: 
        // check that we are at the right byte offset!
        reader.readOpeningTag(TAG_VALUE_LABELS);

        while (reader.checkTag("<" + TAG_VALUE_LABELS_LBL_DEF + ">")) {
            // TODO: checktag should probably *read* the tag, if it is indeed
            // encountered, rather then stop at the beginning of the tag. 
            reader.readOpeningTag(TAG_VALUE_LABELS_LBL_DEF);
            long label_table_length = reader.readInteger();
            // TODO: 
            // think of better variable names...

            String label_table_name = reader.readString(DTAVersion == 117? 33: 129);
            // TODO: 
            // do we need to worry about uniquness? or has Stata already 
            // guaranteed that there are no other category value table 
            // defined under this name?
            reader.readBytes(3); // TODO: skipBytes() instead

            long value_category_offset = 0;

            // read the value_label_table that follows. 
            // should be label_table_length. 
            int number_of_categories = (int) reader.readInteger();
            long text_length = reader.readInteger();

            value_category_offset = 8;

            long[] value_label_offsets = new long[number_of_categories];
            long[] value_label_offsets_sorted = null; 
            long[] category_values = new long[number_of_categories];
            String[] category_value_labels = new String[number_of_categories];

            boolean alreadySorted = true;
            
            for (int i = 0; i < number_of_categories; i++) {
                value_label_offsets[i] = reader.readInteger();
                logger.fine("offset " + i + ": " + value_label_offsets[i]);
                value_category_offset += 4;
                if (i > 0 && value_label_offsets[i] < value_label_offsets[i-1]) {
                    alreadySorted = false;
                }
            }

            if (!alreadySorted) {
                //value_label_offsets_sorted = new long[number_of_categories];
                value_label_offsets_sorted = Arrays.copyOf(value_label_offsets, number_of_categories);
                Arrays.sort(value_label_offsets_sorted);
            }

            for (int i = 0; i < number_of_categories; i++) {
                // TODO: 
                // can the category values be negative?
                category_values[i] = reader.readInteger();
                value_category_offset += 4;
            }

            int total_label_bytes = 0;

            long label_offset = 0;
            long label_end = 0;
            int label_length = 0;

            // Read the remaining bytes in this <lbl> section. 
            // This byte[] array will contain all the value labels for the
            // variable. Each is terminated by the binary zero byte; so we 
            // can read the bytes for each label at the defined offset until 
            // we encounter \000. Or we can rely on the (sorted) list of offsets
            // to determine where each label ends (implemented below). 
            byte[] labelBytes = null;
            if((int)text_length != 0) { //If length is 0 we don't need to read any bytes
                labelBytes = reader.readBytes((int)text_length);
            }
            
            for (int i = 0; i < number_of_categories; i++) {
                label_offset = value_label_offsets[i];

                if (value_label_offsets_sorted == null) {
                label_end = i < number_of_categories - 1 ? value_label_offsets[i + 1] : text_length;
                } else {
                    int sortedPos = Arrays.binarySearch(value_label_offsets_sorted, label_offset);
                    label_end = sortedPos < number_of_categories - 1 ? value_label_offsets_sorted[sortedPos + 1] : text_length;
                }
                label_length = (int)(label_end - label_offset);

                category_value_labels[i] = new String(Arrays.copyOfRange(labelBytes, (int)label_offset, (int)label_end-1), "US-ASCII");
                total_label_bytes += label_length;
            }

            value_category_offset += total_label_bytes;

            logger.fine("text_length: " + text_length);
            logger.fine("total_label_bytes: " + total_label_bytes);
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
                    logger.fine("cross-linking value label table for " + label_table_name);
                    // it is actually a legit condition - when 
                    // a variable is advertised as linked to a category values
                    // table of a certain name, but no such table exists.
                    // -- L.A.
                    for (int j = 0; j < number_of_categories; j++) {
                        VariableCategory cat = new VariableCategory();

                        long cat_value = category_values[j];
                        String cat_label = category_value_labels[j];

                        cat.setValue("" + cat_value);
                        cat.setLabel(cat_label);

                        /* cross-link the variable and category to each other: */
                        cat.setDataVariable(dataTable.getDataVariables().get(i));
                        dataTable.getDataVariables().get(i).getCategories().add(cat);
                    }
                }
            }
        }

        reader.readClosingTag(TAG_VALUE_LABELS);
        logger.fine("readValueLabels(): end.");

    }

    /*
     * Helper methods for decoding data:
     */
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
            if (stringLength == null || stringLength < 1 || stringLength > 2045) {
                throw new IOException("Invalid STRF encountered: " + variableType);
            }
            return stringLength;
        }

        throw new IOException("Unknown/invalid variable type: " + variableType);
    }

    private class DecodedDateTime {

        String format;
        String decodedDateTime;
    }

    private DecodedDateTime decodeDateTimeData(String storageType, String FormatType, String rawDatum) throws IOException {

        logger.fine("(storageType, FormatType, rawDatum)=("
                + storageType + ", " + FormatType + ", " + rawDatum + ")");
        /*
         *         Historical note:
                   pseudofunctions,  td(), tw(), tm(), tq(), and th()
                used to be called     d(),  w(),  m(),  q(), and  h().
                Those names still work but are considered anachronisms.

         */

        long milliSeconds;
        String decodedDateTime = null;
        String format = null;

        if (FormatType.matches("^%tc.*")) {
            // tc is a relatively new format
            // datum is millisecond-wise
            milliSeconds = Math.round(new Double(rawDatum)) + STATA_BIAS_TO_EPOCH;
            decodedDateTime = sdf_ymdhmsS.format(new Date(milliSeconds));
            format = sdf_ymdhmsS.toPattern();
            logger.fine("tc: result=" + decodedDateTime + ", format = " + format);

        } else if (FormatType.matches("^%t?d.*")) {
            milliSeconds = Math.round(new Double(rawDatum)) * MILLISECCONDS_PER_DAY + STATA_BIAS_TO_EPOCH;
            logger.fine("milliSeconds=" + milliSeconds);

            decodedDateTime = sdf_ymd.format(new Date(milliSeconds));
            format = sdf_ymd.toPattern();
            logger.fine("td:" + decodedDateTime + ", format = " + format);

        } else if (FormatType.matches("^%t?w.*")) {

            long weekYears = Math.round(new Double(rawDatum));
            long left = Math.abs(weekYears) % 52L;
            long years;
            if (weekYears < 0L) {
                left = 52L - left;
                if (left == 52L) {
                    left = 0L;
                }
                //out.println("left="+left);
                years = (Math.abs(weekYears) - 1) / 52L + 1L;
                years *= -1L;
            } else {
                years = weekYears / 52L;
            }

            String yearString = Long.toString(1960L + years);
            String dayInYearString = new DecimalFormat("000").format((left * 7) + 1);
            String yearDayInYearString = yearString + "-" + dayInYearString;

            Date tempDate = null;
            try {
                tempDate = new SimpleDateFormat("yyyy-DDD").parse(yearDayInYearString);
            } catch (ParseException ex) {
                throw new IOException(ex);
            }

            decodedDateTime = sdf_ymd.format(tempDate.getTime());
            format = sdf_ymd.toPattern();

        } else if (FormatType.matches("^%t?m.*")) {
            // month 
            long monthYears = Math.round(new Double(rawDatum));
            long left = Math.abs(monthYears) % 12L;
            long years;
            if (monthYears < 0L) {
                left = 12L - left;
                //out.println("left="+left);
                years = (Math.abs(monthYears) - 1) / 12L + 1L;
                years *= -1L;
            } else {
                years = monthYears / 12L;
            }

            String month = null;
            if (left == 12L) {
                left = 0L;
            }
            Long monthdata = (left + 1);
            month = "-" + twoDigitFormatter.format(monthdata) + "-01";
            long year = 1960L + years;
            String monthYear = year + month;
            logger.fine("rawDatum=" + rawDatum + ": monthYear=" + monthYear);

            decodedDateTime = monthYear;
            format = "yyyy-MM-dd";
            logger.fine("tm:" + decodedDateTime + ", format:" + format);

        } else if (FormatType.matches("^%t?q.*")) {
            // quater
            long quaterYears = Math.round(new Double(rawDatum));
            long left = Math.abs(quaterYears) % 4L;
            long years;
            if (quaterYears < 0L) {
                left = 4L - left;
                //out.println("left="+left);
                years = (Math.abs(quaterYears) - 1) / 4L + 1L;
                years *= -1L;
            } else {
                years = quaterYears / 4L;
            }

            String quater = null;

            if ((left == 0L) || (left == 4L)) {
                //quater ="q1"; //
                quater = "-01-01";
            } else if (left == 1L) {
                //quater = "q2"; //
                quater = "-04-01";
            } else if (left == 2L) {
                //quater = "q3"; //
                quater = "-07-01";
            } else if (left == 3L) {
                //quater = "q4"; //
                quater = "-11-01";
            }

            long year = 1960L + years;
            String quaterYear = Long.toString(year) + quater;
            logger.fine("rawDatum=" + rawDatum + ": quaterYear=" + quaterYear);

            decodedDateTime = quaterYear;
            format = "yyyy-MM-dd";
            logger.fine("tq:" + decodedDateTime + ", format:" + format);

        } else if (FormatType.matches("^%t?h.*")) {
            // half year
            // odd number:2nd half
            // even number: 1st half

            long halvesYears = Math.round(new Double(rawDatum));
            long left = Math.abs(halvesYears) % 2L;
            long years;
            if (halvesYears < 0L) {
                years = (Math.abs(halvesYears) - 1) / 2L + 1L;
                years *= -1L;
            } else {
                years = halvesYears / 2L;
            }

            String half = null;
            if (left != 0L) {
                // odd number => 2nd half: "h2"
                //half ="h2"; //
                half = "-07-01";
            } else {
                // even number => 1st half: "h1"
                //half = "h1"; //
                half = "-01-01";
            }
            long year = 1960L + years;
            String halfYear = Long.toString(year) + half;
            logger.fine("rawDatum=" + rawDatum + ": halfYear=" + halfYear);

            decodedDateTime = halfYear;
            format = "yyyy-MM-dd";
            logger.fine("th:" + decodedDateTime + ", format:" + format);

        } else if (FormatType.matches("^%t?y.*")) {
            // year type's origin is 0 AD
            decodedDateTime = rawDatum;
            format = "yyyy";
            logger.fine("th:" + decodedDateTime);
        } else {
            decodedDateTime = rawDatum;
            format = null;
        }
        DecodedDateTime retValue = new DecodedDateTime();
        retValue.decodedDateTime = decodedDateTime;
        retValue.format = format;
        return retValue;
    }

    private class DataReader {

        private BufferedInputStream stream;
        private int DEFAULT_BUFFER_SIZE = 8192;// * 2;
        private byte[] byte_buffer;
        private int buffer_size;
        private long byte_offset;
        private int buffer_byte_offset;
        Boolean LSF = null;

        public DataReader(BufferedInputStream stream) throws IOException {
            this(stream, 0);
        }

        public DataReader(BufferedInputStream stream, int size) throws IOException {
            if (buffer_size > 0) {
                this.DEFAULT_BUFFER_SIZE = size;
            }
            this.stream = stream;
            byte_buffer = new byte[DEFAULT_BUFFER_SIZE];
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

        // this returns the *absolute* byte offest in the stream. 
        public long getByteOffset() {
            return byte_offset + buffer_byte_offset;
        }

        public void setByteOffset(long byte_offset) {
            this.byte_offset = byte_offset;
        }

        /* 
            readBytes is the workhorse method of the internal Data Reader class.
            it reads the requested number of bytes from the buffer, if available, 
            refilling the buffer as necessary. 
            the method allocates the byte array it returns, so there's no need 
            to do so outside of it. 
            the method will throw an exception if for whatever reason it cannot
            read the requested number of bytes. 
         */
        public byte[] readBytes(int n) throws IOException {
            if (n <= 0) {
                throw new IOException("DataReader.readBytes called to read zero or negative number of bytes.");
                }
            byte[] bytes = new byte[n];

            if (this.buffer_size - buffer_byte_offset >= n) {
                System.arraycopy(byte_buffer, buffer_byte_offset, bytes, 0, n);
                buffer_byte_offset += n;
            } else {
                int bytes_read = 0;

                /* if there are any bytes left in the buffer, 
                 * copy them into the return array:
                 */
                if (this.buffer_size - buffer_byte_offset > 0) {
                    logger.fine("reading the remaining " + (this.buffer_size - buffer_byte_offset) + " bytes from the buffer");
                    System.arraycopy(byte_buffer, buffer_byte_offset, bytes, 0, this.buffer_size - buffer_byte_offset);
                    //buffer_byte_offset = this.buffer_size;
                    bytes_read = this.buffer_size - buffer_byte_offset;
                    buffer_byte_offset = this.buffer_size;
                }

                int morebytes = bufferMoreBytes();
                logger.fine("buffered " + morebytes + " bytes");

                /* 
                 * TODO: combine this block with the one above -- ?
                 * if multiple BUFFER_SIZE-byte worth chunk of data is requested,
                 * keep reading and buffering: 
                 */
                while (n - bytes_read > this.buffer_size) {
                    logger.fine("copying a full buffer-worth of bytes into the return array");
                    System.arraycopy(byte_buffer, buffer_byte_offset, bytes, bytes_read, this.buffer_size);
                    //buffer_byte_offset = this.buffer_size;
                    bytes_read += this.buffer_size;
                    buffer_byte_offset = this.buffer_size;
                    morebytes = bufferMoreBytes();
                    logger.fine("buffered "+morebytes+" bytes");
                }

                /* 
                 * finally, copy the last not-a-full-buffer-worth of bytes 
                 * into the return buffer:
                 */
                logger.fine("copying the remaining " + (n - bytes_read) + " bytes.");
                System.arraycopy(byte_buffer, 0, bytes, bytes_read, n - bytes_read);
                buffer_byte_offset = n - bytes_read;
            }

            return bytes;
        }

        /* 
         * This method tries to read and buffer the DEFAULT_BUFFER_SIZE bytes
         * and sets the current buffer size accordingly.
         */
        private int bufferMoreBytes() throws IOException {
            int actual_bytes_read;
            byte_offset += buffer_byte_offset;
            
            if (byte_offset == 0 || buffer_byte_offset == buffer_size) {
                actual_bytes_read = stream.read(byte_buffer, 0, DEFAULT_BUFFER_SIZE);
            // set the current buffer size to the actual number of 
            // bytes read: 
            this.buffer_size = actual_bytes_read;

            // reset the current buffer offset and increment the total
            // byte offset by the size of the last buffer - that should be 
            // equal to the buffer_byte_offset. 
            // (TODO: check that this is the case!)
                

            } else if (buffer_byte_offset < buffer_size) {
                System.arraycopy(byte_buffer, buffer_byte_offset, byte_buffer, 0, buffer_size - buffer_byte_offset);
                this.buffer_size = buffer_size - buffer_byte_offset;
                actual_bytes_read = stream.read(byte_buffer, buffer_size, DEFAULT_BUFFER_SIZE - buffer_size);
                buffer_size += actual_bytes_read;
                

            } else {
                throw new IOException("Offset already past the buffer boundary");
            }
            buffer_byte_offset = 0;

            return actual_bytes_read;
        }

        /* 
         * Convenience methods for reading single bytes of data. 
         * Just like with the other types of integers, both the signed and 
         * unsigned versions are provided. 
         * The readSignedByte() is used to read STATA *data* stored as 
         * type "Byte"; the unsigned version is used to read byte values 
         * in various sections of the file that store the lengths of byte
         * sequences that follow. 
         * Examples: (TODO: ...)
         */
        private byte readSignedByte() throws IOException {
            /* Why not just use readBytes(1) here, you ask?
             * - Because readBytes() will want to allocate a 
             * return byte[] buffer of size 1. */
            byte ret;
            if (buffer_byte_offset > this.buffer_size) {
                throw new IOException("TD - buffer overflow");
            }
            if (buffer_byte_offset < this.buffer_size) {
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

        // Note that readByte() returns the value of Java type "short". 
        // This is to accommodate value larger than 127.
        private short readByte() throws IOException {
            short ret = readSignedByte();

            if (ret < 0) {
                ret += 256;
            }
            return ret;
        }

        /* Various reader methods for reading primitive numeric types; 
         * these are used both for reading the values from the data section
         * (signed integer and floating-point types), and to read numeric 
         * values encoded as unsigned bytes in various sections of the file, 
         * advertising the lengths of the data sections that follow. 
         * Note that the internal methods bytesToInt() and bytesToSignedInt()
         * will throw an exception if LSF (byte order flag) has not yet been 
         * set.
         */
        // Unsigned integer methods readInteger() and readShortInteger()
        // below return long (8 byte) and int (4 byte) integers, respectively.
        // This is to accommodate the values larger than 2^31-1 and 2^15-1, 
        // respectively. 
        public long readInteger() throws IOException {
            return readInteger(4);
        }

        public int readSignedInteger() throws IOException {
            return readSignedInteger(4);
        }

        public short readShortSignedInteger() throws IOException {
            return (short) readSignedInteger(2);
        }

        // Only an unsigned version of readLongInteger() is provided; 
        // This is because STATA does not support 8 byte integer data types.
        // 8 byte integers are only used as unsigned values specifying byte 
        // lengths. 
        public long readLongInteger() throws IOException {
            byte[] raw_bytes = readBytes(8);

            return bytesToLong(raw_bytes);
        }

        // Service readInteger() methods that can read either 2 or 4
        // byte integers:
        private int readInteger(int n) throws IOException {
            byte[] raw_bytes = readBytes(n);

            return (int) bytesToInt(raw_bytes);
        }

        private int readSignedInteger(int n) throws IOException {
            byte[] raw_bytes = readBytes(n);

            return bytesToSignedInt(raw_bytes);
        }

        // Floating point reader methods: 
        public double readDouble() throws IOException {
            if (LSF == null) {
                throw new IOException("Byte order not determined for reading numeric values.");
            }
            ByteBuffer double_buffer = ByteBuffer.wrap(readBytes(8));
            if (LSF) {
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

        /* 
         * internal service methods used by the methods above, to convert 
         * bytes into the appropriate numeric types:
         */
        // Unsigned version:
        private long bytesToInt(byte[] raw_bytes) throws IOException {
            if (LSF == null) {
                throw new IOException("Byte order not determined for reading numeric values.");
            }
            int n = raw_bytes.length;

            if (n != 2 && n != 4 && n != 6 && n != 8) {
                throw new IOException("Unsupported number of bytes in an integer: " + n);
            }
            long ret = 0;
            short unsigned_byte_value = 0;

            for (int i = 0; i < n; i++) {
                if (LSF) {
                    unsigned_byte_value = raw_bytes[i];
                } else {
                    unsigned_byte_value = raw_bytes[n - i - 1];
                }

                if (unsigned_byte_value < 0) {
                    unsigned_byte_value += 256;
                }

                ret += unsigned_byte_value * (1 << (8 * i));
            }

            return ret;
        }

        private int bytesToSignedInt(byte[] raw_bytes) throws IOException {
            if (LSF == null) {
                throw new IOException("Byte order not determined for reading numeric values.");
            }
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
                throw new IOException("Unsupported number of bytes for signed integer: " + n);
            }
            return int_value;
        }

        private long bytesToLong(byte[] raw_bytes) throws IOException {
            if (raw_bytes.length != 8) {
                throw new IOException("Wrong number of bytes in bytesToLong().");
            }
            if (LSF == null) {
                throw new IOException("Byte order not determined for reading numeric values.");
            }

            long ret = 0;

            ByteBuffer byte_buffer
                    = ByteBuffer.wrap(raw_bytes);
            if (LSF) {
                byte_buffer.order(ByteOrder.LITTLE_ENDIAN);

            }
            ret = byte_buffer.getLong();

            return ret;
        }

        /* 
         * Method for reading character strings:
         *
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

        /* 
         * More complex helper methods for reading NewDTA "sections" ...
         * TODO: document this ...
         */
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

        public String readLabelSection(String tag, int limit) throws IOException {
            readOpeningTag(tag);
            /**
             * ll The byte length of the UTF-8 characters, whose length is
             * recorded in a 2-byte unsigned integer encoded according to
             * byteorder.
             */
            int lengthOfLabel = readInteger(2);
            logger.fine("length of label: " + lengthOfLabel);
            String label = null;
            if (lengthOfLabel > 0) {
                label = new String(readBytes(lengthOfLabel), "US-ASCII");
            }
            logger.fine("ret: " + label);
            readClosingTag(tag);
            return label;
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
//            short number = readByte2();
//            byte[] bytes = readBytes(2);
//            short number = 0; // FIXME: Don't hard code this to 0. Some files will have labels.
//            String str = new String(bytes, StandardCharsets.UTF_8);
//            logger.info("str: " + str);
//            int number = bytes.length;
//            short number2 = readByte();
            logger.fine("number: " + number);
            if (number < 0 || number > limit) {
                throw new IOException("<more than limit characters in the section \"tag\">");
            }
            String ret = null;
            if (number > 0) {
                ret = new String(readBytes(number), "US-ASCII");
            }
            logger.fine("ret: " + ret);
            readClosingTag(tag);
            return ret;
        }

        public int readIntegerSection(String tag, int n) throws IOException {
            readOpeningTag(tag);
            int number = readInteger(n);
            readClosingTag(tag);
            return number;
        }

        // This helper method is used for skipping the <ch>llll...</ch> sections
        // inside the "<charachteristics>" section; where llll is a 4-byte unsigned
        // int followed by llll bytes.
        public void skipDefinedSections(String tag) throws IOException {
            logger.fine("entering at offset " + buffer_byte_offset);
            while (checkTag("<" + tag + ">")) {
                // TODO: checkTag() should probably *read* the tag, if it is indeed
                // encountered, rather then stop at the beginning of the tag. 
                logger.fine("tag " + tag + " encountered at offset " + buffer_byte_offset);
                readOpeningTag(tag);
                long number = readInteger(4);
                logger.fine(number + " bytes in this section;");
                if (number < 0) {
                    throw new IOException("<negative number of bytes in skipDefinedSection(\"tag\")?>");
                }
                // TODO: implement skipBytes() instead:
                byte[] skipped_bytes = readBytes((int) number);
                readClosingTag(tag);
                logger.fine("read closing tag </" + tag + ">;");

            }
            logger.fine("exiting at offset " + buffer_byte_offset);
        }

        private boolean checkTag(String tag) throws IOException {
            if (tag == null || tag.equals("")) {
                throw new IOException("opening tag must be a non-empty string.");
            }

            int n = tag.length();
            if ((this.buffer_size - buffer_byte_offset) >= n) {
                return (tag).equals(new String(Arrays.copyOfRange(byte_buffer, buffer_byte_offset, buffer_byte_offset+n),"US-ASCII"));
            }
            else{
                bufferMoreBytes();
                return checkTag(tag);
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

            String closeTagString = new String(closeTag, "US-ASCII");
            logger.fine("closeTagString: " + closeTagString);

            if (closeTagString == null || !closeTagString.equals("</" + tag + ">")) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                String msg = "";
                for (int i = 0; i < 10; i++) {
                    StackTraceElement stackTraceElement = stackTrace[i];
                    msg += stackTraceElement.toString() + "\n";
                }
//                for (StackTraceElement stackTraceElement : stackTrace) {
//                    msg += stackTraceElement.toString() + "\n";
//                }
                throw new IOException("Could not read closing tag </" + tag + ">: " + msg);
            }
        }

        private byte[] readPrimitiveSectionBytes() throws IOException {
            byte[] cached_bytes = null;

            if (buffer_byte_offset > this.buffer_size) {
                throw new IOException("Buffer overflow in DataReader.");
            }
            if (buffer_byte_offset == this.buffer_size) {
                // buffer empty; 
                bufferMoreBytes();
            }

            int cached_offset = buffer_byte_offset;

            while (byte_buffer[buffer_byte_offset] != '<') {
                buffer_byte_offset++;

                if (buffer_byte_offset == this.buffer_size) {
                    logger.fine("reached the end of buffer in readPrimitiveSectionBytes; offset " + buffer_byte_offset);
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
                if (buffer_byte_offset - cached_offset < 0) {
                    throw new IOException("read error in save local buffer 1; TODO: better exception message");
                }
                // empty section - as in <section></section>
                if (buffer_byte_offset - cached_offset == 0) {
                    return null;
                }

                ret_bytes = new byte[buffer_byte_offset - cached_offset];
                System.arraycopy(byte_buffer, cached_offset, ret_bytes, 0, buffer_byte_offset - cached_offset);
            } else {
                if (cached_offset != 0) {
                    throw new IOException("read error in save local buffer 2; TODO: better exception message");
                }
                ret_bytes = new byte[cached_bytes.length + buffer_byte_offset];
                System.arraycopy(cached_bytes, 0, ret_bytes, 0, cached_bytes.length);
                if (buffer_byte_offset > 0) {
                    System.arraycopy(byte_buffer, 0, ret_bytes, cached_bytes.length, buffer_byte_offset);
                }
            }
            return ret_bytes;
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
