package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonReader;
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
import javax.xml.xpath.XPathExpressionException;

/**
 * OAuth2 identity provider for ORCiD. Note that ORCiD has two systems: sandbox
 * and production. Hence having the user endpoint as a parameter.
 * @author michael
 */
public class OrcidOAuth2AP extends AbstractOAuth2AuthenticationProvider {
    
    final static Logger logger = Logger.getLogger(OrcidOAuth2AP.class.getName());

    public static final String PROVIDER_ID_PRODUCTION = "orcid";
    public static final String PROVIDER_ID_SANDBOX = "orcid-sandbox";
    
    public OrcidOAuth2AP(String clientId, String clientSecret, String userEndpoint) {
        scope = "/read-limited"; 
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
    public BaseApi getApiInstance() {
        return OrcidApi.instance( ! baseUserEndpoint.contains("sandbox") );
    }

    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        try ( StringReader reader = new StringReader(responseBody)) {
            DocumentBuilder db = dbFact.newDocumentBuilder();
            Document doc = db.parse( new InputSource(reader) );
            //List<Node> orcidIdNodeList = getNodes(doc, "orcid-message", "orcid-profile","orcid-identifier","path");
            List<Node> orcidIdNodeList = getNodes(doc, "record:record", "common:orcid-identifier","common:path");
            if ( orcidIdNodeList.size() != 1 ) {
                throw new OAuth2Exception(0, responseBody, "Cannot find ORCiD id in response.");
            }
            String orcidId = orcidIdNodeList.get(0).getTextContent().trim();
            //String firstName = getNodes(doc, "orcid-message", "orcid-profile", "orcid-bio", "personal-details", "given-names" )
            String firstName = getNodes(doc, "record:record", "person:person", "person:name", "personal-details:given-names" )
                                .stream().findFirst().map( Node::getTextContent )
                                    .map( String::trim ).orElse("");
            //String familyName = getNodes(doc, "orcid-message", "orcid-profile", "orcid-bio", "personal-details", "family-name" )
            String familyName = getNodes(doc, "record:record", "person:person", "person:name", "personal-details:family-name")
                                .stream().findFirst().map( Node::getTextContent )
                                    .map( String::trim ).orElse("");
            //String affiliation = getNodes(doc, "orcid-message", "orcid-profile", "orcid-activities", "affiliations", "affiliation", "organization", "name" )
            String affiliation = getNodes(doc, "record:record", "activities:activities-summary", "activities:employments", "employment:employment-summary", "employment:organization", "common:name")
                                .stream().findFirst().map( Node::getTextContent )
                                    .map( String::trim ).orElse("");
            //List<String> emails = new ArrayList<>();
            List<String> emails = getAllEmails(doc);
	    /*
            getNodes(doc, "orcid-message", "orcid-profile", "orcid-bio","contact-details","email").forEach( n ->{
               String email = n.getTextContent().trim();
               Node primaryAtt = n.getAttributes().getNamedItem("primary");
               boolean isPrimary = (primaryAtt!=null) && 
                                   (primaryAtt.getTextContent()!=null) &&
                                   (primaryAtt.getTextContent().trim().toLowerCase().equals("true"));
               if ( isPrimary ) {
                   emails.add(0, email);
               } else {
                   emails.add(email);
               }
            });
            String primaryEmail = (emails.size()>1) ? emails.get(0) : "";
	    */
	    String primaryEmail = getPrimaryEmail(doc);
	    /*
	    if ( "" != primaryEmail )
	    {
		    emails.add( primaryEmail );
	    }
	    */
            
            // make the username up
            String username;
            if ( primaryEmail.length() > 0 ) {
                username = primaryEmail.split("@")[0];
            } else {
                username = firstName.split(" ")[0] + "." + familyName;
            }
            
            final ParsedUserResponse userResponse = new ParsedUserResponse(
                    new AuthenticatedUserDisplayInfo(firstName, familyName, primaryEmail, affiliation, ""), orcidId, username);
            userResponse.emails.addAll(emails);
            
            return userResponse;
            
        } catch (SAXException ex) {
            logger.log(Level.SEVERE, "XML error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "I/O error parsing response body from ORCiD: " + ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            logger.log(Level.SEVERE, "While parsing the ORCiD response: Bad parse configuration. " + ex.getMessage(), ex);
        } catch (OAuth2Exception ex) {
            logger.log(Level.SEVERE, "Semantic error parsing response body from ORCiD: " + ex.getMessage(), ex);
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
	    // xmlstarlet sel -t -c "/record:record/person:person/email:emails/email:email[@primary='true']/email:email"
    /**
     * retrieve email from ORCID 2.0 response document, or empty string if no primary email is present
     */
    private String getPrimaryEmail(Document doc)
    {
	    String p = "/record/person/emails/email[@primary='true']/email/text()";
	    NodeList emails = xpath_matches( doc, p );
	    String primary_email  = "";
	    if ( 1 == emails.getLength() )
	    {
		    primary_email = emails.item(0).getTextContent();
	    }
	    // if there are no (or somehow more than 1) primary email(s), then we've already at failure value
	    return primary_email;
    }
    private List<String> getAllEmails(Document doc)
    {
	    String p = "/record/person/emails/email/email/text()";
	    NodeList emails = xpath_matches( doc, p );
	    List<String> rs = new ArrayList<String>();
	    for(int i=0;i<emails.getLength(); ++i) // no iterator in NodeList
	    {
		    rs.add( emails.item(i).getTextContent() );
	    }
	    return rs;
    }
    /**
     * xpath search wrapper; return list of nodes matching an xpath expression (or null, 
     * if there are no matches)
     */
    private NodeList xpath_matches(Document doc, String pattern)
    {
	    XPathFactory xpf = XPathFactory.newInstance();
	    XPath xp = xpf.newXPath();
	    NodeList matches = null;
	    try
	    {
		    XPathExpression srch = xp.compile( pattern );
		    matches = (NodeList) srch.evaluate(doc, XPathConstants.NODESET);
	    }
	    catch( javax.xml.xpath.XPathExpressionException xpe )
	    {
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
        return "http://orcid.org/";
    }

    @Override
    public String getLogo() {
        return "/resources/images/orcid_16x16.png";
    }

}
