package edu.harvard.iq.dataverse.authorization.common;

import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.OAuth2TokenData;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Describes a single user on a remote IDP
 * that uses OAuth2 or SAML.
 *
 * @author michael
 */
public class ExternalIdpUserRecord implements Serializable {

    private final String serviceId;

    /**
     * An immutable value, probably a number. Not a username that may change.
     */
    private final String idInService;

    /**
     * A potentially mutable String that is easier on the eye than a number.
     */
    private final String username;

    private final AuthenticatedUserDisplayInfo displayInfo;

    private final List<String> availableEmailAddresses;

    private final OAuth2TokenData tokenData;

    // -------------------- CONSTRUCTORS --------------------

    public ExternalIdpUserRecord(String serviceId, String idInService, String username,
                                 OAuth2TokenData tokenData, AuthenticatedUserDisplayInfo displayInfo,
                                 List<String> availableEmailAddresses) {
        this.serviceId = serviceId;
        this.idInService = idInService;
        this.username = username;
        this.tokenData = tokenData;
        this.displayInfo = displayInfo;
        this.availableEmailAddresses = availableEmailAddresses;
    }

    public ExternalIdpUserRecord(String serviceId, String idInService, String username,
                                 AuthenticatedUserDisplayInfo displayInfo) {
        this(serviceId, idInService, username, null, displayInfo, Collections.emptyList());
    }

    // -------------------- GETTERS --------------------

    public String getServiceId() {
        return serviceId;
    }

    public String getIdInService() {
        return idInService;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getAvailableEmailAddresses() {
        return availableEmailAddresses;
    }

    public AuthenticatedUserDisplayInfo getDisplayInfo() {
        return displayInfo;
    }

    public OAuth2TokenData getTokenData() {
        return tokenData;
    }

    // -------------------- LOGIC --------------------

    public UserRecordIdentifier toUserRecordIdentifier() {
        return new UserRecordIdentifier(serviceId, idInService);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "ExternalIdpUserRecord{" + "serviceId=" + serviceId + ", idInService=" + idInService + '}';
    }
}
