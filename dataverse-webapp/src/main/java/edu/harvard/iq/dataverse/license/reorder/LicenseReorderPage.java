package edu.harvard.iq.dataverse.license.reorder;


import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.license.dto.LicenseMapper;
import edu.harvard.iq.dataverse.license.dto.LicenseSimpleDto;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.ReorderEvent;

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
    private LicenseRepository licenseRepository;

    @Inject
    private LicenseMapper licenseMapper;

    @Inject
    private SystemConfig systemConfig;

    private List<LicenseSimpleDto> licenses = new ArrayList<>();
    
    private Tuple2<Integer, Integer> lastReorderFromAndTo;
    private LicenseSimpleDto lastReorderLicense;

    // -------------------- GETTERS --------------------

    public List<LicenseSimpleDto> getLicenses() {
        return licenses;
    }

    public Tuple2<Integer, Integer> getLastReorderFromAndTo() {
        return lastReorderFromAndTo;
    }

    public LicenseSimpleDto getLastReorderLicense() {
        return lastReorderLicense;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (!session.getUser().isSuperuser() || systemConfig.isReadonlyMode()) {
            return permissionsWrapper.notAuthorized();
        }

        licenses = licenseMapper.mapToSimpleDtos(licenseRepository.findAllOrderedByPosition(), Locale.forLanguageTag(session.getLocaleCode()));

        return StringUtils.EMPTY;
    }

    public void moveUp(int licenseIndex) {
        LicenseSimpleDto licenseToMove = licenses.remove(licenseIndex);
        licenses.add(licenseIndex - 1, licenseToMove);

        lastReorderFromAndTo = Tuple.of(licenseIndex, licenseIndex - 1);
        lastReorderLicense = licenseToMove;
    }

    public void moveDown(int licenseIndex) {
        LicenseSimpleDto licenseToMove = licenses.remove(licenseIndex);
        licenses.add(licenseIndex + 1, licenseToMove);

        lastReorderFromAndTo = Tuple.of(licenseIndex, licenseIndex + 1);
        lastReorderLicense = licenseToMove;
    }

    public void onRowReorder(ReorderEvent event) {
        lastReorderFromAndTo = Tuple.of(event.getFromIndex(), event.getToIndex());
        lastReorderLicense = licenses.get(event.getToIndex());
    }

    public void undoLastReorder() {
        LicenseSimpleDto licenseMoved = licenses.remove(lastReorderFromAndTo._2().intValue());
        licenses.add(lastReorderFromAndTo._1(), licenseMoved);

        lastReorderFromAndTo = null;
        lastReorderLicense = null;
    }

    /**
     * Saves new positions of the licenses.
     *
     * @return redirect link
     */
    public String saveChanges() {

        licenses.forEach(licenseDto ->
                         {
                             License license = licenseRepository.getById(licenseDto.getLicenseId());
                             license.setPosition(licenses.indexOf(licenseDto) + 1L);
                             licenseRepository.save(license);
                         });

        return "/dashboard-licenses.xhtml?&faces-redirect=true";
    }

    public String cancel() {
        return "/dashboard-licenses.xhtml?&faces-redirect=true";
    }
}
