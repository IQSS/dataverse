package edu.harvard.iq.dataverse.authorization;

/**
 * This object decouples the lookup string used by an
 * {@link AuthenticationProvider} from the internal user identifier used for
 * assigning roles, etc.
 *
 * <br>
 *
 * The lookup string for a {@link BuiltinUser} may be "pete". The lookup string
 * for a Shibboleth user may be "https://idp.crash.com/idp/shibboleth|70236e89"
 * 
 * TODO consider removing this class - not sure we need this level of indirection. Causes data duplication in current code.
 */
public class UserIdentifier {

    /**
     * The String used by an Authentication Provider to look up the user within
     * itself. The lookup string for a {@link BuiltinUser} may be "pete". The
     * lookup string for a Shibboleth user may be
     * "https://idp.crash.com/idp/shibboleth|70236e89"
     */
    String lookupStringPerAuthProvider;

    /**
     * The String used in the permission system to assign roles, for example.
     */
    String internalUserIdentifer;

    public UserIdentifier(String lookupStringPerAuthProvider, String internalUserIdentifer) {
        this.lookupStringPerAuthProvider = lookupStringPerAuthProvider;
        this.internalUserIdentifer = internalUserIdentifer;
    }

    public String getLookupStringPerAuthProvider() {
        return lookupStringPerAuthProvider;
    }

    public String getInternalUserIdentifer() {
        return internalUserIdentifer;
    }
}
