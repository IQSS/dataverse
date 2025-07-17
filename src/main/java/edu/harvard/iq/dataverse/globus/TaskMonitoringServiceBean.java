package edu.harvard.iq.dataverse.globus;

import edu.harvard.iq.dataverse.Dataset;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    final Map<String, String> globusClientKeys = new HashMap<>();
    
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
     * monitoring all the ongoing external Globus upload tasks (transfers TO remote 
     * Globus endnodes). 
     */
    public void checkOngoingUploadTasks() {
        logger.fine("Performing a scheduled external Globus UPLOAD task check");
        List<GlobusTaskInProgress> tasks = globusService.findAllOngoingTasks(GlobusTaskInProgress.TaskType.UPLOAD);

        tasks.forEach(t -> {
            GlobusTaskState retrieved = checkTaskState(t); 

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

        // Unlike with uploads, it is now possible for a user to run several 
        // download transfers on the same dataset - with several download 
        // tasks using the same access rule on the corresponding Globus
        // pseudofolder. This means that we'll need to be careful not to 
        // delete any rule, without checking if there are still other 
        // active tasks using it: 
        Map <String, Long> rulesInUse = new HashMap<>(); 
        
        tasks.forEach(t -> {
            String ruleId = t.getRuleId();
            if (ruleId != null) {
                if (rulesInUse.containsKey(ruleId)) {
                    rulesInUse.put(ruleId, rulesInUse.get(ruleId) + 1); 
                } else {
                    rulesInUse.put(ruleId, 1L);
                }
            }
        });
        
        tasks.forEach(t -> {

            GlobusTaskState retrieved = checkTaskState(t); 

            if (GlobusUtil.isTaskCompleted(retrieved)) {
                FileHandler taskLogHandler = getTaskLogHandler(t);
                Logger taskLogger = getTaskLogger(t, taskLogHandler);
                
                String taskStatus = retrieved == null ? "N/A" : retrieved.getStatus();
                taskLogger.info("Processing completed task " + t.getTaskId() + ", status: " + taskStatus);
                
                boolean deleteRule = true;
                
                if (t.getRuleId() == null || rulesInUse.get(t.getRuleId()) > 1) {
                    taskLogger.info("Access rule " + t.getRuleId() + " is still in use by other tasks.");
                    deleteRule = false;
                    rulesInUse.put(t.getRuleId(), rulesInUse.get(t.getRuleId()) - 1);
                } else {
                    taskLogger.info("Access rule " + t.getRuleId() + " is no longer in use by other tasks; will delete.");
                }

                globusService.processCompletedTask(t, retrieved, GlobusUtil.isTaskSucceeded(retrieved), GlobusUtil.getCompletedTaskStatus(retrieved), deleteRule, taskLogger);
                
                // Whether it finished successfully or failed, the entry for the 
                // task can now be deleted from the database. 
                globusService.deleteTask(t);                

                if (taskLogHandler != null) {
                    taskLogHandler.close();
                }
            } else {
                String taskStatus = retrieved == null ? "N/A" : retrieved.getStatus();
                logger.fine("task "+t.getTaskId()+" is still running; " + ", status: " + taskStatus);
            }
            
        });
    }
    
    private GlobusTaskState checkTaskState(GlobusTaskInProgress task) {
        GlobusTaskState retrieved = null;
        int attempts = 2;
        // we will make an extra attempt to refresh the token and try again
        // in the event of an exception indicating the token is stale 

        String globusClientToken = getClientTokenForStorageDriver(task.getDataset(), false);

        while (retrieved == null && attempts > 0) {
            try {
                retrieved = globusService.getTask(globusClientToken, task.getTaskId(), null);
            } catch (ExpiredTokenException ete) {
                globusClientToken = getClientTokenForStorageDriver(task.getDataset(), true);
            }
            attempts--;
        }

        return retrieved;
    }
    
    private String getClientTokenForStorageDriver(Dataset dataset, boolean forceRefresh) {
        String storageDriverId = dataset.getEffectiveStorageDriverId();
        if (globusClientKeys.get(storageDriverId) == null || forceRefresh) {
            String clientToken = globusService.getClientTokenForDataset(dataset);
            globusClientKeys.put(storageDriverId, clientToken);
        }
        
        return globusClientKeys.get(storageDriverId);
    }
    
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
