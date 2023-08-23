package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DOIServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.HandlenetServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;

public class PidUtil {

    private static final Logger logger = Logger.getLogger(PidUtil.class.getCanonicalName());

    /**
     * @throws BadRequestException          if user didn't supply a DOI.
     *
     * @throws NotFoundException            if DOI not found in DataCite.
     *
     * @throws ServiceUnavailableException  if non 200 or non 404 response from
     *                                      DataCite.
     *
     * @throws InternalServerErrorException on local misconfiguration such as
     *                                      DataCite hostname not in DNS.
     */
    public static JsonObjectBuilder queryDoi(GlobalId globalId, String baseUrl, String username, String password) {
        try {
            // This throws an exception if this is not a DOI, which is the only
            // user-supplied param - treat this as a BadRequest in the catch statement.
            String doi = acceptOnlyDoi(globalId);
            URL url;
            // Other errors are all internal misconfiguration (any problems creating the
            // URL), the
            // DOI doesn't exist (404 from DataCite), or problem at DataCite (other non-200
            // responses).
            int status = 0;
            HttpURLConnection connection = null;
            try {
                url = new URL(baseUrl + "/dois/" + doi);

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                String userpass = username + ":" + password;
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
                connection.setRequestProperty("Authorization", basicAuth);

                status = connection.getResponseCode();
            } catch (IOException ex) {
                // Hostname not in DNS, for example.
                throw new InternalServerErrorException(
                        BundleUtil.getStringFromBundle("pids.datacite.errors.noResponseCode", Arrays.asList(baseUrl)));
            }
            if (status == 404) {
                // Could check to see if Dataverse expects the DOI to be registered - that would
                // result in a 404 from Dataverse before having to contact DataCite, and
                // DataCite could still return a 404
                throw new NotFoundException("404 (NOT FOUND) from DataCite for DOI " + globalId);
            }
            if (status != 200) {
                /*
                 * We could just send back whatever status code DataCite sends, but we've seen
                 * DataCite sometimes respond with 403 when the credentials were OK, and their
                 * 500 error doesn't mean a problem with Dataverse, so wrapping any of them in a
                 * 503 error, to indicate this is a temporary error, might be the better option.
                 * In any case, we need to log the issue to be able to debug it.
                 */
                logger.severe("Received " + status + " error from DataCite for DOI: " + globalId);
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    JsonObject out = Json.createReader(connection.getErrorStream()).readObject();
                    logger.severe("DataCite error response: " + out.toString());
                } else {
                    logger.severe("No error stream from DataCite");
                }
                throw new ServiceUnavailableException();
            }
            JsonObject out;
            try {
                out = Json.createReader(connection.getInputStream()).readObject();
            } catch (IOException ex) {
                return Json.createObjectBuilder().add("response", ex.getLocalizedMessage());
            }
            JsonObject data = out.getJsonObject("data");
            String id = data.getString("id");
            JsonObject attributes = data.getJsonObject("attributes");
            String state = attributes.getString("state");
            JsonObjectBuilder ret = Json.createObjectBuilder().add("id", id).add("state", state);
            return ret;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getLocalizedMessage());
        }
    }

    /**
     * @param PID in the form doi:10.7910/DVN/TJCLKP
     * @return DOI in the form 10.7910/DVN/TJCLKP (no "doi:")
     */
    private static String acceptOnlyDoi(GlobalId globalId) {
        if (!DOIServiceBean.DOI_PROTOCOL.equals(globalId.getProtocol())) {
            throw new IllegalArgumentException(BundleUtil.getStringFromBundle("pids.datacite.errors.DoiOnly"));
        }
        return globalId.getAuthority() + "/" + globalId.getIdentifier();
    }

    static Map<String, GlobalIdServiceBean> providerMap = new HashMap<String, GlobalIdServiceBean>();
    static Map<String, GlobalIdServiceBean> unmanagedProviderMap = new HashMap<String, GlobalIdServiceBean>();

    public static void addAllToProviderList(List<GlobalIdServiceBean> list) {
        for (GlobalIdServiceBean pidProvider : list) {
            providerMap.put(pidProvider.getProviderInformation().get(0), pidProvider);
        }
    }

    public static void addAllToUnmanagedProviderList(List<GlobalIdServiceBean> list) {
        for (GlobalIdServiceBean pidProvider : list) {
            unmanagedProviderMap.put(pidProvider.getProviderInformation().get(0), pidProvider);
        }
    }

    /**
     * 
     * @param identifier The string to be parsed
     * @throws IllegalArgumentException if the passed string cannot be parsed.
     */
    public static GlobalId parseAsGlobalID(String identifier) {
        logger.fine("In parseAsGlobalId: " + providerMap.size());
        for (GlobalIdServiceBean pidProvider : providerMap.values()) {
            logger.fine(" Checking " + String.join(",", pidProvider.getProviderInformation()));
            GlobalId globalId = pidProvider.parsePersistentId(identifier);
            if (globalId != null) {
                return globalId;
            }
        }
        // If no providers can managed this PID, at least allow it to be recognized
        for (GlobalIdServiceBean pidProvider : unmanagedProviderMap.values()) {
            logger.fine(" Checking " + String.join(",", pidProvider.getProviderInformation()));
            GlobalId globalId = pidProvider.parsePersistentId(identifier);
            if (globalId != null) {
                return globalId;
            }
        }
        throw new IllegalArgumentException("Failed to parse identifier: " + identifier);
    }

    /**
     * 
     * @param identifier The string to be parsed
     * @throws IllegalArgumentException if the passed string cannot be parsed.
     */
    public static GlobalId parseAsGlobalID(String protocol, String authority, String identifier) {
        logger.fine("Looking for " + protocol + " " + authority + " " + identifier);
        logger.fine("In parseAsGlobalId: " + providerMap.size());
        for (GlobalIdServiceBean pidProvider : providerMap.values()) {
            logger.fine(" Checking " + String.join(",", pidProvider.getProviderInformation()));
            GlobalId globalId = pidProvider.parsePersistentId(protocol, authority, identifier);
            if (globalId != null) {
                return globalId;
            }
        }
        for (GlobalIdServiceBean pidProvider : unmanagedProviderMap.values()) {
            logger.fine(" Checking " + String.join(",", pidProvider.getProviderInformation()));
            GlobalId globalId = pidProvider.parsePersistentId(protocol, authority, identifier);
            if (globalId != null) {
                return globalId;
            }
        }
        // For unit tests which don't have any provider Beans - todo remove when
        // providers are no longer beans and can be configured easily in tests
        return parseUnmanagedDoiOrHandle(protocol, authority, identifier);
        // throw new IllegalArgumentException("Failed to parse identifier from protocol:
        // " + protocol + ", authority:" + authority + ", identifier: " + identifier);
    }
    /*
     * This method should be deprecated/removed when further refactoring to support
     * multiple PID providers is done. At that point, when the providers aren't
     * beans, this code can be moved into other classes that go in the providerMap.
     * If this method is not kept in sync with the DOIServiceBean and
     * HandlenetServiceBean implementations, the tests using it won't be valid tests
     * of the production code.
     */

    private static GlobalId parseUnmanagedDoiOrHandle(String protocol, String authority, String identifier) {
        // Default recognition - could be moved to new classes in the future.
        if (!GlobalIdServiceBean.isValidGlobalId(protocol, authority, identifier)) {
            return null;
        }
        String urlPrefix = null;
        switch (protocol) {
        case DOIServiceBean.DOI_PROTOCOL:
            if (!GlobalIdServiceBean.checkDOIAuthority(authority)) {
                return null;
            }
            urlPrefix = DOIServiceBean.DOI_RESOLVER_URL;
            break;
        case HandlenetServiceBean.HDL_PROTOCOL:
            urlPrefix = HandlenetServiceBean.HDL_RESOLVER_URL;
            break;
        }
        return new GlobalId(protocol, authority, identifier, "/", urlPrefix, null);
    }
}
