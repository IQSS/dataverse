package edu.harvard.iq.dataverse.util.cache;

import jakarta.json.bind.annotation.JsonbProperty;

import java.util.ArrayList;
import java.util.List;

public class RateLimitSetting {

    @JsonbProperty("tier")
    private int tier;
    @JsonbProperty("limitPerHour")
    private int limitPerHour = RateLimitUtil.NO_LIMIT;
    @JsonbProperty("actions")
    private List<String> actions = new ArrayList<>();

    private int defaultLimitPerHour;

    public RateLimitSetting() {}

    @JsonbProperty("tier")
    public void setTier(int tier) {
        this.tier = tier;
    }
    @JsonbProperty("tier")
    public int getTier() {
        return this.tier;
    }
    @JsonbProperty("limitPerHour")
    public void setLimitPerHour(int limitPerHour) {
        this.limitPerHour = limitPerHour;
    }
    @JsonbProperty("limitPerHour")
    public int getLimitPerHour() {
        return this.limitPerHour;
    }
    @JsonbProperty("actions")
    public void setActions(List<String> actions) {
        this.actions = actions;
    }
    @JsonbProperty("actions")
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
