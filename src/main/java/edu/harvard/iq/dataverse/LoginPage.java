/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author xyang
 */
@ViewScoped
@Named("LoginPage")
public class LoginPage implements java.io.Serializable {
    
    public enum EditMode {LOGIN, SUCCESS, FAILED};
    
    @EJB
    DataverseUserServiceBean dataverseUserService;
    
    private EditMode editMode;

    @NotBlank(message = "Please enter a username.")    
    private String userName;

    @NotBlank(message = "Please enter a password.")    
    private String password;
    
    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
        
    public void init() {
        editMode = EditMode.LOGIN;
    }
    
    public void validateUserName(FacesContext context, UIComponent toValidate, Object value) {
        String uName = (String) value;
        boolean userNameFound = false;
        DataverseUser user = dataverseUserService.findByUserName(uName);
        if (user!=null) {
            userNameFound = true;
        }
        if (!userNameFound) {
            ((UIInput)toValidate).setValid(false);
            FacesMessage message = new FacesMessage("Username is incorrect.");
            context.addMessage(toValidate.getClientId(context), message);
        }
    }    
    
    public boolean validatePassword(String username, String password) {
        DataverseUser user = dataverseUserService.findByUserName(userName);
        String encryptedPassword = PasswordEncryption.getInstance().encrypt(password);
        return encryptedPassword.equals(user.getEncryptedPassword());
    }

    public void login() {
        DataverseUser user = dataverseUserService.findByUserName(userName);
        if (user == null || !validatePassword(userName, password)) {
        editMode = EditMode.FAILED;
        } else {
        editMode = EditMode.SUCCESS;
        }
    }
}
