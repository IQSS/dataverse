/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

    //private String fieldType;
    public void initialize(ValidateDatasetFieldType constraintAnnotation) {
        //this.fieldType = constraintAnnotation.value();
    }

    public boolean isValid(DatasetFieldValue value, ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation(); // we do this so we can have different messages depending on the different issue

        boolean lengthOnly = false;
        
        DatasetFieldType dsfType = value.getDatasetField().getDatasetFieldType();
        String fieldType = dsfType.getFieldType();
        
        if (value.getDatasetField().getTemplate() != null){
            lengthOnly = true;
        }
        
        if (value.getDatasetField().getParentDatasetFieldCompoundValue() != null 
                && value.getDatasetField().getParentDatasetFieldCompoundValue().getParentDatasetField().getTemplate() != null ){
            lengthOnly = true;
        }
        
        if (StringUtils.isBlank(value.getValue())) {
            return true;
        }
        
        if (fieldType.equals("date") && !lengthOnly) {
            boolean valid = false;
            if (!valid) {  
                valid = isValidDate(value.getValue(), "yyyy-MM-dd");
            }
            if (!valid) {
                valid = isValidDate(value.getValue(), "yyyy-MM");
            }
            String YYYYformat = "yyyy";
            if (!valid ) {
                valid = isValidDate(value.getValue(), YYYYformat);
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
            if (!valid ) {
                context.buildConstraintViolationWithTemplate(" " + dsfType.getDisplayName() + " is not a valid date. \""+ YYYYformat + "\" is a supported format.").addConstraintViolation();
                return false;
            }
        } 
        
        if (fieldType.equals("float") && !lengthOnly) {
            try {
                Double.parseDouble(value.getValue());
            } catch (Exception e) {
                context.buildConstraintViolationWithTemplate(" " + dsfType.getDisplayName() + " is not a valid number.").addConstraintViolation();
                return false;
            }
        }
        
        if (fieldType.equals("int") && !lengthOnly) {
            try {
                Integer.parseInt(value.getValue());
            } catch (Exception e) {
                context.buildConstraintViolationWithTemplate(" " + dsfType.getDisplayName() + " is not a valid integer.").addConstraintViolation();
                return false;
            }
        }
        
        if (fieldType.equals("text")  && value.getValue().length() > 255) {
                 context.buildConstraintViolationWithTemplate(" " + dsfType.getDisplayName() + " may not be more than 255 characters.").addConstraintViolation(); 
                 return false;
        }
        
        if (fieldType.equals("textbox")  && value.getValue().length() > 1000) {
                 context.buildConstraintViolationWithTemplate(" " + dsfType.getDisplayName() + " may not be more than 1000 characters.").addConstraintViolation(); 
                 return false;
        }
        if (fieldType.equals("url") && !lengthOnly) {
            try {
                URL url = new URL(value.getValue());
            } catch (MalformedURLException e) {
                context.buildConstraintViolationWithTemplate(" " + dsfType.getDisplayName() + " is not a valid URL.").addConstraintViolation();
                return false;
            }
        }

        if (fieldType.equals("email") && !lengthOnly) {
            //Pattern p =  Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
            //updated to allow dashes
            Pattern p =  Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
            
            Matcher m = p.matcher(value.getValue());
            boolean matchFound = m.matches();
            if (!matchFound) {
                context.buildConstraintViolationWithTemplate(" " + dsfType.getDisplayName() + " is not a valid email address.").addConstraintViolation();
                return false;
            }
        }
               
        return true;
    }
    
        private boolean isValidDate(String dateString, String pattern) {
        boolean valid=true;
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
            if (era == GregorianCalendar.AD ) {
                if ( year > 9999) {
                    valid=false;
                }
            }
        }catch (ParseException e) {
            valid=false;
        }
        if (dateString.length()>pattern.length()) {
            valid=false;
        }
        return valid;
    }
        

}
