package edu.harvard.iq.dataverse.authorization.users;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.validation.constraints.NotNull;

/**
 * @todo Should we rename this from ApiKey to ApiToken? See
 * https://github.com/IQSS/dataverse/issues/459#issuecomment-51400205
 */
@Entity
public class ApiKey implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String key;

    @NotNull
    @JoinColumn(nullable = false)
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

}
