package edu.harvard.iq.dataverse.cache;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class RateLimitUtil {
    private static final Logger logger = Logger.getLogger(RateLimitUtil.class.getCanonicalName());
    protected static final List<RateLimitSetting> rateLimits = new CopyOnWriteArrayList<>();
    protected static final Map<String, Integer> rateLimitMap = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    public static final int NO_LIMIT = -1;

    protected static int getCapacityByTier(SystemConfig systemConfig, int tier) {
        return systemConfig.getIntFromCSVStringOrDefault(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers, tier, NO_LIMIT);
    }

    public static boolean rateLimited(final Map<String, String> cache, final String key, int capacityPerHour) {
        if (capacityPerHour == NO_LIMIT) {
            return false;
        }
        long currentTime = System.currentTimeMillis() / 60000L; // convert to minutes
        double tokensPerMinute = (capacityPerHour / 60.0);
        // Get the last time this bucket was added to
        final String keyLastUpdate = String.format("%s:last_update",key);
        long lastUpdate = longFromKey(cache, keyLastUpdate);
        long deltaTime = currentTime - lastUpdate;
        // Get the current number of tokens in the bucket
        long tokens = longFromKey(cache, key);
        long tokensToAdd = (long) (deltaTime * tokensPerMinute);
        if (tokensToAdd > 0) { // Don't update timestamp if we aren't adding any tokens to the bucket
            tokens = min(capacityPerHour, tokens + tokensToAdd);
            cache.put(keyLastUpdate, String.valueOf(currentTime));
        }
        // Update with any added tokens and decrement 1 token for this call if not rate limited (0 tokens)
        cache.put(key, String.valueOf(max(0, tokens-1)));
        return tokens < 1;
    }

    public static int getCapacityByTierAndAction(SystemConfig systemConfig, Integer tier, String action) {
        if (rateLimits.isEmpty()) {
            init(systemConfig);
        }

        return rateLimitMap.containsKey(getMapKey(tier,action)) ? rateLimitMap.get(getMapKey(tier,action)) :
                rateLimitMap.containsKey(getMapKey(tier)) ? rateLimitMap.get(getMapKey(tier)) :
                        getCapacityByTier(systemConfig, tier);
    }

    private static void init(SystemConfig systemConfig) {
        getRateLimitsFromJson(systemConfig);
        /* Convert the List of Rate Limit Settings containing a list of Actions to a fast lookup Map where the key is:
             for default if no action defined: "{tier}:" and the value is the default limit for the tier
             for each action: "{tier}:{action}" and the value is the limit defined in the setting
        */
        rateLimits.forEach(r -> {
            r.setDefaultLimit(getCapacityByTier(systemConfig, r.getTier()));
            rateLimitMap.put(getMapKey(r.getTier()), r.getDefaultLimitPerHour());
            r.getActions().forEach(a -> rateLimitMap.put(getMapKey(r.getTier(), a), r.getLimitPerHour()));
        });
    }

    private static void getRateLimitsFromJson(SystemConfig systemConfig) {
        String setting = systemConfig.getRateLimitsJson();
        if (!setting.isEmpty()) {
            try {
                JsonReader jr = Json.createReader(new StringReader(setting));
                JsonObject obj= jr.readObject();
                JsonArray lst = obj.getJsonArray("rateLimits");
                rateLimits.addAll(gson.fromJson(String.valueOf(lst),
                        new ArrayList<RateLimitSetting>() {}.getClass().getGenericSuperclass()));
            } catch (Exception e) {
                logger.warning("Unable to parse Rate Limit Json" + ": " + e.getLocalizedMessage());
                rateLimits.add(new RateLimitSetting()); // add a default entry to prevent re-initialization
                e.printStackTrace();
            }
        }
    }

    private static String getMapKey(Integer tier) {
        return getMapKey(tier, null);
    }

    private static String getMapKey(Integer tier, String action) {
        StringBuffer key = new StringBuffer();
        key.append(tier).append(":");
        if (action != null) {
            key.append(action);
        }
        return key.toString();
    }

    private static long longFromKey(Map cache, String key) {
        Object l = cache.get(key);
        return l != null ? Long.parseLong(String.valueOf(l)) : 0L;
    }
}
