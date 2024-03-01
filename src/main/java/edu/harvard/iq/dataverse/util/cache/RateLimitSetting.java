package edu.harvard.iq.dataverse.util.cache;

import java.util.ArrayList;
import java.util.List;

public class RateLimitSetting {

    private int tier;
    private int limitPerHour = RateLimitUtil.NO_LIMIT;
    private List<String> actions = new ArrayList<>();

    private int defaultLimitPerHour;

    public RateLimitSetting() {}

    public void setTier(int tier) {
        this.tier = tier;
    }
    public int getTier() {
        return this.tier;
    }
    public void setLimitPerHour(int limitPerHour) {
        this.limitPerHour = limitPerHour;
    }
    public int getLimitPerHour() {
        return this.limitPerHour;
    }
    public void setActions(List<String> actions) {
        this.actions = actions;
    }
    public List<String> getActions() {
        return this.actions;
    }
    public void setDefaultLimit(int defaultLimitPerHour) {
        this.defaultLimitPerHour = defaultLimitPerHour;
    }
    public int getDefaultLimitPerHour() {
        return this.defaultLimitPerHour;
    }
}
