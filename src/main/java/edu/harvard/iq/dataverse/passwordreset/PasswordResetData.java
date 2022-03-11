package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(indexes = {@Index(columnList="token")
		, @Index(columnList="builtinuser_id")})
@NamedQueries({
    @NamedQuery(name="PasswordResetData.findAll",
            query="SELECT prd FROM PasswordResetData prd"),
    @NamedQuery(name="PasswordResetData.findByUser",
            query="SELECT prd FROM PasswordResetData prd WHERE prd.builtinUser = :user"),
    @NamedQuery(name="PasswordResetData.findByToken",
            query="SELECT prd FROM PasswordResetData prd WHERE prd.token = :token"),
    @NamedQuery(name="PasswordResetData.deleteByUser",
            query="DELETE FROM PasswordResetData prd WHERE prd.builtinUser = :user"),
})
@Entity
public class PasswordResetData implements Serializable {
     
    public enum Reason {
        FORGOT_PASSWORD,
        NON_COMPLIANT_PASSWORD,
        UPGRADE_REQUIRED
    }
    
    // TODO cleaup: can remove the (unused) id field, and use the token field as an id instead.
    // This will prevent duplicate tokens (ok, not a likely poroblem) and would
    // make the token lookup much faster.
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String token;

    /**
     * @todo Is there an annotation to help enforce that a given DataverseUser
     * can only have one token at a time?
     */
    @OneToOne
    @JoinColumn(nullable = false)
    private BuiltinUser builtinUser;

    @Column(nullable = false)
    private Timestamp created;

    @Column(nullable = false)
    private Timestamp expires;
    
    @Enumerated(EnumType.STRING)
    private Reason reason;

    /**
     * This is only here because it has to be: "The class should have a no-arg,
     * public or protected constructor." Please use the constructor that takes
     * arguments.
     */
    @Deprecated
    public PasswordResetData() {
    }

    public PasswordResetData(BuiltinUser aBuiltinUser) {
        builtinUser = aBuiltinUser;
        token = UUID.randomUUID().toString();
        long nowInMilliseconds = new Date().getTime();
        created = new Timestamp(nowInMilliseconds);
        long ONE_MINUTE_IN_MILLISECONDS = 60000;
        long futureInMilliseconds = nowInMilliseconds + (SystemConfig.getMinutesUntilPasswordResetTokenExpires() * ONE_MINUTE_IN_MILLISECONDS);
        expires = new Timestamp(new Date(futureInMilliseconds).getTime());
        reason = Reason.FORGOT_PASSWORD;
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

    public BuiltinUser getBuiltinUser() {
        return builtinUser;
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

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

}
