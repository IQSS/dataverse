package cli.util.model;

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
    
    @ParameterizedTest
    @ValueSource(strings = {
        "#metadataBlock\tname\tdataverseAlias\tdisplayName\tblockURI",
        "#metadataBlock\tNAME\tDataversealias\tDisplayname\tBlockURI"
    })
    void successfulParseAndValidateHeaderLine(String headerLine) throws ParserException {
        List<Block.Header> headers = Block.Header.parseAndValidate(headerLine);
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
        ParserException exception = assertThrows(ParserException.class, () -> Block.Header.parseAndValidate(headerLine));
        assertTrue(exception.hasSubExceptions());
        logger.log(Level.INFO,
            exception.getSubExceptions().stream().map(Throwable::getMessage).collect(Collectors.joining("\n"))
        );
    }
    
}