package edu.harvard.iq.dataverse.util.cache;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class RateLimitUtil {
    private static final Logger logger = Logger.getLogger(RateLimitUtil.class.getCanonicalName());
    static final List<RateLimitSetting> rateLimits = new CopyOnWriteArrayList<>();
    static final Map<String, Integer> rateLimitMap = new ConcurrentHashMap<>();
    public static final int NO_LIMIT = -1;

    static String generateCacheKey(final User user, final String action) {
        return (user != null ? user.getIdentifier() : GuestUser.get().getIdentifier()) +
            (action != null ? ":" + action : "");
    }
    static int getCapacity(SystemConfig systemConfig, User user, String action) {
        if (user != null && user.isSuperuser()) {
            return NO_LIMIT;
        }
        // get the capacity, i.e. calls per hour, from config
        return (user instanceof AuthenticatedUser authUser) ?
                getCapacityByTierAndAction(systemConfig, authUser.getRateLimitTier(), action) :
                getCapacityByTierAndAction(systemConfig, 0, action);
    }
    static boolean rateLimited(final Cache<String, String> rateLimitCache, final String key, int capacityPerHour) {
        if (capacityPerHour == NO_LIMIT) {
            return false;
        }
        long currentTime = System.currentTimeMillis() / 60000L; // convert to minutes
        double tokensPerMinute = (capacityPerHour / 60.0);
        // Get the last time this bucket was added to
        final String keyLastUpdate = String.format("%s:last_update",key);
        long lastUpdate = longFromKey(rateLimitCache, keyLastUpdate);
        long deltaTime = currentTime - lastUpdate;
        // Get the current number of tokens in the bucket
        long tokens = longFromKey(rateLimitCache, key);
        long tokensToAdd = (long) (deltaTime * tokensPerMinute);
        if (tokensToAdd > 0) { // Don't update timestamp if we aren't adding any tokens to the bucket
            tokens = min(capacityPerHour, tokens + tokensToAdd);
            rateLimitCache.put(keyLastUpdate, String.valueOf(currentTime));
        }
        // Update with any added tokens and decrement 1 token for this call if not rate limited (0 tokens)
        rateLimitCache.put(key, String.valueOf(max(0, tokens-1)));
        return tokens < 1;
    }

    static int getCapacityByTierAndAction(SystemConfig systemConfig, Integer tier, String action) {
        if (rateLimits.isEmpty()) {
            init(systemConfig);
        }
        
        if (rateLimitMap.containsKey(getMapKey(tier, action))) {
            return rateLimitMap.get(getMapKey(tier,action));
        } else if (rateLimitMap.containsKey(getMapKey(tier))) {
            return rateLimitMap.get(getMapKey(tier));
        } else {
            return getCapacityByTier(systemConfig, tier);
        }
    }
    static int getCapacityByTier(SystemConfig systemConfig, int tier) {
        int value = NO_LIMIT;
        String csvString = systemConfig.getRateLimitingDefaultCapacityTiers();
        try {
            if (!csvString.isEmpty()) {
                int[] values = Arrays.stream(csvString.split(",")).mapToInt(Integer::parseInt).toArray();
                if (tier < values.length) {
                    value = values[tier];
                }
            }
        } catch (NumberFormatException nfe) {
            logger.warning(nfe.getMessage());
        }
        return value;
    }
    static void init(SystemConfig systemConfig) {
        getRateLimitsFromJson(systemConfig);
        /* Convert the List of Rate Limit Settings containing a list of Actions to a fast lookup Map where the key is:
             for default if no action defined: "{tier}:" and the value is the default limit for the tier
             for each action: "{tier}:{action}" and the value is the limit defined in the setting
        */
        rateLimitMap.clear();
        rateLimits.forEach(r -> {
            r.setDefaultLimit(getCapacityByTier(systemConfig, r.getTier()));
            rateLimitMap.put(getMapKey(r.getTier()), r.getDefaultLimitPerHour());
            r.getActions().forEach(a -> rateLimitMap.put(getMapKey(r.getTier(), a), r.getLimitPerHour()));
        });
    }
    
    @SuppressWarnings("java:S2133") // <- To enable casting to generic in JSON-B we need a class instance, false positive
    static void getRateLimitsFromJson(SystemConfig systemConfig) {
        String setting = systemConfig.getRateLimitsJson();
        rateLimits.clear();
        if (!setting.isEmpty()) {
            try (Jsonb jsonb = JsonbBuilder.create()) {
                rateLimits.addAll(jsonb.fromJson(setting,
                        new ArrayList<RateLimitSetting>() {}.getClass().getGenericSuperclass()));
            } catch (JsonbException e) {
                logger.warning("Unable to parse Rate Limit Json: " + e.getLocalizedMessage() + "   Json:(" + setting + ")");
                rateLimits.add(new RateLimitSetting()); // add a default entry to prevent re-initialization
            // Note: Usually using Exception in a catch block is an antipattern and should be avoided.
            //       As the JSON-B interface does not specify a non-generic type, we have to use this.
            } catch (Exception e) {
                logger.warning("Could not close JSON-B reader");
            }
        }
    }
    static String getMapKey(int tier) {
        return getMapKey(tier, null);
    }
    static String getMapKey(int tier, String action) {
        return tier + ":" + (action != null ? action : "");
    }
    static long longFromKey(Cache<String, String> cache, String key) {
        Object l = cache.get(key);
        return l != null ? Long.parseLong(String.valueOf(l)) : 0L;
    }
}
