package edu.harvard.iq.dataverse.persistence.datafile.ingest;

import javax.ejb.ApplicationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationException(rollback = true)
public class IngestException extends RuntimeException {

    private IngestError errorKey;
    private List<String> errorArguments = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    public IngestException(IngestError errorKey) {
        super("There was a problem during ingest. Passing error key "+ errorKey + " to report.");
        this.errorKey = errorKey;
    }

    public IngestException(IngestError errorKey, Throwable cause) {
        super("There was a problem during ingest. Passing error key "+ errorKey + " to report.", cause);
        this.errorKey = errorKey;
    }

    public IngestException(IngestError errorKey, List<String> errorArguments) {
        super("There was a problem during ingest. Passing error key "+ errorKey + " to report with arguments "+ errorArguments);
        this.errorKey = errorKey;
        this.errorArguments = errorArguments;
    }

    public IngestException(IngestError errorKey, String... errorArguments) {
        super("There was a problem during ingest. Passing error key "+ errorKey + " to report with arguments "+ Arrays.toString(errorArguments));
        this.errorKey = errorKey;
        this.errorArguments = Arrays.asList(errorArguments);
    }

    // -------------------- GETTERS --------------------

    public IngestError getErrorKey() {
        return errorKey;
    }

    public List<String> getErrorArguments() {
        return errorArguments;
    }
}
