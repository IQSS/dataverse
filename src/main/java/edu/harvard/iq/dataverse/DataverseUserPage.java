/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.util.ArrayList;
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
import java.util.ResourceBundle;

/**
 *
 * @author xyang
 */
@ViewScoped
@Named("DataverseUserPage")
public class DataverseUserPage implements java.io.Serializable {

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
    DataverseUserServiceBean dataverseUserService;

    private DataverseUser dataverseUser;
    private EditMode editMode;

    @NotBlank(message = "{enterPasswdMsg}")
    private String inputPassword;

    @NotBlank(message = "{enterPasswdMsg}")
    private String currentPassword;
    private Long dataverseId;
    private String permissionType;
    private List dataIdList;
    private List<UserNotification> notificationsList;
    private int activeIndex;
    private String selectTab = "somedata";
    
    ResourceBundle rBundle=ResourceBundle.getBundle("DataverseUserBundle");

    public DataverseUser getDataverseUser() {
        if (dataverseUser == null) {
            dataverseUser = new DataverseUser();
        }
        return dataverseUser;
    }

    public void setDataverseUser(DataverseUser dataverseUser) {
        this.dataverseUser = dataverseUser;
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

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public List getDataIdList() {
        return dataIdList;
    }

    public void setDataIdList(List dataIdList) {
        this.dataIdList = dataIdList;
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
        if (dataverseUser == null) {
            dataverseUser = (session.getUser().isGuest() ? new DataverseUser() : session.getUser());
        }
        notificationsList = userNotificationService.findByUser(dataverseUser.getId());
        permissionType = "writeAccess";
        dataIdList = new ArrayList();
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

    public void edit(ActionEvent e) {
        editMode = EditMode.EDIT;
    }

    public void create(ActionEvent e) {
        editMode = EditMode.CREATE;
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
        DataverseUser user = dataverseUserService.findByUserName(userName);
        if (editMode == EditMode.CREATE) {
            if (user != null) {
                userNameFound = true;
            }
        } else {
            if (user != null && !user.getId().equals(dataverseUser.getId())) {
                userNameFound = true;
            }
        }
        if (userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(rBundle.getString("usernameIsTakenMsg"));
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void validateUserNameEmail(FacesContext context, UIComponent toValidate, Object value) {
        String userName = (String) value;
        boolean userNameFound = false;
        DataverseUser user = dataverseUserService.findByUserName(userName);
        if (user != null) {
            userNameFound = true;
        } else {
            DataverseUser user2 = dataverseUserService.findByEmail(userName);
            if (user2 != null) {
                userNameFound = true;
            }
        }
        if (!userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(rBundle.getString("usernameOrEmailisIncorrectMsg"));
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void validatePassword(FacesContext context, UIComponent toValidate, Object value) {
        String password = (String) value;
        String encryptedPassword = PasswordEncryption.getInstance().encrypt(password);
        if (!encryptedPassword.equals(dataverseUser.getEncryptedPassword())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(rBundle.getString("passwdIsIncorrectMsg"));
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void updatePassword(String userName) {
        String plainTextPassword = PasswordEncryption.generateRandomPassword();
        DataverseUser user = dataverseUserService.findByUserName(userName);
        if (user == null) {
            user = dataverseUserService.findByEmail(userName);
        }
        user.setEncryptedPassword(PasswordEncryption.getInstance().encrypt(plainTextPassword));
        dataverseUserService.save(user);
    }

    public String save() {
        if (editMode == EditMode.CREATE || editMode == EditMode.CHANGE) {
            if (inputPassword != null) {
                dataverseUser.setEncryptedPassword(dataverseUserService.encryptPassword(inputPassword));
            }
        }
        dataverseUser = dataverseUserService.save(dataverseUser);
        userNotificationService.sendNotification(dataverseUser, new Timestamp(new Date().getTime()), UserNotification.Type.CREATEACC, null);

        if (editMode == EditMode.CREATE) {
            session.setUser(dataverseUser);
            return "/dataverse.xhtml?faces-redirect=true;";
        }

        editMode = null;
        return null;
    }

    public String cancel() {
        if (editMode == EditMode.CREATE) {
            return "/dataverse.xhtml?faces-redirect=true;";
        }

        editMode = null;
        return null;
    }

    public void submit(ActionEvent e) {
        updatePassword(dataverseUser.getUserName());
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
