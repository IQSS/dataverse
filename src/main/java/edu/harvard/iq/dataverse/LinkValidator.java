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
import edu.harvard.iq.dataverse.util.BundleUtil;

@FacesValidator(value = "linkValidator")
public class LinkValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        UIInput taglineInput = (UIInput) component.getAttributes().get("taglineInput");
        UIInput linkUrlInput = (UIInput) component.getAttributes().get("linkUrlInput");

        String taglineStr = (String) taglineInput.getSubmittedValue();
        String urlStr = (String) linkUrlInput.getSubmittedValue();

        FacesMessage msg = null;
        if (taglineStr.isEmpty() && !urlStr.isEmpty()) {
            msg = new FacesMessage(BundleUtil.getStringFromBundle("link.tagline.validate"));
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            FacesContext.getCurrentInstance().addMessage(taglineInput.getClientId(), msg);
        }

        if (msg != null) {
            throw new ValidatorException(msg);
        }

    }

}
