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

package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;


/**
 * @author Leonid Andreev
 * original author:
 * @author a.sone
 */
public class TabularSubsetGenerator {

    // -------------------- LOGIC --------------------

    public void subsetFile(InputStream in, String outfile, List<Integer> columns, Long numCases) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            for (long caseIndex = 0; caseIndex < numCases; caseIndex++) {
                line = reader.readLine();
                if (line != null) {
                    String[] values = line.split("\t", -1);
                    List<String> ln = new ArrayList<>();
                    for (Integer i : columns) {
                        ln.add(values[i]);
                    }
                    out.write(StringUtils.join(ln, "\t") + "\n");
                } else {
                    throw new RuntimeException("Tab file has fewer rows than the determined number of cases.");
                }
            }
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    throw new RuntimeException("Tab file has extra nonempty rows than the determined number of cases.");
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Exception while reading file", ioe);
        }
    }

    public static String[][] readFileIntoTable(DataTable dataTable, File generatedTabularFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(generatedTabularFile))) {
            String[][] result = new String[dataTable.getCaseQuantity().intValue()][];
            String line;
            for (int i = 0; i < dataTable.getCaseQuantity(); i++) {
                line = reader.readLine();
                if (line != null) {
                    result[i] = line.split("\t", -1);
                } else {
                    throw new RuntimeException("The file has fewer rows than the determined number of cases.");
                }
            }
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    throw new RuntimeException("The file has extra non-empty rows than the determined number of cases.");
                }
            }
            return result;
        } catch (IOException ioe) {
            throw new RuntimeException("Exception while reading file", ioe);
        }
    }

    public static Double[] subsetDoubleVector(String[][] table, int column, int numCases) {
        Double[] vector = new Double[numCases];

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            String value;
            try {
                value = table[caseIndex][column];
            } catch (ArrayIndexOutOfBoundsException obe) {
                throw new RuntimeException("No data for row " + caseIndex + " and column " + column);
            }
            // Verified: new Double("NaN") works correctly, resulting in Double.NaN; Double("[+-]Inf") doesn't work
            // however – the constructor appears to be expecting it to be spelled as "Infinity", "-Infinity", etc.
            if ("inf".equalsIgnoreCase(value) || "+inf".equalsIgnoreCase(value)) {
                vector[caseIndex] = Double.POSITIVE_INFINITY;
            } else if ("-inf".equalsIgnoreCase(value)) {
                vector[caseIndex] = Double.NEGATIVE_INFINITY;
            } else if (StringUtils.isBlank(value)) {
                vector[caseIndex] = null;
            } else {
                try {
                    vector[caseIndex] = new Double(value);
                } catch (NumberFormatException ex) {
                    vector[caseIndex] = null; // missing value
                }
            }
        }
        return vector;
    }

    public static Float[] subsetFloatVector(String[][] table, int column, int numCases) {
        Float[] vector = new Float[numCases];

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            String value;
            try {
                value = table[caseIndex][column];
            } catch (ArrayIndexOutOfBoundsException obe) {
                throw new RuntimeException("No data for row " + caseIndex + " and column " + column);
            }
            // Verified: new Float("NaN") works correctly, resulting in Float.NaN; Float("[+-]Inf") doesn't work
            // however – the constructor appears to be expecting it to be spelled as "Infinity", "-Infinity", etc.
            if ("inf".equalsIgnoreCase(value) || "+inf".equalsIgnoreCase(value)) {
                vector[caseIndex] = Float.POSITIVE_INFINITY;
            } else if ("-inf".equalsIgnoreCase(value)) {
                vector[caseIndex] = Float.NEGATIVE_INFINITY;
            } else if (value == null || value.equals("")) {
                vector[caseIndex] = null;
            } else {
                try {
                    vector[caseIndex] = new Float(value);
                } catch (NumberFormatException ex) {
                    vector[caseIndex] = null; // missing value
                }
            }
        }
        return vector;
    }

    public static Long[] subsetLongVector(String[][] table, int column, int numCases) {
        Long[] vector = new Long[numCases];
        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            try {
                vector[caseIndex] = Long.valueOf(table[caseIndex][column]);
            } catch (NumberFormatException nfe) {
                vector[caseIndex] = null;
            } catch (ArrayIndexOutOfBoundsException obe) {
                throw new RuntimeException("No data for row " + caseIndex + " and column " + column);
            }
        }
        return vector;
    }

    public static String[] subsetStringVector(String[][] table, int column, int numCases) {
        String[] vector = new String[numCases];

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            String value;
            try {
                value = table[caseIndex][column];
            } catch (ArrayIndexOutOfBoundsException obe) {
                throw new RuntimeException("No data for row " + caseIndex + " and column " + column);
            }

            if (StringUtils.EMPTY.equals(value)) {
                // An empty string is a string missing value!
                // An empty string in quotes is an empty string!
                vector[caseIndex] = null;
            } else {
                // Strip the outer quotes:
                value = value.replaceFirst("^\"", "")
                        .replaceFirst("\"$", "");

                // We need to restore the special characters that are stored in tab files escaped - quotes, new lines
                // and tabs. Before we do that however, we need to take care of any escaped backslashes stored in
                // the tab file. I.e., "foo\t" should be transformed to "foo<TAB>"; but "foo\\t" should be transformed
                // to "foo\t". This way new lines and tabs that were already escaped in the original data are not
                // going to be transformed to unescaped tab and new line characters!
                String[] splitTokens = value.split(Matcher.quoteReplacement("\\\\"), -2);

                // (note that it's important to use the 2-argument version of String.split(), and set the limit argument
                // to a negative value; otherwise any trailing backslashes are lost.)
                for (int i = 0; i < splitTokens.length; i++) {
                    splitTokens[i] = splitTokens[i].replaceAll(Matcher.quoteReplacement("\\\""), "\"")
                            .replaceAll(Matcher.quoteReplacement("\\t"), "\t")
                            .replaceAll(Matcher.quoteReplacement("\\n"), "\n")
                            .replaceAll(Matcher.quoteReplacement("\\r"), "\r");
                }
                // TODO:
                // Make (some of?) the above optional; for ex., we do need to restore the newlines when calculating
                // UNFs; But if we are subsetting these vectors in order to create a new tab-delimited file, they will
                // actually break things! -- L.A. Jul. 28 2014

                value = StringUtils.join(splitTokens, '\\');

                vector[caseIndex] = value;
            }
        }
        return vector;
    }
}


