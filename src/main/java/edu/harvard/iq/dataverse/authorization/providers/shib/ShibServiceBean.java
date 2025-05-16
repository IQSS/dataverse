package edu.harvard.iq.dataverse.authorization.providers.shib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.authorization.AuthTestDataServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

@Named
@Stateless
public class ShibServiceBean {

    private static final Logger logger = Logger.getLogger(ShibServiceBean.class.getCanonicalName());

    @EJB
    AuthenticationServiceBean authSvc;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    @EJB
    AuthTestDataServiceBean authTestDataService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    SettingsServiceBean settingsService;

    private static final String INCOMMON_MDQ_API_BASE = "https://mdq.incommon.org";
    private static final String INCOMMON_MDQ_API_ENTITIES_URL = INCOMMON_MDQ_API_BASE + "/entities/";
    private static final String INCOMMON_WAYFINDER_URL = "https://wayfinder.incommon.org";
    
    /**
     * "Production" means "don't mess with the HTTP request".
     */
    public enum DevShibAccountType {

        PRODUCTION,
        RANDOM,
        TESTSHIB1,
        HARVARD1,
        HARVARD2,
        TWO_EMAILS,
        INVALID_EMAIL,
        EMAIL_WITH_LEADING_SPACE,
        UID_WITH_LEADING_SPACE,
        IDENTIFIER_WITH_LEADING_SPACE,
        MISSING_REQUIRED_ATTR,
        ONE_AFFILIATION,
        TWO_AFFILIATIONS,
    };

    public DevShibAccountType getDevShibAccountType() {
        DevShibAccountType saneDefault = DevShibAccountType.PRODUCTION;
        String settingReturned = settingsService.getValueForKey(SettingsServiceBean.Key.DebugShibAccountType);
        logger.fine("setting returned: " + settingReturned);
        if (settingReturned != null) {
            try {
                DevShibAccountType parsedValue = DevShibAccountType.valueOf(settingReturned);
                return parsedValue;
            } catch (IllegalArgumentException ex) {
                logger.info("Couldn't parse value: " + ex + " - returning a sane default: " + saneDefault);
                return saneDefault;
            }
        } else {
            logger.fine("Shibboleth dev mode has not been configured. Returning a sane default: " + saneDefault);
            return saneDefault;
        }
    }

    /**
     * This method exists so developers don't have to run Shibboleth locally.
     * You can populate the request with Shibboleth attributes by changing a
     * setting like this:
     *
     * curl -X PUT -d RANDOM
     * http://localhost:8080/api/admin/settings/:DebugShibAccountType
     *
     * When you're done, feel free to delete the setting:
     *
     * curl -X DELETE
     * http://localhost:8080/api/admin/settings/:DebugShibAccountType
     *
     * Note that setting ShibUseHeaders to true will make this "dev mode" stop
     * working.
     */
    public void possiblyMutateRequestInDev(HttpServletRequest request) {
        switch (getDevShibAccountType()) {
            case PRODUCTION:
                logger.fine("Request will not be mutated");
                break;

            case RANDOM:
                mutateRequestForDevRandom(request);
                break;

            case TESTSHIB1:
                ShibUtil.mutateRequestForDevConstantTestShib1(request);
                break;

            case HARVARD1:
                ShibUtil.mutateRequestForDevConstantHarvard1(request);
                break;

            case HARVARD2:
                ShibUtil.mutateRequestForDevConstantHarvard2(request);
                break;

            case TWO_EMAILS:
                ShibUtil.mutateRequestForDevConstantTwoEmails(request);
                break;

            case INVALID_EMAIL:
                ShibUtil.mutateRequestForDevConstantInvalidEmail(request);
                break;

            case EMAIL_WITH_LEADING_SPACE:
                ShibUtil.mutateRequestForDevConstantEmailWithLeadingSpace(request);
                break;

            case UID_WITH_LEADING_SPACE:
                ShibUtil.mutateRequestForDevConstantUidWithLeadingSpace(request);
                break;

            case IDENTIFIER_WITH_LEADING_SPACE:
                ShibUtil.mutateRequestForDevConstantIdentifierWithLeadingSpace(request);
                break;

            case MISSING_REQUIRED_ATTR:
                ShibUtil.mutateRequestForDevConstantMissingRequiredAttributes(request);
                break;

            case ONE_AFFILIATION:
                ShibUtil.mutateRequestForDevConstantOneAffiliation(request);
                break;

            case TWO_AFFILIATIONS:
                ShibUtil.mutateRequestForDevConstantTwoAffiliations(request);
                break;

            default:
                logger.info("Should never reach here");
                break;
        }
    }

    public AuthenticatedUser findAuthUserByEmail(String emailToFind) {
        return authSvc.getAuthenticatedUserByEmail(emailToFind);
    }

    public BuiltinUser findBuiltInUserByAuthUserIdentifier(String authUserIdentifier) {
        return builtinUserService.findByUserName(authUserIdentifier);
    }

    public String getAffiliation(String shibIdp, DevShibAccountType devShibAccountType) {
        if (!devShibAccountType.equals(DevShibAccountType.PRODUCTION)) {
            return getAffiliationFromDiscoFeed(shibIdp, devShibAccountType);
        }
        return getAffiliationViaMDQ(shibIdp);
    }

    public String getAffiliationViaMDQ(String shibIdp) {
        String entityIdEncoded =  URLEncoder.encode(shibIdp, StandardCharsets.UTF_8);
        String apiUrl = INCOMMON_MDQ_API_ENTITIES_URL + entityIdEncoded; //"https://mdq.incommon.org/entities/https%3A%2F%2Ffed.huit.harvard.edu%2Fidp%2Fshibboleth"
        
        logger.fine("cooked Incommon MDQ url: " + apiUrl);
        
        URL url = null;
        try {
            url = new URL(apiUrl);
        } catch (MalformedURLException ex) {
            logger.warning(ex.toString());
            return null;
        }
        if (url == null) {
            logger.warning("MDQ url object was null after parsing " + apiUrl);
            return null;
        }
        
        HttpURLConnection mdqApiRequest = null;
        try {
            mdqApiRequest = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            logger.warning(ex.toString());
            return null;
        }
        if (mdqApiRequest == null) {
            logger.warning("mdq api request was null");
            return null;
        }
        try {
            mdqApiRequest.connect();
        } catch (IOException ex) {
            logger.warning(ex.toString());
            return null;
        }
        
        XMLStreamReader xmlr = null;

        try {
            XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
            xmlr =  xmlFactory.createXMLStreamReader(new InputStreamReader((InputStream) mdqApiRequest.getInputStream()));
            
            while ( xmlr.next() == XMLStreamConstants.COMMENT );
            xmlr.require(XMLStreamConstants.START_ELEMENT, null, "EntityDescriptor");
            
            while (xmlr.hasNext()) {
                int event = xmlr.next();
                
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String currentElement = xmlr.getLocalName();
                    
                    if ("".equals(currentElement)) {
                        int eventType = xmlr.next();
                        if (eventType == XMLStreamConstants.CHARACTERS) {
                            String affiliation = xmlr.getText();
                            return affiliation;
                        } else {
                            logger.warning("Unexpected contet in the OrganizationDisplayName element");
                            return null; 
                        }
                    }
                    
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (xmlr.getLocalName().equals("EntityDescriptor")) return null;
                }       
            }
            
        } catch (IOException ioex) {
            logger.warning("IOException instantiating a stream reader of the mdq api output" + ioex.getMessage());
        } catch (XMLStreamException xsex) {
            logger.warning("Failed to parse the xml output of the mdq api; " + xsex.getMessage());
        } finally {
            if (xmlr != null) {
                try {
                    logger.fine("closing xml reader");
                    xmlr.close();
                } catch (XMLStreamException xsex) {
                    // do we care? 
                }
            }
        }

        logger.warning("Failed to find an affiliation for " + shibIdp);
        return null;
    }

    /*
     * This is the old-style method of obtaining the affiliation - by calling 
     * the DiscoFeed, provided by the locally-running shibd instance, finding 
     * the provider in the full json-formatted list and selecting its "display
     * name". It is kept in the code for now, under the assumption that somebody 
     * may still have reasons to keep using the DiscoFeed-based model.    
    **/
    public String getAffiliationFromDiscoFeed(String shibIdp, DevShibAccountType devShibAccountType) {   
        JsonArray emptyJsonArray = new JsonArray();
        String discoFeedJson = emptyJsonArray.toString();
        String discoFeedUrl;
        if (devShibAccountType.equals(DevShibAccountType.PRODUCTION)) {
            discoFeedUrl = systemConfig.getDataverseSiteUrl() + "/Shibboleth.sso/DiscoFeed";
        } else {
            String devUrl = "http://localhost:8080/resources/dev/sample-shib-identities.json";
            discoFeedUrl = devUrl;
        }
        logger.fine("Trying to get affiliation from disco feed URL: " + discoFeedUrl);
        URL url = null;
        try {
            url = new URL(discoFeedUrl);
        } catch (MalformedURLException ex) {
            logger.info(ex.toString());
            return null;
        }
        if (url == null) {
            logger.info("url object was null after parsing " + discoFeedUrl);
            return null;
        }
        HttpURLConnection discoFeedRequest = null;
        try {
            discoFeedRequest = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            logger.info(ex.toString());
            return null;
        }
        if (discoFeedRequest == null) {
            logger.info("disco feed request was null");
            return null;
        }
        try {
            discoFeedRequest.connect();
        } catch (IOException ex) {
            logger.info(ex.toString());
            return null;
        }
        JsonParser jp = new JsonParser();
        JsonElement root = null;
        try {
            root = jp.parse(new InputStreamReader((InputStream) discoFeedRequest.getInputStream()));
        } catch (IOException ex) {
            logger.info(ex.toString());
            return null;
        }
        if (root == null) {
            logger.info("root was null");
            return null;
        }
        JsonArray rootArray = root.getAsJsonArray();
        if (rootArray == null) {
            logger.info("Couldn't get JSON Array from URL");
            return null;
        }
        discoFeedJson = rootArray.toString();
        logger.fine("Dump of disco feed:" + discoFeedJson);
        String affiliation = ShibUtil.getDisplayNameFromDiscoFeed(shibIdp, discoFeedJson);
        if (affiliation != null) {
            return affiliation;
        } else {
            logger.info("Couldn't find an affiliation from  " + shibIdp);
            return null;
        }
    }

    /* 
     * The redirect URL for initiating the Shibboleth authentication redirect
     * loop using the new InCommon WayFinder service. There are four redirects
     * total in a succesfully completed workflow:
     * -> Wayfinder -> local shibd -> shib.xhtml -> final Dataverse page
     * all four of the above steps are encoded in the initial redirect url, 
     * hence the multi-level url encoding in some of its parts.
    */
    public String getWayfinderRedirectUrl() {
        String encodedEntityId = URLEncoder.encode(getServiceProviderEntityId(), StandardCharsets.UTF_8);
        
        // "targetUrl" is the THIRD level redirect - i.e., this where the locally-
        // running shibd will bounce the user once it receives the redirect back 
        // from InCommon/Wayfinder (which is the SECOND redirect in the loop). 
        // Note that this is a fixed location, the Dataverse page shib.xhtml, 
        // where, in the underlying bean, the actual magic of translating the 
        // SAML attributes into a Dataverse user session happens. 
        // A FOURTH redirect, to the actual destination Dataverse page will be 
        // added, as a redirectPage parameter for the shib.xhtml page when the 
        // final redirect URL is put together in LoginPage.java. 
        String targetUrl = URLEncoder.encode(SystemConfig.getDataverseSiteUrlStatic() + "/shib.xhtml", StandardCharsets.UTF_8);
        // "returnUrl" is the SECOND redirect, that Wayfinder is going to issue, 
        // back to the local shibd instance. This location is also fixed, always
        // pointing to /Shibboleth.sso/Login?SAMLDS=1
        String returnUrl = URLEncoder.encode(SystemConfig.getDataverseSiteUrlStatic() + "/Shibboleth.sso/Login?SAMLDS=1", StandardCharsets.UTF_8);;
        String wayFinderUrl = INCOMMON_WAYFINDER_URL + "/?entityID=" + encodedEntityId
                + "&return=" + returnUrl 
                + "%2526target%3D" + targetUrl; //"%253FredirectPage%253D%25252Fdataverse.xhtml";
        return wayFinderUrl; 
    }
    
    /* 
     * This is the entityID of the *local* Shibboleth service provider - i.e.,
     * the registered id of the shibd instance running locally. 
     * This id is looked up once, in the Singleton that instantiates all 
     * Authentication Providers, on startup and cached in the 
     * ShibAuthenticationProvider instance. 
     * This entity id is needed when generating WayFinder authentication 
     * redirects to InCommon. 
    **/
    private String getServiceProviderEntityId() {
        String shibProvId = ShibAuthenticationProvider.PROVIDER_ID;
        ShibAuthenticationProvider shibAuthProvider = (ShibAuthenticationProvider)authSvc.getAuthenticationProvider(shibProvId); 
        String ourServiceProviderEntityId = shibAuthProvider.getServiceProviderEntityId();
        
        return ourServiceProviderEntityId; 
    }
    
    private void mutateRequestForDevRandom(HttpServletRequest request) {
        Map<String, String> randomUser = authTestDataService.getRandomUser();
        request.setAttribute(ShibUtil.lastNameAttribute, randomUser.get("lastName"));
        request.setAttribute(ShibUtil.firstNameAttribute, randomUser.get("firstName"));
        request.setAttribute(ShibUtil.emailAttribute, randomUser.get("email"));
        request.setAttribute(ShibUtil.shibIdpAttribute, randomUser.get("idp"));
        // eppn
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, UUID.randomUUID().toString().substring(0, 8));
    }

}
