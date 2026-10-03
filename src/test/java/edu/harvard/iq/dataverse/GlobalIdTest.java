package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GlobalIdTest {
    
    // --- DOI test data ---
    private static final String DOI_PROTOCOL = AbstractDOIProvider.DOI_PROTOCOL;
    private static final String DOI_RESOLVER_URL = AbstractDOIProvider.DOI_RESOLVER_URL;
    private static final String DOI_AUTHORITY = "10.5072";
    private static final String DOI_IDENTIFIER = "FK2/BYM3IW";
    
    // --- Handle test data ---
    private static final String HDL_PROTOCOL = HandlePidProvider.HDL_PROTOCOL;
    private static final String HDL_RESOLVER_URL = HandlePidProvider.HDL_RESOLVER_URL;
    private static final String HDL_AUTHORITY = "1902.1";
    private static final String HDL_IDENTIFIER = "111012";
    
    // --- Common test data ---
    private static final String DEFAULT_SEPARATOR = "/";
    private static final String CUSTOM_SEPARATOR = "-";
    private static final String PROVIDER_ID = "testProvider";
    private static final String OTHER_PROVIDER_ID = "otherProvider";
    private static final String OTHER_URL_PREFIX = "https://resolver.example.org/";
    
    // --- JSON property names ---
    private static final String JSON_PROTOCOL = "protocol";
    private static final String JSON_AUTHORITY = "authority";
    private static final String JSON_IDENTIFIER = "identifier";
    private static final String JSON_SEPARATOR = "separator";
    private static final String JSON_URL_PREFIX = "urlPrefix";
    private static final String JSON_PROVIDER_ID = "providerId";
    
    private static Jsonb jsonb;
    
    @BeforeAll
    static void setUpJsonb() {
        jsonb = JsonbBuilder.create();
    }
    
    @AfterAll
    static void tearDownJsonb() throws Exception {
        jsonb.close();
    }
    
    // --- Factory helpers ---
    
    private static GlobalId doi() {
        return new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, DEFAULT_SEPARATOR, DOI_RESOLVER_URL, PROVIDER_ID);
    }
    
    private static GlobalId handle() {
        return new GlobalId(HDL_PROTOCOL, HDL_AUTHORITY, HDL_IDENTIFIER, DEFAULT_SEPARATOR, HDL_RESOLVER_URL, PROVIDER_ID);
    }
    
    private static JsonObject parseJson(String json) {
        try (JsonReader reader = JsonUtil.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
    
    @Nested
    class Construction {
        
        @Test
        void storesDoiComponents() {
            GlobalId id = doi();
            
            assertAll(
                () -> assertEquals(DOI_PROTOCOL, id.getProtocol()),
                () -> assertEquals(DOI_AUTHORITY, id.getAuthority()),
                () -> assertEquals(DOI_IDENTIFIER, id.getIdentifier()),
                () -> assertEquals(DEFAULT_SEPARATOR, id.getSeparator()),
                () -> assertEquals(DOI_RESOLVER_URL, id.getUrlPrefix()),
                () -> assertEquals(PROVIDER_ID, id.getProviderId())
            );
        }
        
        @Test
        void storesHandleComponents() {
            GlobalId id = handle();
            
            assertAll(
                () -> assertEquals(HDL_PROTOCOL, id.getProtocol()),
                () -> assertEquals(HDL_AUTHORITY, id.getAuthority()),
                () -> assertEquals(HDL_IDENTIFIER, id.getIdentifier()),
                () -> assertEquals(DEFAULT_SEPARATOR, id.getSeparator()),
                () -> assertEquals(HDL_RESOLVER_URL, id.getUrlPrefix()),
                () -> assertEquals(PROVIDER_ID, id.getProviderId())
            );
        }
        
        @Test
        void defaultsSeparatorWhenNull() {
            GlobalId id = new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, null, DOI_RESOLVER_URL, null);
            
            assertEquals(DEFAULT_SEPARATOR, id.getSeparator());
        }
        
        @Test
        void keepsCustomSeparator() {
            GlobalId id = new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, CUSTOM_SEPARATOR, DOI_RESOLVER_URL, null);
            
            assertEquals(CUSTOM_SEPARATOR, id.getSeparator());
        }
        
        @Test
        void constructFromDataset() {
            Dataset dataset = new Dataset();
            dataset.setProtocol(DOI_PROTOCOL);
            dataset.setAuthority(DOI_AUTHORITY);
            dataset.setIdentifier(DOI_IDENTIFIER);
            
            GlobalId id = dataset.getGlobalId();
            
            assertAll(
                () -> assertEquals(DOI_PROTOCOL, id.getProtocol()),
                () -> assertEquals(DOI_AUTHORITY, id.getAuthority()),
                () -> assertEquals(DOI_IDENTIFIER, id.getIdentifier())
            );
        }
        
        @Test
        void parsingRejectsInjection() {
            GlobalId id = PidUtil.parseAsGlobalID(HDL_PROTOCOL, "'Select value from datasetfieldvalue';", "ha");
            
            assertNull(id);
        }
    }
    
    @Nested
    class IsComplete {
        
        @Test
        void completeWhenAllPartsPresent() {
            assertTrue(new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, null, null, null).isComplete());
        }
        
        @ParameterizedTest(name = "[{index}] protocol={0}, authority={1}, identifier={2}")
        @CsvSource(nullValues = "NULL", value = {
            "NULL, 10.5072, FK2/BYM3IW",
            "doi,  NULL,    FK2/BYM3IW",
            "doi,  10.5072, NULL",
            "'',   10.5072, FK2/BYM3IW",
            "doi,  '',      FK2/BYM3IW",
            "doi,  10.5072, ''"
        })
        void incompleteWhenPartMissing(String protocol, String authority, String identifier) {
            assertFalse(new GlobalId(protocol, authority, identifier, null, null, null).isComplete());
        }
    }
    
    @Nested
    class Representations {
        
        @Test
        void asStringJoinsParts() {
            assertEquals(DOI_PROTOCOL + ":" + DOI_AUTHORITY + DEFAULT_SEPARATOR + DOI_IDENTIFIER, doi().asString());
        }
        
        @Test
        void asStringUsesCustomSeparator() {
            GlobalId id = new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, CUSTOM_SEPARATOR, null, null);
            
            assertEquals(DOI_PROTOCOL + ":" + DOI_AUTHORITY + CUSTOM_SEPARATOR + DOI_IDENTIFIER, id.asString());
        }
        
        @Test
        void toStringEqualsAsString() {
            GlobalId id = doi();
            
            assertEquals(id.asString(), id.toString());
        }
        
        @Test
        void asRawIdentifierOmitsProtocol() {
            assertEquals(DOI_AUTHORITY + DEFAULT_SEPARATOR + DOI_IDENTIFIER, doi().asRawIdentifier());
        }
        
        @ParameterizedTest(name = "[{index}] protocol={0}, authority={1}, identifier={2}")
        @CsvSource(nullValues = "NULL", value = {
            "NULL, 10.5072, FK2/BYM3IW",
            "doi,  NULL,    FK2/BYM3IW",
            "doi,  10.5072, NULL"
        })
        void emptyStringWhenPartNull(String protocol, String authority, String identifier) {
            GlobalId id = new GlobalId(protocol, authority, identifier, null, null, null);
            
            assertAll(
                () -> assertEquals("", id.asString()),
                () -> assertEquals("", id.asRawIdentifier())
            );
        }
        
        @Test
        void asUrlForDoi() {
            assertEquals(DOI_RESOLVER_URL + DOI_AUTHORITY + DEFAULT_SEPARATOR + DOI_IDENTIFIER, doi().asURL());
        }
        
        @Test
        void asUrlForHandle() {
            assertEquals(HDL_RESOLVER_URL + HDL_AUTHORITY + DEFAULT_SEPARATOR + HDL_IDENTIFIER, handle().asURL());
        }
        
        @Test
        void asUrlNullWithoutIdentifier() {
            assertNull(new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, null, null, DOI_RESOLVER_URL, null).asURL());
        }
        
        @Test
        void asUrlNullForMalformedUrl() {
            assertNull(new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, null, null, null).asURL());
        }
    }
    
    @Nested
    class VerifyImportCharacters {
        
        @ParameterizedTest(name = "[{index}] \"{0}\" is accepted")
        @ValueSource(strings = {"-", "qwertyQWERTY"})
        void acceptsAllowedCharacters(String pid) {
            assertTrue(GlobalId.verifyImportCharacters(pid));
        }
        
        @ParameterizedTest(name = "[{index}] \"{0}\" is rejected")
        @ValueSource(strings = {"Hällochen", "*"})
        void rejectsDisallowedCharacters(String pid) {
            assertFalse(GlobalId.verifyImportCharacters(pid));
        }
    }
    
    @Nested
    class EqualsAndHashCode {
        
        @Test
        void reflexive() {
            GlobalId id = doi();
            assertEquals(id, id);
        }
        
        @Test
        void symmetricWithEqualHashCodes() {
            GlobalId a = doi();
            GlobalId b = doi();
            
            assertAll(
                () -> assertEquals(a, b),
                () -> assertEquals(b, a),
                () -> assertEquals(a.hashCode(), b.hashCode())
            );
        }
        
        @Test
        void transitive() {
            GlobalId a = doi();
            GlobalId b = doi();
            GlobalId c = doi();
            
            assertAll(
                () -> assertEquals(a, b),
                () -> assertEquals(b, c),
                () -> assertEquals(a, c)
            );
        }
        
        @Test
        void notEqualToNullOrOtherType() {
            GlobalId id = doi();
            
            assertAll(
                () -> assertNotEquals(null, id),
                () -> assertNotEquals(id.asString(), id)
            );
        }
        
        @Test
        void allNullFieldsTreatedAsEqual() {
            GlobalId a = new GlobalId(null, null, null, null, null, null);
            GlobalId b = new GlobalId(null, null, null, null, null, null);
            
            assertAll(
                () -> assertEquals(a, b),
                () -> assertEquals(a.hashCode(), b.hashCode())
            );
        }
        
        @Test
        void ignoresSeparatorAndUrlPrefix() {
            GlobalId a = doi();
            GlobalId b = new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, CUSTOM_SEPARATOR, OTHER_URL_PREFIX, PROVIDER_ID);
            
            assertAll(
                () -> assertEquals(a, b),
                () -> assertEquals(a.hashCode(), b.hashCode())
            );
        }
        
        static Stream<Arguments> differingIds() {
            return Stream.of(
                arguments("protocol", new GlobalId(HDL_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, DEFAULT_SEPARATOR, DOI_RESOLVER_URL, PROVIDER_ID)),
                arguments("authority", new GlobalId(DOI_PROTOCOL, HDL_AUTHORITY, DOI_IDENTIFIER, DEFAULT_SEPARATOR, DOI_RESOLVER_URL, PROVIDER_ID)),
                arguments("identifier", new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, HDL_IDENTIFIER, DEFAULT_SEPARATOR, DOI_RESOLVER_URL, PROVIDER_ID)),
                arguments("providerId", new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, DEFAULT_SEPARATOR, DOI_RESOLVER_URL, OTHER_PROVIDER_ID)),
                arguments("providerId (null)", new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, DEFAULT_SEPARATOR, DOI_RESOLVER_URL, null))
            );
        }
        
        @ParameterizedTest(name = "[{index}] differs in {0}")
        @MethodSource("differingIds")
        void notEqualWhenComponentDiffers(String component, GlobalId other) {
            GlobalId id = doi();
            
            assertAll(
                () -> assertNotEquals(id, other),
                () -> assertNotEquals(other, id)
            );
        }
        
        @Test
        void usableAsHashKey() {
            Set<GlobalId> set = new HashSet<>();
            set.add(doi());
            set.add(doi());
            set.add(handle());
            
            Map<GlobalId, String> map = new HashMap<>();
            map.put(doi(), DOI_IDENTIFIER);
            
            assertAll(
                () -> assertEquals(2, set.size()),
                () -> assertTrue(set.contains(doi())),
                () -> assertEquals(DOI_IDENTIFIER, map.get(doi()))
            );
        }
    }
    
    @Nested
    class JsonBinding {
        
        @Nested
        class Serialization {
            
            @Test
            void writesPublicProperties() {
                JsonObject json = parseJson(jsonb.toJson(doi()));
                
                assertAll(
                    () -> assertEquals(
                        Set.of(JSON_PROTOCOL, JSON_AUTHORITY, JSON_IDENTIFIER, JSON_SEPARATOR, JSON_URL_PREFIX, JSON_PROVIDER_ID),
                        json.keySet()),
                    () -> assertEquals(DOI_PROTOCOL, json.getString(JSON_PROTOCOL)),
                    () -> assertEquals(DOI_AUTHORITY, json.getString(JSON_AUTHORITY)),
                    () -> assertEquals(DOI_IDENTIFIER, json.getString(JSON_IDENTIFIER)),
                    () -> assertEquals(DEFAULT_SEPARATOR, json.getString(JSON_SEPARATOR)),
                    () -> assertEquals(DOI_RESOLVER_URL, json.getString(JSON_URL_PREFIX)),
                    () -> assertEquals(PROVIDER_ID, json.getString(JSON_PROVIDER_ID))
                );
            }
            
            @Test
            void omitsTransientComplete() {
                JsonObject json = parseJson(jsonb.toJson(doi()));
                
                assertFalse(json.containsKey("complete"));
            }
            
            @Test
            void omitsNullProperties() {
                GlobalId id = new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, null, null, null);
                
                JsonObject json = parseJson(jsonb.toJson(id));
                
                assertAll(
                    () -> assertFalse(json.containsKey(JSON_URL_PREFIX)),
                    () -> assertFalse(json.containsKey(JSON_PROVIDER_ID)),
                    () -> assertEquals(DEFAULT_SEPARATOR, json.getString(JSON_SEPARATOR))
                );
            }
        }
        
        @Nested
        class Deserialization {
            
            @Test
            void readsAllProperties() {
                String json = JsonUtil.createObjectBuilder()
                    .add(JSON_PROTOCOL, DOI_PROTOCOL)
                    .add(JSON_AUTHORITY, DOI_AUTHORITY)
                    .add(JSON_IDENTIFIER, DOI_IDENTIFIER)
                    .add(JSON_SEPARATOR, CUSTOM_SEPARATOR)
                    .add(JSON_URL_PREFIX, DOI_RESOLVER_URL)
                    .add(JSON_PROVIDER_ID, PROVIDER_ID)
                    .build()
                    .toString();
                
                GlobalId id = jsonb.fromJson(json, GlobalId.class);
                
                assertAll(
                    () -> assertEquals(DOI_PROTOCOL, id.getProtocol()),
                    () -> assertEquals(DOI_AUTHORITY, id.getAuthority()),
                    () -> assertEquals(DOI_IDENTIFIER, id.getIdentifier()),
                    () -> assertEquals(CUSTOM_SEPARATOR, id.getSeparator()),
                    () -> assertEquals(DOI_RESOLVER_URL, id.getUrlPrefix()),
                    () -> assertEquals(PROVIDER_ID, id.getProviderId())
                );
            }
            
            @Test
            void defaultsMissingSeparator() {
                String json = JsonUtil.createObjectBuilder()
                    .add(JSON_PROTOCOL, DOI_PROTOCOL)
                    .add(JSON_AUTHORITY, DOI_AUTHORITY)
                    .add(JSON_IDENTIFIER, DOI_IDENTIFIER)
                    .build()
                    .toString();
                
                GlobalId id = jsonb.fromJson(json, GlobalId.class);
                
                assertAll(
                    () -> assertEquals(DEFAULT_SEPARATOR, id.getSeparator()),
                    () -> assertNull(id.getUrlPrefix()),
                    () -> assertNull(id.getProviderId()),
                    () -> assertTrue(id.isComplete())
                );
            }
            
            @Test
            void defaultsNullSeparator() {
                String json = JsonUtil.createObjectBuilder()
                    .add(JSON_PROTOCOL, DOI_PROTOCOL)
                    .add(JSON_AUTHORITY, DOI_AUTHORITY)
                    .add(JSON_IDENTIFIER, DOI_IDENTIFIER)
                    .addNull(JSON_SEPARATOR)
                    .build()
                    .toString();
                
                GlobalId id = jsonb.fromJson(json, GlobalId.class);
                
                assertEquals(DEFAULT_SEPARATOR, id.getSeparator());
            }
        }
        
        @Test
        void roundTrip() {
            GlobalId original = new GlobalId(DOI_PROTOCOL, DOI_AUTHORITY, DOI_IDENTIFIER, CUSTOM_SEPARATOR, DOI_RESOLVER_URL, PROVIDER_ID);
            
            GlobalId restored = jsonb.fromJson(jsonb.toJson(original), GlobalId.class);
            
            // equals() ignores separator and urlPrefix, so verify them explicitly
            assertAll(
                () -> assertEquals(original, restored),
                () -> assertEquals(original.getSeparator(), restored.getSeparator()),
                () -> assertEquals(original.getUrlPrefix(), restored.getUrlPrefix())
            );
        }
    }
    
    @Nested
    class JavaSerialization {
        
        @Test
        void roundTrip() throws IOException, ClassNotFoundException {
            GlobalId original = doi();
            
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(original);
            }
            
            Object restored;
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                restored = in.readObject();
            }
            
            assertNotNull(restored);
            GlobalId restoredId = (GlobalId) restored;
            assertAll(
                () -> assertEquals(original, restoredId),
                () -> assertEquals(original.getSeparator(), restoredId.getSeparator()),
                () -> assertEquals(original.getUrlPrefix(), restoredId.getUrlPrefix())
            );
        }
    }
}
