package edu.harvard.iq.dataverse.license.validation;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * Class responsible for validating if all information's regarding license are correct.
 */
@FacesValidator("licenseValidator")
public class LicenseErrorHandler implements Validator {

    private UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});

    // -------------------- LOGIC --------------------

    /**
     * Checks if license is valid.
     */
    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {

        if (!urlValidator.isValid(value.toString())) {

            FacesMessage errorMessage = new FacesMessage(StringUtils.EMPTY,
                    BundleUtil.getStringFromBundle("dashboard.license.invalidURL"));
            errorMessage.setSeverity(FacesMessage.SEVERITY_ERROR);

            throw new ValidatorException(errorMessage);
        }
    }
}
