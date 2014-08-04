package edu.harvard.iq.dataverse.authorization;

import java.util.Map;
import java.util.regex.Pattern;

public class ShibGroup implements Group {

    private Map<String, Pattern> headerMatches;

    public Map<String, Pattern> getHeaderMatches() {
        return headerMatches;
    }

}
