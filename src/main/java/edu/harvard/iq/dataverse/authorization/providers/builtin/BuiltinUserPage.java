/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PasswordEncryption;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.hibernate.validator.constraints.NotBlank;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author xyang
 */
@ViewScoped
@Named("DataverseUserPage")
public class BuiltinUserPage implements java.io.Serializable {

    public enum EditMode {

        CREATE, EDIT, CHANGE, FORGOT
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
    PermissionServiceBean permissionService;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    
    @EJB
    AuthenticationServiceBean authSvc;

    private AuthenticatedUser currentUser;
    private BuiltinUser builtinUser;    
    private EditMode editMode;

    @NotBlank(message = "Please enter a password for your account.")
    private String inputPassword;

    @NotBlank(message = "Please enter a password for your account.")
    private String currentPassword;
    private Long dataverseId;
    private List<UserNotification> notificationsList;
    private int activeIndex;
    private String selectTab = "somedata";


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

    public void init() {
        if (editMode == EditMode.CREATE) { //create mode is for sign up
            builtinUser = new BuiltinUser();
        } else {
            if ( session.getUser().isAuthenticated() ) {
                currentUser = (AuthenticatedUser) session.getUser();
                notificationsList = userNotificationService.findByUser(((AuthenticatedUser)currentUser).getId());
                if (currentUser.isBuiltInUser()) {
                    builtinUser =  builtinUserService.findByUserName(currentUser.getUserIdentifier());
                }
            } else {
                notificationsList = Collections.<UserNotification>emptyList();
            }

            switch (selectTab) {
                case "notifications":
                    activeIndex = 1;
                    displayNotification();
                    break;
                default:
                    activeIndex = 0;
                    break;
            }
        }
    }

    public void edit(ActionEvent e) {
        editMode = EditMode.EDIT;
    }

    public void changePassword(ActionEvent e) {
        editMode = EditMode.CHANGE;
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
            FacesMessage message = new FacesMessage("This Username is already taken.");
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

    public void validatePassword(FacesContext context, UIComponent toValidate, Object value) {
        String password = (String) value;
        String encryptedPassword = PasswordEncryption.getInstance().encrypt(password);
        if (!encryptedPassword.equals(builtinUser.getEncryptedPassword())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage("Password is incorrect.");
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void updatePassword(String userName) {
        String plainTextPassword = PasswordEncryption.generateRandomPassword();
        BuiltinUser user = builtinUserService.findByUserName(userName);
        if (user == null) {
            user = builtinUserService.findByEmail(userName);
        }
        user.setEncryptedPassword(PasswordEncryption.getInstance().encrypt(plainTextPassword));
        builtinUserService.save(user);
    }

    public String save() {
        if (editMode == EditMode.CREATE || editMode == EditMode.CHANGE) {
            if (inputPassword != null) {
                builtinUser.setEncryptedPassword(builtinUserService.encryptPassword(inputPassword));
            }
        }
        builtinUser = builtinUserService.save(builtinUser);

        if (editMode == EditMode.CREATE) {
            AuthenticatedUser au = authSvc.createAuthenticatedUser(BuiltinAuthenticationProvider.PROVIDER_ID, builtinUser.getUserName(), builtinUser.createDisplayInfo());
            session.setUser(au);
            userNotificationService.sendNotification(au,
                                                     new Timestamp(new Date().getTime()), 
                                                     UserNotification.Type.CREATEACC, null);
            return "/dataverse.xhtml?faces-redirect=true;";
        } else {
            authSvc.updateAuthenticatedUser(currentUser, builtinUser.createDisplayInfo());
            editMode = null;
            return null;            
        }
    }

    public String cancel() {
        if (editMode == EditMode.CREATE) {
            return "/dataverse.xhtml?faces-redirect=true;";
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
    }

    public void displayNotification() {
        for (UserNotification userNotification : notificationsList) {
            userNotification.setDisplayAsRead(userNotification.isReadNotification());
            if (userNotification.isReadNotification() == false) {
                userNotification.setReadNotification(true);
                userNotificationService.save(userNotification);
            }
        }
    }
}
