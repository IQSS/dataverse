package edu.harvard.iq.dataverse.cache;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.logging.Logger;
import java.util.Map;

@Singleton
@Startup
public class CacheFactoryBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(CacheFactoryBean.class.getCanonicalName());
    private HazelcastInstance hazelcastInstance = null;
    private Map<String, String> rateLimitCache;
    @EJB
    SystemConfig systemConfig;

    public final static String RATE_LIMIT_CACHE = "rateLimitCache";

    @PostConstruct
    public void init() {
        if (hazelcastInstance == null) {
            // TODO: move config to a file (yml)
            Config config = new Config();
            config.setClusterName("dataverse");
            config.getJetConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("localhost:5701");
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("localhost:5702");
            config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getAzureConfig().setEnabled(false);
            //        .setProperty("tag-key", "my-ec2-instance-tag-key")
            //        .setProperty("tag-value", "my-ec2-instance-tag-value");
            hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            rateLimitCache = hazelcastInstance.getMap(RATE_LIMIT_CACHE);
        }
    }
    @PreDestroy
    protected void cleanup() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
            hazelcastInstance = null;
        }
    }
    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    /**
     * Check if user can make this call or if they are rate limited
     * @param user
     * @param action
     * @return true if user is superuser or rate not limited
     */
    public boolean checkRate(User user, String action) {
        int capacity = RateLimitUtil.getCapacity(systemConfig, user, action);
        if (capacity == RateLimitUtil.NO_LIMIT) {
            return true;
        } else {
            String cacheKey = RateLimitUtil.generateCacheKey(user, action);
            return (!RateLimitUtil.rateLimited(rateLimitCache, cacheKey, capacity));
        }
    }

    public long getCacheSize(String cacheName) {
        long cacheSize = 0;
        switch (cacheName) {
            case RATE_LIMIT_CACHE:
                cacheSize = rateLimitCache.size();
                break;
            default:
                break;
        }
        return cacheSize;
    }
    public Object getCacheValue(String cacheName, String key) {
        Object cacheValue = null;
        switch (cacheName) {
            case RATE_LIMIT_CACHE:
                cacheValue = rateLimitCache.containsKey(key) ? rateLimitCache.get(key) : "";
                break;
            default:
                break;
        }
        return cacheValue;
    }
    public void setCacheValue(String cacheName, String key, Object value) {
        switch (cacheName) {
            case RATE_LIMIT_CACHE:
                rateLimitCache.put(key, (String) value);
                break;
            default:
                break;
        }
    }
    public void clearCache(String cacheName) {
        switch (cacheName) {
            case RATE_LIMIT_CACHE:
                rateLimitCache.clear();
                break;
            default:
                break;
        }
    }
}
