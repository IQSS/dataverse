package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;

import javax.servlet.http.HttpServletRequest;

import java.util.Objects;

/**
 * A request in the dataverse context. Similar to an HTTP request (and indeed
 * wraps one) but has more data that's specific to the Dataverse application.
 *
 * @author michael
 */
public class DataverseRequest {

    private final User user;
    private final IpAddress sourceAddress;

    // -------------------- CONSTRUCTORS --------------------

    public DataverseRequest(User aUser, HttpServletRequest aHttpServletRequest) {
        this.user = aUser;

        final String undefined = "0.0.0.0";
        String saneDefault = undefined;
        String remoteAddressStr = saneDefault;

        if (aHttpServletRequest != null) {
            String remoteAddressFromRequest = aHttpServletRequest.getRemoteAddr();
            if (remoteAddressFromRequest != null) {
                remoteAddressStr = remoteAddressFromRequest;
            }
        }
        sourceAddress = IpAddress.valueOf(remoteAddressStr);
    }

    public DataverseRequest(User aUser, IpAddress aSourceAddress) {
        user = aUser;
        sourceAddress = aSourceAddress;
    }

    // -------------------- GETTERS --------------------

    public User getUser() {
        return user;
    }

    /**
     * @return The IP address from which this request arrived.
     */
    public IpAddress getSourceAddress() {
        return sourceAddress;
    }

    // -------------------- LOGIC --------------------

    /**
     * Get an AuthenticatedUser or return null
     *
     * @return
     */
    public AuthenticatedUser getAuthenticatedUser() {

        User authUser = this.getUser();

        if (authUser instanceof AuthenticatedUser) {
            return (AuthenticatedUser) authUser;
        }
        return null;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return Objects.hash(sourceAddress, user.getIdentifier());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DataverseRequest other = (DataverseRequest) obj;
        return Objects.equals(sourceAddress, other.sourceAddress)
                && Objects.equals(user.getIdentifier(), other.user.getIdentifier());
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "[DataverseRequest user:" + getUser() + "@" + getSourceAddress() + "]";
    }

}
