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
package edu.harvard.iq.dataverse.ingest;


import static java.lang.System.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.regex.*;
import java.util.zip.*;
import java.util.logging.Logger;
import org.apache.commons.lang3.builder.*;
import org.apache.commons.io.IOUtils;

/**
 * This is a virtually unchanged DVN v2-3 implementation by 
 * @author Akio Sone
 *
 * incorporated into 4.0 by 
 * @author Leonid Andreev
 * 
 */
public class IngestableDataChecker implements java.io.Serializable {

    /**
     * 
     */

    // static fields
    private static Logger dbgLog = Logger.getLogger(IngestableDataChecker.class.getPackage().getName());

    // default format set
    private static String[] defaultFormatSet = {"POR", "SAV", "DTA", "RDA", "XPT"};
    private String[] testFormatSet;
    // Map that returns a Stata Release number
    private static Map<Byte, String> stataReleaseNumber = new HashMap<Byte, String>();
    public static String STATA_13_HEADER = "<stata_dta><header><release>117</release>";
    public static String STATA_14_HEADER = "<stata_dta><header><release>118</release>";
    public static String STATA_15_HEADER = "<stata_dta><header><release>119</release>";
    // Map that returns a reader-implemented mime-type
    private static Set<String> readableFileTypes = new HashSet<String>();
    private static Map<String, Method> testMethods = new HashMap<String, Method>();
    public static String SAS_XPT_HEADER_80 = "HEADER RECORD*******LIBRARY HEADER RECORD!!!!!!!000000000000000000000000000000  ";
    public static String SAS_XPT_HEADER_11 = "SAS     SAS";
    public static int POR_MARK_POSITION_DEFAULT = 461;
    public static String POR_MARK = "SPSSPORT";
    private static int DEFAULT_BUFFER_SIZE = 500;
    private static String regex = "^test(\\w+)format$";

    // static initialization block
    private static String rdargx = "^(52)(44)(41|42|58)(31|32|33)(0A)$";
    private static int RDA_HEADER_SIZE = 5;
    private static Pattern ptn;


    static {

        stataReleaseNumber.put((byte) 104, "rel_3");
        stataReleaseNumber.put((byte) 105, "rel_4or5");
        stataReleaseNumber.put((byte) 108, "rel_6");
        stataReleaseNumber.put((byte) 110, "rel_7first");
        stataReleaseNumber.put((byte) 111, "rel_7scnd");
        stataReleaseNumber.put((byte) 113, "rel_8_or_9");
        stataReleaseNumber.put((byte) 114, "rel_10");
        stataReleaseNumber.put((byte) 115, "rel_12"); 
        // 116 was an in-house experimental version that was never 
        // released.
        // STATA v.13 introduced a new format, 117. It's a completely
        // new development, unrelated to the old format. 
        stataReleaseNumber.put((byte) 117, "rel_13");

        readableFileTypes.add("application/x-stata");
        readableFileTypes.add("application/x-spss-sav");
        readableFileTypes.add("application/x-spss-por");
        readableFileTypes.add("application/x-rlang-transport");
        readableFileTypes.add("application/x-stata-13");
        readableFileTypes.add("application/x-stata-14");
        readableFileTypes.add("application/x-stata-15");

        Pattern p = Pattern.compile(regex);
        ptn = Pattern.compile(rdargx);

        for (Method m : IngestableDataChecker.class.getDeclaredMethods()) {
            //String mname = m.getName();
            // if (mname.startsWith("test")) && (mname.endsWith("format")){

            Matcher mtr = p.matcher(m.getName());
            if (mtr.matches()) {
                testMethods.put(mtr.group(1), m);
            }
        }
    }
    private boolean windowsNewLine = true;

    // constructors
    // using the default format set
    public IngestableDataChecker() {
        this.testFormatSet = defaultFormatSet;
    }
    // using a user-defined customized format set

    public IngestableDataChecker(String[] requestedFormatSet) {
        this.testFormatSet = requestedFormatSet;
        dbgLog.fine("SubsettableFileChecker instance=" + this.toString());
    }

    // public class methods
    public static String[] getDefaultTestFormatSet() {
        return defaultFormatSet;
    }

    /**
     *  print the usage
     *
     */
    public static void printUsage() {
        out.println("Usage : java subsettableFileChecker <datafileName>");
    }

    // instance methods
    public String[] getTestFormatSet() {
        return this.testFormatSet;
    }

    // test methods start here ------------------------------------------------
    /**
     * test this byte buffer against SPSS-SAV spec
     *
     *
     */
    public String testSAVformat(MappedByteBuffer buff) {
        String result = null;
        buff.rewind();
        boolean DEBUG = false;

        
        // -----------------------------------------
        // Avoid java.nio.BufferUnderflowException
        // -----------------------------------------
        if (buff.capacity() < 4){
            return null;
        }
        
        if (DEBUG) {
            out.println("applying the sav test\n");
        }

        byte[] hdr4 = new byte[4];
        buff.get(hdr4, 0, 4);
        String hdr4sav = new String(hdr4);

        if (DEBUG) {
            out.println("from string=" + hdr4sav);
        }
        if (hdr4sav.equals("$FL2")) {
            if (DEBUG) {
                out.println("this file is spss-sav type");
            }
            result = "application/x-spss-sav";
        } else {
            if (DEBUG) {
                out.println("this file is NOT spss-sav type");
            }
        }
        
        return result;
    }

    
    /**
     * test this byte buffer against STATA DTA spec
     *
     */
    public String testDTAformat(MappedByteBuffer buff) {
        String result = null;
        buff.rewind();
        boolean DEBUG = false;

        if (DEBUG) {
            dbgLog.info("applying the dta test\n");
        }

        // -----------------------------------------
        // Avoid java.nio.BufferUnderflowException
        // -----------------------------------------
        if (buff.capacity() < 4) {            
            return result;
        }
        
        // We first check if it's a "classic", old DTA format 
        // (up to version 115): 
        
        byte[] hdr4 = new byte[4];
        buff.get(hdr4, 0, 4);
        
        if (DEBUG) {
            for (int i = 0; i < hdr4.length; ++i) {
                dbgLog.info(String.format("%d\t%02X\n", i, hdr4[i]));
            }
        }

        if (hdr4[2] != 1) {
            if (DEBUG) {
                dbgLog.info("3rd byte is not 1: given file is not stata-dta type");
            }
            //return result;
        } else if ((hdr4[1] != 1) && (hdr4[1] != 2)) {
            if (DEBUG) {
                dbgLog.info("2nd byte is neither 0 nor 1: this file is not stata-dta type");
            }
            //return result;
        } else if (!IngestableDataChecker.stataReleaseNumber.containsKey(hdr4[0])) {
            if (DEBUG) {
                dbgLog.info("1st byte (" + hdr4[0] +
                    ") is not within the ingestable range [rel. 3-10]: this file is NOT stata-dta type");
            }
            //return result;
        } else {
            if (DEBUG) {
                dbgLog.info("this file is stata-dta type: " +
                    IngestableDataChecker.stataReleaseNumber.get(hdr4[0]) +
                    "(No in HEX=" + hdr4[0] + ")");
            }
            result = "application/x-stata";
        }
        
        if ((result == null)&&(buff.capacity() >= STATA_13_HEADER.length())) {
            // Let's see if it's a "new" STATA (v.13+) format: 
            buff.rewind();
            byte[] headerBuffer = null; 
            String headerString = null; 
            try {
                headerBuffer = new byte[STATA_13_HEADER.length()];
                buff.get(headerBuffer, 0, STATA_13_HEADER.length());
                headerString = new String(headerBuffer, "US-ASCII");
            } catch (Exception ex) {
                // probably a buffer underflow exception; 
                // we don't have to do anything... null will 
                // be returned, below. 
            }
            
            if (STATA_13_HEADER.equals(headerString)) {
                result = "application/x-stata-13";
            }
            
        }

        if ((result == null) && (buff.capacity() >= STATA_14_HEADER.length())) {
            // Let's see if it's a "new" STATA (v.14+) format:
            buff.rewind();
            byte[] headerBuffer = null;
            String headerString = null;
            try {
                headerBuffer = new byte[STATA_14_HEADER.length()];
                buff.get(headerBuffer, 0, STATA_14_HEADER.length());
                headerString = new String(headerBuffer, "US-ASCII");
            } catch (Exception ex) {
                // probably a buffer underflow exception;
                // we don't have to do anything... null will
                // be returned, below.
            }
            if (STATA_14_HEADER.equals(headerString)) {
                result = "application/x-stata-14";
            }
        }

        if ((result == null) && (buff.capacity() >= STATA_15_HEADER.length())) {
            // Let's see if it's a "new" STATA (v.14+) format:
            buff.rewind();
            byte[] headerBuffer = null;
            String headerString = null;
            try {
                headerBuffer = new byte[STATA_15_HEADER.length()];
                buff.get(headerBuffer, 0, STATA_15_HEADER.length());
                headerString = new String(headerBuffer, "US-ASCII");
            } catch (Exception ex) {
                // probably a buffer underflow exception;
                // we don't have to do anything... null will
                // be returned, below.
            }
            if (STATA_15_HEADER.equals(headerString)) {
                result = "application/x-stata-15";
            }
        }

        return result;
    }

    /**
     * test this byte buffer against SAS Transport(XPT) spec
     *
     */
    public String testXPTformat(MappedByteBuffer buff) {
        String result = null;
        buff.rewind();
        boolean DEBUG = false;

        if (DEBUG) {
            out.println("applying the sas-transport test\n");
        }
        // size test
        if (buff.capacity() < 91) {
            if (DEBUG) {
                out.println("this file is NOT sas-exort type\n");
            }

            return result;
        }

        byte[] hdr1 = new byte[80];
        byte[] hdr2 = new byte[11];
        buff.get(hdr1, 0, 80);
        buff.get(hdr2, 0, 11);

        String hdr1st80 = new String(hdr1);
        String hdrnxt11 = new String(hdr2);

        if (DEBUG) {
            out.println("1st-80  bytes=" + hdr1st80);
            out.println("next-11 bytes=" + hdrnxt11);
        }

        if ((hdr1st80.equals(IngestableDataChecker.SAS_XPT_HEADER_80)) &&
            (hdrnxt11.equals(IngestableDataChecker.SAS_XPT_HEADER_11))) {
            if (DEBUG) {
                out.println("this file is sas-export type\n");
            }
            result = "application/x-sas-xport";
        } else {
            if (DEBUG) {
                out.println("this file is NOT sas-exort type\n");
            }
        }
        return result;
    }

    /**
     * test this byte buffer against SPSS Portable (POR) spec
     *
     */
    public String testPORformat(MappedByteBuffer buff) {
        String result = null;
        buff.rewind();
        boolean DEBUG = false;

        if (DEBUG) {
            out.println("applying the spss-por test\n");
        }

        // size test
	int bufferCapacity = buff.capacity();
	dbgLog.fine("Subsettable Checker: buffer capacity: "+bufferCapacity);

        if (bufferCapacity < 491) {
            if (DEBUG) {
                out.println("this file is NOT spss-por type\n");
            }

            return result;
        }

        //windows [0D0A]=>   [1310] = [CR/LF]
        //unix    [0A]  =>   [10]
        //mac     [0D]  =>   [13]
        // 3char  [0D0D0A]=> [131310] spss for windows rel 15
        // expected results
        // unix    case: [0A]   : [80], [161], [242], [323], [404], [485]
        // windows case: [0D0A] : [81], [163], [245], [327], [409], [491]
        //  : [0D0D0A] : [82], [165], [248], [331], [414], [495]

        buff.rewind();
        byte[] nlch = new byte[36];
        int pos1;
        int pos2;
        int pos3;
        int ucase = 0;
        int wcase = 0;
        int mcase = 0;
        int three = 0;
        int nolines = 6;
        int nocols = 80;
        for (int i = 0; i < nolines; ++i) {
            int baseBias = nocols * (i + 1);
            // 1-char case
            pos1 = baseBias + i;

	    if ( pos1 > bufferCapacity - 1 ) {
		dbgLog.fine("Subsettable Checker: request to go beyond buffer capacity ("+pos1+")");
		return result; 
	    }

            buff.position(pos1);
            if (DEBUG) {
                out.println("\tposition(1)=" + buff.position());
            }
            int j = 6 * i;
            nlch[j] = buff.get();

            if (nlch[j] == 10) {
                ucase++;
            } else if (nlch[j] == 13) {
                mcase++;
            }

            // 2-char case
            pos2 = baseBias + 2 * i;

	    if ( pos2 > bufferCapacity - 2 ) {
		dbgLog.fine("Subsettable Checker: request to read 2 bytes beyond buffer capacity ("+pos2+")");
		return result; 
	    }


            buff.position(pos2);
            if (DEBUG) {
                out.println("\tposition(2)=" + buff.position());
            }
            nlch[j + 1] = buff.get();
            nlch[j + 2] = buff.get();

            // 3-char case
            pos3 = baseBias + 3 * i;

	    if ( pos3 > bufferCapacity - 3 ) {
		dbgLog.fine("Subsettable Checker: request to read 3 bytes beyond buffer capacity ("+pos3+")");
		return result; 
	    }


            buff.position(pos3);
            if (DEBUG) {
                out.println("\tposition(3)=" + buff.position());
            }
            nlch[j + 3] = buff.get();
            nlch[j + 4] = buff.get();
            nlch[j + 5] = buff.get();

            if (DEBUG) {
                out.println(i + "-th iteration position =" + nlch[j] + "\t" + nlch[j + 1] + "\t" + nlch[j + 2]);
                out.println(i + "-th iteration position =" + nlch[j + 3] + "\t" + nlch[j + 4] + "\t" + nlch[j + 5]);
            }
            if ((nlch[j + 3] == 13) && (nlch[j + 4] == 13) && (nlch[j + 5] == 10)) {
                three++;
            } else if ((nlch[j + 1] == 13) && (nlch[j + 2] == 10)) {
                wcase++;
            }

            buff.rewind();
        }
        if (three == nolines) {
            if (DEBUG) {
                out.println("0D0D0A case");
            }
            windowsNewLine = false;
        } else if ((ucase == nolines) && (wcase < nolines)) {
            if (DEBUG) {
                out.println("0A case");
            }
            windowsNewLine = false;
        } else if ((ucase < nolines) && (wcase == nolines)) {
            if (DEBUG) {
                out.println("0D0A case");
            }
        } else if ((mcase == nolines) && (wcase < nolines)) {
            if (DEBUG) {
                out.println("0D case");
            }
            windowsNewLine = false;
        }


        buff.rewind();
        int PORmarkPosition = POR_MARK_POSITION_DEFAULT;
        if (windowsNewLine) {
            PORmarkPosition = PORmarkPosition + 5;
        } else if (three == nolines) {
            PORmarkPosition = PORmarkPosition + 10;
        }

        byte[] pormark = new byte[8];
        buff.position(PORmarkPosition);
        buff.get(pormark, 0, 8);
        String pormarks = new String(pormark);

        if (DEBUG) {
            out.println("pormark =>" + pormarks + "<-");
        }

        if (pormarks.equals(POR_MARK)) {
            if (DEBUG) {
                out.println("this file is spss-por type");
            }
            result = "application/x-spss-por";
        } else {
            if (DEBUG) {
                out.println("this file is NOT spss-por type");
            }
        }

        return result;
    }

    /**
     * test this byte buffer against R data file
     *
     */
    public String testRDAformat(MappedByteBuffer buff) {
        String result = null;
        buff.rewind();
        
        if (buff.capacity() < 4){
            return null;
        }
        
        boolean DEBUG = false;
        if (DEBUG) {
            out.println("applying the RData test\n");
            out.println("buffer capacity=" + buff.capacity());
        }
        if (DEBUG) {
            byte[] rawhdr = new byte[4];
            buff.get(rawhdr, 0, 4);
            for (int j = 0; j < 4; j++) {
                out.printf("%02X ", rawhdr[j]);
            }
            out.println();
            buff.rewind();
        }
        // get the first 4 bytes as an int and check its value; 
        // if it is 0x1F8B0800, then gunzip and its first 4 bytes
        int magicNumber = buff.getInt();

        if (DEBUG) {
            out.println("magicNumber in decimal =" + magicNumber);
            out.println("in binary=" + Integer.toBinaryString(magicNumber));
            out.println("in oct=" + Integer.toOctalString(magicNumber));
            out.println("in hex=" + Integer.toHexString(magicNumber));
        }
        try {
            if (magicNumber == 0x1F8B0800) {
                if (DEBUG) {
                    out.println("magicNumber is GZIP");
                }
                // gunzip the first 5 bytes and check their bye-pattern

                // get gzip buffer size

                int gzip_buffer_size = this.getGzipBufferSize(buff);

                byte[] hdr = new byte[gzip_buffer_size];
                buff.get(hdr, 0, gzip_buffer_size);

                try (GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(hdr))) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < RDA_HEADER_SIZE; i++) {
                        sb.append(String.format("%02X", gzin.read()));
                    }
                    String fisrt5bytes = sb.toString();
                    result = this.checkUncompressedFirst5bytes(fisrt5bytes);
                }
            // end of compressed case
            } else {
                // uncompressed case?
                if (DEBUG) {
                    out.println("magicNumber is not GZIP:" + magicNumber);
                    out.println("test as an uncompressed RData file");
                }

                buff.rewind();
                byte[] uchdr = new byte[5];
                buff.get(uchdr, 0, 5);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < uchdr.length; i++) {
                    sb.append(String.format("%02X", uchdr[i]));
                }
                String fisrt5bytes = sb.toString();

                result = this.checkUncompressedFirst5bytes(fisrt5bytes);
            // end of uncompressed case
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    // public instance methods ------------------------------------------------
    public String detectTabularDataFormat(File fh) {
        boolean DEBUG = false;
        String readableFormatType = null;
        FileChannel srcChannel = null;
        FileInputStream inp = null;
        
        try {
            // set-up a FileChannel instance for a given file object
            inp = new FileInputStream(fh);
            srcChannel = inp.getChannel();
            long buffer_size = this.getBufferSize(srcChannel);
            dbgLog.fine("buffer_size: " + buffer_size);

            // create a read-only MappedByteBuffer
            MappedByteBuffer buff = srcChannel.map(FileChannel.MapMode.READ_ONLY, 0, buffer_size);
            
            //this.printHexDump(buff, "hex dump of the byte-buffer");

            buff.rewind();
            dbgLog.fine("before the for loop");
            for (String fmt : this.getTestFormatSet()) {
                
                // get a test method
                Method mthd = testMethods.get(fmt);
                //dbgLog.info("mthd: " + mthd.getName());

                try {
                    // invoke this method
                    Object retobj = mthd.invoke(this, buff);
                    String result = (String) retobj;

                    if (result != null) {
                        dbgLog.fine("result for (" + fmt + ")=" + result);
                        if (DEBUG) {
                            out.println("result for (" + fmt + ")=" + result);
                        }
                        if (readableFileTypes.contains(result)) {
                            readableFormatType = result;
                        }
                        dbgLog.fine("readableFormatType=" + readableFormatType);
                    } else {
                        dbgLog.fine("null was returned for " + fmt + " test");
                        if (DEBUG) {
                            out.println("null was returned for " + fmt + " test");
                        }
                    }
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    // added null check because of "homemade.zip" from https://redmine.hmdc.harvard.edu/issues/3273
                    if (cause.getMessage() != null) {
                        err.format(cause.getMessage());
                        e.printStackTrace();
                    } else {
                        dbgLog.info("cause.getMessage() was null for " + e);
                        e.printStackTrace();
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (BufferUnderflowException e){
                    dbgLog.info("BufferUnderflowException " + e);
                    e.printStackTrace();
                }
                
                if (readableFormatType != null) {
                    break;
                }
            }
            
            // help garbage-collect the mapped buffer sooner, to avoid the jvm  
            // holding onto the underlying file unnecessarily:
            buff = null; 

        } catch (FileNotFoundException fe) {
            dbgLog.fine("exception detected: file was not foud");
            fe.printStackTrace();
        } catch (IOException ie) {
            dbgLog.fine("other io exception detected");
            ie.printStackTrace();
        } finally {
            IOUtils.closeQuietly(srcChannel);
            IOUtils.closeQuietly(inp);
        }
        return readableFormatType;
    }

    /**
     * identify the first 5 bytes
     *
     */
    private String checkUncompressedFirst5bytes(String fisrt5bytes) {
        boolean DEBUG = false;
        String result = null;
        if (DEBUG) {
            out.println("first5bytes=" + fisrt5bytes);
        }
        Matcher mtr = ptn.matcher(fisrt5bytes);

        if (mtr.matches()) {
            if (DEBUG) {
                out.println("RDATA type");
            }
            result = "application/x-rlang-transport";
        } else {
            if (DEBUG) {
                out.println("not binary RDATA type");
            }
        }

        return result;
    }

    /**
     * adjust the size of the buffer according to the size of 
     * the file if necessary; otherwise, use the default size
     */
    private long getBufferSize(FileChannel fileChannel) {
        boolean DEBUG = false;
        int BUFFER_SIZE = DEFAULT_BUFFER_SIZE;
        try {
        if (fileChannel.size() < DEFAULT_BUFFER_SIZE) {
            BUFFER_SIZE = (int) fileChannel.size();
            if (DEBUG) {
                out.println("non-default buffer_size: new size=" + BUFFER_SIZE);
            }
        }
        } catch (IOException ioex) {
            dbgLog.warning("failed to check the physical file size under an open FileChannel");
        }
        return BUFFER_SIZE;
    }

    private int getGzipBufferSize(MappedByteBuffer buff) {
        int GZIP_BUFFER_SIZE = 120;
        /*
        note:
        gzip buffer size <= 118  causes "java.io.EOFException:
        Unexpected end of ZLIB input stream"
        with a byte buffer of 500 bytes
         */
        // adjust gzip buffer size if necessary
        // file.size might be less than the default gzip buffer size
        if (buff.capacity() < GZIP_BUFFER_SIZE) {
            GZIP_BUFFER_SIZE = buff.capacity();
        }
        buff.rewind();
        return GZIP_BUFFER_SIZE;
    }

    /**
     * dump the data buffer in HEX
     *
     */
    public void printHexDump(MappedByteBuffer buff, String hdr) {
        int counter = 0;
        if (hdr != null) {
            out.println(hdr);
        }
        for (int i = 0; i < buff.capacity(); i++) {
            counter = i + 1;
            out.print(String.format("%02X ", buff.get()));
            if (counter % 16 == 0) {
                out.println();
            } else {
                if (counter % 8 == 0) {
                    out.print(" ");
                }
            }
        }
        out.println();
        buff.rewind();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
            ToStringStyle.MULTI_LINE_STYLE);
    }
}
