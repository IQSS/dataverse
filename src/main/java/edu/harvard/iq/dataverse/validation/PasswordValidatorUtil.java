package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
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
        return getCharacterRules4dot0();
    }

    /**
     * These are the character rules Dataverse 4.0 shipped with.
     */
    public static List<CharacterRule> getCharacterRules4dot0() {
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
        logger.info(errors.toString());
        String message = BundleUtil.getStringFromBundle("passwdVal.passwdReq.title");//ResourceBundle.getBundle("Bundle").getString("passwdVal.passwdReq.title");
    //DELETE   //BundleUtil.getStringFromBundle("notification.email.update.maplayer", rootDvNameAsList);
        message += "<ul>";
        String optionalGoodStrengthNote = "";
        if (goodStrength > 0) {  //+ goodStrength + passwords of at least {0} characters are exempt from all other requirements
            optionalGoodStrengthNote = " (" + BundleUtil.getStringFromBundle("passwdVal.passwdReq.goodStrength" , Arrays.asList(Integer.toString(goodStrength))) +")";
        }
        message += "<li " + getColor(errors, ErrorType.TOO_SHORT) + ">" + getOkOrFail(errors, ErrorType.TOO_SHORT) +  BundleUtil.getStringFromBundle("passwdVal.passwdReq.lengthReq" , Arrays.asList(Integer.toString(minLength))) + " " + optionalGoodStrengthNote+ "</li>";//"At least " + minLength + " characters" + optionalGoodStrengthNote + "</li>";
        message += "<li " + getColor(errors, ErrorType.INSUFFICIENT_CHARACTERISTICS) + ">" + getOkOrFail(errors, ErrorType.INSUFFICIENT_CHARACTERISTICS) + BundleUtil.getStringFromBundle("passwdVal.passwdReq.characteristicsReq" , Arrays.asList(Integer.toString(numberOfCharacteristics))) + " " + getRequiredCharacters(characterRules) + "</li>";//"At least " + numberOfCharacteristics + " of the following: " + 
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
            message += "<li " + getColor(errors, ErrorType.ILLEGAL_WORD) + ">" + getOkOrFail(errors, ErrorType.ILLEGAL_WORD) + BundleUtil.getStringFromBundle("passwdVal.passwdReq.dictionaryWords")+"</li>";//"Dictionary words</li>";
        }
        if (showMayNotBlock) {
            message += "</ul>";
        }
        // for debugging
//        message += errors.toString();
        return message;
    }

    private static String getOkOrFail(List<String> errors, ErrorType errorState) {
        if (errors.isEmpty()) {
            return "";
        }
        // FIXME: Figure out how to put these icons on the screen nicely.
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

    // This method especially does not support character rules from other languages
    // Also, this method is a bit klugey because passay 1.1.0 does not allow us to get the name of the character rule.
    public static String getRequiredCharacters(List<CharacterRule> characterRules) {
        
        boolean lowerCase = false;
        boolean upperCase = false;
        boolean digit = false;
        boolean alphabetical = false; //if upper or lower, alphabetical is irrelevant
        boolean special = false;
        
        for(CharacterRule c : characterRules) {
            
            String validChars = c.getValidCharacters();
            if(validChars.equals(EnglishCharacterData.LowerCase.getCharacters())) {
                lowerCase = true;
            } else if(validChars.equals(EnglishCharacterData.UpperCase.getCharacters())) {
                upperCase = true;
            } else if(validChars.equals(EnglishCharacterData.Digit.getCharacters())) {
                digit = true;
            } else if(validChars.equals(EnglishCharacterData.Alphabetical.getCharacters())) {
                alphabetical = true;
            } else if(validChars.equals(EnglishCharacterData.Special.getCharacters())) {
                special = true;
            } else {
                //other rules should cause an error before here, but just in case
                return BundleUtil.getStringFromBundle("passwdVal.passwdReq.unknownPasswordRule");
            }
        }
        
        //these strings are not in the bundle as this whole method is based in English
        String returnString = ((upperCase) ? "uppercase" : "") 
                + ((lowerCase) ? ", lowercase" : "") 
                + ((alphabetical && !(lowerCase || upperCase)) ? ", letter" : "") 
                + ((digit) ? ", numeric" : "") 
                + ((special) ? ", special" : "");
        return StringUtils.strip(returnString, " ,");
    }

}
//
