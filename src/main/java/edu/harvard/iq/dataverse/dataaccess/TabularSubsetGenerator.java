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

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.datavariable.DataVariable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    private static Logger dbgLog = Logger.getLogger(TabularSubsetGenerator.class.getPackage().getName());

    private static int COLUMN_TYPE_STRING = 1;
    private static int COLUMN_TYPE_LONG   = 2;
    private static int COLUMN_TYPE_DOUBLE = 3; 
    private static int COLUMN_TYPE_FLOAT = 4; 
    
    private static int MAX_COLUMN_BUFFER = 8192;
    
    private FileChannel fileChannel = null; 
    
    private int varcount; 
    private int casecount; 
    private int subsetcount;
    
    private byte[][] columnEntries = null; 

    
    private ByteBuffer[] columnByteBuffers;
    private int[] columnBufferSizes;
    private int[] columnBufferOffsets;
    
    private long[] columnStartOffsets; 
    private long[] columnTotalOffsets;
    private long[] columnTotalLengths;
    
    public TabularSubsetGenerator() {
        
    }
    
    public TabularSubsetGenerator (DataFile datafile, List<DataVariable> variables) throws IOException {
        if (!datafile.isTabularData()) {
            throw new IOException("DataFile is not tabular data.");
        }
         
        setVarCount(datafile.getDataTable().getVarQuantity().intValue()); 
        setCaseCount(datafile.getDataTable().getCaseQuantity().intValue()); 
        
           
        
        StorageIO<DataFile> dataAccess = datafile.getStorageIO();
        if (!dataAccess.isLocalFile()) {
            throw new IOException("Subsetting is supported on local files only!");
        }
        
        //File tabfile = datafile.getFileSystemLocation().toFile();
        File tabfile = dataAccess.getFileSystemPath().toFile();
        
        File rotatedImageFile = getRotatedImage(tabfile, getVarCount(), getCaseCount());
        long[] columnEndOffsets = extractColumnOffsets(rotatedImageFile, getVarCount(), getCaseCount()); 
        
        fileChannel = (FileChannel.open(Paths.get(rotatedImageFile.getAbsolutePath()), StandardOpenOption.READ));
        
        if (variables == null || variables.size() < 1 || variables.size() > getVarCount()) {
            throw new IOException("Illegal number of variables in the subset request");
        }
        
        subsetcount = variables.size();
        columnTotalOffsets = new long[subsetcount];
        columnTotalLengths = new long[subsetcount];
        columnByteBuffers = new ByteBuffer[subsetcount];


        
        if (subsetcount == 1) {
            if (!datafile.getDataTable().getId().equals(variables.get(0).getDataTable().getId())) {
                throw new IOException("Variable in the subset request does not belong to the datafile.");
            }
            dbgLog.fine("single variable subset; setting fileChannel position to "+extractColumnOffset(columnEndOffsets, variables.get(0).getFileOrder()));
            fileChannel.position(extractColumnOffset(columnEndOffsets, variables.get(0).getFileOrder()));
            columnTotalLengths[0] = extractColumnLength(columnEndOffsets, variables.get(0).getFileOrder());
            columnTotalOffsets[0] = 0;
        } else {
            columnEntries = new byte[subsetcount][];

            columnBufferSizes = new int[subsetcount];
            columnBufferOffsets = new int[subsetcount];
            columnStartOffsets = new long[subsetcount];

            int i = 0;
            for (DataVariable var : variables) {
                if (!datafile.getDataTable().getId().equals(var.getDataTable().getId())) {
                    throw new IOException("Variable in the subset request does not belong to the datafile.");
                }
                columnByteBuffers[i] = ByteBuffer.allocate(MAX_COLUMN_BUFFER);
                columnTotalLengths[i] = extractColumnLength(columnEndOffsets, var.getFileOrder());
                columnStartOffsets[i] = extractColumnOffset(columnEndOffsets, var.getFileOrder());
                if (columnTotalLengths[i] < MAX_COLUMN_BUFFER) {
                    columnByteBuffers[i].limit((int)columnTotalLengths[i]);
                }
                fileChannel.position(columnStartOffsets[i]);
                columnBufferSizes[i] = fileChannel.read(columnByteBuffers[i]);
                columnBufferOffsets[i] = 0;
                columnTotalOffsets[i] = columnBufferSizes[i];
                i++;
            }
        }
    }
    
    private int getVarCount() {
        return varcount;
    }
    
    private void setVarCount(int varcount) {
        this.varcount = varcount; 
    }
    
    private int getCaseCount() {
        return casecount;
    }
    
    private void setCaseCount(int casecount) {
        this.casecount = casecount; 
    }
    
    
    /* 
     * Note that this method operates on the *absolute* column number, i.e.
     * the number of the physical column in the tabular file. This is stored
     * in DataVariable.FileOrder. 
     * This "column number" should not be confused with the number of column 
     * in the subset request; a user can request any number of variable 
     * columns, in an order that doesn't have to follow the physical order
     * of the columns in the file. 
    */
    private long extractColumnOffset(long[] columnEndOffsets, int column) throws IOException {
        if (columnEndOffsets == null || columnEndOffsets.length <= column) {
            throw new IOException("Offsets table not initialized; or column out of bounds.");
        }
        long columnOffset;
        
        if (column > 0) {
            columnOffset = columnEndOffsets[column - 1];
        } else {
            columnOffset = getVarCount() * 8; 
        }
        return columnOffset; 
    }
    
    /* 
     * See the comment for the method above. 
     */
    private long extractColumnLength(long[] columnEndOffsets, int column) throws IOException {
        if (columnEndOffsets == null || columnEndOffsets.length <= column) {
            throw new IOException("Offsets table not initialized; or column out of bounds.");
        }
        long columnLength; 
        
        if (column > 0) {
            columnLength = columnEndOffsets[column] - columnEndOffsets[column - 1];
        } else {
            columnLength = columnEndOffsets[0] - varcount * 8;  
        }
        
        return columnLength; 
    }
      
    
    private void bufferMoreColumnBytes(int column) throws IOException {
        if (columnTotalOffsets[column] >= columnTotalLengths[column]) {
            throw new IOException("attempt to buffer bytes past the column boundary");
        }
        fileChannel.position(columnStartOffsets[column] + columnTotalOffsets[column]);
        
        columnByteBuffers[column].clear();
        if (columnTotalLengths[column] < columnTotalOffsets[column] + MAX_COLUMN_BUFFER) {
            dbgLog.fine("Limiting the buffer to "+(columnTotalLengths[column] - columnTotalOffsets[column])+" bytes");
            columnByteBuffers[column].limit((int) (columnTotalLengths[column] - columnTotalOffsets[column]));
        }
        columnBufferSizes[column] = fileChannel.read(columnByteBuffers[column]);
        dbgLog.fine("Read "+columnBufferSizes[column]+" bytes for subset column "+column);
        columnBufferOffsets[column] = 0;
        columnTotalOffsets[column] += columnBufferSizes[column];
    }
    
    public byte[] readColumnEntryBytes(int column) {
        return readColumnEntryBytes(column, true);
    }
    
    
    public byte[] readColumnEntryBytes(int column, boolean addTabs) {
        byte[] leftover = null; 
        byte[] ret = null; 
        
        if (columnBufferOffsets[column] >= columnBufferSizes[column]) {
            try {
                bufferMoreColumnBytes(column);
                if (columnBufferSizes[column] < 1) {
                    return null;
                }
            } catch (IOException ioe) {
                return null; 
            }
        }
        
        int byteindex = columnBufferOffsets[column];
        try {
            while (columnByteBuffers[column].array()[byteindex] != '\n') {
                byteindex++;
                if (byteindex == columnBufferSizes[column]) {
                    // save the leftover: 
                    if (leftover == null) {
                        leftover = new byte[columnBufferSizes[column] - columnBufferOffsets[column]];
                        System.arraycopy(columnByteBuffers[column].array(), columnBufferOffsets[column], leftover, 0, columnBufferSizes[column] - columnBufferOffsets[column]);
                    } else {
                        byte[] merged = new byte[leftover.length + columnBufferSizes[column]];
                        
                        System.arraycopy(leftover, 0, merged, 0, leftover.length);
                        System.arraycopy(columnByteBuffers[column].array(), 0, merged, leftover.length, columnBufferSizes[column]);
                        leftover = merged;
                        merged = null; 
                    }
                    // read more bytes:
                    bufferMoreColumnBytes(column);
                    if (columnBufferSizes[column] < 1) {
                        return null;
                    }
                    byteindex = 0;
                }
            }

            // presumably, we have found our '\n':
            if (leftover == null) {
                ret = new byte[byteindex - columnBufferOffsets[column] + 1];
                System.arraycopy(columnByteBuffers[column].array(), columnBufferOffsets[column], ret, 0, byteindex - columnBufferOffsets[column] + 1);
            } else {
                ret = new byte[leftover.length + byteindex + 1];
                System.arraycopy(leftover, 0, ret, 0, leftover.length);
                System.arraycopy(columnByteBuffers[column].array(), 0, ret, leftover.length, byteindex + 1);
            }

        } catch (IOException ioe) {
            return null;
        }

        columnBufferOffsets[column] = (byteindex + 1);

        if (column < columnBufferOffsets.length - 1) {
            ret[ret.length - 1] = '\t';
        }
        return ret;
    }
    
    public int readSingleColumnSubset(byte[] buffer) throws IOException {
        if (columnTotalOffsets[0] == columnTotalLengths[0]) {
            return -1;
        }
        
        if (columnByteBuffers[0] == null) {
            dbgLog.fine("allocating single column subset buffer.");
            columnByteBuffers[0] = ByteBuffer.allocate(buffer.length);
        }
                
        int bytesread = fileChannel.read(columnByteBuffers[0]);
        dbgLog.fine("single column subset: read "+bytesread+" bytes.");
        if (columnTotalOffsets[0] + bytesread > columnTotalLengths[0]) {
            bytesread = (int)(columnTotalLengths[0] - columnTotalOffsets[0]);
        }
        System.arraycopy(columnByteBuffers[0].array(), 0, buffer, 0, bytesread);

        columnTotalOffsets[0] += bytesread;
        columnByteBuffers[0].clear();
        return bytesread > 0 ? bytesread : -1;
    }
    
    
    public byte[] readSubsetLineBytes() throws IOException {
        byte[] ret = null; 
        int total = 0; 
        
        for (int i = 0; i < subsetcount; i++) {
            columnEntries[i] = readColumnEntryBytes(i);
            if (columnEntries[i] == null) {
                throw new IOException("Failed to read subset line entry");
            }
            total += columnEntries[i].length;
        }
        
        ret = new byte[total];
        int offset = 0;
        for (int i = 0; i < subsetcount; i++) {
            System.arraycopy(columnEntries[i], 0, ret, offset, columnEntries[i].length);
            offset += columnEntries[i].length;
        }
        dbgLog.fine("line: "+new String(ret));
        return ret;
    } 
    
    
    public void close() {
        if (fileChannel != null) {
            try {
                fileChannel.close();
            } catch (IOException ioe) {
                // don't care.
            }
        }
    }
    
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
    
    public static Double[] subsetDoubleVector(InputStream in, int column, int numCases) {
        Double[] retVector = new Double[numCases];
        try (Scanner scanner = new Scanner(in)) {
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
                    throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
                }
            }

            int tailIndex = numCases;
            while (scanner.hasNext()) {
                String nextLine = scanner.next();
                if (!"".equals(nextLine)) {
                    throw new RuntimeException("Column " + column + ": tab file has more nonempty rows than the stored number of cases (" + numCases + ")! current index: " + tailIndex + ", line: " + nextLine);
                }
                tailIndex++;
            }

        }
        return retVector;

    }
    
    /*
     * Same deal as with the method above - straightforward, but (potentially) slow. 
     * Not a resource hog though - will only try to store one vector in memory. 
     */
    public static Float[] subsetFloatVector(InputStream in, int column, int numCases) {
        Float[] retVector = new Float[numCases];
        try (Scanner scanner = new Scanner(in)) {
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
                    throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
                }
            }

            int tailIndex = numCases;
            while (scanner.hasNext()) {
                String nextLine = scanner.next();
                if (!"".equals(nextLine)) {
                    throw new RuntimeException("Column "+column+": tab file has more nonempty rows than the stored number of cases ("+numCases+")! current index: "+tailIndex+", line: "+nextLine);
                }
                tailIndex++;
            }

        }
        return retVector;

    }
    
    /*
     * Same deal as with the method above - straightforward, but (potentially) slow. 
     * Not a resource hog though - will only try to store one vector in memory. 
     */
    public static Long[] subsetLongVector(InputStream in, int column, int numCases) {
        Long[] retVector = new Long[numCases];
        try (Scanner scanner = new Scanner(in)) {
            scanner.useDelimiter("\\n");

            for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
                if (scanner.hasNext()) {
                    String[] line = (scanner.next()).split("\t", -1);
                    try {
                        retVector[caseIndex] = new Long(line[column]);
                    } catch (NumberFormatException ex) {
                        retVector[caseIndex] = null; // assume missing value
                    }
                } else {
                    throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
                }
            }

            int tailIndex = numCases;
            while (scanner.hasNext()) {
                String nextLine = scanner.next();
                if (!"".equals(nextLine)) {
                    throw new RuntimeException("Column "+column+": tab file has more nonempty rows than the stored number of cases ("+numCases+")! current index: "+tailIndex+", line: "+nextLine);
                }
                tailIndex++;
            }

        }
        return retVector;

    }
    
    /*
     * Same deal as with the method above - straightforward, but (potentially) slow. 
     * Not a resource hog though - will only try to store one vector in memory. 
     */
    public static String[] subsetStringVector(InputStream in, int column, int numCases) {
        String[] retVector = new String[numCases];
        try (Scanner scanner = new Scanner(in)) {
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
                    throw new RuntimeException("Tab file has fewer rows than the stored number of cases!");
                }
            }

            int tailIndex = numCases;
            while (scanner.hasNext()) {
                String nextLine = scanner.next();
                if (!"".equals(nextLine)) {
                    throw new RuntimeException("Column "+column+": tab file has more nonempty rows than the stored number of cases ("+numCases+")! current index: "+tailIndex+", line: "+nextLine);
                }
                tailIndex++;
            }

        }
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
        try (Scanner scanner = new Scanner(in)) {
            scanner.useDelimiter("\\n");

            for (int caseIndex = 0; caseIndex < numCases; caseIndex++) {
                if (scanner.hasNext()) {
                    String[] line = (scanner.next()).split("\t", -1);
                    int j = 0;
                    for (Integer i : columns) {
                        try {
                            // TODO: verify that NaN and +-Inf are going to be
                            // handled correctly here! -- L.A.
                            // NO, "+-Inf" is not handled correctly; see the
                            // comment further down below.
                            retVector[j][caseIndex] = new Double(line[i]);
                        } catch (NumberFormatException ex) {
                            retVector[j][caseIndex] = null; // missing value
                        }
                        j++;
                    }
                } else {
                    throw new IOException("Tab file has fewer rows than the stored number of cases!");
                }
            }

            int tailIndex = numCases;
            while (scanner.hasNext()) {
                String nextLine = scanner.next();
                if (!"".equals(nextLine)) {
                    throw new IOException("Tab file has more nonempty rows than the stored number of cases ("+numCases+")! current index: "+tailIndex+", line: "+nextLine);
                }
                tailIndex++;
            }

        }
        return retVector;

    }
    
    public String[] subsetStringVector(DataFile datafile, int column) throws IOException {
        return (String[])subsetObjectVector(datafile, column, COLUMN_TYPE_STRING);
    }
    
    public Double[] subsetDoubleVector(DataFile datafile, int column) throws IOException {
        return (Double[])subsetObjectVector(datafile, column, COLUMN_TYPE_DOUBLE);
    }
    
    public Long[] subsetLongVector(DataFile datafile, int column) throws IOException {
        return (Long[])subsetObjectVector(datafile, column, COLUMN_TYPE_LONG);
    }
    
    // Float methods are temporary; 
    // In normal operations we'll be treating all the floating point types as 
    // doubles. I need to be able to handle floats for some 4.0 vs 3.* ingest
    // tests. -- L.A. 
    
    public Float[] subsetFloatVector(DataFile datafile, int column) throws IOException {
        return (Float[])subsetObjectVector(datafile, column, COLUMN_TYPE_FLOAT);
    }
    
    public String[] subsetStringVector(File tabfile, int column, int varcount, int casecount) throws IOException {
        return (String[])subsetObjectVector(tabfile, column, varcount, casecount, COLUMN_TYPE_STRING);
    }
    
    public Double[] subsetDoubleVector(File tabfile, int column, int varcount, int casecount) throws IOException {
        return (Double[])subsetObjectVector(tabfile, column, varcount, casecount, COLUMN_TYPE_DOUBLE);
    }
    
    public Long[] subsetLongVector(File tabfile, int column, int varcount, int casecount) throws IOException {
        return (Long[])subsetObjectVector(tabfile, column, varcount, casecount, COLUMN_TYPE_LONG);
    }
    
    public Float[] subsetFloatVector(File tabfile, int column, int varcount, int casecount) throws IOException {
        return (Float[])subsetObjectVector(tabfile, column, varcount, casecount, COLUMN_TYPE_FLOAT);
    }
    
    public Object[] subsetObjectVector(DataFile dataFile, int column, int columntype) throws IOException {
        if (!dataFile.isTabularData()) {
            throw new IOException("DataFile is not tabular data.");
        }
         
        int varcount = dataFile.getDataTable().getVarQuantity().intValue(); 
        int casecount = dataFile.getDataTable().getCaseQuantity().intValue(); 
        
        if (column >= varcount) {
            throw new IOException("Column "+column+" is out of bounds.");
        }
        
        StorageIO<DataFile> dataAccess = dataFile.getStorageIO();
        if (!dataAccess.isLocalFile()) {
            throw new IOException("Subsetting is supported on local files only!");
        }
        
        //File tabfile = datafile.getFileSystemLocation().toFile();
        File tabfile = dataAccess.getFileSystemPath().toFile();

        if (columntype == COLUMN_TYPE_STRING) {
            String filename = dataFile.getFileMetadata().getLabel();
            if (filename != null) {
                filename = filename.replaceFirst("^_", "");
                Integer fnumvalue = null; 
                try {
                    fnumvalue = new Integer(filename);
                } catch (Exception ex){
                    fnumvalue = null; 
                }
                if (fnumvalue != null) {
                    //if ((fnumvalue.intValue() < 112497)) { // && (fnumvalue.intValue() > 60015)) {
                    if ((fnumvalue.intValue() < 111931)) { // && (fnumvalue.intValue() > 60015)) {
                        if (!(fnumvalue.intValue() == 60007
                                || fnumvalue.intValue() == 59997
                                || fnumvalue.intValue() == 60015
                                || fnumvalue.intValue() == 59948
                                || fnumvalue.intValue() == 60012
                                || fnumvalue.intValue() == 52585
                                || fnumvalue.intValue() == 60005
                                || fnumvalue.intValue() == 60002
                                || fnumvalue.intValue() == 59954
                                || fnumvalue.intValue() == 60008
                                || fnumvalue.intValue() == 54972
                                || fnumvalue.intValue() == 55010
                                || fnumvalue.intValue() == 54996
                                || fnumvalue.intValue() == 53527
                                || fnumvalue.intValue() == 53546
                                || fnumvalue.intValue() == 55002
                                || fnumvalue.intValue() == 55006
                                || fnumvalue.intValue() == 54998
                                || fnumvalue.intValue() == 52552
                                // SPSS/SAV cases with similar issue - compat mode must be disabled
                                //|| fnumvalue.intValue() == 101826 // temporary - tricky file with accents and v. 16...
                                || fnumvalue.intValue() == 54618 // another SAV file, with long strings...
                                || fnumvalue.intValue() == 54619 // [same]
                                || fnumvalue.intValue() == 57983 
                                || fnumvalue.intValue() == 58262
                                || fnumvalue.intValue() == 58288
                                || fnumvalue.intValue() == 58656
                                || fnumvalue.intValue() == 59144
                                // || fnumvalue.intValue() == 69626 [nope!]
                                )) {
                            dbgLog.info("\"Old\" file name detected; using \"compatibility mode\" for a character vector subset;");
                            return subsetObjectVector(tabfile, column, varcount, casecount, columntype, true);
                        }
                    }
                }
            }
        }

        return subsetObjectVector(tabfile, column, varcount, casecount, columntype);
    }
    
    public Object[] subsetObjectVector(File tabfile, int column, int varcount, int casecount, int columntype) throws IOException {
        return subsetObjectVector(tabfile, column, varcount, casecount, columntype, false);
    }
    
    
    
    public Object[] subsetObjectVector(File tabfile, int column, int varcount, int casecount, int columntype, boolean compatmode) throws IOException {
        
        Object[] retVector = null; 
        
        boolean isString = false; 
        boolean isDouble = false;
        boolean isLong   = false; 
        boolean isFloat  = false; 
        
        //Locale loc = new Locale("en", "US");
        
        if (columntype == COLUMN_TYPE_STRING) {
            isString = true; 
            retVector = new String[casecount];
        } else if (columntype == COLUMN_TYPE_DOUBLE) {
            isDouble = true; 
            retVector = new Double[casecount];
        } else if (columntype == COLUMN_TYPE_LONG) {
            isLong = true; 
            retVector = new Long[casecount];
        } else if (columntype == COLUMN_TYPE_FLOAT){
            isFloat = true;
            retVector = new Float[casecount];
        } else {
            throw new IOException("Unsupported column type: "+columntype);
        }
        
        File rotatedImageFile = getRotatedImage(tabfile, varcount, casecount);
        long[] columnEndOffsets = extractColumnOffsets(rotatedImageFile, varcount, casecount); 
        long columnOffset = 0; 
        long columnLength = 0; 
        
        if (column > 0) {
            columnOffset = columnEndOffsets[column - 1];
            columnLength = columnEndOffsets[column] - columnEndOffsets[column - 1];
        } else {
            columnOffset = varcount * 8; 
            columnLength = columnEndOffsets[0] - varcount * 8;  
        }
        int caseindex = 0;
        
        try (FileChannel fc = (FileChannel.open(Paths.get(rotatedImageFile.getAbsolutePath()),
                StandardOpenOption.READ))) {
            fc.position(columnOffset);
            int MAX_COLUMN_BUFFER = 8192;

            ByteBuffer in = ByteBuffer.allocate(MAX_COLUMN_BUFFER);

            if (columnLength < MAX_COLUMN_BUFFER) {
                in.limit((int) (columnLength));
            }

            long bytesRead = 0;
            long bytesReadTotal = 0;

            int byteoffset = 0;
            byte[] leftover = null;

            while (bytesReadTotal < columnLength) {
                bytesRead = fc.read(in);
                byte[] columnBytes = in.array();
                int bytecount = 0;

                while (bytecount < bytesRead) {
                    if (columnBytes[bytecount] == '\n') {
                        /*
                        String token = new String(columnBytes, byteoffset, bytecount-byteoffset, "UTF8");

                        if (leftover != null) {
                            String leftoverString = new String (leftover, "UTF8");
                            token = leftoverString + token;
                            leftover = null;
                        }
                        */
                        /*
                         * Note that the way I was doing it at first - above - 
                         * was not quite the correct way - because I was creating UTF8
                         * strings from the leftover bytes, and the bytes in the 
                         * current buffer *separately*; which means, if a multi-byte
                         * UTF8 character got split in the middle between one buffer
                         * and the next, both chunks of it would become junk 
                         * characters, on each side!
                         * The correct way of doing it, of course, is to create a
                         * merged byte buffer, and then turn it into a UTF8 string. 
                         *      -- L.A. 4.0
                         */
                        String token = null;

                        if (leftover == null) {
                            token = new String(columnBytes, byteoffset, bytecount - byteoffset, "UTF8");
                        } else {
                            byte[] merged = new byte[leftover.length + bytecount - byteoffset];

                            System.arraycopy(leftover, 0, merged, 0, leftover.length);
                            System.arraycopy(columnBytes, byteoffset, merged, leftover.length, bytecount - byteoffset);
                            token = new String(merged, "UTF8");
                            leftover = null;
                            merged = null;
                        }

                        if (isString) {
                            if ("".equals(token)) {
                                // An empty string is a string missing value!
                                // An empty string in quotes is an empty string!
                                retVector[caseindex] = null;
                            } else {
                                // Strip the outer quotes:
                                token = token.replaceFirst("^\\\"", "");
                                token = token.replaceFirst("\\\"$", "");

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

                                String[] splitTokens = token.split(Matcher.quoteReplacement("\\\\"), -2);

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

                                token = StringUtils.join(splitTokens, '\\');

                                // "compatibility mode" - a hack, to be able to produce
                                // unfs identical to those produced by the "early"
                                // unf5 jar; will be removed in production 4.0.
                                // -- L.A. (TODO: ...)
                                if (compatmode && !"".equals(token)) {
                                    if (token.length() > 128) {
                                        if ("".equals(token.trim())) {
                                            // don't ask...
                                            token = token.substring(0, 129);
                                        } else {
                                            token = token.substring(0, 128);
                                            // token = String.format(loc, "%.128s", token);
                                            token = token.trim();
                                            // dbgLog.info("formatted and trimmed: "+token);
                                        }
                                    } else {
                                        if ("".equals(token.trim())) {
                                            // again, don't ask;
                                            // - this replicates some bugginness
                                            // that happens inside unf5;
                                            token = "null";
                                        } else {
                                            token = token.trim();
                                        }
                                    }
                                }

                                retVector[caseindex] = token;
                            }
                        } else if (isDouble) {
                            try {
                                // TODO: verify that NaN and +-Inf are
                                // handled correctly here! -- L.A.
                                // Verified: new Double("nan") works correctly,
                                // resulting in Double.NaN;
                                // Double("[+-]Inf") doesn't work however;
                                // (the constructor appears to be expecting it
                                // to be spelled as "Infinity", "-Infinity", etc.
                                if ("inf".equalsIgnoreCase(token) || "+inf".equalsIgnoreCase(token)) {
                                    retVector[caseindex] = java.lang.Double.POSITIVE_INFINITY;
                                } else if ("-inf".equalsIgnoreCase(token)) {
                                    retVector[caseindex] = java.lang.Double.NEGATIVE_INFINITY;
                                } else if (token == null || token.equals("")) {
                                    // missing value:
                                    retVector[caseindex] = null;
                                } else {
                                    retVector[caseindex] = new Double(token);
                                }
                            } catch (NumberFormatException ex) {
                                dbgLog.warning("NumberFormatException thrown for " + token + " as Double");

                                retVector[caseindex] = null; // missing value
                                // TODO: ?
                            }
                        } else if (isLong) {
                            try {
                                retVector[caseindex] = new Long(token);
                            } catch (NumberFormatException ex) {
                                retVector[caseindex] = null; // assume missing value
                            }
                        } else if (isFloat) {
                            try {
                                if ("inf".equalsIgnoreCase(token) || "+inf".equalsIgnoreCase(token)) {
                                    retVector[caseindex] = java.lang.Float.POSITIVE_INFINITY;
                                } else if ("-inf".equalsIgnoreCase(token)) {
                                    retVector[caseindex] = java.lang.Float.NEGATIVE_INFINITY;
                                } else if (token == null || token.equals("")) {
                                    // missing value:
                                    retVector[caseindex] = null;
                                } else {
                                    retVector[caseindex] = new Float(token);
                                }
                            } catch (NumberFormatException ex) {
                                dbgLog.warning("NumberFormatException thrown for " + token + " as Float");
                                retVector[caseindex] = null; // assume missing value (TODO: ?)
                            }
                        }
                        caseindex++;

                        if (bytecount == bytesRead - 1) {
                            byteoffset = 0;
                        } else {
                            byteoffset = bytecount + 1;
                        }
                    } else {
                        if (bytecount == bytesRead - 1) {
                            // We've reached the end of the buffer;
                            // This means we'll save whatever unused bytes left in
                            // it - i.e., the bytes between the last new line
                            // encountered and the end - in the leftover buffer.

                            // *EXCEPT*, there may be a case of a very long String
                            // that is actually longer than MAX_COLUMN_BUFFER, in
                            // which case it is possible that we've read through
                            // an entire buffer of bytes without finding any
                            // new lines... in this case we may need to add this
                            // entire byte buffer to an already existing leftover
                            // buffer!
                            if (leftover == null) {
                                leftover = new byte[(int) bytesRead - byteoffset];
                                System.arraycopy(columnBytes, byteoffset, leftover, 0, (int) bytesRead - byteoffset);
                            } else {
                                if (byteoffset != 0) {
                                throw new IOException("Reached the end of the byte buffer, with some leftover left from the last read; yet the offset is not zero!");
                                }
                                byte[] merged = new byte[leftover.length + (int) bytesRead];

                                System.arraycopy(leftover, 0, merged, 0, leftover.length);
                                System.arraycopy(columnBytes, byteoffset, merged, leftover.length, (int) bytesRead);
                                // leftover = null;
                                leftover = merged;
                                merged = null;
                            }
                            byteoffset = 0;

                        }
                    }
                    bytecount++;
                }

                bytesReadTotal += bytesRead;
                in.clear();
                if (columnLength - bytesReadTotal < MAX_COLUMN_BUFFER) {
                    in.limit((int) (columnLength - bytesReadTotal));
                }
            }

        }

        if (caseindex != casecount) {
            throw new IOException("Faile to read "+casecount+" tokens for column "+column);
            //System.out.println("read "+caseindex+" tokens instead of expected "+casecount+".");
        }
        
        return retVector; 
    }
    
    private long[] extractColumnOffsets (File rotatedImageFile, int varcount, int casecount) throws IOException {
        long[] byteOffsets = new long[varcount];
        
        try (BufferedInputStream rotfileStream = new BufferedInputStream(new FileInputStream(rotatedImageFile))) {

            byte[] offsetHeader = new byte[varcount * 8];

            int readlen = rotfileStream.read(offsetHeader);

            if (readlen != varcount * 8) {
                throw new IOException("Could not read " + varcount * 8 + " header bytes from the rotated file.");
            }

            for (int varindex = 0; varindex < varcount; varindex++) {
                byte[] offsetBytes = new byte[8];
                System.arraycopy(offsetHeader, varindex * 8, offsetBytes, 0, 8);

                ByteBuffer offsetByteBuffer = ByteBuffer.wrap(offsetBytes);
                byteOffsets[varindex] = offsetByteBuffer.getLong();

                // System.out.println(byteOffsets[varindex]);
            }

        }

        return byteOffsets;
    }
    
    private File getRotatedImage(File tabfile, int varcount, int casecount)  throws IOException {
        String fileName = tabfile.getAbsolutePath();
        String rotatedImageFileName = fileName + ".90d";
        File rotatedImageFile = new File(rotatedImageFileName); 
        if (rotatedImageFile.exists()) {
            //System.out.println("Image already exists!");
            return rotatedImageFile;
        }
        
        return generateRotatedImage(tabfile, varcount, casecount);
        
    }
    
    private File generateRotatedImage (File tabfile, int varcount, int casecount) throws IOException {
        // TODO: throw exceptions if bad file, zero varcount, etc. ...
        
        String fileName = tabfile.getAbsolutePath();
        String rotatedImageFileName = fileName + ".90d";
        
        int MAX_OUTPUT_STREAMS = 32;
        int MAX_BUFFERED_BYTES = 10 * 1024 * 1024; // 10 MB - for now?
        int MAX_COLUMN_BUFFER = 8 * 1024; 
        
        // offsetHeader will contain the byte offsets of the individual column 
        // vectors in the final rotated image file
        byte[] offsetHeader = new byte[varcount * 8];
        int[] bufferedSizes = new int[varcount];
        long[] cachedfileSizes = new long[varcount];
        File[] columnTempFiles = new File[varcount];
        
        for (int i = 0; i < varcount; i++) {
            bufferedSizes[i] = 0; 
            cachedfileSizes[i] = 0;
        }
        
        // TODO: adjust MAX_COLUMN_BUFFER here, so that the total size is 
        // no more than MAX_BUFFERED_BYTES (but no less than 1024 maybe?)
        
        byte[][] bufferedColumns = new byte [varcount][MAX_COLUMN_BUFFER];
        
        // read the tab-delimited file: 
        
        try (FileInputStream tabfileStream = new FileInputStream(tabfile);
                Scanner scanner = new Scanner(tabfileStream)) {
            scanner.useDelimiter("\\n");

            for (int caseindex = 0; caseindex < casecount; caseindex++) {
                if (scanner.hasNext()) {
                    String[] line = (scanner.next()).split("\t", -1);
                    // TODO: throw an exception if there are fewer tab-delimited
                    // tokens than the number of variables specified.
                    String token = "";
                    int tokensize = 0;
                    for (int varindex = 0; varindex < varcount; varindex++) {
                        // TODO: figure out the safest way to convert strings to
                        // bytes here. Is it going to be safer to use getBytes("UTF8")?
                        // we are already making the assumption that the values
                        // in the tab file are in UTF8. -- L.A.
                        token = line[varindex] + "\n";
                        tokensize = token.getBytes().length;
                        if (bufferedSizes[varindex] + tokensize > MAX_COLUMN_BUFFER) {
                            // fill the buffer and dump its contents into the temp file:
                            // (do note that there may be *several* MAX_COLUMN_BUFFERs
                            // worth of bytes in the token!)

                            int tokenoffset = 0;

                            if (bufferedSizes[varindex] != MAX_COLUMN_BUFFER) {
                                tokenoffset = MAX_COLUMN_BUFFER - bufferedSizes[varindex];
                                System.arraycopy(token.getBytes(), 0, bufferedColumns[varindex], bufferedSizes[varindex], tokenoffset);
                            } // (otherwise the buffer is already full, and we should
                              // simply dump it into the temp file, without adding any
                              // extra bytes to it)

                            File bufferTempFile = columnTempFiles[varindex];
                            if (bufferTempFile == null) {
                                bufferTempFile = File.createTempFile("columnBufferFile", "bytes");
                                columnTempFiles[varindex] = bufferTempFile;
                            }

                            // *append* the contents of the buffer to the end of the
                            // temp file, if already exists:
                            try (BufferedOutputStream outputStream = new BufferedOutputStream(
                                    new FileOutputStream(bufferTempFile, true))) {
                                outputStream.write(bufferedColumns[varindex], 0, MAX_COLUMN_BUFFER);
                                cachedfileSizes[varindex] += MAX_COLUMN_BUFFER;

                                // keep writing MAX_COLUMN_BUFFER-size chunks of bytes into
                                // the temp file, for as long as there's more than MAX_COLUMN_BUFFER
                                // bytes left in the token:

                                while (tokensize - tokenoffset > MAX_COLUMN_BUFFER) {
                                    outputStream.write(token.getBytes(), tokenoffset, MAX_COLUMN_BUFFER);
                                    cachedfileSizes[varindex] += MAX_COLUMN_BUFFER;
                                    tokenoffset += MAX_COLUMN_BUFFER;
                                }

                            }

                            // buffer the remaining bytes and reset the buffered
                            // byte counter:

                            System.arraycopy(token.getBytes(), 
                                    tokenoffset, 
                                    bufferedColumns[varindex], 
                                    0,
                                    tokensize - tokenoffset);

                            bufferedSizes[varindex] = tokensize - tokenoffset;

                        } else {
                            // continue buffering
                            System.arraycopy(token.getBytes(), 0, bufferedColumns[varindex], bufferedSizes[varindex], tokensize);
                            bufferedSizes[varindex] += tokensize;
                        }
                    }
                } else {
                    throw new IOException("Tab file has fewer rows than the stored number of cases!");
                }
            }
        }
        
        // OK, we've created the individual byte vectors of the tab file columns;
        // they may be partially saved in temp files and/or in memory.
        // We now need to go through all these buffers and create the final 
        // rotated image file. 
        
        try (BufferedOutputStream finalOut = new BufferedOutputStream(
                new FileOutputStream(new File(rotatedImageFileName)))) {

            // but first we should create the offset header and write it out into
            // the final file; because it should be at the head, doh!

            long columnOffset = varcount * 8;
            // (this is the offset of the first column vector; it is equal to the
            // size of the offset header, i.e. varcount * 8 bytes)

            for (int varindex = 0; varindex < varcount; varindex++) {
                long totalColumnBytes = cachedfileSizes[varindex] + bufferedSizes[varindex];
                columnOffset += totalColumnBytes;
                // totalColumnBytes;
                byte[] columnOffsetByteArray = ByteBuffer.allocate(8).putLong(columnOffset).array();
                System.arraycopy(columnOffsetByteArray, 0, offsetHeader, varindex * 8, 8);
            }

            finalOut.write(offsetHeader, 0, varcount * 8);

            for (int varindex = 0; varindex < varcount; varindex++) {
                long cachedBytesRead = 0;

                // check if there is a cached temp file:

                File cachedTempFile = columnTempFiles[varindex];
                if (cachedTempFile != null) {
                    byte[] cachedBytes = new byte[MAX_COLUMN_BUFFER];
                    try (BufferedInputStream cachedIn = new BufferedInputStream(new FileInputStream(cachedTempFile))) {
                        int readlen = 0;
                        while ((readlen = cachedIn.read(cachedBytes)) > -1) {
                            finalOut.write(cachedBytes, 0, readlen);
                            cachedBytesRead += readlen;
                        }
                    }

                    // delete the temp file:
                    cachedTempFile.delete();

                }

                if (cachedBytesRead != cachedfileSizes[varindex]) {
                    throw new IOException("Could not read the correct number of bytes cached for column "+varindex+"; "+
                        cachedfileSizes[varindex] + " bytes expected, "+cachedBytesRead+" read.");
                }

                // then check if there are any bytes buffered for this column:

                if (bufferedSizes[varindex] > 0) {
                    finalOut.write(bufferedColumns[varindex], 0, bufferedSizes[varindex]);
                }

            }
        }
        
        return new File(rotatedImageFileName);

    }
  
    /*
     * Test method for taking a "rotated" image, and reversing it, reassembling 
     * all the columns in the original order. Which should result in a file 
     * byte-for-byte identical file to the original tab-delimited version.
     *
     * (do note that this method is not efficiently implemented; it's only 
     * being used for experiments so far, to confirm the accuracy of the 
     * accuracy of generateRotatedImage(). It should not be used for any 
     * practical means in the application!)
     */
    private void reverseRotatedImage (File rotfile, int varcount, int casecount) throws IOException {
        // open the file, read in the offset header: 
        try (BufferedInputStream rotfileStream = new BufferedInputStream(new FileInputStream(rotfile))) {
            byte[] offsetHeader = new byte[varcount * 8];
            long[] byteOffsets = new long[varcount];
            
            int readlen = rotfileStream.read(offsetHeader); 
            
            if (readlen != varcount * 8) {
                throw new IOException ("Could not read "+varcount*8+" header bytes from the rotated file.");
            }
            
            for (int varindex = 0; varindex < varcount; varindex++) {
                byte[] offsetBytes = new byte[8];
                System.arraycopy(offsetHeader, varindex*8, offsetBytes, 0, 8);
               
                ByteBuffer offsetByteBuffer = ByteBuffer.wrap(offsetBytes);
                byteOffsets[varindex] = offsetByteBuffer.getLong();
                
                //System.out.println(byteOffsets[varindex]);
            }
            
            String [][] reversedMatrix = new String[casecount][varcount];
            
            long offset = varcount * 8; 
            byte[] columnBytes; 
            
            for (int varindex = 0; varindex < varcount; varindex++) {
                long columnLength = byteOffsets[varindex] - offset; 
                
                
                
                columnBytes = new byte[(int)columnLength];
                readlen = rotfileStream.read(columnBytes);
                
                if (readlen != columnLength) {
                    throw new IOException ("Could not read "+columnBytes+" bytes for column "+varindex);
                }
                /*
                String columnString = new String(columnBytes);
                //System.out.print(columnString);
                String[] values = columnString.split("\n", -1);
                
                if (values.length < casecount) {
                    throw new IOException("count mismatch: "+values.length+" tokens found for column "+varindex);
                }
                
                for (int caseindex = 0; caseindex < casecount; caseindex++) {
                    reversedMatrix[caseindex][varindex] = values[caseindex];
                }*/
                
                int bytecount = 0; 
                int byteoffset = 0; 
                int caseindex = 0;
                //System.out.println("generating value vector for column "+varindex);
                while (bytecount < columnLength) {
                    if (columnBytes[bytecount] == '\n') {
                        String token = new String(columnBytes, byteoffset, bytecount-byteoffset);
                        reversedMatrix[caseindex++][varindex] = token;
                        byteoffset = bytecount + 1;
                    }
                    bytecount++;
                }
                
                if (caseindex != casecount) {
                    throw new IOException("count mismatch: "+caseindex+" tokens found for column "+varindex);
                }
                offset = byteOffsets[varindex];
            }
            
            for (int caseindex = 0; caseindex < casecount; caseindex++) {
                for (int varindex = 0; varindex < varcount; varindex++) {
                    System.out.print(reversedMatrix[caseindex][varindex]);
                    if (varindex < varcount-1) {
                        System.out.print("\t");
                    } else {
                        System.out.print("\n");
                    }
                }
            }
            
        }
        
        
    }
    
    /**
     * main() method, for testing
     * usage: java edu.harvard.iq.dataverse.dataaccess.TabularSubsetGenerator testfile.tab varcount casecount column type
     * make sure the CLASSPATH contains ...
     * 
     */
    
    public static void main(String[] args) {
        
        String tabFileName = args[0]; 
        int varcount = new Integer(args[1]).intValue();
        int casecount = new Integer(args[2]).intValue();
        int column = new Integer(args[3]).intValue();
        String type = args[4];
        
        File tabFile = new File(tabFileName);
        File rotatedImageFile = null; 
        
        TabularSubsetGenerator subsetGenerator = new TabularSubsetGenerator(); 
        
        /*
        try {
            rotatedImageFile = subsetGenerator.getRotatedImage(tabFile, varcount, casecount);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        */
        
        //System.out.println("\nFinished generating \"rotated\" column image file."); 
                
        //System.out.println("\nOffsets:");
        
        MathContext doubleMathContext = new MathContext(15, RoundingMode.HALF_EVEN);
        String FORMAT_IEEE754 = "%+#.15e";
        
        try {
            //subsetGenerator.reverseRotatedImage(rotatedImageFile, varcount, casecount);
            //String[] columns = subsetGenerator.subsetStringVector(tabFile, column, varcount, casecount);
            if ("string".equals(type)) {
                String[] columns = subsetGenerator.subsetStringVector(tabFile, column, varcount, casecount);
                for (int i = 0; i < casecount; i++) {
                    System.out.println(columns[i]);
                }
            } else {

                Double[] columns = subsetGenerator.subsetDoubleVector(tabFile, column, varcount, casecount);
                for (int i = 0; i < casecount; i++) {
                    if (columns[i] != null) {
                        BigDecimal outBigDecimal = new BigDecimal(columns[i], doubleMathContext);
                        System.out.println(String.format(FORMAT_IEEE754, outBigDecimal));
                    } else {
                        System.out.println("NA");
                    }
                    //System.out.println(columns[i]);
                }
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}


