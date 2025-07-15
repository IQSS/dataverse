package edu.harvard.iq.dataverse.settings;

import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                new Setting("testKey2", "testValue2")
            );
            when(typedQuery.getResultList()).thenReturn(resultList);
            
            // When
            JsonObject result = settingsServiceBean.listAllAsJson();
            
            // Then
            assertEquals(2, result.size());
            assertEquals("testValue1", result.getString("testKey1"));
            assertEquals("testValue2", result.getString("testKey2"));
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
            assertEquals(1, result.size());
            JsonObject localizedSetting = result.getJsonObject("localizedKey");
            
            assertEquals(3, localizedSetting.size());
            assertEquals("value_base", localizedSetting.getString("base"));
            assertEquals("value_en", localizedSetting.getString("en"));
            assertEquals("value_fr", localizedSetting.getString("fr"));
        }
    }
}