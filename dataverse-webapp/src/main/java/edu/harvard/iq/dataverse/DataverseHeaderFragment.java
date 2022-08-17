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
import edu.harvard.iq.dataverse.users.SamlSessionRegistry;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class DataverseHeaderFragment implements Serializable {

    private static final Logger logger = Logger.getLogger(DataverseHeaderFragment.class.getName());

    private DataverseDao dataverseDao;
    private SettingsServiceBean settingsService;
    private SystemConfig systemConfig;
    private DataFileServiceBean datafileService;
    private DataverseSession dataverseSession;
    private NavigationWrapper navigationWrapper;
    private UserNotificationRepository userNotificationRepository;
    private ConfirmEmailServiceBean confirmEmailService;
    private WidgetWrapper widgetWrapper;
    private SamlSessionRegistry samlSessionRegistry;

    private List<Breadcrumb> breadcrumbs = new ArrayList<>();

    private Long unreadNotificationCount;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DataverseHeaderFragment() { }

    @Inject
    public DataverseHeaderFragment(DataverseDao dataverseDao, SettingsServiceBean settingsService,
                                   SystemConfig systemConfig, DataFileServiceBean datafileService,
                                   DataverseSession dataverseSession, NavigationWrapper navigationWrapper,
                                   UserNotificationRepository userNotificationRepository, ConfirmEmailServiceBean confirmEmailService,
                                   WidgetWrapper widgetWrapper, SamlSessionRegistry samlSessionRegistry) {
        this.dataverseDao = dataverseDao;
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;
        this.datafileService = datafileService;
        this.dataverseSession = dataverseSession;
        this.navigationWrapper = navigationWrapper;
        this.userNotificationRepository = userNotificationRepository;
        this.confirmEmailService = confirmEmailService;
        this.widgetWrapper = widgetWrapper;
        this.samlSessionRegistry = samlSessionRegistry;
    }

    // -------------------- GETTERS --------------------

    public List<Breadcrumb> getBreadcrumbs() {
        return breadcrumbs;
    }

    // -------------------- LOGIC --------------------

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

    public String logout() {
        samlSessionRegistry.unregister(dataverseSession);
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

    // -------------------- PRIVATE --------------------

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

    private <T extends DvObject> Breadcrumb buildBreadcrumb(T dvObject, String optionalUrlExtension,
                                                            Function<T, String> urlCreator) {
        String url = urlCreator.apply(dvObject) + (optionalUrlExtension == null ? "" : optionalUrlExtension);
        if (widgetWrapper.isWidgetTarget(dvObject)) {
            url = widgetWrapper.wrapURL(url);
        }
        boolean openInNewTab = widgetWrapper.isWidgetView() && !widgetWrapper.isWidgetTarget(dvObject);
        return new Breadcrumb(url, dvObject.getDisplayName(), openInNewTab);
    }

    private Breadcrumb buildBreadcrumbForDataverse(Dataverse dataverse) {
        return buildBreadcrumb(dataverse, null, d -> "/dataverse/" + d.getAlias());
    }

    private Breadcrumb buildBreadcrumbForDataset(Dataset dataset, String optionalUrlExtension) {
        return buildBreadcrumb(dataset, optionalUrlExtension, d -> "/dataset.xhtml?persistentId=" + d.getGlobalIdString());
    }

    private Breadcrumb buildBreadcrumbForDatafile(DataFile datafile, String optionalUrlExtension) {
        return buildBreadcrumb(datafile, optionalUrlExtension, d -> "/file.xhtml?fileId=" + d.getId());
    }

    private String redirectToRoot() {
        return "dataverse.xhtml?alias=" + dataverseDao.findRootDataverse().getAlias();
    }


    // -------------------- INNER CLASSES --------------------

    public static class Breadcrumb implements Serializable {

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