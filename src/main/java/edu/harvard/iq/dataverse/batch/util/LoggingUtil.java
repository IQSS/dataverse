package edu.harvard.iq.dataverse.batch.util;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import org.apache.commons.io.FileUtils;

import javax.batch.runtime.JobExecution;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bmckinney on 7/14/16.
 */
public class LoggingUtil {

    private static final Logger logger = Logger.getLogger(LoggingUtil.class.getName());

    public static void saveJsonLog(String jobJson, String logDir, String jobId) {
        try {
            File dir = new File(logDir);
            if (!dir.exists() && !dir.mkdirs()) {
                logger.log(Level.SEVERE, "Couldn't create directory: " + dir.getAbsolutePath());
            }
            File reportJson = new File(dir.getAbsolutePath() + "/job-" + jobId + ".json");
            FileUtils.writeStringToFile(reportJson, jobJson);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving json report: " + e.getMessage());
        }
    }

    public static ActionLogRecord getActionLogRecord(String userId, JobExecution jobExec, String jobInfo, String jobId) {
        try {
            ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Command, jobExec.getJobName());
            alr.setId(jobId);
            alr.setInfo(jobInfo);
            alr.setUserIdentifier(userId);
            alr.setStartTime(jobExec.getStartTime());
            alr.setEndTime(jobExec.getEndTime());
            if (jobExec.getBatchStatus().name().equalsIgnoreCase("COMPLETED")) {
                alr.setActionResult(ActionLogRecord.Result.OK);
            } else {
                alr.setActionResult(ActionLogRecord.Result.InternalError);
            }
            return alr;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating action log record: " + e.getMessage());
            return null;
        }
    }

}
