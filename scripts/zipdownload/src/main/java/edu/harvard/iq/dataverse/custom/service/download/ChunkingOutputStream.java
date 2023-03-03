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
package edu.harvard.iq.dataverse.custom.service.download;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Simple implementation of HTTP "chunking encoding"
 * 
 * @author Leonid Andreev
 */
public class ChunkingOutputStream extends FilterOutputStream {
    private static final int BUFFER_SIZE = 8192;
    private static final byte[] CHUNK_CLOSE = "\r\n".getBytes();
    private static final String CHUNK_SIZE_FORMAT = "%x\r\n";
    
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int position = 0; 
    
    public ChunkingOutputStream(OutputStream out) {
        super(out);
    }
    
    @Override
    public void write(byte[] data) throws IOException {
        this.write(data, 0, data.length);
    }
    
    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        
        // is this going to fill the buffer?
        if (position + length > BUFFER_SIZE) {
            System.arraycopy(data, offset, buffer, position, BUFFER_SIZE - position);
            offset += (BUFFER_SIZE - position);
            length -= (BUFFER_SIZE - position);
            dumpChunk(buffer, 0, BUFFER_SIZE);
            position = 0; 
        }
        
        // are there still multiple buffer-worths of bytes? 
        while (length > BUFFER_SIZE) {
            dumpChunk(data, offset, BUFFER_SIZE);
            offset += BUFFER_SIZE;
            length -= BUFFER_SIZE;
        }
        
        // finally, buffer the leftover bytes:
        System.arraycopy(data, offset, buffer, position, length);
        position+=length;
        
    }
    
    @Override
    public void write(int i) throws IOException {
        // Hopefully ZipOutputStream never writes single bytes into the stream?
        // Uh, actually it does, *a lot* - at the beginning of the archive, and 
        // when it closes it. 
        
        if (position == BUFFER_SIZE) {
            dumpChunk(buffer, 0, position);
            position = 0;
        }
        buffer[position++] = (byte)i;
    }

    @Override
    public void close() throws IOException {
        if (position > 0) {
            dumpChunk(buffer, 0, position);
        }
        
        // ... and the final, "zero chunk": 
        super.out.write('0');
        super.out.write(CHUNK_CLOSE);
        super.out.write(CHUNK_CLOSE);
        
        super.out.close();
    }
    
    
    private void dumpChunk(byte[] data, int offset, int length) throws IOException {
        String chunkSizeLine = String.format(CHUNK_SIZE_FORMAT, length);
        super.out.write(chunkSizeLine.getBytes());
        super.out.write(data, offset, length);
        // don't forget to close the chunk(!):
        super.out.write(CHUNK_CLOSE);
    }
}
