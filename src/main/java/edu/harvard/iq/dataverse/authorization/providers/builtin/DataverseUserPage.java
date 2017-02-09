package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EMailValidator;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.UserNotification;
import static edu.harvard.iq.dataverse.UserNotification.Type.CREATEDV;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthUtil;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailData;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailException;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailUtil;
import edu.harvard.iq.dataverse.mydata.MyDataPage;
import edu.harvard.iq.dataverse.passwordreset.PasswordValidator;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.primefaces.event.TabChangeEvent;

/**
 *
 */
@ViewScoped
@Named("DataverseUserPage")
public class DataverseUserPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseUserPage.class.getCanonicalName());

    public enum EditMode {
        CREATE, EDIT, CHANGE_PASSWORD, FORGOT
    };

    @Inject
    DataverseSession session;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean fileService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    ConfirmEmailServiceBean confirmEmailService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    GroupServiceBean groupService;
    @Inject
    SettingsWrapper settingsWrapper;
    @Inject
    MyDataPage mydatapage;
    @Inject
    PermissionsWrapper permissionsWrapper;

    @EJB
    AuthenticationServiceBean authSvc;

    private AuthenticatedUser currentUser;
    private BuiltinUser builtinUser;    
    private AuthenticatedUserDisplayInfo userDisplayInfo;
    private transient AuthenticationProvider userAuthProvider;
    private EditMode editMode;
    private String redirectPage = "dataverse.xhtml";

    @NotBlank(message = "Please enter a password for your account.")
    private String inputPassword;

    @NotBlank(message = "Please enter a password for your account.")
    private String currentPassword;
    private Long dataverseId;
    private List<UserNotification> notificationsList;
    private int activeIndex;
    private String selectTab = "somedata";
    UIInput usernameField;

    
    private String username;
    boolean nonLocalLoginEnabled;
    
    public String init() {

        // prevent creating a user if signup not allowed.
        boolean safeDefaultIfKeyNotFound = true;
        boolean signupAllowed = settingsWrapper.isTrueForKey(SettingsServiceBean.Key.AllowSignUp.toString(), safeDefaultIfKeyNotFound);

        if (editMode == EditMode.CREATE && !signupAllowed) {
            return "/403.xhtml";
        }

        if (editMode == EditMode.CREATE) {
            if (session.getUser().isAuthenticated()) {
                editMode = null; // we can't be in create mode for an existing user
                
            } else {
                 // in create mode for new user
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("user.signup.tip"));
                userDisplayInfo = new AuthenticatedUserDisplayInfo();
                return "";
            }
        }

        if ( session.getUser().isAuthenticated() ) {
            setCurrentUser((AuthenticatedUser) session.getUser());
            userAuthProvider = authenticationService.lookupProvider(currentUser);
            notificationsList = userNotificationService.findByUser(currentUser.getId());
            
            switch (selectTab) {
                case "notifications":
                    activeIndex = 1;
                    displayNotification();
                    break;
                case "dataRelatedToMe":
                    mydatapage.init();
                    break;
                // case "groupsRoles":
                // activeIndex = 2;
                // break;
                case "accountInfo":
                    activeIndex = 2;
                    // activeIndex = 3;
                    break;
                case "apiTokenTab":
                    activeIndex = 3;
                    break;
                default:
                    activeIndex = 0;
                    break;
            }

        } else {
            return permissionsWrapper.notAuthorized();
        }

        return "";
    }

    public void edit(ActionEvent e) {
        editMode = EditMode.EDIT;
    }

    public void changePassword(ActionEvent e) {
        editMode = EditMode.CHANGE_PASSWORD;
    }

    public void forgotPassword(ActionEvent e) {
        editMode = EditMode.FORGOT;
    }

    public void validateUserName(FacesContext context, UIComponent toValidate, Object value) {
        String userName = (String) value;
        boolean userNameFound = authenticationService.identifierExists(userName);
        
        if (editMode == EditMode.CREATE && userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.username.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void validateUserEmail(FacesContext context, UIComponent toValidate, Object value) {
        String userEmail = (String) value;
        boolean emailValid = EMailValidator.isEmailValid(userEmail, null);
        if (!emailValid) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("oauth2.newAccount.emailInvalid"), null);
            context.addMessage(toValidate.getClientId(context), message);
            logger.info("Email is not valid: " + userEmail);
            return;
        }
        boolean userEmailFound = false;
        AuthenticatedUser aUser = authenticationService.getAuthenticatedUserByEmail(userEmail);
        if (editMode == EditMode.CREATE) {
            if (aUser != null) {
                userEmailFound = true;
            }
        } else {

            // In edit mode...
            // if there's a match on edit make sure that the email belongs to the 
            // user doing the editing by checking ids
            if ( aUser!=null && ! aUser.getId().equals(currentUser.getId()) ){
                userEmailFound = true;
            }
        }
        if (userEmailFound) {
            ((UIInput) toValidate).setValid(false);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.email.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void validateNewPassword(FacesContext context, UIComponent toValidate, Object value) {
        String password = (String) value;
        if (StringUtils.isBlank(password)){
            logger.log(Level.WARNING, "new password is blank");

            ((UIInput) toValidate).setValid(false);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Password Error", "The new password is blank: re-type it again");
            context.addMessage(toValidate.getClientId(context), message);
            return;

        } 

        int minPasswordLength = 6;
        boolean forceNumber = true;
        boolean forceSpecialChar = false;
        boolean forceCapitalLetter = false;
        int maxPasswordLength = 255;

        PasswordValidator validator = PasswordValidator.buildValidator(forceSpecialChar, forceCapitalLetter, forceNumber, minPasswordLength, maxPasswordLength);
        boolean passwordIsComplexEnough = password!= null && validator.validatePassword(password);
        if (!passwordIsComplexEnough) {
            ((UIInput) toValidate).setValid(false);
            String messageDetail = "Password is not complex enough. The password must have at least one letter, one number and be at least " + minPasswordLength + " characters in length.";
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Password Error", messageDetail);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public String save() {
        boolean passwordChanged = false;
        if ( editMode == EditMode.CHANGE_PASSWORD ) {
            final AuthenticationProvider prv = getUserAuthProvider();
            if ( prv.isPasswordUpdateAllowed() ) {
                if ( ! prv.verifyPassword(currentUser.getAuthenticatedUserLookup().getPersistentUserId(), currentPassword) ) {
                    FacesContext.getCurrentInstance().addMessage("currentPassword", 
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.error.wrongPassword"),null));
                    return null;
                }
                prv.updatePassword(currentUser.getAuthenticatedUserLookup().getPersistentUserId(), inputPassword);
                passwordChanged = true;
                
            } else {
                // erroneous state - we can't change the password for this user, so should not have gotten here. Log and bail out.
                logger.log(Level.WARNING, "Attempt to change a password on {0}, whose provider ({1}) does not support password change", new Object[]{currentUser.getIdentifier(), prv});
                JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.error.cannotChangePassword"));
                return null;
            }
        }
        
        if (editMode == EditMode.CREATE) {
            // Create a new built-in user.
            BuiltinUser builtinUser = new BuiltinUser();
            builtinUser.setUserName( getUsername() );
            builtinUser.applyDisplayInfo(userDisplayInfo);
            builtinUser.updateEncryptedPassword(PasswordEncryption.get().encrypt(inputPassword),
                                                PasswordEncryption.getLatestVersionNumber());
            
            AuthenticatedUser au = authenticationService.createAuthenticatedUser(
                    new UserRecordIdentifier(BuiltinAuthenticationProvider.PROVIDER_ID, builtinUser.getUserName()),
                    builtinUser.getUserName(), builtinUser.getDisplayInfo(), false);
            if ( au == null ) {
                // username exists
                getUsernameField().setValid(false);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.username.taken"), null);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(getUsernameField().getClientId(context), message);
                return null;
            }
            
            // Authenticated user registered. Save the new bulitin, and log in.
            builtinUserService.save(builtinUser);
            session.setUser(au);
            /**
             * @todo Move this to
             * AuthenticationServiceBean.createAuthenticatedUser
             */
            userNotificationService.sendNotification(au,
                    new Timestamp(new Date().getTime()),
                    UserNotification.Type.CREATEACC, null);

            // go back to where user came from
            if ("dataverse.xhtml".equals(redirectPage)) {
                redirectPage = redirectPage + "&alias=" + dataverseService.findRootDataverse().getAlias();
            }

            try {
                redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                logger.log(Level.SEVERE, "Server does not support 'UTF-8' encoding.", ex);
                redirectPage = "dataverse.xhtml&alias=" + dataverseService.findRootDataverse().getAlias();
            }

            logger.log(Level.FINE, "Sending user to = {0}", redirectPage);


            return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";            
            
        } else {
            String emailBeforeUpdate = currentUser.getEmail();
            AuthenticatedUser savedUser = authenticationService.updateAuthenticatedUser(currentUser, userDisplayInfo);
            String emailAfterUpdate = savedUser.getEmail();
            editMode = null;
            StringBuilder msg = new StringBuilder( passwordChanged ? "Your account password has been successfully changed." 
                                                                   : "Your account information has been successfully updated.");
            if (!emailBeforeUpdate.equals(emailAfterUpdate)) {
                String expTime = ConfirmEmailUtil.friendlyExpirationTime(systemConfig.getMinutesUntilConfirmEmailTokenExpires());
                msg.append(" Your email address has changed and must be re-verified. Please check your inbox at ")
                        .append(currentUser.getEmail())
                        .append(" and follow the link we've sent. \n\nAlso, please note that the link will only work for the next ")
                        .append(expTime)
                        .append(" before it has expired.");
                // delete unexpired token, if it exists (clean slate)
                confirmEmailService.deleteTokenForUser(currentUser);
                try {
                    confirmEmailService.beginConfirm(currentUser);
                } catch (ConfirmEmailException ex) {
                    logger.log(Level.INFO, "Unable to send email confirmation link to user id {0}", savedUser.getId());
                }
                session.setUser(currentUser);
                JsfHelper.addSuccessMessage(msg.toString());
            } else {
                JsfHelper.addFlashMessage(msg.toString());
            }
            return null;
        }
    }

    public String cancel() {
        if (editMode == EditMode.CREATE) {
            return "/dataverse.xhtml?alias=" + dataverseService.findRootDataverse().getAlias() + "&faces-redirect=true";
        }

        editMode = null;
        return null;
    }

    public String remove(Long notificationId) {
        UserNotification userNotification = userNotificationService.find(notificationId);
        userNotificationService.delete(userNotification);
        for (UserNotification uNotification : notificationsList) {
            if (Objects.equals(uNotification.getId(), userNotification.getId())) {
                notificationsList.remove(uNotification);
                break;
            }
        }
        return null;
    }

    public void onTabChange(TabChangeEvent event) {
        if (event.getTab().getId().equals("notifications")) {
            displayNotification();
        }
        if (event.getTab().getId().equals("dataRelatedToMe")){
            mydatapage.init();
        }
    }

    private String getRoleStringFromUser(AuthenticatedUser au, DvObject dvObj) {
        // Find user's role(s) for given dataverse/dataset
        Set<RoleAssignment> roles = permissionService.assignmentsFor(au, dvObj);
        List<String> roleNames = new ArrayList();

        // Include roles derived from a user's groups
        Set<Group> groupsUserBelongsTo = groupService.groupsFor(au, dvObj);
        for (Group g : groupsUserBelongsTo) {
            roles.addAll(permissionService.assignmentsFor(g, dvObj));
        }

        for (RoleAssignment ra : roles) {
            roleNames.add(ra.getRole().getName());
        }
        if (roleNames.isEmpty()){
            return "[Unknown]";
        }
        return StringUtils.join(roleNames, "/");
    }

    public void displayNotification() {
        for (UserNotification userNotification : notificationsList) {
            switch (userNotification.getType()) {
                case ASSIGNROLE:
                case REVOKEROLE:
                    // Can either be a dataverse or dataset, so search both
                    Dataverse dataverse = dataverseService.find(userNotification.getObjectId());
                    if (dataverse != null) {
                        userNotification.setRoleString(this.getRoleStringFromUser(this.getCurrentUser(), dataverse ));
                        userNotification.setTheObject(dataverse);
                    } else {
                        Dataset dataset = datasetService.find(userNotification.getObjectId());
                        if (dataset != null){
                            userNotification.setRoleString(this.getRoleStringFromUser(this.getCurrentUser(), dataset ));
                            userNotification.setTheObject(dataset);
                        } else {
                            DataFile datafile = fileService.find(userNotification.getObjectId());
                            userNotification.setRoleString(this.getRoleStringFromUser(this.getCurrentUser(), datafile ));
                            userNotification.setTheObject(datafile);
                        }
                    }
                    break;
                case CREATEDV:
                    userNotification.setTheObject(dataverseService.find(userNotification.getObjectId()));
                    break;

                case REQUESTFILEACCESS:
                    DataFile file = fileService.find(userNotification.getObjectId());
                    userNotification.setTheObject(file.getOwner());
                    break;
                case GRANTFILEACCESS:
                case REJECTFILEACCESS:
                    userNotification.setTheObject(datasetService.find(userNotification.getObjectId()));
                    break;

                case MAPLAYERUPDATED:
                case CREATEDS:
                case SUBMITTEDDS:
                case PUBLISHEDDS:
                case RETURNEDDS:
                    userNotification.setTheObject(datasetVersionService.find(userNotification.getObjectId()));
                    break;

                case CREATEACC:
                    userNotification.setTheObject(userNotification.getUser());
                    break;
                    
                case CHECKSUMFAIL:
                    userNotification.setTheObject(datasetService.find(userNotification.getObjectId()));
                    break;

                case FILESYSTEMIMPORT:
                    userNotification.setTheObject(datasetVersionService.find(userNotification.getObjectId()));
                    break;

                case CHECKSUMIMPORT:
                    userNotification.setTheObject(datasetVersionService.find(userNotification.getObjectId()));
                    break;
            }

            userNotification.setDisplayAsRead(userNotification.isReadNotification());
            if (userNotification.isReadNotification() == false) {
                userNotification.setReadNotification(true);
                userNotificationService.save(userNotification);
            }
        }
    }

    public void sendConfirmEmail() {
        logger.fine("called sendConfirmEmail()");
        String userEmail = currentUser.getEmail();

        try {
            confirmEmailService.beginConfirm(currentUser);
            List<String> args = Arrays.asList(
                    userEmail,
                    ConfirmEmailUtil.friendlyExpirationTime(systemConfig.getMinutesUntilConfirmEmailTokenExpires()));
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("confirmEmail.submitRequest.success", args));
        } catch (ConfirmEmailException ex) {
            Logger.getLogger(DataverseUserPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    /**
     * Determines whether the button to send a verification email appears on user page
     * @return 
     */ 
    public boolean showVerifyEmailButton() {
        final Timestamp emailConfirmed = currentUser.getEmailConfirmed();
        final ConfirmEmailData confirmedDate = confirmEmailService.findSingleConfirmEmailDataByUser(currentUser);
        return (!getUserAuthProvider().isEmailVerified())
                && confirmedDate == null
                && emailConfirmed == null;
    }

    public boolean isEmailIsVerified() {

        return currentUser.getEmailConfirmed() != null && confirmEmailService.findSingleConfirmEmailDataByUser(currentUser) == null;
    }

    public boolean isEmailNotVerified() {
        return currentUser.getEmailConfirmed() == null || confirmEmailService.findSingleConfirmEmailDataByUser(currentUser) != null;
    }

    public boolean isEmailGrandfathered() {
        return currentUser.getEmailConfirmed().equals(ConfirmEmailUtil.getGrandfatheredTime());
    }
    
    public AuthenticationProvider getUserAuthProvider() {
        if ( userAuthProvider == null  ) {
            userAuthProvider = authenticationService.lookupProvider(currentUser);
        }
        return userAuthProvider;
    }
    
    public boolean isPasswordEditable() {
        return getUserAuthProvider().isPasswordUpdateAllowed();
    }
    
    public boolean isAccountDetailsEditable() {
        return getUserAuthProvider().isUserInfoUpdateAllowed();
    }

    public AuthenticatedUserDisplayInfo getUserDisplayInfo() {
        return userDisplayInfo;
    }

    public void setUserDisplayInfo(AuthenticatedUserDisplayInfo userDisplayInfo) {
        this.userDisplayInfo = userDisplayInfo;
    }
    
    public EditMode getChangePasswordMode () {
        return EditMode.CHANGE_PASSWORD;
    }

    public AuthenticatedUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(AuthenticatedUser currentUser) {
        this.currentUser = currentUser;
        userDisplayInfo = currentUser.getDisplayInfo();
        username = currentUser.getUserIdentifier();
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public String getRedirectPage() {
        return redirectPage;
    }

    public void setRedirectPage(String redirectPage) {
        this.redirectPage = redirectPage;
    } 

    public String getInputPassword() {
        return inputPassword;
    }

    public void setInputPassword(String inputPassword) {
        this.inputPassword = inputPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public Long getDataverseId() {

        if (dataverseId == null) {
            dataverseId = dataverseService.findRootDataverse().getId();
        }
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public List getNotificationsList() {
        return notificationsList;
    }

    public void setNotificationsList(List notificationsList) {
        this.notificationsList = notificationsList;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public String getSelectTab() {
        return selectTab;
    }

    public void setSelectTab(String selectTab) {
        this.selectTab = selectTab;
    }

    public UIInput getUsernameField() {
        return usernameField;
    }

    public void setUsernameField(UIInput usernameField) {
        this.usernameField = usernameField;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isNonLocalLoginEnabled() {
        return AuthUtil.isNonLocalLoginEnabled(authenticationService.getAuthenticationProviders());
    }

}