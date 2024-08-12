/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.globus;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    @Resource
    ManagedScheduledExecutorService scheduler;
    
    @EJB
    SystemConfig systemConfig;
    @EJB
    SettingsServiceBean settingsSvc;
    @EJB 
    GlobusServiceBean globusService;
        
    @PostConstruct
    public void init() {
        if (systemConfig.isGlobusTaskMonitoringServer()) {
            int pollingInterval = SystemConfig.getIntLimitFromStringOrDefault(
                settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusPollingInterval), 60);
            this.scheduler.scheduleAtFixedRate(this::checkOngoingTasks,
                    0, pollingInterval,
                    TimeUnit.SECONDS);
        }
    }
    
    /**
     * This method will be executed on a timer-like schedule, continuously 
     * monitoring all the ongoing external Globus tasks (transfers). 
     * @todo make sure the executions do not overlap/stack up
     */
    public void checkOngoingTasks() {
        List<GlobusTaskInProgress> tasks = globusService.findAllOngoingTasks();
        
        tasks.forEach(t -> {
            GlobusTaskState retrieved = globusService.getTask(t.getGlobusToken(), t.getTaskId(), null);
            if (GlobusUtil.isTaskCompleted(retrieved)) {
                if (GlobusUtil.isTaskSucceeded(retrieved)) {
                    // Do our thing, finalize adding the files to the dataset
                    globusService.addFilesOnSuccess(t);
                }
                // Whether it finished successfully, or failed in the process, 
                // there's no need to keep monitoring this task, so we can 
                // delete it. 
                globusService.deleteTask(t);
                // @todo double-check that the locks have been properly handled
            }
        });
    }
    
}
