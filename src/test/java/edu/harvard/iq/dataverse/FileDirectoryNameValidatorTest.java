package edu.harvard.iq.dataverse;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
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
            {false, "f**bar"},});
    }

    @Test
    public void testIsEmailValid() {
        assertEquals(isValid, FileDirectoryNameValidator.isFileDirectoryNameValid(fileDirectoryName, null));
    }

}
