/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import com.univocity.parsers.common.DataProcessingException;
import com.univocity.parsers.common.DataValidationException;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import edu.harvard.iq.dataverse.search.SolrField;
import edu.harvard.iq.dataverse.util.metadata.Placeholder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author skraffmi
 */
public class DatasetFieldTypeTest {
    
    static BeanListProcessor<DatasetFieldType> datasetFieldTypeProcessor = new BeanListProcessor<>(DatasetFieldType.class);
    static TsvParser parser;
    static TsvParserSettings settings = new TsvParserSettings();
    
    static Map<DatasetFieldType.Headers, String> subject = new HashMap<>();
    
    @BeforeAll
    static void setUpClass() {
        settings.setProcessor(datasetFieldTypeProcessor);
        settings.setHeaderExtractionEnabled(true);
        // TODO: replace this char with a global constant (introduced when creating the parsing bean)
        settings.getFormat().setComment('\'');
        parser = new TsvParser(settings);
    }

    @BeforeEach
    void setUp() {
        subject.clear();
        subject.put(DatasetFieldType.Headers.NAME, "test");
        subject.put(DatasetFieldType.Headers.TITLE, "Testfield");
        subject.put(DatasetFieldType.Headers.DESCRIPTION, "A little test");
        subject.put(DatasetFieldType.Headers.WATERMARK, "Type here...");
        subject.put(DatasetFieldType.Headers.FIELD_TYPE, "none");
        subject.put(DatasetFieldType.Headers.DISPLAY_ORDER, "1");
        subject.put(DatasetFieldType.Headers.DISPLAY_FORMAT, "");
        subject.put(DatasetFieldType.Headers.ADVANCED_SEARCH_FIELD, "FALSE");
        subject.put(DatasetFieldType.Headers.ALLOW_CONTROLLED_VOCABULARY, "FALSE");
        subject.put(DatasetFieldType.Headers.ALLOW_MULTIPLES, "FALSE");
        subject.put(DatasetFieldType.Headers.FACETABLE, "FALSE");
        subject.put(DatasetFieldType.Headers.DISPLAY_ON_CREATE, "FALSE");
        subject.put(DatasetFieldType.Headers.REQUIRED, "FALSE");
        subject.put(DatasetFieldType.Headers.PARENT, "");
        subject.put(DatasetFieldType.Headers.METADATA_BLOCK, "test");
        subject.put(DatasetFieldType.Headers.TERM_URI, "");
    }
    
    @Test
    public void parseUnmodifiedSubject() {
        // given (remember - subject will be RESET before every test!)
        StringReader subjectUnderTest = new StringReader(generateDatasetFieldTSV(subject));
        // when
        parser.parse(subjectUnderTest);
        // then
        assertEquals(1, datasetFieldTypeProcessor.getBeans().size());
        assertNotNull(datasetFieldTypeProcessor.getBeans().get(0));
        assertNotNull(datasetFieldTypeProcessor.getBeans().get(0).getName());
    }
    
    private static Stream<Arguments> booleanOptionsMatrix(List<String> testValues) {
        List<Arguments> args = new ArrayList<>();
        // create a "matrix" with stream in stream and flattening afterwards.
        testValues.stream()
            .map(tv -> DatasetFieldType.Headers.booleanKeys()
                .stream()
                .map(h -> Arguments.of(h, tv))
                .collect(Collectors.toUnmodifiableList()))
            .forEach(args::addAll);
        return args.stream();
    }
    
    private static Stream<Arguments> booleanOptionsInvalidMatrix() {
        return booleanOptionsMatrix(List.of("blubb", "1234", "0", "1"));
    }
    
    private static Stream<Arguments> booleanOptionsValidMatrix() {
        return booleanOptionsMatrix(List.of("true", "TRUE", "false", "FALSE"));
    }
    
    @ParameterizedTest
    @MethodSource("booleanOptionsInvalidMatrix")
    public void parseInvalidBooleanOpt(DatasetFieldType.Headers key, String boolOpt) {
        // given (remember - subject will be RESET before every test!)
        subject.put(key, boolOpt);
        StringReader subjectUnderTest = new StringReader(generateDatasetFieldTSV(subject));
        // when & then
        assertThrows(DataProcessingException.class, () -> parser.parse(subjectUnderTest));
    }
    
    @ParameterizedTest
    @MethodSource("booleanOptionsValidMatrix")
    public void parseValidBooleanOpt(DatasetFieldType.Headers key, String boolOpt) {
        // given (remember - subject will be RESET before every test!)
        subject.put(key, boolOpt);
        StringReader subjectUnderTest = new StringReader(generateDatasetFieldTSV(subject));
        // when
        parser.parse(subjectUnderTest);
        // then
        assertEquals(1, datasetFieldTypeProcessor.getBeans().size());
        assertNotNull(datasetFieldTypeProcessor.getBeans().get(0));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"-1", "-100", "0.00", "abc", "_foobar!"})
    public void parseInvalidDisplayOrder(String displayOrder) {
        // given (remember - subject will be RESET before every test!)
        subject.put(DatasetFieldType.Headers.DISPLAY_ORDER, displayOrder);
        StringReader subjectUnderTest = new StringReader(generateDatasetFieldTSV(subject));
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(subjectUnderTest));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {".foobar", "!foo", "foo!", "_foo_", "-foo-foo", "foo.", "_foo.foo_", "1foo", ".bar"})
    public void parseInvalidName(String name) {
        // given (remember - subject will be RESET before every test!)
        subject.put(DatasetFieldType.Headers.NAME, name);
        StringReader subjectUnderTest = new StringReader(generateDatasetFieldTSV(subject));
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(subjectUnderTest));
    }
    
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"hello", "HelloMyName", "hello_my_name_is", "_foo.bar", "foo.bar", "_foo", "foo_"})
    void parseValidParentName(String parent) {
        // given
        subject.put(DatasetFieldType.Headers.PARENT, parent);
        StringReader reader = new StringReader(generateDatasetFieldTSV(subject));
        
        // when
        parser.parse(reader);
        List<DatasetFieldType> blocks = datasetFieldTypeProcessor.getBeans();
        
        // then
        assertEquals(1, blocks.size());
        if (!parent.isEmpty()) {
            assertNotNull(blocks.get(0).getParentDatasetFieldType());
            assertTrue(blocks.get(0).getParentDatasetFieldType() instanceof DatasetFieldType);
            assertTrue(blocks.get(0).getParentDatasetFieldType() instanceof Placeholder.DatasetFieldType);
            assertEquals(parent, blocks.get(0).getParentDatasetFieldType().getName());
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"hello", "helloMyName", "hello_my_name_is", "foobar_", "foo213bar", "foo1234", "foo_"})
    void parseValidMetadataBlock(String block) {
        // given
        subject.put(DatasetFieldType.Headers.METADATA_BLOCK, block);
        StringReader reader = new StringReader(generateDatasetFieldTSV(subject));
        
        // when
        parser.parse(reader);
        List<DatasetFieldType> blocks = datasetFieldTypeProcessor.getBeans();
        
        // then
        assertEquals(1, blocks.size());
        assertNotNull(blocks.get(0).getMetadataBlock());
        assertTrue(blocks.get(0).getMetadataBlock() instanceof MetadataBlock);
        assertTrue(blocks.get(0).getMetadataBlock() instanceof Placeholder.MetadataBlock);
        assertEquals(block, blocks.get(0).getMetadataBlock().getName());
    }


    /**
     * Test of isSanitizeHtml method, of class DatasetFieldType.
     */
    @Test
    public void testIsSanitizeHtml() {
        System.out.println("isSanitizeHtml");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.TEXT);
        Boolean result = instance.isSanitizeHtml();
        assertFalse(result);
               
        //if textbox then sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.TEXTBOX);
        result = instance.isSanitizeHtml();
        assertEquals(true, result);
        
        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        result = instance.isSanitizeHtml();
        assertEquals(false, result);
        
        //URL, too
        instance.setFieldType(DatasetFieldType.FieldType.URL);
        result = instance.isSanitizeHtml();
        assertEquals(true, result);
    }
    
    @Test
    public void testIsEscapeOutputText(){
                System.out.println("testIsEscapeOutputText");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.TEXT);
        Boolean result = instance.isEscapeOutputText();
        assertTrue(result);
        
        //if Disaplay Format includes a link then don't escape
        instance.setDisplayFormat("'<a target=\"_blank\" href=\"http://www.rcsb.org/pdb/explore/explore.do?structureId=#VALUE\">PDB (RCSB) #VALUE</a>'");
        result = instance.isEscapeOutputText();
        assertFalse(result);  
        
        //if textbox then sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.TEXTBOX);
        result = instance.isEscapeOutputText();
        assertFalse( result);
        
        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        result = instance.isEscapeOutputText();
        assertTrue(result);
        
        //URL, too
        instance.setFieldType(DatasetFieldType.FieldType.URL);
        result = instance.isEscapeOutputText();
        assertEquals(false, result);
        
    }
    
    @Test
    public void testGetSolrField(){
        
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.DATE);
        SolrField solrField = instance.getSolrField();       
        assertEquals(SolrField.SolrType.DATE, solrField.getSolrType());
        
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        solrField = instance.getSolrField();       
        assertEquals(SolrField.SolrType.EMAIL, solrField.getSolrType());
        DatasetFieldType parent = new DatasetFieldType();
        parent.setAllowMultiples(true);
        instance.setParentDatasetFieldType(parent);
        solrField = instance.getSolrField();
        assertEquals(true, solrField.isAllowedToBeMultivalued());
        
    }
    
    private static final String header = "#datasetField\t" + String.join("\t", DatasetFieldType.Headers.keys());
    
    /**
     * This method simply inserts all the values from the map into a line, combined by \t and adds a "header" line before it.
     * It does this based on the {@link MetadataBlock.Headers} enum value order, which is the same as in the TSV definition.
     * Nonpresent values will be inserted as blank strings.
     *
     * @param values
     * @return
     */
    public static String generateDatasetFieldTSV(Map<DatasetFieldType.Headers, String> values) {
        List<String> fieldValues = Arrays.stream(DatasetFieldType.Headers.values())
            .map(k -> values.getOrDefault(k, ""))
            .collect(Collectors.toList());
        return header + settings.getFormat().getLineSeparatorString() + "\t" + String.join("\t", fieldValues);
    }
    
}
