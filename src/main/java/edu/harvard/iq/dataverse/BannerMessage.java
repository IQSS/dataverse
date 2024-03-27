
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.Serializable;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;


/**
 *
 * @author skraffmi
 */
@Entity
public class BannerMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private boolean dismissibleByUser;
    
    @Column
    private boolean active;

    @OneToMany(mappedBy = "bannerMessage", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<BannerMessageText> bannerMessageTexts;
    
    @OneToMany(mappedBy = "bannerMessage", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<UserBannerMessage> userBannerMessages;

    public Collection<BannerMessageText> getBannerMessageTexts() {
        return this.bannerMessageTexts;
    }

    public void setBannerMessageTexts(Collection<BannerMessageText> bannerMessageTexts) {
        this.bannerMessageTexts = bannerMessageTexts;
    }
    
    
    public String getDisplayValue(){        
        String retVal = "";
        for (BannerMessageText msgTxt : this.getBannerMessageTexts()) {
            if (msgTxt.getLang().equals(BundleUtil.getCurrentLocale().getLanguage())) {
                retVal = msgTxt.getMessage();
            }
        }
        return retVal;               
    }

    public boolean isDismissibleByUser() {
        return dismissibleByUser;
    }

    public void setDismissibleByUser(boolean dismissibleByUser) {
        this.dismissibleByUser = dismissibleByUser;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
        if (!(object instanceof BannerMessage)) {
            return false;
        }
        BannerMessage other = (BannerMessage) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.BannerMessage[ id=" + id + " ]";
    }
    
}
