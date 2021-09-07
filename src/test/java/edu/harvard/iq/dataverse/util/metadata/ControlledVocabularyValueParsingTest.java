package edu.harvard.iq.dataverse.util.metadata;

import com.univocity.parsers.common.DataValidationException;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.MetadataBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ControlledVocabularyValueTest {
    
    static BeanListProcessor<ControlledVocabularyValue> controlledVocabularyValueProcessor = new BeanListProcessor<>(ControlledVocabularyValue.class);
    static TsvParser parser;
    static TsvParserSettings settings = new TsvParserSettings();
    
    @BeforeAll
    static void setUp() {
        settings.setProcessor(controlledVocabularyValueProcessor);
        settings.setHeaderExtractionEnabled(true);
        // TODO: replace this char with a global constant (introduced when creating the parsing bean)
        settings.getFormat().setComment('\'');
        parser = new TsvParser(settings);
    }
    
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   "})
    public void parseInvalidValue(String value) {
        // given
        StringReader subjectUnderTest = new StringReader(generateCvvTSV(Map.of(
            ControlledVocabularyValue.Headers.DATASET_FIELD, "test",
            ControlledVocabularyValue.Headers.VALUE, value,
            ControlledVocabularyValue.Headers.DISPLAY_ORDER, "0")));
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(subjectUnderTest));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"https://www^^", "doi:1234^", "hello my name", "hello!", "hello#"})
    public void parseInvalidIdentifier(String identifier) {
        // given
        StringReader subjectUnderTest = new StringReader(generateCvvTSV(Map.of(
            ControlledVocabularyValue.Headers.DATASET_FIELD, "test",
            ControlledVocabularyValue.Headers.VALUE, "test",
            ControlledVocabularyValue.Headers.DISPLAY_ORDER, "0",
            ControlledVocabularyValue.Headers.IDENTIFIER, identifier)));
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(subjectUnderTest));
    }
    
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"https://skosmos/foo#bar", "doi:1234", "hello_my-name", "foo+bar"})
    public void parseValidIdentifier(String identifier) {
        // given
        StringReader subjectUnderTest = new StringReader(generateCvvTSV(Map.of(
            ControlledVocabularyValue.Headers.DATASET_FIELD, "test",
            ControlledVocabularyValue.Headers.VALUE, "test",
            ControlledVocabularyValue.Headers.DISPLAY_ORDER, "0",
            ControlledVocabularyValue.Headers.IDENTIFIER, identifier)));
        // when
        parser.parse(subjectUnderTest);
        // then
        assertEquals(1, controlledVocabularyValueProcessor.getBeans().size());
        if (!identifier.isEmpty()) {
            assertEquals(identifier, controlledVocabularyValueProcessor.getBeans().get(0).getIdentifier());
        } else {
            assertNull(controlledVocabularyValueProcessor.getBeans().get(0).getIdentifier());
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"-1", "-100", "0.00", "abc", "_foobar!"})
    public void parseInvalidDisplayOrder(String displayOrder) {
        // given
        StringReader subjectUnderTest = new StringReader(generateCvvTSV(Map.of(
                ControlledVocabularyValue.Headers.DATASET_FIELD, "test",
                ControlledVocabularyValue.Headers.VALUE, "test",
                ControlledVocabularyValue.Headers.DISPLAY_ORDER, displayOrder)));
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(subjectUnderTest));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {".foobar", "!foo", "foo!", "_foo_", "-foo-foo", "foo.", "_foo.foo_", "1foo", ".bar"})
    public void parseInvalidDatasetFieldTypeName(String fieldName) {
        // given
        StringReader subjectUnderTest = new StringReader(generateCvvTSV(Map.of(
            ControlledVocabularyValue.Headers.DATASET_FIELD, fieldName,
            ControlledVocabularyValue.Headers.VALUE, "test",
            ControlledVocabularyValue.Headers.DISPLAY_ORDER, "0")));
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(subjectUnderTest));
    }
    
    @Test
    public void parseAlternateValues() {
        // given
        String t1 = "test1";
        String t2 = "test2";
        String tsv = generateCvvTSV(Map.of(
                ControlledVocabularyValue.Headers.DATASET_FIELD, "test",
                ControlledVocabularyValue.Headers.VALUE, "test",
                ControlledVocabularyValue.Headers.DISPLAY_ORDER, "0"),
                List.of(t1, t2));
        StringReader subjectUnderTest = new StringReader(tsv);
        // when
        parser.parse(subjectUnderTest);
        // then
        assertEquals(1, controlledVocabularyValueProcessor.getBeans().size());
        
        ControlledVocabularyValue result = controlledVocabularyValueProcessor.getBeans().get(0);
        
        assertFalse(result.getControlledVocabAlternates().isEmpty());
        assertTrue(result.getControlledVocabAlternates().stream().allMatch(a -> a instanceof Placeholder.ControlledVocabAlternate));
        
        assertEquals(2, result.getControlledVocabAlternates().size());
        assertTrue(result.getControlledVocabAlternates().stream().anyMatch(a -> t1.equals(a.getStrValue())));
        assertTrue(result.getControlledVocabAlternates().stream().anyMatch(a -> t2.equals(a.getStrValue())));
    }
    
    private static final String header = "#controlledVocabulary\t" + String.join("\t", ControlledVocabularyValue.Headers.keys());
    
    /**
     * This method simply inserts all the values from the map into a line, combined by \t and adds a "header" line before it.
     * It does this based on the {@link MetadataBlock.Headers} enum value order, which is the same as in the TSV definition.
     * Nonpresent values will be inserted as blank strings.
     *
     * @param values
     * @return
     */
    public static String generateCvvTSV(Map<ControlledVocabularyValue.Headers, String> values) {
        List<String> fieldValues = Arrays.stream(ControlledVocabularyValue.Headers.values())
            .map(k -> values.getOrDefault(k, ""))
            .collect(Collectors.toList());
        return header + settings.getFormat().getLineSeparatorString() + "\t" + String.join("\t", fieldValues);
    }
    
    public static String generateCvvTSV(Map<ControlledVocabularyValue.Headers, String> values, List<String> altValues) {
        List<String> fieldValues = Arrays.stream(ControlledVocabularyValue.Headers.values())
            .map(k -> values.getOrDefault(k, ""))
            .collect(Collectors.toList());
        
        String headerWithAlts = header + ("\t"+ControlledVocabularyValue.Headers.Constants.ALT_VALUES).repeat(altValues.size()-1);
        
        return headerWithAlts + settings.getFormat().getLineSeparatorString() +
            "\t" + String.join("\t", fieldValues) + String.join("\t", altValues);
    }
}