package edu.harvard.iq.dataverse.util.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RateLimitUtilTest {

    static SystemConfig mockedSystemConfig = mock(SystemConfig.class);
    static SystemConfig mockedSystemConfigBad = mock(SystemConfig.class);

    static String getJsonSetting() {
        return """
             [
               {
                 "tier": 0,
                 "limitPerHour": 10,
                 "actions": [
                   "GetLatestPublishedDatasetVersionCommand",
                   "GetPrivateUrlCommand",
                   "GetDatasetCommand",
                   "GetLatestAccessibleDatasetVersionCommand"
                 ]
               },
               {
                 "tier": 0,
                 "limitPerHour": 1,
                 "actions": [
                   "CreateGuestbookResponseCommand",
                   "UpdateDatasetVersionCommand",
                   "DestroyDatasetCommand",
                   "DeleteDataFileCommand",
                   "FinalizeDatasetPublicationCommand",
                   "PublishDatasetCommand"
                 ]
               },
               {
                 "tier": 1,
                 "limitPerHour": 30,
                 "actions": [
                   "CreateGuestbookResponseCommand",
                   "GetLatestPublishedDatasetVersionCommand",
                   "GetPrivateUrlCommand",
                   "GetDatasetCommand",
                   "GetLatestAccessibleDatasetVersionCommand",
                   "UpdateDatasetVersionCommand",
                   "DestroyDatasetCommand",
                   "DeleteDataFileCommand",
                   "FinalizeDatasetPublicationCommand",
                   "PublishDatasetCommand"
                 ]
               }
             ]""";
    }
    static final String settingJsonBad = "{\n";

    @BeforeAll
    public static void setUp() {
        doReturn(getJsonSetting()).when(mockedSystemConfig).getRateLimitsJson();
        doReturn("100,200").when(mockedSystemConfig).getRateLimitingDefaultCapacityTiers();
        doReturn(settingJsonBad).when(mockedSystemConfigBad).getRateLimitsJson();
        doReturn("100,200").when(mockedSystemConfigBad).getRateLimitingDefaultCapacityTiers();
    }
    @BeforeEach
    public void resetRateLimitUtilSettings() {
        RateLimitUtil.rateLimitMap.clear();
        RateLimitUtil.rateLimits.clear();
    }
    @ParameterizedTest
    @CsvSource(value = {
            "100,0,",
            "200,1,",
            "1,0,DestroyDatasetCommand",
            "100,0,Default Limit",
            "30,1,DestroyDatasetCommand",
            "200,1,Default Limit",
            "-1,2,Default No Limit"
    })
    void testConfig(int exp, int tier, String action) {
        if (action == null) {
            assertEquals(exp, RateLimitUtil.getCapacityByTier(mockedSystemConfig, tier));
        } else {
            assertEquals(exp, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, tier, action));
        }
    }
    @ParameterizedTest
    @CsvSource(value = {
            "100,0,",
            "200,1,",
            "100,0,GetLatestAccessibleDatasetVersionCommand",
            "200,1,GetLatestAccessibleDatasetVersionCommand",
            "-1,2,GetLatestAccessibleDatasetVersionCommand"
    })
    void testBadJson(int exp, int tier, String action) {
        if (action == null) {
            assertEquals(exp, RateLimitUtil.getCapacityByTier(mockedSystemConfigBad, tier));
        } else {
            assertEquals(exp, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfigBad, tier, action));
        }
    }

    @Test
    public void testGenerateCacheKey() {
        User user = GuestUser.get();
        assertEquals(RateLimitUtil.generateCacheKey(user,"action1"), ":guest:action1");
    }
    @Test
    public void testGetCapacity() {
        SystemConfig config = mock(SystemConfig.class);
        resetRateLimitUtil(config, true);

        GuestUser guestUser = GuestUser.get();
        assertEquals(10, RateLimitUtil.getCapacity(config, guestUser, "GetPrivateUrlCommand"));

        AuthenticatedUser authUser = new AuthenticatedUser();
        authUser.setRateLimitTier(1);
        assertEquals(30, RateLimitUtil.getCapacity(config, authUser, "GetPrivateUrlCommand"));
        authUser.setSuperuser(true);
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(config, authUser, "GetPrivateUrlCommand"));

        // no setting means rate limiting is not on
        resetRateLimitUtil(config, false);

        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(config, guestUser, "GetPrivateUrlCommand"));
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(config, guestUser, "xyz"));
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(config, authUser, "GetPrivateUrlCommand"));
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(config, authUser, "abc"));
        authUser.setRateLimitTier(99);
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(config, authUser, "def"));
    }
    private void resetRateLimitUtil(SystemConfig config, boolean enable) {
        doReturn(enable ? getJsonSetting() : "").when(config).getRateLimitsJson();
        doReturn(enable ? "100,200" : "").when(config).getRateLimitingDefaultCapacityTiers();
        RateLimitUtil.rateLimitMap.clear();
        RateLimitUtil.rateLimits.clear();
    }
}
