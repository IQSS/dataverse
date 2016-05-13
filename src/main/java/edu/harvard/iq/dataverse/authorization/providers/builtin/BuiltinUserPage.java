/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseHeaderFragment;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.UserNotification;
import static edu.harvard.iq.dataverse.UserNotification.Type.CREATEDV;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.mydata.MyDataPage;
import edu.harvard.iq.dataverse.passwordreset.PasswordValidator;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
 * @author xyang
 */
@ViewScoped
@Named("DataverseUserPage")
public class BuiltinUserPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(BuiltinUserPage.class.getCanonicalName());

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
    
    public EditMode getChangePasswordMode () {
        return EditMode.CHANGE_PASSWORD;
    }

    public AuthenticatedUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(AuthenticatedUser currentUser) {
        this.currentUser = currentUser;
    }

    public BuiltinUser getBuiltinUser() {
        return builtinUser;
    }

    public void setBuiltinUser(BuiltinUser builtinUser) {
        this.builtinUser = builtinUser;
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

    public String init() {

        // prevent creating a user if signup not allowed.
        boolean safeDefaultIfKeyNotFound = true;
        boolean signupAllowed = settingsWrapper.isTrueForKey(SettingsServiceBean.Key.AllowSignUp.toString(), safeDefaultIfKeyNotFound);
        logger.fine("signup is allowed: " + signupAllowed);

        if (editMode == EditMode.CREATE && !signupAllowed) {
            return "/403.xhtml";
        }

        if (editMode == EditMode.CREATE) {
            if (!session.getUser().isAuthenticated()) { // in create mode for new user
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("user.signup.tip"));
                builtinUser = new BuiltinUser();
                return "";
            } else {
                editMode = null; // we can't be in create mode for an existing user
            }
        }
        
        if ( session.getUser().isAuthenticated() ) {
            currentUser = (AuthenticatedUser) session.getUser();
            notificationsList = userNotificationService.findByUser(((AuthenticatedUser)currentUser).getId());
            if (currentUser.isBuiltInUser()) {
                builtinUser =  builtinUserService.findByUserName(currentUser.getUserIdentifier());
            }
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
        boolean userNameFound = false;
        BuiltinUser user = builtinUserService.findByUserName(userName);
        if (editMode == EditMode.CREATE) {
            if (user != null) {
                userNameFound = true;
            }
        } else {
            if (user != null && !user.getId().equals(builtinUser.getId())) {
                userNameFound = true;
            }
        }
        if (userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, JH.localize("user.username.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }
    
    public void validateUserEmail(FacesContext context, UIComponent toValidate, Object value) {
        String userEmail = (String) value;
        boolean userEmailFound = false;
        BuiltinUser user = builtinUserService.findByEmail(userEmail);
        AuthenticatedUser aUser = authenticationService.getAuthenticatedUserByEmail(userEmail);
        if (editMode == EditMode.CREATE) {
            if (user != null || aUser != null) {
                userEmailFound = true;
            }
        } else {
            //In edit mode...
            if (user != null || aUser != null){
                 userEmailFound = true;               
            }
            //if there's a match on edit make sure that the email belongs to the 
            // user doing the editing by checking ids
            if ((user != null && user.getId().equals(builtinUser.getId())) || (aUser!=null && aUser.getId().equals(builtinUser.getId()))){
                userEmailFound = false;
            }
        }
        if (userEmailFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, JH.localize("user.email.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }
    

    public void validateUserNameEmail(FacesContext context, UIComponent toValidate, Object value) {
        String userName = (String) value;
        boolean userNameFound = false;
        BuiltinUser user = builtinUserService.findByUserName(userName);
        if (user != null) {
            userNameFound = true;
        } else {
            BuiltinUser user2 = builtinUserService.findByEmail(userName);
            if (user2 != null) {
                userNameFound = true;
            }
        }
        if (!userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage("Username or Email is incorrect.");
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void validateCurrentPassword(FacesContext context, UIComponent toValidate, Object value) {
        
        String password = (String) value;
        
        if (StringUtils.isBlank(password)){
            logger.log(Level.WARNING, "current password is blank");
            
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Password Error", "Password is blank: re-type it again.");
            context.addMessage(toValidate.getClientId(context), message);
            return;
            
        } else {
            logger.log(Level.INFO, "current paswword is not blank");
        }
        
        
        
        if ( ! PasswordEncryption.getVersion(builtinUser.getPasswordEncryptionVersion()).check(password, builtinUser.getEncryptedPassword()) ) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Password Error", "Password is incorrect.");
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
            
        } else {
            logger.log(Level.INFO, "new paswword is not blank");
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


    public void updatePassword(String userName) {
        String plainTextPassword = PasswordEncryption.generateRandomPassword();
        BuiltinUser user = builtinUserService.findByUserName(userName);
        if (user == null) {
            user = builtinUserService.findByEmail(userName);
        }
        user.updateEncryptedPassword(PasswordEncryption.get().encrypt(plainTextPassword), PasswordEncryption.getLatestVersionNumber());
        builtinUserService.save(user);
    }

    public String save() {
        boolean passwordChanged = false;
        if (editMode == EditMode.CREATE || editMode == EditMode.CHANGE_PASSWORD) {
            if (inputPassword != null) {
                builtinUser.updateEncryptedPassword(PasswordEncryption.get().encrypt(inputPassword), PasswordEncryption.getLatestVersionNumber());
                passwordChanged = true;
            } else {
                // just defensive coding: for in case when the validator is not
                // working
                logger.log(Level.WARNING, "inputPassword is still null");
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, JH.localize("user.noPasswd"), null);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);
                return null;
            }
        }
        builtinUser = builtinUserService.save(builtinUser);

        if (editMode == EditMode.CREATE) {
            AuthenticatedUser au = authSvc.createAuthenticatedUser(
                    new UserRecordIdentifier(BuiltinAuthenticationProvider.PROVIDER_ID, builtinUser.getUserName()),
                    builtinUser.getUserName(), builtinUser.getDisplayInfo(), false);
            if ( au == null ) {
                // username exists
                getUsernameField().setValid(false);
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, JH.localize("user.username.taken"), null);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(getUsernameField().getClientId(context), message);
                return null;
            }
            session.setUser(au);
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
                Logger.getLogger(BuiltinUserPage.class.getName()).log(Level.SEVERE, null, ex);
                redirectPage = "dataverse.xhtml&alias=" + dataverseService.findRootDataverse().getAlias();
            }

            logger.log(Level.FINE, "Sending user to = {0}", redirectPage);

            return redirectPage + (!redirectPage.contains("?") ? "?" : "&") + "faces-redirect=true";            
            
            
        } else {
            authSvc.updateAuthenticatedUser(currentUser, builtinUser.getDisplayInfo());
            editMode = null;
            String msg = "Your account information has been successfully updated.";
            if (passwordChanged) {
                msg = "Your account password has been successfully changed.";
            }
            JsfHelper.addFlashMessage(msg);
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

    public void submit(ActionEvent e) {
        updatePassword(builtinUser.getUserName());
        editMode = null;
    }

    public String remove(Long notificationId) {
        UserNotification userNotification = userNotificationService.find(notificationId);
        userNotificationService.delete(userNotification);
        for (UserNotification uNotification : notificationsList) {
            if (uNotification.getId() == userNotification.getId()) {
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
            }

            userNotification.setDisplayAsRead(userNotification.isReadNotification());
            if (userNotification.isReadNotification() == false) {
                userNotification.setReadNotification(true);
                userNotificationService.save(userNotification);
            }
        }
    }
}
