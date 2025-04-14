package edu.harvard.iq.dataverse.util.cache;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import java.util.logging.Logger;

@Singleton
@Startup
public class CacheFactoryBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(CacheFactoryBean.class.getCanonicalName());
    // Retrieved from Hazelcast, implements ConcurrentMap and is threadsafe
    Cache<String, String> rateLimitCache;
    @EJB
    SystemConfig systemConfig;
    @Inject
    CacheManager manager;
    public final static String RATE_LIMIT_CACHE = "rateLimitCache";

    @PostConstruct
    public void init() {
        logger.severe(">>>> CacheFactoryBean init ");
        rateLimitCache = manager.getCache(RATE_LIMIT_CACHE);
        if (rateLimitCache == null) {
            CompleteConfiguration<String, String> config =
                    new MutableConfiguration<String, String>()
                            .setTypes( String.class, String.class );
            rateLimitCache = manager.createCache(RATE_LIMIT_CACHE, config);
            logger.severe(">>>> rateLimitCache.getClass() " + rateLimitCache.getClass().getName());
        }
    }

    /**
     * Check if user can make this call or if they are rate limited
     * @param user
     * @param command
     * @return true if user is superuser or rate not limited
     */
    public boolean checkRate(User user, Command command) {
        final String action = command.getClass().getSimpleName();
        int capacity = RateLimitUtil.getCapacity(systemConfig, user, action);
        if (capacity == RateLimitUtil.NO_LIMIT) {
            return true;
        } else if (capacity == RateLimitUtil.RESET_CACHE) {
            rateLimitCache.clear();
            return true;
        } else {
            String cacheKey = RateLimitUtil.generateCacheKey(user, action);
            return (!RateLimitUtil.rateLimited(rateLimitCache, cacheKey, capacity));
        }
    }
    public String getStats(String cacheType, String filter) {
        if (RATE_LIMIT_CACHE.equals(cacheType)) {
            return RateLimitUtil.getStats(rateLimitCache, filter);
        }
        return "";
    }
}
