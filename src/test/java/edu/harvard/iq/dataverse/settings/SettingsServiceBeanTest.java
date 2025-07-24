package edu.harvard.iq.dataverse.settings;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsServiceBeanTest {
    
    @Nested
    class KeyEnumTest {
        static List<Arguments> parseTestParameters() {
            return List.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("    ", null),
                Arguments.of("foobar", null),
                Arguments.of("ShowMuteOptions", null),
                Arguments.of(":FooBar", null),
                Arguments.of(":ShowMuteOptions", SettingsServiceBean.Key.ShowMuteOptions)
            );
        }
        
        @MethodSource("parseTestParameters")
        @ParameterizedTest
        void testParse(String sut, SettingsServiceBean.Key expected) {
            assertEquals(expected, SettingsServiceBean.Key.parse(sut));
        }
        
        @Test
        void testToString() {
            // Make sure we test the intended behavior so it doesn't change by accident.
            assertEquals(":ShowMuteOptions", SettingsServiceBean.Key.ShowMuteOptions.toString());
        }
        
        @Test
        void testRoundtrip() {
            for (SettingsServiceBean.Key key : SettingsServiceBean.Key.values()) {
                assertEquals(key, SettingsServiceBean.Key.parse(key.toString()));
            }
        }
    }
    
    @Nested
    class ValidateSettingNameTest {
        
        @ValueSource(strings = {":ShowMuteOptions", ":AllowApiTokenLookupViaApi", ":OAuth2CallbackUrl"})
        @ParameterizedTest
        void testValidateSettingName_validNames(String name) {
            assertDoesNotThrow(() -> SettingsServiceBean.validateSettingName(name));
        }
        
        @CsvSource({
            "invalidName, 'The name of the setting is invalid.'",
            ":invalid:suffix, 'The name of the setting may not have a colon separated suffix since Dataverse 6.8. Please update your scripts.'",
            ":NonExistentKey, 'The name of the setting is invalid.'",
            ":ShowMuteOptions/lang/en, 'The name of the setting is invalid.'"
        })
        @ParameterizedTest
        void testValidateSettingName_invalidNames(String name, String expectedMessage) {
            SettingsValidationException exception = assertThrows(SettingsValidationException.class,
                () -> SettingsServiceBean.validateSettingName(name));
            assertEquals(expectedMessage, exception.getMessage());
        }
    }
    
    @Nested
    class ValidateSettingLangTest {
        
        @ValueSource(strings = {"en", "fr", "de"})
        @ParameterizedTest
        void testValidateSettingLang_validLanguage(String language) {
            assertDoesNotThrow(() -> SettingsServiceBean.validateSettingLang(language));
        }
        
        @CsvSource({
            ", 'The language ''null'' is not a valid ISO 639-1 language code.'",
            "e, 'The language ''e'' is not a valid ISO 639-1 language code.'",
            "xyz, 'The language ''xyz'' is not a valid ISO 639-1 language code.'",
            "zz, 'The language ''zz'' is not a valid ISO 639-1 language code.'"
        })
        @ParameterizedTest
        void testValidateSettingLang_invalidLanguage(String language, String expectedMessage) {
            SettingsValidationException exception = assertThrows(SettingsValidationException.class,
                () -> SettingsServiceBean.validateSettingLang(language));
            assertEquals(expectedMessage, exception.getMessage());
        }
    }
    
    @Nested
    class ValidateKeysTest {
        static List<Arguments> validateKeysTestParameters() {
            return List.of(
                Arguments.of(
                    Json.createObjectBuilder()
                        .add(":ApplicationTermsOfUse", "validValue1")
                        .add(":ApplicationTermsOfUse/lang/en", "validValue2")
                        .build(),
                    List.of()
                ),
                Arguments.of(
                    Json.createObjectBuilder()
                        .add(":Invalid:Key", "value1")
                        .add(":NonExistentKey/lang/fr", "value2")
                        .build(),
                    List.of(":Invalid:Key", ":NonExistentKey/lang/fr")
                ),
                Arguments.of(
                    Json.createObjectBuilder()
                        .add(":ApplicationTermsOfUse", "value3")
                        .add("NoColonKey", "value4")
                        .build(),
                    List.of("NoColonKey")
                )
            );
        }
        
        @MethodSource("validateKeysTestParameters")
        @ParameterizedTest
        void testValidateKeys(JsonObject input, List<String> expectedInvalidKeys) {
            List<String> result = SettingsServiceBean.validateKeys(input);
            assertEquals(expectedInvalidKeys, result);
        }
    }
    
    @Nested
    class ListAllAsJsonTest {
        
        static TypedQuery<Setting> typedQuery = mock(TypedQuery.class);
        static EntityManager em = mock(EntityManager.class);
        static SettingsServiceBean settingsServiceBean = new SettingsServiceBean();
        
        @BeforeAll
        static void setup() {
            settingsServiceBean.em = em;
            
            when(em.createNamedQuery(
                ArgumentMatchers.eq("Setting.findAll"),
                ArgumentMatchers.eq(Setting.class)))
                .thenReturn(typedQuery);
        }
        
        @Test
        void testListAllAsJson_noSettings() {
            // Given
            List<Setting> emptyList = Collections.emptyList();
            when(typedQuery.getResultList()).thenReturn(emptyList);
            
            // When
            JsonObject result = settingsServiceBean.listAllAsJson();
            
            // Then
            assertEquals(0, result.size());
        }
        
        @Test
        void testListAllAsJson_nonLocalizedSettings() {
            // Given
            List<Setting> resultList = List.of(
                new Setting("testKey1", "testValue1"),
                new Setting("testKey2", "12345")
            );
            when(typedQuery.getResultList()).thenReturn(resultList);
            
            // When
            JsonObject result = settingsServiceBean.listAllAsJson();
            
            // Then
            assertEquals(2, result.size());
            assertEquals("testValue1", result.getString("testKey1"));
            assertEquals("12345", result.getString("testKey2"));
        }
        
        @Test
        void testListAllAsJson_jsonSetting() {
            // Given
            JsonObject expected = Json.createObjectBuilder()
                .add("default", "2147483648")
                .add("fileOne", "4000000000")
                .add("s3", "8000000000")
                .build();
            
            List<Setting> resultList = List.of(
                new Setting(SettingsServiceBean.Key.MaxFileUploadSizeInBytes.toString(), "{\"default\":\"2147483648\",\"fileOne\":\"4000000000\",\"s3\":\"8000000000\"}")
            );
            when(typedQuery.getResultList()).thenReturn(resultList);
            
            // When
            JsonObject result = settingsServiceBean.listAllAsJson();
            
            // Then
            assertEquals(1, result.size());
            assertEquals(expected.toString(), result.getJsonObject(SettingsServiceBean.Key.MaxFileUploadSizeInBytes.toString()).toString());
        }
        
        @Test
        void testListAllAsJson_localizedSettings() {
            // Given
            List<Setting> resultList = List.of(
                new Setting("localizedKey", "value_base"),
                new Setting("localizedKey", "en", "value_en"),
                new Setting("localizedKey", "fr", "value_fr")
            );
            when(typedQuery.getResultList()).thenReturn(resultList);
            
            // When
            JsonObject result = settingsServiceBean.listAllAsJson();
            
            // Then
            assertEquals(3, result.size());
            assertEquals("value_base", result.getString("localizedKey"));
            assertEquals("value_en", result.getString("localizedKey/lang/en"));
            assertEquals("value_fr", result.getString("localizedKey/lang/fr"));
        }
    }
    
    @Nested
    class ConvertJsonToSettingsTest {
        
        @Test
        void testConvertJsonToSettings_simpleKeyValues() {
            // Given
            JsonObject input = Json.createObjectBuilder()
                .add(":Key1", "Value1")
                .add(":Key2", "123456")
                // The REST API endpoint presents a JsonObject, which may have number literals in it.
                // Check that we can cope with that.
                .add(":Key3", 123456)
                .build();
            
            // When
            Set<Setting> result = SettingsServiceBean.convertJsonToSettings(input);
            
            // Then
            assertEquals(3, result.size());
            assertEquals(
                Set.of(new Setting(":Key1", "Value1"),
                       new Setting(":Key2", "123456"),
                       new Setting(":Key3", "123456")
                ), result);
        }
        
        @Test
        void testConvertJsonToSettings_localizedKeysWithSimpleValues() {
            // Given
            JsonObject input = Json.createObjectBuilder()
                .add(":LocalizedKey/lang/en", "EnglishValue")
                .add(":LocalizedKey/lang/fr", "FrenchValue")
                .build();
            
            // When
            Set<Setting> result = SettingsServiceBean.convertJsonToSettings(input);
            
            // Then
            assertEquals(2, result.size());
            assertEquals(
                Set.of(new Setting(":LocalizedKey", "en", "EnglishValue"),
                       new Setting(":LocalizedKey", "fr", "FrenchValue")
                ), result);
        }
        
        @Test
        void testConvertJsonToSettings_emptyJson() {
            // Given
            JsonObject input = Json.createObjectBuilder().build();
            
            // When
            Set<Setting> result = SettingsServiceBean.convertJsonToSettings(input);
            
            // Then
            assertEquals(0, result.size());
        }
        
        @Test
        void testConvertJsonToSettings_complexJsonValue() {
            // Given
            JsonObject input = Json.createObjectBuilder()
                .add(
                    ":MaxFileUploadSizeInBytes",
                    Json.createObjectBuilder()
                        .add("default", "2147483648")
                        .add("fileOne", "4000000000")
                        .add("s3", "8000000000")
                        .build())
                .build();
            
            // When
            Set<Setting> result = SettingsServiceBean.convertJsonToSettings(input);
            
            // Then
            assertEquals(1, result.size());
            assertEquals(new Setting(":MaxFileUploadSizeInBytes",
                    "{\"default\":\"2147483648\",\"fileOne\":\"4000000000\",\"s3\":\"8000000000\"}"),
                result.stream().toList().get(0));
        }
        
        
    }
    
    @Nested
    class ReplaceAllSettingsTest {
        
        static TypedQuery<Setting> typedQuery = mock(TypedQuery.class);
        static EntityManager em = mock(EntityManager.class);
        static SettingsServiceBean settingsServiceBean = new SettingsServiceBean();
        
        @BeforeAll
        static void setup() {
            settingsServiceBean.em = em;
            
            when(em.createNamedQuery(
                ArgumentMatchers.eq("Setting.findAll"),
                ArgumentMatchers.eq(Setting.class)))
                .thenReturn(typedQuery);
        }
        
        @AfterEach
        void reset() {
            // After each test, we need to clear the invocations for test isolation.
            clearInvocations(em);
        }
        
        @Test
        void testReplaceAllSettings_null() {
            // When/Then
            NullPointerException exception = assertThrows(NullPointerException.class,
                () -> settingsServiceBean.replaceAllSettings(null));
            assertEquals("The list of new settings cannot be null (it may be empty).", exception.getMessage());
        }
        
        @Test
        void testReplaceAllSettings_updateDeleteCreate() {
            // Given
            Setting existingSetting1 = new Setting(":Key1", "Value1");
            Setting existingSetting2 = new Setting(":Key2", "Value2");
            Setting newSetting1 = new Setting(":Key1", "UpdatedValue1");
            Setting newSetting3 = new Setting(":Key3", "Value3");
            
            when(typedQuery.getResultList()).thenReturn(List.of(existingSetting1, existingSetting2));
            
            // When
            Map<Setting, SettingsServiceBean.Op> result = settingsServiceBean.replaceAllSettings(Set.of(newSetting1, newSetting3));
            
            // Then
            assertEquals(3, result.size());
            assertEquals(SettingsServiceBean.Op.UPDATED, result.get(existingSetting1));
            assertEquals(SettingsServiceBean.Op.DELETED, result.get(existingSetting2));
            assertEquals(SettingsServiceBean.Op.CREATED, result.get(newSetting3));
            // We cannot track the em.merge() call in this unit-test, as this happens in ORM code, beyond our reach.
            // Thus check the update to the ORM-tracked entity happened.
            assertEquals("UpdatedValue1", existingSetting1.getContent());
            
            // Verify interactions
            verify(em).remove(existingSetting2);
            verify(em).persist(newSetting3);
            verify(em).flush(); // verify persistence is enforced
        }
        
        @Test
        void testReplaceAllSettings_noChanges() {
            // Given
            Setting existingSetting = new Setting(":Key1", "Value1");
            Setting newSetting = new Setting(":Key1", "Value1");
            
            when(typedQuery.getResultList()).thenReturn(List.of(existingSetting));
            
            // When
            Map<Setting, SettingsServiceBean.Op> result = settingsServiceBean.replaceAllSettings(Set.of(newSetting));
            
            // Then
            assertEquals(1, result.size());
            assertEquals(SettingsServiceBean.Op.UNCHANGED, result.get(existingSetting));
            
            // Verify no interactions causing change
            verify(em, never()).persist(any(Setting.class));
            verify(em, never()).remove(any(Setting.class));
            verify(em, never()).merge(any(Setting.class));
        }
    }
}