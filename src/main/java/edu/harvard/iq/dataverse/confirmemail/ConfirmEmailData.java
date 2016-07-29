package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 *
 * @author bsilverstein
 */
@Table(indexes = {
    @Index(columnList = "token"),
    @Index(columnList = "authenticateduser_id")})
@NamedQueries({
    @NamedQuery(name = "ConfirmEmailData.findAll",
            query = "SELECT prd FROM ConfirmEmailData prd"),
    @NamedQuery(name = "ConfirmEmailData.findByUser",
            query = "SELECT prd FROM ConfirmEmailData prd WHERE prd.authenticatedUser = :user"),
    @NamedQuery(name = "ConfirmEmailData.findByToken",
            query = "SELECT prd FROM ConfirmEmailData prd WHERE prd.token = :token")
})
@Entity
public class ConfirmEmailData implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String token;

    @OneToOne
    @JoinColumn(nullable = false, unique = true)
    private AuthenticatedUser authenticatedUser;

    @Column(nullable = false)
    private Timestamp created;

    @Column(nullable = false)
    private Timestamp expires;

    public ConfirmEmailData(AuthenticatedUser anAuthenticatedUser, long minutesUntilConfirmEmailTokenExpires) {
        authenticatedUser = anAuthenticatedUser;
        token = UUID.randomUUID().toString();
        long nowInMilliseconds = new Date().getTime();
        created = new Timestamp(nowInMilliseconds);
        long ONE_MINUTE_IN_MILLISECONDS = 60000;
        long futureInMilliseconds = nowInMilliseconds + (minutesUntilConfirmEmailTokenExpires * ONE_MINUTE_IN_MILLISECONDS);
        expires = new Timestamp(new Date(futureInMilliseconds).getTime());
    }

    public boolean isExpired() {
        if (this.expires == null) {
            return true;
        }
        long expiresInMilliseconds = this.expires.getTime();
        long nowInMilliseconds = new Date().getTime();
        return nowInMilliseconds > expiresInMilliseconds;
    }

    public String getToken() {
        return token;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public Timestamp getCreated() {
        return created;
    }

    public Timestamp getExpires() {
        return expires;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This is only here because it has to be: "The class should have a no-arg,
     * public or protected constructor." Please use the constructor that takes
     * arguments.
     */
    @Deprecated
    public ConfirmEmailData() {
    }

}
