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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv;

import java.io.FileReader;
import java.io.InputStreamReader;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * Dataverse 4.0 implementation of <code>TabularDataFileReader</code> for the
 * plain CSV file with a variable name header.
 *
 *
 * @author Oscar Smith
 *
 * This implementation uses the Apache CSV Parser
 */
public class CSVFileReader extends TabularDataFileReader {

    private static final Logger logger = Logger.getLogger(CSVFileReader.class.getPackage().getName());
    private static final int DIGITS_OF_PRECISION_DOUBLE = 15;
    private static final String FORMAT_IEEE754 = "%+#." + DIGITS_OF_PRECISION_DOUBLE + "e";
    private MathContext doubleMathContext;
    private CSVFormat inFormat;
    //private final Set<Character> firstNumCharSet = new HashSet<>();

    // DATE FORMATS
    private static SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[]{
        new SimpleDateFormat("yyyy-MM-dd"), //new SimpleDateFormat("yyyy/MM/dd"),
    //new SimpleDateFormat("MM/dd/yyyy"),
    //new SimpleDateFormat("MM-dd-yyyy"),
    };

    // TIME FORMATS
    private static SimpleDateFormat[] TIME_FORMATS = new SimpleDateFormat[]{
        // Date-time up to seconds with timezone, e.g. 2013-04-08 13:14:23 -0500
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),
        // Date-time up to seconds and no timezone, e.g. 2013-04-08 13:14:23
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    };

    public CSVFileReader(TabularDataFileReaderSpi originator, char delim) {
        super(originator);
        if (delim == ','){
            inFormat = CSVFormat.EXCEL;
        } else if (delim == '\t'){
            inFormat = CSVFormat.TDF;
        }
    }

    private void init() throws IOException {
        doubleMathContext = new MathContext(DIGITS_OF_PRECISION_DOUBLE, RoundingMode.HALF_EVEN);
        //firstNumCharSet.addAll(Arrays.asList(new Character[]{'+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}));
    }

    /**
     * Reads a CSV file, converts it into a dataverse DataTable.
     *
     * @param stream a <code>BufferedInputStream</code>.
     * @return an <code>TabularDataIngest</code> object
     * @throws java.io.IOException if a reading error occurs.
     */
    @Override
    public TabularDataIngest read(BufferedInputStream stream, boolean saveWithVariableHeader, File dataFile) throws IOException {
        init();

        if (stream == null) {
            throw new IOException(BundleUtil.getStringFromBundle("ingest.csv.nullStream"));
        }
        TabularDataIngest ingesteddata = new TabularDataIngest();
        DataTable dataTable = new DataTable();

        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(stream));

        File tabFileDestination = File.createTempFile("data-", ".tab");
        PrintWriter tabFileWriter = new PrintWriter(tabFileDestination.getAbsolutePath());

        int lineCount = readFile(localBufferedReader, dataTable, saveWithVariableHeader, tabFileWriter);

        logger.fine("Tab file produced: " + tabFileDestination.getAbsolutePath());

        dataTable.setUnf("UNF:6:NOTCALCULATED");

        ingesteddata.setTabDelimitedFile(tabFileDestination);
        ingesteddata.setDataTable(dataTable);
        return ingesteddata;

    }

    public int readFile(BufferedReader csvReader, DataTable dataTable, boolean saveWithVariableHeader, PrintWriter finalOut) throws IOException {

        List<DataVariable> variableList = new ArrayList<>();
        CSVParser parser = new CSVParser(csvReader, inFormat.withHeader());
        Map<String, Integer> headers = parser.getHeaderMap();

        int i = 0;
        String variableNameHeader = null;
        
        for (String varName : headers.keySet()) {
            // @todo: is .keySet() guaranteed to return the names in the right order?
            if (varName == null || varName.isEmpty()) {
                // TODO:
                // Add a sensible variable name validation algorithm.
                // -- L.A. 4.0 alpha 1
                throw new IOException(BundleUtil.getStringFromBundle("ingest.csv.invalidHeader"));
            }

            DataVariable dv = new DataVariable(i, dataTable);
            dv.setName(varName);
            dv.setLabel(varName);
            variableList.add(dv);

            dv.setTypeCharacter();
            dv.setIntervalDiscrete();
            
            if (saveWithVariableHeader) {
                    variableNameHeader = variableNameHeader == null
                            ? varName 
                            : variableNameHeader.concat("\t" + varName);
                }
            
            i++;
        }

        dataTable.setVarQuantity((long) variableList.size());
        dataTable.setDataVariables(variableList);

        boolean[] isNumericVariable = new boolean[headers.size()];
        boolean[] isIntegerVariable = new boolean[headers.size()];
        boolean[] isTimeVariable = new boolean[headers.size()];
        boolean[] isDateVariable = new boolean[headers.size()];

        for (i = 0; i < headers.size(); i++) {
            // OK, let's assume that every variable is numeric;
            // but we'll go through the file and examine every value; the
            // moment we find a value that's not a legit numeric one, we'll
            // assume that it is in fact a String.
            isNumericVariable[i] = true;
            isIntegerVariable[i] = true;
            isDateVariable[i] = true;
            isTimeVariable[i] = true;
        }

        // First, "learning" pass.
        // (we'll save the incoming stream in another temp file:)
        SimpleDateFormat[] selectedDateTimeFormat = new SimpleDateFormat[headers.size()];
        SimpleDateFormat[] selectedDateFormat = new SimpleDateFormat[headers.size()];

        File firstPassTempFile = File.createTempFile("firstpass-", ".csv");

        try (CSVPrinter csvFilePrinter = new CSVPrinter(
                // TODO allow other parsers of tabular data to use this parser by changin inFormat
                new FileWriter(firstPassTempFile.getAbsolutePath()), inFormat)) {
            //Write  headers
            csvFilePrinter.printRecord(headers.keySet());
            for (CSVRecord record : parser.getRecords()) {
                // Checks if #records = #columns in header
                if (!record.isConsistent()) {
                    List<String> args = Arrays.asList(new String[]{"" + (parser.getCurrentLineNumber() - 1),
                                                                   "" + headers.size(),
                                                                   "" + record.size()});
                    throw new IOException(BundleUtil.getStringFromBundle("ingest.csv.recordMismatch", args));
                }

                for (i = 0; i < headers.size(); i++) {
                    String varString = record.get(i);
                    isIntegerVariable[i] = isIntegerVariable[i]
                                           && varString != null
                                           && (varString.isEmpty()
                                               || varString.equals("null")
                                               || (StringUtils.isNumeric(varString)
                                                    || (varString.substring(0,1).matches("[+-]") 
                                                        && StringUtils.isNumeric(varString.substring(1)))));
                    if (isNumericVariable[i]) {
                        // If variable might be "numeric" test to see if this value is a parsable number:
                        if (varString != null && !varString.isEmpty()) {

                            boolean isNumeric = false;
                            boolean isInteger = false;

                            if (varString.equalsIgnoreCase("NaN")
                                || varString.equalsIgnoreCase("NA")
                                || varString.equalsIgnoreCase("Inf")
                                || varString.equalsIgnoreCase("+Inf")
                                || varString.equalsIgnoreCase("-Inf")
                                || varString.equalsIgnoreCase("null")) {
                                continue;
                            } else {
                                try {
                                    Double testDoubleValue = new Double(varString);
                                    continue;
                                } catch (NumberFormatException ex) {
                                    // the token failed to parse as a double
                                    // so the column is a string variable.
                                }
                            }
                            isNumericVariable[i] = false;
                        }
                    }

                    // If this is not a numeric column, see if it is a date collumn
                    // by parsing the cell as a date or date-time value:
                    if (!isNumericVariable[i]) {

                        Date dateResult = null;

                        if (isTimeVariable[i]) {
                            if (varString != null && !varString.isEmpty()) {
                                boolean isTime = false;

                                if (selectedDateTimeFormat[i] != null) {
                                    ParsePosition pos = new ParsePosition(0);
                                    dateResult = selectedDateTimeFormat[i].parse(varString, pos);

                                    if (dateResult != null && pos.getIndex() == varString.length()) {
                                        // OK, successfully parsed a value!
                                        isTime = true;
                                    }
                                } else {
                                    for (SimpleDateFormat format : TIME_FORMATS) {
                                        ParsePosition pos = new ParsePosition(0);
                                        dateResult = format.parse(varString, pos);
                                        if (dateResult != null && pos.getIndex() == varString.length()) {
                                            // OK, successfully parsed a value!
                                            isTime = true;
                                            selectedDateTimeFormat[i] = format;
                                            break;
                                        }
                                    }
                                }
                                if (!isTime) {
                                    isTimeVariable[i] = false;
                                    // if the token didn't parse as a time value,
                                    // we will still try to parse it as a date, below.
                                    // unless this column is NOT a date.
                                } else {
                                    // And if it is a time value, we are going to assume it's
                                    // NOT a date.
                                    isDateVariable[i] = false;
                                }
                            }
                        }

                        if (isDateVariable[i]) {
                            if (varString != null && !varString.isEmpty()) {
                                boolean isDate = false;

                                // TODO:
                                // Strictly speaking, we should be doing the same thing
                                // here as with the time formats above; select the
                                // first one that works, then insist that all the
                                // other values in this column match it... but we
                                // only have one, as of now, so it should be ok.
                                // -- L.A. 4.0 beta
                                for (SimpleDateFormat format : DATE_FORMATS) {
                                    // Strict parsing - it will throw an
                                    // exception if it doesn't parse!
                                    format.setLenient(false);
                                    try {
                                        format.parse(varString);
                                        isDate = true;
                                        selectedDateFormat[i] = format;
                                        break;
                                    } catch (ParseException ex) {
                                        //Do nothing
                                    }
                                }
                                isDateVariable[i] = isDate;
                            }
                        }
                    }
                }

                csvFilePrinter.printRecord(record);
            }
        }
        dataTable.setCaseQuantity(parser.getRecordNumber());
        parser.close();
        csvReader.close();

        // Re-type the variables that we've determined are numerics:
        for (i = 0; i < headers.size(); i++) {
            if (isNumericVariable[i]) {
                dataTable.getDataVariables().get(i).setTypeNumeric();

                if (isIntegerVariable[i]) {
                    dataTable.getDataVariables().get(i).setIntervalDiscrete();
                } else {
                    dataTable.getDataVariables().get(i).setIntervalContinuous();
                }
            } else if (isDateVariable[i] && selectedDateFormat[i] != null) {
                // Dates are still Strings, i.e., they are "character" and "discrete";
                // But we add special format values for them:
                dataTable.getDataVariables().get(i).setFormat(DATE_FORMATS[0].toPattern());
                dataTable.getDataVariables().get(i).setFormatCategory("date");
            } else if (isTimeVariable[i] && selectedDateTimeFormat[i] != null) {
                // Same for time values:
                dataTable.getDataVariables().get(i).setFormat(selectedDateTimeFormat[i].toPattern());
                dataTable.getDataVariables().get(i).setFormatCategory("time");
            }
        }
        // Second, final pass.
        try (BufferedReader secondPassReader = new BufferedReader(new FileReader(firstPassTempFile))) {
            parser = new CSVParser(secondPassReader, inFormat.withHeader());
            String[] caseRow = new String[headers.size()];
            
            // Save the variable name header, if requested
            if (saveWithVariableHeader) {
                if (variableNameHeader == null) {
                    throw new IOException("failed to generate the Variable Names header");
                }
                finalOut.println(variableNameHeader);
            }

            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    List<String> args = Arrays.asList(new String[]{"" + (parser.getCurrentLineNumber() - 1),
                                                                   "" + headers.size(),
                                                                   "" + record.size()});
                    throw new IOException(BundleUtil.getStringFromBundle("ingest.csv.recordMismatch", args));
                }

                for (i = 0; i < headers.size(); i++) {
                    String varString = record.get(i);
                    if (isNumericVariable[i]) {
                        if (varString == null || varString.isEmpty() || varString.equalsIgnoreCase("NA")) {
                            // Missing value - represented as an empty string in
                            // the final tab file
                            caseRow[i] = "";
                        } else if (varString.equalsIgnoreCase("NaN")) {
                            // "Not a Number" special value:
                            caseRow[i] = "NaN";
                        } else if (varString.equalsIgnoreCase("Inf")
                                || varString.equalsIgnoreCase("+Inf")) {
                            // Positive infinity:
                            caseRow[i] = "Inf";
                        } else if (varString.equalsIgnoreCase("-Inf")) {
                            // Negative infinity:
                            caseRow[i] = "-Inf";
                        } else if (varString.equalsIgnoreCase("null")) {
                            // By request from Gus - "NULL" is recognized as a
                            // numeric zero:
                            caseRow[i] = isIntegerVariable[i] ? "0" : "0.0";
                        } else {
                            /* No re-formatting is done on any other numeric values.
                             * We'll save them as they were, for archival purposes.
                             * The alternative solution - formatting in sci. notation
                             * is commented-out below.
                             */
                            caseRow[i] = varString;
                            /*
                             if (isIntegerVariable[i]) {
                                try {
                                    Integer testIntegerValue = new Integer(varString);
                                    caseRow[i] = testIntegerValue.toString();
                                } catch (NumberFormatException ex) {
                                    throw new IOException("Failed to parse a value recognized as an integer in the first pass! (?)");
                                }
                            } else {
                                try {
                                    Double testDoubleValue = new Double(varString);
                                    if (testDoubleValue.equals(0.0)) {
                                        caseRow[i] = "0.0";
                                    } else {
                                                                            // One possible implementation:
                                        //
                                        // Round our fractional values to 15 digits
                                        // (minimum number of digits of precision guaranteed by
                                        // type Double) and format the resulting representations
                                        // in a IEEE 754-like "scientific notation" - for ex.,
                                        // 753.24 will be encoded as 7.5324e2
                                        BigDecimal testBigDecimal = new BigDecimal(varString, doubleMathContext);
                                        caseRow[i] = String.format(FORMAT_IEEE754, testBigDecimal);

                                        // Strip meaningless zeros and extra + signs:
                                        caseRow[i] = caseRow[i].replaceFirst("00*e", "e");
                                        caseRow[i] = caseRow[i].replaceFirst("\\.e", ".0e");
                                        caseRow[i] = caseRow[i].replaceFirst("e\\+00", "");
                                        caseRow[i] = caseRow[i].replaceFirst("^\\+", "");
                                    }
                                } catch (NumberFormatException ex) {
                                    throw new IOException("Failed to parse a value recognized as numeric in the first pass! (?)");
                                }
                            }
                             */
                        }
                    } else if (isTimeVariable[i] || isDateVariable[i]) {
                        // Time and Dates are stored NOT quoted (don't ask).
                        if (varString != null) {
                            // Dealing with quotes:
                            // remove the leading and trailing quotes, if present:
                            varString = varString.replaceFirst("^\"*", "");
                            varString = varString.replaceFirst("\"*$", "");
                            caseRow[i] = varString;
                        } else {
                            caseRow[i] = "";
                        }
                    } else {
                        // Treat as a String:
                        // Strings are stored in tab files quoted;
                        // Missing values are stored as an empty string
                        // between two tabs (or one tab and the new line);
                        // Empty strings stored as "" (quoted empty string).
                        // For the purposes  of this CSV ingest reader, we are going
                        // to assume that all the empty strings in the file are
                        // indeed empty strings, and NOT missing values:
                        if (varString != null) {
                            // escape the quotes, newlines, and tabs:
                            varString = varString.replace("\"", "\\\"");
                            varString = varString.replace("\n", "\\n");
                            varString = varString.replace("\t", "\\t");
                            // final pair of quotes:
                            varString = "\"" + varString + "\"";
                            caseRow[i] = varString;
                        } else {
                            caseRow[i] = "\"\"";
                        }
                    }
                }
                finalOut.println(StringUtils.join(caseRow, "\t"));
            }
        }
        long linecount = parser.getRecordNumber();
        finalOut.close();
        parser.close();
        logger.fine("Tmp File: " + firstPassTempFile);
        // Firstpass file is deleted to prevent tmp from filling up.
        firstPassTempFile.delete();
        if (dataTable.getCaseQuantity().intValue() != linecount) {
            List<String> args = Arrays.asList(new String[]{"" + dataTable.getCaseQuantity().intValue(),
                                                           "" + linecount});
            throw new IOException(BundleUtil.getStringFromBundle("ingest.csv.line_mismatch", args));
        }
        return (int) linecount;
    }

}
