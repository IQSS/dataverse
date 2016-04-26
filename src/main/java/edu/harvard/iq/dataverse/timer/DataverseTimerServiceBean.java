/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.timer;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.harvest.client.HarvestTimerInfo;
import edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author roberttreacy
 */
@Stateless
public class DataverseTimerServiceBean implements Serializable {
    @Resource
    javax.ejb.TimerService timerService;
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.timer.DataverseTimerServiceBean");
    @EJB
    HarvesterServiceBean harvesterService;
    @EJB
    DataverseServiceBean dataverseService;
    
    /*@EJB
    StudyServiceLocal studyService;*/


    public void createTimer(Date initialExpiration, long intervalDuration, Serializable info) {
        try {
            logger.log(Level.INFO,"Creating timer on " + InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException ex) {
            Logger.getLogger(DataverseTimerServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        timerService.createTimer(initialExpiration, intervalDuration, info);
    }


    /**
     * This method is called whenever an EJB Timer goes off.
     * Check to see if this is a Harvest Timer, and if it is
     * Run the harvest for the given (scheduled) dataverse
     * @param timer
     */
    @Timeout
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void handleTimeout(javax.ejb.Timer timer) {
        // We have to put all the code in a try/catch block because
        // if an exception is thrown from this method, Glassfish will automatically
        // call the method a second time. (The minimum number of re-tries for a Timer method is 1)

        try {
            logger.log(Level.INFO,"Handling timeout on " + InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException ex) {
            Logger.getLogger(DataverseTimerServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (timer.getInfo() instanceof HarvestTimerInfo) {
            HarvestTimerInfo info = (HarvestTimerInfo) timer.getInfo();
            try {

                logger.log(Level.INFO, "DO HARVESTING of dataverse " + info.getHarvestingDataverseId());
                harvesterService.doHarvest(info.getHarvestingDataverseId());

            } catch (Throwable e) {
                dataverseService.setHarvestResult(info.getHarvestingDataverseId(), harvesterService.HARVEST_RESULT_FAILED);
                //mailService.sendHarvestErrorNotification(dataverseService.find().getSystemEmail(), dataverseService.find().getName());
                logException(e, logger);
            }
        }
        /* Export timers: (not yet implemented!) -- L.A.
        if (timer.getInfo() instanceof ExportTimerInfo) {
            try {
                ExportTimerInfo info = (ExportTimerInfo) timer.getInfo();
                logger.info("handling timeout");
                studyService.exportUpdatedStudies();
            } catch (Throwable e) {
                mailService.sendExportErrorNotification(vdcNetworkService.find().getSystemEmail(), vdcNetworkService.find().getName());
                logException(e, logger);
            }
        }
        */

    }

    public void removeHarvestTimers() {
        // Remove all the harvest timers, if exist: 
        //
        // (the logging messages below are set to level INFO; it's ok, 
        // since this code is only called on startup of the application, 
        // and it may be useful to know what existing timers were encountered).
        
        logger.log(Level.INFO,"Removing existing harvest timers..");
        
        int i = 1; 
        for (Iterator it = timerService.getTimers().iterator(); it.hasNext();) {
             
            Timer timer = (Timer) it.next();
            logger.log(Level.INFO, "HarvesterService: checking timer "+i);
            
            if (timer.getInfo() instanceof HarvestTimerInfo) {
                logger.log(Level.INFO, "HarvesterService: timer "+i+" is a harvesting one; removing.");
                timer.cancel();
            }
            
            i++; 
        }
    }

    private void createHarvestTimer(Dataverse harvestedDataverse) {
        HarvestingClient harvestedDataverseConfig = harvestedDataverse.getHarvestingClientConfig();
        
        if (harvestedDataverseConfig == null) {
            logger.info("ERROR: No Harvesting Configuration found for dataverse id="+harvestedDataverse.getId());
            return;
        }        
        
        if (harvestedDataverseConfig.isScheduled()) {
            long intervalDuration = 0;
            Calendar initExpiration = Calendar.getInstance();
            initExpiration.set(Calendar.MINUTE, 0);
            initExpiration.set(Calendar.SECOND, 0);
            if (harvestedDataverseConfig.getSchedulePeriod().equals(HarvestingClient.SCHEDULE_PERIOD_DAILY)) {
                intervalDuration = 1000 * 60 * 60 * 24;
                initExpiration.set(Calendar.HOUR_OF_DAY, harvestedDataverseConfig.getScheduleHourOfDay());

            } else if (harvestedDataverseConfig.getSchedulePeriod().equals(harvestedDataverseConfig.SCHEDULE_PERIOD_WEEKLY)) {
                intervalDuration = 1000 * 60 * 60 * 24 * 7;
                initExpiration.set(Calendar.HOUR_OF_DAY, harvestedDataverseConfig.getScheduleHourOfDay());
                initExpiration.set(Calendar.DAY_OF_WEEK, harvestedDataverseConfig.getScheduleDayOfWeek());

            } else {
                logger.log(Level.WARNING, "Could not set timer for harvestedDataverse id, " + harvestedDataverse.getId() + ", unknown schedule period: " + harvestedDataverseConfig.getSchedulePeriod());
                return;
            }
            Date initExpirationDate = initExpiration.getTime();
            Date currTime = new Date();
            if (initExpirationDate.before(currTime)) {
                initExpirationDate.setTime(initExpiration.getTimeInMillis() + intervalDuration);
            }
            logger.log(Level.INFO, "Setting timer for dataverse " + harvestedDataverse.getName() + ", initial expiration: " + initExpirationDate);
            createTimer(initExpirationDate, intervalDuration, new HarvestTimerInfo(harvestedDataverse.getId(), harvestedDataverse.getName(), harvestedDataverseConfig.getSchedulePeriod(), harvestedDataverseConfig.getScheduleHourOfDay(), harvestedDataverseConfig.getScheduleDayOfWeek()));
        }
    }

    public void updateHarvestTimer(Dataverse harvestedDataverse) {
        removeHarvestTimer(harvestedDataverse);
        createHarvestTimer(harvestedDataverse);
    }

    
    public void removeHarvestTimer(Dataverse harvestedDataverse) {
         // Clear dataverse timer, if one exists
        try {
            logger.log(Level.INFO,"Removing harvest timer on " + InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException ex) {
            Logger.getLogger(DataverseTimerServiceBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Iterator it = timerService.getTimers().iterator(); it.hasNext();) {
            Timer timer = (Timer) it.next();
            if (timer.getInfo() instanceof HarvestTimerInfo) {
                HarvestTimerInfo info = (HarvestTimerInfo) timer.getInfo();
                if (info.getHarvestingDataverseId().equals(harvestedDataverse.getId())) {
                    timer.cancel();
                }
            }
        }
    }

    public void createExportTimer() {
            /* Not yet implemented. The DVN 3 implementation can be used as a model */

    }

    public void createExportTimer(Dataverse dataverse) {
         /* Not yet implemented. The DVN 3 implementation can be used as a model */

    }

     public void removeExportTimer() {
         /* Not yet implemented. The DVN 3 implementation can be used as a model */
    }

    /* Utility methods: */ 
    private void logException(Throwable e, Logger logger) {

        boolean cause = false;
        String fullMessage = "";
        do {
            String message = e.getClass().getName() + " " + e.getMessage();
            if (cause) {
                message = "\nCaused By Exception.................... " + e.getClass().getName() + " " + e.getMessage();
            }
            StackTraceElement[] ste = e.getStackTrace();
            message += "\nStackTrace: \n";
            for (int m = 0; m < ste.length; m++) {
                message += ste[m].toString() + "\n";
            }
            fullMessage += message;
            cause = true;
        } while ((e = e.getCause()) != null);
        logger.severe(fullMessage);
    }
    
}