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
import java.util.*;
import java.util.Scanner;
import java.util.logging.*;
import java.io.*;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


import org.apache.commons.lang.*;


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
        
        File tabfile = dataFile.getFileSystemLocation().toFile();
        
        return subsetObjectVector(tabfile, column, varcount, casecount, columntype);
    }
    
    public Object[] subsetObjectVector(File tabfile, int column, int varcount, int casecount, int columntype) throws IOException {
        
        Object[] retVector = null; 
        
        boolean isString = false; 
        boolean isDouble = false;
        boolean isLong   = false; 
        boolean isFloat  = false; 
        
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
        long[] columnEndOffsets = getColumnOffsets(rotatedImageFile, varcount, casecount); 
        long columnOffset = 0; 
        long columnLength = 0; 
        
        if (column > 0) {
            columnOffset = columnEndOffsets[column - 1];
            columnLength = columnEndOffsets[column] - columnEndOffsets[column - 1];
        } else {
            columnOffset = varcount * 8; 
            columnLength = columnEndOffsets[0] - varcount * 8;  
        }
        
        FileChannel fc = (FileChannel.open(Paths.get(rotatedImageFile.getAbsolutePath()), StandardOpenOption.READ));
        fc.position(columnOffset);
        int MAX_COLUMN_BUFFER = 8192;
        
        ByteBuffer in = ByteBuffer.allocate(MAX_COLUMN_BUFFER);
        
        if (columnLength < MAX_COLUMN_BUFFER) {
          in.limit((int)(columnLength));
        }
        
        long bytesRead = 0;
        long bytesReadTotal = 0;
        int caseindex = 0; 
        int byteoffset = 0; 
        byte[] leftover = null; 
        
        while (bytesReadTotal < columnLength) {
            bytesRead = fc.read(in);
            byte[] columnBytes = in.array();
            int bytecount = 0; 

            
            while (bytecount < bytesRead) {
                if (columnBytes[bytecount] == '\n') {
                    String token = new String(columnBytes, byteoffset, bytecount-byteoffset);

                    if (leftover != null) {
                        String leftoverString = new String (leftover);
                        token = leftoverString + token;
                        leftover = null;
                    }
                    
                    if (isString) {
                        if ("".equals(token)) {
                            // An empty string is a string missing value!
                            // An empty string in quotes is an empty string!
                            retVector[caseindex] = null;
                        } else {
                            token = token.replaceFirst("^\\\"", "");
                            token = token.replaceFirst("\\\"$", "");
                            retVector[caseindex] = token;
                        }
                    } else if (isDouble) {
                        try {
                            // TODO: verify that NaN and +-Inf are 
                            // handled correctly here! -- L.A.
                            retVector[caseindex] = new Double(token);
                        } catch (NumberFormatException ex) {
                            retVector[caseindex] = null; // missing value
                        }
                    } else if (isLong) {
                        try {
                            retVector[caseindex] = new Long(token);
                        } catch (NumberFormatException ex) {
                            retVector[caseindex] = null; // assume missing value
                        }
                    } else if (isFloat) {
                        try {
                            retVector[caseindex] = new Float(token);
                        } catch (NumberFormatException ex) {
                            retVector[caseindex] = null; // assume missing value
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
                        leftover = new byte[(int)bytesRead - byteoffset];
                        System.arraycopy(columnBytes, byteoffset, leftover, 0, (int)bytesRead - byteoffset);
                        byteoffset = 0;

                    }
                }
                bytecount++;
            }
            
            bytesReadTotal += bytesRead; 
            in.clear();
            if (columnLength - bytesReadTotal < MAX_COLUMN_BUFFER) {
                in.limit((int)(columnLength - bytesReadTotal));
            }
        }
        
        fc.close();

        if (caseindex != casecount) {
            throw new IOException("Faile to read "+casecount+" tokens for column "+column);
            //System.out.println("read "+caseindex+" tokens instead of expected "+casecount+".");
        }
        
        return retVector; 
    }
    
    private long[] getColumnOffsets (File rotatedImageFile, int varcount, int casecount) throws IOException {
         BufferedInputStream rotfileStream = new BufferedInputStream(new FileInputStream(rotatedImageFile));
        
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
        
        rotfileStream.close(); 
        
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
        int MAX_COLUMN_BUFFER = 8192; 
        
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
        
        FileInputStream tabfileStream = new FileInputStream(tabfile);
        
        Scanner scanner = new Scanner(tabfileStream);
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
                    if (bufferedSizes[varindex]+tokensize > MAX_COLUMN_BUFFER) {
                        // fill the buffer and dump its contents into the temp file:
                        if (bufferedSizes[varindex] != MAX_COLUMN_BUFFER) {
                            System.arraycopy(token.getBytes(), 0, bufferedColumns[varindex], bufferedSizes[varindex], MAX_COLUMN_BUFFER-bufferedSizes[varindex]);
                        }
                        File bufferTempFile = columnTempFiles[varindex]; 
                        if (bufferTempFile == null) {
                            bufferTempFile = File.createTempFile("columnBufferFile", "bytes");
                            columnTempFiles[varindex] = bufferTempFile; 
                        } 
                        
                        // *append* the contents of the buffer to the end of the
                        // temp file, if already exists:
                        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream (bufferTempFile, true));
                        outputStream.write(bufferedColumns[varindex], 0, MAX_COLUMN_BUFFER);
                        cachedfileSizes[varindex] += MAX_COLUMN_BUFFER;
                        outputStream.close();
                        
                        // buffer the remaining bytes and reset the buffered 
                        // byte counter: 
                        
                        System.arraycopy(token.getBytes(), 
                                MAX_COLUMN_BUFFER-bufferedSizes[varindex],
                                bufferedColumns[varindex], 
                                0,
                                bufferedSizes[varindex] + tokensize - MAX_COLUMN_BUFFER);
                        
                        bufferedSizes[varindex] = bufferedSizes[varindex] + tokensize - MAX_COLUMN_BUFFER;
                        
                    } else {
                        // continue buffering
                        System.arraycopy(token.getBytes(), 0, bufferedColumns[varindex], bufferedSizes[varindex], tokensize);
                        bufferedSizes[varindex] += tokensize; 
                    }
                }
            } else {
                scanner.close();
                throw new IOException("Tab file has fewer rows than the stored number of cases!");
            }
            
        }
        
        // OK, we've created the individual byte vectors of the tab file columns;
        // they may be partially saved in temp files and/or in memory.
        // We now need to go through all these buffers and create the final 
        // rotated image file. 
        
        BufferedOutputStream finalOut = new BufferedOutputStream(new FileOutputStream (new File(rotatedImageFileName)));
        
        // but first we should create the offset header and write it out into 
        // the final file; because it should be at the head, doh!
        
        long columnOffset = varcount * 8;
        // (this is the offset of the first column vector; it is equal to the
        // size of the offset header, i.e. varcount * 8 bytes)
      
        for (int varindex = 0; varindex < varcount; varindex++) {
            long totalColumnBytes = cachedfileSizes[varindex] + bufferedSizes[varindex];
            columnOffset+=totalColumnBytes;
            //totalColumnBytes;
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
                BufferedInputStream cachedIn = new BufferedInputStream(new FileInputStream(cachedTempFile));
                int readlen = 0; 
                while ((readlen = cachedIn.read(cachedBytes)) > -1) {
                    finalOut.write(cachedBytes, 0, readlen);
                    cachedBytesRead += readlen;
                }
                cachedIn.close();
            }
            
            if (cachedBytesRead != cachedfileSizes[varindex]) {
                finalOut.close();
                throw new IOException("Could not read the correct number of bytes cached for column "+varindex+"; "+
                        cachedfileSizes[varindex] + " bytes expected, "+cachedBytesRead+" read.");
            }
            
            // then check if there are any bytes buffered for this column:
            
            if (bufferedSizes[varindex] > 0) {
                finalOut.write(bufferedColumns[varindex], 0, bufferedSizes[varindex]);
            }
            
        }
        
        finalOut.close();
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
        BufferedInputStream rotfileStream = new BufferedInputStream(new FileInputStream(rotfile));
        
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
        
        rotfileStream.close();
        
        
    }
    
    /**
     * main() method, for testing
     * usage: java edu.harvard.iq.dataverse.dataaccess.TabularSubsetGenerator testfile.tab varcount casecount
     * make sure the CLASSPATH contains ...
     * 
     */
    
    public static void main(String[] args) {
        
        String tabFileName = args[0]; 
        int varcount = new Integer(args[1]).intValue();
        int casecount = new Integer(args[2]).intValue();
        int column = new Integer(args[3]).intValue();
        
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
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}


