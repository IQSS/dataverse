package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.license.FileTermsOfUse.RestrictType;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.BundleUtil;

import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@ViewScoped
public class TermsOfUseSelectItemsFactory implements Serializable {

    @EJB
    private LicenseDAO licenseDao;
    
    @EJB
    private TermsOfUseFactory termsOfUseFactory;
    
    @EJB
    private TermsOfUseFormMapper termsOfUseFormMapper;
    
    @Inject
    private SettingsWrapper settingsWrapper;
    
    @Inject
    private DataverseSession session;

    
    
    // -------------------- LOGIC --------------------
    
    public List<SelectItem> buildLicenseSelectItems() {
        List<SelectItem> selectItems = new ArrayList<>();
        
        for (License license: licenseDao.findActive()) {
            FileTermsOfUse termsOfUse = termsOfUseFactory.createTermsOfUseFromLicense(license);
            
            selectItems.add(buildSelectItem(termsOfUse, license.getLocalizedName(session.getLocale())));
        }
        
        Optional<SelectItemGroup> otherTermsOfUseItemGroup = buildOtherTermsOfUseItemGroup();
        otherTermsOfUseItemGroup.ifPresent(selectItems::add);
        
        return selectItems;
    }
    
    
    // -------------------- PRIVATE --------------------
    
    private Optional<SelectItemGroup> buildOtherTermsOfUseItemGroup() {
        boolean allRightsReservedEnabled = Boolean.valueOf(settingsWrapper.getSettingValue(Key.AllRightsReservedTermsOfUseActive.toString()));
        boolean restrictedAccessEnabled = Boolean.valueOf(settingsWrapper.getSettingValue(Key.RestrictedAccessTermsOfUseActive.toString()));
        
        if (!allRightsReservedEnabled && !restrictedAccessEnabled) {
            return Optional.empty();
        }
        
        SelectItemGroup otherTermsOfUseItemsGroup = new SelectItemGroup(BundleUtil.getStringFromBundle("file.editTermsOfUseDialog.otherTermsOfUse"));
        List<SelectItem> otherTermsOfUseItems = new ArrayList<>();
        
        if (allRightsReservedEnabled) {
            FileTermsOfUse termsOfUse = termsOfUseFactory.createAllRightsReservedTermsOfUse();
            
            otherTermsOfUseItems.add(buildSelectItem(termsOfUse, BundleUtil.getStringFromBundle("file.allRightsReserved")));
        }
        
        if (restrictedAccessEnabled) {
            FileTermsOfUse termsOfUse = termsOfUseFactory.createRestrictedTermsOfUse(RestrictType.ACADEMIC_PURPOSE);
            
            otherTermsOfUseItems.add(buildSelectItem(termsOfUse, BundleUtil.getStringFromBundle("file.termsOfAccess.restricted")));
        }
        otherTermsOfUseItemsGroup.setSelectItems(otherTermsOfUseItems.toArray(new SelectItem[0]));
        
        return Optional.of(otherTermsOfUseItemsGroup);
    }
    
    private SelectItem buildSelectItem(FileTermsOfUse termsOfUse, String label) {
        TermsOfUseForm termsOfUseForm = termsOfUseFormMapper.mapToForm(termsOfUse);
        return new SelectItem(termsOfUseForm.getTypeWithLicenseId(), label);
    }
    
    
}
