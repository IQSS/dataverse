package edu.harvard.iq.dataverse.util;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import edu.harvard.iq.dataverse.export.openaire.Cleanup;
import edu.harvard.iq.dataverse.export.openaire.FirstNames;
import edu.harvard.iq.dataverse.export.openaire.Organizations;
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
 *         Adds a parameter that can improve accuracy, e.g. for curated
 *         repositories, allowing the code to assume that all Person entries are
 *         in <family name>, <given name> order.
 * 
 *         Possible ToDo - one could also allow local configuration of specific
 *         words that will automatically categorize one-off cases that the
 *         algorithm would otherwise mis-categorize. For example, the code
 *         appears to not recognize names ending in "Project" as an
 *         Organization.
 * 
 */

public class PersonOrOrgUtil {

    static boolean assumeCommaInPersonName = false;

    static {
        setAssumeCommaInPersonName(Boolean.parseBoolean(System.getProperty("dataverse.personOrOrg.assumeCommaInPersonName", "false")));
    }

    public static JsonObject getPersonOrOrganization(String name, boolean organizationIfTied) {
        name = Cleanup.normalize(name);

        String givenName = null;
        String familyName = null;
        // adapted from a Datacite algorithm,
        // https://github.com/IQSS/dataverse/issues/2243#issuecomment-358615313
        boolean isOrganization = Organizations.getInstance().isOrganization(name);
        // ToDo - could add a check of stop words to handle problem cases, i.e. if name
        // contains something in that list, it is an org
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
                givenName=null;
            }

        } else {
            if (assumeCommaInPersonName) {
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

    public static void setAssumeCommaInPersonName(boolean assume) {
        assumeCommaInPersonName = assume;
    }

}
