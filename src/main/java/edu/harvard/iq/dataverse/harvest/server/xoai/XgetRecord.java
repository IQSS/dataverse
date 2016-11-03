/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xoai.model.oaipmh.GetRecord;
import com.lyncode.xoai.model.oaipmh.Record;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Leonid Andreev
 * 
 * This is the Dataverse extension of XOAI GetRecord, 
 * optimized to stream individual records to the output directly
 */

public class XgetRecord extends GetRecord {
    private static final String RECORD_FIELD = "record";
    private static final String RECORD_START_ELEMENT = "<"+RECORD_FIELD+">";
    private static final String RECORD_CLOSE_ELEMENT = "</"+RECORD_FIELD+">";
    private static final String RESUMPTION_TOKEN_FIELD = "resumptionToken";
    private static final String EXPIRATION_DATE_ATTRIBUTE = "expirationDate";
    private static final String COMPLETE_LIST_SIZE_ATTRIBUTE = "completeListSize"; 
    private static final String CURSOR_ATTRIBUTE = "cursor";
    
    
    public XgetRecord(Xrecord record) {
        super(record);
    }
    
    public void writeToStream(OutputStream outputStream) throws IOException {

        if (this.getRecord() == null) {
            throw new IOException("XgetRecord: null Record");
        }
        Xrecord xrecord = (Xrecord) this.getRecord();

        outputStream.write(RECORD_START_ELEMENT.getBytes());
        outputStream.flush();

        xrecord.writeToStream(outputStream);

        outputStream.write(RECORD_CLOSE_ELEMENT.getBytes());
        outputStream.flush();

    }

}
