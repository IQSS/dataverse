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

    static String generateYear()
    {
        StringBuilder guid = new StringBuilder();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        guid.append(calendar.get(Calendar.YEAR));

        return guid.toString();
    }

    static String generateTimeString()
    {
        StringBuilder guid = new StringBuilder();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        guid.append(calendar.get(Calendar.YEAR));
        guid.append(calendar.get(Calendar.DAY_OF_YEAR));
        guid.append(calendar.get(Calendar.HOUR_OF_DAY));
        guid.append(calendar.get(Calendar.MINUTE));
        guid.append(calendar.get(Calendar.SECOND));
        guid.append(calendar.get(Calendar.MILLISECOND));
        double random = Math.random();
        guid.append(random);

        return guid.toString();
    }
}
