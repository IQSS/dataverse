package edu.harvard.iq.dataverse.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RateLimitUtilTest {

    private SystemConfig mockedSystemConfig;

    static final String settingJson = "{\n" +
            "  \"rateLimits\":[\n" +
            "    {\n" +
            "      \"tier\": 0,\n" +
            "      \"limitPerHour\": 10,\n" +
            "      \"actions\": [\n" +
            "        \"GetLatestPublishedDatasetVersionCommand\",\n" +
            "        \"GetPrivateUrlCommand\",\n" +
            "        \"GetDatasetCommand\",\n" +
            "        \"GetLatestAccessibleDatasetVersionCommand\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"tier\": 0,\n" +
            "      \"limitPerHour\": 1,\n" +
            "      \"actions\": [\n" +
            "        \"CreateGuestbookResponseCommand\",\n" +
            "        \"UpdateDatasetVersionCommand\",\n" +
            "        \"DestroyDatasetCommand\",\n" +
            "        \"DeleteDataFileCommand\",\n" +
            "        \"FinalizeDatasetPublicationCommand\",\n" +
            "        \"PublishDatasetCommand\"\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"tier\": 1,\n" +
            "      \"limitPerHour\": 30,\n" +
            "      \"actions\": [\n" +
            "        \"CreateGuestbookResponseCommand\",\n" +
            "        \"GetLatestPublishedDatasetVersionCommand\",\n" +
            "        \"GetPrivateUrlCommand\",\n" +
            "        \"GetDatasetCommand\",\n" +
            "        \"GetLatestAccessibleDatasetVersionCommand\",\n" +
            "        \"UpdateDatasetVersionCommand\",\n" +
            "        \"DestroyDatasetCommand\",\n" +
            "        \"DeleteDataFileCommand\",\n" +
            "        \"FinalizeDatasetPublicationCommand\",\n" +
            "        \"PublishDatasetCommand\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    static final String settingJsonBad = "{\n";

    @BeforeEach
    public void setup() {
        mockedSystemConfig = mock(SystemConfig.class);
        doReturn(100).when(mockedSystemConfig).getIntFromCSVStringOrDefault(eq(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers),eq(0), eq(RateLimitUtil.NO_LIMIT));
        doReturn(200).when(mockedSystemConfig).getIntFromCSVStringOrDefault(eq(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers),eq(1), eq(RateLimitUtil.NO_LIMIT));
        doReturn(RateLimitUtil.NO_LIMIT).when(mockedSystemConfig).getIntFromCSVStringOrDefault(eq(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers),eq(2), eq(RateLimitUtil.NO_LIMIT));
        // clear the static data so it can be reloaded with the new mocked data
        RateLimitUtil.rateLimitMap.clear();
        RateLimitUtil.rateLimits.clear();
    }
    @Test
    public void testConfig() {
        doReturn(settingJson).when(mockedSystemConfig).getRateLimitsJson();
        assertEquals(100, RateLimitUtil.getCapacityByTier(mockedSystemConfig, 0));
        assertEquals(200, RateLimitUtil.getCapacityByTier(mockedSystemConfig, 1));
        assertEquals(1, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 0, "DestroyDatasetCommand"));
        assertEquals(100, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 0, "Default Limit"));

        assertEquals(30, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 1, "GetLatestAccessibleDatasetVersionCommand"));
        assertEquals(200, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 1, "Default Limit"));

        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 2, "Default No Limit"));
    }
    @Test
    public void testBadJson() {
        doReturn(settingJsonBad).when(mockedSystemConfig).getRateLimitsJson();
        assertEquals(100, RateLimitUtil.getCapacityByTier(mockedSystemConfig, 0));
        assertEquals(200, RateLimitUtil.getCapacityByTier(mockedSystemConfig, 1));
        assertEquals(100, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 0, "GetLatestAccessibleDatasetVersionCommand"));
        assertEquals(200, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 1, "GetLatestAccessibleDatasetVersionCommand"));
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacityByTierAndAction(mockedSystemConfig, 2, "GetLatestAccessibleDatasetVersionCommand"));
    }

    @Test
    public void testGenerateCacheKey() {
        User user = GuestUser.get();
        assertEquals(RateLimitUtil.generateCacheKey(user,"action1"), ":guest:action1");
    }
    @Test
    public void testGetCapacity() {
        doReturn(settingJson).when(mockedSystemConfig).getRateLimitsJson();
        GuestUser guestUser = GuestUser.get();
        assertEquals(10, RateLimitUtil.getCapacity(mockedSystemConfig, guestUser, "GetPrivateUrlCommand"));

        AuthenticatedUser authUser = new AuthenticatedUser();
        authUser.setRateLimitTier(1);
        assertEquals(30, RateLimitUtil.getCapacity(mockedSystemConfig, authUser, "GetPrivateUrlCommand"));
        authUser.setSuperuser(true);
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(mockedSystemConfig, authUser, "GetPrivateUrlCommand"));
    }
}
