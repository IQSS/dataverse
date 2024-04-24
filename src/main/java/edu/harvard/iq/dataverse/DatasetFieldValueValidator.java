/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.validation.EMailValidator;
import edu.harvard.iq.dataverse.validation.URLValidator;
import org.apache.commons.lang3.StringUtils;

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

        // verify no junk in individual fields and values are within range
        if (dsfType.getName() != null && (dsfType.getName().equals(DatasetFieldConstant.northLatitude) || dsfType.getName().equals(DatasetFieldConstant.southLatitude) ||
                dsfType.getName().equals(DatasetFieldConstant.westLongitude) || dsfType.getName().equals(DatasetFieldConstant.eastLongitude))) {
            try {
                verifyBoundingBoxCoordinatesWithinRange(dsfType.getName(), value.getValue());
            } catch (IllegalArgumentException iae) {
                    try {
                        context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + "  " + BundleUtil.getStringFromBundle("dataset.metadata.invalidEntry")).addConstraintViolation();
                    } catch (NullPointerException e) {
                    }
                    return false;
            }
        }

        // validate fields that are siblings and depend on each others values
        if (value.getDatasetField().getParentDatasetFieldCompoundValue() != null &&
                value.getDatasetField().getParentDatasetFieldCompoundValue().getParentDatasetField().getValidationMessage() == null) {
            Optional<String> failureMessage = validateChildConstraints(value.getDatasetField());
            if (failureMessage.isPresent()) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getParentDatasetFieldType().getDisplayName() +  "  " +
                            BundleUtil.getStringFromBundle(failureMessage.get()) ).addConstraintViolation();

                    // save the failure message in the parent so we don't keep validating the children
                    value.getDatasetField().getParentDatasetFieldCompoundValue().getParentDatasetField().setValidationMessage(failureMessage.get());

                } catch (NullPointerException npe) {
                }
                return false;
            }
        }

        if (fieldType.equals(FieldType.TEXT) && !lengthOnly && value.getDatasetField().getDatasetFieldType().getValidationFormat() != null) {
            boolean valid = value.getValue().matches(value.getDatasetField().getDatasetFieldType().getValidationFormat());
            if (!valid) {
                try {
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + "  " + BundleUtil.getStringFromBundle("dataset.metadata.invalidEntry")).addConstraintViolation();
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
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() +  "  " + BundleUtil.getStringFromBundle("dataset.metadata.invalidDate")  ).addConstraintViolation();
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
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() +  "  " + BundleUtil.getStringFromBundle("dataset.metadata.invalidNumber") ).addConstraintViolation();
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
                    context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() +  "  " + BundleUtil.getStringFromBundle("dataset.metadata.invalidInteger")  ).addConstraintViolation();
                } catch (NullPointerException npe) {

                }

                return false;
            }
        }
        // Note, length validation for FieldType.TEXT was removed to accommodate migrated data that is greater than 255 chars.

        if (fieldType.equals(FieldType.URL) && !lengthOnly) {
            boolean isValidUrl = URLValidator.isURLValid(value.getValue());
            if (!isValidUrl) {
                context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + value.getValue() +  "  " + BundleUtil.getStringFromBundle("dataset.metadata.invalidURL")).addConstraintViolation();
                return false;
            }
        }

        if (fieldType.equals(FieldType.EMAIL) && !lengthOnly) {
            boolean isValidMail = EMailValidator.isEmailValid(value.getValue());
            if (!isValidMail) {
                context.buildConstraintViolationWithTemplate(dsfType.getDisplayName() + " " + value.getValue() +  "  " + BundleUtil.getStringFromBundle("dataset.metadata.invalidEmail")).addConstraintViolation();
                return false;
            }
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

    public boolean isValidAuthorIdentifier(String userInput, Pattern pattern) {
        return pattern.matcher(userInput).matches();
    }

    // Validate child fields against each other and return failure message or Optional.empty() if success
    public Optional<String> validateChildConstraints(DatasetField dsf) {
        final String fieldName = dsf.getDatasetFieldType().getName() != null ? dsf.getDatasetFieldType().getName() : "";
        Optional<String> returnFailureMessage = Optional.empty();

        // Validate Child Constraint for Geospatial Bounding Box
        // validate the four points of the box to insure proper layout
        if (fieldName.equals(DatasetFieldConstant.northLatitude) || fieldName.equals(DatasetFieldConstant.westLongitude)
                || fieldName.equals(DatasetFieldConstant.eastLongitude) || fieldName.equals(DatasetFieldConstant.southLatitude)) {
            final String failureMessage = "dataset.metadata.invalidGeospatialCoordinates";

            try {
                final Map<String, String> coords = new HashMap<>();
                dsf.getParentDatasetFieldCompoundValue().getChildDatasetFields().forEach(f -> {
                        coords.put(f.getDatasetFieldType().getName(), f.getValue());
                });
                if (!validateBoundingBox(coords.get(DatasetFieldConstant.westLongitude),
                        coords.get(DatasetFieldConstant.eastLongitude),
                        coords.get(DatasetFieldConstant.northLatitude),
                        coords.get(DatasetFieldConstant.southLatitude))) {
                    returnFailureMessage = Optional.of(failureMessage);
                }
            } catch (IllegalArgumentException e) { // IllegalArgumentException NumberFormatException
                returnFailureMessage = Optional.of(failureMessage);
            }
        }

        return returnFailureMessage;
    }

    public static boolean validateBoundingBox(final String westLon, final String eastLon, final String northLat, final String southLat) {
        boolean returnVal = false;

        try {
            Float west = verifyBoundingBoxCoordinatesWithinRange(DatasetFieldConstant.westLongitude, westLon);
            Float east = verifyBoundingBoxCoordinatesWithinRange(DatasetFieldConstant.eastLongitude, eastLon);
            Float north = verifyBoundingBoxCoordinatesWithinRange(DatasetFieldConstant.northLatitude, northLat);
            Float south = verifyBoundingBoxCoordinatesWithinRange(DatasetFieldConstant.southLatitude, southLat);
            returnVal = west <= east && south <= north;
        } catch (IllegalArgumentException e) {
            returnVal = false;
        }

        return returnVal;
    }

    private static Float verifyBoundingBoxCoordinatesWithinRange(final String name, final String value) throws IllegalArgumentException {
        int max = name.equals(DatasetFieldConstant.westLongitude) || name.equals(DatasetFieldConstant.eastLongitude) ? 180 : 90;
        int min = max * -1;

        final Float returnVal = value != null ? Float.parseFloat(value) : Float.NaN;
        if (returnVal.isNaN() || returnVal < min || returnVal > max) {
            throw new IllegalArgumentException(String.format("Value (%s) not in range (%s-%s)", returnVal.isNaN() ? "missing" : returnVal, min, max));
        }
        return returnVal;
    }
}
