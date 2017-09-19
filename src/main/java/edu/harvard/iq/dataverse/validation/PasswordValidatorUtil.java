package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

public class PasswordValidatorUtil {

    private static final Logger logger = Logger.getLogger(PasswordValidatorUtil.class.getCanonicalName());

    // TODO: Work on switching ILLEGAL_MATCH to something like TOO_MANY_DIGITS.
    public enum ErrorType {
        TOO_SHORT, INSUFFICIENT_CHARACTERISTICS, ILLEGAL_MATCH, ILLEGAL_WORD
    };

    public static List<CharacterRule> getCharacterRules(String configString) {
        if (configString == null || configString.isEmpty()) {
            return getCharacterRulesDefault(); //sane default
        } else {
            List<CharacterRule> rules = parseConfigString(configString);
            return rules;
        }
    }

    /**
     * The default out-of-the-box character rules for Dataverse.
     */
    public static List<CharacterRule> getCharacterRulesDefault() {
        List<CharacterRule> characterRules = new ArrayList<>();
        characterRules.add(new CharacterRule(EnglishCharacterData.Alphabetical, 1));
        characterRules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        return characterRules;
    }

    /**
     * Parses the list of character rules as defined in the database. Recall how
     * configString is formatted: "UpperCase:1,LowerCase:1,Digit:1,Special:1"
     */
    public static List<CharacterRule> parseConfigString(String configString) {
        List<CharacterRule> characterRules = new ArrayList<>();
        String[] typePlusNums = configString.split(",");
        for (String typePlusNum : typePlusNums) {
            String[] configArray = typePlusNum.split(":");
            String type = configArray[0];
            String num = configArray[1];
            EnglishCharacterData typeData = EnglishCharacterData.valueOf(type);
            characterRules.add(new CharacterRule(typeData, new Integer(num)));
        }
        return characterRules;
    }

    //TODO: Relocate this messaging to the bundle and refactor passwordreset.xhtml to use it accordingly.
    public static String getPasswordRequirements(int minLength, int maxLength, List<CharacterRule> characterRules, int numberOfCharacteristics, int numberOfConsecutiveDigitsAllowed, int goodStrength, boolean dictionaryEnabled, List<String> errors) {
        logger.fine(errors.toString());
        String message = BundleUtil.getStringFromBundle("passwdVal.passwdReq.title");
        message += "<ul>";
        String optionalGoodStrengthNote = "";
        if (goodStrength > 0) { 
            optionalGoodStrengthNote = " (" + BundleUtil.getStringFromBundle("passwdVal.passwdReq.goodStrength" , Arrays.asList(Integer.toString(goodStrength))) +")";
        }
        message += "<li " + getColor(errors, ErrorType.TOO_SHORT) + ">" + getOkOrFail(errors, ErrorType.TOO_SHORT) +  BundleUtil.getStringFromBundle("passwdVal.passwdReq.lengthReq" , Arrays.asList(Integer.toString(minLength))) + " " + optionalGoodStrengthNote+ "</li>";
        message += "<li " + getColor(errors, ErrorType.INSUFFICIENT_CHARACTERISTICS) + ">" + getOkOrFail(errors, ErrorType.INSUFFICIENT_CHARACTERISTICS) +  getRequiredCharacters(characterRules, numberOfCharacteristics) + "</li>"; 
        message += "</ul>";
        boolean repeatingDigitRuleEnabled = Integer.MAX_VALUE != numberOfConsecutiveDigitsAllowed;
        boolean showMayNotBlock = repeatingDigitRuleEnabled || dictionaryEnabled;
        if (showMayNotBlock) {
            message += BundleUtil.getStringFromBundle("passwdVal.passwdReq.notInclude");
            message += "<ul>";
        }
        if (repeatingDigitRuleEnabled) {
            message += "<li " + getColor(errors, ErrorType.ILLEGAL_MATCH) + ">" + getOkOrFail(errors, ErrorType.ILLEGAL_MATCH) + BundleUtil.getStringFromBundle("passwdVal.passwdReq.consecutiveDigits" , Arrays.asList(Integer.toString(numberOfConsecutiveDigitsAllowed))) + "</li>";
        }
        if (dictionaryEnabled) {
            message += "<li " + getColor(errors, ErrorType.ILLEGAL_WORD) + ">" + getOkOrFail(errors, ErrorType.ILLEGAL_WORD) + BundleUtil.getStringFromBundle("passwdVal.passwdReq.dictionaryWords")+"</li>";
        }
        if (showMayNotBlock) {
            message += "</ul>";
        }
        return message;
    }

    private static String getOkOrFail(List<String> errors, ErrorType errorState) {
        if (errors.isEmpty()) {
            return "";
        }

        if (errors.contains(errorState.toString())) {
            String fail = "<span class=\"glyphicon glyphicon-ban-circle\" style=\"color:#a94442\"/> ";
            return fail;
        } else {
            String ok = "<span class=\"glyphicon glyphicon-ok\" style=\"color:#3c763d\"/> ";
            return ok;
        }
    }

    // TODO: Consider deleting this method if no one wants it.
    @Deprecated
    private static String getColor(List<String> errors, ErrorType errorState) {
        if (errors.isEmpty()) {
            return "";
        }
        if (errors.contains(errorState.toString())) {
            String red = "style=\"color:red;\"";
            return "";
        } else {
            String green = "style=\"color:green;\"";
            return "";
        }
    }

    
    /**
     * Creates the string for stating the character requirements. 
     */
    // This method especially does not support character rules from other languages
    // Also, this method is a bit klugey because passay 1.1.0 does not allow us to get the name of the character rule.
    public static String getRequiredCharacters(List<CharacterRule> characterRules, int numberOfCharacteristics) {
        
        //how many of each character class
        int lowercase = 0;
        int uppercase = 0;
        int digit = 0;
        int alphabetical = 0; //if upper or lower > 0, alphabetical is irrelevant
        int special = 0;
        
        for(CharacterRule c : characterRules) {

            String validChars = c.getValidCharacters();
            if(validChars.equals(EnglishCharacterData.LowerCase.getCharacters())) {
                lowercase = c.getNumberOfCharacters();
            } else if(validChars.equals(EnglishCharacterData.UpperCase.getCharacters())) {
                uppercase = c.getNumberOfCharacters();
            } else if(validChars.equals(EnglishCharacterData.Digit.getCharacters())) {
                digit = c.getNumberOfCharacters();
            } else if(validChars.equals(EnglishCharacterData.Alphabetical.getCharacters())) {
                alphabetical = c.getNumberOfCharacters();
            } else if(validChars.equals(EnglishCharacterData.Special.getCharacters())) {
                special = c.getNumberOfCharacters();
            } else {
                //other rules should cause an error before here, but just in case
                return BundleUtil.getStringFromBundle("passwdVal.passwdReq.unknownPasswordRule");
            }
        }
        
        //these below method strings are not in the bundle as this whole method is based in English
        String returnString = "";
        
        if(lowercase <= 1 && uppercase <= 1 && digit <= 1 && alphabetical <= 1 && special <= 1) {
            returnString = ((uppercase == 1) ? "uppercase" : "") 
                    + ((lowercase == 1) ? ", lowercase" : "") 
                    + ((alphabetical == 1 ) ? ", letter" : "") 
                    + ((digit == 1) ? ", numeral" : "") 
                    + ((special == 1) ? ", special" : "");
            

            
            String eachOrSomeCharacteristics = ((characterRules.size()) > numberOfCharacteristics ) ? Integer.toString(numberOfCharacteristics) : "each";
            return BundleUtil.getStringFromBundle("passwdVal.passwdReq.characteristicsReq" , Arrays.asList(eachOrSomeCharacteristics)) 
                    + " " + StringUtils.strip(returnString, " ,");
        } else {
            //if requiring multiple of any character type, we use a different string format
            //this could be made to look nicer, but we don't expect this to be utilized
            returnString = "Fufill " + numberOfCharacteristics + ": At least " + ((uppercase > 0) ? uppercase + " uppercase characters, " : "")
                    + ((lowercase > 0) ? lowercase + " lowercase characters, " : "")
                    + ((alphabetical > 0 ) ? " letter characters, " : "") 
                    + ((digit > 0) ? digit + " numeral characters, " : "")
                    + ((special > 0) ? special + " special characters, " : ""); //then strip
            return StringUtils.strip(returnString, " ,");
        }
    }

}
