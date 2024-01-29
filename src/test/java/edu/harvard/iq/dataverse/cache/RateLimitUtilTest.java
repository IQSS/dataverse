package edu.harvard.iq.dataverse.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class RateLimitUtilTest {

    @Mock
    SystemConfig systemConfig;

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
        lenient().doReturn(100).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(0), anyInt());
        lenient().doReturn(200).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(1), anyInt());
        lenient().doReturn(RateLimitUtil.NO_LIMIT).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(2), anyInt());
        RateLimitUtil.rateLimitMap.clear();
        RateLimitUtil.rateLimits.clear();
    }
    @Test
    public void testConfig() {
        lenient().doReturn(settingJson).when(systemConfig).getRateLimitsJson();
        assertEquals(100, RateLimitUtil.getCapacityByTier(systemConfig, 0));
        assertEquals(200, RateLimitUtil.getCapacityByTier(systemConfig, 1));
        assertEquals(1, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 0, "DestroyDatasetCommand"));
        assertEquals(100, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 0, "Default Limit"));

        assertEquals(30, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 1, "GetLatestAccessibleDatasetVersionCommand"));
        assertEquals(200, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 1, "Default Limit"));

        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 2, "Default No Limit"));
    }
    @Test
    public void testBadJson() {
        lenient().doReturn(settingJsonBad).when(systemConfig).getRateLimitsJson();
        assertEquals(100, RateLimitUtil.getCapacityByTier(systemConfig, 0));
        assertEquals(200, RateLimitUtil.getCapacityByTier(systemConfig, 1));
        assertEquals(100, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 0, "GetLatestAccessibleDatasetVersionCommand"));
        assertEquals(200, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 1, "GetLatestAccessibleDatasetVersionCommand"));
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacityByTierAndAction(systemConfig, 2, "GetLatestAccessibleDatasetVersionCommand"));
    }

    @Test
    public void testGenerateCacheKey() {
        User user = GuestUser.get();
        assertEquals(RateLimitUtil.generateCacheKey(user,"action1"), ":guest:action1");
    }
    @Test
    public void testGetCapacity() {
        lenient().doReturn(settingJson).when(systemConfig).getRateLimitsJson();
        GuestUser guestUser = GuestUser.get();
        assertEquals(10, RateLimitUtil.getCapacity(systemConfig, guestUser, "GetPrivateUrlCommand"));

        AuthenticatedUser authUser = new AuthenticatedUser();
        authUser.setRateLimitTier(1);
        assertEquals(30, RateLimitUtil.getCapacity(systemConfig, authUser, "GetPrivateUrlCommand"));
        authUser.setSuperuser(true);
        assertEquals(RateLimitUtil.NO_LIMIT, RateLimitUtil.getCapacity(systemConfig, authUser, "GetPrivateUrlCommand"));
    }
}
