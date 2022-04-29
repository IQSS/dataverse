package cli.util.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Predicate;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class FieldTest {
    
    private static final Logger logger = Logger.getLogger(BlockTest.class.getCanonicalName());
    
    @Nested
    class TypesTest {
        Predicate<String> allowedTypes = Field.Types.matchesTypes();
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"foobar", "hello_hello", "NONE", "DATE"})
        void failing(String subject) {
            assertFalse(allowedTypes.test(subject));
        }
    
        @ParameterizedTest
        @ValueSource(strings = {"none", "text", "textbox", "date", "email", "int", "float"})
        void succeeding(String subject) {
            assertTrue(allowedTypes.test(subject));
        }
    }
    
    @Nested
    class HeaderTest {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "_foobar_", "_foo_bar_"})
        void invalidNames(String subject) {
            assertFalse(Field.Header.NAME.isValid(subject));
        }
    
        @ParameterizedTest
        @ValueSource(strings = {"foobar_", "foo_bar_", "_foobar", "_foo_bar", "foobar", "foobar1234", "foo_bar_1234"})
        void validNames(String subject) {
            assertTrue(Field.Header.NAME.isValid(subject));
            assertTrue(Field.Header.PARENT.isValid(subject));
        }
    
        @ParameterizedTest
        @EmptySource
        void validParentName(String subject) {
            assertTrue(Field.Header.PARENT.isValid(subject));
        }
    
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"   ", "\t"})
        void invalidEmptyOrText(String subject) {
            assertFalse(Field.Header.WATERMARK.isValid(subject));
            assertFalse(Field.Header.DISPLAY_FORMAT.isValid(subject));
        }
    
        @ParameterizedTest
        @ValueSource(strings = { "", "foobar", "My name is Hase, I know about nothing."})
        void validEmptyOrText(String subject) {
            assertTrue(Field.Header.WATERMARK.isValid(subject));
            assertTrue(Field.Header.DISPLAY_FORMAT.isValid(subject));
        }
    
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "true", "false", "0", "1", "foobar"})
        void invalidBoolean(String subject) {
            assertFalse(Field.Header.ADVANCED_SEARCH_FIELD.isValid(subject));
            assertFalse(Field.Header.ALLOW_CONTROLLED_VOCABULARY.isValid(subject));
            assertFalse(Field.Header.ALLOW_MULTIPLES.isValid(subject));
            assertFalse(Field.Header.FACETABLE.isValid(subject));
            assertFalse(Field.Header.DISPLAY_ON_CREATE.isValid(subject));
            assertFalse(Field.Header.REQUIRED.isValid(subject));
        }
    
        @ParameterizedTest
        @ValueSource(strings = { "TRUE", "FALSE" })
        void validBoolean(String subject) {
            assertTrue(Field.Header.ADVANCED_SEARCH_FIELD.isValid(subject));
            assertTrue(Field.Header.ALLOW_CONTROLLED_VOCABULARY.isValid(subject));
            assertTrue(Field.Header.ALLOW_MULTIPLES.isValid(subject));
            assertTrue(Field.Header.FACETABLE.isValid(subject));
            assertTrue(Field.Header.DISPLAY_ON_CREATE.isValid(subject));
            assertTrue(Field.Header.REQUIRED.isValid(subject));
        }
    }
}