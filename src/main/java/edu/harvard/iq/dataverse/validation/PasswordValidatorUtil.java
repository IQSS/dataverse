package edu.harvard.iq.dataverse.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

public class PasswordValidatorUtil {

    private static final Logger logger = Logger.getLogger(PasswordValidatorUtil.class.getCanonicalName());

    public static List<CharacterRule> getCharacterRules(String configString) {
        logger.info("configString: " + configString);
        List<CharacterRule> saneDefault = getCharacterRulesDefault();
        if (configString == null || configString.isEmpty()) {
            return saneDefault;
        }
//        // FIXME: Actually parse this "Alphabetical:1,Digit:1" string or decide if there's a better way to express the old rules since Dataverse 4.0.
//        if ("Alphabetical:1,Digit:1".equals(configString)) {
//            return getCharacterRules4dot0();
//        }
        // FIXME: Actualy parse "UpperCase:1,LowerCase:1,Digit:1,Special:1" so the numbers can be increased, etc.
        if ("UpperCase:1,LowerCase:1,Digit:1,Special:1".equals(configString)) {
            return getCharacterRulesHarvardLevel3();
        }
        return saneDefault;
    }

    /**
     * The default out-of-the-box character rules for Dataverse.
     */
    public static List<CharacterRule> getCharacterRulesDefault() {
        return getCharacterRules4dot0();
    }

    /**
     * These are the character rules that Harvard's security policy requires for
     * level 3 data if you have fewer than 20 characters.
     */
    public static List<CharacterRule> getCharacterRulesHarvardLevel3() {
        List<CharacterRule> characterRules = new ArrayList<>();
        characterRules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        characterRules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        characterRules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        characterRules.add(new CharacterRule(EnglishCharacterData.Special, 1));
        return characterRules;
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

}
