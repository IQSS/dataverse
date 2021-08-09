package edu.harvard.iq.dataverse.ingest;

import java.io.Serializable;

/**
 * Event that should be fired when ingest preparation task are finished.
 * It should trigger ingesting of specific files.
 *
 * @author dbojanek
 */
public class IngestMessageSendEvent implements Serializable {

    private static final long serialVersionUID = -1289769569178639830L;

    private IngestMessage ingestMessage;

    // -------------------- CONSTRUCTORS --------------------

    public IngestMessageSendEvent(IngestMessage ingestMessage) {
        this.ingestMessage = ingestMessage;
    }

    // -------------------- GETTERS --------------------

    public IngestMessage getIngestMessage() {
        return ingestMessage;
    }
}
