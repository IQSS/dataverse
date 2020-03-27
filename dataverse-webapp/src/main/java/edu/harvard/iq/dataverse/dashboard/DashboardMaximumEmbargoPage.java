package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import javax.faces.view.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@ViewScoped
@Named("MaximumEmbargoPage")
public class DashboardMaximumEmbargoPage implements Serializable {

    private SettingsServiceBean settingsService;
    private DataverseSession session;
    private PermissionsWrapper permissionsWrapper;
    private DataverseDao dataverseDao;

    private boolean isMaximumEmbargoSet;
    private int maximumEmbargoLength;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DashboardMaximumEmbargoPage() {
    }

    @Inject
    public DashboardMaximumEmbargoPage(SettingsServiceBean settingsService, DataverseSession session,
                                       PermissionsWrapper permissionsWrapper, DataverseDao dataverseDao) {
        this.settingsService = settingsService;
        this.session = session;
        this.permissionsWrapper = permissionsWrapper;
        this.dataverseDao = dataverseDao;
    }

    // -------------------- GETTERS --------------------

    public boolean isMaximumEmbargoSet() {
        return isMaximumEmbargoSet;
    }

    public int getMaximumEmbargoLength() {
        return maximumEmbargoLength;
    }

    // -------------------- LOGIC --------------------
    public String init() {
        if (!session.getUser().isSuperuser()) {
            return permissionsWrapper.notAuthorized();
        }

        isMaximumEmbargoSet = settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.MaximumEmbargoLength) > 0;
        maximumEmbargoLength = settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.MaximumEmbargoLength);

        return StringUtils.EMPTY;
    }

    public String save() {
        if(isMaximumEmbargoSet) {
            setMaxEmbargoSetting(maximumEmbargoLength);
        } else {
            setMaxEmbargoSetting(0);
        }

        return StringUtils.EMPTY;
    }


    public String cancel() {
        return "/dashboard.xhtml?dataverseId=" + dataverseDao.findRootDataverse().getId() + "&faces-redirect=true";
    }

    // -------------------- PRIVATE ---------------------
    private void setMaxEmbargoSetting(int maxLength) {
        Try.of(() -> settingsService.setValueForKey(SettingsServiceBean.Key.MaximumEmbargoLength, Integer.toString(maxLength)))
                .onSuccess(setting -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dashboard.card.maximumembargo.save.success")))
                .onFailure(setting -> JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dashboard.card.maximumembargo.save.failure")));
    }

    // -------------------- SETTERS --------------------

    public void setMaximumEmbargoSet(boolean maximumEmbargoSet) {
        isMaximumEmbargoSet = maximumEmbargoSet;
    }

    public void setMaximumEmbargoLength(int maximumEmbargoLength) {
        this.maximumEmbargoLength = maximumEmbargoLength;
    }
}
