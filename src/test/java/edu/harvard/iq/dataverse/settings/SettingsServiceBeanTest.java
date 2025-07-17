package edu.harvard.iq.dataverse.settings;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
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
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SettingsServiceBean.validateSettingLang(language));
            assertEquals(expectedMessage, exception.getMessage());
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
}