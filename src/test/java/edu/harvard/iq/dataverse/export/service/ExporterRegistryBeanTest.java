package edu.harvard.iq.dataverse.export.service;

import edu.harvard.iq.dataverse.export.service.ExporterRegistryBean.Details;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.Exporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExporterRegistryBeanTest {
    
    /* Fixture graph (arrow reads "is prerequisite of"):  BASE -> DERIVED -> DEEP;  STANDALONE has no relations. */
    private static final String BASE = "base";
    private static final String DERIVED = "derived";
    private static final String DEEP = "deep";
    private static final String STANDALONE = "standalone";
    
    private static Exporter mockExporter(String formatName) {
        return mockExporter(formatName, null);
    }
    
    private static Exporter mockExporter(String formatName, String prerequisite) {
        Exporter exporter = mock(Exporter.class);
        when(exporter.getFormatName()).thenReturn(formatName);
        when(exporter.getPrerequisiteFormatName()).thenReturn(Optional.ofNullable(prerequisite));
        when(exporter.getDisplayName(any())).thenReturn(formatName.toUpperCase(Locale.ROOT));
        when(exporter.getMediaType()).thenReturn("application/" + formatName);
        when(exporter.isHarvestable()).thenReturn(true);
        when(exporter.isAvailableToUsers()).thenReturn(false);
        return exporter;
    }
    
    private static Stream<Exporter> mockExporters(String... formatNames) {
        return Arrays.stream(formatNames)
            .map(ExporterRegistryBeanTest::mockExporter);
    }
    
    private static Map<String, Exporter> mapOf(Exporter... exporters) {
        return Arrays.stream(exporters)
            .collect(Collectors.toUnmodifiableMap(Exporter::getFormatName, Function.identity()));
    }
    
    private static Map<String, Exporter> chainFixture() {
        return mapOf(mockExporter(BASE), mockExporter(DERIVED, BASE), mockExporter(DEEP, DERIVED), mockExporter(STANDALONE));
    }
    
    private static List<String> formatNamesOf(List<Exporter> exporters) {
        return exporters.stream().map(Exporter::getFormatName).toList();
    }
    
    @Nested
    class EmptyRegistry {
        
        private final ExporterRegistryBean registry = new ExporterRegistryBean();
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "unknown")
        void getReturnsEmptyForAnyName(String formatName) {
            assertTrue(registry.get(formatName).isEmpty());
        }
        
        @Test
        void exposesNoExportersAtAll() {
            assertAll(
                () -> assertTrue(registry.getAll().isEmpty()),
                () -> assertTrue(registry.getDetails().isEmpty()),
                () -> assertTrue(registry.getDetail("unknown").isEmpty()),
                () -> assertTrue(registry.getTransitiveDependents("unknown").isEmpty())
            );
        }
        
        @Test
        void requireAllExistAcceptsEmptyListMeaningAll() {
            assertDoesNotThrow(() -> registry.requireAllExist(List.of()));
        }
    }
    
    @Nested
    class PopulatedRegistry {
        
        private Map<String, Exporter> exporters;
        private ExporterRegistryBean registry;
        
        @BeforeEach
        void setUp() {
            exporters = chainFixture();
            registry = new ExporterRegistryBean(exporters);
        }
        
        @Test
        void getByFormatNameReturnsRegisteredInstance() {
            assertAll(
                () -> assertSame(exporters.get(DEEP), registry.get(DEEP).orElseThrow()),
                () -> assertTrue(registry.get("unknown").isEmpty()),
                () -> assertTrue(registry.get((String) null).isEmpty())
            );
        }
        
        @Test
        void getByDetailsRoundTrips() {
            Details details = registry.getDetail(DERIVED).orElseThrow();
            assertSame(exporters.get(DERIVED), registry.get(details));
        }
        
        @Test
        void getByNullDetailsIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> registry.get((Details) null));
        }
        
        @Test
        void getAllIsCompleteAndUnmodifiable() {
            List<Exporter> all = registry.getAll();
            assertAll(
                () -> assertEquals(exporters.size(), all.size()),
                () -> assertTrue(all.containsAll(exporters.values())),
                () -> assertThrows(UnsupportedOperationException.class, () -> all.add(mockExporter("intruder")))
            );
        }
        
        @Test
        void detailsMirrorExporterProperties() {
            Details details = registry.getDetail(BASE).orElseThrow();
            assertAll(
                () -> assertEquals(BASE, details.formatName()),
                () -> assertEquals("BASE", details.localizedDisplayName()),
                () -> assertEquals("application/base", details.mediaType()),
                () -> assertTrue(details.isHarvestable()),
                () -> assertFalse(details.isAvailableToUsers())
            );
        }
        
        @Test
        void getDetailsCoversEveryFormat() {
            Set<String> names = registry.getDetails().stream()
                .map(Details::formatName)
                .collect(Collectors.toSet());
            assertEquals(exporters.keySet(), names);
        }
        
        @Test
        void requireExistsAcceptsRegisteredFormat() {
            assertDoesNotThrow(() -> registry.requireExists(STANDALONE));
        }
        
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "unknown"})
        void requireExistsRejectsNullOrUnknown(String formatName) {
            assertThrows(IllegalArgumentException.class, () -> registry.requireExists(formatName));
        }
        
        @Test
        void requireAllExistEnumeratesOnlyInvalidFormats() {
            var ex = assertThrows(IllegalArgumentException.class,
                () -> registry.requireAllExist(List.of(BASE, "foo", DEEP, "bar")));
            assertAll(
                () -> assertTrue(ex.getMessage().contains("foo")),
                () -> assertTrue(ex.getMessage().contains("bar")),
                () -> assertFalse(ex.getMessage().contains(BASE)),
                () -> assertFalse(ex.getMessage().contains(DEEP))
            );
        }
        
        @Test
        void requireAllExistRejectsNullList() {
            assertThrows(IllegalArgumentException.class, () -> registry.requireAllExist(null));
        }
        
        @ParameterizedTest(name = "dependents of ''{0}''")
        @MethodSource("expectedDependents")
        void transitiveDependentsAreResolved(String format, Set<String> expected) {
            assertEquals(expected, registry.getTransitiveDependents(format));
        }
        
        static Stream<Arguments> expectedDependents() {
            return Stream.of(
                arguments(BASE, Set.of(DERIVED, DEEP)),
                arguments(DERIVED, Set.of(DEEP)),
                arguments(DEEP, Set.of()),
                arguments(STANDALONE, Set.of()),
                arguments("unknown", Set.of())
            );
        }
    }
    
    @Nested
    class VerifyRequirements {
        
        @Test
        void acceptsValidChainAndEmptyMap() {
            assertAll(
                () -> assertDoesNotThrow(() -> ExporterRegistryBean.verifyRequirements(chainFixture())),
                () -> assertDoesNotThrow(() -> ExporterRegistryBean.verifyRequirements(Map.of()))
            );
        }
        
        @Test
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> ExporterRegistryBean.verifyRequirements(null));
        }
        
        @ParameterizedTest(name = "{0}")
        @MethodSource("brokenGraphs")
        void rejectsBrokenGraphs(Map<String, Exporter> exporters) {
            assertThrows(ExportException.class, () -> ExporterRegistryBean.verifyRequirements(exporters));
        }
        
        static Stream<Named<Map<String, Exporter>>> brokenGraphs() {
            return Stream.of(
                Named.of("missing prerequisite", mapOf(mockExporter("a", "ghost"))),
                Named.of("self-referencing format", mapOf(mockExporter("a", "a"))),
                Named.of("two-node cycle", mapOf(
                    mockExporter("a", "b"),
                    mockExporter("b", "a"))
                ),
                Named.of("chain leading into a cycle (d -> a -> b -> a)", mapOf(
                    mockExporter("d", "a"),
                    mockExporter("a", "b"),
                    mockExporter("b", "a"))
                )
            );
        }
    }
    
    @Nested
    class BuildTransitiveDependents {
        
        private final Map<String, Set<String>> dependents =
            ExporterRegistryBean.buildTransitiveDependents(chainFixture());
        
        @Test
        void everyFormatHasAnEntryIncludingLeaves() {
            assertEquals(Set.of(BASE, DERIVED, DEEP, STANDALONE), dependents.keySet());
        }
        
        @Test
        void resolvesTransitivelyAndNeverIncludesSelf() {
            assertAll(
                () -> assertEquals(Set.of(DERIVED, DEEP), dependents.get(BASE)),
                () -> assertEquals(Set.of(DEEP), dependents.get(DERIVED)),
                () -> assertEquals(Set.of(), dependents.get(DEEP)),
                () -> assertEquals(Set.of(), dependents.get(STANDALONE))
            );
        }
        
        @Test
        void resultIsDeeplyUnmodifiable() {
            assertAll(
                () -> assertThrows(UnsupportedOperationException.class, () -> dependents.put("x", Set.of())),
                () -> assertThrows(UnsupportedOperationException.class, () -> dependents.get(BASE).add("x"))
            );
        }
        
        @Test
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> ExporterRegistryBean.buildTransitiveDependents(null));
        }
    }
    
    @Nested
    class TopologicalComparator {
        @Test
        void comparatorFallsBackToFormatNameOrder() {
            // Given an empty registry
            ExporterRegistryBean registry = new ExporterRegistryBean();
            
            // When
            List<Exporter> sorted = Stream.of(mockExporter("b"), mockExporter("a"))
                .sorted(registry.getTopologicalComparator())
                .toList();
            
            // Then
            assertEquals(List.of("a", "b"), formatNamesOf(sorted));
        }
        
        @Test
        void comparatorOrdersPrerequisitesFirstAndTiesByName() {
            // Given a populated registry
            ExporterRegistryBean registry = new ExporterRegistryBean(chainFixture());
            
            List<Exporter> sorted = registry.getAll().stream()
                .sorted(registry.getTopologicalComparator())
                .toList();
            assertEquals(List.of(BASE, DERIVED, DEEP, STANDALONE), formatNamesOf(sorted));
        }
        
        @Test
        void sortsByDescendingDependentCountThenByName() {
            Comparator<Exporter> comparator = ExporterRegistryBean.buildTopologicalComparator(Map.of(
                "a", Set.of("b", "c"),
                "b", Set.of("c"),
                "y", Set.of(),
                "c", Set.of(),
                "z", Set.of("y")
            ));
            
            List<Exporter> sorted = mockExporters("y", "c", "b", "z", "a")
                .sorted(comparator)
                .toList();
            assertEquals(List.of("a", "b", "z", "c", "y"), formatNamesOf(sorted));
        }
        
        @Test
        void treatsFormatsAbsentFromMapAsHavingNoDependents() {
            Comparator<Exporter> comparator = ExporterRegistryBean.buildTopologicalComparator(Map.of("a", Set.of("b")));
            List<Exporter> sorted = mockExporters("unknown", "b", "a")
                .sorted(comparator)
                .toList();
            assertEquals(List.of("a", "b", "unknown"), formatNamesOf(sorted));
        }
        
        @Test
        void rejectsNull() {
            assertThrows(NullPointerException.class, () -> ExporterRegistryBean.buildTopologicalComparator(null));
        }
    }
}
