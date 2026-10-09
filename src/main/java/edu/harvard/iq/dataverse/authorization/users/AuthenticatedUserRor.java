package edu.harvard.iq.dataverse.authorization.users;

import edu.harvard.iq.dataverse.ExternalIdentifier;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A ROR (Research Organization Registry) identifier associated with an
 * {@link AuthenticatedUser}. A user can have several, kept in order by
 * {@code displayOrder}; the first one is the user's primary ROR (see
 * {@link AuthenticatedUser#getPrimaryRor()}).
 */
@Entity
@Table(indexes = {@Index(columnList = "authenticateduser_id"),
                  @Index(columnList = "rorid")})
public class AuthenticatedUserRor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "authenticateduser_id", nullable = false)
    private AuthenticatedUser authenticatedUser;

    // Full URL form, e.g. https://ror.org/03vek6s52 (same convention as authenticatedOrcid)
    @Column(nullable = false, length = 255)
    private String rorId;

    @Column(nullable = false)
    private int displayOrder;

    public AuthenticatedUserRor() {
    }

    public AuthenticatedUserRor(AuthenticatedUser authenticatedUser, String rorId, int displayOrder) {
        this.authenticatedUser = authenticatedUser;
        this.rorId = rorId;
        this.displayOrder = displayOrder;
    }

    /**
     * Normalizes a ROR identifier to its full URL form.
     *
     * @param input a bare ROR ID ("03vek6s52") or a ROR URL ("https://ror.org/03vek6s52")
     * @return the ROR URL, or {@code null} if the input is not a valid ROR identifier
     */
    public static String normalizeRorId(String input) {
        if (input == null) {
            return null;
        }
        String candidate = input.trim().toLowerCase(Locale.ROOT);
        if (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        // Use getPattern() rather than isValidIdentifier(), which shares one Matcher across threads
        if (!ExternalIdentifier.ROR.getPattern().matcher(candidate).matches()) {
            return null;
        }
        return ExternalIdentifier.ROR.format(candidate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public String getRorId() {
        return rorId;
    }

    public void setRorId(String rorId) {
        this.rorId = rorId;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AuthenticatedUserRor)) {
            return false;
        }
        AuthenticatedUserRor other = (AuthenticatedUserRor) object;
        // Unsaved instances (null id) are only equal to themselves
        return id != null && id.equals(other.id);
    }

    @Override
    public String toString() {
        return "AuthenticatedUserRor[ id=" + id + ", rorId=" + rorId + ", displayOrder=" + displayOrder + " ]";
    }
}
