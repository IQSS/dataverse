package edu.harvard.iq.dataverse.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CacheFactoryBeanTest {

    private SystemConfig mockedSystemConfig;
    static CacheFactoryBean cache = null;
    // Second instance for cluster testing
    static CacheFactoryBean cache2 = null;
    AuthenticatedUser authUser = new AuthenticatedUser();
    GuestUser guestUser = GuestUser.get();
    String action;
    static final String staticHazelcastSystemProperties = "dataverse.hazelcast.";
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

    @BeforeAll
    public static void setup() {
        System.setProperty(staticHazelcastSystemProperties + "cluster", "dataverse-test");
        if (System.getenv("JENKINS_HOME") != null) {
            System.setProperty(staticHazelcastSystemProperties + "join", "AWS");
        } else {
            System.setProperty(staticHazelcastSystemProperties + "join", "Multicast");
        }
        //System.setProperty(staticHazelcastSystemProperties + "join", "TcpIp");
        //System.setProperty(staticHazelcastSystemProperties + "members", "localhost:5701,localhost:5702");
    }
    @BeforeEach
    public void init() throws IOException {
        // reuse cache and config for all tests
        if (cache == null) {
            mockedSystemConfig = mock(SystemConfig.class);
            doReturn(settingDefaultCapacity).when(mockedSystemConfig).getRateLimitingDefaultCapacityTiers();
            doReturn(settingJson).when(mockedSystemConfig).getRateLimitsJson();
            cache = new CacheFactoryBean();
            cache.systemConfig = mockedSystemConfig;
            cache.init(); // PostConstruct - start Hazelcast

            // clear the static data so it can be reloaded with the new mocked data
            RateLimitUtil.rateLimitMap.clear();
            RateLimitUtil.rateLimits.clear();

            // testing cache implementation and code coverage
            final String cacheKey = "CacheTestKey" + UUID.randomUUID();
            final String cacheValue = "CacheTestValue" + UUID.randomUUID();
            long cacheSize = cache.getCacheSize(cache.RATE_LIMIT_CACHE);
            cache.setCacheValue(cache.RATE_LIMIT_CACHE, cacheKey,cacheValue);
            assertTrue(cache.getCacheSize(cache.RATE_LIMIT_CACHE) > cacheSize);
            Object cacheValueObj = cache.getCacheValue(cache.RATE_LIMIT_CACHE, cacheKey);
            assertTrue(cacheValueObj != null && cacheValue.equalsIgnoreCase((String) cacheValueObj));
        }

        // reset to default auth user
        authUser.setRateLimitTier(1);
        authUser.setSuperuser(false);
        authUser.setUserIdentifier("authUser");

        // create a unique action for each test
        action = "cmd-" + UUID.randomUUID();
    }

    @AfterAll
    public static void cleanup() {
        if (cache != null) {
            cache.cleanup(); // PreDestroy - shutdown Hazelcast
            cache = null;
        }
        if (cache2 != null) {
            cache2.cleanup(); // PreDestroy - shutdown Hazelcast
            cache2 = null;
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

    @Test
    public void testCluster() {
        //make sure at least 1 entry is in the original cache
        cache.checkRate(authUser, action);

        // create a second cache to test cluster
        cache2 = new CacheFactoryBean();
        cache2.systemConfig = mockedSystemConfig;
        cache2.init(); // PostConstruct - start Hazelcast

        // check to see if the new cache synced with the existing cache
        long s1 = cache.getCacheSize(CacheFactoryBean.RATE_LIMIT_CACHE);
        long s2 = cache2.getCacheSize(CacheFactoryBean.RATE_LIMIT_CACHE);
        assertTrue(s1 > 0 && s1 == s2, "Size1:" + s1 + " Size2:" + s2 );

        String key = "key1";
        String value = "value1";
        // verify that both caches stay in sync
        cache.setCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key, value);
        assertTrue(value.equals(cache2.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
        // clearing one cache also clears the other cache in the cluster
        cache2.clearCache(CacheFactoryBean.RATE_LIMIT_CACHE);
        assertTrue(String.valueOf(cache.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)).isEmpty());

        // verify no issue dropping one node from cluster
        cache2.setCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key, value);
        assertTrue(value.equals(cache2.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
        assertTrue(value.equals(cache.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
        cache2.cleanup(); // remove cache2
        assertTrue(value.equals(cache.getCacheValue(CacheFactoryBean.RATE_LIMIT_CACHE, key)));
    }
}
