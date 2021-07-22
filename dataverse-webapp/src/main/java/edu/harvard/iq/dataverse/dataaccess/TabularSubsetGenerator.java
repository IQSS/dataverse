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

import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;


/**
 * @author Leonid Andreev
 * original author:
 * @author a.sone
 */

public class TabularSubsetGenerator {

    public void subsetFile(InputStream in, String outfile, List<Integer> columns, Long numCases,
                           String delimiter) {
        try {
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\n");

            BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
            for (long caseIndex = 0; caseIndex < numCases; caseIndex++) {
                if (scanner.hasNext()) {
                    String[] line = (scanner.next()).split(delimiter, -1);
                    List<String> ln = new ArrayList<String>();
                    for (Integer i : columns) {
                        ln.add(line[i]);
                    }
                    out.write(StringUtils.join(ln, "\t") + "\n");
                } else {
                    throw new RuntimeException("Tab file has fewer rows than the determined number of cases.");
                }
            }

            while (scanner.hasNext()) {
                if (!"".equals(scanner.next())) {
                    throw new RuntimeException("Tab file has extra nonempty rows than the determined number of cases.");

                }
            }

            scanner.close();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
     * Straightforward method for subsetting a column; inefficient on large
     * files, OK to use on small files:
     */

    public static Double[] subsetDoubleVector(InputStream in, int column, int numCases) {
        Double[] retVector = new Double[numCases];
        Scanner scanner = new Scanner(in);
        scanner.useDelimiter("\\n");

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            if (scanner.hasNext()) {
                String[] line = (scanner.next()).split("\t", -1);

                // Verified: new Double("nan") works correctly,
                // resulting in Double.NaN;
                // Double("[+-]Inf") doesn't work however;
                // (the constructor appears to be expecting it
                // to be spelled as "Infinity", "-Infinity", etc.
                if ("inf".equalsIgnoreCase(line[column]) || "+inf".equalsIgnoreCase(line[column])) {
                    retVector[caseIndex] = java.lang.Double.POSITIVE_INFINITY;
                } else if ("-inf".equalsIgnoreCase(line[column])) {
                    retVector[caseIndex] = java.lang.Double.NEGATIVE_INFINITY;
                } else if (line[column] == null || line[column].equals("")) {
                    // missing value:
                    retVector[caseIndex] = null;
                } else {
                    try {
                        retVector[caseIndex] = new Double(line[column]);
                    } catch (NumberFormatException ex) {
                        retVector[caseIndex] = null; // missing value
                    }
                }

            } else {
                scanner.close();
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
        }

        int tailIndex = numCases;
        while (scanner.hasNext()) {
            String nextLine = scanner.next();
            if (!"".equals(nextLine)) {
                scanner.close();
                throw new RuntimeException("Column " + column + ": tab file has more nonempty rows than the stored number of cases (" + numCases + ")! current index: " + tailIndex + ", line: " + nextLine);
            }
            tailIndex++;
        }

        scanner.close();
        return retVector;

    }

    /*
     * Same deal as with the method above - straightforward, but (potentially) slow.
     * Not a resource hog though - will only try to store one vector in memory.
     */
    public static Float[] subsetFloatVector(InputStream in, int column, int numCases) {
        Float[] retVector = new Float[numCases];
        Scanner scanner = new Scanner(in);
        scanner.useDelimiter("\\n");

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            if (scanner.hasNext()) {
                String[] line = (scanner.next()).split("\t", -1);
                // Verified: new Float("nan") works correctly,
                // resulting in Float.NaN;
                // Float("[+-]Inf") doesn't work however;
                // (the constructor appears to be expecting it
                // to be spelled as "Infinity", "-Infinity", etc.
                if ("inf".equalsIgnoreCase(line[column]) || "+inf".equalsIgnoreCase(line[column])) {
                    retVector[caseIndex] = java.lang.Float.POSITIVE_INFINITY;
                } else if ("-inf".equalsIgnoreCase(line[column])) {
                    retVector[caseIndex] = java.lang.Float.NEGATIVE_INFINITY;
                } else if (line[column] == null || line[column].equals("")) {
                    // missing value:
                    retVector[caseIndex] = null;
                } else {
                    try {
                        retVector[caseIndex] = new Float(line[column]);
                    } catch (NumberFormatException ex) {
                        retVector[caseIndex] = null; // missing value
                    }
                }
            } else {
                scanner.close();
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
        }

        int tailIndex = numCases;
        while (scanner.hasNext()) {
            String nextLine = scanner.next();
            if (!"".equals(nextLine)) {
                scanner.close();
                throw new RuntimeException("Column " + column + ": tab file has more nonempty rows than the stored number of cases (" + numCases + ")! current index: " + tailIndex + ", line: " + nextLine);
            }
            tailIndex++;
        }

        scanner.close();
        return retVector;

    }

    /*
     * Same deal as with the method above - straightforward, but (potentially) slow.
     * Not a resource hog though - will only try to store one vector in memory.
     */
    public static Long[] subsetLongVector(InputStream in, int column, int numCases) {
        Long[] retVector = new Long[numCases];
        Scanner scanner = new Scanner(in);
        scanner.useDelimiter("\\n");

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            if (scanner.hasNext()) {
                String[] line = (scanner.next()).split("\t", -1);
                try {
                    retVector[caseIndex] = Long.valueOf(line[column]);
                } catch (NumberFormatException ex) {
                    retVector[caseIndex] = null; // assume missing value
                }
            } else {
                scanner.close();
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
        }

        int tailIndex = numCases;
        while (scanner.hasNext()) {
            String nextLine = scanner.next();
            if (!"".equals(nextLine)) {
                scanner.close();
                throw new RuntimeException("Column " + column + ": tab file has more nonempty rows than the stored number of cases (" + numCases + ")! current index: " + tailIndex + ", line: " + nextLine);
            }
            tailIndex++;
        }

        scanner.close();
        return retVector;

    }

    /*
     * Same deal as with the method above - straightforward, but (potentially) slow.
     * Not a resource hog though - will only try to store one vector in memory.
     */
    public static String[] subsetStringVector(InputStream in, int column, int numCases) {
        String[] retVector = new String[numCases];
        Scanner scanner = new Scanner(in);
        scanner.useDelimiter("\\n");

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            if (scanner.hasNext()) {
                String[] line = (scanner.next()).split("\t", -1);
                retVector[caseIndex] = line[column];


                if ("".equals(line[column])) {
                    // An empty string is a string missing value!
                    // An empty string in quotes is an empty string!
                    retVector[caseIndex] = null;
                } else {
                    // Strip the outer quotes:
                    line[column] = line[column].replaceFirst("^\\\"", "");
                    line[column] = line[column].replaceFirst("\\\"$", "");

                    // We need to restore the special characters that
                    // are stored in tab files escaped - quotes, new lines
                    // and tabs. Before we do that however, we need to
                    // take care of any escaped backslashes stored in
                    // the tab file. I.e., "foo\t" should be transformed
                    // to "foo<TAB>"; but "foo\\t" should be transformed
                    // to "foo\t". This way new lines and tabs that were
                    // already escaped in the original data are not
                    // going to be transformed to unescaped tab and
                    // new line characters!
                    String[] splitTokens = line[column].split(Matcher.quoteReplacement("\\\\"), -2);

                    // (note that it's important to use the 2-argument version
                    // of String.split(), and set the limit argument to a
                    // negative value; otherwise any trailing backslashes
                    // are lost.)
                    for (int i = 0; i < splitTokens.length; i++) {
                        splitTokens[i] = splitTokens[i].replaceAll(Matcher.quoteReplacement("\\\""), "\"");
                        splitTokens[i] = splitTokens[i].replaceAll(Matcher.quoteReplacement("\\t"), "\t");
                        splitTokens[i] = splitTokens[i].replaceAll(Matcher.quoteReplacement("\\n"), "\n");
                        splitTokens[i] = splitTokens[i].replaceAll(Matcher.quoteReplacement("\\r"), "\r");
                    }
                    // TODO:
                    // Make (some of?) the above optional; for ex., we
                    // do need to restore the newlines when calculating UNFs;
                    // But if we are subsetting these vectors in order to
                    // create a new tab-delimited file, they will
                    // actually break things! -- L.A. Jul. 28 2014

                    line[column] = StringUtils.join(splitTokens, '\\');

                    retVector[caseIndex] = line[column];
                }

            } else {
                scanner.close();
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
        }

        int tailIndex = numCases;
        while (scanner.hasNext()) {
            String nextLine = scanner.next();
            if (!"".equals(nextLine)) {
                scanner.close();
                throw new RuntimeException("Column " + column + ": tab file has more nonempty rows than the stored number of cases (" + numCases + ")! current index: " + tailIndex + ", line: " + nextLine);
            }
            tailIndex++;
        }

        scanner.close();
        return retVector;

    }
}


