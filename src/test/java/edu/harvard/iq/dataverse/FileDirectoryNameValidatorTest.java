package edu.harvard.iq.dataverse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileDirectoryNameValidatorTest {

    public boolean isValid;
    public String fileDirectoryName;

    public FileDirectoryNameValidatorTest(boolean isValid, String fileDirectoryName) {
        this.isValid = isValid;
        this.fileDirectoryName = fileDirectoryName;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
            {true, "foobar"},
            // The leading "-" gets stripped.
            {true, "-foobar"},
            {true, "_foobar"},
            {true, "foobar_"},
            {true, "folder/sub"},
            {true, "folder///sub"},
            {true, "folder///sub/third"},
            {false, "f**bar"},});
    }

    @Test
    public void testIsFileDirectoryNameValid() {
        assertEquals(isValid, FileDirectoryNameValidator.isFileDirectoryNameValid(fileDirectoryName, null));
    }

}
