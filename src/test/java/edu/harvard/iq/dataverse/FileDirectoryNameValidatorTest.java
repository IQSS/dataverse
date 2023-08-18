package edu.harvard.iq.dataverse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileDirectoryNameValidatorTest {

    @ParameterizedTest
    @CsvSource({
        "true,foobar",
        // The leading "-" gets stripped.
        "true,-foobar",
        "true,_foobar",
        "true,foobar_",
        "true,folder/sub",
        "true,folder///sub",
        "true,folder///sub/third",
        "false,f**bar"
    })
    public void testIsFileDirectoryNameValid(boolean isValid, String fileDirectoryName) {
        assertEquals(isValid, FileDirectoryNameValidator.isFileDirectoryNameValid(fileDirectoryName, null));
    }

}
