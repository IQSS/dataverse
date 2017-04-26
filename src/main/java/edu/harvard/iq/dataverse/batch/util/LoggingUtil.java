/*
   Copyright (C) 2005-2017, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
*/

package edu.harvard.iq.dataverse.batch.util;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import org.apache.commons.io.FileUtils;

import javax.batch.runtime.JobExecution;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


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
            if (jobExec.getBatchStatus().name().equalsIgnoreCase("STARTED")) {
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
    
    public static Logger getJobLogger(String jobId) {
        try {
            Logger jobLogger = Logger.getLogger("job-"+jobId);
            FileHandler fh;
            String logDir = System.getProperty("com.sun.aas.instanceRoot") + System.getProperty("file.separator") 
                    + "logs" + System.getProperty("file.separator") + "batch-jobs" + System.getProperty("file.separator");
            fh = new FileHandler(logDir + "job-" + jobId + ".log");
            logger.log(Level.INFO, "JOB LOG: " + logDir + "job-" + jobId + ".log");
            jobLogger.addHandler(fh);
            fh.setFormatter(new JobLogFormatter());
            return jobLogger;
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "Unable to create job logger: " + e.getMessage());
            return null;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to create job logger: " + e.getMessage());
            return null;
        }
    }

    public static class JobLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getLevel() + ": " + record.getMessage() + "\n";
        }
    }
}
