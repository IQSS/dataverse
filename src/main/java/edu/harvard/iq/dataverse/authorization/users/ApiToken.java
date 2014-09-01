package edu.harvard.iq.dataverse.authorization.users;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.validation.constraints.NotNull;

@Entity
@NamedQueries({
    @NamedQuery( name="ApiToken.findByToken", query="SELECT t FROM ApiToken t WHERE t.token=:token" )
})
public class ApiToken implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String token;

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

    public String getToken() {
        return token;
    }

    public void setToken(String aToken) {
        this.token = aToken;
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
    
}
