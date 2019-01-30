package edu.harvard.iq.dataverse.dataverse.validation;

import edu.harvard.iq.dataverse.dataverse.banners.BannerLimits;
import edu.harvard.iq.dataverse.dataverse.banners.DataverseBanner;
import edu.harvard.iq.dataverse.dataverse.banners.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.DataverseClock;
import edu.harvard.iq.dataverse.util.DateUtil;

import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class which handles errors when adding new banner, errors are displayed thanks to <p:message/>
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

    public List<FacesMessage> handleBannerAddingErrors(DataverseBanner banner,
                                                       DataverseLocalizedBanner dlb,
                                                       FacesContext faceContext) {

        ArrayList<DataverseLocalizedBanner> dataverseLocalizedBanners =
                new ArrayList<>(banner.getDataverseLocalizedBanner());

        if (dlb.getImage().length < 1) {
            addErrorMessageImageWasMissing(faceContext, dataverseLocalizedBanners.indexOf(dlb));

        } else if (ImageValidator.isImageResolutionTooBig(dlb.getImage(),
                bannerLimits.getMaxWidth(), bannerLimits.getMaxHeight())) {
            addErrorMessageResolutionTooBigError(faceContext, dataverseLocalizedBanners.indexOf(dlb));
        }

        if (dlb.getImage().length > bannerLimits.getMaxSizeInBytes()) {
            addErrorMessageSizeTooBigError(faceContext, dataverseLocalizedBanners.indexOf(dlb));
        }

        validateEndDate(banner.getFromTime(), banner.getToTime(), faceContext);

        return faceContext.getMessageList();
    }

    private void addErrorMessageResolutionTooBigError(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":upload",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                        BundleUtil.getStringFromBundle("dataversemessages.banners.resolutionError")));

    }

    private void addErrorMessageSizeTooBigError(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":second-file-warning",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                        BundleUtil.getStringFromBundle("dataversemessages.banners.sizeError")));

    }

    private void addErrorMessageImageWasMissing(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":upload",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                        BundleUtil.getStringFromBundle("dataversemessages.banners.missingError")));

    }

    private void validateEndDate(Date fromTime, Date toTime, FacesContext faceContext) {
        if (fromTime == null || toTime == null) {
            return;
        }
        if (toTime.before(fromTime)) {
            faceContext.addMessage("edit-text-messages-form:message-fromtime",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                            BundleUtil.getStringFromBundle("textmessages.enddate.valid")));
        }
        LocalDateTime now = DataverseClock.now();

        if (!toTime.after(DateUtil.convertToDate(now))) {
            faceContext.addMessage("edit-text-messages-form:message-totime",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                            BundleUtil.getStringFromBundle("textmessages.enddate.future")));
        }
    }
}
