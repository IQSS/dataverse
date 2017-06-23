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

import java.io.*;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.*;
import java.util.logging.*;
import java.util.*;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.math.MathContext;
import java.math.RoundingMode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * Dataverse 4.0 implementation of <code>TabularDataFileReader</code> for the
 * plain CSV file with a variable name header.
 *
 *
 * @author Leonid Andreev
 *
 * This implementation uses external R-Scripts to do the bulk of the processing.
 */
public class CSVFileReader extends TabularDataFileReader {

    private static final Logger dbglog = Logger.getLogger(CSVFileReader.class.getPackage().getName());
    private static final int DIGITS_OF_PRECISION_DOUBLE = 15;
    private static final String FORMAT_IEEE754 = "%+#." + DIGITS_OF_PRECISION_DOUBLE + "e";
    private MathContext doubleMathContext;
    private CSVFormat inFormat = CSVFormat.EXCEL.withHeader();
    private CSVFormat outFormat = CSVFormat.TDF;
    private Set<Character> firstNumCharSet = new HashSet<>();

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

    public CSVFileReader(TabularDataFileReaderSpi originator) {
        super(originator);
    }

    private void init() throws IOException {
        doubleMathContext = new MathContext(DIGITS_OF_PRECISION_DOUBLE, RoundingMode.HALF_EVEN);
        firstNumCharSet.addAll(Arrays.asList(new Character[]{'+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}));
    }

    /**
     * Reads a CSV file, converts it into a dataverse DataTable.
     *
     * @param stream a <code>BufferedInputStream</code>.
     * @return an <code>TabularDataIngest</code> object
     * @throws java.io.IOException if a reading error occurs.
     */
    @Override
    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {
        init();

        if (stream == null) {
            throw new IOException("Stream can't be null.");
        }
        TabularDataIngest ingesteddata = new TabularDataIngest();
        DataTable dataTable = new DataTable();

        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(stream));

        File tabFileDestination = File.createTempFile("data-", ".tab");
        PrintWriter tabFileWriter = new PrintWriter(tabFileDestination.getAbsolutePath());

        int lineCount = readFile(localBufferedReader, dataTable, tabFileWriter);

        dbglog.fine("CSV ingest: found " + lineCount + " data cases/observations.");
        dbglog.fine("Tab file produced: " + tabFileDestination.getAbsolutePath());

        dataTable.setUnf("UNF:6:NOTCALCULATED");

        ingesteddata.setTabDelimitedFile(tabFileDestination);
        ingesteddata.setDataTable(dataTable);
        return ingesteddata;

    }

    public int readFile(BufferedReader csvReader, DataTable dataTable, PrintWriter finalOut) throws IOException {

        List<DataVariable> variableList = new ArrayList<>();
        CSVParser parser = new CSVParser(csvReader, inFormat);
        dbglog.fine("Headers: " + parser.getHeaderMap());
        Map<String, Integer> headers = parser.getHeaderMap();
        for (String varName : headers.keySet()) {

            if (varName == null || varName.isEmpty()) {
                // TODO: 
                // Add a sensible variable name validation algorithm.
                // -- L.A. 4.0 alpha 1
                throw new IOException("Invalid variable names in the first line! - First line of a CSV file must contain a comma-separated list of the names of the variables.");
            }

            DataVariable dv = new DataVariable();
            dv.setName(varName);
            dv.setLabel(varName);
            dv.setInvalidRanges(new ArrayList<>());
            dv.setSummaryStatistics(new ArrayList<>());
            dv.setUnf("UNF:6:NOTCALCULATED");
            dv.setCategories(new ArrayList<>());
            variableList.add(dv);

            dv.setTypeCharacter();
            dv.setIntervalDiscrete();
            dv.setFileOrder(headers.get(varName));
            dv.setDataTable(dataTable);
        }

        dataTable.setVarQuantity(new Long(variableList.size()));
        dataTable.setDataVariables(variableList);

        boolean[] isNumericVariable = new boolean[headers.size()];
        boolean[] isIntegerVariable = new boolean[headers.size()];
        boolean[] isTimeVariable = new boolean[headers.size()];
        boolean[] isDateVariable = new boolean[headers.size()];

        for (int i = 0; i < headers.size(); i++) {
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

        File firstPassTempFile = File.createTempFile("firstpass-", ".tab");

        try (CSVPrinter csvFilePrinter = new CSVPrinter(
                new FileWriter(firstPassTempFile.getAbsolutePath()), outFormat)) {
            // Write the header line
            csvFilePrinter.printRecord(headers.keySet());
            for (CSVRecord record : parser.getRecords()) {
                // Checks if #records = #columns in header
                if (!record.isConsistent()) {
                    throw new IOException("Reading mismatch, line " + (parser.getCurrentLineNumber() + 1)
                            + " of the Data file: " + headers.size()
                            + " delimited values expected, " + record.size() + " found.");
                }

                for (int i = 0; i < headers.size(); i++) {
                    String varString = record.get(i);
                    if (isNumericVariable[i]) {
                        // If we haven't given up on the "numeric" status of this
                        // variable, let's perform some tests on it, and see if
                        // this value is still a parsable number:
                        if (varString != null && (!varString.isEmpty())) {

                            boolean isNumeric = false;
                            boolean isInteger = false;

                            if (varString.equalsIgnoreCase("NaN")
                                    || varString.equalsIgnoreCase("NA")
                                    || varString.equalsIgnoreCase("Inf")
                                    || varString.equalsIgnoreCase("+Inf")
                                    || varString.equalsIgnoreCase("-Inf")
                                    || varString.equalsIgnoreCase("null")) {
                                isNumeric = true;
                            } else {
                                try {
                                    Double testDoubleValue = new Double(varString);
                                    isNumeric = true;
                                } catch (NumberFormatException ex) {
                                    // the token failed to parse as a double number;
                                    // so we'll have to assume it's just a string variable.
                                }
                            }

                            if (!isNumeric) {
                                isNumericVariable[i] = false;
                            } else {
                                if (isIntegerVariable[i]) {
                                    if ((varString.equals("null")
                                            || firstNumCharSet.contains(varString.charAt(0))
                                            && StringUtils.isNumeric(varString.substring(1)))) {
                                        isInteger = true;
                                    }

                                }
                            }
                            if (!isInteger) {
                                isIntegerVariable[i] = false;
                            }
                        }
                    }

                    // And if we have concluded that this is not a numeric column,
                    // let's see if we can parse the string token as a date or
                    // a date-time value:
                    if (!isNumericVariable[i]) {

                        Date dateResult = null;

                        if (isTimeVariable[i]) {
                            if (varString != null && (!record.get(i).isEmpty())) {
                                boolean isTime = false;

                                if (selectedDateTimeFormat[i] != null) {
                                    dbglog.fine("will try selected format " + selectedDateTimeFormat[i].toPattern());
                                    ParsePosition pos = new ParsePosition(0);
                                    dateResult = selectedDateTimeFormat[i].parse(varString, pos);

                                    if (dateResult == null) {
                                        dbglog.fine(selectedDateTimeFormat[i].toPattern() + ": null result.");
                                    } else if (pos.getIndex() != varString.length()) {
                                        dbglog.fine(selectedDateTimeFormat[i].toPattern() + ": didn't parse to the end - bad time zone?");
                                    } else {
                                        // OK, successfully parsed a value!
                                        isTime = true;
                                        dbglog.fine(selectedDateTimeFormat[i].toPattern() + " worked!");
                                    }
                                } else {
                                    for (SimpleDateFormat format : TIME_FORMATS) {
                                        dbglog.fine("will try format " + format.toPattern());
                                        ParsePosition pos = new ParsePosition(0);
                                        dateResult = format.parse(varString, pos);
                                        if (dateResult == null) {
                                            dbglog.fine(format.toPattern() + ": null result.");
                                            continue;
                                        }
                                        if (pos.getIndex() != varString.length()) {
                                            dbglog.fine(format.toPattern() + ": didn't parse to the end - bad time zone?");
                                            continue;
                                        }
                                        // OK, successfully parsed a value!
                                        isTime = true;
                                        dbglog.fine(format.toPattern() + " worked!");
                                        selectedDateTimeFormat[i] = format;
                                        break;
                                    }
                                }
                                if (!isTime) {
                                    isTimeVariable[i] = false;
                                    // OK, the token didn't parse as a time value;
                                    // But we will still try to parse it as a date, below.
                                    // unless of course we have already decided that this column
                                    // is NOT a date.
                                } else {
                                    // And if it is a time value, we are going to assume it's
                                    // NOT a date.
                                    isDateVariable[i] = false;
                                }
                            }
                        }

                        if (isDateVariable[i]) {
                            if (varString != null && (!varString.isEmpty())) {
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
                                    dbglog.fine("will try format " + format.toPattern());
                                    try {
                                        format.parse(record.get(i));
                                        dbglog.fine("format " + format.toPattern() + " worked!");
                                        isDate = true;
                                        selectedDateFormat[i] = format;
                                        break;
                                    } catch (ParseException ex) {
                                        //Do nothing
                                        dbglog.fine("format " + format.toPattern() + " didn't work.");
                                    }
                                }
                                if (!isDate) {
                                    isDateVariable[i] = false;
                                }
                            }
                        }
                    }
                }

                csvFilePrinter.printRecord(record);
            }
        }
        dataTable.setCaseQuantity(parser.getCurrentLineNumber());
        parser.close();
        csvReader.close();

        // Re-type the variables that we've determined are numerics:
        for (int i = 0; i < headers.size(); i++) {
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
            dbglog.info("Tmp File: " + firstPassTempFile);
            parser = new CSVParser(secondPassReader, outFormat.withHeader());
            String[] caseRow = new String[headers.size()];

            finalOut.println(StringUtils.join(headers.keySet().toArray(new String[0]), "\t"));
            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    throw new IOException("Reading mismatch, line " + (parser.getCurrentLineNumber() + 1)
                            + " of the Data file: " + headers.size()
                            + " delimited values expected, " + record.size() + " found.");
                }

                // TODO:
                // isolate CSV parsing into its own method/class, to avoid
                // code duplication in the 2 passes, above;
                // do not save the result of the 1st pass - simply reopen the
                // original file (?).
                // -- L.A. 4.0.2/4.1
                for (int i = 0; i < headers.size(); i++) {
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
                            if (isIntegerVariable[i]) {
                                caseRow[i] = "0";
                            } else {
                                caseRow[i] = "0.0";
                            }
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
                             Integer testIntegerValue = new Integer(valueTokens[i]);
                             caseRow[i] = testIntegerValue.toString();
                             } catch (NumberFormatException ex) {
                             throw new IOException ("Failed to parse a value recognized as an integer in the first pass! (?)");
                             }
                             } else {
                             try {
                             Double testDoubleValue = new Double(valueTokens[i]);
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
                             BigDecimal testBigDecimal = new BigDecimal(valueTokens[i], doubleMathContext);
                             // an experiment - what's gonna happen if we just
                             // use the string representation of the bigdecimal object
                             // above?
                             //caseRow[i] = testBigDecimal.toString();
                             =
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
                            String charToken = varString;
                            // Dealing with quotes:
                            // remove the leading and trailing quotes, if present:
                            charToken = charToken.replaceFirst("^\"*", "");
                            charToken = charToken.replaceFirst("\"*$", "");
                            caseRow[i] = charToken;
                        } else {
                            caseRow[i] = "";
                        }
                    } else {
                        // Treat as a String:
                        // Strings are stored in tab files quoted;
                        // Missing values are stored as tab-delimited nothing -
                        // i.e., an empty string between two tabs (or one tab and
                        // the new line);
                        // Empty strings stored as "" (quoted empty string).
                        // For the purposes  of this CSV ingest reader, we are going
                        // to assume that all the empty strings in the file are
                        // indeed empty strings, and NOT missing values:
                        if (record.get(i) != null) {
                            String charToken = varString;
                            // Dealing with quotes:
                            // remove the leading and trailing quotes, if present:
                            charToken = charToken.replaceFirst("^\"", "");
                            charToken = charToken.replaceFirst("\"$", "");
                            // escape the remaining ones:
                            charToken = charToken.replace("\"", "\\\"");
                            // final pair of quotes:
                            charToken = "\"" + charToken + "\"";
                            caseRow[i] = charToken;
                        } else {
                            caseRow[i] = "\"\"";
                        }
                    }
                }
                dbglog.fine("CaseRow: " + Arrays.toString(caseRow));
                finalOut.println(StringUtils.join(caseRow, "\t"));
            }
        }
        finalOut.close();
        long linecount = parser.getCurrentLineNumber();
        parser.close();
        if (dataTable.getCaseQuantity().intValue() != linecount) {
            throw new IOException("Mismatch between line counts in first and final passes!, "
                    + dataTable.getCaseQuantity().intValue() + " found on first count, but "
                    + linecount + " found on second.");
        }
        new File(firstPassTempFile.getAbsolutePath()).delete();
        return (int) linecount;
    }

}
