/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 *
 * @author qqmyers
 */

@Named
@RequestScoped
public class FailedPIDResolutionLoggingServiceBean {
    
    public static final String LOG_HEADER = "#Fields: pid\trequestURI\tHTTP method\tclient_ip\teventTime\n";


    public void logEntry(FailedPIDResolutionEntry entry) {
            LoggingUtil.saveLogFileAppendWithHeader(entry.toString(), "../logs", getLogFileName(), LOG_HEADER);
    }

    public String getLogFileName() {
        return "PIDFailures_" + new SimpleDateFormat("yyyy-MM").format(new Timestamp(new Date().getTime())) + ".log";
    }

    public static class FailedPIDResolutionEntry {

        private String eventTime;
        private String clientIp;
        private String requestUrl;
        private String identifier;
        private String method;

        public FailedPIDResolutionEntry() {

        }

        public FailedPIDResolutionEntry(String persistentId, String requestURI, String method, IpAddress sourceAddress) {
            try {
                setIdentifier(URLEncoder.encode(persistentId, StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException e) {
                // Should never happen
                e.printStackTrace();
            }
            setRequestUrl(requestURI);
            setMethod(method);
            setClientIp(sourceAddress.toString());
            setEventTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Timestamp(new Date().getTime())));
        }

        @Override
        public String toString() {
            return getIdentifier() + "\t" +
                    getRequestUrl() + "\t" +
                    getMethod() + "\t" +
                    getClientIp() + "\t" +
                    getEventTime() + "\n";
        }

        /**
         * @return the eventTime
         */
        public String getEventTime() {
            if (eventTime == null) {
                return "-";
            }
            return eventTime;
        }

        /**
         * @param eventTime
         *            the eventTime to set
         */
        public final void setEventTime(String eventTime) {
            this.eventTime = eventTime;
        }

        /**
         * @return the clientIp
         */
        public String getClientIp() {
            if (clientIp == null) {
                return "-";
            }
            return clientIp;
        }

        /**
         * @param clientIp
         *            the clientIp to set
         */
        public final void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        /**
         * @return the HTTP Method
         */
        public String getMethod() {
            return method;
        }

        /**
         * @param method
         *            - the HTTP Method used
         */
        public final void setMethod(String method) {
            this.method = method;
        }

        /**
         * @return the requestUrl
         */
        public String getRequestUrl() {
            if (requestUrl == null) {
                return "-";
            }
            return requestUrl;
        }

        /**
         * @param requestUrl
         *            the requestUrl to set
         */
        public final void setRequestUrl(String requestUrl) {
            this.requestUrl = requestUrl;
        }

        /**
         * @return the identifier
         */
        public String getIdentifier() {
            if (identifier == null) {
                return "-";
            }
            return identifier;
        }

        /**
         * @param identifier
         *            the identifier to set
         */
        public final void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

    }
}
