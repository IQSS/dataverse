package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearchService;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.JsfRedirectHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("LinkToDataverseDialog")
public class LinkToDataverseDialog implements java.io.Serializable {

    private static final Logger logger = LoggerFactory.getLogger(LinkToDataverseDialog.class);
    
    public enum LinkMode {
        SAVEDSEARCH, LINKDATAVERSE
    }

    @EJB
    private DataverseRepository dataverseRepository;
    @Inject
    private SavedSearchService savedSearchService;
    @EJB
    private DataverseLinkingService linkingService;
    @Inject
    private DataverseSession session;
    
    private boolean canLinkDataverse;
    private boolean canLinkSavedSearch;
    
    private LinkMode linkMode;
    
    private Dataverse dataverse;
    private String searchQuery;
    private List<String> searchFilterQueriesDebug;

    private Long linkingDataverseId;
    private List<SelectItem> linkingDVSelectItems;
    private Dataverse targetDataverseLink;


    // -------------------- GETTERS --------------------

    public boolean isCanLinkDataverse() {
        return canLinkDataverse;
    }
    public boolean isCanLinkSavedSearch() {
        return canLinkSavedSearch;
    }
    public LinkMode getLinkMode() {
        return linkMode;
    }
    public String getSearchQuery() {
        return searchQuery;
    }
    public List<String> getSearchFilterQueriesDebug() {
        return searchFilterQueriesDebug;
    }
    public List<SelectItem> getLinkingDVSelectItems() {
        return linkingDVSelectItems;
    }
    public Long getLinkingDataverseId() {
        return linkingDataverseId;
    }
    
    // -------------------- LOGIC --------------------

    public void init(Dataverse dataverse, String searchQuery, List<String> searchFilterQueriesDebug) {
        canLinkDataverse = session.getUser().isSuperuser() && !dataverse.isRoot();
        canLinkSavedSearch = session.getUser().isSuperuser() && StringUtils.isNotEmpty(searchQuery);

        if (canLinkDataverse || canLinkSavedSearch) {
            this.dataverse = dataverse;
            this.searchQuery = searchQuery;
            this.searchFilterQueriesDebug = searchFilterQueriesDebug;
        }
    }

    public void setupDialogForDataverseLinking() {
        linkMode = LinkMode.LINKDATAVERSE;
        updateLinkableDataverses();
    }
    public void setupDialogForSavedSearchLinking() {
        linkMode = LinkMode.SAVEDSEARCH;
        updateLinkableDataverses();
    }
    
    public String saveLinkedDataverse() {

        if (linkingDataverseId == null) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.link.user");
            logger.error(msg);
            JsfHelper.addFlashErrorMessage(msg);
            return JsfRedirectHelper.redirectToDataverse(dataverse.getAlias());
        }

        Try.of(() -> linkingService.saveLinkedDataverse(dataverseRepository.getById(linkingDataverseId), dataverse))
                .onFailure(ex -> handleSaveLinkedDataverseExceptions(ex, linkingDataverseId))
                .onSuccess(savedLinkingDv -> {
                    Dataverse savedTargetDataverseLink = savedLinkingDv.getLinkingDataverse();

                    JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.linked.success.wait", getSuccessMessageArguments(savedTargetDataverseLink)));
                });

        return JsfRedirectHelper.redirectToDataverse(dataverse.getAlias());
    }
    
    public String saveSavedSearch() {
        if (linkingDataverseId == null) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataverse.link.select"));
            return "";
        }
        targetDataverseLink = dataverseRepository.getById(linkingDataverseId);

        AuthenticatedUser savedSearchCreator = getAuthenticatedUser();
        if (savedSearchCreator == null) {
            String msg = BundleUtil.getStringFromBundle("dataverse.search.user");
            logger.error(msg);
            JsfHelper.addFlashErrorMessage(msg);
            return JsfRedirectHelper.redirectToDataverse(dataverse.getAlias());
        }


        Try.of(() -> savedSearchService.saveSavedDataverseSearch(searchQuery, searchFilterQueriesDebug, targetDataverseLink))
                .onSuccess(savedSearch -> {
                    String hrefArgument = "<a href=\"/dataverse/" + targetDataverseLink.getAlias() + "\">" + StringEscapeUtils.escapeHtml(targetDataverseLink
                                                                                                                                                  .getDisplayName()) + "</a>";
                    JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.saved.search.success", hrefArgument));
                })
                .onFailure(ex -> {
                    logger.error("There was a problem linking this search", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.saved.search.failure") + " " + ex);
                });


        return JsfRedirectHelper.redirectToDataverse(dataverse.getAlias());
    }

    // -------------------- PRIVATE --------------------

    private void updateLinkableDataverses() {
        linkingDVSelectItems = new ArrayList<>();

        //Since only a super user function add all dvs
        List<Dataverse> dataversesForLinking = dataverseRepository.findAll();// permissionService.getDataversesUserHasPermissionOn(session.getUser(), Permission.PublishDataverse);


        //for linking - make sure the link hasn't occurred and its not int the tree
        if (this.linkMode.equals(LinkMode.LINKDATAVERSE)) {

            // remove this and it's parent tree
            Dataverse testDV = dataverse;
            while (testDV != null) {
                dataversesForLinking.remove(testDV);
                testDV = testDV.getOwner();
            }

            for (Dataverse removeLinked : linkingService.findLinkingDataverses(dataverse.getId())) {
                dataversesForLinking.remove(removeLinked);
            }
        }


        for (Dataverse selectDV : dataversesForLinking) {
            linkingDVSelectItems.add(new SelectItem(selectDV.getId(),
                    selectDV.getDisplayName() + " (" + BundleUtil.getStringFromBundle("dataverse.alias")
                            + ": " + selectDV.getAlias() + ")"));
        }

        if (dataversesForLinking.size() == 1 && dataversesForLinking.get(0) != null) {
            targetDataverseLink = dataversesForLinking.get(0);
            linkingDataverseId = targetDataverseLink.getId();
        }
    }
    

    private void handleSaveLinkedDataverseExceptions(Throwable ex, long dataverseToLinkId) {
        String msg = BundleUtil.getStringFromBundle("dataverse.link.error", dataverse.getDisplayName());
        JsfHelper.addFlashErrorMessage(msg);

        logger.error("Unable to link dataverse with id: " + dataverse.getId() + " to " + dataverseToLinkId, ex);
    }


    private Object[] getSuccessMessageArguments(Dataverse savedTargetDataverseLink) {
        List<String> arguments = new ArrayList<>();
        arguments.add(StringEscapeUtils.escapeHtml(dataverse.getDisplayName()));
        String linkString = "<a href=\"/dataverse/" + savedTargetDataverseLink.getAlias() + "\">" + StringEscapeUtils.escapeHtml(savedTargetDataverseLink
                                                                                                                                    .getDisplayName()) + "</a>";
        arguments.add(linkString);
        return arguments.toArray();
    }

    private AuthenticatedUser getAuthenticatedUser() {
        User user = session.getUser();
        if (user.isAuthenticated()) {
            return (AuthenticatedUser) user;
        } else {
            return null;
        }
    }

    // -------------------- SETTERS --------------------

    public void setLinkingDataverseId(Long linkingDataverseId) {
        this.linkingDataverseId = linkingDataverseId;
    }
    
}
