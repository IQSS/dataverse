package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenData;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.validation.constraints.NotNull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

/**
 * OAuth2 identity provider for ORCiD. Note that ORCiD has two systems: sandbox
 * and production. Hence having the user endpoint as a parameter.
 * 
 * @author michael
 * @author pameyer
 */
public class OrcidOAuth2AP extends AbstractOAuth2AuthenticationProvider {
    
    final static Logger logger = Logger.getLogger(OrcidOAuth2AP.class.getName());

    public static final String PROVIDER_ID_PRODUCTION = "orcid";
    public static final String PROVIDER_ID_SANDBOX = "orcid-sandbox";
    
    public OrcidOAuth2AP(String clientId, String clientSecret, String userEndpoint) {
    
        if(userEndpoint != null && userEndpoint.startsWith("https://pub")) {
            this.scope = Arrays.asList("/authenticate");
        } else {
            this.scope = Arrays.asList("/read-limited");
        }
        
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUserEndpoint = userEndpoint;
    }
    
    @Override
    public String getUserEndpoint( OAuth2AccessToken token )  {
        try ( StringReader sRdr = new StringReader(token.getRawResponse());
                JsonReader jRdr = Json.createReader(sRdr) ) {
            String orcid = jRdr.readObject().getString("orcid");
            return baseUserEndpoint.replace("{ORCID}", orcid);
        }
    }
    
    @Override
    public DefaultApi20 getApiInstance() {
        return OrcidApi.instance( ! baseUserEndpoint.contains("sandbox") );
    }
    
    @Override
    final protected OAuth2UserRecord getUserRecord(@NotNull String responseBody, @NotNull OAuth2AccessToken accessToken, @NotNull OAuth20Service service)
        throws OAuth2Exception {
        
        // parse the main response
        final ParsedUserResponse parsed = parseUserResponse(responseBody);
        
        // mixin org data, but optional
        try {
            Optional<AuthenticatedUserDisplayInfo> orgData = getOrganizationalData(accessToken, service);
            if (orgData.isPresent()) {
                parsed.displayInfo.setAffiliation(orgData.get().getAffiliation());
                parsed.displayInfo.setPosition(orgData.get().getPosition());
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not get affiliation data from ORCiD due to an IO problem: {0}", ex.getLocalizedMessage());
        }
        
        // mixin ORCiD not present in main response
        String orcidNumber = extractOrcidNumber(accessToken.getRawResponse());
    
        return new OAuth2UserRecord(getId(), orcidNumber,
            parsed.username,
            OAuth2TokenData.from(accessToken),
            parsed.displayInfo,
            parsed.emails);
    }
    
    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        try ( StringReader reader = new StringReader(responseBody)) {
            DocumentBuilder db = dbFact.newDocumentBuilder();
            Document doc = db.parse( new InputSource(reader) );
            
            String firstName = getNodes(doc, "person:person", "person:name", "personal-details:given-names" )
                                .stream().findFirst().map( Node::getTextContent )
                                    .map( String::trim ).orElse("");
            String familyName = getNodes(doc, "person:person", "person:name", "personal-details:family-name")
                                .stream().findFirst().map( Node::getTextContent )
                                    .map( String::trim ).orElse("");
            
            // fallback - try to use the credit-name
            if ( (firstName + familyName).equals("") ) {
                firstName = getNodes(doc, "person:person", "person:name", "personal-details:credit-name" )
                                .stream().findFirst().map( Node::getTextContent )
                                    .map( String::trim ).orElse("");
            }
            
            String primaryEmail = getPrimaryEmail(doc);
            List<String> emails = getAllEmails(doc);
            
            // make the username up
            String username;
            if ( primaryEmail.length() > 0 ) {
                username = primaryEmail.split("@")[0];
            } else {
                username = firstName.split(" ")[0] + "." + familyName;
            }
            username = username.replaceAll("[^a-zA-Z0-9.]","");
            
            // returning the parsed user. The user-id-in-provider will be added by the caller, since ORCiD passes it
            // on the access token response.
            // Affilifation added after a later call.
            final ParsedUserResponse userResponse = new ParsedUserResponse(
                    new AuthenticatedUserDisplayInfo(firstName, familyName, primaryEmail, "", ""), null, username);
            userResponse.emails.addAll(emails);
            
            return userResponse;
            
        } catch (SAXException ex) {
            logger.log(Level.SEVERE, "XML error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "I/O error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            logger.log(Level.SEVERE, "While parsing the ORCiD response: Bad parse configuration. " + ex.getMessage(), ex);
        }
        
        return null;
    }
    
    private List<Node> getNodes( Node node, String... path ) {
        return getNodes( node, Arrays.asList(path) );
    }
    
    private List<Node> getNodes( Node node, List<String> path ) {
        NodeList childs = node.getChildNodes();
        final Stream<Node> nodeStream = IntStream.range(0, childs.getLength())
                .mapToObj( childs::item )
                .filter( n -> n.getNodeName().equals(path.get(0)) );
        
        if ( path.size() == 1 ) {
            // accumulate and return mode
            return nodeStream.collect( Collectors.toList() );
            
        } else {
            // dig-in mode.
            return nodeStream.findFirst()
                             .map( n -> getNodes(n, path.subList(1, path.size())))
                             .orElse( Collections.<Node>emptyList() );
        }
        
    }
    
    /**
     * retrieve email from ORCID 2.0 response document, or empty string if no primary email is present
     */
    private String getPrimaryEmail(Document doc) {
	    // `xmlstarlet sel -t -c "/record:record/person:person/email:emails/email:email[@primary='true']/email:email"`, if you're curious
	    String p = "/person/emails/email[@primary='true']/email/text()";
	    NodeList emails = xpathMatches( doc, p );
	    String primaryEmail  = "";
	    if ( 1 == emails.getLength() ) {
		    primaryEmail = emails.item(0).getTextContent();
	    }
	    // if there are no (or somehow more than 1) primary email(s), then we've already at failure value
	    return primaryEmail;
    }
    
    /**
     * retrieve all emails (including primary) from ORCID 2.0 response document
     */
    private List<String> getAllEmails(Document doc) {
	    String p = "/person/emails/email/email/text()";
	    NodeList emails = xpathMatches( doc, p );
	    List<String> rs = new ArrayList<>();
	    for(int i=0;i<emails.getLength(); ++i) { // no iterator in NodeList
		    rs.add( emails.item(i).getTextContent() );
	    }
	    return rs;
    }
    
    /**
     * xpath search wrapper; return list of nodes matching an xpath expression (or null, 
     * if there are no matches)
     */
    private NodeList xpathMatches(Document doc, String pattern) {
	    XPathFactory xpf = XPathFactory.newInstance();
	    XPath xp = xpf.newXPath();
	    NodeList matches = null;
	    try {
		    XPathExpression srch = xp.compile( pattern );
		    matches = (NodeList) srch.evaluate(doc, XPathConstants.NODESET);
            
	    } catch( javax.xml.xpath.XPathExpressionException xpe ) {
		    //no-op; intended for hard-coded xpath expressions that won't change at runtime
	    }
	    return matches;
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        if (PROVIDER_ID_PRODUCTION.equals(getId())) {
            return new AuthenticationProviderDisplayInfo(getId(), BundleUtil.getStringFromBundle("auth.providers.title.orcid"), "ORCID user repository");
        }
        return new AuthenticationProviderDisplayInfo(getId(), "ORCID Sandbox", "ORCID dev sandbox ");
    }

    @Override
    public boolean isDisplayIdentifier() {
        return true;
    }

    @Override
    public String getPersistentIdName() {
        return BundleUtil.getStringFromBundle("auth.providers.persistentUserIdName.orcid");
    }

    @Override
    public String getPersistentIdDescription() {
        return BundleUtil.getStringFromBundle("auth.providers.persistentUserIdTooltip.orcid");
    }

    @Override
    public String getPersistentIdUrlPrefix() {
        return "https://orcid.org/";
    }

    @Override
    public String getLogo() {
        return "/resources/images/orcid_16x16.png";
    }
    
    protected String extractOrcidNumber( String rawResponse ) throws OAuth2Exception {
        try ( JsonReader rdr = Json.createReader( new StringReader(rawResponse)) ) {
            JsonObject tokenData = rdr.readObject();
            return tokenData.getString("orcid");
        } catch ( Exception e ) {
            throw new OAuth2Exception(0, rawResponse, "Cannot find ORCiD id in access token response.");
        }
    }

    protected Optional<AuthenticatedUserDisplayInfo> getOrganizationalData(OAuth2AccessToken accessToken, OAuth20Service service) throws IOException {
        
        OAuthRequest request = new OAuthRequest(Verb.GET, getUserEndpoint(accessToken).replace("/person", "/employments"));
        request.setCharset("UTF-8");
        service.signRequest(accessToken, request);
        
        try {
            Response response = service.execute(request);
            int responseCode = response.getCode();
            String responseBody = response.getBody();
    
            if (responseCode != 200 && responseBody != null) {
                // This is bad, but not bad enough to stop a signup/in process.
                logger.log(Level.WARNING, "Cannot get affiliation data from ORCiD. Response code: {0} body:\n{1}\n/body",
                    new Object[]{responseCode, responseBody});
                return Optional.empty();
                
            } else {
                return Optional.of(parseActivitiesResponse(responseBody));
            }
        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.WARNING, "Could not get affiliation data from ORCiD due to threading problems.");
            return Optional.empty();
        }
    }
    
    protected AuthenticatedUserDisplayInfo parseActivitiesResponse( String responseBody ) {
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        
        try ( StringReader reader = new StringReader(responseBody)) {
            DocumentBuilder db = dbFact.newDocumentBuilder();
            Document doc = db.parse( new InputSource(reader) );
            String organization = getNodes(doc, "activities:employments", 
                                  "employment:employment-summary", "employment:organization", "common:name")
                    .stream().findFirst().map( Node::getTextContent )
                    .map( String::trim ).orElse(null);
            
            String department = getNodes(doc, "activities:employments", "employment:employment-summary", "employment:department-name").stream()
                                    .findFirst().map( Node::getTextContent ).map( String::trim ).orElse(null);
            String role = getNodes(doc, "activities:employments", "employment:employment-summary", "employment:role-title").stream()
                                    .findFirst().map( Node::getTextContent ).map( String::trim ).orElse(null);
            
            String position = Stream.of(role, department).filter(Objects::nonNull).collect( joining(", "));
            
            return new AuthenticatedUserDisplayInfo(null, null, null, organization, position);
            
        } catch (SAXException ex) {
            logger.log(Level.SEVERE, "XML error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "I/O error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            logger.log(Level.SEVERE, "While parsing the ORCiD response: Bad parse configuration. " + ex.getMessage(), ex);
        }
        
        return null;   
    }
}
