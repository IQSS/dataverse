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
    SystemConfig systemConfig;
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
            this.scheduler.scheduleWithFixedDelay(this::checkOngoingTasks,
                    0, pollingInterval,
                    TimeUnit.SECONDS);
        } else {
            logger.info("Skipping Globus task monitor initialization");
        }
    }
    
    /**
     * This method will be executed on a timer-like schedule, continuously 
     * monitoring all the ongoing external Globus tasks (transfers). 
     */
    public void checkOngoingTasks() {
        logger.fine("Performing a scheduled external Globus task check");
        List<GlobusTaskInProgress> tasks = globusService.findAllOngoingTasks();

        tasks.forEach(t -> {
            FileHandler taskLogHandler = getTaskLogHandler(t);
            Logger taskLogger = getTaskLogger(t, taskLogHandler);
            
            GlobusTaskState retrieved = globusService.getTask(t.getGlobusToken(), t.getTaskId(), taskLogger);
            if (GlobusUtil.isTaskCompleted(retrieved)) {
                // Do our thing, finalize adding the files to the dataset
                globusService.processCompletedTask(t, GlobusUtil.isTaskSucceeded(retrieved), GlobusUtil.getTaskStatus(retrieved), taskLogger);
                // Whether it finished successfully, or failed in the process, 
                // there's no need to keep monitoring this task, so we can 
                // delete it.
                //globusService.deleteExternalUploadRecords(t.getTaskId());
                globusService.deleteTask(t);
            }
            
            if (taskLogHandler != null) {
                // @todo it should be prudent to cache these loggers and handlers 
                // between monitoring runs (should be fairly easy to do)
                taskLogHandler.close();
            }
        });
    }
    
    private FileHandler getTaskLogHandler(GlobusTaskInProgress task) {
        if (task == null) {
            return null; 
        }
        
        Date startDate = new Date(task.getStartTime().getTime());
        String logTimeStamp = logFormatter.format(startDate);
        
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs" + File.separator + "globusUpload_" + task.getDataset().getId() + "_" + logTimeStamp
                + ".log";
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler(logFileName);
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
                "edu.harvard.iq.dataverse.upload.client.DatasetServiceBean." + "GlobusUpload" + logTimeStamp);
            taskLogger.setUseParentHandlers(false);
       
        taskLogger.addHandler(logFileHandler);
        
        return taskLogger;        
    }
    
}
