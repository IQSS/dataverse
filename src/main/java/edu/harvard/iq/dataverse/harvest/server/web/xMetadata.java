 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.web;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.xml.XmlWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Leonid Andreev
 */
public class xMetadata extends Metadata {
    private InputStream inputStream; 
    private boolean unread;
    
    public xMetadata(String value) {
        super(value);
    }
    
    public xMetadata(InputStream value) throws IOException {
        super((String)null);
        this.inputStream = value;
        this.unread = true;
    }

    
    @Override
    public void write(XmlWriter writer) throws XmlWriteException {
        // Do nothing!
        // - rather than writing Metadata as an XML writer stram, the pre-exported
        // *and pre-validated* content will be written as a byte stream (below)
    }
    
    public void writeToStream(OutputStream outputStream) throws IOException {
        InputStream inputStream = getMetadataInputStream();

        outputStream.flush();

        int bufsize;
        byte[] buffer = new byte[4 * 8192];

        while ((bufsize = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bufsize);
            outputStream.flush();
        }

        inputStream.close();
        unread = false;

    }
    
    public InputStream getMetadataInputStream() throws IOException {
        if (unread && inputStream != null) {
            return inputStream; 
        }
        
        throw new IOException ("No InputStream for the metadata record, or InputStream has already been read.");
    }
}
