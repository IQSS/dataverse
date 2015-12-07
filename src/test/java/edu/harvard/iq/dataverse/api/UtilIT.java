package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;

public class UtilIT {

    private static final Logger logger = Logger.getLogger(UtilIT.class.getCanonicalName());

    public static final String keyString = "X-Dataverse-key";

    static String getRestAssuredBaseUri() {
        String saneDefaultInDev = "http://localhost:8080";
        String restAssuredBaseUri = saneDefaultInDev;
        String specifiedUri = System.getProperty("dataverse.test.baseurl");
        if (specifiedUri != null) {
            restAssuredBaseUri = specifiedUri;
        }
        logger.info("Base URL for tests: " + restAssuredBaseUri);
        return restAssuredBaseUri;
    }

}
