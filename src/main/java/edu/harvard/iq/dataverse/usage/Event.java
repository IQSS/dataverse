/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author luopc
 */
public class Event {
    public enum EventType{
        VIEW_DATAVERSE,
        VIEW_DATASET,
        DOWNLOAD_FILE,
        
        LOGIN,
        LOGOUT,
        
        REQUEST_ACCESS_FILE,
        GRANT_REQUEST_ACCESS_FILE,
        REJECT_REQUEST_ACCESS_FILE,
        
        REQUEST_JOIN_GROUP,
        ACCEPT_JOIN_GROUP,
        REJECT_JOIN_GROUP
    }
    
    //timestamp and event type
    private Date date;
    private EventType eventType;
    
    //http request information
    private String ip;
    private String userAgent;
    private String xforwardedfor;
    private String referrer;
    
    //user information
    private String userId;
    private String userName;
    private String affiliation;
    private String position;
    
    //iplocation
    private String continent;
    private String country;
    private String subdivision;
    private String city;
    private Double latitude;
    private Double longitude;
    
    
    //additional information: dataverse,dataset,datafile,group ID
    private Long dataverseId;
    private Long datasetId;
    private Long datafileId;
    private Long groupId;
    
    public Event addTimeStamp(Date date){
        this.date = date;
        return this;
    }
    
    public Event addType(EventType eventType){
        this.eventType = eventType;
        return this;
    }
    
    public Event addHttpInfo(HttpServletRequest request){
        this.ip = request.getRemoteAddr();
        this.userAgent = request.getHeader("User-Agent");
        this.xforwardedfor = request.getHeader("X-Forwarded-For");
        this.referrer = request.getHeader("referer");
        return this;
    }
    
    public Event addUserInfo(User user){
        if(!user.isAuthenticated()){
            userId = user.getIdentifier();
        }else{
            AuthenticatedUser aUser = (AuthenticatedUser)user;
            this.userId = aUser.getUserIdentifier();
            this.userName = aUser.getName();
            this.affiliation = aUser.getAffiliation();
            this.position = aUser.getPosition();
        }
        return this;
    }
    
    private static final String DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATE_TIME);

    public String toJson(){
        JsonObjectBuilder builder = jsonObjectBuilder();
        
        builder.add("date", dateTimeFormat.format(date));
        builder.add("eventType", eventType.toString());
        
        builder.add("ip",ip);
        if(userAgent != null)
            builder.add("userAgent", userAgent);
        if(xforwardedfor != null)
            builder.add("xforwardedfor", xforwardedfor);
        if(referrer != null)
            builder.add("referrer", referrer);
        
        if(userId != null)
            builder.add("userId", userId);
        if(userName != null)
            builder.add("userName", userName);
        if(affiliation != null)
            builder.add("affiliation", affiliation);
        if(position != null)
            builder.add("position", position);

        if(continent != null)
            builder.add("continent", continent);
        if(country != null)
            builder.add("country", country);
        if(subdivision != null)
            builder.add("subdivision", subdivision);
        if(city != null)
            builder.add("city", city);
        if(latitude != null)
            builder.add("latitude", latitude);
        if(longitude != null)
            builder.add("longitude", longitude);
        
        if(dataverseId != null)
            builder.add("dataverseId", dataverseId);
        if(datasetId != null)
            builder.add("datasetId", datasetId);
        if(datafileId != null)
            builder.add("datafileId", datafileId);
        if(groupId != null)
            builder.add("groupId", groupId);
        
        return builder.build().toString();
    }

    @Override
    public String toString(){
        return toJson();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getXforwardedfor() {
        return xforwardedfor;
    }

    public void setXforwardedfor(String xforwardedfor) {
        this.xforwardedfor = xforwardedfor;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
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

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSubdivision() {
        return subdivision;
    }

    public void setSubdivision(String subdivision) {
        this.subdivision = subdivision;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public Long getDatafileId() {
        return datafileId;
    }

    public void setDatafileId(Long datafileId) {
        this.datafileId = datafileId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

}
