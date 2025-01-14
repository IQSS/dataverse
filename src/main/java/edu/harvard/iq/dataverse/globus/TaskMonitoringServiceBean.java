package edu.harvard.iq.dataverse.globus;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * 
 * This Singleton monitors ongoing Globus tasks by checking with the centralized
 * Globus API on the status of all the registered ongoing tasks. 
 * When a successful completion of a task is detected, the service triggers
 * the execution of the associated tasks (for example, finalizing adding datafiles
 * to the dataset on completion of a remote Globus upload). When a task fails or 
 * terminates abnormally, a message is logged and the task record is deleted 
 * from the database. 
 * 
 * @author landreev
 */
@Singleton
@Startup
public class TaskMonitoringServiceBean {
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.globus.TaskMonitoringServiceBean");
    
    @Resource
    ManagedScheduledExecutorService scheduler;
    
    @EJB
    SettingsServiceBean settingsSvc;
    @EJB 
    GlobusServiceBean globusService;
    
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    
    @PostConstruct
    public void init() {
        if (JvmSettings.GLOBUS_TASK_MONITORING_SERVER.lookupOptional(Boolean.class).orElse(false)) {
            logger.info("Starting Globus task monitoring service");
            int pollingInterval = SystemConfig.getIntLimitFromStringOrDefault(
                settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusPollingInterval), 600);
            
            // Monitoring service scheduler for ongoing upload tasks:
            this.scheduler.scheduleWithFixedDelay(this::checkOngoingUploadTasks,
                    0, pollingInterval,
                    TimeUnit.SECONDS);
            
            // A separate monitoring service scheduler for ongoing download tasks: 
            this.scheduler.scheduleWithFixedDelay(this::checkOngoingDownloadTasks,
                    0, pollingInterval,
                    TimeUnit.SECONDS);

        } else {
            logger.info("Skipping Globus task monitor initialization");
        }
        
        
    }
    
    /**
     * This method will be executed on a timer-like schedule, continuously 
     * monitoring all the ongoing external Globus tasks (transfers TO remote 
     * Globus endnodes). 
     */
    public void checkOngoingUploadTasks() {
        logger.fine("Performing a scheduled external Globus UPLOAD task check");
        List<GlobusTaskInProgress> tasks = globusService.findAllOngoingTasks(GlobusTaskInProgress.TaskType.UPLOAD);

        tasks.forEach(t -> {
            GlobusTaskState retrieved = globusService.getTask(t.getGlobusToken(), t.getTaskId(), null);

            if (GlobusUtil.isTaskCompleted(retrieved)) {
                FileHandler taskLogHandler = getTaskLogHandler(t);
                Logger taskLogger = getTaskLogger(t, taskLogHandler);

                // Do our thing, finalize adding the files to the dataset
                globusService.processCompletedTask(t, retrieved, GlobusUtil.isTaskSucceeded(retrieved), GlobusUtil.getCompletedTaskStatus(retrieved), true, taskLogger);
                // Whether it finished successfully, or failed in the process, 
                // there's no need to keep monitoring this task, so we can 
                // delete it.
                //globusService.deleteExternalUploadRecords(t.getTaskId());
                globusService.deleteTask(t);

                if (taskLogHandler != null) {
                    taskLogHandler.close();
                }
            }

        });
    }
    
    /**
     * This method will be executed on a timer-like schedule, continuously 
     * monitoring all the ongoing external Globus download tasks (transfers by
     * Dataverse users FROM remote, Dataverse-managed Globus endnodes). 
     */
    public void checkOngoingDownloadTasks() {
        logger.fine("Performing a scheduled external Globus DOWNLOAD task check");
        List<GlobusTaskInProgress> tasks = globusService.findAllOngoingTasks(GlobusTaskInProgress.TaskType.DOWNLOAD);

        tasks.forEach(t -> {

            // @todo: this was quite dumb, actually - saving the access token in 
            // the database, hoping to keep reusing it throughout the life of 
            // the transfer. It has of course a very good chance to expire 
            // before it's completed. 
            GlobusTaskState retrieved = globusService.getTask(t.getGlobusToken(), t.getTaskId(), null);

            if (retrieved != null && GlobusUtil.isTaskCompleted(retrieved)) {
                FileHandler taskLogHandler = getTaskLogHandler(t);
                Logger taskLogger = getTaskLogger(t, taskLogHandler);
                
                String taskStatus = retrieved == null ? "N/A" : retrieved.getStatus();
                taskLogger.info("Processing completed task " + t.getTaskId() + ", status: " + taskStatus);
                
                // Unlike uploads, it is now possible for a user to run several 
                // download transfers on the same dataset - with several download 
                // tasks using the same access rule on the corresponding Globus
                // psuedofolder. This means that we need to be careful not to 
                // delete the rule, without checking if there are still other 
                // active tasks using it: 
                
                boolean deleteRule = true;
                
                if (t.getRuleId() == null || globusService.isRuleInUseByOtherTasks(t.getRuleId())) {
                    taskLogger.info("Access rule " + t.getRuleId() + " is in use by other tasks.");
                    deleteRule = false;
                } else {
                    taskLogger.info("Access rule " + t.getRuleId() + " is no longer in use by other tasks; proceeding to delete.");
                }

                globusService.processCompletedTask(t, retrieved, GlobusUtil.isTaskSucceeded(retrieved), GlobusUtil.getCompletedTaskStatus(retrieved), deleteRule, taskLogger);
                // globusService.processCompletedTask(t, GlobusUtil.isTaskSucceeded(retrieved), GlobusUtil.getTaskStatus(retrieved), taskLogger);
                
                // Whether it finished successfully or failed, the entry for the 
                // task can now be deleted from the database. 
                globusService.deleteTask(t);
                
                

                if (taskLogHandler != null) {
                    taskLogHandler.close();
                }
            }
        });
    }
    // @todo: combine the 2 methods below into one (?)
    // @todo: move the method(s) below into the GlobusUtil, for the Globus Service to use as well
    // @todo: switch to a different log formatter (from the default xml) (?)
    private FileHandler getTaskLogHandler(GlobusTaskInProgress task) {
        if (task == null) {
            return null; 
        }
        
        Date startDate = new Date(task.getStartTime().getTime());
        String logTimeStamp = logFormatter.format(startDate);
        
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") 
                + File.separator + "logs" 
                + File.separator + "globus" + task.getTaskType() + "_" 
                + logTimeStamp + "_" + task.getDataset().getId()
                + ".log";
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler(logFileName, true);
        } catch (IOException | SecurityException ex) {
            // @todo log this error somehow?
            fileHandler = null;
        }
        return fileHandler;
    }
    
    private Logger getTaskLogger(GlobusTaskInProgress task, FileHandler logFileHandler) {
        if (logFileHandler == null) {
            return null;
        }
        Date startDate = new Date(task.getStartTime().getTime());
        String logTimeStamp = logFormatter.format(startDate);
        
        Logger taskLogger = Logger.getLogger(
                "edu.harvard.iq.dataverse.globus.GlobusServiceBean." + "Globus" 
                        + task.getTaskType() + logTimeStamp);
            taskLogger.setUseParentHandlers(false);
       
        taskLogger.addHandler(logFileHandler);
        
        return taskLogger;        
    }
    
}
