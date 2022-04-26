package cli.util.model;

import cli.util.TsvBlockReader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {
    
    private static final Logger logger = Logger.getLogger(ValidatorTest.class.getCanonicalName());
    
    @Nested
    class UtilsTest {
        @ParameterizedTest
        @CsvSource(nullValues = "NULL",
            value = {
                "NULL,NULL",
                "hello,hello",
                "'   hello','   hello'",
                "'   hello   ','   hello   '",
                "'   hello','   hello\t\t\t'",
                "'\t\t\thello','\t\t\thello\t\t\t'",
                "'\t\t\thello\ttest','\t\t\thello\ttest\t\t'",
                "'\t\t\thello\ttest\t\t ','\t\t\thello\ttest\t\t '",
            })
        void trimming(String expected, String sut) {
            assertEquals(expected, TsvBlockReader.rtrimColumns(sut));
        }
        
        @ParameterizedTest
        @CsvSource(nullValues = "NULL",
            value = {
                "false,NULL",
                "false,''",
                "false,hello",
                "false,https://",
                "false,www.foo.bar",
                "false,://foo.bar.com",
                "true,https://wwww.foobar.com",
                "true,https://wwww.foobar.com/hello",
                "true,https://wwww.foobar.com:1214/hello",
                "true,https://host/hello",
            })
        void urlValidation(boolean expected, String sut) {
            assertEquals(expected, Validator.isValidUrl(sut));
        }
    }
    
    @Nested
    class ValidateBlockHeader {
        List<String> blockHeaders = Block.Header.getHeaders();
    
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
        void validateHeaderLine_Block_Throws(String line) {
            ParserException exception = assertThrows(ParserException.class, () -> Validator.validateHeaderLine(line, Block.TRIGGER, blockHeaders));
            assertTrue(exception.hasSubExceptions());
            logger.log(Level.FINE,
                exception.getSubExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining("\n"))
            );
        }
    
        @ParameterizedTest
        @ValueSource(strings = {
            "#metadataBlock\tname\tdataverseAlias\tdisplayName\tblockURI",
            "#metadataBlock\tNAME\tDataversealias\tDisplayname\tBlockURI"
        })
        void validateHeaderLine_Block_True(String line) throws ParserException {
            List<String> headers = Validator.validateHeaderLine(line, Block.TRIGGER, blockHeaders);
            assertFalse(headers.isEmpty());
            // we expect the normalized form, so the arrays should match!
            assertEquals(blockHeaders, headers);
        }
    }
}