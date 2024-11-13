package edu.harvard.iq.dataverse.authorization;

import com.nimbusds.openid.connect.sdk.claims.UserInfo;

/**
 * Encapsulates both the user's identifier ({@link UserRecordIdentifier}) and the user's claims information
 * ({@link UserInfo}) retrieved from an OIDC (OpenID Connect) bearer token.
 * <p>
 * This class serves as a container for both the {@link UserRecordIdentifier}, which uniquely identifies
 * the user within the system, and the {@link UserInfo}, which holds the user's claims data provided by
 * an OIDC provider. It simplifies the management of these related pieces of user data when handling
 * OIDC token validation and authorization processes.
 *
 * @see UserRecordIdentifier
 * @see UserInfo
 */
public class OIDCUserInfo {
    private final UserRecordIdentifier userRecordIdentifier;
    private final UserInfo userClaimsInfo;

    public OIDCUserInfo(UserRecordIdentifier userRecordIdentifier, UserInfo userClaimsInfo) {
        this.userRecordIdentifier = userRecordIdentifier;
        this.userClaimsInfo = userClaimsInfo;
    }

    public UserRecordIdentifier getUserRecordIdentifier() {
        return userRecordIdentifier;
    }

    public UserInfo getUserClaimsInfo() {
        return userClaimsInfo;
    }
}
