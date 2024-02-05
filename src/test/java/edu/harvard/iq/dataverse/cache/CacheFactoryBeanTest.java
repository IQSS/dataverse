package edu.harvard.iq.dataverse.cache;

import com.hazelcast.cluster.Address;
import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CacheFactoryBeanTest {
    private static final Logger logger = Logger.getLogger(CacheFactoryBeanTest.class.getCanonicalName());
    private SystemConfig mockedSystemConfig;
    static CacheFactoryBean cache = null;
    // Second instance for cluster testing
    static CacheFactoryBean cache2 = null;
    AuthenticatedUser authUser = new AuthenticatedUser();
    GuestUser guestUser = GuestUser.get();
    String action;
    static final String settingDefaultCapacity = "30,60,120";
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
    public void init() throws IOException {
        // Reuse cache and config for all tests
        if (cache == null) {
            mockedSystemConfig = mock(SystemConfig.class);
            doReturn(settingDefaultCapacity).when(mockedSystemConfig).getRateLimitingDefaultCapacityTiers();
            doReturn(settingJson).when(mockedSystemConfig).getRateLimitsJson();
            cache = new CacheFactoryBean();
            cache.systemConfig = mockedSystemConfig;
            if (cache.hzInstance == null) {
                cache.hzInstance = Hazelcast.newHazelcastInstance(getConfig());
            }
            cache.init(); // PostConstruct - set up Hazelcast

            // Clear the static data, so it can be reloaded with the new mocked data
            RateLimitUtil.rateLimitMap.clear();
            RateLimitUtil.rateLimits.clear();

            // Testing cache implementation and code coverage
            final String cacheKey = "CacheTestKey" + UUID.randomUUID();
            final String cacheValue = "CacheTestValue" + UUID.randomUUID();
            long cacheSize = cache.getCacheSize(cache.RATE_LIMIT_CACHE);
            cache.setCacheValue(cache.RATE_LIMIT_CACHE, cacheKey,cacheValue);
            assertTrue(cache.getCacheSize(cache.RATE_LIMIT_CACHE) > cacheSize);
            Object cacheValueObj = cache.getCacheValue(cache.RATE_LIMIT_CACHE, cacheKey);
            assertTrue(cacheValueObj != null && cacheValue.equalsIgnoreCase((String) cacheValueObj));
        }

        // Reset to default auth user
        authUser.setRateLimitTier(1);
        authUser.setSuperuser(false);
        authUser.setUserIdentifier("authUser");

        // Create a unique action for each test
        action = "cmd-" + UUID.randomUUID();
    }

    @AfterAll
    public static void cleanup() {
        if (cache != null && cache.hzInstance != null) {
            cache.hzInstance.shutdown();
        }
        if (cache2 != null && cache2.hzInstance != null) {
            cache2.hzInstance.shutdown();
        }
    }
    @Test
    public void testGuestUserGettingRateLimited() {
        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(guestUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(cache.getCacheSize(cache.RATE_LIMIT_CACHE) > 0);
        assertTrue(rateLimited && cnt > 1 && cnt <= 30, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }

    @Test
    public void testAdminUserExemptFromGettingRateLimited() {
        authUser.setSuperuser(true);
        authUser.setUserIdentifier("admin");
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
        authUser.setRateLimitTier(2); // 120 cals per hour - 1 added token every 30 seconds
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
            Thread.sleep(1000);// Wait for bucket to be replenished (check each second for 1 minute max)
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

    @Test
    public void testCluster() {
        // Make sure at least 1 entry is in the original cache
        cache.checkRate(authUser, action);

        // Create a second cache to test cluster
        cache2 = new CacheFactoryBean();
        cache2.systemConfig = mockedSystemConfig;
        if (cache2.hzInstance == null) {
            // Needed for Jenkins to form cluster based on TcpIp since Multicast fails
            Address initialCache = cache.hzInstance.getCluster().getLocalMember().getAddress();
            String members = String.format("%s:%d", initialCache.getHost(),initialCache.getPort());
            logger.info("Switching to TcpIp mode with members: " + members);
            cache2.hzInstance = Hazelcast.newHazelcastInstance(getConfig(members));
        }
        cache2.init(); // PostConstruct - set up Hazelcast

        // Check to see if the new cache synced with the existing cache
        long s1 = cache.getCacheSize(CacheFactoryBean.RATE_LIMIT_CACHE);
        long s2 = cache2.getCacheSize(CacheFactoryBean.RATE_LIMIT_CACHE);
        assertTrue(s1 > 0 && s1 == s2, "Size1:" + s1 + " Size2:" + s2 );

        String key = "key1";
        String value = "value1";
        // Verify that both caches stay in sync
        cache.setCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key, value);
        assertTrue(value.equals(cache2.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
        // Clearing one cache also clears the other cache in the cluster
        cache2.clearCache(CacheFactoryBean.RATE_LIMIT_CACHE);
        assertTrue(String.valueOf(cache.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)).isEmpty());

        // Verify no issue dropping one node from cluster
        cache2.setCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key, value);
        assertTrue(value.equals(cache2.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
        assertTrue(value.equals(cache.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
        // Shut down hazelcast on cache2 and make sure data is still available in original cache
        cache2.hzInstance.shutdown();
        cache2 = null;
        assertTrue(value.equals(cache.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
    }

    private Config getConfig() {
        return getConfig(null);
    }
    private Config getConfig(String members) {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAzureConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
        if (members != null) {
            config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(members);
        }
        return config;
    }
}
