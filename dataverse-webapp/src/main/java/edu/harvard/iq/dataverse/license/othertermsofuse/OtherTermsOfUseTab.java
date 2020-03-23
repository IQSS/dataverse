package edu.harvard.iq.dataverse.license.othertermsofuse;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.primefaces.model.DefaultStreamedContent;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("OtherTermsOfUseTab")
public class OtherTermsOfUseTab implements Serializable {

    @Inject
    private DataverseSession session;

    @Inject
    private PermissionsWrapper permissionsWrapper;

    @EJB
    private SettingsServiceBean settingsServiceBean;

    private List<OtherTermsOfUseDto> otherTermsOfUseDto = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public List<OtherTermsOfUseDto> getOtherTermsOfUseDto() {
        return otherTermsOfUseDto;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (!session.getUser().isSuperuser()) {
            return permissionsWrapper.notAuthorized();
        }

        otherTermsOfUseDto.add(new OtherTermsOfUseDto(Key.AllRightsReservedTermsOfUseActive,
                                                      "All rights reserved",
                                                      Boolean.parseBoolean(settingsServiceBean.getValueForKey(Key.AllRightsReservedTermsOfUseActive)),
                                                      DefaultStreamedContent.builder()
                                                              .stream(() -> new ByteArrayInputStream(FileUtil.getFileFromResources(
                                                                      "/images/allrightsreserved.png")))
                                                              .build()));

        otherTermsOfUseDto.add(new OtherTermsOfUseDto(Key.RestrictedAccessTermsOfUseActive,
                                                      "Restricted access",
                                                      Boolean.parseBoolean(settingsServiceBean.getValueForKey(Key.RestrictedAccessTermsOfUseActive)),
                                                      DefaultStreamedContent.builder()
                                                              .stream(() -> new ByteArrayInputStream(FileUtil.getFileFromResources(
                                                                      "/images/restrictedaccess.png")))
                                                      .build()));

        return StringUtils.EMPTY;
    }

    public void saveLicenseActiveStatus(OtherTermsOfUseDto otherTermsOfUseDto) {
        settingsServiceBean.setValueForKey(otherTermsOfUseDto.getIsActiveSettingKey(),
                                           String.valueOf(otherTermsOfUseDto.isActive()));
    }
}
