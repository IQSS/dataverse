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
import edu.harvard.iq.dataverse.util.xml.XmlUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jakarta.json.JsonObject;
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
    
    static final Logger logger = Logger.getLogger(OrcidOAuth2AP.class.getName());

    public static final String PROVIDER_ID = "orcid";

    private static final String ROR_URL_PREFIX = "https://ror.org/";
    private static final Pattern ROR_ID = Pattern.compile("^(?:https?://ror\\.org/)?(0[a-z0-9]{8})$");

    public OrcidOAuth2AP(String clientId, String clientSecret, String userEndpoint) {
        this.id=PROVIDER_ID;
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
        String orcid = JsonUtil.getJsonObject(token.getRawResponse()).getString("orcid");
        return baseUserEndpoint.replace("{ORCID}", orcid);
    }
    
    @Override
    public DefaultApi20 getApiInstance() {
        return OrcidApi.instance( isProduction() );
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
                parsed.displayInfo.setAffiliationRor(orgData.get().getAffiliationRor());
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
        try ( StringReader reader = new StringReader(responseBody)) {
            DocumentBuilder db = XmlUtil.getSecureDocumentBuilder();
            if (db != null) {
                Document doc = db.parse(new InputSource(reader));

                String firstName = getNodes(doc, "person:person", "person:name", "personal-details:given-names")
                        .stream().findFirst().map(Node::getTextContent)
                        .map(String::trim).orElse("");
                String familyName = getNodes(doc, "person:person", "person:name", "personal-details:family-name")
                        .stream().findFirst().map(Node::getTextContent)
                        .map(String::trim).orElse("");

                // fallback - try to use the credit-name
                if ((firstName + familyName).equals("")) {
                    firstName = getNodes(doc, "person:person", "person:name", "personal-details:credit-name")
                            .stream().findFirst().map(Node::getTextContent)
                            .map(String::trim).orElse("");
                }

                String primaryEmail = getPrimaryEmail(doc);
                List<String> emails = getAllEmails(doc);

                // make the username up
                String username;
                if (primaryEmail.length() > 0) {
                    username = primaryEmail.split("@")[0];
                } else {
                    username = firstName.split(" ")[0] + "." + familyName;
                }
                username = username.replaceAll("[^a-zA-Z0-9.]", "");

                // returning the parsed user. The user-id-in-provider will be added by the caller, since ORCiD passes it
                // on the access token response.
                // Affiliation added after a later call.
                final ParsedUserResponse userResponse = new ParsedUserResponse(
                        new AuthenticatedUserDisplayInfo(firstName, familyName, primaryEmail, "", ""), null, username);
                userResponse.emails.addAll(emails);

                return userResponse;
            }
        } catch (SAXException ex) {
            logger.log(Level.SEVERE, "XML error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "I/O error parsing response body from ORCiD: " + ex.getMessage(), ex);
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
        if (isProduction()) {
            return new AuthenticationProviderDisplayInfo(getId(), BundleUtil.getStringFromBundle("auth.providers.title.orcid"), "ORCID user repository");
        }
        return new AuthenticationProviderDisplayInfo(getId(), "ORCID Sandbox", "ORCID dev sandbox ");
    }

    public String getPersistentIdUrlPrefix() {
        if(isProduction()) {
            return "https://orcid.org/";
        }
        return "https://sandbox.orcid.org/";
    }

    public final static String getLogo() {
        return "/resources/images/orcid_16x16.png";
    }
    
    protected String extractOrcidNumber( String rawResponse ) throws OAuth2Exception {
        try {
            JsonObject tokenData = JsonUtil.getJsonObject(rawResponse);
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
        DocumentBuilderFactory dbFact = edu.harvard.iq.dataverse.util.xml.XmlUtil.getSecureDocumentBuilderFactory();
        
        try ( StringReader reader = new StringReader(responseBody)) {
            DocumentBuilder db = dbFact.newDocumentBuilder();
            Document doc = db.parse( new InputSource(reader) );
            // v2.x lists employment summaries directly, v3.0 wraps each one in an affiliation group,
            // and the two use different prefixes for the same elements, so match on local names.
            List<Node> employments = getEmploymentSummaries(doc);
            Optional<Node> employment = employments.stream()
                    .filter(summary -> getChildren(summary, "end-date").isEmpty())
                    .max(Comparator.comparing(OrcidOAuth2AP::getStartDate))
                    .or(() -> employments.stream().findFirst());

            Optional<Node> organization = employment.flatMap(summary -> getChild(summary, "organization"));
            String organizationName = organization.flatMap(org -> getChildText(org, "name")).orElse(null);
            String department = employment.flatMap(summary -> getChildText(summary, "department-name")).orElse(null);
            String role = employment.flatMap(summary -> getChildText(summary, "role-title")).orElse(null);

            String position = Stream.of(role, department).filter(Objects::nonNull).collect( joining(", "));

            AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(null, null, null, organizationName, position);
            displayInfo.setAffiliationRor(organization.flatMap(OrcidOAuth2AP::getRorId).orElse(null));
            return displayInfo;

        } catch (SAXException ex) {
            logger.log(Level.SEVERE, "XML error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "I/O error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            logger.log(Level.SEVERE, "While parsing the ORCiD response: Bad parse configuration. " + ex.getMessage(), ex);
        }
        
        return null;
    }

    private static List<Node> getEmploymentSummaries(Document doc) {
        List<Node> summaries = new ArrayList<>();
        for (Node employments : getChildren(doc, "employments")) {
            for (Node group : getChildren(employments, "affiliation-group")) {
                summaries.addAll(getChildren(group, "employment-summary"));
            }
            summaries.addAll(getChildren(employments, "employment-summary"));
        }
        return summaries;
    }

    private static String getStartDate(Node employmentSummary) {
        return getChild(employmentSummary, "start-date")
                .map(date -> Stream.of("year", "month", "day")
                        .map(part -> getChildText(date, part).orElse("00"))
                        .collect(joining("-")))
                .orElse("");
    }

    private static Optional<String> getRorId(Node organization) {
        return getChild(organization, "disambiguated-organization")
                .filter(disambiguated -> getChildText(disambiguated, "disambiguation-source").filter("ROR"::equalsIgnoreCase).isPresent())
                .flatMap(disambiguated -> getChildText(disambiguated, "disambiguated-organization-identifier"))
                .map(ROR_ID::matcher)
                .filter(Matcher::matches)
                .map(matcher -> ROR_URL_PREFIX + matcher.group(1));
    }

    private static List<Node> getChildren(Node node, String localName) {
        NodeList children = node.getChildNodes();
        return IntStream.range(0, children.getLength())
                .mapToObj(children::item)
                .filter(child -> child.getNodeType() == Node.ELEMENT_NODE && localName.equals(getLocalName(child)))
                .collect(Collectors.toList());
    }

    private static Optional<Node> getChild(Node node, String localName) {
        return getChildren(node, localName).stream().findFirst();
    }

    private static Optional<String> getChildText(Node node, String localName) {
        return getChild(node, localName).map(Node::getTextContent).map(String::trim).filter(text -> !text.isEmpty());
    }

    private static String getLocalName(Node node) {
        String name = node.getNodeName();
        return name.substring(name.indexOf(':') + 1);
    }

    public String getOrcidUrl(String id) {
        return (id == null) ? null : getPersistentIdUrlPrefix() + id;
    }
    
    private boolean isProduction() {
        return !baseUserEndpoint.contains("sandbox");
    }
}
