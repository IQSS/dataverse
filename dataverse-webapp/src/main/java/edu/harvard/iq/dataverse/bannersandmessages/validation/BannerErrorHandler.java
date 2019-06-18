package edu.harvard.iq.dataverse.bannersandmessages.validation;

import edu.harvard.iq.dataverse.bannersandmessages.banners.BannerLimits;
import edu.harvard.iq.dataverse.bannersandmessages.banners.DataverseBanner;
import edu.harvard.iq.dataverse.bannersandmessages.banners.DataverseLocalizedBanner;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.DataverseClock;
import edu.harvard.iq.dataverse.util.DateUtil;
import org.apache.tika.Tika;

import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.time.LocalDateTime;
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

    // -------------------- LOGIC --------------------

    public List<FacesMessage> handleBannerAddingErrors(DataverseBanner banner,
                                                       DataverseLocalizedBanner dlb,
                                                       FacesContext faceContext) {

        int localizedBannerIndex = banner.getDataverseLocalizedBanner().indexOf(dlb);
        String extensionDetector = new Tika().detect(dlb.getImage());

        if (dlb.getImage().length < 1) {
            addErrorMessageImageWasMissing(faceContext, localizedBannerIndex);

        } else if (ImageValidator.isImageResolutionTooBig(dlb.getImage(),
                                                          bannerLimits.getMaxWidth(), bannerLimits.getMaxHeight())) {
            addErrorMessageResolutionTooBigError(faceContext, localizedBannerIndex);
        }

        if (!dlb.getImageLink().isPresent()) {
            addErrorMessageLinkWasMissing(faceContext, localizedBannerIndex);
        }

        if (dlb.getImage().length > bannerLimits.getMaxSizeInBytes()) {
            addErrorMessageSizeTooBigError(faceContext, localizedBannerIndex);
        }

        if (!extensionDetector.equals("image/jpeg") && !extensionDetector.equals("image/png")) {
            addErrorMessageWrongExtension(faceContext, localizedBannerIndex);
        }

        validateEndDate(banner.getFromTime(), banner.getToTime(), faceContext);

        return faceContext.getMessageList();
    }

    // -------------------- PRIVATE --------------------

    private void addErrorMessageResolutionTooBigError(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":upload",
                               new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                                                BundleUtil.getStringFromBundle("dataversemessages.banners.resolutionError")));

    }

    private void addErrorMessageLinkWasMissing(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":message-locale",
                               new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                                                BundleUtil.getStringFromBundle("dataversemessages.banners.linkError")));

    }

    private void addErrorMessageWrongExtension(FacesContext faceContext, int index) {

        faceContext.addMessage("edit-text-messages-form:repeater:" + index + ":first-file-warning",
                               new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("messages.error"),
                                                BundleUtil.getStringFromBundle("dataversemessages.banners.extensionError")));

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
