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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx;


import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestException;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * New (4.0) ingest plugin for Excel/XLSX (XML) spreadsheeets.
 * <p>
 * It utilizes Apache POI framework for reading XLSX data; and uses an
 * event-based, SAX model for parsing the extracted XML. This way spreadsheets
 * of any size can be converted into tab-delimited data with a fairly small
 * memory footprint.
 *
 * @author Leonid Andreev
 */
public class XLSXFileReader extends TabularDataFileReader {

    private static final Logger logger = Logger.getLogger(XLSXFileReader.class.getPackage().getName());
    private static final char DELIMITER_CHAR = '\t';

    private static XlsxColumnIndexConverter converter = new XlsxColumnIndexConverter();

    // -------------------- CONSTRUCTORS --------------------

    public XLSXFileReader(TabularDataFileReaderSpi originator) {
        super(originator);
    }

    // -------------------- LOGIC --------------------

    /**
     * Reads an XLSX file, converts it into a dataverse DataTable.
     *
     * @param stream  a <code>BufferedInputStream</code>.
     * @return an <code>TabularDataIngest</code> object
     * @throws java.io.IOException if a reading error occurs.
     */
    @Override
    public TabularDataIngest read(BufferedInputStream stream, File dataFile) throws IOException {
        File firstPassTempFile = null;
        try {
            firstPassTempFile = File.createTempFile("firstpass-", ".tab");
            return getTabularDataIngest(stream, firstPassTempFile);
        } finally {
            firstPassTempFile.delete();
        }
    }

    public XMLReader fetchSheetParser(SharedStringsTable sst, DataTable dataTable, PrintWriter tempOut) throws SAXException {
        // An attempt to use org.apache.xerces.parsers.SAXParser resulted
        // in some weird conflict in the app; the default XMLReader obtained
        // from the XMLReaderFactory (from xml-apis.jar) appears to be working
        // just fine. however,
        // TODO: verify why the app gets built with xml-apis-1.0.b2.jar; it's
        // an old version - 1.4 seems to be the current release, and 2.0.2
        // (a new development?) appears to be available. We don't specifically
        // request this 1.0.* version, so another package must have it defined
        // as a dependency. We need to verify our dependencies, we most likely
        // have some hard-coded versions in our pom.xml that are both old and
        // unnecessary.
        // -- L.A. 4.0 alpha 1

        XMLReader xReader = XMLReaderFactory.createXMLReader();
        logger.fine("creating new SheetHandler;");
        ContentHandler handler = new SheetHandler(sst, dataTable, tempOut);
        xReader.setContentHandler(handler);
        return xReader;
    }

    public void processSheet(InputStream inputStream, DataTable dataTable, PrintWriter tempOut) throws Exception {
        logger.info("entering processSheet");
        OPCPackage pkg = OPCPackage.open(inputStream);
        XSSFReader r = new XSSFReader(pkg);
        SharedStringsTable sst = r.getSharedStringsTable();

        XMLReader parser = fetchSheetParser(sst, dataTable, tempOut);

        // rId2 found by processing the Workbook
        // Seems to either be rId# or rSheet#
        InputStream sheet1 = r.getSheet("rId1");
        InputSource sheetSource = new InputSource(sheet1);
        parser.parse(sheetSource);
        sheet1.close();
    }

    // -------------------- PRIVATE --------------------

    private TabularDataIngest getTabularDataIngest(BufferedInputStream stream, File firstPassTempFile) throws IOException {
        TabularDataIngest ingesteddata = new TabularDataIngest();
        DataTable dataTable = new DataTable();
        PrintWriter firstPassWriter = new PrintWriter(firstPassTempFile.getAbsolutePath());
        try {
            processSheet(stream, dataTable, firstPassWriter);
        } catch (IngestException ie) {
            logger.log(Level.FINE, "Could not parse Excel/XLSX spreadsheet.", ie);
            throw ie;
        } catch (Exception ex) {
            logger.log(Level.FINE, "Could not parse Excel/XLSX spreadsheet.", ex);
            throw new IngestException(IngestError.EXCEL_PARSE);
        }

        if (dataTable.getCaseQuantity() == null || dataTable.getCaseQuantity().intValue() < 1) {

            if (dataTable.getVarQuantity() == null || dataTable.getVarQuantity().intValue() < 1) {
                throw new IngestException(IngestError.EXCEL_NO_ROWS);
            } else {
                throw new IngestException(IngestError.EXCEL_ONLY_ONE_ROW);
            }
        }

        // 2nd pass:
        File tabFileDestination = File.createTempFile("data-", ".tab");
        try (BufferedReader secondPassReader = new BufferedReader(new FileReader(firstPassTempFile));
             PrintWriter finalWriter = new PrintWriter(tabFileDestination.getAbsolutePath());) {

            int varQnty = dataTable.getVarQuantity().intValue();
            int lineCounter = 0;
            String line = null;
            String[] caseRow = new String[varQnty];
            String[] valueTokens;

            while ((line = secondPassReader.readLine()) != null) {
                // chop the line:
                line = line.replaceFirst("[\r\n]*$", "");
                valueTokens = line.split("" + DELIMITER_CHAR, -2);

                if (valueTokens.length != varQnty) {
                    throw new IngestException(IngestError.EXCEL_MISMATCH,
                            Arrays.asList(Integer.toString(lineCounter + 1),
                                    Integer.toString(varQnty),
                                    Integer.toString(valueTokens.length)));
                }

                for (int i = 0; i < varQnty; i++) {
                    if (dataTable.getDataVariables().get(i).isTypeNumeric()) {
                        if (valueTokens[i] == null || valueTokens[i].equals(".") || valueTokens[i].equals("") || valueTokens[i].equalsIgnoreCase(
                                "NA")) {
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
                            caseRow[i] = "0";
                        } else {
                            try {
                                Double testDoubleValue = new Double(valueTokens[i]);
                                caseRow[i] = testDoubleValue.toString();
                            } catch (Exception ex) {
                                throw new IngestException(IngestError.EXCEL_NUMERIC_PARSE, String.valueOf(i), valueTokens[i]);
                            }
                        }
                    } else {
                        // Treat as a String:
                        // Strings are stored in tab files quoted;
                        // Missing values are stored as tab-delimited nothing -
                        // i.e., an empty string between two tabs (or one tab and
                        // the new line);
                        // Empty strings stored as "" (quoted empty string).

                        if (valueTokens[i] != null && !valueTokens[i].equals(".")) {
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
                            caseRow[i] = "";
                        }
                    }
                }
                finalWriter.println(StringUtils.join(caseRow, "\t"));
                lineCounter++;
            }

            if (dataTable.getCaseQuantity().intValue() != lineCounter) {
                throw new IngestException(IngestError.EXCEL_LINE_COUNT);
            }

            dataTable.setUnf("UNF:6:NOTCALCULATED");

            ingesteddata.setTabDelimitedFile(tabFileDestination);
            ingesteddata.setDataTable(dataTable);

            String varNames = dataTable.getDataVariables().stream()
                    .map(DataVariable::getName)
                    .collect(Collectors.joining(", "));
            logger.fine(String.format("Produced temporary file %s\nFound %d variables, %d observations.\nVariable names: %s",
                    ingesteddata.getTabDelimitedFile().getAbsolutePath(), dataTable.getVarQuantity(), dataTable.getCaseQuantity(), varNames));
            return ingesteddata;
        }
    }

    // -------------------- INNER CLASSES --------------------

    private static class SheetHandler extends DefaultHandler {

        private DataTable dataTable;
        private SharedStringsTable sst;
        private String cellContents;
        private boolean nextIsString;
        private boolean variableHeader;
        private String[] variableNames;
        private int caseCount;
        private int columnCount;
        private boolean[] isNumericVariable;
        private String[] dataRow;
        private PrintWriter tempOut;

        // -------------------- CONSTRUCTORS --------------------

        private SheetHandler(SharedStringsTable sst) {
            this(sst, null, null);
        }

        private SheetHandler(SharedStringsTable sst, DataTable dataTable, PrintWriter tempOut) {
            this.sst = sst;
            this.dataTable = dataTable;
            this.tempOut = tempOut;
            variableHeader = true;
            caseCount = 0;
            columnCount = 0;
        }

        // -------------------- LOGIC --------------------

        public void startElement(String uri, String localName, String name, Attributes attributes) {
            logger.fine("entering startElement (" + name + ")");

            // first raw encountered:
            if (variableHeader && "row".equals(name)) {
                Long varCount;
                String rAttribute = attributes.getValue("t");
                if (rAttribute == null) {
                    logger.warning("Null r attribute in the first row element!");
                } else if (!rAttribute.equals("1")) {
                    logger.warning("Attribute r of the first row element is not \"1\"!");
                }

                String spansAttribute = attributes.getValue("spans");
                if (spansAttribute == null) {
                    logger.warning("Null spans attribute in the first row element!");
                }
                int colIndex = spansAttribute.indexOf(':');
                if (colIndex < 1 || (colIndex == spansAttribute.length() - 1)) {
                    logger.warning("Invalid spans attribute in the first row element: " + spansAttribute + "!");
                }
                try {
                    varCount = new Long(spansAttribute.substring(colIndex + 1));
                } catch (Exception ex) {
                    varCount = null;
                }

                if (varCount == null || varCount.intValue() < 1) {
                    throw new IngestException(IngestError.EXCEL_UNKNOWN_OR_INVALID_COLUMN_COUNT);
                }

                logger.info("Established variable (column) count: " + varCount);

                dataTable.setVarQuantity(varCount);
                variableNames = new String[varCount.intValue()];
            }

            // c => cell
            if ("c".equals(name)) {
                // try and establish the location index (column number) of this
                // cell, from the "r" attribute:

                String indexAttribute = attributes.getValue("r");

                if (indexAttribute == null) {
                    logger.warning("Null r attribute in a cell element!");
                }
                if (!indexAttribute.matches(".*[0-9]")) {
                    logger.warning("Invalid index (r) attribute in a cell element: " + indexAttribute + "!");
                }
                columnCount = converter.columnToIndex(indexAttribute.replaceFirst("[0-9].*$", ""));

                if (columnCount < 0) {
                    throw new IngestException(IngestError.EXCEL_AMBIGUOUS_INDEX_POSITION);
                }

                String cellType = attributes.getValue("t");
                nextIsString = cellType != null && cellType.equals("s");
            }
            // Clear contents cache
            cellContents = "";
        }

        public void endElement(String uri, String localName, String name) {
            logger.fine("entering endElement (" + name + ")");
            // Process the content cache as required.
            // Do it now, as characters() may be called more than once
            if (nextIsString) {
                int idx = Integer.parseInt(cellContents);
                cellContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
                nextIsString = false;
            }

            // v => contents of a cell
            // Output after we've seen the string contents
            if ("v".equals(name)) {
                if (variableHeader) {
                    logger.fine("variable header mode; cell " + columnCount + ", cell contents: " + cellContents);

                    //variableNames.add(cellContents);
                    variableNames[columnCount] = cellContents;
                } else {
                    dataRow[columnCount] = cellContents;
                    logger.fine("data row mode; cell " + columnCount + ", cell contents: " + cellContents);
                }
            }

            if ("row".equals(name)) {
                if (variableHeader) {
                    // Initialize variables:
                    logger.fine("variableHeader mode; ");
                    List<DataVariable> variableList = new ArrayList<DataVariable>();
                    //columnCount = variableNames.size();
                    columnCount = dataTable.getVarQuantity().intValue();

                    for (int i = 0; i < columnCount; i++) {
                        String varName = variableNames[i];

                        if (varName == null || varName.equals("")) {
                            varName = converter.indexToColumn(i);
                        }
                        if (varName == null) {
                            throw new IngestException(IngestError.EXCEL_UNKNOWN_VARIABLE_NAME, String.valueOf(i));
                        }

                        varName = varName.replaceAll("[ _\t\n\r]", "");

                        DataVariable dv = new DataVariable(i, dataTable);
                        dv.setName(varName);
                        dv.setLabel(varName);
                        variableList.add(dv);
                        dv.setTypeCharacter();
                        dv.setIntervalDiscrete();
                    }

                    dataTable.setDataVariables(variableList);
                    isNumericVariable = new boolean[columnCount];

                    for (int i = 0; i < columnCount; i++) {
                        // OK, let's assume that every variable is numeric;
                        // but we'll go through the file and examine every value; the
                        // moment we find a value that's not a legit numeric one, we'll
                        // assume that it is in fact a String.
                        isNumericVariable[i] = true;
                    }
                    variableHeader = false;
                } else {
                    logger.fine("row mode;");
                    // go through the values and make an educated guess about the
                    // data types:

                    for (int i = 0; i < dataTable.getVarQuantity().intValue(); i++) {
                        if (isNumericVariable[i]) {
                            // If we haven't given up on the "numeric" status of this
                            // variable, let's perform some tests on it, and see if
                            // this value is still a parsable number:
                            if (dataRow[i] != null && (!dataRow[i].equals(""))) {
                                boolean isNumeric = false;
                                if (dataRow[i].equalsIgnoreCase(".")
                                        || dataRow[i].equalsIgnoreCase("NaN")
                                        || dataRow[i].equalsIgnoreCase("NA")
                                        || dataRow[i].equalsIgnoreCase("Inf")
                                        || dataRow[i].equalsIgnoreCase("+Inf")
                                        || dataRow[i].equalsIgnoreCase("-Inf")
                                        || dataRow[i].equalsIgnoreCase("null")) {
                                    isNumeric = true;
                                } else {
                                    try {
                                        Double testDoubleValue = new Double(dataRow[i]);
                                        isNumeric = true;
                                    } catch (Exception ex) {
                                        // the token failed to parse as a double number;
                                        // so we'll have to assume it's just a string variable.
                                    }
                                }
                                if (!isNumeric) {
                                    isNumericVariable[i] = false;
                                }
                            }
                        }
                    }
                    // print out the data row:
                    tempOut.println(StringUtils.join(dataRow, "\t"));
                    caseCount++;
                }
                columnCount = 0;
                dataRow = new String[dataTable.getVarQuantity().intValue()];
            }

            if ("sheetData".equals(name)) {
                dataTable.setCaseQuantity(new Long(caseCount));

                // Re-type the variables that we've determined are numerics:
                for (int i = 0; i < dataTable.getVarQuantity().intValue(); i++) {
                    if (isNumericVariable[i]) {
                        dataTable.getDataVariables().get(i).setTypeNumeric();
                        dataTable.getDataVariables().get(i).setIntervalContinuous();
                    }
                }
                tempOut.close();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            cellContents += new String(ch, start, length);
        }
    }
}
