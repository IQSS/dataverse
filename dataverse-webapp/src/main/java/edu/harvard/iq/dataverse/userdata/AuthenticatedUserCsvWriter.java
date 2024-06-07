package edu.harvard.iq.dataverse.userdata;

import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import io.vavr.control.Option;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes a CSV file with authenticated user details.
 */
@Stateless
public class AuthenticatedUserCsvWriter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticatedUserCsvWriter.class);

    // -------------------- LOGIC --------------------

    public void write(OutputStream outputStream, List<AuthenticatedUser> authenticatedUsers) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream);
             BufferedWriter streamWriter = new BufferedWriter(writer);
             CSVPrinter csvPrinter = new CSVPrinter(streamWriter, CSVFormat.DEFAULT)) {

            csvPrinter.printRecord(AuthenticatedUserCSVRecord.getHeaders());
            for(AuthenticatedUser user : authenticatedUsers) {
                csvPrinter.printRecord(buildRecord(user).getValues());
            }
        } catch (IOException ioe) {
            logger.error("Couldn't write user data to csv", ioe);
            throw ioe;
        }
    }

    // -------------------- PRIVATE --------------------

    private AuthenticatedUserCSVRecord buildRecord(AuthenticatedUser user) {
        AuthenticatedUserCSVRecord record = new AuthenticatedUserCSVRecord();

        record.setId(user.getId());
        record.setUsername(user.getIdentifier());
        record.setName(user.getDisplayInfo().getTitle());
        record.setEmail(user.getEmail());
        record.setAffiliation(user.getAffiliation());
        record.setSuperuser(user.isSuperuser());
        record.setAuthentication(user.getAuthenticatedUserLookup());
        record.setVerificationStatus(user.getEmailConfirmed());
        record.setNotificationLanguage(user.getNotificationsLanguage());
        record.setAccountCreation(user.getCreatedTime());
        record.setLastLogin(user.getLastLoginTime());
        record.setLastApiUse(user.getLastApiUseTime());
        return record;
    }

    // -------------------- INNER CLASSES --------------------

    enum AuthenticatedUserCsvColumn {
        ID("ID"),
        USERNAME("Username"),
        NAME("Name"),
        EMAIL("Email"),
        AFFILIATION("Affiliation"),
        SUPERUSER("Superuser"),
        AUTHENTICATION("Authentication"),
        VERIFICATION_STATUS("Verification status"),
        NOTIFICATION_LANGUAGE("Notification language"),
        ACCOUNT_CREATION("Account creation"),
        LAST_LOGIN("Last login"),
        LAST_API_USE("Last API use");

        final String columnName;

        AuthenticatedUserCsvColumn(String columnName) {
            this.columnName = columnName;
        }

        public String getColumnName() {
            return columnName;
        }
    }

    static class AuthenticatedUserCSVRecord {

        private static final List<String> CSV_HEADERS = Arrays.stream(AuthenticatedUserCsvColumn.values())
                .map(AuthenticatedUserCsvColumn::getColumnName)
                .collect(Collectors.toList());

        private Map<AuthenticatedUserCsvColumn, String> data = new HashMap<>();

        public static List<String> getHeaders() {
            return CSV_HEADERS;
        }

        public List<String> getValues() {
            return Arrays.stream(AuthenticatedUserCsvColumn.values())
                    .map(data::get)
                    .collect(Collectors.toList());
        }

        public void setId(Long id) {
            data.put(AuthenticatedUserCsvColumn.ID, safeToString(id));
        }

        public void setUsername(String username) {
            data.put(AuthenticatedUserCsvColumn.USERNAME, safeToString(username));
        }

        public void setName(String name) {
            data.put(AuthenticatedUserCsvColumn.NAME, safeToString(name));
        }

        public void setEmail(String email) {
            data.put(AuthenticatedUserCsvColumn.EMAIL, safeToString(email));
        }

        public void setAffiliation(String affiliation) {
            data.put(AuthenticatedUserCsvColumn.AFFILIATION, safeToString(affiliation));
        }

        public void setSuperuser(boolean superuser) {
            data.put(AuthenticatedUserCsvColumn.SUPERUSER, Boolean.toString(superuser));
        }

        public void setAuthentication(AuthenticatedUserLookup lookup) {
            data.put(AuthenticatedUserCsvColumn.AUTHENTICATION, Option.of(lookup)
                    .map(AuthenticatedUserLookup::getAuthenticationProviderId).getOrElse(""));
        }

        public void setVerificationStatus(Timestamp emailConfirmed) {
            data.put(AuthenticatedUserCsvColumn.VERIFICATION_STATUS, Option.of(emailConfirmed)
                    .map(s -> "Verified").getOrElse("Not verified"));
        }

        public void setNotificationLanguage(Locale notificationsLanguage) {
            data.put(AuthenticatedUserCsvColumn.NOTIFICATION_LANGUAGE, Option.of(notificationsLanguage)
                    .map(Locale::getLanguage).getOrElse(""));
        }

        public void setAccountCreation(Timestamp createdTime) {
            data.put(AuthenticatedUserCsvColumn.ACCOUNT_CREATION, safeTimestampToString(createdTime));
        }

        public void setLastLogin(Timestamp lastLoginTime) {
            data.put(AuthenticatedUserCsvColumn.LAST_LOGIN, safeTimestampToString(lastLoginTime));
        }

        public void setLastApiUse(Timestamp lastApiUseTime) {
            data.put(AuthenticatedUserCsvColumn.LAST_API_USE, safeTimestampToString(lastApiUseTime));
        }

        // -------------------- PRIVATE --------------------

        private String safeTimestampToString(Object object) {
            return object != null ? Util.getDateTimeFormat().format(object) : null;
        }

        private String safeToString(Object object) {
            return object != null ? object.toString() : "";
        }
    }
}
