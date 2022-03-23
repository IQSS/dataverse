/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import static edu.harvard.iq.dataverse.makedatacount.MakeDataCountUtil.LOG_HEADER;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

/**
 *
 * @author matthew
 */

@Named
@RequestScoped
public class MakeDataCountLoggingServiceBean {
    
    @EJB
    SystemConfig systemConfig;

    public void logEntry(MakeDataCountEntry entry) {
        if(systemConfig.getMDCLogPath() != null) {
            LoggingUtil.saveLogFileAppendWithHeader(entry.toString(), systemConfig.getMDCLogPath(), getLogFileName() , LOG_HEADER);
        }
    }
    
    public String getLogFileName() {
        return "counter_"+new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(new Date().getTime()))+".log";
    }
    
    public static class MakeDataCountEntry {
    
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

        public MakeDataCountEntry(FacesContext fc, DataverseRequestServiceBean dvRequestService, DatasetVersion publishedVersion) {
            if(fc != null) {
                HttpServletRequest req = (HttpServletRequest)fc.getExternalContext().getRequest();
                setRequestUrl(req.getRequestURI() + "?" + req.getQueryString());
                setTargetUrl(req.getRequestURI() + "?" + req.getQueryString());
                setUserAgent(req.getHeader("user-agent")); 
                HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);
                setSessionCookieId(session.getId());
            }

            if(publishedVersion != null) {
                setIdentifier(publishedVersion.getDataset().getGlobalId().asString());
                setAuthors(publishedVersion.getAuthorsStr(false).replace(";", "|"));
                
                //Note: These publisher/publisher-id values are fake. This is ok as currently Make Data Count
                //derives this info from your DataCite credentials, and DataCite has said we can take this route.
                //It may be possible to provide the correct information, which is client-id (DataCite client-id)
                //and the client-id value off of our account page, but Counter Processor seems limited in what is parses
                //for these values. As no one uses them, we are chosing to not pass real data at this point. --MAD 4.11
                setPublisher("grid");
                setPublisherId("tbd"); //"tbd" is a special case in counter processor that gives values that pass MDC.
                setTitle(publishedVersion.getTitle());
                setVersion(String.valueOf(publishedVersion.getVersionNumber()));
                
                Date releaseTime = publishedVersion.getReleaseTime();
                if(null == releaseTime) { //Seems to be null when called from Datasets api
                    releaseTime = publishedVersion.getLastUpdateTime();
                }
                setPublicationYear(new SimpleDateFormat("yyyy").format(releaseTime));
                
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");     
                    format.setTimeZone(TimeZone.getTimeZone("GMT"));
                setPublicationDate(format.format(releaseTime));
            }

            if(dvRequestService != null) {
                setClientIp(String.valueOf(dvRequestService.getDataverseRequest().getSourceAddress()));
                setUserId(dvRequestService.getDataverseRequest().getUser().getIdentifier());
            }

            setEventTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Timestamp(new Date().getTime())));
        }

        //This version of the constructor is for the downloads tracked in FileDownloadServiceBean
        //Technically you should be able to get to publishedVersion via the data file, but guestbook's datafile doesn't have that info
        //This is passed a DataFile to log the file downloaded
        public MakeDataCountEntry(FacesContext fc, DataverseRequestServiceBean dvRequestService, DatasetVersion publishedVersion, DataFile df) {
            this(fc, dvRequestService, publishedVersion);

            setFilename(df.getStorageIdentifier());
            setSize(String.valueOf(df.getFilesize()));
            
            //Note that at the time of writing, Guestbook is the only one calling this and it does a manual change on its end
            //This command: entry.setTargetUrl("/api/access/datafile/" + guestbookResponse.getDataFile().getId());
            //--MAD 4.10
        }    

        //Exception thrown if no published metadata exists for DataFile
        //This is passed a DataFile to log the file downloaded. uriInfo and headers are passed in lieu of FacesContext
        public MakeDataCountEntry(UriInfo uriInfo, HttpHeaders headers, DataverseRequestServiceBean dvRequestService, DataFile df) throws UnsupportedOperationException{
            this(null, dvRequestService, df.getLatestPublishedFileMetadata().getDatasetVersion());
            
            if(uriInfo != null) {
                setRequestUrl(uriInfo.getRequestUri().toString());
                setTargetUrl(uriInfo.getRequestUri().toString());
            }
            if(null != headers && null != headers.getRequestHeader("user-agent")) {
                setUserAgent(headers.getRequestHeader("user-agent").get(0));
            }
            
            setFilename(df.getStorageIdentifier()); 
            setSize(String.valueOf(df.getFilesize()));
        }
        
        //Originally used when downloading dataset metadata
        public MakeDataCountEntry(UriInfo uriInfo, HttpHeaders headers, DataverseRequestServiceBean dvRequestService, Dataset ds) throws UnsupportedOperationException{
            this(null, dvRequestService, ds.getReleasedVersion());
            
            if(uriInfo != null) {
                setRequestUrl(uriInfo.getRequestUri().toString());
                setTargetUrl(uriInfo.getRequestUri().toString());
            }
            if(null != headers && null != headers.getRequestHeader("user-agent")) {
                setUserAgent(headers.getRequestHeader("user-agent").get(0));
            }
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
        public final void setEventTime(String eventTime) {
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
        public final void setClientIp(String clientIp) {
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
        public final void setSessionCookieId(String sessionCookieId) {
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
        public final void setUserId(String userId) {
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
        public final void setRequestUrl(String requestUrl) {
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
        public final void setIdentifier(String identifier) {
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
        public final void setFilename(String filename) {
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
        public final void setSize(String size) {
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
        public final void setUserAgent(String userAgent) {
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
        public final void setTitle(String title) {
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
        public final void setPublisher(String publisher) {
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
        public final void setPublisherId(String publisherId) {
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
        public final void setAuthors(String authors) {
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
        public final void setPublicationDate(String publicationDate) {
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
        public final void setVersion(String version) {
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
        public final void setTargetUrl(String targetUrl) {
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
        public final void setPublicationYear(String publicationYear) {
            this.publicationYear = publicationYear;
        }

    }
}
