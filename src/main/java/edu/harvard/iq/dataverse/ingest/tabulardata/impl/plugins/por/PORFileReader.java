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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.por;

import java.io.*;
import java.nio.*;
import java.util.logging.*;

import java.util.*;
import java.util.regex.*;
import java.text.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

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
import edu.harvard.iq.dataverse.ingest.tabulardata.InvalidData;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.sav.SPSSConstants;



/**
 * ingest plugin for SPSS/POR ("portable") file format.
 *
 * This reader plugin has been fully re-implemented for the DVN 4.0;
 * It is still borrows heavily from, and builds on the basis of the 
 * old implementation by Akio Sone, that was in use in the versions 
 * 2-3 of the DVN.
 * 
 * @author Akio Sone at UNC-Odum
 * @author Leonid Andreev
 */

public class PORFileReader  extends TabularDataFileReader{
    @Inject
    VariableServiceBean varService;
        
    // static fields ---------------------------------------------------------//
    private static final String MissingValueForTextDataFile = "";

    private TabularDataIngest ingesteddata = new TabularDataIngest();
    private DataTable dataTable = new DataTable(); 
    
    private static final int POR_HEADER_SIZE = 500;   
    private static final int POR_MARK_POSITION_DEFAULT = 461;
    private static final String POR_MARK = "SPSSPORT";
    private static final int LENGTH_SECTION_HEADER = 1;
    private static final int LENGTH_SECTION_2 = 19;        
    private static final String MIME_TYPE = "application/x-spss-por";
    private static Pattern pattern4positiveInteger = Pattern.compile("[0-9A-T]+");
    private static Pattern pattern4Integer = Pattern.compile("[-]?[0-9A-T]+");
    private static Calendar GCO = new GregorianCalendar();
    static {
        // set the origin of GCO to 1582-10-15
        GCO.set(1, 1582);// year
        GCO.set(2, 9); // month
        GCO.set(5, 15);// day of month
        GCO.set(9, 0);// AM(0) or PM(1)
        GCO.set(10, 0);// hh
        GCO.set(12, 0);// mm
        GCO.set(13, 0);// ss
        GCO.set(14, 0); // SS millisecond
        GCO.set(15, 0);// z
        
    }
    private static final long SPSS_DATE_BIAS = 60*60*24*1000;
    private static final long SPSS_DATE_OFFSET = SPSS_DATE_BIAS + Math.abs(GCO.getTimeInMillis());
    

    // instance fields -------------------------------------------------------//

    private static Logger dbgLog = Logger.getLogger(PORFileReader.class.getPackage().getName());

    private boolean isCurrentVariableString = false;
    private String currentVariableName = null;

    private int caseQnty=0;
    private int varQnty=0;

    private Map<String, Integer> variableTypeTable = new LinkedHashMap<String, Integer>();
    private List<Integer> variableTypelList = new ArrayList<Integer>();
    private List<Integer> printFormatList = new ArrayList<Integer>();
    private Map<String, String> printFormatTable = new LinkedHashMap<String, String>();
    private Map<String, String> printFormatNameTable = new LinkedHashMap<String, String>();
    private Map<String, String> formatCategoryTable = new LinkedHashMap<String, String>();
    private Map<String, Map<String, String>> valueLabelTable = new LinkedHashMap<String, Map<String, String>>();
    private Map<String, String> valueVariableMappingTable = new LinkedHashMap<String, String>();
    private List<String> variableNameList = new ArrayList<String>();
    private Map<String, String> variableLabelMap = new LinkedHashMap<String, String>();
    // missing value table: string/numeric data are stored  => String
    // the number of missing values are unknown beforehand => List
    private Map<String, List<String>> missingValueTable = new LinkedHashMap<String, List<String>>();
    // variableName=> missingValue type[field code]
    private Map<String, List<String>> missingValueCodeTable = new LinkedHashMap<String, List<String>>();
    private Map<String, InvalidData> invalidDataTable = new LinkedHashMap<String, InvalidData>();
    private Set<Integer> decimalVariableSet = new HashSet<Integer>();
    private List<Integer> formatDecimalPointPositionList= new ArrayList<Integer>();



    // date/time data format
    private SimpleDateFormat sdf_ymd    = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat sdf_ymdhms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat sdf_dhms   = new SimpleDateFormat("DDD HH:mm:ss");
    private SimpleDateFormat sdf_hms    = new SimpleDateFormat("HH:mm:ss");

    // DecimalFormat for doubles
    // may need more setXXXX() to handle scientific data
    private NumberFormat doubleNumberFormatter = new DecimalFormat();

    private String[] variableFormatTypeList;
    private String[] dateFormatList;



    // Constructor -----------------------------------------------------------//

    public PORFileReader(TabularDataFileReaderSpi originator){
        super(originator);
    }

    
    private void init() throws IOException {
        
        Context ctx = null; 
        try {
            ctx = new InitialContext();
            varService = (VariableServiceBean) ctx.lookup("java:global/dataverse-4.0/VariableServiceBean");
        } catch (NamingException nex) {
            try {
                ctx = new InitialContext();
                varService = (VariableServiceBean) ctx.lookup("java:global/dataverse/VariableServiceBean");
            } catch (NamingException nex2) {
                if (dbgLog.isLoggable(Level.INFO)) dbgLog.fine("Could not look up initial context, or the variable service in JNDI!");
                throw new IOException ("Could not look up initial context, or the variable service in JNDI!"); 
            }
        }
        sdf_ymd.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf_ymdhms.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf_dhms.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf_hms.setTimeZone(TimeZone.getTimeZone("GMT"));
    
        doubleNumberFormatter.setGroupingUsed(false);
        doubleNumberFormatter.setMaximumFractionDigits(340); // TODO: 340?? -- L.A. 4.0 beta
    }
    
    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException{
        dbgLog.fine("PORFileReader: read() start");
        
        if (dataFile != null) {
            throw new IOException ("this plugin does not support external raw data files");
        }
        
        
        File tempPORfile = decodeHeader(stream);
        BufferedReader bfReader = null;
        
        try {            
            bfReader = new BufferedReader(new InputStreamReader(new FileInputStream(tempPORfile.getAbsolutePath()), "US-ASCII"));
            if (bfReader == null){
                dbgLog.fine("bfReader is null");
                throw new IOException("bufferedReader is null");
            }
            
            decodeSec2(bfReader);
            
            while(true){

                char[] header = new char[LENGTH_SECTION_HEADER]; // 1 byte
                bfReader.read(header);
                String headerId = Character.toString(header[0]);
                
                dbgLog.fine("////////////////////// headerId="+headerId+ "//////////////////////");
                
                if (headerId.equals("Z")){
                    throw new IOException("reading failure: wrong headerId(Z) here");
                }
                
                if (headerId.equals("F")) {
                    // missing value
                    if ((missingValueTable !=null) && (missingValueTable.size()>0)){
                        processMissingValueData();
                    }
                }
                                
                if (headerId.equals("8") && isCurrentVariableString){
                    headerId = "8S";
                }

                decode(headerId, bfReader);

                
                // for last iteration
                if (headerId.equals("F")){
                    // finished the last block (F == data) 
                    // without reaching the end of this file.
                    break;
                }
            }
            
                    
        } finally {
            try {
                if (bfReader!= null){
                    bfReader.close();
                }
            } catch (IOException ex){
                ex.printStackTrace();
            }

            if (tempPORfile.exists()){
                tempPORfile.delete();
            }
        }
        
        dbgLog.fine("done parsing headers and decoding;");

        List<DataVariable> variableList = new ArrayList<DataVariable>();
        
        for (int indx = 0; indx < variableTypelList.size(); indx++) {
            
            DataVariable dv = new DataVariable();
            String varName = variableNameList.get(indx); 
            dv.setName(varName);
            String varLabel = variableLabelMap.get(varName);
            if (varLabel != null && varLabel.length() > 255) {
                varLabel = varLabel.substring(0, 255);
            }
            dv.setLabel(varLabel);
            
            dv.setInvalidRanges(new ArrayList());
            dv.setSummaryStatistics( new ArrayList() );
            dv.setUnf("UNF:6:");
            dv.setCategories(new ArrayList());
            dv.setFileOrder(indx);
            dv.setDataTable(dataTable);
            
            variableList.add(dv);            
            
            int simpleType = 0;
            if (variableTypelList.get(indx) != null) {
                simpleType = variableTypelList.get(indx).intValue();
            }

            if (simpleType <= 0) {
                // We need to make one last type adjustment:
                // Dates and Times will be stored as character values in the 
                // dataverse tab files; even though they are not typed as 
                // strings at this point:
                // TODO: 
                // Make sure the date/time format is properly preserved!
                // (see the setFormatCategory below... but double-check!)
                // -- L.A. 4.0 alpha
                String variableFormatType = variableFormatTypeList[indx];
                
                if (variableFormatType != null) {
                    if (variableFormatType.equals("time")
                        || variableFormatType.equals("date")) {
                        simpleType = 1; 
                    
                        String formatCategory = formatCategoryTable.get(varName);

                        if (formatCategory != null) {
                            if (dateFormatList[indx] != null) {
                                dbgLog.fine("setting format category to "+formatCategory);
                                variableList.get(indx).setFormatCategory(formatCategory);
                                dbgLog.fine("setting formatschemaname to "+dateFormatList[indx]);
                                variableList.get(indx).setFormatSchemaName(dateFormatList[indx]);
                            }
                        }
                    } else if (variableFormatType.equals("other")) {
                        dbgLog.fine("Variable of format type \"other\"; type adjustment may be needed");
                        dbgLog.fine("SPSS print format: "+printFormatTable.get(variableList.get(indx).getName()));
                        
                        if (printFormatTable.get(variableList.get(indx).getName()).equals("WKDAY")
                            || printFormatTable.get(variableList.get(indx).getName()).equals("MONTH")) {
                            // week day or month; 
                            // These are not treated as time/date values (meaning, we 
                            // don't define time/date formats for them; there's likely 
                            // no valid ISO time/date format for just a month or a day 
                            // of week). However, the
                            // values will be stored in the TAB files as strings, 
                            // and not as numerics - as they were stored in the 
                            // SAV file. So we need to adjust the type here.
                            // -- L.A. 
                            
                            simpleType = 1;
                        }
                    }
                }
                
            }
            
            dbgLog.fine("Finished creating variable "+indx+", "+varName);
            
            // OK, we can now assign the types: 
            
            if (simpleType > 0) {
                // String: 
                variableList.get(indx).setVariableFormatType(varService.findVariableFormatTypeByName("character"));
                variableList.get(indx).setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
            } else {
                // Numeric: 
                variableList.get(indx).setVariableFormatType(varService.findVariableFormatTypeByName("numeric"));
                // discrete or continuous?
                // "decimal variables" become dataverse data variables of interval type "continuous":
        
                if (decimalVariableSet.contains(indx)) {
                    variableList.get(indx).setVariableIntervalType(varService.findVariableIntervalTypeByName("continuous"));
                } else {
                    variableList.get(indx).setVariableIntervalType(varService.findVariableIntervalTypeByName("discrete"));
                }
                
            }
            dbgLog.fine("Finished configuring variable type information.");
        }
        
        
        dbgLog.fine("done configuring variables;");
        
        /* 
         * From the original (3.6) code: 
            //smd.setVariableTypeMinimal(ArrayUtils.toPrimitive(variableTypelList.toArray(new Integer[variableTypelList.size()])));
            smd.setVariableFormat(printFormatList);
            smd.setVariableFormatName(printFormatNameTable);
            smd.setVariableFormatCategory(formatCategoryTable);
            smd.setValueLabelMappingTable(valueVariableMappingTable);
         * TODO: 
         * double-check that it's all being taken care of by the new plugin!
         * (for variable format and formatName, consult the SAV plugin)
         */
        
        dataTable.setDataVariables(variableList);
        
        // Assign value labels: 
        
        assignValueLabels(valueLabelTable);
        
        ingesteddata.setDataTable(dataTable);
        
        dbgLog.info("PORFileReader: read() end");
        return ingesteddata;
    }
    
    private void decode(String headerId, BufferedReader reader) throws IOException{
        if (headerId.equals("1")) decodeProductName(reader);
        else if (headerId.equals("2")) decodeLicensee(reader);
        else if (headerId.equals("3")) decodeFileLabel(reader);
        else if (headerId.equals("4")) decodeNumberOfVariables(reader);
        else if (headerId.equals("5")) decodeFieldNo5(reader);
        else if (headerId.equals("6")) decodeWeightVariable(reader);
        else if (headerId.equals("7")) decodeVariableInformation(reader);
        else if (headerId.equals("8")) decodeMissValuePointNumeric(reader);
        else if (headerId.equals("8S")) decodeMissValuePointString(reader);
        else if (headerId.equals("9")) decodeMissValueRangeLow(reader);
        else if (headerId.equals("A")) decodeMissValueRangeHigh(reader);
        else if (headerId.equals("B")) decodeMissValueRange(reader);
        else if (headerId.equals("C")) decodeVariableLabel(reader);
        else if (headerId.equals("D")) decodeValueLabel(reader);
        else if (headerId.equals("E")) decodeDocument(reader);
        else if (headerId.equals("F")) decodeData(reader);
    }
    

    private File decodeHeader(BufferedInputStream stream) throws IOException {
        dbgLog.fine("decodeHeader(): start");
        File tempPORfile = null;

        if (stream  == null){
            throw new IllegalArgumentException("file == null!");
        }
        
        byte[] headerByes = new byte[POR_HEADER_SIZE];

        if (stream.markSupported()){
            stream.mark(1000);
        }
        int nbytes = stream.read(headerByes, 0, POR_HEADER_SIZE);

        //printHexDump(headerByes, "hex dump of the byte-array");

        if (nbytes == 0){
            throw new IOException("decodeHeader: reading failure");
        } else if ( nbytes < 491) {
           // Size test: by defnition, it must have at least
            // 491-byte header, i.e., the file size less than this threshold
            // is not a POR file
           dbgLog.fine("this file is NOT spss-por type");
           throw new IllegalArgumentException("file is not spss-por type");
        }
        // rewind the current reading position back to the beginning
        if (stream.markSupported()){
            stream.reset();
        }

        // line-terminating characters are usually one or two by defnition
        // however, a POR file saved by a genuine SPSS for Windows
        // had a three-character line terminator, i.e., failed to remove the
        // original file's one-character terminator when it was opened, and
        // saved it with the default two-character terminator without
        // removing original terminators. So we have to expect such a rare
        // case
        //
        // terminator
        // windows [0D0A]=>   [1310] = [CR/LF]
        // unix    [0A]  =>   [10]
        // mac     [0D]  =>   [13]
        // 3char  [0D0D0A]=> [131310] spss for windows rel 15
        //
        // terminating characters should be found at the following
        //                             column positions[counting from 0]:
        // unix    case: [0A]   : [80], [161], [242], [323], [404], [485]
        // windows case: [0D0A] : [81], [163], [245], [327], [409], [491]
        //           : [0D0D0A] : [82], [165], [248], [331], [414], [495]
        
        // convert b into a ByteBuffer
        
        ByteBuffer buff = ByteBuffer.wrap(headerByes);
        byte[] nlch = new byte[36];
        int pos1;
        int pos2;
        int pos3;
        int ucase = 0;
        int wcase = 0;
        int mcase = 0;
        int three = 0;
        int nolines = 6;
        int nocols = 80;
        for (int i = 0; i < nolines; ++i) {
            int baseBias = nocols * (i + 1);
            // 1-char case
            pos1 = baseBias + i;
            buff.position(pos1);
            dbgLog.finer("\tposition(1)=" + buff.position());
            int j = 6 * i;
            nlch[j] = buff.get();

            if (nlch[j] == 10) {
                ucase++;
            } else if (nlch[j] == 13) {
                mcase++;
            }

            // 2-char case
            pos2 = baseBias + 2 * i;
            buff.position(pos2);
            dbgLog.finer("\tposition(2)=" + buff.position());
            
            nlch[j + 1] = buff.get();
            nlch[j + 2] = buff.get();

            // 3-char case
            pos3 = baseBias + 3 * i;
            buff.position(pos3);
            dbgLog.finer("\tposition(3)=" + buff.position());
            
            nlch[j + 3] = buff.get();
            nlch[j + 4] = buff.get();
            nlch[j + 5] = buff.get();

            dbgLog.finer(i + "-th iteration position =" +
                    nlch[j] + "\t" + nlch[j + 1] + "\t" + nlch[j + 2]);
            dbgLog.finer(i + "-th iteration position =" +
                    nlch[j + 3] + "\t" + nlch[j + 4] + "\t" + nlch[j + 5]);
            
            if ((nlch[j + 3] == 13) &&
                (nlch[j + 4] == 13) &&
                (nlch[j + 5] == 10)) {
                three++;
            } else if ((nlch[j + 1] == 13) && (nlch[j + 2] == 10)) {
                wcase++;
            }

            buff.rewind();
        }
        
        boolean windowsNewLine = true;
        if (three == nolines) {
            windowsNewLine = false; // lineTerminator = "0D0D0A"
        } else if ((ucase == nolines) && (wcase < nolines)) {
            windowsNewLine = false; // lineTerminator = "0A"
        } else if ((ucase < nolines) && (wcase == nolines)) {
            windowsNewLine = true; //lineTerminator = "0D0A"
        } else if ((mcase == nolines) && (wcase < nolines)) {
            windowsNewLine = false; //lineTerminator = "0D"
        }


        buff.rewind();
        int PORmarkPosition = POR_MARK_POSITION_DEFAULT;
        if (windowsNewLine) {
            PORmarkPosition = PORmarkPosition + 5;
        } else if (three == nolines) {
            PORmarkPosition = PORmarkPosition + 10;
        }

        byte[] pormark = new byte[8];
        buff.position(PORmarkPosition);
        buff.get(pormark, 0, 8);
        String pormarks = new String(pormark);

        //dbgLog.fine("pormark =>" + pormarks + "<-");
        dbgLog.fine("pormark[hex: 53 50 53 53 50 4F 52 54 == SPSSPORT] =>" +
                new String(Hex.encodeHex(pormark)) + "<-");

        if (pormarks.equals(POR_MARK)) {
            dbgLog.fine("POR ID toke test: Passed");
            init();
                        
            dataTable.setOriginalFileFormat(MIME_TYPE);
            dataTable.setUnf("UNF:6:NOTCALCULATED");

        } else {
            dbgLog.fine("this file is NOT spss-por type");
            throw new IllegalArgumentException(
                "decodeHeader: POR ID token was not found");
        }

        // save the POR file without new line characters

        FileOutputStream fileOutPOR = null;
        Writer fileWriter = null;

        // Scanner class can handle three-character line-terminator
        Scanner porScanner = null;
        
        try {
            tempPORfile = File.createTempFile("tempPORfile.", ".por");
            fileOutPOR = new FileOutputStream(tempPORfile);
            fileWriter = new BufferedWriter(new OutputStreamWriter(fileOutPOR, "utf8"));
            porScanner = new Scanner(stream);

            // Because 64-bit and 32-bit machines decode POR's first 40-byte
            // sequence differently, the first 5 leader lines are skipped from
            // the new-line-stripped file

            int lineCounter= 0;
            while(porScanner.hasNextLine()){
                lineCounter++;
                if (lineCounter<=5){
                    String line = porScanner.nextLine().toString();
                    dbgLog.fine("line="+lineCounter+":"+line.length()+":"+line);
                } else {
                    fileWriter.write(porScanner.nextLine().toString());
                }
            }
        } finally {
            try{
                if (fileWriter != null){
                    fileWriter.close();
                }
            } catch (IOException ex){
                ex.printStackTrace();
            }

            if (porScanner != null){
                porScanner.close();
            }
        }

        return tempPORfile;
    }



    private void decodeSec2(BufferedReader reader) throws IOException {
        dbgLog.fine("decodeSec2(): start");
        if (reader ==null){
            throw new IllegalArgumentException("decodeSec2: stream == null!");
        }

        // Because a 64-bit machine may not save the first 40
        // bytes of a POR file in a way as a 32-bit machine does,
        // the first 5 lines of a POR file is excluded from the read-back
        // file and the new 1st line contains the format mark "SPSSPORT"
        // somewhere in it.

        // mark the start position for the later rewind
        if (reader.markSupported()){
            reader.mark(100000);
        }


        char[] sixthLineCharArray = new char[80];
        int nbytes_sixthLine = reader.read(sixthLineCharArray);

        String sixthLine = new String(sixthLineCharArray);
        dbgLog.fine("sixthLineCharArray="+
            Arrays.deepToString(ArrayUtils.toObject(sixthLineCharArray)));
        int signatureLocation = sixthLine.indexOf(POR_MARK);

        if (signatureLocation >= 0){
            dbgLog.fine("format signature was found at:"+signatureLocation);
        } else {
            dbgLog.severe("signature string was not found");
            throw new IOException("signature string was not found");
        }

        // rewind the position to the beginning
        reader.reset();

        // skip bytes up to the signature string
        long skippedBytes = reader.skip(signatureLocation);

        char[] sec2_leader = new char[POR_MARK.length()];
        int nbytes_sec2_leader = reader.read(sec2_leader);

        String leader_string = new String(sec2_leader);

        dbgLog.fine("format signature [SPSSPORT] detected="+leader_string);


        if (leader_string.equals("SPSSPORT")){
            dbgLog.fine("signature was correctly detected");

        } else {
            dbgLog.severe(
            "the format signature is not found at the previously located column");
            throw new IOException("decodeSec2: failed to find the signature string");
        }

        int length_section_2 = LENGTH_SECTION_2;

        char[] Sec2_bytes = new char[length_section_2];

        int nbytes_sec2 = reader.read(Sec2_bytes);

        if (nbytes_sec2 == 0){
            dbgLog.severe("decodeSec2: reading error");
            throw new IOException("decodeSec2: reading error");
        } else {
            dbgLog.fine("bytes read="+nbytes_sec2);
        }

        String sec2 = new String(Sec2_bytes);
        dbgLog.fine("sec2[creation date/time]="+sec2);

        // sec2
        //       0123456789012345678
        //       A8/YYYYMMDD6/HHMMSS
        // thus
        // section2 should has 3 elements

        String[] section2 = StringUtils.split(sec2, '/');

        dbgLog.fine("section2="+StringUtils.join(section2, "|"));

        String fileCreationDate =null;
        String fileCreationTime = null;
        if ((section2.length == 3)&& (section2[0].startsWith("A"))){
            fileCreationDate = section2[1].substring(0,7);
            fileCreationTime = section2[2];
        } else {
            dbgLog.severe("decodeSec2: file creation date/time were not correctly detected");
            throw new IOException("decodeSec2: file creation date/time were not correctly detected");
        }
        dbgLog.fine("fileCreationDate="+fileCreationDate);
        dbgLog.fine("fileCreationTime="+fileCreationTime);
        ///smd.getFileInformation().put("fileCreationDate", fileCreationDate);
        ///smd.getFileInformation().put("fileCreationTime", fileCreationTime);
        ///smd.getFileInformation().put("varFormat_schema", "SPSS");
        dbgLog.fine("decodeSec2(): end");
    }


    private void decodeProductName(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeProductName: reader == null!");
        }

        String productName = parseStringField(reader);
        ///smd.getFileInformation().put("productName", productName);
    }


    private void decodeLicensee(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeLicensee: reader == null!");
        }

        String licenseeName = parseStringField(reader);
        ///smd.getFileInformation().put("licenseeName", licenseeName);
    }


    private void decodeFileLabel(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeFileLabel: reader == null!");
        }

        String fileLabel = parseStringField(reader);     
        // TODO: is this "file label" potentially useful? -- L.A. 4.0 beta
        ///smd.getFileInformation().put("fileLabel", fileLabel);
    }


    private void decodeNumberOfVariables(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeNumberOfVariables: reader == null!");
        }
        
        String temp = null;
        char[] tmp = new char[1];
        StringBuilder sb = new StringBuilder();

        while (reader.read(tmp) > 0) {
            temp = Character.toString(tmp[0]);
            if (temp.equals("/")) {
                break;
            } else {
                sb.append(temp);
            }
        }

        String rawNumberOfVariables = sb.toString();
        int rawLength = rawNumberOfVariables.length();

        String numberOfVariables = StringUtils.stripStart((StringUtils.strip(rawNumberOfVariables)), "0");
        
        if ((numberOfVariables.equals("")) && (numberOfVariables.length() == rawLength)){
            numberOfVariables ="0";
        }

        varQnty = Integer.valueOf(numberOfVariables, 30);
        dataTable.setVarQuantity(Long.valueOf(numberOfVariables, 30));
    }


    private void decodeFieldNo5(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeFieldNo5: reader == null!");
        }    
        
        int field5 = parseNumericField(reader);
    }


    private void decodeWeightVariable(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeWeightVariable: reader == null!");
        }    
        
        String weightVariableName = parseStringField(reader);
        // TODO: make sure case weight variables are properly handled! 
        // -- L.A. 4.0 beta
        ///smd.getFileInformation().put("caseWeightVariableName", weightVariableName);
        ///smd.setCaseWeightVariableName(weightVariableName);
    }


    private void decodeVariableInformation(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeVariableInformation: reader == null!");
        } 

        // step 1: variable type
        int variableType = parseNumericField(reader);
        variableTypelList.add(variableType);
        isCurrentVariableString = (variableType > 0);
            
            
        // step 2: variable name            
        String variableName = parseStringField(reader);
        currentVariableName = variableName;
        variableNameList.add(variableName);
        variableTypeTable.put(variableName,variableType);
           
        // step 3: format(print/write)
        int[] printWriteFormatTable = new int[6];
        for (int i=0; i < 6; i++){
            printWriteFormatTable[i]= parseNumericField(reader);
        }

        int formatCode = printWriteFormatTable[0];
        int formatWidth = printWriteFormatTable[1];
        int formatDecimalPointPosition = printWriteFormatTable[2];

        formatDecimalPointPositionList.add(formatDecimalPointPosition);
        if (!SPSSConstants.FORMAT_CODE_TABLE_POR.containsKey(formatCode)){
                throw new IOException("Unknown format code was found = " + formatCode);
        } else {
            printFormatList.add(printWriteFormatTable[0]);
        }

        if (!SPSSConstants.ORDINARY_FORMAT_CODE_SET.contains(formatCode)){
            StringBuilder sb = new StringBuilder(SPSSConstants.FORMAT_CODE_TABLE_POR.get(formatCode) + formatWidth);
            if (formatDecimalPointPosition > 0){
                sb.append("."+ formatDecimalPointPosition);
            }
            printFormatNameTable.put(variableName, sb.toString());
        }

        printFormatTable.put(variableName, SPSSConstants.FORMAT_CODE_TABLE_POR.get(formatCode));
    }


    private void decodeMissValuePointNumeric(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeMissValuePointNumeric: reader == null!");
        }
        
        if (missingValueCodeTable.containsKey(currentVariableName)){
            missingValueCodeTable.get(currentVariableName).add("8");
        } else {
            List<String> mvc = new ArrayList<String>();
            mvc.add("8");
            missingValueCodeTable.put(currentVariableName, mvc);
        }

        String missingValuePoint=null;

        // missing values are not always integers
        String base30value = getNumericFieldAsRawString(reader);
        if (base30value.indexOf(".")>=0){
            missingValuePoint = doubleNumberFormatter.format(base30Tobase10Conversion(base30value));
        } else {
            missingValuePoint= Integer.valueOf(base30value, 30).toString();
        }

        if (missingValueTable.containsKey(currentVariableName)){
            // already stored
            (missingValueTable.get(currentVariableName)).add(missingValuePoint);
        } else {
            // no missing value stored
            List<String> mv = new ArrayList<String>();
            mv.add(missingValuePoint);
            missingValueTable.put(currentVariableName, mv);
        }
    }


    private void decodeMissValuePointString(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeMissValuePointString: reader == null!");
        }    
        
        if (missingValueCodeTable.containsKey(currentVariableName)){
            missingValueCodeTable.get(currentVariableName).add("8");
        } else {
            List<String> mvc = new ArrayList<String>();
            mvc.add("8");
            missingValueCodeTable.put(currentVariableName, mvc);
        }
        
        String missingValuePointString  = parseStringField(reader);
        
        if (missingValueTable.containsKey(currentVariableName)){
            // already stored
            (missingValueTable.get(currentVariableName)).add(missingValuePointString);
        } else {
            // no missing value stored
            List<String> mv = new ArrayList<String>();
            mv.add(missingValuePointString);
            missingValueTable.put(currentVariableName, mv);
        }
    }


    private void decodeMissValueRangeLow(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeMissValueRangeLow: reader == null!");
        }
        
        if (missingValueCodeTable.containsKey(currentVariableName)){
            missingValueCodeTable.get(currentVariableName).add("9");
        } else {
            List<String> mvc = new ArrayList<String>();
            mvc.add("9");
            missingValueCodeTable.put(currentVariableName, mvc);
        }

        String missingValueRangeLOtype=null;

        // missing values are not always integers
        String base30value = getNumericFieldAsRawString(reader);

        if (base30value.indexOf(".")>=0){
            missingValueRangeLOtype = doubleNumberFormatter.format(base30Tobase10Conversion(base30value));
        } else {
            missingValueRangeLOtype= Integer.valueOf(base30value, 30).toString();
        }
        
        if (missingValueTable.containsKey(currentVariableName)){
            // already stored
            (missingValueTable.get(currentVariableName)).add("LOWEST");
            (missingValueTable.get(currentVariableName)).add(missingValueRangeLOtype);
        } else {
            // no missing value stored
            List<String> mv = new ArrayList<String>();
            mv.add("LOWEST");
            mv.add(missingValueRangeLOtype);
            missingValueTable.put(currentVariableName, mv);
        }
    }


    private void decodeMissValueRangeHigh(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeMissValueRangeHigh: reader == null!");
        }
        
        if (missingValueCodeTable.containsKey(currentVariableName)){
            missingValueCodeTable.get(currentVariableName).add("A");
        } else {
            List<String> mvc = new ArrayList<String>();
            mvc.add("A");
            missingValueCodeTable.put(currentVariableName, mvc);
        }

        String missingValueRangeHItype = null;

        // missing values are not always integers
        String base30value = getNumericFieldAsRawString(reader);

        if (base30value.indexOf(".")>=0){
            missingValueRangeHItype = doubleNumberFormatter.format(base30Tobase10Conversion(base30value));
        } else {
            missingValueRangeHItype= Integer.valueOf(base30value, 30).toString();
        }

        if (missingValueTable.containsKey(currentVariableName)){
            // already stored
            (missingValueTable.get(currentVariableName)).add(missingValueRangeHItype);
            (missingValueTable.get(currentVariableName)).add("HIGHEST");
        } else {
            // no missing value stored
           List<String> mv = new ArrayList<String>();
           mv.add(missingValueRangeHItype);
           mv.add("HIGHEST");
           missingValueTable.put(currentVariableName, mv);
        }
    }
    
    
    private void decodeMissValueRange(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeMissValueRange: reader == null!");
        }

        if (missingValueCodeTable.containsKey(currentVariableName)){
            missingValueCodeTable.get(currentVariableName).add("B");
        } else {
            List<String> mvc = new ArrayList<String>();
            mvc.add("B");
            missingValueCodeTable.put(currentVariableName, mvc);
        }
        
        String[] missingValueRange = new String[2];

       // missing values are not always integers
        String base30value0 = getNumericFieldAsRawString(reader);

        if (base30value0.indexOf(".")>=0){
            missingValueRange[0] = doubleNumberFormatter.format(base30Tobase10Conversion(base30value0));
        } else {
            missingValueRange[0]= Integer.valueOf(base30value0, 30).toString();
        }

        String base30value1 = getNumericFieldAsRawString(reader);

        if (base30value1.indexOf(".")>=0){
            missingValueRange[1] = doubleNumberFormatter.format(base30Tobase10Conversion(base30value1));
        } else {
            missingValueRange[1]= Integer.valueOf(base30value1, 30).toString();
        }

        if (missingValueTable.containsKey(currentVariableName)){
            // already stored
            (missingValueTable.get(currentVariableName)).add(missingValueRange[0]);
            (missingValueTable.get(currentVariableName)).add(missingValueRange[1]);
        } else {
            // no missing value stored
           List<String> mv = new ArrayList<String>();
           mv.add(missingValueRange[0]);
           mv.add(missingValueRange[1]);
           missingValueTable.put(currentVariableName, mv);
        }
    }
    

    private void decodeVariableLabel(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeVariableLabel: reader == null!");
        }    

        String variableLabel = parseStringField(reader);
        variableLabelMap.put(currentVariableName, variableLabel);
        // note: not all variables have their variable label; therefore,
        // saving them to the metatadata object is done within read() method

    }
    
    
    private void decodeValueLabel(BufferedReader reader) throws IOException {
        Map<String, String> valueLabelSet = new LinkedHashMap<String, String>();
        
        int numberOfVariables = parseNumericField(reader);
        String[] variableNames = new String[numberOfVariables];

        for (int i= 0; i< numberOfVariables; i++){
            variableNames[i] = parseStringField(reader);
        }

        int numberOfvalueLabelSets = parseNumericField(reader);
        boolean isStringType = variableTypeTable.get(variableNames[0]) > 0 ? true : false;

        for (int i=0; i<numberOfvalueLabelSets ;i++){
            String[] tempValueLabel = new String[2];
            if (isStringType){
                // String case
                tempValueLabel[0] = parseStringField(reader);
            } else {
                // Numeric case
                // values may not be always integers
                String base30value = getNumericFieldAsRawString(reader);

                Matcher matcher = pattern4Integer.matcher(base30value);

                if (matcher.matches()) {
                    // integer case
                    tempValueLabel[0] = Long.valueOf(base30value, 30).toString();
                } else {
                    // double case
                    tempValueLabel[0] = doubleNumberFormatter.format(base30Tobase10Conversion(base30value));
                }
            }


            tempValueLabel[1] = parseStringField(reader);
            valueLabelSet.put(tempValueLabel[0],tempValueLabel[1]);
        }
        // save the value-label mapping list
        // use the first variable name as the key
        valueLabelTable.put(variableNames[0], valueLabelSet);

        // create a mapping table that finds the key variable for this mapping table
        for (String vn : variableNames){
            valueVariableMappingTable.put(vn, variableNames[0]);
        }
    }


    private void decodeDocument(BufferedReader reader) throws IOException {
        if (reader ==null){
            throw new IllegalArgumentException("decodeVariableLabel: reader == null!");
        }    
        
        int noOfdocumentLines = parseNumericField(reader);
        String[] document = new String[noOfdocumentLines];

        for (int i= 0; i< noOfdocumentLines; i++){
            document[i] = parseStringField(reader);
        }

        // TODO: 
        // verify if this "document" is any useful potentially. 
        // -- L.A. 4.0 beta
        ///smd.getFileInformation().put("document", StringUtils.join(document," " ));
    }


    private void decodeData(BufferedReader reader) throws IOException {
        dbgLog.fine("decodeData(): start");
        // TODO: get rid of this "variableTypeFinal"; -- L.A. 4.0 beta
        int[] variableTypeFinal= new int[varQnty];
        dateFormatList = new String[varQnty];

        // create a File object to save the tab-delimited data file
        File tabDelimitedDataFile = File.createTempFile("tempTabfile.", ".tab");
        ingesteddata.setTabDelimitedFile(tabDelimitedDataFile);
        

        FileOutputStream fileOutTab = null;
        PrintWriter pwout = null;

        try {
            fileOutTab = new FileOutputStream(tabDelimitedDataFile);
            pwout = new PrintWriter(new OutputStreamWriter(fileOutTab, "utf8"), true);

            variableFormatTypeList = new String[varQnty];
            for (int i = 0; i < varQnty; i++) {
                variableFormatTypeList[i] = SPSSConstants.FORMAT_CATEGORY_TABLE.get(printFormatTable.get(variableNameList.get(i)));
                formatCategoryTable.put(variableNameList.get(i), variableFormatTypeList[i]);
            }

            // contents (variable) checker concering decimals
            Arrays.fill(variableTypeFinal, 0);

            // raw-case counter
            int j = 0; // case

            // use while instead for because the number of cases (observations) is usually unknown
            FBLOCK: while(true){
                j++;

                // case(row)-wise storage object; to be updated after each row-reading

                String[] casewiseRecordForTabFile = new String[varQnty];
                // warning: the above object is later shallow-copied to the
                // data object for calculating a UNF value/summary statistics
                //

                for (int i=0; i<varQnty; i++){
                    // check the type of this variable
                    boolean isStringType = variableTypeTable.get(variableNameList.get(i)) > 0 ? true : false;

                    if (isStringType){
                        // String case
                        variableTypeFinal[i]=-1;

                        StringBuilder sb_StringLengthBase30 = new StringBuilder("");
                        int stringLengthBase10 = 0;
                        String buffer = "";
                        char[] tmp = new char[1];

                        int nint;
                        while((nint = reader.read(tmp))>0){
                            buffer =  Character.toString(tmp[0]);
                            if (buffer.equals("/")){
                                break;
                            } else if (buffer.equals("Z")){
                                if (i == 0){
                                    // the reader has passed the last case; subtract 1 from the j counter
                                    caseQnty = j-1;
                                    break FBLOCK;
                                }
                            } else {
                                sb_StringLengthBase30.append(buffer);
                            }


                        }

                        if (nint == 0){
                            // no more data to be read (reached the eof)
                            caseQnty = j - 1;
                            break FBLOCK;
                        }


                        dbgLog.finer(j+"-th case "+i+"=th var:datum length=" +sb_StringLengthBase30.toString());

                        // this length value should be a positive integer
                        Matcher mtr = pattern4positiveInteger.matcher(sb_StringLengthBase30.toString());
                        if (mtr.matches()){
                            stringLengthBase10 = Integer.valueOf(sb_StringLengthBase30.toString(), 30);
                        } else{
                            // reading error case
                            throw new IOException("reading F(data) section: string: length is not integer");
                        }

                        // read this string-variable's contents after "/"
                        char[] char_datumString = new char[stringLengthBase10];
                        reader.read(char_datumString);

                        String datum = new String(char_datumString);
                        casewiseRecordForTabFile[i] =  "\"" + datum.replaceAll("\"",Matcher.quoteReplacement("\\\"")) + "\"";
                        // end of string case
                    } else {

                        // numeric case
                        StringBuilder sb_datumNumericBase30 = new StringBuilder("");
                        boolean isMissingValue = false;
                        String datum = null;
                        String datumForTabFile = null;
                        String datumDateFormat = null;

                        String buffer = "";
                        char[] tmp = new char[1];
                        int nint;
                        while((nint = reader.read(tmp))>0){
                            sb_datumNumericBase30.append(buffer);
                            buffer = Character.toString(tmp[0]);

                            if (buffer.equals("/")){
                                break;
                            } else if (buffer.equals("Z")){
                                if (i == 0){
                                    // the reader has passed the last case
                                    // subtract 1 from the j counter
                                    dbgLog.fine("Z-mark was detected");
                                    caseQnty = j-1;
                                    break FBLOCK;
                                }
                            } else if (buffer.equals("*")) {
                                // '*' is the first character of the system missing value
                                datumForTabFile = MissingValueForTextDataFile;
                                datum = null;
                                isMissingValue = true;

                               // read next char '.' as part of the missing value
                                reader.read(tmp);
                                buffer = Character.toString(tmp[0]);
                                break;
                            }

                        }
                        if (nint == 0){
                            // no more data to be read; reached the eof
                            caseQnty = j - 1;
                            break FBLOCK;
                        }

                        // follow-up process for non-missing-values
                        if (!isMissingValue) {
                            // decode a numeric datum as String
                            String datumNumericBase30 = sb_datumNumericBase30.toString();
                            Matcher matcher = pattern4Integer.matcher(datumNumericBase30);

                            if (matcher.matches()){
                                // integer case
                                datum = Long.valueOf(datumNumericBase30, 30).toString();
                            } else {
                                // double case
                                datum = doubleNumberFormatter.format(base30Tobase10Conversion(datumNumericBase30));
                            }

                            // now check format (if date or time)
                            String variableFormatType = variableFormatTypeList[i];

                            if (variableFormatType.equals("date")){
                                variableTypeFinal[i]=-1;
                                long dateDatum = Long.parseLong(datum)*1000L- SPSS_DATE_OFFSET;
                                datum = sdf_ymd.format(new Date(dateDatum));
                                datumDateFormat = sdf_ymd.toPattern();

                            } else if (variableFormatType.equals("time")) {
                                variableTypeFinal[i]=-1;
                                int formatDecimalPointPosition = formatDecimalPointPositionList.get(i);

                                if (printFormatTable.get(variableNameList.get(i)).equals("DTIME")){

                                    if (datum.indexOf(".") < 0){
                                        long dateDatum  = Long.parseLong(datum)*1000L - SPSS_DATE_BIAS;
                                        datum = sdf_dhms.format(new Date(dateDatum));
                                        // don't save date format for dtime
                                    } else {
                                        // decimal point included
                                        String[] timeData = datum.split("\\.");
                                        long dateDatum = Long.parseLong(timeData[0])*1000L - SPSS_DATE_BIAS;
                                        StringBuilder sb_time = new StringBuilder(sdf_dhms.format(new Date(dateDatum)));

                                        if (formatDecimalPointPosition > 0){
                                            sb_time.append("."+timeData[1].substring(0,formatDecimalPointPosition));
                                        }

                                        datum = sb_time.toString();
                                        // DTIME is weird date/time format that no one uses outside of 
                                        // SPSS; so we are not even going to bother trying to save
                                        // this variable as a datetime. 
                                    }

                                } else if (printFormatTable.get(variableNameList.get(i)).equals("DATETIME")){
                                    // TODO: 
                                    // (for both datetime and "dateless" time)
                                    // keep the longest of the matching formats - i.e., if there are *some*
                                    // values in the vector that have thousands of a second, that should be 
                                    // part of the saved format!
                                    //  -- L.A. Aug. 12 2014 

                                    if (datum.indexOf(".") < 0){
                                        long dateDatum  = Long.parseLong(datum)*1000L - SPSS_DATE_OFFSET;
                                        datum = sdf_ymdhms.format(new Date(dateDatum));
                                        datumDateFormat = sdf_ymdhms.toPattern();
                                    } else {
                                        // decimal point included
                                        String[] timeData = datum.split("\\.");
                                        long dateDatum = Long.parseLong(timeData[0])*1000L- SPSS_DATE_OFFSET;
                                        StringBuilder sb_time = new StringBuilder(sdf_ymdhms.format(new Date(dateDatum)));

                                        if (formatDecimalPointPosition > 0){
                                            sb_time.append("."+timeData[1].substring(0,formatDecimalPointPosition));
                                        }

                                        datum = sb_time.toString();
                                        datumDateFormat = sdf_ymdhms.toPattern() + (formatDecimalPointPosition > 0 ? ".S" : "" );
                                    }

                                } else if (printFormatTable.get(variableNameList.get(i)).equals("TIME")){

                                    if (datum.indexOf(".") < 0){
                                        long dateDatum = Long.parseLong(datum)*1000L;
                                        datum = sdf_hms.format(new Date(dateDatum));
                                        datumDateFormat = sdf_hms.toPattern();
                                    } else {
                                        // decimal point included
                                        String[] timeData = datum.split("\\.");
                                        long dateDatum = Long.parseLong(timeData[0])*1000L;
                                        StringBuilder sb_time = new StringBuilder(sdf_hms.format(new Date(dateDatum)));

                                        if (formatDecimalPointPosition > 0){
                                            sb_time.append("."+timeData[1].substring(0,formatDecimalPointPosition));
                                        }

                                        datum = sb_time.toString();
                                        datumDateFormat = sdf_hms.toPattern() + (formatDecimalPointPosition > 0 ? ".S" : "" );
                                    }
                                }

                            } else if (variableFormatType.equals("other")){

                                if (printFormatTable.get(variableNameList.get(i)).equals("WKDAY")){
                                    // day of week
                                    variableTypeFinal[i]=-1;
                                    datum = SPSSConstants.WEEKDAY_LIST.get(Integer.valueOf(datum)-1);

                                } else if (printFormatTable.get(variableNameList.get(i)).equals("MONTH")){
                                    // month
                                    variableTypeFinal[i]=-1;
                                    datum = SPSSConstants.MONTH_LIST.get(Integer.valueOf(datum)-1);
                                }
                            }

                            // since value is not missing, set both values to be the same
                            datumForTabFile = datum;

                            // decimal-point check (variable is integer or not)
                            if (variableTypeFinal[i]==0){
                                if (datum.indexOf(".") >=0){
                                    variableTypeFinal[i] = 1;
                                    decimalVariableSet.add(i);
                                }
                            }
                        }

                        if (datumDateFormat != null) {
                            dateFormatList[i] = datumDateFormat;
                        }
                        casewiseRecordForTabFile[i]= datumForTabFile;

                    } // end: if: string vs numeric variable

                } // end:for-loop-i (variable-wise loop)


                // print the i-th case; use casewiseRecord to dump the current case to the tab-delimited file
                pwout.println(StringUtils.join(casewiseRecordForTabFile, "\t"));

            } // end: while-block
        } finally {
            // close the print writer
            if (pwout != null) {
                pwout.close();
            }
        }

        ///smd.setDecimalVariables(decimalVariableSet);
        dataTable.setCaseQuantity(new Long(caseQnty));

        dbgLog.fine("decodeData(): end");
    }
    
    
    private void processMissingValueData(){
        /*

         POR's missing-value storage differs form the counterpart of SAV;
         this method transforms the POR-native storage to the SAV-type
         after this process, missingValueTable contains point-type
         missing values for later catStat/sumStat processing;
         range and mixed type cases are stored in invalidDataTable

         missingValueCodeTable=
            {VAR1=[9], VAR2=[A], VAR3=[9, 8], VAR4=[A, 8],
             VAR5=[8, 8, 8], VAR6=[B], VAR7=[B, 8]}

         missingValueTable=
            {VAR1=[-1], VAR2=[-1], VAR3=[-2, -1], VAR4=[-1, -2],
             VAR5=[-1, -2, -3], VAR6=[-2, -1], VAR7=[-3, -2, -1]}


         missingValueTable={VAR1=[], VAR2=[], VAR3=[-1], VAR4=[-2],
             VAR5=[-1, -2, -3], VAR6=[], VAR7=[-2]}

         */

        dbgLog.fine("missingValueCodeTable="+missingValueCodeTable);
        Set<Map.Entry<String,List<String>>> msvlc = missingValueCodeTable.entrySet();
        for (Iterator<Map.Entry<String,List<String>>> itc = msvlc.iterator(); itc.hasNext();){
            Map.Entry<String, List<String>> et = itc.next();
            String variable = et.getKey();
            dbgLog.fine("variable="+variable);
            List<String> codeList = et.getValue();
            List<String> valueList = missingValueTable.get(variable);
            dbgLog.fine("codeList="+codeList);
            dbgLog.fine("valueList="+valueList);
            int type;
            InvalidData invalidDataInfo = null;
            if (valueList.size() == 3){
                if (codeList.get(0).equals("8") && codeList.get(1).equals("8") &&
                        codeList.get(2).equals("8") ){
                    type = 3;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidValues(valueList);
                } else if (codeList.get(0).equals("9") && codeList.get(1).equals("8")){
                    type = -3;

                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidValues(valueList.subList(2, 3));
                    invalidDataInfo.setInvalidRange(valueList.subList(0, 2));

                } else if (codeList.get(0).equals("A") && codeList.get(1).equals("8")){
                    type = -3;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidValues(valueList.subList(2, 3));
                    invalidDataInfo.setInvalidRange(valueList.subList(0, 2));
                } else if (codeList.get(0).equals("B") && codeList.get(1).equals("8")){
                    type = -3;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidValues(valueList.subList(2, 3));
                    invalidDataInfo.setInvalidRange(valueList.subList(0, 2));
                } else {
                   dbgLog.severe("unkown missing-value combination(3 values)");
                }
                
            } else if (valueList.size() == 2){
                if (codeList.get(0).equals("8") && codeList.get(1).equals("8")){
                    type = 2;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidValues(valueList);

                } else if (codeList.get(0).equals("9")){
                    type = -2;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidRange(valueList.subList(0, 2));

                } else if (codeList.get(0).equals("A")){
                    type = -2;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidRange(valueList.subList(0, 2));
                } else if (codeList.get(0).equals("B")){
                    type = -2;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidRange(valueList.subList(0, 2));

                } else {
                    dbgLog.severe("unknown missing value combination(2 values)");
                }
            } else if (valueList.size() == 1){
                if (codeList.get(0).equals("8")){
                    type = 1;
                    invalidDataInfo = new InvalidData(type);
                    invalidDataInfo.setInvalidValues(valueList);
                } else {
                    dbgLog.severe("unknown missing value combination(2 values)");
                }
            }
            invalidDataTable.put(variable, invalidDataInfo);
        }

        dbgLog.fine("invalidDataTable="+invalidDataTable);


        Set<Map.Entry<String,List<String>>> msvl = missingValueTable.entrySet();
        for (Iterator<Map.Entry<String,List<String>>> it = msvl.iterator(); it.hasNext();){
            Map.Entry<String, List<String>> et = it.next();

            String variable = et.getKey();
            List<String> valueList = et.getValue();

            List<String> codeList = missingValueCodeTable.get(variable);

            dbgLog.finer("var="+variable+"\tvalue="+valueList+"\t code"+ codeList);
            List<String> temp = new ArrayList<String>();
            for (int j=0; j<codeList.size(); j++){
                if (codeList.get(j).equals("8")){
                  temp.add(valueList.get(j));
                }
            }
            missingValueTable.put(variable, temp);
        }
        dbgLog.fine("missingValueTable="+missingValueTable);
    }
    
    
    
    // utility methods  -----------------------------------------------------//
    
    private int parseNumericField(BufferedReader reader) throws IOException{
        String temp = null;
        char[] tmp = new char[1];
        StringBuilder sb = new StringBuilder();
        while(reader.read(tmp) > 0 ){
            temp = Character.toString(tmp[0]);//new String(tmp);
            if (temp.equals("/")){
                break;
            } else {
                sb.append(temp);
            }
            //temp = sb.toString();//new String(tmp);
        }
        String base30numberString = sb.toString();
        dbgLog.finer("base30numberString="+base30numberString);
        int base10equivalent = Integer.valueOf(base30numberString, 30);
        dbgLog.finer("base10equivalent="+base10equivalent);
        return base10equivalent;
    }


    private String parseStringField(BufferedReader reader) throws IOException{
        String temp = null;
        char[] tmp = new char[1];
        StringBuilder sb = new StringBuilder();
        while(reader.read(tmp) > 0 ){
            temp = Character.toString(tmp[0]);//new String(tmp);
            if (temp.equals("/")){
                break;
            } else {
                sb.append(temp);
            }
            //temp = sb.toString();//new String(tmp);
        }
        String base30numberString = sb.toString();
        //dbgLog.fine("base30numberString="+base30numberString);
        int base10equivalent = Integer.valueOf(base30numberString, 30);
        //dbgLog.fine("base10equivalent="+base10equivalent);
        char[] stringBody = new char[base10equivalent];
        reader.read(stringBody);
        String stringData = new String(stringBody);
        dbgLog.finer("stringData="+stringData);
        return stringData;
    }



    private String getNumericFieldAsRawString(BufferedReader reader) throws IOException{
        String temp = null;
        char[] tmp = new char[1];
        StringBuilder sb = new StringBuilder();
        while(reader.read(tmp) > 0 ){
            temp = Character.toString(tmp[0]);//new String(tmp);
            if (temp.equals("/")){
                break;
            } else {
                sb.append(temp);
            }
            //temp = sb.toString();//new String(tmp);
        }
        String base30numberString = sb.toString();
        dbgLog.finer("base30numberString="+base30numberString);

        return base30numberString;
    }


    private double base30Tobase10Conversion(String base30String){

        // new base(radix) number
        int oldBase = 30;
        //dbgLog.fine("base30String="+base30String);

        // trim white-spaces from the both ends
        String base30StringClean = StringUtils.trim(base30String);
        //dbgLog.fine("base30StringClean="+base30StringClean);

        // check the negative/positive sign
        boolean isNegativeNumber = false;
        boolean hasPositiveSign = false;
        if (base30StringClean.startsWith("-")){
            isNegativeNumber = true;
        }

        if (base30StringClean.startsWith("+")){
            hasPositiveSign = true;
        }

        // remove the sign if exits
        String base30StringNoSign = null;

        if ((isNegativeNumber) ||(hasPositiveSign)){
            base30StringNoSign = base30StringClean.substring(1);
        } else {
            base30StringNoSign = new String(base30StringClean);
        }

        // check the scientific notation
        // if so, divide it into the significand and exponent
        String significand  = null;
        long exponent = 0;

        int plusIndex = base30StringNoSign.indexOf("+");
        int minusIndex = base30StringNoSign.indexOf("-");

        if (plusIndex> 0){
            significand = base30StringNoSign.substring(0, plusIndex);
            exponent = Long.valueOf( base30StringNoSign.substring(plusIndex+1), oldBase );

        } else if (minusIndex > 0){
            significand = base30StringNoSign.substring(0, minusIndex);
            exponent = -1 * Long.valueOf( base30StringNoSign.substring(minusIndex+1), oldBase );

        } else {
            significand = new String(base30StringNoSign);
        }


        // "move" decimal point; for each shift right, subtract one from exponent; end result is a string with no decimal
        int decimalIndex = significand.indexOf(".");
        if (decimalIndex != -1) {
            exponent -= (significand.length() - (decimalIndex + 1) );
            significand = significand.substring(0, decimalIndex) + significand.substring( decimalIndex + 1 );
        }

        // TODO: Verify that the MathContext/Rounding methods are OK:
        // -- L.A. 4.0 beta
        MathContext mc = new MathContext(15,RoundingMode.HALF_UP);
        long base10Significand = Long.parseLong(significand, oldBase);
        BigDecimal base10value = new BigDecimal( String.valueOf(base10Significand), mc );
        BigDecimal exponentialComponent = new BigDecimal("1", mc);

        for (int g=0; g < Math.abs(exponent); g++) {
            exponentialComponent = exponentialComponent.multiply(new BigDecimal("30", mc));
        }

        if (exponent >= 0) {
            base10value = base10value.multiply(exponentialComponent, mc);
        } else {
            base10value = base10value.divide(exponentialComponent, mc);
        }

        // negative sign if applicable
        if (isNegativeNumber){
            base10value = base10value.multiply(new BigDecimal("-1", mc));
        }

        return base10value.doubleValue();
    }
    
    void assignValueLabels(Map<String, Map<String, String>> valueLabelTable) {
        // Let's go through all the categorical value label mappings and 
        // assign them to the correct variables: 
        
        for (int i = 0; i < dataTable.getVarQuantity().intValue(); i++) {
            
            String varName = dataTable.getDataVariables().get(i).getName();
            
            Map<String, String> valueLabelPairs = valueLabelTable.get(varName);
            if (valueLabelPairs != null && !valueLabelPairs.isEmpty()) {
                for (String value : valueLabelPairs.keySet()) {
                    
                    VariableCategory cat = new VariableCategory();
                    cat.setValue(value);
                    cat.setLabel(valueLabelPairs.get(value));

                    /* cross-link the variable and category to each other: */
                    cat.setDataVariable(dataTable.getDataVariables().get(i));
                    dataTable.getDataVariables().get(i).getCategories().add(cat);
                }
            }
        }
    }
    
    private void print2Darray(Object[][] datatable, String title){
        dbgLog.fine(title);
        for (int i=0; i< datatable.length; i++){
            dbgLog.fine(StringUtils.join(datatable[i], "|"));
        }
    }    

    
}

