
package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.ExternalIdentifier;
import edu.harvard.iq.dataverse.RorAffiliationDetails;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author stephenkraffmiller
 */
public class AffiliationResolverUtil {

    /**
     * Resolves ROR details from an affiliation string and external vocabulary
     * lookup function.
     *
     * @param rawAffiliation
     * @param vocabularyLookup
     * @return
     */
    public static RorAffiliationDetails resolveRorDetails(
            String rawAffiliation) {

        Map<String, String> attributeMap = new HashMap<>();
        String orgName = rawAffiliation;

        if (ExternalIdentifier.ROR.isValidIdentifier(rawAffiliation)) {
            JsonObject jo = getExternalVocabularyValue(rawAffiliation);
            if (jo != null && jo.containsKey("termName")) {
                JsonValue termName = jo.get("termName");
                if (termName.getValueType() == JsonValue.ValueType.STRING) {
                    orgName = ((JsonString) termName).getString();
                }
            }

            attributeMap.put("schemeURI", "https://ror.org");
            attributeMap.put("affiliationIdentifierScheme", "ROR");
            attributeMap.put("affiliationIdentifier", rawAffiliation);
        }

        return new RorAffiliationDetails(orgName, attributeMap);
    }
    
    private static JsonObject getExternalVocabularyValue(String id) {
        return CDI.current()
                .select(DatasetFieldServiceBean.class)
                .get()
                .getExternalVocabularyValue(id);
    }
}
