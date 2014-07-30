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
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;
import org.hibernate.validator.constraints.NotBlank;
import java.util.ResourceBundle;

/**
 *
 * @author xyang
 */
@ViewScoped
@Named("LoginPage")
public class LoginPage implements java.io.Serializable {
    
    public enum EditMode {LOGIN, SUCCESS, FAILED};
    
    @Inject DataverseSession session;    
    
    @EJB
    DataverseUserServiceBean dataverseUserService;
    
    ResourceBundle rBundle=ResourceBundle.getBundle("LoginBundle");
    
    @NotBlank(message = "{enterUsernameMsg}")    
    private String userName;

    @NotBlank(message = "{enterPasswdMsg}")    
    private String password;
    
    public void init() {
        
        // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Log In", " - Log in to continue."));  
        
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

    /*
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
    */
    
    public boolean validatePassword(String username, String password) {
        DataverseUser user = dataverseUserService.findByUserName(userName);
        String encryptedPassword = PasswordEncryption.getInstance().encrypt(password);
        return encryptedPassword.equals(user.getEncryptedPassword());
    }

    public String login() {
        DataverseUser user = dataverseUserService.findByUserName(userName);
        if (user == null || !validatePassword(userName, password)) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,rBundle.getString("loginFailedSummary"), rBundle.getString("checkUsernameOrPasswdDetail")));
            return null;
        } else {
            session.setUser(user);
            return "/dataverse.xhtml?faces-redirect=true";
            
        }
    }
}
