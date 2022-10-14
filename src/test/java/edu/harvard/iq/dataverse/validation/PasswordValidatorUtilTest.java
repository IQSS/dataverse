/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.xml.html.HtmlPrinter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

/**
 *
 * @author pdurbin
 */
@RunWith(Enclosed.class)
public class PasswordValidatorUtilTest {

    public static class PasswordValidatorUtilNoParamTest {
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

    @RunWith(Parameterized.class)
    public static class PasswordValidatorUtilParamTest {

        // influences use of # or "each" in text generation
        @Parameter(0)
        public int numberOfCharacteristics;

        @Parameter(1)
        public String characterRulesConfigString;

        @Parameter(2)
        public String expectedValue;

        @Parameters
        public static Collection data() {
            return Arrays.asList(new Object[][] {
                {
                    2,
                    null,
                    "At least 1 character from each of the following types: letter, numeral"
                },
                {
                    2,
                    "UpperCase:1,LowerCase:1,Digit:1,Special:1",
                    "At least 1 character from 2 of the following types: uppercase, lowercase, numeral, special"
                },
                {
                    4,
                    "UpperCase:1,LowerCase:1,Digit:1,Special:1",
                    "At least 1 character from each of the following types: uppercase, lowercase, numeral, special"
                },

                // Should say each, even if more characteristics set than possible
                {
                    2,
                    "Digit:1",
                    "At least 1 character from each of the following types: numeral"
                },

                {
                    2,
                    "Digit:2",
                    "Fufill 2: At least 2 numeral characters"
                },
                {
                    2,
                    "LowerCase:1,Digit:2,Special:3",
                    "Fufill 2: At least 1 lowercase characters, 2 numeral characters, 3 special characters"
                },

                // letter is mentioned even though that configuration is discouraged
                {
                    2,
                    "UpperCase:1,LowerCase:1,Digit:1,Special:1,Alphabetical:1",
                    "At least 1 character from 2 of the following types: uppercase, lowercase, letter, numeral, special"
                }
            });
        }

        @Test
        public void testGetRequiredCharacters() {
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
            assertEquals(message + ": " + reqString, expectedValue, reqString);
        }
    }
}
