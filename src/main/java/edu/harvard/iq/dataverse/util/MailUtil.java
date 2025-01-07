package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class MailUtil {

    private static final Logger logger = Logger.getLogger(MailUtil.class.getCanonicalName());

    public static String getSubjectTextBasedOnNotification(UserNotification userNotification, Object objectOfNotification) {
        List<String> rootDvNameAsList = Arrays.asList(BrandingUtil.getInstallationBrandName());
        String datasetDisplayName = "";

        if (objectOfNotification != null) {
            if (objectOfNotification instanceof Dataset) {
                datasetDisplayName = ((Dataset) objectOfNotification).getDisplayName();
            } else if (objectOfNotification instanceof DatasetVersion) {
                datasetDisplayName = ((DatasetVersion) objectOfNotification).getDataset().getDisplayName();
            } else if (objectOfNotification instanceof DataFile) {
                datasetDisplayName = ((DataFile) objectOfNotification).getOwner().getDisplayName();
            }
        }

        switch (userNotification.getType()) {
            case ASSIGNROLE:
                return BundleUtil.getStringFromBundle("notification.email.assign.role.subject", rootDvNameAsList);
            case REVOKEROLE:
                return BundleUtil.getStringFromBundle("notification.email.revoke.role.subject", rootDvNameAsList);
            case CREATEDV:
                return BundleUtil.getStringFromBundle("notification.email.create.dataverse.subject", rootDvNameAsList);
            case REQUESTFILEACCESS:
                String userNameFirst = userNotification.getRequestor().getFirstName();
                String userNameLast = userNotification.getRequestor().getLastName();
                String userIdentifier = userNotification.getRequestor().getIdentifier();
                return BundleUtil.getStringFromBundle("notification.email.request.file.access.subject", Arrays.asList(rootDvNameAsList.get(0), userNameFirst, userNameLast, userIdentifier, datasetDisplayName));
            case REQUESTEDFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.requested.file.access.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case GRANTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.grant.file.access.subject", rootDvNameAsList);
            case REJECTFILEACCESS:
                return BundleUtil.getStringFromBundle("notification.email.rejected.file.access.subject", rootDvNameAsList);
            case DATASETCREATED:
                return BundleUtil.getStringFromBundle("notification.email.dataset.created.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case CREATEDS:
                return BundleUtil.getStringFromBundle("notification.email.create.dataset.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case SUBMITTEDDS:
                return BundleUtil.getStringFromBundle("notification.email.submit.dataset.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case PUBLISHEDDS:
                return BundleUtil.getStringFromBundle("notification.email.publish.dataset.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case PUBLISHFAILED_PIDREG:
                return BundleUtil.getStringFromBundle("notification.email.publishFailure.dataset.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case RETURNEDDS:
                return BundleUtil.getStringFromBundle("notification.email.returned.dataset.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case WORKFLOW_SUCCESS:
                return BundleUtil.getStringFromBundle("notification.email.workflow.success.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case WORKFLOW_FAILURE:
                return BundleUtil.getStringFromBundle("notification.email.workflow.failure.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case STATUSUPDATED:
                return BundleUtil.getStringFromBundle("notification.email.status.change.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case CREATEACC:
                return BundleUtil.getStringFromBundle("notification.email.create.account.subject", rootDvNameAsList);
            case CHECKSUMFAIL:
                return BundleUtil.getStringFromBundle("notification.email.checksumfail.subject", rootDvNameAsList);
            case FILESYSTEMIMPORT:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.import.filesystem.subject", dsNameAsList);
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.import.filesystem.subject", rootDvNameAsList);
                }
            case GLOBUSUPLOADCOMPLETED:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadCompleted.subject", dsNameAsList);
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadCompleted.subject", rootDvNameAsList);
                }
            case GLOBUSDOWNLOADCOMPLETED:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.globus.downloadCompleted.subject", dsNameAsList);
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.globus.downloadCompleted.subject", rootDvNameAsList);
                }
            case GLOBUSUPLOADCOMPLETEDWITHERRORS:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadCompletedWithErrors.subject", dsNameAsList);
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadCompletedWithErrors.subject", rootDvNameAsList);
                }
            case GLOBUSUPLOADREMOTEFAILURE:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadFailedRemotely.subject", dsNameAsList);
                    
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadFailedRemotely.subject", rootDvNameAsList);
                }
            case GLOBUSUPLOADLOCALFAILURE:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadFailedLocally.subject", dsNameAsList);
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.globus.uploadFailedLocally.subject", rootDvNameAsList);
                }
            case GLOBUSDOWNLOADCOMPLETEDWITHERRORS:
                try {
                    DatasetVersion version =  (DatasetVersion)objectOfNotification;
                    List<String> dsNameAsList = Arrays.asList(version.getDataset().getDisplayName());
                    return BundleUtil.getStringFromBundle("notification.email.globus.downloadCompletedWithErrors.subject", dsNameAsList);
                } catch (Exception e) {
                    return BundleUtil.getStringFromBundle("notification.email.globus.downloadCompletedWithErrors.subject", rootDvNameAsList);
                }

            case CHECKSUMIMPORT:
                return BundleUtil.getStringFromBundle("notification.email.import.checksum.subject", rootDvNameAsList);
            case CONFIRMEMAIL:
                return BundleUtil.getStringFromBundle("notification.email.verifyEmail.subject", rootDvNameAsList);
            case APIGENERATED:
                return BundleUtil.getStringFromBundle("notification.email.apiTokenGenerated.subject",  rootDvNameAsList);
            case INGESTCOMPLETED:
                return BundleUtil.getStringFromBundle("notification.email.ingestCompleted.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case INGESTCOMPLETEDWITHERRORS:
                return BundleUtil.getStringFromBundle("notification.email.ingestCompletedWithErrors.subject", Arrays.asList(rootDvNameAsList.get(0), datasetDisplayName));
            case DATASETMENTIONED:
                return BundleUtil.getStringFromBundle("notification.email.datasetWasMentioned.subject", rootDvNameAsList);
        }
        return "";
    }

}
