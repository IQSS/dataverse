package cli.util.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockTest {
    
    private static final Logger logger = Logger.getLogger(BlockTest.class.getCanonicalName());
    
    static final Configuration config = Configuration.defaultConfig();
    static final String validHeaderLine = "#metadataBlock\tname\tdataverseAlias\tdisplayName\tblockURI";
    static final String validBlockDef = "\tmyblock\tdataverse\tFooBar Block\thttps://foobar.com/";
    
    @Nested
    class HeaderTest {
        @ParameterizedTest
        @ValueSource(strings = {
            validHeaderLine,
            "#metadataBlock\tNAME\tDataversealias\tDisplayname\tBlockURI"
        })
        void successfulParseAndValidateHeaderLine(String headerLine) throws ParserException {
            List<Block.Header> headers = Block.Header.parseAndValidate(headerLine, config);
            assertFalse(headers.isEmpty());
            assertEquals(List.of(Block.Header.values()), headers);
        }
    
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "hello",
            "#metadataBlock test",
            "#metadataBlock\tname\tdataverseAlias\tdisplayName",
            "\t#metadataBlock\tname\tdataverseAlias\tdisplayName\tblockURI",
            "#metadataBlock\tname\tdataverseAlias\tdisplayName\tblockURI\tfoobar",
            "#metadataBlock\tname\tdataverseAlias\tdisplayName\tdisplayName\tblockURI",
            "dataverseAlias\tdisplayName\tblockURI\t#metadataBlock\tname"
        })
        void failingParseAndValidateHeaderLine(String headerLine) throws ParserException {
            ParserException exception = assertThrows(ParserException.class, () -> Block.Header.parseAndValidate(headerLine, config));
            assertTrue(exception.hasSubExceptions());
            logger.log(Level.FINE,
                exception.getSubExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining("\n"))
            );
        }
    }
    
    @Nested
    class ParseLineTest {
        Block.BlockBuilder builder;
        
        @BeforeEach
        void setUp() throws ParserException {
            builder = new Block.BlockBuilder(validHeaderLine, config);
        }
    
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "\t",
            "myblock",
            "\tmyblock\t",
            "\tmyblock\tdataverse",
            "\tmyblock\tdataverse\tFooBar Block",
            "\tmyblock\tdataverse\tFooBar Block\thttps://",
            "\tmyblock\tdataverse\tFooBar Block\thttps://foobar.com/\thello",
            "myblock\tdataverse\tFooBar Block\thttps://foobar.com/"
        })
        void failingParseLine(String line) throws ParserException {
            ParserException exception = assertThrows(ParserException.class, () -> builder.parseAndValidateLine(line));
            assertFalse(builder.hasSucceeded());
        }
    
        @ParameterizedTest
        @ValueSource(strings = {
            validBlockDef
        })
        void succeedingParseLine(String line) throws ParserException {
            builder.parseAndValidateLine(line);
            assertTrue(builder.hasSucceeded());
        }
        
        @Test
        void failingDoubleAdditionAttempt() throws ParserException {
            builder.parseAndValidateLine(validBlockDef);
            assertTrue(builder.hasSucceeded());
            ParserException exception = assertThrows(ParserException.class, () -> builder.parseAndValidateLine(validBlockDef));
            assertFalse(builder.hasSucceeded());
        }
    }
    
}
