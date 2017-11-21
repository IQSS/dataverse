package edu.harvard.iq.dataverse.summarystats;

import edu.harvard.iq.dataverse.DataFile;
import java.io.Serializable;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonReader;

@Stateless
@Named
public class SummaryStatsServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(SummaryStatsServiceBean.class.getCanonicalName());

    public boolean processPrepFile(String jsonIn, DataFile dataFile) {
        boolean persisted = false;
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(jsonIn));
            // The JSON format is the "prep" format. For example:
            // https://dataverse.harvard.edu/api/access/datafile/3040230?format=prep'
            // The JSON format is an object, not an array.
            jsonReader.readObject();
            // FIXME: Once it exists, call the method that does the persisting.
            persisted = true;
        } catch (Exception ex) {
            logger.info("processPrepFile called but there was an Exception parsing the JSON: " + ex);
        }
        return persisted;
    }

}
