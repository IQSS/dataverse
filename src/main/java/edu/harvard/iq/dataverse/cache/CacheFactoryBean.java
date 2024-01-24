package edu.harvard.iq.dataverse.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.logging.Logger;

@Singleton
@Startup
public class CacheFactoryBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(CacheFactoryBean.class.getCanonicalName());
    private static JedisPool jedisPool = null;
    @EJB
    SystemConfig systemConfig;

    @PostConstruct
    public void init() {
        logger.info("CacheFactoryBean.init Redis Host:Port " + systemConfig.getRedisBaseHost() + ":" + systemConfig.getRedisBasePort());
        jedisPool = new JedisPool(new JedisPoolConfig(), systemConfig.getRedisBaseHost(), Integer.valueOf(systemConfig.getRedisBasePort()),
                systemConfig.getRedisUser(), systemConfig.getRedisPassword());
    }
    @Override
    protected void finalize() throws Throwable {
        if (jedisPool != null) {
            jedisPool.close();
        }
        super.finalize();
    }

    /**
     * Check if user can make this call or if they are rate limited
     * @param user
     * @param action
     * @return true if user is superuser or rate not limited
     */
    public boolean checkRate(User user, String action) {
        if (user != null && user.isSuperuser()) {
            return true;
        };
        StringBuffer id = new StringBuffer();
        id.append(user != null ? user.getIdentifier() : GuestUser.get().getIdentifier());
        if (action != null) {
            id.append(":").append(action);
        }

        // get the capacity, i.e. calls per hour, from config
        int capacity = (user instanceof AuthenticatedUser) ?
                RateLimitUtil.getCapacityByTier(systemConfig, ((AuthenticatedUser) user).getRateLimitTier()) :
                RateLimitUtil.getCapacityByTier(systemConfig, 0);
        return (!RateLimitUtil.rateLimited(jedisPool, id.toString(), capacity));
    }
}
