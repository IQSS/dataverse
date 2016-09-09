 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.web;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.xml.XmlWriter;
import edu.harvard.iq.dataverse.Dataset;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Leonid Andreev
 */
public class xMetadata extends Metadata {
    //private InputStream inputStream;
    //private Dataset dataset;
    //private boolean unread;
    
    
    public xMetadata(String value) {
        super(value);
    }
    
    /*public xMetadata(Dataset dataset) throws IOException {
        super((String)null);
        //this.inputStream = value;
        //this.unread = true;
        //this.dataset = dataset;
    }*/

    
    @Override
    public void write(XmlWriter writer) throws XmlWriteException {
        // Do nothing!
        // - rather than writing Metadata as an XML writer stram, we will write 
        // the pre-exported *and pre-validated* content as a byte stream (below).
    }
    
    /*
    public Dataset getDataset() {
        return dataset;
    }
    
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }*/
    
    /*
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

    }*/
    
    /*
    public InputStream getMetadataInputStream() throws IOException {
        if (unread && inputStream != null) {
            return inputStream; 
        }
        
        throw new IOException ("No InputStream for the metadata record, or InputStream has already been read.");
    }
*/
}
