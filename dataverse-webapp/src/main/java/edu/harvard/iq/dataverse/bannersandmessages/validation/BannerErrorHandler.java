package edu.harvard.iq.dataverse.bannersandmessages.validation;

import edu.harvard.iq.dataverse.bannersandmessages.banners.BannerLimits;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseBanner;
import edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages.DataverseLocalizedBanner;

import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import java.util.List;

/**
 * Class which handles errors when adding new banner that can't be validated by standard jsf
 * validators. Errors are displayed thanks to <p:message/>
 */
@Stateless
public class BannerErrorHandler {

    private BannerLimits bannerLimits;

    public BannerErrorHandler() {
    }

    @Inject
    public BannerErrorHandler(BannerLimits bannerLimits) {
        this.bannerLimits = bannerLimits;
    }

    // -------------------- LOGIC --------------------

    public List<FacesMessage> handleBannerAddingErrors(DataverseBanner banner,
                                                       DataverseLocalizedBanner dlb,
                                                       FacesContext faceContext) {

        int localizedBannerIndex = banner.getDataverseLocalizedBanner().indexOf(dlb);

        if (dlb.getImage().length < 1) {
            addErrorMessageImageWasMissing(faceContext, localizedBannerIndex);

        } else if (ImageValidator.isImageResolutionTooBig(dlb.getImage(),
                                                          bannerLimits.getMaxWidth(), bannerLimits.getMaxHeight())) {
            addErrorMessageResolutionTooBigError(faceContext, localizedBannerIndex);
        }

        return faceContext.getMessageList();
    }

    // -------------------- PRIVATE --------------------

    private void addErrorMessageResolutionTooBigError(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":upload",
                               new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                                                BundleUtil.getStringFromBundle("dataversemessages.banners.resolutionError")));

    }

    private void addErrorMessageImageWasMissing(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":upload",
                               new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                                                BundleUtil.getStringFromBundle("dataversemessages.banners.missingError")));

    }
}
