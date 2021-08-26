package edu.harvard.iq.dataverse.dataverse;

import edu.harvard.iq.dataverse.FeaturedDataverseServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.JsfRedirectHelper;
import io.vavr.control.Try;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.DualListModel;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("FeaturedDataversesDialog")
public class FeaturedDataversesDialog implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(FeaturedDataversesDialog.class.getCanonicalName());

    @Inject
    private DataverseService dataverseService;
    @Inject
    private FeaturedDataverseServiceBean featuredDataverseService;
    @Inject
    private PermissionsWrapper permissionWrapper;
    
    private boolean canEditFeaturedDataverses;
    private DualListModel<Dataverse> featuredDataverses = new DualListModel<>(new ArrayList<>(), new ArrayList<>());
    private Dataverse dataverse;

    // -------------------- GETTERS --------------------

    public boolean isCanEditFeaturedDataverses() {
        return canEditFeaturedDataverses;
    }

    public DualListModel<Dataverse> getFeaturedDataverses() {
        return featuredDataverses;
    }

    // -------------------- LOGIC --------------------

    public void init(Dataverse dataverse) {
        canEditFeaturedDataverses = permissionWrapper.canIssueUpdateDataverseCommand(dataverse);
        
        if (canEditFeaturedDataverses) {
            this.dataverse = dataverse;
        }
    }

    public void setupDialog() {
        List<Dataverse> featuredSource = featuredDataverseService.findFeaturableDataverses(dataverse.getId());
        List<Dataverse> featuredTarget = featuredDataverseService.findByDataverseId(dataverse.getId());

        featuredTarget.forEach(featuredDataverse -> featuredSource.remove(featuredDataverse));

        featuredDataverses = new DualListModel<>(featuredSource, featuredTarget);
    }

    public String saveFeaturedDataverse() {

        Try.of(() -> dataverseService.saveFeaturedDataverse(dataverse, featuredDataverses.getTarget()))
                .onSuccess(savedDataverse -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.feature.update")))
                .onFailure(ex -> {
                    logger.log(Level.SEVERE, "Unexpected Exception calling dataverse command", ex);
                    JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.update.failure"), "");
                });

        return JsfRedirectHelper.redirectToDataverse(dataverse.getAlias());
    }

    // -------------------- SETTERS --------------------

    public void setFeaturedDataverses(DualListModel<Dataverse> featuredDataverses) {
        this.featuredDataverses = featuredDataverses;
    }
}
