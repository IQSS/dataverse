package edu.harvard.iq.dataverse.bannersandmessages.banners;

import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseBannerDto;
import edu.harvard.iq.dataverse.bannersandmessages.banners.dto.DataverseLocalizedBannerDto;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.Components;
import org.primefaces.component.datalist.DataList;
import org.primefaces.component.tabview.Tab;
import org.primefaces.component.tabview.TabView;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.ejb.EJB;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.Serializable;

@ViewScoped
@Named("BannerTab")
public class BannerTab implements Serializable {

    private long dataverseId;
    private DataverseBannerDto bannerToDelete;

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
    
    public void deleteBanner() {
        bannerDAO.delete(bannerToDelete.getId());
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataversemessages.banners.delete.success"));

        Long allBannersCount = bannerDAO.countBannersForDataverse(dataverseId);

        TabView tabView = Components.findComponentsInChildren(Components.getCurrentForm(), TabView.class).get(0);
        Tab bannersTab = Components.findComponentsInChildren(tabView, Tab.class).get(1);

        DataList dataListComponent = Components.findComponentsInChildren(bannersTab, DataList.class).get(0);
        if (dataListComponent.getFirst() >= allBannersCount && dataListComponent.getFirst() >= dataListComponent.getRows()) {
            dataListComponent.setFirst(dataListComponent.getFirst() - dataListComponent.getRows());
        }
        lazyBannerHistory.setRowCount(allBannersCount.intValue());

    }
    
    public void deactivateBanner(long textMessageId) {
        bannerDAO.deactivate(textMessageId);
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataversemessages.banners.deactivate.success"));
    }

    public StreamedContent getDisplayImage(DataverseLocalizedBannerDto localizedBanner) {
        return DefaultStreamedContent.builder()
                .contentType(localizedBanner.getContentType())
                .stream(() -> new ByteArrayInputStream(localizedBanner.getContent()))
                .build();
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

    public DataverseBannerDto getBannerToDelete() {
        return bannerToDelete;
    }

    public void setBannerToDelete(DataverseBannerDto bannerToDelete) {
        this.bannerToDelete = bannerToDelete;
    }
}
