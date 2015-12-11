/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import edu.harvard.iq.dataverse.usage.Event.EventType;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 *
 * @author luopc
 */
public class UsageSearchQuery {
    public enum DateHistogramInterval{YEAR,MONTH,DAY,HOUR}

    private DateHistogramInterval dateHistogramInterval;

    private List<EventType> eventTypes;
    private String ip;
    private String userId;
    private String userName;
    private String affiliation;
    private String position;

    private Date startTime;
    private Date endTime;
    
    private TimeZone timeZone = TimeZone.getDefault();

    private List<Long> dataverseIds;
    private List<Long> datasetIds;
    private List<Long> datafileIds;
    private List<Long> groupIds;

    private Long from = 0L;
    private Long size = 10L;

    public DateHistogramInterval getDateHistogramInterval() {
        return dateHistogramInterval;
    }

    public void setDateHistogramInterval(DateHistogramInterval dateHistogramInterval) {
        this.dateHistogramInterval = dateHistogramInterval;
    }

    public List<EventType> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(List<EventType> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public List<Long> getDataverseIds() {
        return dataverseIds;
    }

    public void setDataverseIds(List<Long> dataverseIds) {
        this.dataverseIds = dataverseIds;
    }

    public List<Long> getDatasetIds() {
        return datasetIds;
    }

    public void setDatasetIds(List<Long> datasetIds) {
        this.datasetIds = datasetIds;
    }

    public List<Long> getDatafileIds() {
        return datafileIds;
    }

    public void setDatafileIds(List<Long> datafileIds) {
        this.datafileIds = datafileIds;
    }

    public List<Long> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Long> groupIds) {
        this.groupIds = groupIds;
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }
}
