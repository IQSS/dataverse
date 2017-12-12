package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

// Dataporten is a part of ScribeJava in the future https://github.com/scribejava/scribejava/pull/805
// import com.github.scribejava.apis.DataportenApi; //Uncomment and delete DataportenApi.java when ScribeJava is updated in Maven
import com.github.scribejava.core.builder.api.BaseApi;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUserNameFields;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonArray;

/**
 *
 * @author ran033@uit.no (Ruben Andreassen)
 */
public class DataportenOAuth2AP extends AbstractOAuth2AuthenticationProvider {
    
    public DataportenOAuth2AP(String aClientId, String aClientSecret) {
        id = "dataporten";
        title = BundleUtil.getStringFromBundle("auth.providers.title.dataporten");
        clientId = aClientId;
        clientSecret = aClientSecret;
        baseUserEndpoint = "https://auth.dataporten.no/userinfo";
    }
    
    @Override
    public BaseApi getApiInstance() {
        return DataportenApi.instance();
    }
    
    @Override
    protected ParsedUserResponse parseUserResponse( String responseBody ) {
        
        try ( StringReader rdr = new StringReader(responseBody);
            JsonReader jrdr = Json.createReader(rdr) )  {
            JsonObject responseObject = jrdr.readObject();
            JsonObject userObject = responseObject.getJsonObject("user");
            JsonArray userid_secArray = userObject.getJsonArray("userid_sec");
            
            String username = "";
            
            /*
            Example reponse
            {
                "user": {
                    "userid": "76a7a061-3c55-430d-8ee0-6f82ec42501f",
                    "userid_sec": ["feide:andreas@uninett.no"],
                    "name": "Andreas \u00c5kre Solberg",
                    "email": "andreas.solberg@uninett.no",
                    "profilephoto": "p:a3019954-902f-45a3-b4ee-bca7b48ab507"
                },
                "audience": "e8160a77-58f8-4006-8ee5-ab64d17a5b1e"
            }
            */
            
            // Extract ad username using regexp
            Pattern p = Pattern.compile("^feide:([0-9a-zA-Z]+?)@.*$");
            Matcher m = p.matcher(userid_secArray.getString(0));
            if(m.matches()) {
                username = m.group(1);
            }
            
            ShibUserNameFields shibUserNameFields = ShibUtil.findBestFirstAndLastName(null, null, userObject.getString("name",""));
            AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo(
                    shibUserNameFields.getFirstName(),
                    shibUserNameFields.getLastName(),
                    userObject.getString("email",""),
                    "", //company
                    ""
            );
            
            return new ParsedUserResponse(
                    displayInfo, 
                    userObject.getString("userid"), //persistentUserId 
                    username, //username
                    displayInfo.getEmailAddress().length()>0 ? Collections.singletonList(displayInfo.getEmailAddress())
                                                             : Collections.emptyList() );

        }
        
    }

    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

    @Override
    public String getPersistentIdName() {
        return BundleUtil.getStringFromBundle("auth.providers.persistentUserIdName.dataporten");
    }

    @Override
    public String getPersistentIdDescription() {
        return BundleUtil.getStringFromBundle("auth.providers.persistentUserIdTooltip.dataporten");
    }

    @Override
    public String getPersistentIdUrlPrefix() {
        return null;
    }

    @Override
    public String getLogo() {
        return null;
    }
}
