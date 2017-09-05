/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.xml.html.HtmlPrinter;
import java.util.ArrayList;
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

//    /**
//     * Test of getCharacterRules method, of class PasswordValidatorUtil.
//     */
//    @Test
//    public void testGetCharacterRules() {
//        System.out.println("getCharacterRules");
//        String configString = "";
//        List<CharacterRule> expResult = null;
//        List<CharacterRule> result = PasswordValidatorUtil.getCharacterRules(configString);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getCharacterRulesDefault method, of class PasswordValidatorUtil.
//     */
//    @Test
//    public void testGetCharacterRulesDefault() {
//        System.out.println("getCharacterRulesDefault");
//        List<CharacterRule> expResult = null;
//        List<CharacterRule> result = PasswordValidatorUtil.getCharacterRulesDefault();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getCharacterRulesHarvardLevel3 method, of class PasswordValidatorUtil.
//     */
//    @Test
//    public void testGetCharacterRulesHarvardLevel3() {
//        System.out.println("getCharacterRulesHarvardLevel3");
//        List<CharacterRule> expResult = null;
//        List<CharacterRule> result = PasswordValidatorUtil.getCharacterRulesHarvardLevel3();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getCharacterRules4dot0 method, of class PasswordValidatorUtil.
//     */
//    @Test
//    public void testGetCharacterRules4dot0() {
//        System.out.println("getCharacterRules4dot0");
//        List<CharacterRule> expResult = null;
//        List<CharacterRule> result = PasswordValidatorUtil.getCharacterRules4dot0();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of getPasswordRequirements method, of class PasswordValidatorUtil.
     */
    @Test
    public void testGetPasswordRequirements() {
        System.out.println("getPasswordRequirements");
        int minLength = 6;
        int maxLength = 0;
        List<CharacterRule> characterRules = PasswordValidatorUtil.getCharacterRules4dot0();
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
        List<CharacterRule> characterRules = PasswordValidatorUtil.getCharacterRules4dot0();
        String reqChars = PasswordValidatorUtil.getRequiredCharacters(characterRules);
        System.out.println("Character rules string for 4.0: ");
        System.out.println(reqChars);
        assertEquals("letter, numeric", reqChars);
        
        characterRules = PasswordValidatorUtil.getCharacterRulesDefault();
        reqChars = PasswordValidatorUtil.getRequiredCharacters(characterRules);
        System.out.println("Character rules string for default, same as 4.0: ");
        System.out.println(reqChars);
        assertEquals("letter, numeric", reqChars);
          
        String characterRulesConfigString = "UpperCase:1,LowerCase:1,Digit:1,Special:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqChars = PasswordValidatorUtil.getRequiredCharacters(characterRules);
        System.out.println("Character rules string for '" + characterRulesConfigString + "': ");
        System.out.println(reqChars);
        assertEquals("uppercase, lowercase, numeric, special", reqChars);
        
        characterRulesConfigString = "UpperCase:1,LowerCase:1,Digit:1,Special:1,Alphabetical:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqChars = PasswordValidatorUtil.getRequiredCharacters(characterRules);
        System.out.println("Character rules string for '" + characterRulesConfigString + "', letter should not be mentioned: ");
        System.out.println(reqChars);
        assertEquals("uppercase, lowercase, numeric, special", reqChars);
        
        characterRulesConfigString = "Digit:1";
        characterRules = PasswordValidatorUtil.getCharacterRules(characterRulesConfigString);
        reqChars = PasswordValidatorUtil.getRequiredCharacters(characterRules);
        System.out.println("Character rules string for '" + characterRulesConfigString + "': ");
        System.out.println(reqChars);
        assertEquals("numeric", reqChars);
        
        //one for the unknown
        //one for only one
    }

    
//    /**
//     * Test of getRequiredCharacters method, of class PasswordValidatorUtil.
//     */
//    @Test
//    public void testGetRequiredCharacters() {
//        System.out.println("getRequiredCharacters");
//        List<CharacterRule> characterRules = null;
//        String expResult = "";
//        String result = PasswordValidatorUtil.getRequiredCharacters(characterRules);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
