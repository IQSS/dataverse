 
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.xml.XmlWriter;

/**
 *
 * @author Leonid Andreev
 */
public class Xmetadata extends Metadata {
    
    
    public Xmetadata(String value) {
        super(value);
    }
    
    
    @Override
    public void write(XmlWriter writer) throws XmlWriteException {
        // Do nothing!
        // - rather than writing Metadata as an XML writer stram, we will write 
        // the pre-exported *and pre-validated* content as a byte stream (below).
    }
    
}
