package cli.util.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
    
}