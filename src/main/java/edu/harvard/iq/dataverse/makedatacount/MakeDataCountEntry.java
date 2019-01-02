/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.makedatacount;

/**
 *
 * @author matthew
 */
public class MakeDataCountEntry {
    
    private String eventTime;
    private String clientIp;
    private String sessionCookieId;
    private String userCookieId;
    private String userId;
    private String requestUrl;
    private String identifier;
    private String filename;
    private String size;
    private String userAgent;
    private String title;
    private String publisher;
    private String publisherId;
    private String authors;
    private String publicationDate;
    private String version;
    private String otherId;
    private String targetUrl;
    private String publicationYear;
    
    public MakeDataCountEntry() {
        
    }

    @Override
    public String toString() {
        return  getEventTime() + "\t" +
                getClientIp() + "\t" +
                getSessionCookieId() + "\t" +
                getUserCookieId() + "\t" +
                getUserId() + "\t" +
                getRequestUrl() + "\t" +
                getIdentifier() + "\t" +
                getFilename() + "\t" +
                getSize() + "\t" +
                getUserAgent() + "\t" +
                getTitle() + "\t" +
                getPublisher() + "\t" +
                getPublisherId() + "\t" +
                getAuthors() + "\t" +
                getPublicationDate() + "\t" +
                getVersion() + "\t" +
                getOtherId() + "\t" +
                getTargetUrl() + "\t" +
                getPublicationYear() + "\n";
    }
    
    /**
     * @return the eventTime
     */
    public String getEventTime() {
        if(eventTime == null) {
            return "-";
        }
        return eventTime;
    }

    /**
     * @param eventTime the eventTime to set
     */
    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    /**
     * @return the clientIp
     */
    public String getClientIp() {
        if(clientIp == null) {
            return "-";
        }
        return clientIp;
    }

    /**
     * @param clientIp the clientIp to set
     */
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    /**
     * @return the sessionCookieId
     */
    public String getSessionCookieId() {
        if(sessionCookieId == null) {
            return "-";
        }
        return sessionCookieId;
    }

    /**
     * @param sessionCookieId the sessionCookieId to set
     */
    public void setSessionCookieId(String sessionCookieId) {
        this.sessionCookieId = sessionCookieId;
    }

    /**
     * @return the uesrCookieId
     */
    public String getUserCookieId() {
        if(userCookieId == null) {
            return "-";
        }
        return userCookieId;
    }

    /**
     * @param uesrCookieId the uesrCookieId to set
     */
    public void setUserCookieId(String uesrCookieId) {
        this.userCookieId = uesrCookieId;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        if(userId == null) {
            return "-";
        }
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @return the requestUrl
     */
    public String getRequestUrl() {
        if(requestUrl == null) {
            return "-";
        }
        return requestUrl;
    }

    /**
     * @param requestUrl the requestUrl to set
     */
    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        if(identifier == null) {
            return "-";
        }
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        if(filename == null) {
            return "-";
        }
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the size
     */
    public String getSize() {
        if(size == null) {
            return "-";
        }
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(String size) {
        this.size = size;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        if(userAgent == null) {
            return "-";
        }
        return userAgent;
    }

    /**
     * @param userAgent the userAgent to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        if(title == null) {
            return "-";
        }
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the publisher
     */
    public String getPublisher() {
        if(publisher == null) {
            return "-";
        }
        return publisher;
    }

    /**
     * @param publisher the publisher to set
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * @return the publisherId
     */
    public String getPublisherId() {
        if(publisherId == null) {
            return "-";
        }
        return publisherId;
    }

    /**
     * @param publisherId the publisherId to set
     */
    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    /**
     * @return the authors
     */
    public String getAuthors() {
        if(authors == null) {
            return "-";
        }
        return authors;
    }

    /**
     * @param authors the authors to set
     */
    public void setAuthors(String authors) {
        this.authors = authors;
    }

    /**
     * @return the publicationDate
     */
    public String getPublicationDate() {
        if(publicationDate == null) {
            return "-";
        }
        return publicationDate;
    }

    /**
     * @param publicationDate the publicationDate to set
     */
    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        if(version == null) {
            return "-";
        }
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the otherId
     */
    public String getOtherId() {
        if(otherId == null) {
            return "-";
        }
        return otherId;
    }

    /**
     * @param otherId the otherId to set
     */
    public void setOtherId(String otherId) {
        this.otherId = otherId;
    }

    /**
     * @return the targetUrl
     */
    public String getTargetUrl() {
        if(targetUrl == null) {
            return "-";
        }
        return targetUrl;
    }

    /**
     * @param targetUrl the targetUrl to set
     */
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * @return the publicationYear
     */
    public String getPublicationYear() {
        if(publicationYear == null) {
            return "-";
        }
        return publicationYear;
    }

    /**
     * @param publicationYear the publicationYear to set
     */
    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }
}
