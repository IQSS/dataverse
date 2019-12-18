package edu.harvard.iq.dataverse.bannersandmessages.banners;

import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Named;

import java.io.Serializable;

@ViewScoped
@Named("BannerTab")
public class BannerTab implements Serializable {

    private long dataverseId;

    @EJB
    private LazyBannerHistory lazyBannerHistory;

    @EJB
    private BannerDAO bannerDAO;

    public String init() {
        lazyBannerHistory.setDataverseId(dataverseId);

        return StringUtils.EMPTY;
    }

    public String newBannerPage() {
        return "/dataverse-newBannerPage.xhtml?dataverseId=" + dataverseId + "&faces-redirect=true";
    }

    public String reuseBanner(String bannerId) {
        return "/dataverse-newBannerPage.xhtml?dataverseId=" + dataverseId +
                "&bannerTemplateId=" + bannerId + "&faces-redirect=true";
    }

    public long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public LazyBannerHistory getLazyBannerHistory() {
        return lazyBannerHistory;
    }

    public void setLazyBannerHistory(LazyBannerHistory lazyBannerHistory) {
        this.lazyBannerHistory = lazyBannerHistory;
    }

    public BannerDAO getBannerDAO() {
        return bannerDAO;
    }
}
