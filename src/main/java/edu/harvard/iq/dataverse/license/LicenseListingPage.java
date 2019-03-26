package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.license.dto.LicenseDto;
import edu.harvard.iq.dataverse.license.dto.LicenseMapper;
import edu.harvard.iq.dataverse.util.BundleUtil;
import org.apache.commons.lang.StringUtils;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


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
    private PermissionsWrapper permissionsWrapper;

    @Inject
    private LicenseDAO licenseDAO;

    @Inject
    private LicenseMapper licenseMapper;

    private List<LicenseDto> licenses = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public List<LicenseDto> getLicenses() {
        return licenses;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (!session.getUser().isSuperuser()) {
            return permissionsWrapper.notAuthorized();
        }

        licenses = licenseMapper.mapToDtos(licenseDAO.findAll());

        return StringUtils.EMPTY;
    }

    public void saveLicenseActiveStatus(LicenseDto licenseDto) {

        if (licenseDAO.countActiveLicenses() <= 1 && !licenseDto.isActive()) {
            licenseDto.setActive(true);
            displayNoLicensesActiveWarningMessage();
            return;
        }

        License license = licenseDAO.find(licenseDto.getId());
        license.setActive(licenseDto.isActive());
        licenseDAO.saveChanges(license);
    }

    public String redirectToLicenseReorderPage() {
        return "/dashboard-licenses-reorder.xhtml?&faces-redirect=true";
    }

    // -------------------- PRIVATE --------------------

    private void displayNoLicensesActiveWarningMessage() {
        FacesContext.getCurrentInstance().
                addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        StringUtils.EMPTY,
                        BundleUtil.getStringFromBundle("dashboard.license.noActiveLicensesWarning")));
        ;
    }

}
