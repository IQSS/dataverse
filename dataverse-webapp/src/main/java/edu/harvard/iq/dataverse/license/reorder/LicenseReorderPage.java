package edu.harvard.iq.dataverse.license.reorder;


import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.license.LicenseDAO;
import edu.harvard.iq.dataverse.license.dto.LicenseMapper;
import edu.harvard.iq.dataverse.license.dto.LicenseSimpleDto;
import org.apache.commons.lang.StringUtils;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ViewScoped
@Named("LicenseReorderPage")
public class LicenseReorderPage implements Serializable {

    @Inject
    private DataverseSession session;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @Inject
    private LicenseDAO licenseDAO;

    @Inject
    private LicenseMapper licenseMapper;

    private List<LicenseSimpleDto> licenses = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public List<LicenseSimpleDto> getLicenses() {
        return licenses;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (!session.getUser().isSuperuser()) {
            return permissionsWrapper.notAuthorized();
        }

        licenses = licenseMapper.mapToSimpleDtos(licenseDAO.findAll(), Locale.forLanguageTag(session.getLocaleCode()));

        return StringUtils.EMPTY;
    }

    /**
     * Saves new positions of the licenses.
     *
     * @return redirect link
     */
    public String saveChanges() {

        licenses.forEach(licenseDto ->
                         {
                             License license = licenseDAO.find(licenseDto.getLicenseId());
                             license.setPosition(licenses.indexOf(licenseDto) + 1L);
                             licenseDAO.saveChanges(license);
                         });

        return "/dashboard-licenses.xhtml?&faces-redirect=true";
    }

    public String cancel() {
        return "/dashboard-licenses.xhtml?&faces-redirect=true";
    }
}
