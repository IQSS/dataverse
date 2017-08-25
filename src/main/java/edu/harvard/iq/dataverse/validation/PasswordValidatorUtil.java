package edu.harvard.iq.dataverse.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

public class PasswordValidatorUtil {

    private static final Logger logger = Logger.getLogger(PasswordValidatorUtil.class.getCanonicalName());

    // TODO: Work on switching ILLEGAL_MATCH to something like TOO_MANY_DIGITS.
    public enum ErrorType {
        TOO_SHORT, INSUFFICIENT_CHARACTERISTICS, ILLEGAL_MATCH
    };

    public static List<CharacterRule> getCharacterRules(String configString) {
        logger.info("configString: " + configString);
        List<CharacterRule> saneDefault = getCharacterRulesDefault();
        if (configString == null || configString.isEmpty()) {
            return saneDefault;
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
        String message = "Your password must contain:";
        message += "<ul>";
        String optionalGoodStrengthNote = "";
        if (goodStrength > 0) {
            optionalGoodStrengthNote = " (passwords of at least " + goodStrength + " characters are exempt from all other requirements)";
        }
        message += "<li " + getColor(errors, ErrorType.TOO_SHORT) + ">" + getOkOrFail(errors, ErrorType.TOO_SHORT) + "At least " + minLength + " characters" + optionalGoodStrengthNote + "</li>";
        message += "<li " + getColor(errors, ErrorType.INSUFFICIENT_CHARACTERISTICS) + ">" + getOkOrFail(errors, ErrorType.INSUFFICIENT_CHARACTERISTICS) + "At least " + numberOfCharacteristics + " of the following: " + getRequiredCharacters(characterRules) + "</li>";
        message += "</ul>";
        boolean repeatingDigitRuleEnabled = Integer.MAX_VALUE != numberOfConsecutiveDigitsAllowed;
        boolean showMayNotBlock = repeatingDigitRuleEnabled || dictionaryEnabled;
        if (showMayNotBlock) {
            message += "It may not include:";
            message += "<ul>";
        }
        if (repeatingDigitRuleEnabled) {
            message += "<li " + getColor(errors, ErrorType.ILLEGAL_MATCH) + ">" + getOkOrFail(errors, ErrorType.ILLEGAL_MATCH) + "More than " + numberOfConsecutiveDigitsAllowed + " numbers in a row</li>";
        }
        if (dictionaryEnabled) {
            message += "<li>Dictionary words or common acronyms of 5 or more letters</li>";
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

    // FIXME: Figure out how to pull "a letter", for example, out of a CharacterRule.
    public static String getRequiredCharacters(List<CharacterRule> characterRules) {
        switch (characterRules.size()) {
            case 2:
                return "a letter and a number";
            case 4:
                return "uppercase, lowercase, numeric, or special characters";
            default:
                return "UNKNOWN";
        }
    }

}
