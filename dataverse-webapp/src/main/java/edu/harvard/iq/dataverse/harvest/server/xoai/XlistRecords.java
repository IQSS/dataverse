
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xml.exceptions.XmlWriteException;
import static com.lyncode.xoai.model.oaipmh.Granularity.Second;
import com.lyncode.xoai.model.oaipmh.ListRecords;
import com.lyncode.xoai.model.oaipmh.Record;
import com.lyncode.xoai.model.oaipmh.ResumptionToken;
import com.lyncode.xoai.xml.XmlWriter;
import static com.lyncode.xoai.xml.XmlWriter.defaultContext;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Leonid Andreev
 * 
 * This is the Dataverse extension of XOAI ListRecords, 
 * optimized to stream individual records using fast dumping 
 * of pre-exported metadata fragments (and by-passing expensive 
 * XML parsing and writing). 
 */
public class XlistRecords extends ListRecords {
    private static final String RECORD_FIELD = "record";
    private static final String RECORD_START_ELEMENT = "<"+RECORD_FIELD+">";
    private static final String RECORD_CLOSE_ELEMENT = "</"+RECORD_FIELD+">";
    private static final String RESUMPTION_TOKEN_FIELD = "resumptionToken";
    private static final String EXPIRATION_DATE_ATTRIBUTE = "expirationDate";
    private static final String COMPLETE_LIST_SIZE_ATTRIBUTE = "completeListSize"; 
    private static final String CURSOR_ATTRIBUTE = "cursor";
    
    public void writeToStream(OutputStream outputStream) throws IOException {
        if (!this.records.isEmpty()) {
            for (Record record : this.records) {
                outputStream.write(RECORD_START_ELEMENT.getBytes());
                outputStream.flush();

                ((Xrecord)record).writeToStream(outputStream);

                outputStream.write(RECORD_CLOSE_ELEMENT.getBytes());
                outputStream.flush();
            }
        }
        
        if (resumptionToken != null) {
            
            String resumptionTokenString = resumptionTokenToString(resumptionToken);
            if (resumptionTokenString == null) {
                throw new IOException("XlistRecords: failed to output resumption token");
            }
            outputStream.write(resumptionTokenString.getBytes());
            outputStream.flush();
        }
    }
    
    private String resumptionTokenToString(ResumptionToken token) {
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            XmlWriter writer = new XmlWriter(byteOutputStream, defaultContext());

            writer.writeStartElement(RESUMPTION_TOKEN_FIELD);
            
            if (token.getExpirationDate() != null)
                writer.writeAttribute(EXPIRATION_DATE_ATTRIBUTE, token.getExpirationDate(), Second);
            if (token.getCompleteListSize() != null)
                writer.writeAttribute(COMPLETE_LIST_SIZE_ATTRIBUTE, "" + token.getCompleteListSize());
            if (token.getCursor() != null)
                writer.writeAttribute(CURSOR_ATTRIBUTE, "" + token.getCursor());
            if (token.getValue() != null)
                writer.write(token.getValue());
            
            writer.writeEndElement(); // resumptionToken;
            writer.flush();
            writer.close();

            String ret = byteOutputStream.toString();

            return ret;
        } catch (XMLStreamException | XmlWriteException e) {
            return null;
        }
    }
    
}
