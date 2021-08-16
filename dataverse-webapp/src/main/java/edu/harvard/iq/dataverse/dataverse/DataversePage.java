package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchIncludeFragment;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.JsfRedirectHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;


/**
 * @author gdurand
 */
@RequestScoped
@Named("DataversePage")
public class DataversePage {

    private static final Logger logger = LoggerFactory.getLogger(DataversePage.class);

    @Inject
    private DataverseDao dataverseDao;
    @Inject
    private DataverseService dataverseService;
    @Inject
    private FeaturedDataverseServiceBean featuredDataverseService;
    @Inject
    private PermissionServiceBean permissionService;
    @Inject
    private PermissionsWrapper permissionsWrapper;
    @Inject
    private DataverseSession session;
    @Inject
    private SearchIncludeFragment searchIncludeFragment;
    @Inject
    private LinkToDataverseDialog linkToDataverseDialog;
    @Inject
    private FeaturedDataversesDialog featuredDataversesDialog;

    @Inject @Param(name = "alias")
    private String dataverseAlias;
    @Inject @Param(name = "id")
    private Long dataverseId;

    private Dataverse dataverse;

    private boolean showDescriptionAndCarousel;
    private List<Dataverse> carouselFeaturedDataverses = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public Dataverse getDataverse() {
        return dataverse;
    }

    public String getDataverseAlias() {
        return dataverseAlias;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public boolean isShowDescriptionAndCarousel() {
        return showDescriptionAndCarousel;
    }

    public List<Dataverse> getCarouselFeaturedDataverses() {
        return carouselFeaturedDataverses;
    }

    // -------------------- LOGIC --------------------
    @PostConstruct
    public void postConstruct() {

        if (StringUtils.isNotEmpty(dataverseAlias)) {
            dataverse = dataverseDao.findByAlias(dataverseAlias);
        } else if(dataverseId != null) {
            dataverse = dataverseDao.find(dataverseId);
        } else {
            dataverse = dataverseDao.findRootDataverse();
        }
        if (dataverse == null) {
            permissionsWrapper.notFound();
        }
    }

    public String init() {

        if (!dataverse.isReleased() && !permissionsWrapper.canViewUnpublishedDataverse(dataverse)) {
            return permissionsWrapper.notAuthorized();
        }

        searchIncludeFragment.search(dataverse);
        linkToDataverseDialog.init(dataverse, searchIncludeFragment.getQuery(), searchIncludeFragment.getFilterQueriesDebug());

        featuredDataversesDialog.init(dataverse);

        showDescriptionAndCarousel = searchIncludeFragment.getFilterQueries().isEmpty() && StringUtils.isEmpty(searchIncludeFragment.getQuery());
        if (showDescriptionAndCarousel) {
            carouselFeaturedDataverses = featuredDataverseService.findByDataverseIdQuick(dataverse.getId());
        }

        return null;
    }

    public String releaseDataverse() {
        if (!session.getUser().isAuthenticated()) {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.not.authorized"));
        }

        Try.of(() -> dataverseService.publishDataverse(dataverse))
                .onFailure(ex -> {
                    logger.error("Unexpected Exception calling  publish dataverse command", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.failure"));
                })
                .onSuccess(dv -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success")));

        return JsfRedirectHelper.redirectToDataverse(dataverse.getAlias());
    }

    public String deleteDataverse() {

        Try.run(() -> dataverseService.deleteDataverse(dataverse))
                .onFailure(ex -> {
                    logger.error("Unexpected Exception calling  delete dataverse command", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.delete.failure"));
                })
                .onSuccess(dv -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.delete.success")));

        return JsfRedirectHelper.redirectToDataverse(dataverse.getOwner().getAlias());
    }

    public Boolean isEmptyDataverse() {
        return !dataverseDao.hasData(dataverse);
    }

    public boolean isUserCanChangeAllowMessageAndBanners() {
        return dataverse.isAllowMessagesBanners() && (session.getUser().isSuperuser() || permissionService.isUserAbleToEditDataverse(session.getUser(), this.dataverse));
    }

    public String redirectToMetrics() {
        return "/metrics.xhtml?faces-redirect=true";
    }

}
