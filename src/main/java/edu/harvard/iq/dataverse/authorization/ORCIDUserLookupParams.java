package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;

public class ORCIDUserLookupParams extends OAuthUserLookupParams {

    private static final String ORCID_BASE_URL = "http://orcid.org/";
    private static final String ORCID_BASE_URL_HTTPS = "https://orcid.org/";

    public ORCIDUserLookupParams(String userId) {
        super(userId);
    }

    @Override
    public String getAuthenticatedUserId() {
        return extractIdFromUrl(userId);
    }

    @Override
    public String getProviderId() {
        return OrcidOAuth2AP.PROVIDER_ID;
    }

    /**
     * Extracts the ORCID iD from a full ORCID URL.
     * <p>
     * This method checks if the provided string starts with "http://orcid.org/" or "https://orcid.org/"
     * and, if so, returns the trailing part of the string. If the string does not
     * match the base URL, it is returned as-is, assuming it might already be the ID.
     *
     * @param orcidUrlOrId The full ORCID URL (e.g., "http://orcid.org/0009-0007-1267-8782")
     *                     or an ORCID iD itself.
     * @return The extracted ORCID iD (e.g., "0009-0007-1267-8782"), or the original string if it's not a URL.
     * Returns null if the input is null.
     */
    private static String extractIdFromUrl(String orcidUrlOrId) {
        if (orcidUrlOrId == null) {
            return null;
        }
        if (orcidUrlOrId.startsWith(ORCID_BASE_URL)) {
            return orcidUrlOrId.substring(ORCID_BASE_URL.length());
        }
        if (orcidUrlOrId.startsWith(ORCID_BASE_URL_HTTPS)) {
            return orcidUrlOrId.substring(ORCID_BASE_URL_HTTPS.length());
        }
        // If it's not a URL, assume it's already the ID.
        return orcidUrlOrId;
    }
}
