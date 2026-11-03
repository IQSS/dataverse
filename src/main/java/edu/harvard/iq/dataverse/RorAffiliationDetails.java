
package edu.harvard.iq.dataverse;

import java.util.Map;

/**
 *
 * @author stephenkraffmiller
 */
public class RorAffiliationDetails {
    private final String orgName;
    private final Map<String, String> attributes;

    public RorAffiliationDetails(String orgName, Map<String, String> attributes) {
        this.orgName = orgName;
        this.attributes = attributes;
    }

    public String getOrgName() { return orgName; }
    public Map<String, String> getAttributes() { return attributes; }
}
