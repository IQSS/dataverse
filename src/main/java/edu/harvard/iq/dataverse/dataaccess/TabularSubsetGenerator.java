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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;


import org.apache.commons.lang3.StringUtils;


/**
 * 
 * @author Leonid Andreev
 * original author:
 * @author a.sone
 */
 
public class TabularSubsetGenerator implements SubsetGenerator {

    private static Logger logger = Logger.getLogger(TabularSubsetGenerator.class.getPackage().getName());

    //private static int MAX_COLUMN_BUFFER = 8192;
        
    public TabularSubsetGenerator() {
        
    }
    
    /**
     * This class used to be much more complex. There were methods for subsetting
     * from fixed-width field files; including using the optimized, "90 deg. rotated"
     * versions of such files (i.e. you create a *columns-wise* copy of your data 
     * file in which the columns are stored sequentially, and a table of byte 
     * offsets of each column. You can then read individual variable columns 
     * for cheap; at the expense of doubling the storage size of your tabular 
     * data files. These methods were not used, so they were deleted (in Jan. 2024
     * prior to 6.2.
     * Please consult git history if you are interested in looking at that code. 
     */
        
    public void subsetFile(String infile, String outfile, List<Integer> columns, Long numCases) {
        subsetFile(infile, outfile, columns, numCases, "\t");
    }

    public void subsetFile(String infile, String outfile, List<Integer> columns, Long numCases,
        String delimiter) {
        try (FileInputStream fis = new FileInputStream(new File(infile))){
            subsetFile(fis, outfile, columns, numCases, delimiter);
        } catch (IOException ex) {
            throw new RuntimeException("Could not open file "+infile);
        }
    }


    public void subsetFile(InputStream in, String outfile, List<Integer> columns, Long numCases,
        String delimiter) {
          try (Scanner scanner = new Scanner(in); BufferedWriter out = new BufferedWriter(new FileWriter(outfile))) {
            scanner.useDelimiter("\\n");

                for (long caseIndex = 0; caseIndex < numCases; caseIndex++) {
                    if (scanner.hasNext()) {
                        String[] line = (scanner.next()).split(delimiter,-1);
                        List<String> ln = new ArrayList<String>();
                        for (Integer i : columns) {
                            ln.add(line[i]);
                        }
                        out.write(StringUtils.join(ln,"\t")+"\n");
                    } else {
                        throw new RuntimeException("Tab file has fewer rows than the determined number of cases.");
                    }
                }

              while (scanner.hasNext()) {
                  if (!"".equals(scanner.next()) ) {
                      throw new RuntimeException("Tab file has extra nonempty rows than the determined number of cases.");

                  }
              }

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
    
    public static Double[] subsetDoubleVector(InputStream in, int column, int numCases, boolean skipHeader) {
        Double[] retVector = new Double[numCases];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            if (skipHeader) {
                reader.readLine(); // Skip the header line
            }
            
            String line;
            int caseIndex = 0;
            while ((line = reader.readLine()) != null && caseIndex < numCases) {
                String[] fields = line.split("\t", -1);
                if (fields.length > column) {
                    retVector[caseIndex] = parseDoubleValue(fields[column]);
                } else {
                    throw new RuntimeException("Column index out of bounds");
                }
                caseIndex++;
            }
    
            if (caseIndex < numCases) {
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
    
            // Check for extra non-empty lines
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    throw new RuntimeException("Tab file has more nonempty rows than the stored number of cases (" + numCases + ")!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading from input stream", e);
        }
        return retVector;
    }
    
    private static Double parseDoubleValue(String value) {
        if (value == null || value.isEmpty()) {
            return null; // missing value
        }
        value = value.toLowerCase();
        if ("inf".equals(value) || "+inf".equals(value)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-inf".equals(value)) {
            return Double.NEGATIVE_INFINITY;
        } else {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                return null; // missing value
            }
        }
    }
    
    /*
     * Same deal as with the method above - straightforward, but (potentially) slow. 
     * Not a resource hog though - will only try to store one vector in memory. 
     */
    public static Float[] subsetFloatVector(InputStream in, int column, int numCases, boolean skipHeader) {
        Float[] retVector = new Float[numCases];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            if (skipHeader) {
                reader.readLine(); // Skip the header line
            }
            
            String line;
            int caseIndex = 0;
            while ((line = reader.readLine()) != null && caseIndex < numCases) {
                String[] fields = line.split("\t", -1);
                if (fields.length > column) {
                    retVector[caseIndex] = parseFloatValue(fields[column]);
                } else {
                    throw new RuntimeException("Column index out of bounds");
                }
                caseIndex++;
            }
    
            if (caseIndex < numCases) {
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
    
            // Check for extra non-empty lines
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    throw new RuntimeException("Tab file has more nonempty rows than the stored number of cases (" + numCases + ")!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading from input stream", e);
        }
        return retVector;
    }
    
    private static Float parseFloatValue(String value) {
        if (value == null || value.isEmpty()) {
            return null; // missing value
        }
        value = value.toLowerCase();
        if ("inf".equals(value) || "+inf".equals(value)) {
            return Float.POSITIVE_INFINITY;
        } else if ("-inf".equals(value)) {
            return Float.NEGATIVE_INFINITY;
        } else {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                return null; // missing value
            }
        }
    }
    
    /*
     * Same deal as with the method above - straightforward, but (potentially) slow. 
     * Not a resource hog though - will only try to store one vector in memory. 
     */
    public static Long[] subsetLongVector(InputStream in, int column, int numCases, boolean skipHeader) {
        Long[] retVector = new Long[numCases];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            if (skipHeader) {
                reader.readLine(); // Skip the header line
            }
            
            String line;
            int caseIndex = 0;
            while ((line = reader.readLine()) != null && caseIndex < numCases) {
                String[] fields = line.split("\t", -1);
                if (fields.length > column) {
                    retVector[caseIndex] = parseLongValue(fields[column]);
                } else {
                    throw new RuntimeException("Column index out of bounds");
                }
                caseIndex++;
            }
    
            if (caseIndex < numCases) {
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
    
            // Check for extra non-empty lines
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    throw new RuntimeException("Tab file has more nonempty rows than the stored number of cases (" + numCases + ")!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading from input stream", e);
        }
        return retVector;
    }
    
    private static Long parseLongValue(String value) {
        if (value == null || value.isEmpty()) {
            return null; // missing value
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null; // missing value
        }
    }
    
    /*
     * Same deal as with the method above - straightforward, but (potentially) slow. 
     * Not a resource hog though - will only try to store one vector in memory. 
     */
    public static String[] subsetStringVector(InputStream in, int column, int numCases, boolean skipHeader) {
        String[] retVector = new String[numCases];
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            if (skipHeader) {
                reader.readLine(); // Skip the header line
            }
            
            String line;
            int caseIndex = 0;
            while ((line = reader.readLine()) != null && caseIndex < numCases) {
                String[] fields = line.split("\t", -1);
                if (fields.length > column) {
                    retVector[caseIndex] = parseStringValue(fields[column]);
                } else {
                    throw new RuntimeException("Column index out of bounds");
                }
                caseIndex++;
            }
    
            if (caseIndex < numCases) {
                throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
            }
    
            // Check for extra non-empty lines
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    throw new RuntimeException("Tab file has more nonempty rows than the stored number of cases (" + numCases + ")!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading from input stream", e);
        }
        return retVector;
    }
    
    private static String parseStringValue(String value) {
        if (value.isEmpty() || "".equals(value)) {
            return null; // An empty string is a string missing value
        }
        // Strip the outer quotes:
        value = value.replaceFirst("^\\\"", "").replaceFirst("\\\"$", "");
    
        // Unescape special characters
        String[] splitTokens = value.split(Matcher.quoteReplacement("\\\\"), -2);
        for (int i = 0; i < splitTokens.length; i++) {
            splitTokens[i] = splitTokens[i].replaceAll(Matcher.quoteReplacement("\\\""), "\"")
                                           .replaceAll(Matcher.quoteReplacement("\\t"), "\t")
                                           .replaceAll(Matcher.quoteReplacement("\\n"), "\n")
                                           .replaceAll(Matcher.quoteReplacement("\\r"), "\r");
        }
        return StringUtils.join(splitTokens, '\\');
    }
}