package edu.harvard.iq.dataverse.util.cache;

import com.hazelcast.cluster.Address;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.impl.ListDataverseContentCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListExplicitGroupsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListFacetsCommand;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.Tags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CacheFactoryBeanTest {
    private SystemConfig mockedSystemConfig;
    static CacheFactoryBean cache = null;

    AuthenticatedUser authUser = new AuthenticatedUser();
    GuestUser guestUser = GuestUser.get();
    static final String settingDefaultCapacity = "30,60,120";
    public String getJsonSetting() {
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
    @BeforeEach
    public void init() throws IOException {
        // Reuse cache and config for all tests
        if (cache == null) {
            mockedSystemConfig = mock(SystemConfig.class);
            doReturn(settingDefaultCapacity).when(mockedSystemConfig).getRateLimitingDefaultCapacityTiers();
            doReturn(getJsonSetting()).when(mockedSystemConfig).getRateLimitsJson();
            cache = new CacheFactoryBean();
            cache.systemConfig = mockedSystemConfig;
            if (cache.rateLimitCache == null) {
                cache.rateLimitCache = new TestCache(getConfig());
            }

            // Clear the static data, so it can be reloaded with the new mocked data
            RateLimitUtil.rateLimitMap.clear();
            RateLimitUtil.rateLimits.clear();
        }

        // Reset to default auth user
        authUser.setRateLimitTier(1);
        authUser.setSuperuser(false);
        authUser.setUserIdentifier("authUser");
    }

    @AfterAll
    public static void cleanup() {
        Hazelcast.shutdownAll();
    }
    @Test
    public void testGuestUserGettingRateLimited() {
        Command action = new ListDataverseContentCommand(null,null);
        boolean rateLimited = false;
        int cnt = 0;
        for (; cnt <100; cnt++) {
            rateLimited = !cache.checkRate(guestUser, action);
            if (rateLimited) {
                break;
            }
        }
        String key = RateLimitUtil.generateCacheKey(guestUser, action.getClass().getSimpleName());
        assertTrue(cache.rateLimitCache.containsKey(key));
        assertTrue(rateLimited && cnt > 1 && cnt <= 31, "rateLimited:"+rateLimited + " cnt:"+cnt);
    }

    @Test
    public void testAdminUserExemptFromGettingRateLimited() {
        Command action = new ListExplicitGroupsCommand(null,null);
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
        assertFalse(rateLimited);
        assertTrue(cnt >= 99, "cnt:"+cnt);
    }

    @Test
    @Tag(Tags.NOT_ESSENTIAL_UNITTESTS)
    @ResourceLock(value = "cache")
    public void testAuthenticatedUserGettingRateLimited() throws InterruptedException {
        Command action = new ListFacetsCommand(null,null);
        authUser.setRateLimitTier(2); // 120 cals per hour - 1 added token every 30 seconds
        boolean rateLimited = false;
        int cnt;
        for (cnt = 0; cnt <200; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertTrue(rateLimited);
        assertEquals(120, cnt);

        for (cnt = 0; cnt <60; cnt++) {
            Thread.sleep(1000);// Wait for bucket to be replenished (check each second for 1 minute max)
            rateLimited = !cache.checkRate(authUser, action);
            if (!rateLimited) {
                break;
            }
        }
        assertFalse(rateLimited, "rateLimited:"+rateLimited + " cnt:"+cnt);

        // Now change the user's tier, so it is no longer limited
        authUser.setRateLimitTier(3); // tier 3 = no limit
        for (cnt = 0; cnt <200; cnt++) {
            rateLimited = !cache.checkRate(authUser, action);
            if (rateLimited) {
                break;
            }
        }
        assertFalse(rateLimited);
        assertEquals(200, cnt);
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

    // convert Hazelcast IMap<String,String> to JCache Cache<Object, Object>
    private class TestCache implements Cache<String, String>{
        HazelcastInstance hzInstance;
        IMap<String, String> cache;
        TestCache(Config config) {
            hzInstance = Hazelcast.newHazelcastInstance(config);
            cache = hzInstance.getMap("test");
            Address address = hzInstance.getCluster().getLocalMember().getAddress();
            cache.put("memberAddress", String.format("%s:%d", address.getHost(), address.getPort()));
        }
        @Override
        public String get(String s) {
            return cache.get(s);
        }
        @Override
        public Map<String, String> getAll(Set<? extends String> set) {
            return null;
        }
        @Override
        public boolean containsKey(String s) {
            return get(s) != null;
        }
        @Override
        public void loadAll(Set<? extends String> set, boolean b, CompletionListener completionListener) {

        }
        @Override
        public void put(String s, String s2) {
            cache.put(s,s2);
        }
        @Override
        public String getAndPut(String s, String s2) {
            return null;
        }
        @Override
        public void putAll(Map<? extends String, ? extends String> map) {

        }
        @Override
        public boolean putIfAbsent(String s, String s2) {
            return false;
        }
        @Override
        public boolean remove(String s) {
            return false;
        }
        @Override
        public boolean remove(String s, String s2) {
            return false;
        }
        @Override
        public String getAndRemove(String s) {
            return null;
        }
        @Override
        public boolean replace(String s, String s2, String v1) {
            return false;
        }
        @Override
        public boolean replace(String s, String s2) {
            return false;
        }
        @Override
        public String getAndReplace(String s, String s2) {
            return null;
        }
        @Override
        public void removeAll(Set<? extends String> set) {

        }
        @Override
        public void removeAll() {

        }
        @Override
        public void clear() {
            cache.clear();
        }
        @Override
        public <C extends Configuration<String, String>> C getConfiguration(Class<C> aClass) {
            return null;
        }
        @Override
        public <T> T invoke(String s, EntryProcessor<String, String, T> entryProcessor, Object... objects) throws EntryProcessorException {
            return null;
        }
        @Override
        public <T> Map<String, EntryProcessorResult<T>> invokeAll(Set<? extends String> set, EntryProcessor<String, String, T> entryProcessor, Object... objects) {
            return null;
        }
        @Override
        public String getName() {
            return null;
        }
        @Override
        public CacheManager getCacheManager() {
            return null;
        }
        @Override
        public void close() {
            hzInstance.shutdown();
        }
        @Override
        public boolean isClosed() {
            return false;
        }
        @Override
        public <T> T unwrap(Class<T> aClass) {
            return null;
        }
        @Override
        public void registerCacheEntryListener(CacheEntryListenerConfiguration<String, String> cacheEntryListenerConfiguration) {

        }
        @Override
        public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<String, String> cacheEntryListenerConfiguration) {

        }
        @Override
        public Iterator<Cache.Entry<String, String>> iterator() {
            return null;
        }
    }
}
