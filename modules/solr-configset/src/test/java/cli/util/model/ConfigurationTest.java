package cli.util.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
    
    private final Configuration testConfiguration = Configuration.defaultConfig();
    
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
        assertEquals(expected, testConfiguration.rtrimColumns(sut));
    }
}