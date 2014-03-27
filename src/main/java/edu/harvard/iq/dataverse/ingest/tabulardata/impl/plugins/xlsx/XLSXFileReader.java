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


import java.io.*;
import java.io.FileReader;
import java.util.logging.*;
import java.util.*;

import javax.inject.Inject;


import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;

import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.spi.TabularDataFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.StringUtils;

import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 * New (4.0) ingest plugin for Excel/XLSX (XML) spreadsheeets.
 *
 * It utilizes Apache POI framework for reading XLSX data; and uses an
 * event-based, SAX model for parsing the extracted XML. This way spreadsheets
 * of any size can be converted into tab-delimited data with a fairly small 
 * memory footprint.
 * 
 * @author Leonid Andreev
 *
 */
public class XLSXFileReader extends TabularDataFileReader {

    @Inject
    VariableServiceBean varService;

    private static final Logger dbglog = Logger.getLogger(XLSXFileReader.class.getPackage().getName());
    private char delimiterChar = '\t';

    public XLSXFileReader(TabularDataFileReaderSpi originator) {
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
                dbglog.severe("Could not look up initial context, or the variable service in JNDI!");
                //throw new IOException ("Could not look up initial context, or the variable service in JNDI!"); 
            }
        }
    }
    
    /**
     * Reads an XLSX file, converts it into a dataverse DataTable.
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

        File firstPassTempFile = File.createTempFile("firstpass-", ".tab");
        PrintWriter firstPassWriter = new PrintWriter(firstPassTempFile.getAbsolutePath());
        try {
            processSheet(stream, dataTable, firstPassWriter);
        } catch (Exception ex) {
            throw new IOException("Could not parse Excel/XLSX spreadsheet. "+ex.getMessage());
        }

        // 2nd pass:
        
        File tabFileDestination = File.createTempFile("data-", ".tab");
        PrintWriter finalWriter = new PrintWriter(tabFileDestination.getAbsolutePath());
        
        BufferedReader secondPassReader = new BufferedReader(new FileReader(firstPassTempFile));
        
        int varQnty = dataTable.getVarQuantity().intValue();
        int lineCounter = 0;
        String line = null;
        String[] caseRow = new String[varQnty];
        String[] valueTokens;

        
        while ((line = secondPassReader.readLine()) != null) {
            // chop the line:
            line = line.replaceFirst("[\r\n]*$", "");
            valueTokens = line.split("" + delimiterChar, -2);

            if (valueTokens == null) {
                throw new IOException("Failed to read line " + (lineCounter + 1) + " during the second pass.");
            }

            if (valueTokens.length != varQnty) {
                throw new IOException("Reading mismatch, line " + (lineCounter + 1) + " during the second pass: "
                        + varQnty + " delimited values expected, " + valueTokens.length + " found.");
            }
        
            for (int i = 0; i < varQnty; i++) {
                if (isNumericVariable(dataTable.getDataVariables().get(i))) {
                    if (valueTokens[i] == null || valueTokens[i].equals(".") || valueTokens[i].equals("") || valueTokens[i].equalsIgnoreCase("NA")) {
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
                            throw new IOException ("Failed to parse a value recognized as numeric in the first pass! (?)");
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

        secondPassReader.close();
        finalWriter.close();
        
        if (dataTable.getCaseQuantity().intValue() != lineCounter) {
            throw new IOException("Mismatch between line counts in first and final passes!");
        }
        
        dataTable.setUnf("UNF:6:NOTCALCULATED");
        
        ingesteddata.setTabDelimitedFile(tabFileDestination);
        ingesteddata.setDataTable(dataTable);
        
        dbglog.fine("Produced temporary file "+ingesteddata.getTabDelimitedFile().getAbsolutePath());
        dbglog.fine("Found "+dataTable.getVarQuantity()+" variables, "+dataTable.getCaseQuantity()+" observations.");
        String varNames = null;
        for (int i = 0; i<dataTable.getVarQuantity().intValue(); i++) {
            if (varNames == null) {
                varNames = dataTable.getDataVariables().get(i).getName();
            } else {
                varNames = varNames + ", " + dataTable.getDataVariables().get(i).getName();
            }
        }
        dbglog.fine("Variable names: "+varNames);

        
        return ingesteddata;

    }

    private boolean isNumericVariable(DataVariable dataVariable) {
        if (dataVariable.getVariableFormatType() != null) {
            return ("numeric".equals(dataVariable.getVariableFormatType().getName()));
        }
        return false; 
    }
    
    public void processSheet(String filename, DataTable dataTable, PrintWriter tempOut) throws Exception {
        BufferedInputStream xlsxInputStream = new BufferedInputStream(new FileInputStream(new File(filename)));
        processSheet(xlsxInputStream, dataTable, tempOut);
    }

    public void processSheet(InputStream inputStream, DataTable dataTable, PrintWriter tempOut) throws Exception {
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
    
    public XMLReader fetchSheetParser(SharedStringsTable sst, DataTable dataTable, PrintWriter tempOut) throws SAXException {
        // An attempt to use org.apache.xerces.parsers.SAXParser resulted 
        // in some weird conflict in the app; the default XMLReader obtained 
        // from the XMLReaderFactory (from xml-apis.jar) appears to be working
        // just fine. however, 
        // TODO: verify why the app gest built with xml-apis-1.0.b2.jar; it's 
        // an old version - 1.4 seems to be the current release, and 2.0.2
        // (a new development?) appears to be available. We don't specifically
        // request this 1.0.* version, so another package must have it defined
        // as a dependency. We need to verify our dependencies, we most likely 
        // have some hard-coded versions in our pom.xml that are both old and 
        // unnecessary.
        // -- L.A. 4.0 alpha 1
 
        XMLReader xReader = XMLReaderFactory.createXMLReader();
        ContentHandler handler = new SheetHandler(sst, dataTable, varService, tempOut);
        xReader.setContentHandler(handler);
        return xReader;
    }

    private static class SheetHandler extends DefaultHandler {

        private DataTable dataTable;
        private SharedStringsTable sst;
        private String cellContents;
        private boolean nextIsString;
        private boolean variableHeader;
        private List<String> variableNames;
        private int caseCount;
        private int columnCount;
        private VariableServiceBean variableServiceLocal;
        boolean[] isNumericVariable;
        String[] dataRow; 
        PrintWriter tempOut; 

        private SheetHandler(SharedStringsTable sst) {
            this(sst, null, null, null);
        }

        private SheetHandler(SharedStringsTable sst, DataTable dataTable, VariableServiceBean variableService, PrintWriter tempOut) {
            this.sst = sst;
            this.dataTable = dataTable;
            this.variableServiceLocal = variableService;
            this.tempOut = tempOut; 
            variableHeader = true;
            variableNames = new ArrayList<String>(); 
            caseCount = 0; 
            columnCount = 0; 
        }
        
        public void startElement(String uri, String localName, String name,
                Attributes attributes) throws SAXException {
            // c => cell
            if (name.equals("c")) {
                String cellType = attributes.getValue("t");
                if (cellType != null && cellType.equals("s")) {
                    nextIsString = true;
                } else {
                    nextIsString = false;
                }
            }
            // Clear contents cache
            cellContents = "";
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            // Process the content cache as required.
            // Do it now, as characters() may be called more than once
            if (nextIsString) {
                int idx = Integer.parseInt(cellContents);
                cellContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
                nextIsString = false;
            }

            // v => contents of a cell
            // Output after we've seen the string contents
            if (name.equals("v")) {
                if (variableHeader) {
                    variableNames.add(cellContents);
                } else {
                    dataRow[columnCount++] = cellContents;
                }
            }
            
            if (name.equals("row")) {
                if (variableHeader) {
                    // Initialize variables:
                    List<DataVariable> variableList = new ArrayList<DataVariable>();
                    columnCount = variableNames.size();
                    for (int i = 0; i < columnCount; i++) {
                        String varName = variableNames.get(i);

                        if (varName == null || varName.equals("")) {
                            // TODO: 
                            // Add a sensible variable name validation algorithm.
                            // -- L.A. 4.0 alpha 1
                            //throw new IOException ("Invalid variable names in the first line!");
                        }
                        varName = varName.replaceAll("[ _\t\n\r]", "");
                        
                        DataVariable dv = new DataVariable();
                        dv.setName(varName);
                        dv.setLabel(varName);
                        dv.setInvalidRanges(new ArrayList());
                        dv.setSummaryStatistics(new ArrayList());
                        dv.setUnf("UNF:6:NOTCALCULATED");
                        dv.setCategories(new ArrayList());
                        variableList.add(dv);

                        if (variableServiceLocal != null) {
                            dv.setVariableFormatType(variableServiceLocal.findVariableFormatTypeByName("character"));
                            dv.setVariableIntervalType(variableServiceLocal.findVariableIntervalTypeByName("discrete"));
                        }

                        dv.setFileOrder(i);
                        dv.setDataTable(dataTable);
                    }
        
                    dataTable.setVarQuantity(new Long(columnCount));
                    dataTable.setDataVariables(variableList);
                    isNumericVariable = new boolean[columnCount];
                    
                    for (int i=0; i<columnCount; i++) {
                        // OK, let's assume that every variable is numeric; 
                        // but we'll go through the file and examine every value; the 
                        // moment we find a value that's not a legit numeric one, we'll 
                        // assume that it is in fact a String. 
                        isNumericVariable[i] = true; 
                    }
                    columnCount = 0;
                    variableHeader = false; 
                } else {
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
                    columnCount = 0; 
                    caseCount++;
                }
                dataRow = new String[dataTable.getVarQuantity().intValue()];
            }
            
            if (name.equals("sheetData")) {
                dataTable.setCaseQuantity(new Long(caseCount));
            
                // Re-type the variables that we've determined are numerics:
        
                for (int i = 0; i < dataTable.getVarQuantity().intValue(); i++) {
                    if (isNumericVariable[i] && variableServiceLocal != null) {
                        dataTable.getDataVariables().get(i).setVariableFormatType(variableServiceLocal.findVariableFormatTypeByName("numeric"));
                        dataTable.getDataVariables().get(i).setVariableIntervalType(variableServiceLocal.findVariableIntervalTypeByName("continuous"));
                    }
                }
                
                tempOut.close(); 
            }
        }

        public void characters(char[] ch, int start, int length)
                throws SAXException {
            cellContents += new String(ch, start, length);
        }
    }

    public static void main(String[] args) throws Exception {
        XLSXFileReader testReader = new XLSXFileReader(new XLSXFileReaderSpi());
        DataTable dataTable;
        
        BufferedInputStream xlsxInputStream = new BufferedInputStream(new FileInputStream(new File(args[0])));
        
        TabularDataIngest dataIngest = testReader.read(xlsxInputStream, null);
        
        dataTable = dataIngest.getDataTable();
        
        System.out.println("Produced temporary file "+dataIngest.getTabDelimitedFile().getAbsolutePath());
        System.out.println("Found "+dataTable.getVarQuantity()+" variables, "+dataTable.getCaseQuantity()+" observations.");
        System.out.println("Variable names:");
        for (int i = 0; i<dataTable.getVarQuantity().intValue(); i++) {
            System.out.println(dataTable.getDataVariables().get(i).getName());
        }
    }
    

}
