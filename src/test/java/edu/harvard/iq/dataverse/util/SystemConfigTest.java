package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SystemConfigTest {

    @Mock
    private AuthenticationServiceBean authenticationServiceBean;

    @Mock
    private DataverseServiceBean dataverseServiceBean;

    @Mock
    private SettingsServiceBean settingsServiceBean;

    private SystemConfig systemConfig;

    @BeforeEach
    void setup() {
        systemConfig = new SystemConfig();
        systemConfig.authenticationService = authenticationServiceBean;
        systemConfig.dataverseService = dataverseServiceBean;
        systemConfig.settingsService = settingsServiceBean;
    }

    @ParameterizedTest
    @CsvSource({
            ", 5",
            "test, 5",
            "-10, -10",
            "0, 0",
            "10, 10"
    })
    void testGetLongLimitFromStringOrDefault(String inputString, long expectedResult) {
        long actualResult = systemConfig.getLongLimitFromStringOrDefault(inputString, 5L);
        assertEquals(expectedResult, actualResult);
    }

    @ParameterizedTest
    @CsvSource({
            ", 5",
            "test, 5",
            "-10, -10",
            "0, 0",
            "10, 10"
    })
    void testGetIntLimitFromStringOrDefault(String inputString, int expectedResult) {
        int actualResult = systemConfig.getIntLimitFromStringOrDefault(inputString, 5);
        assertEquals(expectedResult, actualResult);
    }
}
