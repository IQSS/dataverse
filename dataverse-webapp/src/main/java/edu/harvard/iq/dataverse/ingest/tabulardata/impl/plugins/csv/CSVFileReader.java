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

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestException;
import io.vavr.Tuple2;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dataverse 4.0 implementation of <code>TabularDataFileReader</code> for the
 * plain CSV file with a variable name header.
 *
 * @author Oscar Smith
 * <p>
 * This implementation uses the Apache CSV Parser
 */
public class CSVFileReader extends TabularDataFileReader {

    private static final Logger logger = Logger.getLogger(CSVFileReader.class.getPackage().getName());

    private CSVFormat inFormat;
    private final Set<Character> firstNumCharSet = new HashSet<>();

    // DATE FORMATS
    private static SimpleDateFormat[] DATE_FORMATS = { new SimpleDateFormat("yyyy-MM-dd") };

    // TIME FORMATS
    private static SimpleDateFormat[] TIME_FORMATS = {
            // Date-time up to seconds with timezone, e.g. 2013-04-08 13:14:23 -0500
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),
            // Date-time up to seconds and no timezone, e.g. 2013-04-08 13:14:23
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    };

    // -------------------- CONSTRUCTORS --------------------

    public CSVFileReader(TabularDataFileReaderSpi originator, char delim) {
        super(originator);
        if (delim == ',') {
            inFormat = CSVFormat.EXCEL;
        } else if (delim == '\t') {
            inFormat = CSVFormat.TDF;
        }
    }

    // -------------------- LOGIC --------------------

    private void init() throws IOException {
        firstNumCharSet.addAll(Arrays.asList('+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
    }

    /** Reads a CSV file, converts it into a dataverse DataTable. */
    @Override
    public TabularDataIngest read(Tuple2<BufferedInputStream, File> streamAndFile, File dataFile) throws IOException {
        init();

        boolean trySemicolonFormat;
        // that stream is closed elsewhere, but as we've no reason to keep it open, let it be closed right after use:
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(streamAndFile._1()))) {
            trySemicolonFormat = isHeaderOfSemicolonType(reader);
        }

        TabularDataIngest ingesteddata = new TabularDataIngest();
        DataTable dataTable = new DataTable();

        File tabFileDestination = File.createTempFile("data-", ".tab");
        File firstPassTempFile = File.createTempFile("firstpass-", ".csv");
        boolean ingestExceptionHappened = false;

        if (trySemicolonFormat) {
            try (PrintWriter tabFileWriter = new PrintWriter(tabFileDestination.getAbsolutePath())) {
                readFile(streamAndFile._2(), dataTable, tabFileWriter, firstPassTempFile, inFormat.withHeader().withDelimiter(';'));
            } catch (IngestException ie) {
                logger.log(Level.WARNING, "Semicolon-format ingest failed – deleting intermediate files " +
                        "and trying again with default settings", ie);
                ingestExceptionHappened = true;
                tabFileDestination.delete();
                tabFileDestination.createNewFile();
                dataTable = new DataTable();
            } finally {
                firstPassTempFile.delete();
            }
        }
        if (!trySemicolonFormat || ingestExceptionHappened) {
            try (PrintWriter tabFileWriter = new PrintWriter(tabFileDestination.getAbsolutePath())) {
                if (!firstPassTempFile.exists()) {
                    firstPassTempFile.createNewFile();
                }
                readFile(streamAndFile._2(), dataTable, tabFileWriter, firstPassTempFile, inFormat.withHeader());
            } finally {
                firstPassTempFile.delete();
            }
        }
        logger.fine("Tab file produced: " + tabFileDestination.getAbsolutePath());
        dataTable.setUnf("UNF:6:NOTCALCULATED");
        ingesteddata.setTabDelimitedFile(tabFileDestination);
        ingesteddata.setDataTable(dataTable);
        return ingesteddata;
    }

    public int readFile(File file, DataTable dataTable, PrintWriter finalOut, File firstPassTempFile, CSVFormat csvFormat) throws IOException {

        if (file == null) {
            throw new IngestException(IngestError.UNKNOWN_ERROR);
        }

        List<DataVariable> variableList = new ArrayList<>();
        CSVParser parser = CSVParser.parse(file, selectCharset(), csvFormat);
        Map<String, Integer> headers = parser.getHeaderMap();

        int i = 0;
        for (String varName : headers.keySet()) {
            if (varName == null || varName.isEmpty()) {
                // TODO: Add a sensible variable name validation algorithm. -- L.A. 4.0 alpha 1
                throw new IngestException(IngestError.CSV_INVALID_HEADER);
            }

            DataVariable dv = new DataVariable(i, dataTable);
            dv.setName(varName);
            dv.setLabel(varName);
            variableList.add(dv);

            dv.setTypeCharacter();
            dv.setIntervalDiscrete();
            i++;
        }

        dataTable.setVarQuantity((long) variableList.size());
        dataTable.setDataVariables(variableList);

        boolean[] isNumericVariable = new boolean[headers.size()];
        boolean[] isIntegerVariable = new boolean[headers.size()];
        boolean[] isTimeVariable = new boolean[headers.size()];
        boolean[] isDateVariable = new boolean[headers.size()];

        for (i = 0; i < headers.size(); i++) {
            // OK, let's assume that every variable is numeric; but we'll go through the file and examine every value;
            // the moment we find a value that's not a legit numeric one, we'll assume that it is in fact a String.
            isNumericVariable[i] = true;
            isIntegerVariable[i] = true;
            isDateVariable[i] = true;
            isTimeVariable[i] = true;
        }

        // First, "learning" pass. (we'll save the incoming stream in another temp file:)
        SimpleDateFormat[] selectedDateTimeFormat = new SimpleDateFormat[headers.size()];
        SimpleDateFormat[] selectedDateFormat = new SimpleDateFormat[headers.size()];

        try (CSVPrinter csvFilePrinter = new CSVPrinter(
                // TODO allow other parsers of tabular data to use this parser by changing inFormat
                new FileWriter(firstPassTempFile.getAbsolutePath()), inFormat)) {
            // Write  headers
            csvFilePrinter.printRecord(headers.keySet());
            for (CSVRecord record : parser) {
                // Checks if #records = #columns in header
                if (!record.isConsistent()) {
                    List<String> args = Arrays.asList("" + (parser.getCurrentLineNumber() - 1),
                                                      "" + headers.size(),
                                                      "" + record.size());
                    throw new IngestException(IngestError.CSV_RECORD_MISMATCH, args);
                }

                for (i = 0; i < headers.size(); i++) {
                    String varString = record.get(i);
                    isIntegerVariable[i] = isIntegerVariable[i]
                            && varString != null
                            && (varString.isEmpty()
                            || varString.equals("null")
                            || (firstNumCharSet.contains(varString.charAt(0))
                            && StringUtils.isNumeric(varString.substring(1))));
                    if (isNumericVariable[i]) {
                        // If variable might be "numeric" test to see if this value is a parsable number:
                        if (varString != null && !varString.isEmpty()) {
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
                                    // the token failed to parse as a double so the column is a string variable.
                                }
                            }
                            isNumericVariable[i] = false;
                        }
                    }

                    // If this is not a numeric column, see if it is a date collumn
                    // by parsing the cell as a date or date-time value:
                    if (!isNumericVariable[i]) {

                        Date dateResult;

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
                                    // if the token didn't parse as a time value, we will still try to parse it as a
                                    // date, below. unless this column is NOT a date.
                                } else {
                                    // And if it is a time value, we are going to assume it's NOT a date.
                                    isDateVariable[i] = false;
                                }
                            }
                        }

                        if (isDateVariable[i]) {
                            if (varString != null && !varString.isEmpty()) {
                                boolean isDate = false;

                                // TODO: Strictly speaking, we should be doing the same thing here as with the time
                                // formats above; select the first one that works, then insist that all the other values
                                // in this column match it... but we only have one, as of now, so it should be ok.
                                // -- L.A. 4.0 beta
                                for (SimpleDateFormat format : DATE_FORMATS) {
                                    // Strict parsing - it will throw an exception if it doesn't parse!
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

            for (CSVRecord record : parser) {
                if (!record.isConsistent()) {
                    List<String> args = Arrays.asList("" + (parser.getCurrentLineNumber() - 1),
                                                      "" + headers.size(),
                                                      "" + record.size());
                    throw new IngestException(IngestError.CSV_RECORD_MISMATCH, args);
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
                            caseRow[i] = varString;
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
                        // Treat as a String: Strings are stored in tab files quoted; Missing values are stored as an
                        // empty string between two tabs (or one tab and the new line).
                        // Empty strings stored as "" (quoted empty string). For the purposes  of this CSV ingest
                        // reader, we are going to assume that all the empty strings in the file are indeed empty
                        // strings, and NOT missing values:
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
            long linecount = parser.getRecordNumber();
            if (dataTable.getCaseQuantity().intValue() != linecount) {
                List<String> args = Arrays.asList("" + dataTable.getCaseQuantity().intValue(),
                        "" + linecount);
                throw new IngestException(IngestError.CSV_LINE_MISMATCH, args);
            }
            return (int) linecount;
        } finally {
            finalOut.close();
            parser.close();
        }
    }

    // -------------------- PRIVATE --------------------

    private boolean isHeaderOfSemicolonType(BufferedReader reader) {
        return reader.lines()
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(l -> l.split(";").length > l.split(",").length)
                .orElse(false);
    }
}
