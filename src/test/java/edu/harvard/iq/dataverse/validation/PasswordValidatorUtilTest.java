/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.xml.html.HtmlPrinter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

/**
 *
 * @author pdurbin
 */
public class PasswordValidatorUtilTest {

    static class PasswordValidatorUtilNoParamTest {
        /**
         * Test of getPasswordRequirements method, of class PasswordValidatorUtil.
         */
        @Test
        public void testGetPasswordRequirements() {
            System.out.println("getPasswordRequirements");
            int minLength = 6;
            int maxLength = 0;
            List<CharacterRule> characterRules = PasswordValidatorUtil.getCharacterRulesDefault();
            int numberOfCharacteristics = 2;
            int numberOfRepeatingCharactersAllowed = 4;
            int goodStrength = 21;
            boolean dictionaryEnabled = true;
            List<String> errors = new ArrayList<>();
            System.out.println("---Show all");
            String req1 = PasswordValidatorUtil.getPasswordRequirements(minLength, maxLength, characterRules, numberOfCharacteristics, numberOfRepeatingCharactersAllowed, goodStrength, dictionaryEnabled, errors);
            System.out.println(HtmlPrinter.prettyPrint(req1));
            System.out.println("---Hide all");
            String req2 = PasswordValidatorUtil.getPasswordRequirements(minLength, maxLength, characterRules, numberOfCharacteristics, 0, 0, false, errors);
            System.out.println(HtmlPrinter.prettyPrint(req2));
            System.out.println("---Show may not include sequence");
            String req3 = PasswordValidatorUtil.getPasswordRequirements(minLength, maxLength, characterRules, numberOfCharacteristics, numberOfRepeatingCharactersAllowed, goodStrength, false, errors);
            System.out.println(HtmlPrinter.prettyPrint(req3));
            System.out.println("---Show may not dictionary");
            String req4 = PasswordValidatorUtil.getPasswordRequirements(minLength, maxLength, characterRules, numberOfCharacteristics, 0, goodStrength, true, errors);
            System.out.println(HtmlPrinter.prettyPrint(req4));
        }
        
        /**
         * Test of parseConfigString method, of class PasswordValidatorUtil.
         */
        @Test
        public void testParseConfigString() {
            String configString = "UpperCase:1,LowerCase:4,Digit:1,Special:1";
            List<CharacterRule> rules = PasswordValidatorUtil.parseConfigString(configString);

            System.out.println("Uppercase valid chars: " + rules.get(0).getValidCharacters());
            System.out.println("Lowercase valid chars: " + rules.get(1).getValidCharacters());
            System.out.println("Special valid chars: " + rules.get(3).getValidCharacters());

            assertEquals(4, rules.size());
            assertEquals(EnglishCharacterData.UpperCase.getCharacters(), rules.get(0).getValidCharacters());
            assertEquals(EnglishCharacterData.LowerCase.getCharacters(), rules.get(1).getValidCharacters());
            assertEquals(EnglishCharacterData.Digit.getCharacters(), rules.get(2).getValidCharacters());
            assertEquals(EnglishCharacterData.Special.getCharacters(), rules.get(3).getValidCharacters());
        }

    }

    static Stream<Arguments> configurations() {
        return Stream.of(
            Arguments.of(2, null,
                "At least 1 character from each of the following types: letter, numeral"),
            Arguments.of(2, "UpperCase:1,LowerCase:1,Digit:1,Special:1",
                "At least 1 character from 2 of the following types: uppercase, lowercase, numeral, special"),
            Arguments.of(4, "UpperCase:1,LowerCase:1,Digit:1,Special:1",
                "At least 1 character from each of the following types: uppercase, lowercase, numeral, special"),
            // Should say each, even if more characteristics set than possible
            Arguments.of(2, "Digit:1", "At least 1 character from each of the following types: numeral"),
            Arguments.of(2, "Digit:2", "Fufill 2: At least 2 numeral characters"),
            Arguments.of(2, "LowerCase:1,Digit:2,Special:3",
                "Fufill 2: At least 1 lowercase characters, 2 numeral characters, 3 special characters"),
            // letter is mentioned even though that configuration is discouraged
            Arguments.of(2, "UpperCase:1,LowerCase:1,Digit:1,Special:1,Alphabetical:1",
                "At least 1 character from 2 of the following types: uppercase, lowercase, letter, numeral, special")
        );
    }
    @ParameterizedTest
    @MethodSource("configurations")
    void testGetRequiredCharacters(int numberOfCharacteristics, String characterRulesConfigString, String expectedValue) {
        List<CharacterRule> characterRules;
        String message = "Character rules string for ";
        if (characterRulesConfigString != null) {
            characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
            message += characterRulesConfigString;
        } else {
            characterRules = PasswordValidatorUtil.getCharacterRulesDefault();
            message += "default";
        }
        
        String reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules, numberOfCharacteristics);
        assertEquals(expectedValue, reqString, message + ": " + reqString);
    }
}
