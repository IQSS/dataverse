package edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.logging.Logger;

public class DataReader {
    private static Logger logger = Logger.getLogger(DTAFileReader.class.getPackage().getName());
    private BufferedInputStream stream;
    private int DEFAULT_BUFFER_SIZE = 8192;// * 2;
    private byte[] buffer;
    private int buffer_size;
    private long byte_offset;
    private int buffer_byte_offset;
    private Boolean LSF = null;

    public DataReader(BufferedInputStream stream) throws IOException {
        this(stream, 0);
    }

    public DataReader(BufferedInputStream stream, int size) throws IOException {
        if (buffer_size > 0) {
            this.DEFAULT_BUFFER_SIZE = size;
        }
        this.stream = stream;
        buffer = new byte[DEFAULT_BUFFER_SIZE];
        byte_offset = 0;
        buffer_byte_offset = 0;

        bufferMoreBytes();
    }

    public void setLSF(boolean lsf) {
        LSF = lsf;
    }

    // this returns the *absolute* byte offest in the stream. 
    public long getByteOffset() {
        return byte_offset + buffer_byte_offset;
    }

    /* 
        readBytes is the workhorse method of the internal Data Reader class.
        it reads the requested number of bytes from the buffer, if available, 
        refilling the buffer as necessary. 
        the method allocates the byte array it returns, so there's no need 
        to do so outside of it. 
        the method will throw an exception if for whatever reason it cannot
        read the requested number of bytes. 
     */
    public byte[] readBytes(int n) throws IOException {
        if (n <= 0) {
            throw new IOException("DataReader.readBytes called to read zero or negative number of bytes.");
            }
        byte[] bytes = new byte[n];

        if (this.buffer_size - buffer_byte_offset >= n) {
            System.arraycopy(buffer, buffer_byte_offset, bytes, 0, n);
            buffer_byte_offset += n;
        } else {
            int bytes_read = 0;

            // copy any bytes left in the buffer into the return array:
            if (this.buffer_size - buffer_byte_offset > 0) {
                logger.fine("reading the remaining " + (this.buffer_size - buffer_byte_offset) + " bytes from the buffer");
                System.arraycopy(buffer, buffer_byte_offset, bytes, 0, this.buffer_size - buffer_byte_offset);
                //buffer_byte_offset = this.buffer_size;
                bytes_read = this.buffer_size - buffer_byte_offset;
                buffer_byte_offset = this.buffer_size;
            }

            int morebytes = bufferMoreBytes();
            logger.fine("buffered " + morebytes + " bytes");

            /* 
             * keep reading and buffering buffer-size chunks, until
             * we read the requested number of bytes.
             */
            while (n - bytes_read > this.buffer_size) {
                logger.fine("copying a full buffer-worth of bytes into the return array");
                System.arraycopy(buffer, buffer_byte_offset, bytes, bytes_read, this.buffer_size);
                //buffer_byte_offset = this.buffer_size;
                bytes_read += this.buffer_size;
                buffer_byte_offset = this.buffer_size;
                morebytes = bufferMoreBytes();
                logger.fine("buffered "+morebytes+" bytes");
            }

            /* 
             * finally, copy the last not-a-full-buffer-worth of bytes 
             * into the return buffer:
             */
            logger.fine("copying the remaining " + (n - bytes_read) + " bytes.");
            System.arraycopy(buffer, 0, bytes, bytes_read, n - bytes_read);
            buffer_byte_offset = n - bytes_read;
        }

        return bytes;
    }

    /* 
     * This method tries to read and buffer the DEFAULT_BUFFER_SIZE bytes
     * and sets the current buffer size accordingly.
     */
    private int bufferMoreBytes() throws IOException {
        int actual_bytes_read;
        byte_offset += buffer_byte_offset;

        if (byte_offset == 0 || buffer_byte_offset == buffer_size) {
            actual_bytes_read = stream.read(buffer, 0, DEFAULT_BUFFER_SIZE);
            // set the current buffer size to the actual number of 
            // bytes read: 
            this.buffer_size = actual_bytes_read;

            // reset the current buffer offset and increment the total
            // byte offset by the size of the last buffer - that should be 
            // equal to the buffer_byte_offset. 

        } else if (buffer_byte_offset < buffer_size) {
            System.arraycopy(buffer, buffer_byte_offset, buffer, 0, buffer_size - buffer_byte_offset);
            this.buffer_size = buffer_size - buffer_byte_offset;
            actual_bytes_read = stream.read(buffer, buffer_size, DEFAULT_BUFFER_SIZE - buffer_size);
            buffer_size += actual_bytes_read;

        } else {
            throw new IOException("Offset already past the buffer boundary");
        }
        buffer_byte_offset = 0;

        return actual_bytes_read;
    }

    /*
     * Checks that LSF is not null, and sets the buffer byte order accordingly
     */
    private void checkLSF(ByteBuffer buffer) throws IOException{
        if (LSF == null) {
            throw new IOException("Byte order not determined for reading numeric values.");
        } else if (LSF) {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    /* 
     * Convenience methods for reading single bytes of data. 
     * Just like with the other types of integers, both the signed and 
     * unsigned versions are provided. 
     * The readByte() is used to read STATA *data* stored as 
     * type "Byte"; the unsigned version is used to read byte values 
     * in various sections of the file that store the lengths of byte
     * sequences that follow. 
     */
    public byte readByte() throws IOException {
        /* Why not just use readBytes(1) here, you ask?
         * - Because readBytes() will want to allocate a 
         * return byte[] buffer of size 1. */
        byte ret;
        if (buffer_byte_offset > this.buffer_size) {
            throw new IOException("TD - buffer overflow");
        } else if (buffer_byte_offset < this.buffer_size) {
            ret = buffer[buffer_byte_offset];
            buffer_byte_offset++;
        } else {
            if (bufferMoreBytes() < 1) {
                throw new IOException("reached the end of data stream prematurely.");
            }
            ret = buffer[0];
            buffer_byte_offset = 1;
        }
        return ret;
    }

    // Note that readUByte() returns the value of Java type "short". 
    // This is to accommodate value larger than 127.
    public short readUByte() throws IOException {
        short ret = readByte();
        if (ret < 0) {
            ret += 256;
        }
        return ret;
    }

    /* Various reader methods for reading primitive numeric types; 
     * these are used both for reading the values from the data section
     * (signed integer and floating-point types), and to read numeric 
     * values encoded as unsigned bytes in various sections of the file, 
     * advertising the lengths of the data sections that follow. 
     * Note that the internal methods bytesToInt() and bytesToSignedInt()
     * will throw an exception if LSF (byte order flag) has not yet been 
     * set.
     */
    // Unsigned integer methods readUInt() and readUShort()
    // return long (8 byte) and int (4 byte) integers for overflow reasons
    public int readUShort() throws IOException {
        return (int) readULong(2);
    }

    public long readUInt() throws IOException {
        return readULong(4);
    }

    public long readULong() throws IOException {
        return readULong(8);
    }

    public short readShort() throws IOException {
        ByteBuffer byte_buffer = ByteBuffer.wrap(readBytes(2));
        checkLSF(byte_buffer);
        return byte_buffer.getShort();
    }

    public int readInt() throws IOException {
        ByteBuffer byte_buffer = ByteBuffer.wrap(readBytes(4));
        checkLSF(byte_buffer);
        return byte_buffer.getInt();
    }

    public long readULong(int n) throws IOException {
        byte[] raw_bytes = readBytes(n);
        if (LSF == null) {
            throw new IOException("Byte order not determined for reading numeric values.");
        }

        if (n != 2 && n != 4 && n != 6 && n != 8) {
            throw new IOException("Unsupported number of bytes in an integer: " + n);
        }
        long ret = 0;
        short unsigned_byte_value;

        for (int i = 0; i < n; i++) {
            if (LSF) {
                unsigned_byte_value = raw_bytes[i];
            } else {
                unsigned_byte_value = raw_bytes[n - i - 1];
            }

            if (unsigned_byte_value < 0) {
                unsigned_byte_value += 256;
            }

            ret += unsigned_byte_value * (1L << (8 * i));
        }
        if(ret < 0){
            throw new IOException("Sorry for hoping this wouldn't be used with values over 2^63-1");
        }
        return ret;
    }

    // Floating point reader methods: 
    public double readDouble() throws IOException {
        ByteBuffer byte_buffer = ByteBuffer.wrap(readBytes(8));
        checkLSF(byte_buffer);
        return byte_buffer.getDouble();
    }

    public float readFloat() throws IOException {
        ByteBuffer byte_buffer = ByteBuffer.wrap(readBytes(4));
        checkLSF(byte_buffer);
        return byte_buffer.getFloat();
    }


    /* 
     * Method for reading character strings:
     *
     * readString() reads NULL-terminated strings; i.e. it chops the 
     * string at the first zero encountered. 
     * we probably need an alternative, readRawString(), that reads 
     * a String as is. 
     */
    public String readString(int n) throws IOException {

        String ret = new String(readBytes(n), "US-ASCII");

        // Remove the terminating and/or padding zero bytes:
        if (ret != null && ret.indexOf(0) > -1) {
            return ret.substring(0, ret.indexOf(0));
        }
        return ret;
    }
    
    /* 
     * Same, but expecting potential Unicode characters.
     */
    public String readUtfString(int n) throws IOException {

        String ret = new String(readBytes(n), "UTF8");

        // Remove the terminating and/or padding zero bytes:
        if (ret.indexOf(0) > -1) {
            return ret.substring(0, ret.indexOf(0));
        }
        return ret;
    }

    /* 
     * More complex helper methods for reading NewDTA "sections" ...
     */
    public byte[] readPrimitiveSection(String tag) throws IOException {
        readOpeningTag(tag);
        byte[] ret = readPrimitiveSectionBytes();
        readClosingTag(tag);
        return ret;
    }

    public byte[] readPrimitiveSection(String tag, int length) throws IOException {
        readOpeningTag(tag);
        byte[] ret = readBytes(length);
        readClosingTag(tag);
        return ret;
    }

    public String readPrimitiveStringSection(String tag) throws IOException {
        return new String(readPrimitiveSection(tag), "US-ASCII");
    }

    public String readPrimitiveStringSection(String tag, int length) throws IOException {
        return new String(readPrimitiveSection(tag, length), "US-ASCII");
    }

    public String readLabelSection(String tag, int limit) throws IOException {
        readOpeningTag(tag);
        /**
         * ll The byte length of the UTF-8 characters, whose length is
         * recorded in a 2-byte unsigned integer encoded according to
         * byteorder.
         */
        int lengthOfLabel = readUShort();
        logger.fine("length of label: " + lengthOfLabel);
        String label = null;
        if (lengthOfLabel > 0) {
            label = new String(readBytes(lengthOfLabel), "US-ASCII");
        }
        logger.fine("ret: " + label);
        readClosingTag(tag);
        return label;
    }

    /* 
     * This method reads a string section the length of which is *defined*.
     * the format of the section is as follows: 
     * <tag>Lxxxxxx...x</tag>
     * where L is a single byte specifying the length of the enclosed 
     * string; followed by L bytes.
     * L must be within 
     * 0 <= L <= limit
     * (for example, the "dataset label" is limited to 80 characters).
     */
    public String readDefinedStringSection(String tag, int limit) throws IOException {
        readOpeningTag(tag);
        short number = readUByte();
        logger.fine("number: " + number);
        if (number < 0 || number > limit) {
            throw new IOException("<more than limit characters in the section \"tag\">");
        }
        String ret = null;
        if (number > 0) {
            ret = new String(readBytes(number), "US-ASCII");
        }
        logger.fine("ret: " + ret);
        readClosingTag(tag);
        return ret;
    }

    public long readIntegerSection(String tag, int n) throws IOException {
        readOpeningTag(tag);
        long number = readULong(n);
        readClosingTag(tag);
        return number;
    }

    // This helper method is used for skipping the <ch>llll...</ch> sections
    // inside the "<charachteristics>" section; where llll is a 4-byte unsigned
    // int followed by llll bytes.
    public void skipDefinedSections(String tag) throws IOException {
        logger.fine("entering at offset " + buffer_byte_offset);
        while (checkTag("<" + tag + ">")) {
            logger.fine("tag " + tag + " encountered at offset " + buffer_byte_offset);
            readOpeningTag(tag);
            long number = readULong(4);
            logger.fine(number + " bytes in this section;");
            if (number < 0) {
                throw new IOException("<negative number of bytes in skipDefinedSection(\"tag\")?>");
            }
            byte[] skipped_bytes = readBytes((int) number);
            readClosingTag(tag);
            logger.fine("read closing tag </" + tag + ">;");

        }
        logger.fine("exiting at offset " + buffer_byte_offset);
    }

    public boolean checkTag(String tag) throws IOException {
        if (tag == null || tag.equals("")) {
            throw new IOException("opening tag must be a non-empty string.");
        }

        int n = tag.length();
        if ((this.buffer_size - buffer_byte_offset) >= n) {
            return (tag).equals(new String(Arrays.copyOfRange(buffer, buffer_byte_offset, buffer_byte_offset+n),"US-ASCII"));
        }
        else{
            bufferMoreBytes();
            return checkTag(tag);
        }

    }

    public void readOpeningTag(String tag) throws IOException {
        if (tag == null || tag.equals("")) {
            throw new IOException("opening tag must be a non-empty string.");
        }

        String openTagString = new String(readBytes(tag.length() + 2), "US-ASCII");
        if (openTagString == null || !openTagString.equals("<"+tag+">")) {
            throw new IOException("Could not read opening tag <"+tag+">");
        }
    }

    public void readClosingTag(String tag) throws IOException {
        if (tag == null || tag.equals("")) {
            throw new IOException("closing tag must be a non-empty string.");
        }

        String closeTagString = new String(readBytes(tag.length() + 3), "US-ASCII");
        logger.fine("closeTagString: " + closeTagString);

        if (closeTagString == null || !closeTagString.equals("</" + tag + ">")) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String msg = "";
            for (int i = 0; i < 10; i++) {
                StackTraceElement stackTraceElement = stackTrace[i];
                msg += stackTraceElement.toString() + "\n";
            }
            throw new IOException("Could not read closing tag </" + tag + ">: " + msg);
        }
    }

    private byte[] readPrimitiveSectionBytes() throws IOException {
        byte[] cached_bytes = null;

        if (buffer_byte_offset > this.buffer_size) {
            throw new IOException("Buffer overflow in DataReader.");
        }
        if (buffer_byte_offset == this.buffer_size) {
            // buffer empty; 
            bufferMoreBytes();
        }

        int cached_offset = buffer_byte_offset;

        while (buffer[buffer_byte_offset] != '<') {
            buffer_byte_offset++;

            if (buffer_byte_offset == this.buffer_size) {
                logger.fine("reached the end of buffer in readPrimitiveSectionBytes; offset " + buffer_byte_offset);
                cached_bytes = mergeCachedBytes(cached_bytes, cached_offset);
                bufferMoreBytes();
                cached_offset = 0;
            }
        }

        return mergeCachedBytes(cached_bytes, cached_offset);
    }

    private byte[] mergeCachedBytes(byte[] cached_bytes, int cached_offset) throws IOException {

        byte[] ret_bytes;
        if (cached_bytes == null) {
            if (buffer_byte_offset - cached_offset < 0) {
                throw new IOException("Error merging internal read buffer (no bytes cached to merge)");
            }
            // empty section - as in <section></section>
            if (buffer_byte_offset - cached_offset == 0) {
                return null;
            }

            ret_bytes = new byte[buffer_byte_offset - cached_offset];
            System.arraycopy(buffer, cached_offset, ret_bytes, 0, buffer_byte_offset - cached_offset);
        } else {
            if (cached_offset != 0) {
                throw new IOException("Error merging internal read buffer (non-zero cached offset)");
            }
            ret_bytes = new byte[cached_bytes.length + buffer_byte_offset];
            System.arraycopy(cached_bytes, 0, ret_bytes, 0, cached_bytes.length);
            if (buffer_byte_offset > 0) {
                System.arraycopy(buffer, 0, ret_bytes, cached_bytes.length, buffer_byte_offset);
            }
        }
        return ret_bytes;
    }

}
