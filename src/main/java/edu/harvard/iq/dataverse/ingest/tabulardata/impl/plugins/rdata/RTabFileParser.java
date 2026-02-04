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
package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata;

import java.io.*;
import java.util.Arrays;
import java.util.logging.*;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.datavariable.DataVariable;

/**
 * This is a customized version of CSVFileReader;
 
 * Tab files saved by R need some special post-processing, unique to the
 * R Ingest, so a specialized version of the file parser was needed. 
 * 
 *
 * @author Leonid Andreev
 *
 */
public class RTabFileParser implements java.io.Serializable {
    private char delimiterChar='\t';

    private static Logger dbgLog =
       Logger.getLogger(RTabFileParser.class.getPackage().getName());


    public RTabFileParser () {
    }

    public RTabFileParser (char delimiterChar) {
        this.delimiterChar = delimiterChar;
    }


    // version of the read method that parses the CSV file and stores
    // its content in the data table matrix (in memory).
    // TODO: remove this method.
    // Only the version that reads the file and stores it in a TAB file
    // should be used.


    public int read(BufferedReader csvReader, DataTable dataTable, boolean saveWithVariableHeader, PrintWriter pwout) throws IOException {
        dbgLog.fine("RTabFileParser: Inside R Tab file parser");
      
        int varQnty = 0;

        try {
            varQnty = dataTable.getVarQuantity().intValue();
        } catch (Exception ex) {
            //return -1;
            throw new IOException (BundleUtil.getStringFromBundle("rtabfileparser.ioexception.parser1"));
        }

        if (varQnty == 0) {
            //return -1;
            throw new IOException (BundleUtil.getStringFromBundle("rtabfileparser.ioexception.parser2"));
        }

        dbgLog.fine("CSV reader; varQnty: "+varQnty);
        dbgLog.fine("CSV reader; delimiter: "+delimiterChar);


        String[] caseRow = new String[varQnty];

        String line;
        String[] valueTokens;

        int lineCounter = 0;

        boolean[] isCharacterVariable = new boolean[varQnty];
        boolean[] isContinuousVariable = new boolean[varQnty];
        boolean[] isTimeVariable = new boolean[varQnty];
        boolean[] isBooleanVariable = new boolean[varQnty];
        
        String variableNameHeader = null;
        
        if (dataTable.getDataVariables() != null) {
            for (int i = 0; i < varQnty; i++) {
                DataVariable var = dataTable.getDataVariables().get(i);
                if (var == null) {
                    throw new IOException ("null dataVariable passed to the parser");
                    
                }
                if (var.getType() == null) {
                    throw new IOException ("null dataVariable type passed to the parser");
                }
                if (var.isTypeCharacter()) {
                    isCharacterVariable[i] = true; 
                    isContinuousVariable[i] = false; 
                    
                    if (var.getFormatCategory() != null && 
                            (var.getFormatCategory().startsWith("date") || var.getFormatCategory().startsWith("time"))) {
                            isTimeVariable[i] = true; 
                        }
                    
                } else if (var.isTypeNumeric()) {
                    isCharacterVariable[i] = false; 
                    
                    if (var.getInterval() == null) {
                        // throw exception!
                    }
                    if (var.isIntervalContinuous()) {
                        isContinuousVariable[i] = true;
                    } else {
                        // discrete by default:
                        isContinuousVariable[i] = false; 
                        if (var.getFormatCategory() != null && var.getFormatCategory().equals("Boolean")) {
                            isBooleanVariable[i] = true; 
                        }
                    }
                } else {
                     throw new IOException ("unknown dataVariable format passed to the parser");
                }
                
                if (saveWithVariableHeader) {
                    variableNameHeader = variableNameHeader == null  
                            ? var.getName() 
                            : variableNameHeader.concat("\t" + var.getName());
                }
            }
        } else {
            throw new IOException ("null dataVariables list passed to the parser");
        }
        
        if (saveWithVariableHeader) {
            if (variableNameHeader == null) {
                throw new IOException ("failed to generate the Variable Names header");
            }
            pwout.println(variableNameHeader);
        }
        
        while ((line = csvReader.readLine()) != null) {
            // chop the line:
            line = line.replaceFirst("[\r\n]*$", "");
            valueTokens = line.split(""+delimiterChar, -2);

            if (valueTokens == null) {
                throw new IOException(BundleUtil.getStringFromBundle("rtabfileparser.ioexception.failed" , Arrays.asList(Integer.toString(lineCounter + 1))));

            }

            if (valueTokens.length != varQnty) {
                throw new IOException(BundleUtil.getStringFromBundle("rtabfileparser.ioexception.mismatch" , Arrays.asList(Integer.toString(lineCounter + 1),Integer.toString(varQnty),Integer.toString(valueTokens.length))));
            }

            //dbgLog.fine("case: "+lineCounter);

            for ( int i = 0; i < varQnty; i++ ) {
                //dbgLog.fine("value: "+valueTokens[i]);

                if (isCharacterVariable[i]) {
                    // String. Adding to the table, quoted.
                    // Empty strings stored as " " (one white space):
                    if (valueTokens[i] != null && (!valueTokens[i].equals(""))) {
                        String charToken = valueTokens[i];
                        // Dealing with quotes: 
                        // remove the leading and trailing quotes, if present:
                        charToken = charToken.replaceFirst("^\"", "");
                        charToken = charToken.replaceFirst("\"$", "");
                        // escape the remaining ones:
                        charToken = charToken.replace("\"", "\\\"");
                        // final pair of quotes:
                        if (isTimeVariable==null || (!isTimeVariable[i])) {
                            charToken = "\"" + charToken + "\"";
                        }
                        caseRow[i] = charToken;
                    } else {
                        // missing value:
                           caseRow[i] = ""; 
                    }

                } else if (isContinuousVariable[i]) {
                    // Numeric, Double:
                    // This is the major case of special/custom processing,
                    // specific for R ingest. It was found to be impossible
                    // to write a numeric/continuous column into the tab file
                    // while unambiguously preserving both NA and NaNs, if both
                    // are present. At least, not if using the standard 
                    // write.table function. So it seemed easier to treat this
                    // as a special case, rather than write our own write.table
                    // equivalent in R. On the R side, if any special values 
                    // are present in the columns, the values will be 
                    // converted into a character vector. The NAs and NaNs will 
                    // be replaced with the character tokens "NA" and "NaN" 
                    // respectively. Of course R will add double quotes around 
                    // the tokens, hence the post-processing - we'll just need 
                    // to remove all these quotes, and then we'll be fine. 
                    
                    dbgLog.fine("R Tab File Parser; double value: "+valueTokens[i]); 
                    // Dealing with quotes: 
                    // remove the leading and trailing quotes, if present:
                    valueTokens[i] = valueTokens[i].replaceFirst("^\"", "");
                    valueTokens[i] = valueTokens[i].replaceFirst("\"$", "");
                    if (valueTokens[i] != null && valueTokens[i].equalsIgnoreCase("NA")) {
                        caseRow[i] = "";
                    } else if (valueTokens[i] != null && valueTokens[i].equalsIgnoreCase("NaN")) {
                        caseRow[i] = "NaN";
                    } else if (valueTokens[i] != null && 
                            ( valueTokens[i].equalsIgnoreCase("Inf")
                            || valueTokens[i].equalsIgnoreCase("+Inf"))) {
                        caseRow[i] = "Inf";
                    } else if (valueTokens[i] != null && valueTokens[i].equalsIgnoreCase("-Inf")) {
                        caseRow[i] = "-Inf";
                    } else {
                        try {
                            Double testDoubleValue = new Double(valueTokens[i]);
                            caseRow[i] = testDoubleValue.toString();//valueTokens[i];
                        } catch (Exception ex) {
                            dbgLog.fine("caught exception reading numeric value; variable: " + i + ", case: " + lineCounter + "; value: " + valueTokens[i]);

                            //dataTable[i][lineCounter] = (new Double(0)).toString();
                            caseRow[i] = "";
                            
                            // TODO:
                            // decide if we should rather throw an exception and exit here; 
                            // all the values in this file at this point must be 
                            // legit numeric values (?) -- L.A.
                        }
                    }
                } else if (isBooleanVariable[i]) {
                    if (valueTokens[i] != null) {
                        String charToken = valueTokens[i];
                        // remove the leading and trailing quotes, if present:
                        charToken = charToken.replaceFirst("^\"", "");
                        charToken = charToken.replaceFirst("\"$", "");
                        
                        if (charToken.equals("FALSE")) {
                            caseRow[i] = "0";
                        } else if (charToken.equals("TRUE")) {
                            caseRow[i] = "1";
                        } else if (charToken.equals("")) {
                            // Legit case - Missing Value!
                            caseRow[i] = charToken;
                        } else {
                            throw new IOException(BundleUtil.getStringFromBundle("rtabfileparser.ioexception.boolean" , Arrays.asList(Integer.toString( +i)))+charToken);
                        }
                    } else {
                        throw new IOException(BundleUtil.getStringFromBundle("rtabfileparser.ioexception.read" , Arrays.asList(Integer.toString(i))));
                    }

                    
                } else {
                    // Numeric, Integer:
                    // One special case first: R NA (missing value) needs to be 
                    // converted into the DVN's missing value - an empty String;
                    // (strictly speaking, this isn't necessary - an attempt to 
                    // create an Integer object from the String "NA" would
                    // result in an exception, that would be intercepted below,
                    // with the same end result)
                    dbgLog.fine("R Tab File Parser; integer value: "+valueTokens[i]);
                    if (valueTokens[i] != null && valueTokens[i].equalsIgnoreCase("NA")) {
                        caseRow[i] = "";
                    } else {
                        try {
                            Integer testIntegerValue = new Integer(valueTokens[i]);
                            caseRow[i] = testIntegerValue.toString();
                        } catch (Exception ex) {
                            dbgLog.fine("caught exception reading numeric value; variable: " + i + ", case: " + lineCounter + "; value: " + valueTokens[i]);

                            //dataTable[i][lineCounter] = "0";
                            caseRow[i] = "";
                        }
                    }
                }
            }

            pwout.println(StringUtils.join(caseRow, "\t"));

            lineCounter++;
        }

        //csvData.setData(dataTable);
        //return csvData;

        pwout.close();
        return lineCounter;
    }

}