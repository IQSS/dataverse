/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;


 @FacesValidator(value = "linkValidator")
public class LinkValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        UIInput linkTextInput = (UIInput) component.getAttributes().get("linkTextInput");
        UIInput linkUrlInput = (UIInput) component.getAttributes().get("linkUrlInput");

        String textStr = (String) linkTextInput.getSubmittedValue();
        String urlStr = (String) linkUrlInput.getSubmittedValue();

        if (textStr.isEmpty() && urlStr.isEmpty()) {
             return;
         }
         FacesMessage msg = null;
         if (textStr.isEmpty()) {
              msg = new FacesMessage("Link Text is required for Link Url.");
             msg.setSeverity(FacesMessage.SEVERITY_ERROR);
           FacesContext.getCurrentInstance().addMessage(linkTextInput.getClientId(), msg);
           }
         if (urlStr.isEmpty()) {
            msg = new FacesMessage("Url is required for link text.");
             msg.setSeverity(FacesMessage.SEVERITY_ERROR);
             FacesContext.getCurrentInstance().addMessage(linkUrlInput.getClientId(), msg);
            
         }
         if (msg!=null) {
            throw new ValidatorException(msg);
         }

     }
}
