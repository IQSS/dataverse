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
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;

import edu.harvard.iq.dataverse.ingest.plugin.spi.*;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

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
    private char delimiterChar = ',';
    
    // DATE FORMATS
    private static SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd")
    };
  
    // TIME FORMATS
    private static SimpleDateFormat[] TIME_FORMATS = new SimpleDateFormat[] {
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
    }
    
    /**
     * Reads a CSV file, converts it into a dataverse DataTable.
     *
     * @param stream a <code>BufferedInputStream</code>.
     * @param ignored
     * @return an <code>TabularDataIngest</code> object
     * @throws java.io.IOException if a reading error occurs.
     */
    @Override
    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {
        init();
        
        TabularDataIngest ingesteddata = new TabularDataIngest();
        DataTable dataTable = new DataTable();

        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(stream));

        File tabFileDestination = File.createTempFile("data-", ".tab");
        PrintWriter tabFileWriter = new PrintWriter(tabFileDestination.getAbsolutePath());

        int lineCount = readFile(localBufferedReader, dataTable, tabFileWriter);        
        
        dbglog.fine("CSV ingest: found "+lineCount+" data cases/observations.");
        dbglog.fine("Tab file produced: "+tabFileDestination.getAbsolutePath());
        
        dataTable.setUnf("UNF:6:NOTCALCULATED");
        
        ingesteddata.setTabDelimitedFile(tabFileDestination);
        ingesteddata.setDataTable(dataTable);
        return ingesteddata;

    }

    public int readFile(BufferedReader csvReader, DataTable dataTable, PrintWriter finalOut) throws IOException {
        
        String line;
        String[] valueTokens;

        int lineCounter = 0;
       
        // Read first line: 
        
        line = csvReader.readLine();
        line = line.replaceFirst("[\r\n]*$", "");
        valueTokens = line.split("" + delimiterChar, -2);
        
        if (valueTokens == null || valueTokens.length < 1) {
            throw new IOException("Failed to read first, variable name line of the CSV file.");
        }

        int variableCount = valueTokens.length;
        
        // Create variables: 
        
        List<DataVariable> variableList = new ArrayList<>();
        
        for (int i = 0; i < variableCount; i++) {
            String varName = valueTokens[i];
            
            if (varName == null || varName.equals("")) {
                // TODO: 
                // Add a sensible variable name validation algorithm.
                // -- L.A. 4.0 alpha 1
                throw new IOException ("Invalid variable names in the first line! - First line of a CSV file must contain a comma-separated list of the names of the variables.");
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
            dv.setFileOrder(i);
            dv.setDataTable(dataTable);
        }
        
        dataTable.setVarQuantity(new Long(variableCount));
        dataTable.setDataVariables(variableList);
        
        boolean[] isNumericVariable = new boolean[variableCount];
        boolean[] isIntegerVariable = new boolean[variableCount];
        boolean[] isTimeVariable = new boolean[variableCount];
        boolean[] isDateVariable = new boolean[variableCount];
        
        for (int i = 0; i < variableCount; i++) {
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
        
        SimpleDateFormat[] selectedDateTimeFormat = new SimpleDateFormat[variableCount]; 
        SimpleDateFormat[] selectedDateFormat = new SimpleDateFormat[variableCount];

        
        File firstPassTempFile = File.createTempFile("firstpass-", ".tab");
        PrintWriter firstPassWriter = new PrintWriter(firstPassTempFile.getAbsolutePath());
        
        
        while ((line = csvReader.readLine()) != null) {
            // chop the line:
            line = line.replaceFirst("[\r\n]*$", "");
            valueTokens = line.split("" + delimiterChar, -2);

            if (valueTokens == null) {
                throw new IOException("Failed to read line " + (lineCounter + 1) + " of the Data file.");
            }

            int tokenCount = valueTokens.length;
            
            if (tokenCount > variableCount) {
                
                // we'll make another attempt to parse the fields - there could be commas 
                // inside character strings. The only way to disambiguate this situation
                // we are going to support, for now, is to allow commas inside tokens 
                // wrapped in double quotes. We may potentially add other mechanisms, 
                // such as allowing to specify a custom string wrapper character (something other
                // than the double quote), or maybe recognizing escaped commas ("\,") as 
                // non-separating ones. 
                // -- L.A. 4.0.2
                
                valueTokens = null; 
                valueTokens = new String[variableCount];
                
                int tokenStart = 0;
                boolean quotedStringMode = false; 
                boolean potentialDoubleDoubleQuote = false;
                tokenCount = 0; 
                
                for (int i = 0; i < line.length(); i++) {
                    if (tokenCount > variableCount) {
                        throw new IOException("Reading mismatch, line " + (lineCounter + 1) + " of the data file contains more than "
                        + variableCount + " comma-delimited values.");
                    }
                    
                    char c = line.charAt(i);
                    
                    if (tokenStart == i && c == '"') {
                        quotedStringMode = true; 
                    } else if (c == ',' && !quotedStringMode) {
                        valueTokens[tokenCount] = line.substring(tokenStart, i);
                        tokenCount++; 
                        tokenStart = i+1; 
                    } else if (i == line.length() - 1) {
                        valueTokens[tokenCount] = line.substring(tokenStart, line.length());
                        tokenCount++; 
                    } else if (quotedStringMode && c == '"') {
                        quotedStringMode = false; 
                        //unless this is a double double quote in the middle of a quoted
                        // string; apparently a standard notation for encoding double
                        // quotes inside quoted strings (??)
                        potentialDoubleDoubleQuote = true; 
                    } else if (potentialDoubleDoubleQuote && c == '"') {
                        // OK, that was a "double double" quote.
                        // going back into the quoted mode:
                        quotedStringMode = true; 
                        potentialDoubleDoubleQuote = false; 
                        // TODO: figure out what we do with such double double quote
                        // sequences in the final tab file. Do we want to convert 
                        // them back to a "single double" quote?
                        // -- L.A. 4.0.2/4.1
                    }
                        
                }
            }
                        
            //dbglog.info("Number of CSV tokens in the line number " + lineCounter + " : "+tokenCount);
            
            // final token count check: 
            
            if (tokenCount != variableCount) {
                
                throw new IOException("Reading mismatch, line " + (lineCounter + 1) + " of the Data file: "
                        + variableCount + " delimited values expected, " + tokenCount + " found.");
            }

            for (int i = 0; i < variableCount; i++) {
                if (isNumericVariable[i]) {
                    // If we haven't given up on the "numeric" status of this 
                    // variable, let's perform some tests on it, and see if 
                    // this value is still a parsable number:
                    if (valueTokens[i] != null && (!valueTokens[i].equals(""))) {

                        boolean isNumeric = false; 
                        boolean isInteger = false; 
                        
                        if (valueTokens[i].equalsIgnoreCase("NaN")
                                || valueTokens[i].equalsIgnoreCase("NA")
                                || valueTokens[i].equalsIgnoreCase("Inf")
                                || valueTokens[i].equalsIgnoreCase("+Inf")
                                || valueTokens[i].equalsIgnoreCase("-Inf")
                                || valueTokens[i].equalsIgnoreCase("null")) {
                            isNumeric = true;
                        } else {
                            try {
                                Double testDoubleValue = new Double(valueTokens[i]);
                                isNumeric = true; 
                            } catch (NumberFormatException ex) {
                                // the token failed to parse as a double number;
                                // so we'll have to assume it's just a string variable.
                            }
                        }
                        
                        if (!isNumeric) {
                            isNumericVariable[i] = false; 
                        } else if (isIntegerVariable[i]) {
                            try {
                                Integer testIntegerValue = new Integer(valueTokens[i]);
                                isInteger = true; 
                            } catch (NumberFormatException ex) {
                                // the token failed to parse as an integer number;
                                // we'll assume it's a non-integere numeric...
                            }
                            if (!isInteger) {
                                isIntegerVariable[i] = false; 
                            }
                        }
                    }
                }
                
                // And if we have concluded that this is not a numeric column, 
                // let's see if we can parse the string token as a date or 
                // a date-time value:
                
                if (!isNumericVariable[i]) {
                    
                    Date dateResult = null; 
                    
                    if (isTimeVariable[i]) {
                        if (valueTokens[i] != null && (!valueTokens[i].equals(""))) {
                            boolean isTime = false;

                            if (selectedDateTimeFormat[i] != null) {
                                dbglog.fine("will try selected format " + selectedDateTimeFormat[i].toPattern());
                                ParsePosition pos = new ParsePosition(0);
                                dateResult = selectedDateTimeFormat[i].parse(valueTokens[i], pos);

                                if (dateResult == null) {
                                    dbglog.fine(selectedDateTimeFormat[i].toPattern() + ": null result.");
                                } else if (pos.getIndex() != valueTokens[i].length()) {
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
                                    dateResult = format.parse(valueTokens[i], pos);
                                    if (dateResult == null) {
                                        dbglog.fine(format.toPattern() + ": null result.");
                                        continue;
                                    }
                                    if (pos.getIndex() != valueTokens[i].length()) {
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
                        if (valueTokens[i] != null && (!valueTokens[i].equals(""))) {
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
                                    dateResult = format.parse(valueTokens[i]);
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
            
            firstPassWriter.println(line);
            lineCounter++;
        }
        
        firstPassWriter.close(); 
        csvReader.close();
        dataTable.setCaseQuantity(new Long(lineCounter));

            
        // Re-type the variables that we've determined are numerics:
        
        for (int i = 0; i < variableCount; i++) {
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
        
        // Re-open the saved file and reset the line counter: 
        
        BufferedReader secondPassReader = new BufferedReader(new FileReader(firstPassTempFile));
        lineCounter = 0;
        String[] caseRow = new String[variableCount];

        
        while ((line = secondPassReader.readLine()) != null) {
            // chop the line:
            line = line.replaceFirst("[\r\n]*$", "");
            valueTokens = line.split("" + delimiterChar, -2);

            if (valueTokens == null) {
                throw new IOException("Failed to read line " + (lineCounter + 1) + " during the second pass.");
            }

            int tokenCount = valueTokens.length;
            
            if (tokenCount > variableCount) {
                
                // again, similar check for quote-encased strings that contain 
                // commas inside them. 
                // -- L.A. 4.0.2
                
                valueTokens = null; 
                valueTokens = new String[variableCount];
                
                int tokenStart = 0;
                boolean quotedStringMode = false;
                boolean potentialDoubleDoubleQuote = false;
                tokenCount = 0; 
                
                for (int i = 0; i < line.length(); i++) {
                    if (tokenCount > variableCount) {
                        throw new IOException("Reading mismatch, line " + (lineCounter + 1) + " of the data file contains more than "
                        + variableCount + " comma-delimited values.");
                    }
                    
                    char c = line.charAt(i);
                    
                    if (tokenStart == i && c == '"') {
                        quotedStringMode = true; 
                    } else if (c == ',' && !quotedStringMode) {
                        valueTokens[tokenCount] = line.substring(tokenStart, i);
                        tokenCount++; 
                        tokenStart = i+1; 
                    } else if (i == line.length() - 1) {
                        valueTokens[tokenCount] = line.substring(tokenStart, line.length());
                        tokenCount++; 
                    } else if (quotedStringMode && c == '"') {
                        quotedStringMode = false; 
                        potentialDoubleDoubleQuote = true; 
                    } else if (potentialDoubleDoubleQuote && c == '"') {
                        quotedStringMode = true; 
                        potentialDoubleDoubleQuote = false; 
                    }
                        
                }
            }
            
            // TODO: 
            // isolate CSV parsing into its own method/class, to avoid
            // code duplication in the 2 passes, above;
            // do not save the result of the 1st pass - simply reopen the 
            // original file (?). 
            // -- L.A. 4.0.2/4.1
            
            if (tokenCount != variableCount) {
                throw new IOException("Reading mismatch, line " + (lineCounter + 1) + " during the second pass: "
                        + variableCount + " delimited values expected, " + valueTokens.length + " found.");
            }
        
            for (int i = 0; i < variableCount; i++) {
                if (isNumericVariable[i]) {
                    if (valueTokens[i] == null || valueTokens[i].equalsIgnoreCase("") || valueTokens[i].equalsIgnoreCase("NA")) {
                        // Missing value - represented as an empty string in 
                        // the final tab file
                        caseRow[i] = "";
                    } else if (valueTokens[i].equalsIgnoreCase("NaN")) {
                        // "Not a Number" special value: 
                        caseRow[i] = "NaN";
                    } else if (valueTokens[i].equalsIgnoreCase("Inf")
                            || valueTokens[i].equalsIgnoreCase("+Inf")) {
                        // Positive infinity:
                        caseRow[i] = "Inf";
                    } else if (valueTokens[i].equalsIgnoreCase("-Inf")) {
                        // Negative infinity: 
                        caseRow[i] = "-Inf";
                    } else if (valueTokens[i].equalsIgnoreCase("null")) {
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
                        caseRow[i] = valueTokens[i];
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
                    if (valueTokens[i] != null) {
                        String charToken = valueTokens[i];
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
                    if (valueTokens[i] != null) {
                        String charToken = valueTokens[i];
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
            
            finalOut.println(StringUtils.join(caseRow, "\t"));
            lineCounter++;

            
        }

        secondPassReader.close();
        finalOut.close();
        
        if (dataTable.getCaseQuantity().intValue() != lineCounter) {
            throw new IOException("Mismatch between line counts in first and final passes!");
        }
        
        return lineCounter;
    }

}
