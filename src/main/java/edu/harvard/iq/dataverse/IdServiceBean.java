package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.CommandContext;

import java.util.*;

/** TODO Property for Dataset and CommandContext. Apply to DOIDataCiteServiceBean and DOIEZIdServiceBean. Refactor PidServiceBean to match this interface? */
public interface IdServiceBean {

    boolean alreadyExists(Dataset dataset) throws Exception;

    String createIdentifier(Dataset dataset) throws Exception;

    HashMap getIdentifierMetadata(Dataset dataset);

    HashMap lookupMetadataFromIdentifier(String protocol, String authority, String separator, String identifier);

    String getIdentifierForLookup(String protocol, String authority, String separator, String identifier);

    String modifyIdentifier(Dataset dataset, HashMap<String, String> metadata) throws Exception;

    void deleteIdentifier(Dataset datasetIn) throws Exception;

    HashMap getMetadataFromStudyForCreateIndicator(Dataset datasetIn);

    HashMap getMetadataFromDatasetForTargetURL(Dataset datasetIn);

    String getIdentifierFromDataset(Dataset dataset);

    boolean publicizeIdentifier(Dataset studyIn);

    static IdServiceBean getBean(String protocol, String doiProvider, CommandContext ctxt) {
        // TODO rather put the configured bean in the ctxt
        if ("hdl".equals(protocol))
            return (ctxt.handleNet());
        else if (protocol.equals("doi"))
            if (doiProvider.equals("EZID"))
                return ctxt.doiEZId();
            else if (doiProvider.equals("DataCite"))
                return ctxt.doiDataCite();
            else throw new UnsupportedOperationException("Unknown doiProvider: " + doiProvider);
        else throw new UnsupportedOperationException("Unknown protocol: " + protocol);
    }

    String generateYear();

    String generateTimeString();
}
