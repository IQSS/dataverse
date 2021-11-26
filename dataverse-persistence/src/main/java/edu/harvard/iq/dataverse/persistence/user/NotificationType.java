package edu.harvard.iq.dataverse.persistence.user;

import java.util.HashSet;
import java.util.Set;

public class NotificationType {
    public static final String ASSIGNROLE = "ASSIGNROLE";
    public static final String REVOKEROLE = "REVOKEROLE";
    public static final String CREATEDV = "CREATEDV";
    public static final String CREATEDS = "CREATEDS";
    public static final String CREATEACC = "CREATEACC";
    public static final String MAPLAYERUPDATED = "MAPLAYERUPDATED";
    public static final String MAPLAYERDELETEFAILED = "MAPLAYERDELETEFAILED";
    public static final String SUBMITTEDDS = "SUBMITTEDDS";
    public static final String RETURNEDDS = "RETURNEDDS";
    public static final String PUBLISHEDDS = "PUBLISHEDDS";
    public static final String REQUESTFILEACCESS = "REQUESTFILEACCESS";
    public static final String GRANTFILEACCESS = "GRANTFILEACCESS";
    public static final String REJECTFILEACCESS = "REJECTFILEACCESS";
    public static final String FILESYSTEMIMPORT = "FILESYSTEMIMPORT";
    public static final String CHECKSUMIMPORT = "CHECKSUMIMPORT";
    public static final String CHECKSUMFAIL = "CHECKSUMFAIL";
    public static final String CONFIRMEMAIL = "CONFIRMEMAIL";
    public static final String GRANTFILEACCESSINFO = "GRANTFILEACCESSINFO";
    public static final String REJECTFILEACCESSINFO = "REJECTFILEACCESSINFO";

    // -------------------- LOGIC --------------------

    public static Set<String> getTypes() {
        HashSet<String> notifications = new HashSet<>();

        notifications.add(ASSIGNROLE);
        notifications.add(REVOKEROLE);
        notifications.add(CREATEDV);
        notifications.add(CREATEDS);
        notifications.add(CREATEACC);
        notifications.add(MAPLAYERUPDATED);
        notifications.add(MAPLAYERDELETEFAILED);
        notifications.add(SUBMITTEDDS);
        notifications.add(RETURNEDDS);
        notifications.add(PUBLISHEDDS);
        notifications.add(REQUESTFILEACCESS);
        notifications.add(GRANTFILEACCESS);
        notifications.add(REJECTFILEACCESS);
        notifications.add(FILESYSTEMIMPORT);
        notifications.add(CHECKSUMIMPORT);
        notifications.add(CHECKSUMFAIL);
        notifications.add(CONFIRMEMAIL);
        notifications.add(GRANTFILEACCESSINFO);
        notifications.add(REJECTFILEACCESSINFO);

        return notifications;
    }
}
