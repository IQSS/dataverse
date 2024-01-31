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

import java.util.Arrays;
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
    public enum JoinVia {
        Multicast, TcpIp, AWS, Azure;
    }
    @PostConstruct
    public void init() {
        if (hazelcastInstance == null) {
            hazelcastInstance = Hazelcast.newHazelcastInstance(getConfig());
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

    private Config getConfig() {
        JoinVia joinVia;
        try {
            String join = System.getProperty("dataverse.hazelcast.join", "Multicast");
            joinVia = JoinVia.valueOf(join);
        } catch (IllegalArgumentException e) {
            logger.warning("dataverse.hazelcast.join must be one of " + JoinVia.values() + ". Defaulting to Multicast");
            joinVia = JoinVia.Multicast;
        }
        Config config = new Config();
        config.setClusterName("dataverse");
        config.getJetConfig().setEnabled(true);
        if (joinVia == JoinVia.TcpIp) {
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            String members = System.getProperty("dataverse.hazelcast.members", "");
            logger.info("dataverse.hazelcast.members: " + members);
            try {
                Arrays.stream(members.split(",")).forEach(m ->
                        config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(m));
            } catch (IllegalArgumentException e) {
                logger.warning("dataverse.hazelcast.members must contain at least 1 'host:port' entry, Defaulting to Multicast");
                joinVia = JoinVia.Multicast;
            }
        }
        logger.info("dataverse.hazelcast.join:" + joinVia);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(joinVia == JoinVia.Multicast);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(joinVia == JoinVia.TcpIp);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(joinVia == JoinVia.AWS);
        config.getNetworkConfig().getJoin().getAzureConfig().setEnabled(joinVia == JoinVia.Azure);
        return config;
    }
}
