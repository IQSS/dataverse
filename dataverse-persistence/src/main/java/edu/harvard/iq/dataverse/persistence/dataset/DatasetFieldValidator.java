package edu.harvard.iq.dataverse.persistence.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.config.EMailValidator;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * @author gdurand
 */
public class DatasetFieldValidator implements ConstraintValidator<ValidateDatasetFieldType, DatasetField> {

    private static final Logger logger = Logger.getLogger(DatasetFieldValidator.class.getCanonicalName());

    @Override
    public void initialize(ValidateDatasetFieldType constraintAnnotation) {
    }

    @Override
    public boolean isValid(DatasetField value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation(); // we do this so we can have different messages depending on the different issue

        DatasetFieldType dsfType = value.getDatasetFieldType();
        FieldType fieldType = dsfType.getFieldType();
        //SEK Additional logic turns off validation for templates
        if (isTemplateDatasetField(value)) {
            return true;
        }
        if (StringUtils.isBlank(value.getValue()) && dsfType.isPrimitive() && isRequiredInDataverse(value)) {
            try {
                context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + BundleUtil.getStringFromBundle(
                        "isrequired")).addConstraintViolation();
            } catch (NullPointerException npe) {
                logger.log(Level.FINE, "Error occurred during validation", npe);
            }

            return false;
        }

        if (StringUtils.isBlank(value.getValue()) || StringUtils.equals(value.getValue(), DatasetField.NA_VALUE)) {
            return true;
        }

        if (fieldType.equals(FieldType.TEXT) && value.getDatasetFieldType().getValidationFormat() != null) {
            boolean valid = value.getValue().matches(value.getDatasetFieldType().getValidationFormat());
            if (!valid) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + BundleUtil.getStringFromBundle("isNotValidEntry")).addConstraintViolation();
                } catch (NullPointerException e) {
                    return false;
                }
                return false;
            }
        }

        if (fieldType.equals(FieldType.DATE)) {
            boolean valid = false;
            String testString = value.getValue();

            if (!valid) {
                valid = isValidDate(testString, "yyyy-MM-dd");
            }
            if (!valid) {
                valid = isValidDate(testString, "yyyy-MM");
            }

            //If AD must be a 4 digit year
            if (value.getValue().contains("AD")) {
                testString = (testString.substring(0, testString.indexOf("AD"))).trim();
            }

            String YYYYformat = "yyyy";
            if (!valid) {
                valid = isValidDate(testString, YYYYformat);
                if (!StringUtils.isNumeric(testString)) {
                    valid = false;
                }
            }

            //If BC must be numeric
            if (!valid && value.getValue().contains("BC")) {
                testString = (testString.substring(0, testString.indexOf("BC"))).trim();
                if (StringUtils.isNumeric(testString)) {
                    valid = true;
                }
            }

            // Validate Bracket entries
            // Must start with "[", end with "?]" and not start with "[-"
            if (!valid && value.getValue().startsWith("[") && value.getValue().endsWith("?]") && !value.getValue().startsWith("[-")) {
                testString = value.getValue().replace("[", " ").replace("?]", " ").replace("-", " ").replace("BC", " ").replace("AD", " ").trim();
                if (value.getValue().contains("BC") && StringUtils.isNumeric(testString)) {
                    valid = true;
                } else {
                    valid = isValidDate(testString, YYYYformat);
                    if (!StringUtils.isNumeric(testString)) {
                        valid = false;
                    }
                }
            }

            if (!valid) {
                // TODO:
                // This is a temporary fix for the early beta!
                // (to accommodate dates with time stamps from Astronomy files)
                // As a real fix, we need to introduce a different type -
                // "datetime" for ex. and use it for timestamps;
                // We do NOT want users to be able to enter a full time stamp
                // as the release date...
                // -- L.A. 4.0 beta

                valid = (isValidDate(value.getValue(), "yyyy-MM-dd'T'HH:mm:ss") || isValidDate(value.getValue(), "yyyy-MM-dd'T'HH:mm:ss.SSS") || isValidDate(value.getValue(), "yyyy-MM-dd HH:mm:ss"));

            }
            if (!valid) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + BundleUtil.getStringFromBundle("isNotValidDate", Lists.newArrayList(YYYYformat))).addConstraintViolation();
                } catch (NullPointerException npe) {
                    logger.log(Level.FINE, "Error occurred during validation", npe);
                }

                return false;
            }
        }

        if (fieldType.equals(FieldType.FLOAT)) {
            try {
                Double.parseDouble(value.getValue());
            } catch (NumberFormatException e) {
                logger.fine("Float value failed validation: " + value.getValue() + " (" + dsfType.getDisplayName() + ")");
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + BundleUtil.getStringFromBundle("isNotValidNumber")).addConstraintViolation();
                } catch (NullPointerException npe) {
                    logger.log(Level.FINE, "Error occurred during validation", npe);
                }

                return false;
            }
        }

        if (fieldType.equals(FieldType.INT)) {
            try {
                Integer.parseInt(value.getValue());
            } catch (NumberFormatException e) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + BundleUtil.getStringFromBundle("isNotValidInteger")).addConstraintViolation();
                } catch (NullPointerException npe) {
                    logger.log(Level.FINE, "Error occurred during validation", npe);
                }

                return false;
            }
        }
        // Note, length validation for FieldType.TEXT was removed to accommodate migrated data that is greater than 255 chars.

        if (fieldType.equals(FieldType.URL)) {
            try {
                URL url = new URL(value.getValue());
            } catch (MalformedURLException e) {
                try {
                    context.buildConstraintViolationWithTemplate(
                            dsfType.getDisplayName() + " " + value.getValue() + " " +
                                    BundleUtil.getStringFromBundle("isNotValidUrl")).addConstraintViolation();
                } catch (NullPointerException npe) {
                    logger.log(Level.FINE, "Error occurred during validation", npe);
                }

                return false;
            }
        }

        if (fieldType.equals(FieldType.EMAIL)) {
            return EMailValidator.isEmailValid(value.getValue(), context);

        }
        return true;
    }

    private boolean isTemplateDatasetField(DatasetField dsf) {
        return getTopParentDatasetField(dsf).getTemplate() != null;
    }

    private DatasetField getTopParentDatasetField(DatasetField dsf) {
        return dsf.getDatasetFieldParent()
                .map(this::getTopParentDatasetField)
                .getOrElse(dsf);
    }

    private boolean isValidDate(String dateString, String pattern) {
        boolean valid = true;
        Date date;
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setLenient(false);
        try {
            dateString = dateString.trim();
            date = sdf.parse(dateString);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            int era = calendar.get(Calendar.ERA);
            if (era == GregorianCalendar.AD) {
                if (year > 9999) {
                    valid = false;
                }
            }
        } catch (ParseException e) {
            valid = false;
        }
        if (dateString.length() > pattern.length()) {
            valid = false;
        }
        return valid;
    }

    public boolean isValidAuthorIdentifier(String userInput, Pattern pattern) {
        return pattern.matcher(userInput).matches();
    }
    
    private boolean isRequiredInDataverse(DatasetField field) {
      
      DatasetFieldType fieldType = field.getDatasetFieldType();
      if (fieldType.isRequired()) {
          return true;
      }
      
      Dataverse dv = getDataverse(field).getMetadataBlockRootDataverse();
      
      return dv.getDataverseFieldTypeInputLevels()
              .stream()
              .filter(inputLevel -> inputLevel.getDatasetFieldType().equals(field.getDatasetFieldType()))
              .map(inputLevel -> inputLevel.isRequired())
              .findFirst()
              .orElse(false);
    }

    private Dataverse getDataverse(DatasetField field) {
        return getTopParentDatasetField(field)
                .getDatasetVersion()
                .getDataset()
                .getOwner();
    }
}
