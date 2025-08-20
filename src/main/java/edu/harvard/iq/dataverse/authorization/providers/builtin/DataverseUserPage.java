package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.validation.EMailValidator;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.validation.UserNameValidator;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthUtil;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailException;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailUtil;
import edu.harvard.iq.dataverse.mydata.MyDataPage;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import static edu.harvard.iq.dataverse.util.StringUtil.toOption;

import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ActionEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
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
    UserServiceBean userService;
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
    @EJB
    PasswordValidatorServiceBean passwordValidatorService;
    @Inject
    SettingsWrapper settingsWrapper;
    @Inject
    MyDataPage mydatapage;
    @Inject
    PermissionsWrapper permissionsWrapper;

    @EJB
    AuthenticationServiceBean authSvc;
    
    @Inject
    private OAuth2LoginBackingBean oauth2LoginBackingBean;


    private AuthenticatedUser currentUser;
    private AuthenticatedUserDisplayInfo userDisplayInfo;
    private transient AuthenticationProvider userAuthProvider;
    private EditMode editMode;
    private String redirectPage = "dataverse.xhtml";
    private final String accountInfoTab = "dataverseuser.xhtml?selectTab=accountInfo";

    @NotBlank(message = "{password.retype}")
    private String inputPassword;

    @NotBlank(message = "{password.current}")
    private String currentPassword;
    private Long dataverseId;
    private List<UserNotification> notificationsList;
    private int activeIndex;
    private String selectTab = "dataRelatedToMe";
    UIInput usernameField;

    
    private String username;
    boolean nonLocalLoginEnabled;
    private List<String> passwordErrors;
    
    
    private List<Type> notificationTypeList;
    private Set<Type> mutedEmails;
    private Set<Type> mutedNotifications;
    private Set<Type> disabledNotifications;

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
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("user.message.signup.label"), BundleUtil.getStringFromBundle("user.message.signup.tip"));
                userDisplayInfo = new AuthenticatedUserDisplayInfo();
                return "";
            }
        }

        if (session.getUser(true).isAuthenticated()) {
            setCurrentUser((AuthenticatedUser) session.getUser());
            userAuthProvider = authenticationService.lookupProvider(currentUser);
            notificationsList = userNotificationService.findByUser(currentUser.getId());
            notificationTypeList = Arrays.asList(Type.values()).stream()
                    .filter(x -> !Type.CONFIRMEMAIL.equals(x) && x.hasDescription() && !settingsWrapper.isAlwaysMuted(x))
                    .collect(Collectors.toList());
            mutedEmails = new HashSet<>(currentUser.getMutedEmails());
            mutedNotifications = new HashSet<>(currentUser.getMutedNotifications());
            disabledNotifications = new HashSet<>(settingsWrapper.getAlwaysMutedSet());
            disabledNotifications.addAll(settingsWrapper.getNeverMutedSet());
            
            switch (selectTab) {
                case "notifications":
                    activeIndex = 1;
                    displayNotification();
                    break;
                case "dataRelatedToMe":
                    mydatapage.init();
                    activeIndex = 0;
                    break;
                case "accountInfo":
                    activeIndex = 2;
                    break;
                case "apiTokenTab":
                    activeIndex = 3;
                    break;
                default:
                    //TODO: Do we need to call mydatapage.init(); here too?
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
        
        // SF fix for issue 3752
        // checks if username has any invalid characters 
        boolean userNameValid = userName != null && UserNameValidator.isUserNameValid(userName);
        
        if (editMode == EditMode.CREATE && userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.username.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
        
        if (editMode == EditMode.CREATE && !userNameValid) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.username.invalid"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void validateUserEmail(FacesContext context, UIComponent toValidate, Object value) {
        String userEmail = (String) value;
        boolean emailValid = EMailValidator.isEmailValid(userEmail);
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
                    BundleUtil.getStringFromBundle("passwdVal.passwdReset.valFacesError"), BundleUtil.getStringFromBundle("passwdVal.passwdReset.valFacesErrorDesc"));
            context.addMessage(toValidate.getClientId(context), message);
            return;

        } 

        final List<String> errors = passwordValidatorService.validate(password, new Date(), false);
        this.passwordErrors = errors;
        if (!errors.isEmpty()) {
            ((UIInput) toValidate).setValid(false);
        }
    }

    public String save() {
        boolean passwordChanged = false;
        
        //First reget user to make sure they weren't deactivated or deleted
        if (session.getUser().isAuthenticated() && !session.getUser(true).isAuthenticated()) {
            return "dataverse.xhtml?alias=" + dataverseService.findRootDataverse().getAlias() + "&faces-redirect=true";
        }
        
        if (editMode == EditMode.CHANGE_PASSWORD) {
            final AuthenticationProvider prv = getUserAuthProvider();
            if (prv.isPasswordUpdateAllowed()) {
                if (!prv.verifyPassword(currentUser.getAuthenticatedUserLookup().getPersistentUserId(), currentPassword)) {
                    FacesContext.getCurrentInstance().addMessage("currentPassword",
                                                                 new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.error.wrongPassword"), null));
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
            builtinUser.updateEncryptedPassword(PasswordEncryption.get().encrypt(inputPassword),
                                                PasswordEncryption.getLatestVersionNumber());
            
            AuthenticatedUser au = authenticationService.createAuthenticatedUser(
                    new UserRecordIdentifier(BuiltinAuthenticationProvider.PROVIDER_ID, builtinUser.getUserName()),
                    builtinUser.getUserName(), userDisplayInfo, false);
            if ( au == null ) {
                // Username already exists, show an error message
                getUsernameField().setValid(false);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.username.taken"), null);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(getUsernameField().getClientId(context), message);
                return null;
            }
            
            // The Authenticated User was just created via the UI, add an initial login timestamp
            au = userService.updateLastLogin(au);
            
            // Authenticated user registered. Save the new bulitin, and log in.
            builtinUserService.save(builtinUser);
            session.setUser(au);
            /**
             * @todo Move this to
             * AuthenticationServiceBean.createAuthenticatedUser
             */
            userNotificationService.sendNotification(au,
                    new Timestamp(new Date().getTime()),
                    Type.CREATEACC, null);

            // go back to where user came from
            
            // (but if they came from the login page, then send them to the 
            // root dataverse page instead. the only situation where we do 
            // want to send them back to the login page is if they hit 
            // 'cancel'. 
            
            if ("/loginpage.xhtml".equals(redirectPage) || "loginpage.xhtml".equals(redirectPage)) {
                redirectPage = "/dataverse.xhtml";
            }
            
            if ("dataverse.xhtml".equals(redirectPage)) {
                redirectPage = redirectPage + "?alias=" + dataverseService.findRootDataverse().getAlias();
            }

            try {
                redirectPage = URLDecoder.decode(redirectPage, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                logger.log(Level.SEVERE, "Server does not support 'UTF-8' encoding.", ex);
                redirectPage = "dataverse.xhtml?alias=" + dataverseService.findRootDataverse().getAlias();
            }

            logger.log(Level.FINE, "Sending user to = {0}", redirectPage);


            return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";            

        //Happens if user is logged out while editing
        } else if (!session.getUser().isAuthenticated()) {
            logger.info("Redirecting");
            return permissionsWrapper.notAuthorized() + "faces-redirect=true";
        }else {
            currentUser.setMutedEmails(mutedEmails);
            currentUser.setMutedNotifications(mutedNotifications);
            String emailBeforeUpdate = currentUser.getEmail();
            AuthenticatedUser savedUser = authenticationService.updateAuthenticatedUser(currentUser, userDisplayInfo);
            String emailAfterUpdate = savedUser.getEmail();
            editMode = null;
            StringBuilder msg = new StringBuilder( passwordChanged ? BundleUtil.getStringFromBundle("userPage.passwordChanged" )
                                                                   :  BundleUtil.getStringFromBundle("userPage.informationUpdated"));
            if (!emailBeforeUpdate.equals(emailAfterUpdate)) {
                String expTime = ConfirmEmailUtil.friendlyExpirationTime(systemConfig.getMinutesUntilConfirmEmailTokenExpires());
                List<String> args = Arrays.asList(currentUser.getEmail(),expTime);
                // delete unexpired token, if it exists (clean slate)
                confirmEmailService.deleteTokenForUser(currentUser);
                try {
                    confirmEmailService.beginConfirm(currentUser);
                } catch (ConfirmEmailException ex) {
                    logger.log(Level.INFO, "Unable to send email confirmation link to user id {0}", savedUser.getId());
                }
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("confirmEmail.changed", args));
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
        List<String> roleNames = new ArrayList<>();

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
                case REQUESTEDFILEACCESS:
                    DataFile file = fileService.find(userNotification.getObjectId());
                    if (file != null) {
                        userNotification.setTheObject(file.getOwner());
                    }
                    break;
                case GRANTFILEACCESS:
                case REJECTFILEACCESS:
                case DATASETCREATED:
                case DATASETMENTIONED:
                    userNotification.setTheObject(datasetService.find(userNotification.getObjectId()));
                    break;

                case CREATEDS:
                case SUBMITTEDDS:
                case PUBLISHEDDS:
                case PUBLISHFAILED_PIDREG:
                case RETURNEDDS:
                case WORKFLOW_SUCCESS:
                case WORKFLOW_FAILURE:
                case PIDRECONCILED:
                case STATUSUPDATED:
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

                case GLOBUSUPLOADCOMPLETED:
                case GLOBUSUPLOADCOMPLETEDWITHERRORS:
                case GLOBUSDOWNLOADCOMPLETED:
                case GLOBUSDOWNLOADCOMPLETEDWITHERRORS:
                case GLOBUSUPLOADREMOTEFAILURE:
                case GLOBUSUPLOADLOCALFAILURE: 
                    userNotification.setTheObject(datasetService.find(userNotification.getObjectId()));
                    break;

                case CHECKSUMIMPORT:
                    userNotification.setTheObject(datasetVersionService.find(userNotification.getObjectId()));
                    break;

                case APIGENERATED:
                    userNotification.setTheObject(userNotification.getUser());
                    break;

                case INGESTCOMPLETED:
                case INGESTCOMPLETEDWITHERRORS:
                    userNotification.setTheObject(datasetService.find(userNotification.getObjectId()));
                    break;
            }

            userNotification.setDisplayAsRead(userNotification.isReadNotification());
            if (userNotification.isReadNotification() == false) {
                userNotification.setReadNotification(true);
                // consider switching to userNotificationService.markAsRead
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

    
    public boolean showVerifyEmailButton() {
        return !confirmEmailService.hasVerifiedEmail(currentUser);
    }
    
    public boolean isEmailIsVerified() {
        return confirmEmailService.hasVerifiedEmail(currentUser);
    }

    public boolean isEmailNotVerified() {
        return !confirmEmailService.hasVerifiedEmail(currentUser);
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

    public void setNotificationsList(List<UserNotification> notificationsList) {
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
        return AuthUtil.isNonLocalSignupEnabled(authenticationService.getAuthenticationProviders(), systemConfig);
    }

    public String getReasonForReturn(DatasetVersion datasetVersion) {
        // TODO: implement me! See getReasonsForReturn in api/Notifications.java
        return "";
    }

    public String getPasswordRequirements() {
        return passwordValidatorService.getGoodPasswordDescription(passwordErrors);
    }
    
    public String getRequestorName(UserNotification notification) {
        if(notification == null) return BundleUtil.getStringFromBundle("notification.email.info.unavailable");
        if(notification.getRequestor() == null) return BundleUtil.getStringFromBundle("notification.email.info.unavailable");;
        return (notification.getRequestor().getLastName() != null && notification.getRequestor().getLastName() != null) ? notification.getRequestor().getFirstName() + " " + notification.getRequestor().getLastName() : BundleUtil.getStringFromBundle("notification.email.info.unavailable");
    }
    
    public String getRequestorEmail(UserNotification notification) {
        if(notification == null) return BundleUtil.getStringFromBundle("notification.email.info.unavailable");;
        if(notification.getRequestor() == null) return BundleUtil.getStringFromBundle("notification.email.info.unavailable");;
        return notification.getRequestor().getEmail() != null ? notification.getRequestor().getEmail() : BundleUtil.getStringFromBundle("notification.email.info.unavailable");
    }

    public List<Type> getNotificationTypeList() {
        return notificationTypeList;
    }

    public void setNotificationTypeList(List<Type> notificationTypeList) {
        this.notificationTypeList = notificationTypeList;
    }

    public Set<Type> getToReceiveEmails() {
        return notificationTypeList.stream().filter(
            x -> isDisabled(x) ? !settingsWrapper.isAlwaysMuted(x) && settingsWrapper.isNeverMuted(x) : !mutedEmails.contains(x)
        ).collect(Collectors.toSet());
    }

    public void setToReceiveEmails(Set<Type> toReceiveEmails) {
        this.mutedEmails = notificationTypeList.stream().filter(
            x -> !isDisabled(x) && !toReceiveEmails.contains(x)
        ).collect(Collectors.toSet());
    }

    public Set<Type> getToReceiveNotifications() {
        return notificationTypeList.stream().filter(
            x -> isDisabled(x) ? !settingsWrapper.isAlwaysMuted(x) && settingsWrapper.isNeverMuted(x) : !mutedNotifications.contains(x)
        ).collect(Collectors.toSet());
    }

    public void setToReceiveNotifications(Set<Type> toReceiveNotifications) {
        this.mutedNotifications = notificationTypeList.stream().filter(
            x -> !isDisabled(x) && !toReceiveNotifications.contains(x) 
        ).collect(Collectors.toSet());
    }
    
    public boolean isDisabled(Type t) {
        return disabledNotifications.contains(t);
    }

    public boolean isOrcidEnabled() {
        return authenticationService.getOrcidAuthenticationProvider() != null;
    }

    public void startOrcidAuthentication() {
        OrcidOAuth2AP orcidProvider = authenticationService.getOrcidAuthenticationProvider();

        if (orcidProvider == null) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("auth.orcid.notConfigured"));
            return;
        }

        try {
            // Use the appropriate method to get the authorization URL
            String state = oauth2LoginBackingBean.createState(orcidProvider, toOption(accountInfoTab));
            String authorizationUrl = orcidProvider.buildAuthzUrl(state,
                    systemConfig.getDataverseSiteUrl() + "/oauth2/orcidConfirm.xhtml");
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            externalContext.redirect(authorizationUrl);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error starting ORCID authentication", ex);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("auth.orcid.error"));
        }
    }

    public void removeOrcid() {
        currentUser.setAuthenticatedOrcid(null);
        userService.save(currentUser);
    }

    public String getOrcidForDisplay() {
        if (currentUser == null || currentUser.getAuthenticatedOrcid() == null) {
            return "";
        }
        String orcidUrl = currentUser.getAuthenticatedOrcid();
        int index = orcidUrl.lastIndexOf('/');
        if (index > 0) {
            return orcidUrl.substring(index + 1);
        } else {
            return orcidUrl;
        }
    }

}
