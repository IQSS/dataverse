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


import java.util.*;
import java.util.Scanner;
import java.util.logging.*;
import java.io.*;
import java.io.FileNotFoundException;
import org.apache.commons.lang.*;

//import edu.harvard.iq.dvn.ingest.dsb.*;


/**
 * 
 * @author Leonid Andreev
 * original author:
 * @author a.sone
 */
 
public class TabularSubsetGenerator implements SubsetGenerator {

    private static Logger dbgLog = Logger.getLogger(TabularSubsetGenerator.class.getPackage().getName());

       
    public  void subsetFile(String infile, String outfile, Set<Integer> columns, Long numCases) {
        subsetFile(infile, outfile, columns, numCases, "\t");
    }

    public void subsetFile(String infile, String outfile, Set<Integer> columns, Long numCases,
        String delimiter) {
        try {
            subsetFile(new FileInputStream(new File(infile)), outfile, columns, numCases, delimiter);
        } catch (IOException ex) {
            throw new RuntimeException("Could not open file "+infile);
        }
    }


    public void subsetFile(InputStream in, String outfile, Set<Integer> columns, Long numCases,
        String delimiter) {
        try {
          Scanner scanner =  new Scanner(in);
          scanner.useDelimiter("\\n");

          BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
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
                try {
                    retVector[caseIndex] = new Double(line[column]);
                } catch (NumberFormatException ex) {
                    retVector[caseIndex] = null; // missing value
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
                throw new RuntimeException("Column "+column+": tab file has more nonempty rows than the stored number of cases ("+numCases+")! current index: "+tailIndex+", line: "+nextLine);
            }
            tailIndex++;
        }

        scanner.close();
        return retVector;

    }

    /*
     * Straightforward method for subsetting a tab-delimited data file, extracting
     * all the columns representing continuous variables and returning them as 
     * a 2-dimensional array of Doubles;
     * Inefficient on large files, OK to use on small ones.
     */
    public static Double[][] subsetDoubleVectors(InputStream in, Set<Integer> columns, int numCases) throws IOException {
        Double[][] retVector = new Double[columns.size()][numCases];
        Scanner scanner = new Scanner(in);
        scanner.useDelimiter("\\n");

        for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
            if (scanner.hasNext()) {
                String[] line = (scanner.next()).split("\t", -1);
                int j = 0;
                for (Integer i : columns) {
                    try {
                        // TODO: verify that NaN and +-Inf are going to be
                        // handled correctly here! -- L.A. 
                        retVector[j][caseIndex] = new Double(line[i]);
                    } catch (NumberFormatException ex) {
                        retVector[j][caseIndex] = null; // missing value
                    }
                    j++; 
                }
            } else {
                scanner.close();
                throw new IOException("Tab file has fewer rows than the stored number of cases!");
            }
        }

        int tailIndex = numCases;
        while (scanner.hasNext()) {
            String nextLine = scanner.next();
            if (!"".equals(nextLine)) {
                scanner.close();
                throw new IOException("Tab file has more nonempty rows than the stored number of cases ("+numCases+")! current index: "+tailIndex+", line: "+nextLine);
            }
            tailIndex++;
        }

        scanner.close();
        return retVector;

    }

}
