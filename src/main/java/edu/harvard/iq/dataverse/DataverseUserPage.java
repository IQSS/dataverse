/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author xyang
 */
@ViewScoped
@Named("DataverseUserPage")
public class DataverseUserPage implements java.io.Serializable {
    
    public enum EditMode {CREATE, INFO, EDIT, CHANGE};

    @EJB
    DataverseUserServiceBean dataverseUserService;

    private DataverseUser dataverseUser = new DataverseUser();
    private EditMode editMode;
    
    @NotBlank(message = "Please enter a password for your account.")    
    private String inputPassword;
    
    @NotBlank(message = "Please enter a password for your account.")    
    private String currentPassword;
        
    public DataverseUser getDataverseUser() {
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

    public void init() {
        if (dataverseUser.getId() != null) {  
            dataverseUser = dataverseUserService.find(dataverseUser.getId());
        } else { 
            try {
                dataverseUser = dataverseUserService.findDataverseUser();
            } catch (EJBException e) {
            }
        }
        editMode = EditMode.INFO;
    }
    
    public void edit(ActionEvent e) {
        editMode = EditMode.EDIT;
    }

    public void create(ActionEvent e) {
        dataverseUser.setId( Long.parseLong(String.valueOf(0)));
        editMode = EditMode.CREATE;
    }

    public void changePassword(ActionEvent e) {
        editMode = EditMode.CHANGE;
    }

    public void validateUserName(FacesContext context, UIComponent toValidate, Object value) {
        String userName = (String) value;
        boolean userNameFound = false;
        DataverseUser user = dataverseUserService.findByUserName(userName);
        if (editMode == EditMode.CREATE) {
            if (user!=null) {
                userNameFound = true;
            }
        } else {
            if (user!=null && !user.getId().equals(dataverseUser.getId())) {
                userNameFound = true;
            }
        }
        if (userNameFound) {
            ((UIInput)toValidate).setValid(false);
            FacesMessage message = new FacesMessage("This Username is already taken.");
            context.addMessage(toValidate.getClientId(context), message);
        }
    }
    
    public void validatePassword(FacesContext context, UIComponent toValidate, Object value) {
        String password = (String) value;
        String encryptedPassword = PasswordEncryption.getInstance().encrypt(password);
        if (!encryptedPassword.equals(dataverseUser.getEncryptedPassword())) {
            ((UIInput)toValidate).setValid(false);
            FacesMessage message = new FacesMessage("Password is incorrect.");
            context.addMessage(toValidate.getClientId(context), message);        
        }    
    }

    public void save(ActionEvent e) {
        if (editMode == EditMode.CREATE|| editMode == EditMode.CHANGE) {
            if (inputPassword!=null) {
                dataverseUser.setEncryptedPassword(dataverseUserService.encryptPassword(inputPassword));
            }
        }
        dataverseUser = dataverseUserService.save(dataverseUser); 
        editMode = EditMode.INFO;
    }

    public void cancel(ActionEvent e) {
        editMode = EditMode.INFO;
    }
}