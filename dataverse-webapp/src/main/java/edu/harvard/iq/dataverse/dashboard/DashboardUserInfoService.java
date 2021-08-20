package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.common.AuthenticatedUserUtil;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Stateless
public class DashboardUserInfoService {

    @Inject
    private ConfirmEmailServiceBean confirmEmailService;

    @Inject
    private DataverseSession session;

    // -------------------- LOGIC --------------------

    public List<DashboardUserInfo> createDashboardUsers(UserListResult userListResult) {
        List<DashboardUserInfo> userInfoList = new LinkedList<>();
        for (AuthenticatedUser user : userListResult.getUserList()) {
            userInfoList.add(new DashboardUserInfo(user, getAuthProviderFriendlyName(user),
                    hasSelectedConfirmedEmail(user), getUserNotificationLanguageDisplayName(user, session.getLocale())));
        }
        return userInfoList;
    }

    // -------------------- PRIVATE --------------------

    private String getAuthProviderFriendlyName(AuthenticatedUser user) {
        return AuthenticatedUserUtil.getAuthenticationProviderFriendlyName(user.getAuthenticatedUserLookup().getAuthenticationProviderId());
    }

    private boolean hasSelectedConfirmedEmail(AuthenticatedUser user) {
        return user.getEmailConfirmed() != null &&
                confirmEmailService.findSingleConfirmEmailDataByUser(user) == null;
    }

    private String getUserNotificationLanguageDisplayName(AuthenticatedUser user, Locale locale) {
        return StringUtils.capitalize(user.getNotificationsLanguage().getDisplayLanguage(locale));
    }
}
