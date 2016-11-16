package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.entities.JobExecutionEntity;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import org.apache.commons.lang.StringUtils;

import javax.batch.api.listener.JobListener;
import javax.batch.api.listener.StepListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.sql.Timestamp;
import java.util.Date;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@Dependent
public class FileRecordJobListener implements StepListener, JobListener {

    private static final Logger logger = Logger.getLogger(FileRecordJobListener.class.getName());

    private static final UserNotification.Type notifyType = UserNotification.Type.FILESYSTEMIMPORT;

    @Inject
    private JobContext jobContext = null;

    @Inject
    private StepContext stepContext;

    Properties jobParams;

    @EJB
    UserNotificationServiceBean notificationServiceBean;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;

    @EJB
    ActionLogServiceBean actionLogServiceBean;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    PermissionServiceBean permissionServiceBean;

    @EJB
    DataFileServiceBean dataFileServiceBean;
    
    @Override
    public void afterStep() throws Exception {
        // no-op
    }

    @Override
    public void beforeStep() throws Exception {
        // no-op
    }

    Dataset dataset;
    String mode;
    AuthenticatedUser user;

    @Override
    public void beforeJob() throws Exception {

        // update job properties to be used elsewhere to determine dataset, user and mode
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        jobParams.setProperty("datasetGlobalId", getDatasetGlobalId());
        jobParams.setProperty("userId", getUserId());
        jobParams.setProperty("mode", getMode());

        DatasetVersion workingVersion = dataset.getEditVersion();

        // if mode = UPDATE or REPLACE, remove all filemetadata from the dataset version and start fresh
        // if mode = MERGE (default), do nothing since only new files will be added
        if (mode.equalsIgnoreCase(ImportMode.UPDATE.name()) || mode.equalsIgnoreCase(ImportMode.REPLACE.name())) {
            try {
                List <FileMetadata> fileMetadataList = workingVersion.getFileMetadatas();
                for (FileMetadata fmd : fileMetadataList) {
                    dataFileServiceBean.deleteFromVersion(workingVersion, fmd.getDataFile());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error setting mode to UPDATE in beforeJob: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterJob() throws Exception {
        doReport();
        logger.log(Level.INFO, "After Job {0}, instance {1} and execution {2}, batch status [{3}], exit status [{4}]",
                new Object[]{jobContext.getJobName(), jobContext.getInstanceId(), jobContext.getExecutionId(),
                        jobContext.getBatchStatus(), jobContext.getExitStatus()});
    }

    /**
     * Generate all the job reports and user notifications.
     */
    private void doReport() {

        try {

            String jobJson;
            String jobId = Long.toString(jobContext.getInstanceId());
            JobOperator jobOperator = BatchRuntime.getJobOperator();

            if (user == null) {
                logger.log(Level.SEVERE, "Cannot find authenticated user.");
                return;
            }
            if (dataset == null) {
                logger.log(Level.SEVERE, "Cannot find dataset.");
                return;
            }

            long datasetVersionId = dataset.getLatestVersion().getId();

            JobExecution jobExecution = jobOperator.getJobExecution(jobContext.getInstanceId());
            if (jobExecution != null) {

                Date date = new Date();
                Timestamp timestamp =  new Timestamp(date.getTime());

                JobExecutionEntity jobExecutionEntity = JobExecutionEntity.create(jobExecution);
                jobExecutionEntity.setExitStatus("COMPLETED");
                jobExecutionEntity.setStatus(BatchStatus.COMPLETED);
                jobExecutionEntity.setEndTime(date);
                jobJson = new ObjectMapper().writeValueAsString(jobExecutionEntity);

                String logDir = System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs"
                        + File.separator + "batch-jobs" + File.separator;

                // [1] save json log to file
                LoggingUtil.saveJsonLog(jobJson, logDir, jobId);
                // [2] send user notifications
                notificationServiceBean.sendNotification(user, timestamp, notifyType, datasetVersionId);
                // also send admin notification
                AuthenticatedUser adminUser = authenticationServiceBean.getAdminUser();
                if (adminUser != null) {
                    notificationServiceBean.sendNotification(adminUser, timestamp, notifyType, datasetVersionId);
                }
                // [3] action log it
                // truncate the log message or risk: 
                // Internal Exception: org.postgresql.util.PSQLException: ERROR: value too long for type character varying(1024)
                actionLogServiceBean.log(LoggingUtil.getActionLogRecord(user.getIdentifier(), jobExecution,
                        StringUtils.substring(jobJson, 0, 1024), jobId));

            } else {
                logger.log(Level.SEVERE, "Job execution is null");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating job json: " + e.getMessage());
        }
    }

    // utils
    /**
     * Get the dataset based on the job parameter: datasetId or datasetPrimaryKey.
     * @return dataset global identifier
     */
    private String getDatasetGlobalId() {
        if (jobParams.containsKey("datasetId")) {
            String datasetId = jobParams.getProperty("datasetId");
            dataset = datasetServiceBean.findByGlobalId(datasetId);
            return dataset.getGlobalId();
        }
        if (jobParams.containsKey("datasetPrimaryKey")) {
            long datasetPrimaryKey = Long.parseLong(jobParams.getProperty("datasetPrimaryKey"));
            dataset = datasetServiceBean.find(datasetPrimaryKey);
            return dataset.getGlobalId();
        }
        logger.log(Level.SEVERE, "Can't find dataset.");
        dataset = null;
        return null;
    }

    /**
     * Get the authenticated user based on the job parameter: userPrimaryKey or userId.
     * @return user
     */
    private String getUserId() {
        if (jobParams.containsKey("userPrimaryKey")) {
            long userPrimaryKey = Long.parseLong(jobParams.getProperty("userPrimaryKey"));
            user = authenticationServiceBean.findByID(userPrimaryKey);
            return Long.toString(user.getId());
        }
        if (jobParams.containsKey("userId")) {
            String userId = jobParams.getProperty("userId");
            user = authenticationServiceBean.getAuthenticatedUser(userId);
            return Long.toString(user.getId());
        }
        logger.log(Level.SEVERE, "Cannot find authenticated user.");
        user = null;
        return null;
    }

    /**
     * Get the import mode: MERGE (default), UPDATE, REPLACE
     * @return mode
     */
    private String getMode() {
        if (jobParams.containsKey("mode")) {
            mode = jobParams.getProperty("mode").toUpperCase();
        } else {
            mode = ImportMode.MERGE.name();
        }
        return mode;
    }

}
