package edu.harvard.iq.dataverse.cache;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RateLimitSetting {

    @JsonProperty("tier")
    private int tier;
    @JsonProperty("limitPerHour")
    private int limitPerHour = RateLimitUtil.NO_LIMIT;
    @JsonProperty("actions")
    private List<String> rateLimitActions = new ArrayList<>();

    private int defaultLimitPerHour;

    public RateLimitSetting() {}

    @JsonProperty("tier")
    public void setTier(int tier) {
        this.tier = tier;
    }
    @JsonProperty("tier")
    public int getTier() {
        return this.tier;
    }
    @JsonProperty("limitPerHour")
    public void setLimitPerHour(int limitPerHour) {
        this.limitPerHour = limitPerHour;
    }
    @JsonProperty("limitPerHour")
    public int getLimitPerHour() {
        return this.limitPerHour;
    }
    @JsonProperty("actions")
    public void setRateLimitActions(List<String> rateLimitActions) {
        this.rateLimitActions = rateLimitActions;
    }
    @JsonProperty("actions")
    public List<String> getRateLimitActions() {
        return this.rateLimitActions;
    }
    public void setDefaultLimit(int defaultLimitPerHour) {
        this.defaultLimitPerHour = defaultLimitPerHour;
    }
    public int getDefaultLimitPerHour() {
        return this.defaultLimitPerHour;
    }
}
