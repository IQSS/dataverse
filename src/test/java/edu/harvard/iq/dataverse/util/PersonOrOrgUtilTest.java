package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.json.JsonObject;

@LocalJvmSettings
public class PersonOrOrgUtilTest {

        public PersonOrOrgUtilTest() {
        }
        
        @Test
        public void testOrganizationSimpleName() {
            verifyIsOrganization("IBM");
            verifyIsOrganization("Harvard University");
        }

        @Test
        public void testOrganizationCOMPLEXName() {
            verifyIsOrganization("The Institute for Quantitative Social Science");
            verifyIsOrganization("Council on Aging");
            verifyIsOrganization("The Ford Foundation");
            verifyIsOrganization("United Nations Economic and Social Commission for Asia and the Pacific (UNESCAP)");
            verifyIsOrganization("Michael J. Fox Foundation for Parkinson's Research");
            // The next examples are known to be asserted to be a Person without an entry in the OrgWordArray
            // So we test when no array is set via JvmSetting to verify the problem still exists in
            // the underlying algorithm
            JsonObject obj = PersonOrOrgUtil.getPersonOrOrganization("Portable Antiquities of the Netherlands", false, false);
            assertTrue(obj.getBoolean("isPerson"));
            JsonObject obj2 = PersonOrOrgUtil.getPersonOrOrganization("Max Mustermann GmbH", false, false);
            assertTrue(obj2.getBoolean("isPerson"));
        }

        @Test
        @JvmSetting(key = JvmSettings.ORG_PHRASE_ARRAY, value = "Portable,GmbH")
        public void testOrganizationWithOrgPhraseArray() {
            // The next examples are known to be asserted to be a Person without an entry in the OrgWordArray
            // So we test with the array set via JvmSetting to verify the array works
            verifyIsOrganization("Portable Antiquities of the Netherlands");
            verifyIsOrganization("Max Mustermann GmbH");
        }

        @Test
        public void testOrganizationAcademicName() {
            verifyIsOrganization("John Smith Center");
            verifyIsOrganization("John Smith Group");
            // An example the base algorithm doesn't handle:
            JsonObject obj = PersonOrOrgUtil.getPersonOrOrganization("John Smith Project", false, false);
            assertTrue(obj.getBoolean("isPerson"));
        }

        @Test
        @JvmSetting(key = JvmSettings.ASSUME_COMMA_IN_PERSON_NAME, value = "true")
        public void testOrganizationAcademicNameWithAssumeComma() {
            verifyIsOrganization("John Smith Center");
            verifyIsOrganization("John Smith Group");
            verifyIsOrganization("John Smith Project");
        }

        
        @Test
        public void testOrganizationCommaOrDash() {
            verifyIsOrganization("Digital Archive of Massachusetts Anti-Slavery and Anti-Segregation Petitions, Massachusetts Archives, Boston MA");
            verifyIsOrganization("U.S. Department of Commerce, Bureau of the Census, Geography Division");
            verifyIsOrganization("Harvard Map Collection, Harvard College Library");
            verifyIsOrganization("Geographic Data Technology, Inc. (GDT)");
        }

        @Disabled
        @Test
        public void testOrganizationES() {
            //Spanish recognition is not enabled - see export/Organization.java
            verifyIsOrganization("Compañía de San Fernando");
        }
        
        /**
         * Name is composed of:
         * <First Names> <Family Name>
         */
        @Test
        public void testName() {
            verifyIsPerson("Jorge Mario Bergoglio", "Jorge Mario", "Bergoglio");
            verifyIsPerson("Bergoglio", null, null);
            verifyIsPerson("Francesco Cadili", "Francesco", "Cadili");
            // This Philip Seymour Hoffman example is from ShibUtilTest.
            verifyIsPerson("Philip Seymour Hoffman", "Philip Seymour", "Hoffman");

            // test Smith (is also a name)
            verifyIsPerson("John Smith", "John", "Smith");
            // resolved using hint file
            verifyIsPerson("Guido van Rossum", "Guido", "van Rossum");
            // test only name
            verifyIsPerson("Francesco", "Francesco", null);
            // test only family name
            verifyIsPerson("Cadili", null, null);
            
            verifyIsPerson("kcjim11, kcjim11", "kcjim11", "kcjim11");
            
            verifyIsPerson("Bartholomew 3, James", "James", "Bartholomew 3");
            verifyIsPerson("Smith, ", null, "Smith");
            verifyIsPerson("Smith,", null, "Smith");
        }
        
        private void verifyIsOrganization(String fullName) {
            JsonObject obj = PersonOrOrgUtil.getPersonOrOrganization(fullName, false, false);
            System.out.println(JsonUtil.prettyPrint(obj));
            assertEquals(obj.getString("fullName"),fullName);
            assertFalse(obj.getBoolean("isPerson"));

        }
        
        private void verifyIsPerson(String fullName, String givenName, String familyName) {
            verifyIsPerson(fullName, givenName, familyName, false);
        }
        
        private void verifyIsPerson(String fullName, String givenName, String familyName, boolean isPerson) {
            JsonObject obj = PersonOrOrgUtil.getPersonOrOrganization(fullName, false, isPerson);
            System.out.println(JsonUtil.prettyPrint(obj));
            assertEquals(obj.getString("fullName"), StringUtil.normalize(fullName));
            assertTrue(obj.getBoolean("isPerson"));
            assertEquals(obj.containsKey("givenName"), givenName != null);
            if(obj.containsKey("givenName") && givenName != null) {
                assertEquals(obj.getString("givenName"),givenName);
            }
            assertEquals(obj.containsKey("familyName"), familyName != null);
            if(obj.containsKey("familyName") && familyName != null) {
                assertEquals(obj.getString("familyName"),familyName);
            }
        }

    }
