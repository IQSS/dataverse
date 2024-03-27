
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 *
 * @author skraffmi
 */
@Entity
public class UserBannerMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser user;
    
    @OneToOne
    @JoinColumn(nullable = false)
    private BannerMessage bannerMessage;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column( nullable=false )
    private Date bannerDismissalTime;

    public Long getId() {
        return id;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public BannerMessage getBannerMessage() {
        return bannerMessage;
    }

    public void setBannerMessage(BannerMessage bannerMessage) {
        this.bannerMessage = bannerMessage;
    }

    public Date getBannerDismissalTime() {
        return bannerDismissalTime;
    }

    public void setBannerDismissalTime(Date bannerDismissalTime) {
        this.bannerDismissalTime = bannerDismissalTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserBannerMessage)) {
            return false;
        }
        UserBannerMessage other = (UserBannerMessage) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.UserBannerMessage[ id=" + id + " ]";
    }
    
}
