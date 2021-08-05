package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.GenericDao;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.bannersandmessages.validation.ImageValidator;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.license.dto.LicenseDto;
import edu.harvard.iq.dataverse.license.dto.LicenseIconDto;
import edu.harvard.iq.dataverse.license.dto.LicenseMapper;
import edu.harvard.iq.dataverse.license.dto.LocaleTextDto;
import edu.harvard.iq.dataverse.persistence.config.URLValidator;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Page that is responsible for showing all valid and invalid licenses while also giving ability to disable/enable
 * them all across Dataverse.
 */
@ViewScoped
@Named("LicenseListingPage")
public class LicenseListingPage implements Serializable {

    @Inject
    private DataverseSession session;

    @Inject
    private SettingsWrapper settingsWrapper;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @Inject
    private LicenseRepository licenseRepository;

    @Inject
    private LicenseMapper licenseMapper;

    @Inject
    private SystemConfig systemConfig;

    @Inject
    private LicenseLimits licenseLimits;

    private List<LicenseDto> licenses = new ArrayList<>();

    private LicenseDto freshLicense;
    private LicenseDto licenseForPreview;
    private LicenseDto licenseForEdit;

    // -------------------- GETTERS --------------------

    public List<LicenseDto> getLicenses() {
        return licenses;
    }

    public LicenseDto getFreshLicense() {
        return freshLicense;
    }

    /**
     * Is Used to indicate which license localized names to show.
     */
    public LicenseDto getLicenseForPreview() {
        return licenseForPreview;
    }

    public LicenseDto getLicenseForEdit() {
        return licenseForEdit;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (!session.getUser().isSuperuser() || systemConfig.isReadonlyMode()) {
            return permissionsWrapper.notAuthorized();
        }

        licenses = licenseMapper.mapToDtos(licenseRepository.findAllOrderedByPosition());
        removeLicenseLanguagesNotPresentInDataverse(licenses);

        freshLicense = prepareFreshLicense();

        return StringUtils.EMPTY;
    }

    /**
     * Handles image upload for new license.
     *
     * @param event
     */
    public void uploadImageForNewLicenseEvent(FileUploadEvent event) {
        UploadedFile uploadedImage = event.getFile();

        if (!isUploadedImageValid(event, uploadedImage)) return;

        freshLicense.getIcon().setContent(uploadedImage.getContent());
        freshLicense.getIcon().setContentType(uploadedImage.getContentType());
    }

    /**
     * Handles image upload for license that already exists and wants to be edited.
     *
     * @param event
     */
    public void editLicenseImageEvent(FileUploadEvent event) {
        UploadedFile uploadedImage = event.getFile();

        if (!isUploadedImageValid(event, uploadedImage)) return;

        licenseForEdit.getIcon().setContent(uploadedImage.getContent());
        licenseForEdit.getIcon().setContentType(uploadedImage.getContentType());
    }

    /**
     * Validates and sets active license status
     *
     * @param licenseDto
     */
    public void saveLicenseActiveStatus(LicenseDto licenseDto) {

        if (licenseRepository.countActiveLicenses() <= 1 && !licenseDto.isActive()) {
            licenseDto.setActive(true);
            displayNoLicensesActiveWarningMessage();
            return;
        }

        License license = licenseRepository.getById(licenseDto.getId());
        license.setActive(licenseDto.isActive());
        licenseRepository.save(license);
    }

    /**
     * Saves new license and validates if there are any errors.
     *
     * @return redirect string
     */
    public String saveNewLicense() {

        freshLicense.setPosition(licenseRepository.findMaxLicensePosition() + 1);
        License license = licenseMapper.mapToLicense(freshLicense);
        licenseRepository.save(license);

        freshLicense = prepareFreshLicense();

        return "/dashboard-licenses.xhtml?&faces-redirect=true";
    }

    public String saveEditedLicense(LicenseDto licenseDto) {

        License license = licenseRepository.getById(licenseDto.getId());
        license = licenseMapper.editLicense(licenseDto, license);
        licenseRepository.save(license);

        return "/dashboard-licenses.xhtml?&faces-redirect=true";
    }

    public void validateLicenseUrl(FacesContext context, UIComponent toValidate, Object rawValue) throws ValidatorException {
        String valueStr = (String)rawValue;

        if (!URLValidator.isURLValid(valueStr)) {
            String message = BundleUtil.getStringFromBundle("dashboard.license.invalidURL", BundleUtil.getStringFromBundle("dashboard.license.licenseURL"));
            throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "", message));
        }
    }

    public String redirectToLicenseReorderPage() {
        return "/dashboard-licenses-reorder.xhtml?&faces-redirect=true";
    }

    public int getLicenseIconFileSizeLimit() {
        return licenseLimits.getMaxSizeInBytes();
    }

    public StreamedContent getLicenseIconForDisplay(LicenseIconDto licenseIconDto) {
        return DefaultStreamedContent.builder()
                .contentType(licenseIconDto.getContentType())
                .stream(() -> new ByteArrayInputStream(licenseIconDto.getContent()))
                .build();
    }

    public void removeLicenseIcon(LicenseIconDto licenseIconDto) {
        licenseIconDto.setContent(new byte[0]);
        licenseIconDto.setContentType(StringUtils.EMPTY);
    }

    public boolean hasIcon(LicenseIconDto licenseIconDto) {
        return licenseIconDto != null && licenseIconDto.getContent() != null && licenseIconDto.getContent().length > 0;
    }

    public String getLocalizedNameLabel(LocaleTextDto localeTextDto) {
        return BundleUtil.getStringFromBundle("dashboard.license.localizedName", localeTextDto.getLocale().getDisplayName(session.getLocale()));
    }

    public String getRequiredMessage(String fieldName) {
        return BundleUtil.getStringFromBundle("dashboard.license.missingTextField", fieldName);
    }

    // -------------------- PRIVATE --------------------

    /**
     * Prepares licenseDto in order to use it for adding new license.
     *
     * @return licenseDto
     */
    private LicenseDto prepareFreshLicense() {
        LicenseDto licenseDto = new LicenseDto();

        settingsWrapper.getConfiguredLocales().keySet().forEach(localeKey ->
                                                                        licenseDto.getLocalizedNames().add(new LocaleTextDto(Locale.forLanguageTag(localeKey), StringUtils.EMPTY)));

        licenseDto.setIcon(new LicenseIconDto());
        licenseDto.setUrl(StringUtils.EMPTY);
        licenseDto.setName(StringUtils.EMPTY);
        licenseDto.setActive(false);

        return licenseDto;
    }

    private void removeLicenseLanguagesNotPresentInDataverse(List<LicenseDto> licenses) {
        Set<String> dataverseLocales = settingsWrapper.getConfiguredLocales().keySet();

        licenses.stream()
                .map(LicenseDto::getLocalizedNames)
                .forEach(localeTextDtos -> localeTextDtos
                        .removeIf(localeTextDto -> !dataverseLocales.contains(localeTextDto.getLocale().getLanguage())));
    }

    private void displayNoLicensesActiveWarningMessage() {
        FacesContext.getCurrentInstance().
                addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                  StringUtils.EMPTY,
                                                  BundleUtil.getStringFromBundle("dashboard.license.noActiveLicensesWarning")));
    }

    private boolean isUploadedImageValid(FileUploadEvent event, UploadedFile uploadedImage) {
        if (ImageValidator.isImageResolutionTooBig(uploadedImage.getContent(),
                licenseLimits.getMaxWidth(), licenseLimits.getMaxHeight())) {

            FacesContext context = FacesContext.getCurrentInstance();
            context.validationFailed();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "", BundleUtil.getStringFromBundle("dashboard.license.fileType.resolutionError"));
            FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(context), message);
            return false;
        }
        return true;
    }

    // -------------------- SETTERS --------------------

    public void setLicenseForPreview(LicenseDto licenseForPreview) {
        this.licenseForPreview = licenseForPreview;
    }

    public void setLicenseForEdit(LicenseDto licenseForEdit) {
        this.licenseForEdit = licenseForEdit;
    }

}
