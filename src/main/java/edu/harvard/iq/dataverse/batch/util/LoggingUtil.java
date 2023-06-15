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

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import static edu.harvard.iq.dataverse.batch.jobs.importer.filesystem.FileRecordJobListener.SEP;
import edu.harvard.iq.dataverse.engine.command.Command;
import org.apache.commons.io.FileUtils;

import javax.batch.runtime.JobExecution;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


public class LoggingUtil {

    private static final Logger logger = Logger.getLogger(LoggingUtil.class.getName());
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

    public static void saveJsonLog(String jobJson, String logDir, String jobId) {
        try {
            String fileName = "/job-" + jobId + ".json";
            saveLogFile(jobJson, logDir, fileName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving json report: " + e.getMessage());
        }
    }

    public static void saveLogFile(String fileContent, String logDir, String fileName) {

        try {
            checkCreateLogDirectory(logDir);
            File dir = new File(logDir);
            if (!dir.exists() && !dir.mkdirs()) {
                logger.log(Level.SEVERE, "Couldn't create directory: " + dir.getAbsolutePath());
            }
            File logFile = new File(dir.getAbsolutePath() + fileName);
            FileUtils.writeStringToFile(logFile, fileContent);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving log report: " + fileName + " " + e.getMessage());
        }
    }
    
    public static void writeOnSuccessFailureLog(Command command, String failureNotes, DvObject dvo){
        String logDir = System.getProperty("com.sun.aas.instanceRoot") + SEP + "logs" + SEP + "process-failures" + SEP;
        String identifier = dvo.getIdentifier();
        
        if (identifier != null) {
            identifier = identifier.substring(identifier.indexOf("/") + 1);
        } else {
            identifier = dvo.getId().toString();
        }
        if (command != null){
            failureNotes =  failureNotes + "\r\n Command: " + command.toString();
        }

        String logTimestamp = logFormatter.format(new Date());
        String fileName = "/process-failure" +  "-" + identifier + "-" + logTimestamp + ".txt";
        LoggingUtil.saveLogFile(failureNotes, logDir, fileName);
        
    }
     
    public static void saveLogFileAppendWithHeader(String fileContent, String logDir, String fileName, String logHeader) {
        try {
            checkCreateLogDirectory(logDir);
            File dir = new File(logDir);
            if (!dir.exists() && !dir.mkdirs()) {
                logger.log(Level.SEVERE, "Couldn't create directory: " + dir.getAbsolutePath());
            }
            File logFile = new File(dir.getAbsolutePath() +"/"+ fileName);
            if(!logFile.exists() && null != logHeader) {
                FileUtils.writeStringToFile(logFile, logHeader);
            }
            FileUtils.writeStringToFile(logFile, fileContent, true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving log report: " + fileName + " " + e.getMessage());
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

    /**
     * check if the directory for log files exists, and create if necessary
     */
    private static void checkCreateLogDirectory( String logDir )
    {
	    try
	    {
		    File d = new File( logDir );
		    if ( ! d.exists() )
		    {
			    logger.log(Level.INFO,"log directory: " + d.getAbsolutePath() + " absent, trying to create");
			    d.mkdirs();
			    if ( ! d.exists() )
			    {
			    	logger.log(Level.SEVERE,"unable to create log directory: " + d.getAbsolutePath() );
			    }
			    else
			    {
			    	logger.log(Level.INFO,"log directory: " + d.getAbsolutePath() + " created");
			    }
		    }
	    }
	    catch( SecurityException e)
	    {
		    logger.log( Level.SEVERE, "security exception checking / creating log directory: " + logDir );
	    }
    }
    
    public static Logger getJobLogger(String jobId) {
	    try {
		    Logger jobLogger = Logger.getLogger("job-"+jobId);
		    FileHandler fh;
		    String logDir = System.getProperty("com.sun.aas.instanceRoot") + File.separator
			    + "logs" + File.separator + "batch-jobs" + File.separator;
		    checkCreateLogDirectory( logDir );
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
 