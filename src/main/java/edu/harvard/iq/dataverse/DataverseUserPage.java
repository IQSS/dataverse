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

    @EJB
    DataverseUserServiceBean dataverseUserService;

    private DataverseUser dataverseUser = new DataverseUser();
    private boolean editMode = false;
    
    @NotBlank(message = "Please enter a password for your account.")    
    private String inputPassword;
    
    public DataverseUser getDataverseUser() {
        return dataverseUser;
    }

    public void setDataverseUser(DataverseUser dataverseUser) {
        this.dataverseUser = dataverseUser;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public String getInputPassword() {
        return inputPassword;
    }

    public void setInputPassword(String inputPassword) {
        this.inputPassword = inputPassword;
    }
    
    public void init() {
        editMode = true;;
    }
    public void edit(ActionEvent e) {
        editMode = true;
    }
    
    public void validateUserName(FacesContext context, UIComponent toValidate, Object value) {
        String userName = (String) value;
        DataverseUser user = dataverseUserService.findByUserName(userName);
        if (user!=null) {
            ((UIInput)toValidate).setValid(false);
            FacesMessage message = new FacesMessage("This Username is already taken.");
            context.addMessage(toValidate.getClientId(context), message);
        }
    }
    
    public void save(ActionEvent e) {
        if (inputPassword!=null) {
            dataverseUser.setEncryptedPassword(dataverseUserService.encryptPassword(inputPassword));
        }
        dataverseUser = dataverseUserService.save(dataverseUser);
        editMode = false;
    }

    public void cancel(ActionEvent e) {
        init();
        editMode = true;
    }
}