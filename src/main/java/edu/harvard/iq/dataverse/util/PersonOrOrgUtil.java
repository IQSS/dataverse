package edu.harvard.iq.dataverse.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import edu.harvard.iq.dataverse.export.openaire.Cleanup;
import edu.harvard.iq.dataverse.export.openaire.FirstNames;
import edu.harvard.iq.dataverse.export.openaire.Organizations;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

/**
 *
 * @author qqmyers
 * 
 *         Adapted from earlier code in OpenAireExportUtil
 * 
 *         Implements an algorithm derived from code at DataCite to determine
 *         whether a name is that of a Person or Organization and, if the
 *         former, to pull out the given and family names.
 * 
 *         Adds parameters that can improve accuracy:
 * 
 *         * e.g. for curated repositories, allowing the code to assume that all
 *         Person entries are in <family name>, <given name> order.
 * 
 *         * allow local configuration of specific words/phrases that will
 *         automatically categorize one-off cases that the algorithm would
 *         otherwise mis-categorize. For example, the code appears to not
 *         recognize names ending in "Project" as an Organization.
 * 
 */

public class PersonOrOrgUtil {

    private static final Logger logger = Logger.getLogger(PersonOrOrgUtil.class.getCanonicalName());

    static boolean assumeCommaInPersonName = false;
    static List<String> orgPhrases;

    static {
        setAssumeCommaInPersonName(Boolean.parseBoolean(System.getProperty("dataverse.personOrOrg.assumeCommaInPersonName", "false")));
        setOrgPhraseArray(System.getProperty("dataverse.personOrOrg.orgPhraseArray", null));
    }

    /**
     * This method tries to determine if a name belongs to a person or an
     * organization and, if it is a person, what the given and family names are. The
     * core algorithm is adapted from a Datacite algorithm, see
     * https://github.com/IQSS/dataverse/issues/2243#issuecomment-358615313
     * 
     * @param name
     *            - the name to test
     * @param organizationIfTied
     *            - if a given name isn't found, should the name be assumed to be
     *            from an organization. This could be a generic true/false or
     *            information from some non-name aspect of the entity, e.g. which
     *            field is in use, or whether a .edu email exists, etc.
     * @param isPerson
     *            - if this is known to be a person due to other info (i.e. they
     *            have an ORCID). In this case the algorithm is just looking for
     *            given/family names.
     * @return
     */
    public static JsonObject getPersonOrOrganization(String name, boolean organizationIfTied, boolean isPerson) {
        name = Cleanup.normalize(name);

        String givenName = null;
        String familyName = null;

        boolean isOrganization = !isPerson && Organizations.getInstance().isOrganization(name);
        if (!isOrganization) {
            for (String phrase : orgPhrases) {
                if (name.contains(phrase)) {
                    isOrganization = true;
                    break;
                }
            }
        }
        if (name.contains(",")) {
            givenName = FirstNames.getInstance().getFirstName(name);
            // contributorName=<FamilyName>, <FirstName>
            if (givenName != null && !isOrganization) {
                // givenName ok
                isOrganization = false;
                // contributor_map.put("nameType", "Personal");
                if (!name.replaceFirst(",", "").contains(",")) {
                    // contributorName=<FamilyName>, <FirstName>
                    String[] fullName = name.split(", ");
                    givenName = fullName[1];
                    familyName = fullName[0];
                }
            } else if (isOrganization || organizationIfTied) {
                isOrganization = true;
                givenName = null;
            }

        } else {
            if (assumeCommaInPersonName && !isPerson) {
                isOrganization = true;
            } else {
                givenName = FirstNames.getInstance().getFirstName(name);

                if (givenName != null && !isOrganization) {
                    isOrganization = false;
                    if (givenName.length() + 1 < name.length()) {
                        familyName = name.substring(givenName.length() + 1);
                    }
                } else {
                    // default
                    if (isOrganization || organizationIfTied) {
                        isOrganization = true;
                        givenName=null;
                    }
                }
            }
        }
        JsonObjectBuilder job = new NullSafeJsonBuilder();
        job.add("fullName", name);
        job.add("givenName", givenName);
        job.add("familyName", familyName);
        job.add("isPerson", !isOrganization);
        return job.build();

    }

    // Public for testing
    public static void setOrgPhraseArray(String phraseArray) {
        orgPhrases = new ArrayList<String>();
        if (!StringUtil.isEmpty(phraseArray)) {
            try {
                JsonArray phrases = JsonUtil.getJsonArray(phraseArray);
                phrases.forEach(val -> {
                    JsonString strVal = (JsonString) val;
                    orgPhrases.add(strVal.getString());
                });
            } catch (Exception e) {
                logger.warning("Could not parse Org phrase list");
            }
        }

    }

    // Public for testing
    public static void setAssumeCommaInPersonName(boolean assume) {
        assumeCommaInPersonName = assume;
    }

}
