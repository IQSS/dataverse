package edu.harvard.iq.dataverse.util.metadata;

import com.univocity.parsers.common.DataValidationException;
import com.univocity.parsers.common.processor.BeanListProcessor;
import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.MetadataBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MetadataBlockParsingTest {
    
    static BeanListProcessor<MetadataBlock> metadataBlockProcessor = new BeanListProcessor<>(MetadataBlock.class);
    static TsvParser parser;
    static TsvParserSettings settings = new TsvParserSettings();
    static final String LONGER_THAN_256_CHARS = "Jx7Agh8hSs4EwkCHzxwXQHOVYiL0i79n4hxeP1PbVRgkmRyUqB9dlFSoFbqCmoZ0OUCPHLOz" +
        "JMAZeTDxI3dj7QAQG6UuNBUaFDgyG40TRK6X3FiA0f8p4LZBHQC1HIbpIw7wiNmDoEfbrGHehAgbXWDDEXelGL4TXhSxHXIqfgNaLD9fNnk" +
        "XXcqNsuWMvkDQNrKhUWFQQybhHWS8jh62AjRWEvqFXvqVAnrgZ8xFnRiSpDkubsGuZWZqRFVN6wSPd9sp0GrpEWa5eCv0oFtQLHx0";
    
    @BeforeAll
    static void setUp() {
        settings.setProcessor(metadataBlockProcessor);
        settings.setHeaderExtractionEnabled(true);
        // TODO: replace this char with a global constant (introduced when creating the parsing bean)
        settings.getFormat().setComment('\'');
        parser = new TsvParser(settings);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"hello", "helloMyName", "hello_my_name", "h1234"})
    void setName_AllValid(String name) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.NAME, name,
                   MetadataBlock.Headers.DISPLAY_NAME, "display")));
        
        // when
        parser.parse(reader);
        List<MetadataBlock> blocks = metadataBlockProcessor.getBeans();
        
        // then
        assertEquals(1, blocks.size());
        assertEquals(name, blocks.get(0).getName());
    }
    
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"1234", "!", "hello+", "Hello", "hello-my_name_is", "what-s-up-5", "1234-foobar"})
    void setName_AllInvalid(String name) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.NAME, name,
                   MetadataBlock.Headers.DISPLAY_NAME, "display")));
        
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(reader));
    }
    
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"https://demo.dataverse.org/foobar", "http://demo.dataverse.org/foobar"})
    void setNamespaceUri_Valid(String uri) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.NAME, "test",
                   MetadataBlock.Headers.DISPLAY_NAME, "display",
                   MetadataBlock.Headers.NAMESPACE_URI, uri)));
    
        // when
        parser.parse(reader);
        List<MetadataBlock> blocks = metadataBlockProcessor.getBeans();
    
        // then
        assertEquals(1, blocks.size());
        assertEquals(uri, Optional.ofNullable(blocks.get(0).getNamespaceUri()).orElse(""));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"//demo.dataverse.org/foobar", "doi://demo.dataverse.org/foobar"})
    void setNamespaceUri_Invalid(String uri) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.NAME, "test",
                   MetadataBlock.Headers.DISPLAY_NAME, "display",
                   MetadataBlock.Headers.NAMESPACE_URI, uri)));
    
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(reader));
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"hello", "H 1234", "Hello this is my Name", "1234 Foo Bar Town", "DO NOT USE!!!"})
    void setDisplayName_AllValid(String displayName) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.DISPLAY_NAME, displayName,
                MetadataBlock.Headers.NAME, "test")));
        
        // when
        parser.parse(reader);
        List<MetadataBlock> blocks = metadataBlockProcessor.getBeans();
        
        // then
        assertEquals(1, blocks.size());
        assertEquals(displayName, blocks.get(0).getDisplayName());
    }
    
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" \t", "\t hello", "   Hello Hello", LONGER_THAN_256_CHARS})
    void setDisplayName_AllInvalid(String displayName) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.NAME, displayName,
                MetadataBlock.Headers.DISPLAY_NAME, "display")));
        
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(reader));
    }
    
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"hello", "HelloMyName", "hello_my_name_is", "hello-im-marc", "_foo-bar", "1234-test", "test-1234", "hello123"})
    void setOwner_AllValid(String owner) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.DISPLAY_NAME, "test",
                   MetadataBlock.Headers.NAME, "test",
                   MetadataBlock.Headers.OWNER, owner)));
        
        // when
        parser.parse(reader);
        List<MetadataBlock> blocks = metadataBlockProcessor.getBeans();
        
        // then
        assertEquals(1, blocks.size());
        if (!owner.isEmpty()) {
            assertNotNull(blocks.get(0).getOwner());
            assertTrue(blocks.get(0).getOwner() instanceof Dataverse);
            assertTrue(blocks.get(0).getOwner() instanceof Placeholder.Dataverse);
            assertEquals(owner, blocks.get(0).getOwner().getAlias());
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"1234", "hello+", "Hello!"})
    void setOwner_AllInvalid(String owner) {
        // given
        StringReader reader = new StringReader(generateMetadataBlockTSV(
            Map.of(MetadataBlock.Headers.OWNER, owner,
                   MetadataBlock.Headers.DISPLAY_NAME, "display",
                   MetadataBlock.Headers.NAME, "test")));
        
        // when & then
        assertThrows(DataValidationException.class, () -> parser.parse(reader));
    }
    
    private static final String header = "#metadatablock\t" + String.join("\t", MetadataBlock.Headers.keys());
    
    /**
     * This method simply inserts all the values from the map into a line, combined by \t and adds a "header" line before it.
     * It does this based on the {@link MetadataBlock.Headers} enum value order, which is the same as in the TSV definition.
     * Nonpresent values will be inserted as blank strings.
     *
     * @param values
     * @return
     */
    public static String generateMetadataBlockTSV(Map<MetadataBlock.Headers, String> values) {
        List<String> fieldValues = Arrays.stream(MetadataBlock.Headers.values())
                                         .map(k -> values.getOrDefault(k, ""))
                                         .collect(Collectors.toList());
        return header + settings.getFormat().getLineSeparatorString() + "\t" + String.join("\t", fieldValues);
    }
}