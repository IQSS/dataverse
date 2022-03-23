package edu.harvard.iq.dataverse.authorization.users;

import java.io.Serializable;
import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@NamedQueries({
    @NamedQuery( name="ApiToken.findByTokenString", query="SELECT t FROM ApiToken t WHERE t.tokenString = :tokenString" ),
    @NamedQuery( name="ApiToken.findByUser",        query="SELECT t FROM ApiToken t WHERE t.authenticatedUser = :user")
})
@Table(indexes = {@Index(columnList="authenticateduser_id")})
public class ApiToken implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String tokenString;

    @NotNull
    @JoinColumn(nullable = false)
    @ManyToOne
    private AuthenticatedUser authenticatedUser;

    @Column(nullable = false)
    boolean disabled;

    @Column(nullable = false)
    private Timestamp createTime;

    @Column(nullable = false)
    private Timestamp expireTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenString() {
        return tokenString;
    }

    public void setTokenString(String aToken) {
        this.tokenString = aToken;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }
    
    public boolean isDisabled() {
        return disabled;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public Timestamp getExpireTime() {
        return expireTime;
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public void setExpireTime(Timestamp expireTime) {
        this.expireTime = expireTime;
    }

    public boolean isExpired() {
        return getExpireTime().before(new Timestamp(System.currentTimeMillis()));
    }
    
}
