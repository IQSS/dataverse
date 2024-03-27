package edu.harvard.iq.dataverse.authorization.providers.shib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.validation.EMailValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletRequest;

public class ShibUtil {

    private static final Logger logger = Logger.getLogger(ShibUtil.class.getCanonicalName());

    /**
     * @todo make this configurable? See
     * https://github.com/IQSS/dataverse/issues/2129
     */
    public static final String shibIdpAttribute = "Shib-Identity-Provider";
    /**
     * @todo Make attribute used (i.e. "eppn") configurable:
     * https://github.com/IQSS/dataverse/issues/1422
     *
     * OR *maybe* we can rely on people installing Dataverse to configure shibd
     * to always send "eppn" as an attribute, via attribute mappings or what
     * have you.
     */
    public static final String uniquePersistentIdentifier = "eppn";
    public static final String usernameAttribute = "uid";
    public static final String displayNameAttribute = "cn";
    public static final String firstNameAttribute = "givenName";
    public static final String lastNameAttribute = "sn";
    public static final String emailAttribute = "mail";
    public static final String testShibIdpEntityId = "https://idp.testshib.org/idp/shibboleth";

    /**
     * Used to display "Harvard University", for example, based on
     * https://dataverse.harvard.edu/Shibboleth.sso/DiscoFeed
     */
    public static String getDisplayNameFromDiscoFeed(String entityIdToFind, String discoFeed) {
        JsonParser jsonParser = new JsonParser();
        JsonElement root = jsonParser.parse(discoFeed);
        JsonArray identityProviders = root.getAsJsonArray();
        for (JsonElement identityProvider : identityProviders) {
            JsonObject provider = identityProvider.getAsJsonObject();
            JsonElement entityId = provider.get("entityID");
            if (entityId != null) {
                if (entityId.getAsString().equals(entityIdToFind)) {
                    JsonElement displayNamesElement = provider.get("DisplayNames");
                    if (displayNamesElement != null) {
                        JsonArray displayNamesArray = displayNamesElement.getAsJsonArray();
                        JsonElement firstDisplayName = displayNamesArray.get(0);
                        if (firstDisplayName != null) {
                            JsonObject friendlyNameObject = firstDisplayName.getAsJsonObject();
                            if (friendlyNameObject != null) {
                                JsonElement friendlyNameElement = friendlyNameObject.get("value");
                                if (friendlyNameElement != null) {
                                    String friendlyName = friendlyNameElement.getAsString();
                                    return friendlyName;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param firstName First or "given" name.
     * @param lastName Last or "family" name.
     * @param displayName Only used if first and last are not provided.
     * @return ShibUserNameFields contains separate first and last name fields.
     *
     * @todo Do something more intelligent with displayName. By comparing
     * displayName to the firstName and lastName strings, we should be able to
     * figure out where the proper split is, like this:
     *
     * - "Guido|van Rossum"
     *
     * - "Philip Seymour|Hoffman" (see FirstNameTest.java)
     *
     * Also, we currently compel all Shibboleth IdPs to send us firstName and
     * lastName so the logic to handle null/empty values for firstName and
     * lastName is only currently exercised by the GitHub Identity Provider. As
     * such this method should be moved out of ShibUtil and somewhere that does
     * more generic processing of user information.
     */
    public static ShibUserNameFields findBestFirstAndLastName(String firstName, String lastName, String displayName) {
        firstName = findSingleValue(firstName);
        lastName = findSingleValue(lastName);
        if ((firstName == null || firstName.isEmpty()) && (lastName == null || lastName.isEmpty())) {
            // We're desperate at this point. No firstName, no lastName. Let's try to return something reasonable from displayName.
            if (displayName != null) {
                String[] parts = displayName.split(" ");
                if (parts.length > 1) {
                    return new ShibUserNameFields(parts[0], parts[1]);
                }
            }
        }
        return new ShibUserNameFields(firstName, lastName);
    }

    public static String findSingleValue(String mayHaveMultipleValues) {
        if (mayHaveMultipleValues == null) {
            return null;
        }
        String singleValue = mayHaveMultipleValues;
        String[] parts = mayHaveMultipleValues.split(";");
        if (parts.length != 1) {
            logger.fine("parts (before sorting): " + Arrays.asList(parts));
            // predictable order (sorted alphabetically)
            Arrays.sort(parts);
            logger.fine("parts (after sorting): " + Arrays.asList(parts));
            try {
                String first = parts[0];
                singleValue = first;
            } catch (ArrayIndexOutOfBoundsException ex) {
                /**
                 * @todo Is it possible to reach this line via a test? If not,
                 * remove this try/catch.
                 */
                logger.fine("Couldn't find first part of " + singleValue);
            }
        }
        return singleValue;
    }

    /**
     * @deprecated because of a typo; use {@link #generateFriendlyLookingUserIdentifier(String, String)} instead
     * @see #generateFriendlyLookingUserIdentifier(String, String)
     * @param usernameAssertion
     * @param email
     * @return a friendly-looking user identifier based on the asserted username or email, or a UUID as fallback
     */
    @Deprecated
    public static String generateFriendlyLookingUserIdentifer(String usernameAssertion, String email) {
        return generateFriendlyLookingUserIdentifier(usernameAssertion, email);
    }

    /**
     * @param usernameAssertion
     * @param email
     * @return a friendly-looking user identifier based on the asserted username or email, or a UUID as fallback
     */
    public static String generateFriendlyLookingUserIdentifier(String usernameAssertion, String email) {
        if (usernameAssertion != null && !usernameAssertion.isEmpty()) {
            return usernameAssertion;
        }
        if (email != null && !email.isEmpty()) {
            if (email.contains("@")) {
                String[] parts = email.split("@");
                try {
                    String firstPart = parts[0];
                    return firstPart;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    /**
                     * @todo Is it possible to reach this line via a test? If
                     * not, remove this try/catch.
                     */
                    logger.fine(ex + " parsing " + email);
                }
            } else {
                boolean passedValidation = EMailValidator.isEmailValid(email);
                logger.fine("Odd email address. No @ sign ('" + email + "'). Passed email validation: " + passedValidation);
            }
        } else {
            logger.fine("email attribute not sent by IdP");
        }
        logger.fine("the best we can do is generate a random UUID");
        return UUID.randomUUID().toString();
    }

    static void mutateRequestForDevConstantTestShib1(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, ShibUtil.testShibIdpEntityId);
        // the TestShib "eppn" looks like an email address
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "saml@testshib.org");
//        request.setAttribute(displayNameAttribute, "Sam El");
        request.setAttribute(ShibUtil.firstNameAttribute, "Samuel;Sam");
        request.setAttribute(ShibUtil.lastNameAttribute, "El");
        // TestShib doesn't send "mail" attribute so let's mimic that.
//        request.setAttribute(emailAttribute, "saml@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "saml");
    }

    static void mutateRequestForDevConstantHarvard1(HttpServletRequest request) {
        /**
         * Harvard's IdP doesn't send a username (uid).
         */
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fed.huit.harvard.edu/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "constantHarvard");
        /**
         * @todo Does Harvard really send displayName? At one point they didn't.
         * Let's simulate the non-sending of displayName here.
         */
//        request.setAttribute(displayNameAttribute, "John Harvard");
        request.setAttribute(ShibUtil.firstNameAttribute, "John");
        request.setAttribute(ShibUtil.lastNameAttribute, "Harvard");
        request.setAttribute(ShibUtil.emailAttribute, "jharvard@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "jharvard");
    }

    static void mutateRequestForDevConstantHarvard2(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fed.huit.harvard.edu/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "constantHarvard2");
//        request.setAttribute(displayNameAttribute, "Grace Hopper");
        request.setAttribute(ShibUtil.firstNameAttribute, "Grace");
        request.setAttribute(ShibUtil.lastNameAttribute, "Hopper");
        request.setAttribute(ShibUtil.emailAttribute, "ghopper@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "ghopper");
    }

    static void mutateRequestForDevConstantTwoEmails(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "twoEmails");
        request.setAttribute(ShibUtil.firstNameAttribute, "Eric");
        request.setAttribute(ShibUtil.lastNameAttribute, "Allman");
        request.setAttribute(ShibUtil.emailAttribute, "eric1@mailinator.com;eric2@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "eallman");
    }

    static void mutateRequestForDevConstantInvalidEmail(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "invalidEmail");
        request.setAttribute(ShibUtil.firstNameAttribute, "Invalid");
        request.setAttribute(ShibUtil.lastNameAttribute, "Email");
        request.setAttribute(ShibUtil.emailAttribute, "elisah.da mota@example.com");
        request.setAttribute(ShibUtil.usernameAttribute, "invalidEmail");
    }

    static void mutateRequestForDevConstantEmailWithLeadingSpace(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "leadingWhitespace");
        request.setAttribute(ShibUtil.firstNameAttribute, "leadingWhitespace");
        request.setAttribute(ShibUtil.lastNameAttribute, "leadingWhitespace");
        request.setAttribute(ShibUtil.emailAttribute, " leadingWhitespace@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "leadingWhitespace");
    }

    static void mutateRequestForDevConstantUidWithLeadingSpace(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "leadingWhitespace");
        request.setAttribute(ShibUtil.firstNameAttribute, "leadingWhitespace");
        request.setAttribute(ShibUtil.lastNameAttribute, "leadingWhitespace");
        request.setAttribute(ShibUtil.emailAttribute, "leadingWhitespace@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, " leadingWhitespace");
    }

    // the identifier is the IdP plus the eppn separated by a |
    static void mutateRequestForDevConstantIdentifierWithLeadingSpace(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, " https://fake.example.com/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "leadingWhitespace");
        request.setAttribute(ShibUtil.firstNameAttribute, "leadingWhitespace");
        request.setAttribute(ShibUtil.lastNameAttribute, "leadingWhitespace");
        request.setAttribute(ShibUtil.emailAttribute, "leadingWhitespace@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "leadingWhitespace");
    }

    static void mutateRequestForDevConstantMissingRequiredAttributes(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        /**
         * @todo When shibIdpAttribute is set to null why don't we see the error
         * in the GUI?
         */
//        request.setAttribute(shibIdpAttribute, null);
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "missing");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, null);
        request.setAttribute(ShibUtil.firstNameAttribute, "Missing");
        request.setAttribute(ShibUtil.lastNameAttribute, "Required");
        request.setAttribute(ShibUtil.emailAttribute, "missing@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "missing");
    }

    static void mutateRequestForDevConstantOneAffiliation(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "oneAffiliation");
        request.setAttribute(ShibUtil.firstNameAttribute, "Lurneen");
        request.setAttribute(ShibUtil.lastNameAttribute, "Lumpkin");
        request.setAttribute(ShibUtil.emailAttribute, "oneAffiliaton@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "oneAffiliaton");
        // Affiliation. "ou" is the suggested attribute in :ShibAffiliationAttribute.
        request.setAttribute("ou", "Beer-N-Brawl");
    }

    static void mutateRequestForDevConstantTwoAffiliations(HttpServletRequest request) {
        request.setAttribute(ShibUtil.shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, "twoAffiliatons");
        request.setAttribute(ShibUtil.firstNameAttribute, "Lenny");
        request.setAttribute(ShibUtil.lastNameAttribute, "Leonard");
        request.setAttribute(ShibUtil.emailAttribute, "twoAffiliatons@mailinator.com");
        request.setAttribute(ShibUtil.usernameAttribute, "twoAffiliatons");
        // Affiliation. "ou" is the suggested attribute in :ShibAffiliationAttribute.
        request.setAttribute("ou", "SNPP;Stonecutters");
    }

    public static Map<String, String> getRandomUserStatic() {
        Map<String, String> fakeUser = new HashMap<>();
        String shortRandomString = UUID.randomUUID().toString().substring(0, 8);
        fakeUser.put("firstName", shortRandomString);
        fakeUser.put("lastName", shortRandomString);
        fakeUser.put("displayName", shortRandomString + " " + shortRandomString);
        fakeUser.put("email", shortRandomString + "@mailinator.com");
        fakeUser.put("idp", "https://idp." + shortRandomString + ".com/idp/shibboleth");
        fakeUser.put("username", shortRandomString);
        fakeUser.put("eppn", shortRandomString);
        return fakeUser;
    }

    /**
     * These are attributes that were found to be interesting while developing
     * the Shibboleth feature. Only the ones that are defined elsewhere are
     * actually used.
     */
    static List<String> shibAttrs = Arrays.asList(
            ShibUtil.shibIdpAttribute,
            ShibUtil.uniquePersistentIdentifier,
            ShibUtil.usernameAttribute,
            ShibUtil.displayNameAttribute,
            ShibUtil.firstNameAttribute,
            ShibUtil.lastNameAttribute,
            ShibUtil.emailAttribute,
            "telephoneNumber",
            "affiliation",
            "unscoped-affiliation",
            "entitlement",
            "persistent-id"
    );

    /**
     * These are the attributes we are getting from the IdP at testshib.org, a
     * dump from https://pdurbin.pagekite.me/Shibboleth.sso/Session
     *
     * Miscellaneous
     *
     * Session Expiration (barring inactivity): 479 minute(s)
     *
     * Client Address: 10.0.2.2
     *
     * SSO Protocol: urn:oasis:names:tc:SAML:2.0:protocol
     *
     * Identity Provider: https://idp.testshib.org/idp/shibboleth
     *
     * Authentication Time: 2014-09-12T17:07:36.137Z
     *
     * Authentication Context Class:
     * urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
     *
     * Authentication Context Decl: (none)
     *
     *
     *
     * Attributes
     *
     * affiliation: Member@testshib.org;Staff@testshib.org
     *
     * cn: Me Myself And I
     *
     * entitlement: urn:mace:dir:entitlement:common-lib-terms
     *
     * eppn: myself@testshib.org
     *
     * givenName: Me Myself
     *
     * persistent-id:
     * https://idp.testshib.org/idp/shibboleth!https://pdurbin.pagekite.me/shibboleth!zylzL+NruovU5OOGXDOL576jxfo=
     *
     * sn: And I
     *
     * telephoneNumber: 555-5555
     *
     * uid: myself
     *
     * unscoped-affiliation: Member;Staff
     *
     */
    public static void printAttributes(HttpServletRequest request) {
        List<String> shibValues = new ArrayList<>();
        if (request == null) {
            logger.fine("HttpServletRequest was null. No shib values to print.");
            return;
        }
        for (String attr : shibAttrs) {

            /**
             * @todo explain in Installers Guide that in order for these
             * attributes to be found attributePrefix="AJP_" must be added to
             * /etc/shibboleth/shibboleth2.xml like this:
             *
             * <ApplicationDefaults entityID="https://dataverse.org/shibboleth"
             * REMOTE_USER="eppn" attributePrefix="AJP_">
             *
             */
            Object attrObject = request.getAttribute(attr);
            if (attrObject != null) {
                shibValues.add(attr + ": " + attrObject.toString());
            }
        }
        logger.fine("shib values: " + shibValues);
    }

}
