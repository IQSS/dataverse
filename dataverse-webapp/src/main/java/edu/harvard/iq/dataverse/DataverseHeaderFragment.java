package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.user.UserNotificationRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class DataverseHeaderFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseHeaderFragment.class.getName());

    @EJB
    DataverseDao dataverseDao;

    @Inject
    SettingsServiceBean settingsService;

    @EJB
    SystemConfig systemConfig;

    @EJB
    DataFileServiceBean datafileService;

    @Inject
    DataverseSession dataverseSession;

    @Inject
    SettingsWrapper settingsWrapper;

    @Inject
    NavigationWrapper navigationWrapper;

    @EJB
    UserNotificationRepository userNotificationRepository;

    @Inject
    ConfirmEmailServiceBean confirmEmailService;

    @Inject
    private WidgetWrapper widgetWrapper;

    List<Breadcrumb> breadcrumbs = new ArrayList<>();

    private Long unreadNotificationCount;

    public List<Breadcrumb> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void initBreadcrumbs(DvObject dvObject) {
        if (dvObject == null) {
            return;
        }
        if (dvObject.getId() != null) {
            initBreadcrumbs(dvObject, null);
        } else {
            initBreadcrumbs(dvObject.getOwner(), dvObject instanceof Dataverse ? BundleUtil.getStringFromBundleWithLocale("newDataverse", dataverseSession.getLocale()) :
                    dvObject instanceof Dataset ? BundleUtil.getStringFromBundleWithLocale("newDataset", dataverseSession.getLocale()) : null);
        }
    }

    public void initBreadcrumbsForFileMetadata(FileMetadata fmd) {

        initBreadcrumbsForFileMetadata(fmd, null);
    }

    public void initBreadcrumbsForDataFile(DataFile datafile, String subPage) {
        Dataset dataset = datafile.getOwner();
        Long getDatasetVersionID = dataset.getLatestVersion().getId();
        FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, datafile.getId());

        initBreadcrumbsForFileMetadata(fmd, subPage);
    }

    public Boolean shouldShowUnconfirmedMailInfoBanner() {
        return confirmEmailService.hasEffectivelyUnconfirmedMail(dataverseSession.getUser());
    }

    public void initBreadcrumbsForFileMetadata(FileMetadata fmd, String subPage) {
        if (fmd == null) {
            return;
        }

        breadcrumbs.clear();

        String optionalUrlExtension = "&version=" + fmd.getDatasetVersion().getSemanticVersion();
        //First Add regular breadcrumb for the data file
        DataFile datafile = fmd.getDataFile();
        breadcrumbs.add(0, buildBreadcrumbForDatafile(datafile, optionalUrlExtension));

        //Get the Dataset Owning the Datafile and add version to the breadcrumb
        Dataset dataset = datafile.getOwner();

        breadcrumbs.add(0, buildBreadcrumbForDataset(dataset, optionalUrlExtension));

        // now get Dataverse Owner of the dataset and proceed as usual
        Dataverse dataverse = dataset.getOwner();
        while (dataverse != null) {
            breadcrumbs.add(0, buildBreadcrumbForDataverse(dataverse));
            dataverse = dataverse.getOwner();
        }

        if (subPage != null) {
            breadcrumbs.add(new Breadcrumb(subPage));
        }

    }

    public Long getUnreadNotificationCount() {

        if (unreadNotificationCount != null) {
            return unreadNotificationCount;
        }

        User user = dataverseSession.getUser();
        if (user.isAuthenticated()) {
            AuthenticatedUser aUser = (AuthenticatedUser) user;
            unreadNotificationCount = userNotificationRepository.getUnreadNotificationCountByUser(aUser.getId());
        } else {
            unreadNotificationCount = 0L;
        }
        return this.unreadNotificationCount;
    }

    public void initBreadcrumbs(DvObject dvObject, String subPage) {
        breadcrumbs.clear();
        while (dvObject != null) {
            breadcrumbs.add(0, buildBreadcrumbForDvObject(dvObject));
            dvObject = dvObject.getOwner();
        }

        if (subPage != null) {
            breadcrumbs.add(new Breadcrumb(subPage));
        }
    }

    private Breadcrumb buildBreadcrumbForDvObject(DvObject dvObject) {
        if (dvObject.isInstanceofDataverse()) {
            return buildBreadcrumbForDataverse((Dataverse) dvObject);
        } else if (dvObject.isInstanceofDataset()) {
            return buildBreadcrumbForDataset((Dataset) dvObject, null);
        } else if (dvObject.isInstanceofDataFile()) {
            return buildBreadcrumbForDatafile((DataFile) dvObject, null);
        }
        throw new IllegalArgumentException("Unknown dvObject type: " + dvObject.getClass().getName());
    }

    private Breadcrumb buildBreadcrumbForDataverse(Dataverse dataverse) {
        String dataverseUrl = "/dataverse/" + dataverse.getAlias();
        if (widgetWrapper.isWidgetTarget(dataverse)) {
            dataverseUrl = widgetWrapper.wrapURL(dataverseUrl);
        }
        boolean openInNewTab = widgetWrapper.isWidgetView() && !widgetWrapper.isWidgetTarget(dataverse);

        return new Breadcrumb(dataverseUrl, dataverse.getDisplayName(), openInNewTab);
    }
    private Breadcrumb buildBreadcrumbForDataset(Dataset dataset, String optionalUrlExtension) {
        String dataverseUrl = "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + (optionalUrlExtension == null ? "" : optionalUrlExtension);
        if (widgetWrapper.isWidgetTarget(dataset)) {
            dataverseUrl = widgetWrapper.wrapURL(dataverseUrl);
        }
        boolean openInNewTab = widgetWrapper.isWidgetView() && !widgetWrapper.isWidgetTarget(dataset);

        return new Breadcrumb(dataverseUrl, dataset.getDisplayName(), openInNewTab);
    }
    private Breadcrumb buildBreadcrumbForDatafile(DataFile datafile, String optionalUrlExtension) {
        String dataverseUrl = "/file.xhtml?fileId=" + datafile.getId() + (optionalUrlExtension == null ? "" : optionalUrlExtension);
        if (widgetWrapper.isWidgetTarget(datafile)) {
            dataverseUrl = widgetWrapper.wrapURL(dataverseUrl);
        }
        boolean openInNewTab = widgetWrapper.isWidgetView() && !widgetWrapper.isWidgetTarget(datafile);

        return new Breadcrumb(dataverseUrl, datafile.getDisplayName(), openInNewTab);
    }

    public String logout() {
        dataverseSession.setUser(null);
        dataverseSession.setStatusDismissed(false);

        String redirectPage = navigationWrapper.getPageFromContext();
        try {
            redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoginPage.class.getName()).log(Level.SEVERE, null, ex);
            redirectPage = redirectToRoot();
        }

        if (StringUtils.isEmpty(redirectPage)) {
            redirectPage = redirectToRoot();
        }

        logger.log(Level.INFO, "Sending user to = " + redirectPage);
        return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";
    }

    private String redirectToRoot() {
        return "dataverse.xhtml?alias=" + dataverseDao.findRootDataverse().getAlias();
    }

    public boolean isSignupAllowed() {
        return systemConfig.isSignupAllowed();
    }

    public boolean isRootDataverseThemeDisabled(Dataverse dataverse) {
        if (dataverse == null) {
            return false;
        }
        if (dataverse.getOwner() == null) {
            // We're operating on the root dataverse.
            return settingsService.isTrueForKey(Key.DisableRootDataverseTheme);
        } else {
            return false;
        }
    }

    public String getSignupUrl(String loginRedirect) {
        String signUpUrl = settingsService.getValueForKey(Key.SignUpUrl);
        return signUpUrl + (!signUpUrl.contains("?") ? loginRedirect : loginRedirect.replace("?", "&"));
    }

    public String getLoginRedirectPage() {
        System.out.println("DEPRECATED call to getLoginRedirectPage method in DataverseHeaderfragment: " + navigationWrapper.getRedirectPage());
        return navigationWrapper.getRedirectPage();
    }

    public void addBreadcrumb(String url, String linkString) {
        breadcrumbs.add(new Breadcrumb(url, linkString));
    }

    public void addBreadcrumb(String text) {
        breadcrumbs.add(new Breadcrumb(text));
    }

    // inner class used for breadcrumbs
    public static class Breadcrumb implements java.io.Serializable {

        private final String breadcrumbText;
        private final String url;
        private final boolean openUrlInNewTab;

        public Breadcrumb(String url, String breadcrumbText, boolean openUrlInNewTab) {
            this.url = url;
            this.breadcrumbText = breadcrumbText;
            this.openUrlInNewTab = openUrlInNewTab;
        }

        public Breadcrumb(String url, String breadcrumbText) {
            this(url, breadcrumbText, false);
        }

        public Breadcrumb(String breadcrumbText) {
            this(null, breadcrumbText, false);
        }

        public String getBreadcrumbText() {
            return breadcrumbText;
        }

        public String getUrl() {
            return url;
        }

        public boolean isOpenUrlInNewTab() {
            return openUrlInNewTab;
        }
    }
}