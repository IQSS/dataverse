package edu.harvard.iq.dataverse.dataverse;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseLinkingDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFeaturedDataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.search.SavedSearchService;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import javax.faces.view.ViewScoped;
import org.primefaces.model.DualListModel;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;


/**
 * @author gdurand
 */
@ViewScoped
@Named("DataversePage")
public class DataversePage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataversePage.class.getCanonicalName());

    public enum LinkMode {
        SAVEDSEARCH, LINKDATAVERSE
    }

    @EJB
    DataverseDao dataverseDao;
    @Inject
    DataverseSession session;
    @EJB
    FeaturedDataverseServiceBean featuredDataverseService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    SearchIncludeFragment searchIncludeFragment;
    @EJB
    DataverseLinkingDao linkingService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    private DataverseService dataverseService;
    @Inject
    private SavedSearchService savedSearchService;

    private Dataverse dataverse = new Dataverse();
    private LinkMode linkMode;

    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private List<Dataverse> dataversesForLinking;
    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse linkingDataverse;
    private List<Dataverse> carouselFeaturedDataverses = null;

    // -------------------- GETTERS --------------------

    public Dataverse getLinkingDataverse() {
        return linkingDataverse;
    }

    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }

    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }

    public List<Dataverse> getDataversesForLinking() {
        return dataversesForLinking;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public LinkMode getLinkMode() {
        return linkMode;
    }

    public boolean isRootDataverse() {
        return dataverse.getOwner() == null;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return featuredDataverses;
    }

    // -------------------- LOGIC --------------------
    public String init() {

        if (dataverse.getAlias() != null) {
            dataverse = dataverseDao.findByAlias(dataverse.getAlias());
        } else if (dataverse.getId() != null) {
            dataverse = dataverseDao.find(dataverse.getId());
        } else {
            try {
                dataverse = dataverseDao.findRootDataverse();
            } catch (EJBException e) {
                // @todo handle case with no root dataverse (a fresh installation) with message about using API to create the root
                dataverse = null;
            }
        }

        // check if dv exists and user has permission
        if (dataverse == null) {
            return permissionsWrapper.notFound();
        }
        if (!dataverse.isReleased() && !permissionService.on(dataverse).has(Permission.ViewUnpublishedDataverse)) {
            return permissionsWrapper.notAuthorized();
        }
        initFeaturedDataverses();
        carouselFeaturedDataverses = featuredDataverseService.findByDataverseIdQuick(dataverse.getId());

        return null;
    }

    public List<Dataverse> getCarouselFeaturedDataverses() {
        return carouselFeaturedDataverses;
    }

    public String saveFeaturedDataverse() {

        Try<Dataverse> saveFeaturedDataverseOperation = Try.of(() -> dataverseService.saveFeaturedDataverse(dataverse, featuredDataverses.getTarget()))
                .onSuccess(savedDataverse -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.feature.update")))
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
                    JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataverse.update.failure"));
                });

        if (saveFeaturedDataverseOperation.isFailure()) {
            return StringUtils.EMPTY;
        }

        return returnRedirect();
    }

    public String saveLinkedDataverse() {

        if (linkingDataverseId == null) {
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.link.user");
            logger.severe(msg);
            JsfHelper.addFlashErrorMessage(msg);
            return returnRedirect();
        }

        Try.of(() -> dataverseService.saveLinkedDataverse(dataverseDao.find(linkingDataverseId), dataverse))
                .onFailure(ex -> handleSaveLinkedDataverseExceptions(ex, linkingDataverseId))
                .onSuccess(savedLinkingDv -> {
                    linkingDataverse = savedLinkingDv.getLinkingDataverse();

                    JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.linked.success.wait", getSuccessMessageArguments()));
                });

        return returnRedirect();
    }

    public void setupLinkingPopup(String popupSetting) {
        if (popupSetting.equals("link")) {
            setLinkMode(LinkMode.LINKDATAVERSE);
        } else {
            setLinkMode(LinkMode.SAVEDSEARCH);
        }
        updateLinkableDataverses();
    }

    public String saveSavedSearch() {
        if (linkingDataverseId == null) {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }
        linkingDataverse = dataverseDao.find(linkingDataverseId);

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.search.user");
            logger.severe(msg);
            JsfHelper.addFlashErrorMessage(msg);
            return returnRedirect();
        }


        Try.of(() -> savedSearchService.saveSavedDataverseSearch(searchIncludeFragment.getQuery(), searchIncludeFragment.getFilterQueriesDebug(), dataverse))
                .onSuccess(savedSearch -> {
                    String hrefArgument = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml(linkingDataverse.getDisplayName()) + "</a>";
                    JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.saved.search.success", Collections.singleton(hrefArgument)));
                })
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "There was a problem linking this search", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.saved.search.failure") + " " + ex);
                });


        return returnRedirect();
    }

    public String releaseDataverse() {
        if (!session.getUser().isAuthenticated()) {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.not.authorized"));
        }

        Try.of(() -> dataverseService.publishDataverse(dataverse))
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.failure"));
                })
                .onSuccess(dv -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success")));

        return returnRedirect();
    }

    public String deleteDataverse() {

        Try.run(() -> dataverseService.deleteDataverse(dataverse))
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "Unexpected Exception calling  delete dataverse command", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.delete.failure"));
                })
                .onSuccess(dv -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.delete.success")));

        return "/dataverse.xhtml?alias=" + dataverse.getOwner().getAlias() + "&faces-redirect=true";
    }

    public Boolean isEmptyDataverse() {
        return !dataverseDao.hasData(dataverse);
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return session.getUser().isSuperuser();
    }

    public boolean isUserAdminForCurrentDataverse() {
        return permissionService.isUserAdminForDataverse(session.getUser(), this.dataverse);
    }

    // -------------------- PRIVATE --------------------

    private void handleSaveLinkedDataverseExceptions(Throwable ex, long dataverseToLinkId) {
        String msg = BundleUtil.getStringFromBundle("dataverse.link.error", Lists.newArrayList(dataverse.getDisplayName()));
        JsfHelper.addFlashErrorMessage(msg);

        logger.log(Level.SEVERE, "Unable to link dataverse with id: " + dataverse.getId() + " to " + dataverseToLinkId, ex);
    }


    private List<String> getSuccessMessageArguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(StringEscapeUtils.escapeHtml(dataverse.getDisplayName()));
        String linkString = "<a href=\"/dataverse/" + linkingDataverse.getAlias() + "\">" + StringEscapeUtils.escapeHtml(linkingDataverse.getDisplayName()) + "</a>";
        arguments.add(linkString);
        return arguments;
    }

    private AuthenticatedUser getAuthenticatedUser() {
        User user = session.getUser();
        if (user.isAuthenticated()) {
            return (AuthenticatedUser) user;
        } else {
            return null;
        }
    }

    private void initFeaturedDataverses() {
        List<Dataverse> featuredSource = new ArrayList<>();
        List<Dataverse> featuredTarget = new ArrayList<>();
        featuredSource.addAll(dataverseDao.findAllPublishedByOwnerId(dataverse.getId()));
        featuredSource.addAll(linkingService.findLinkingDataverses(dataverse.getId()));
        List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());
        for (DataverseFeaturedDataverse dfd : featuredList) {
            Dataverse fd = dfd.getFeaturedDataverse();
            featuredTarget.add(fd);
            featuredSource.remove(fd);
        }
        featuredDataverses = new DualListModel<>(featuredSource, featuredTarget);

    }

    private void updateLinkableDataverses() {
        dataversesForLinking = new ArrayList<>();
        linkingDVSelectItems = new ArrayList<>();

        //Since only a super user function add all dvs
        dataversesForLinking = dataverseDao.findAll();// permissionService.getDataversesUserHasPermissionOn(session.getUser(), Permission.PublishDataverse);


        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {

            // remove this and it's parent tree
            dataversesForLinking.remove(dataverse);
            Dataverse testDV = dataverse;
            while (testDV.getOwner() != null) {
                dataversesForLinking.remove(testDV.getOwner());
                testDV = testDV.getOwner();
            }

            for (Dataverse removeLinked : linkingService.findLinkingDataverses(dataverse.getId())) {
                dataversesForLinking.remove(removeLinked);
            }
        }


        for (Dataverse selectDV : dataversesForLinking) {
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(), selectDV.getDisplayName()));
        }

        if (dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
            linkingDataverse = dataversesForLinking.get(0);
            linkingDataverseId = linkingDataverse.getId();
        }
    }

    private String returnRedirect() {
        return "/dataverse.xhtml?alias=" + dataverse.getAlias() + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setLinkingDataverse(Dataverse linkingDataverse) {
        this.linkingDataverse = linkingDataverse;
    }

    public void setLinkingDVSelectItems(List<SelectItem> linkingDVSelectItems) {
        this.linkingDVSelectItems = linkingDVSelectItems;
    }

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }

    public void setDataversesForLinking(List<Dataverse> dataversesForLinking) {

        this.dataversesForLinking = dataversesForLinking;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setLinkMode(LinkMode linkMode) {
        this.linkMode = linkMode;
    }

    public void setFeaturedDataverses(DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }
}
