package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;

public class InAppNotificationsJsonPrinter {

    @EJB
    private static DataverseServiceBean dataverseServiceBean;

    @EJB
    private static SystemConfig systemConfig;

    public static void addFieldsByType(NullSafeJsonBuilder notificationJson, UserNotification.Type type) {
        switch (type) {
            case ASSIGNROLE:
            case REVOKEROLE:
            case CREATEDV:
            case REQUESTFILEACCESS:
            case REQUESTEDFILEACCESS:
            case GRANTFILEACCESS:
            case REJECTFILEACCESS:
            case DATASETCREATED:
            case CREATEDS:
            case SUBMITTEDDS:
            case PUBLISHEDDS:
            case PUBLISHFAILED_PIDREG:
            case RETURNEDDS:
            case WORKFLOW_SUCCESS:
            case WORKFLOW_FAILURE:
            case STATUSUPDATED:
            case PIDRECONCILED:
            case CREATEACC:
                notificationJson.add("rootDataverseName", dataverseServiceBean.findRootDataverse().getName());
                notificationJson.add("userGuideUrl", systemConfig.getGuidesUrl());
            case CHECKSUMFAIL:
            case FILESYSTEMIMPORT:
            case GLOBUSUPLOADCOMPLETED:
            case GLOBUSDOWNLOADCOMPLETED:
            case GLOBUSUPLOADCOMPLETEDWITHERRORS:
            case GLOBUSUPLOADREMOTEFAILURE:
            case GLOBUSUPLOADLOCALFAILURE:
            case GLOBUSDOWNLOADCOMPLETEDWITHERRORS:
            case CHECKSUMIMPORT:
            case CONFIRMEMAIL:
            case APIGENERATED:
            case INGESTCOMPLETED:
            case INGESTCOMPLETEDWITHERRORS:
            case DATASETMENTIONED:
        }
    }
}
