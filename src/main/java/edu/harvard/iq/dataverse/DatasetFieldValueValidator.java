/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
public class DatasetFieldValueValidator implements ConstraintValidator<ValidateDatasetFieldType, DatasetFieldValue> {
    private static final Logger logger = Logger.getLogger(DatasetFieldValueValidator.class.getCanonicalName());

    //private String fieldType;
    public void initialize(ValidateDatasetFieldType constraintAnnotation) {
        //this.fieldType = constraintAnnotation.value();
    }

    public boolean isValid(DatasetFieldValue value, ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation(); // we do this so we can have different messages depending on the different issue

        boolean lengthOnly = false;

        DatasetFieldType dsfType = value.getDatasetField().getDatasetFieldType();
        FieldType fieldType = dsfType.getFieldType();

        if (value.getDatasetField().getTemplate() != null) {
            lengthOnly = true;
        }

        if (value.getDatasetField().getParentDatasetFieldCompoundValue() != null
                && value.getDatasetField().getParentDatasetFieldCompoundValue().getParentDatasetField().getTemplate() != null) {
            lengthOnly = true;
        }

        if (StringUtils.isBlank(value.getValue()) || StringUtils.equals(value.getValue(), DatasetField.NA_VALUE)) {
            return true;
        }

        if (fieldType.equals(FieldType.TEXT) && !lengthOnly && value.getDatasetField().getDatasetFieldType().getValidationFormat() != null) {
            boolean valid = value.getValue().matches(value.getDatasetField().getDatasetFieldType().getValidationFormat());
            if (!valid) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is not a valid entry.").addConstraintViolation();
                } catch (NullPointerException e) {
                    return false;
                }
                return false;
            }
        }

        if (fieldType.equals(FieldType.DATE) && !lengthOnly) {
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
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is not a valid date. \"" + YYYYformat + "\" is a supported format.").addConstraintViolation();
                } catch (NullPointerException npe) {

                }

                return false;
            }
        }

        if (fieldType.equals(FieldType.FLOAT) && !lengthOnly) {
            try {
                Double.parseDouble(value.getValue());
            } catch (Exception e) {
                logger.fine("Float value failed validation: " + value.getValue() + " (" + dsfType.getDisplayName() + ")");
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is not a valid number.").addConstraintViolation();
                } catch (NullPointerException npe) {

                }

                return false;
            }
        }

        if (fieldType.equals(FieldType.INT) && !lengthOnly) {
            try {
                Integer.parseInt(value.getValue());
            } catch (Exception e) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " is not a valid integer.").addConstraintViolation();
                } catch (NullPointerException npe) {

                }

                return false;
            }
        }
        // Note, length validation for FieldType.TEXT was removed to accommodate migrated data that is greater than 255 chars.

        if (fieldType.equals(FieldType.URL) && !lengthOnly) {
            try {
                URL url = new URL(value.getValue());
            } catch (MalformedURLException e) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + value.getValue() + "  is not a valid URL.").addConstraintViolation();
                } catch (NullPointerException npe) {

                }

                return false;
            }
        }

        if (fieldType.equals(FieldType.EMAIL) && !lengthOnly) {
            if(value.getDatasetField().isRequired() && value.getValue()==null){
                return false;
            }
            
            return EMailValidator.isEmailValid(value.getValue(), context);

        }

        return true;
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

    public boolean isValidAuthorIdentifierOrcid(String userInput) {
        // https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
        String validRegex = "^\\d{4}-\\d{4}-\\d{4}-(\\d{4}|\\d{3}X)$";
        Pattern pattern = Pattern.compile(validRegex);
        Matcher matcher = pattern.matcher(userInput);
        return matcher.matches();
    }

    public boolean isValidAuthorIdentifierIsni(String userInput) {
        String validRegex = "^\\d*$";
        Pattern pattern = Pattern.compile(validRegex);
        Matcher matcher = pattern.matcher(userInput);
        return matcher.matches();
    }

    public boolean isValidAuthorIdentifierLcna(String userInput) {
        String validRegex = "^[a-z]+\\d+$";
        Pattern pattern = Pattern.compile(validRegex);
        Matcher matcher = pattern.matcher(userInput);
        return matcher.matches();
    }

    public boolean isValidAuthorIdentifierViaf(String userInput) {
        String validRegex = "^\\d*$";
        Pattern pattern = Pattern.compile(validRegex);
        Matcher matcher = pattern.matcher(userInput);
        return matcher.matches();
    }

    public boolean isValidAuthorIdentifierGnd(String userInput) {
        // GND regex from https://www.wikidata.org/wiki/Property:P227
        String validRegex = "^1[01]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X]$";
        Pattern pattern = Pattern.compile(validRegex);
        Matcher matcher = pattern.matcher(userInput);
        return matcher.matches();
    }

}
