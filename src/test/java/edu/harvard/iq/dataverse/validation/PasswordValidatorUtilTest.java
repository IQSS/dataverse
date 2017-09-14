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
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

/**
 *
 * @author pdurbin
 */
public class PasswordValidatorUtilTest {

    public PasswordValidatorUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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

    @Test
    public void testGetRequiredCharacters() {
        int numberOfCharacteristics = 2; //influences use of # or "each" in text generation
        String textBeforeList = BundleUtil.getStringFromBundle("passwdVal.passwdReq.characteristicsReq" , Arrays.asList(Integer.toString(numberOfCharacteristics)))+ " " ;
        List<CharacterRule> characterRules = PasswordValidatorUtil.getCharacterRulesDefault();
        String reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for default: ");
        System.out.println(reqString);
        assertEquals("At least 1 character from each of the following types: letter, numeral", reqString);
          
        String characterRulesConfigString = "UpperCase:1,LowerCase:1,Digit:1,Special:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString + "': ");
        System.out.println(reqString);
        assertEquals("At least 1 character from 2 of the following types: uppercase, lowercase, numeral, special", reqString);
        
        numberOfCharacteristics = 4;
        characterRulesConfigString = "UpperCase:1,LowerCase:1,Digit:1,Special:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString + "': ");
        System.out.println(reqString);
        assertEquals("At least 1 character from each of the following types: uppercase, lowercase, numeral, special", reqString);
        
        numberOfCharacteristics = 4;
        characterRulesConfigString = "UpperCase:1,LowerCase:1,Digit:1,Special:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString );
        System.out.println(reqString);
        assertEquals("At least 1 character from each of the following types: uppercase, lowercase, numeral, special", reqString);
        
        numberOfCharacteristics = 2;
        characterRulesConfigString = "UpperCase:1,LowerCase:1,Digit:1,Special:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString );
        System.out.println(reqString);
        assertEquals("At least 1 character from 2 of the following types: uppercase, lowercase, numeral, special", reqString);
        
        numberOfCharacteristics = 2; //Should say each, even if more characteristics set than possible
        characterRulesConfigString = "Digit:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString + "': ");
        System.out.println(reqString);
        assertEquals("At least 1 character from each of the following types: numeral", reqString);
        
        characterRulesConfigString = "Digit:2";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString + "': ");
        System.out.println(reqString);
        assertEquals("Fufill 2: At least 2 numeral characters", reqString);
        
        characterRulesConfigString = "LowerCase:1,Digit:2,Special:3";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString + "': ");
        System.out.println(reqString);
        assertEquals("Fufill 2: At least 1 lowercase characters, 2 numeral characters, 3 special characters", reqString);
        
        numberOfCharacteristics = 2;
        characterRulesConfigString = "UpperCase:1,LowerCase:1,Digit:1,Special:1,Alphabetical:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqString = PasswordValidatorUtil.getRequiredCharacters(characterRules,numberOfCharacteristics);
        System.out.println("Character rules string for '" + characterRulesConfigString + "', letter is mentioned even though that configuration is discouraged: ");
        System.out.println(reqString);
        assertEquals("At least 1 character from 2 of the following types: uppercase, lowercase, letter, numeral, special", reqString);
    }
}
