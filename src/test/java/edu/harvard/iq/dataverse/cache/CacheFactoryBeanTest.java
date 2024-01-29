package edu.harvard.iq.dataverse.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class CacheFactoryBeanTest {

    @Mock
    SystemConfig systemConfig;
    @InjectMocks
    static CacheFactoryBean cache = new CacheFactoryBean();
    AuthenticatedUser authUser = new AuthenticatedUser();
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

    @BeforeEach
    public void setup() throws IOException {
        lenient().doReturn(30).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(0), anyInt());
        lenient().doReturn(60).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(1), anyInt());
        lenient().doReturn(120).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(2), anyInt());
        lenient().doReturn(RateLimitUtil.NO_LIMIT).when(systemConfig).getIntFromCSVStringOrDefault(any(),eq(3), anyInt());
        lenient().doReturn(settingJson).when(systemConfig).getRateLimitsJson();

        cache.init();
        authUser.setRateLimitTier(1); // reset to default

        // testing cache implementation and code coverage
        final String cacheKey = "CacheTestKey" + UUID.randomUUID();
        final String cacheValue = "CacheTestValue" + UUID.randomUUID();
        long cacheSize = cache.getCacheSize(cache.RATE_LIMIT_CACHE);
        cache.setCacheValue(cache.RATE_LIMIT_CACHE, cacheKey,cacheValue);
        assertTrue(cache.getCacheSize(cache.RATE_LIMIT_CACHE) > cacheSize);
        Object cacheValueObj = cache.getCacheValue(cache.RATE_LIMIT_CACHE, cacheKey);
        assertTrue(cacheValueObj != null && cacheValue.equalsIgnoreCase((String) cacheValueObj));
    }

    @Test
    public void testGuestUserGettingRateLimited() throws InterruptedException {
        User user = GuestUser.get();
        String action = "cmd-" + UUID.randomUUID();
        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(user, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(cache.getCacheSize(cache.RATE_LIMIT_CACHE) > 0);
        assertTrue(rateLimited && cnt > 1 && cnt <= 30, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }

    @Test
    public void testAdminUserExemptFromGettingRateLimited() throws InterruptedException {
        authUser.setSuperuser(true);
        authUser.setUserIdentifier("admin");
        String action = "cmd-" + UUID.randomUUID();
        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited && cnt >= 99, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }

    @Test
    public void testAuthenticatedUserGettingRateLimited() throws InterruptedException {
        authUser.setSuperuser(false);
        authUser.setUserIdentifier("authUser");
        authUser.setRateLimitTier(2); // 120 cals per hour - 1 added token every 30 seconds
        String action = "cmd-" + UUID.randomUUID();
        boolean rateLimited = false;
        int cnt;
        for (cnt = 0; cnt <200; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(rateLimited && cnt == 120, "rateLimited:"+rateLimited + " cnt:"+cnt);

        for (cnt = 0; cnt <60; cnt++) {
            Thread.sleep(1000);// wait for bucket to be replenished (check each second for 1 minute max)
            rateLimited = !cache.checkRate(authUser, action);
            if (!rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited, "rateLimited:"+rateLimited + " cnt:"+cnt);

        // Now change the user's tier, so it is no longer limited
        authUser.setRateLimitTier(3); // tier 3 = no limit
        for (cnt = 0; cnt <200; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(!rateLimited && cnt == 200, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }
}
