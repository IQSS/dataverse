package edu.harvard.iq.dataverse;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.util.Date;
import java.util.logging.Logger;

public class DeleteOneTimeUrlTask {

  private  Scheduler quartz;

  private S3BigDataUploadServiceBean s3BigDataUploadService;

  private final String onTimeUrl;

  private int schedulerPeriod;


  Logger log = Logger.getLogger(DeleteOneTimeUrlTask.class.getCanonicalName());

  public DeleteOneTimeUrlTask(String onTimeUrl, int schedulerPeriod, S3BigDataUploadServiceBean s3BigDataUploadService) {
    this.onTimeUrl = onTimeUrl;
    this.s3BigDataUploadService = s3BigDataUploadService;
    this.schedulerPeriod = schedulerPeriod;

    try {

      //Creating scheduler
      quartz = new StdSchedulerFactory().getScheduler();


      //Creating job and link to Runner
      JobDetailImpl job = new JobDetailImpl();
      job.setName("DeleteExpired");
      job.setJobClass(Runner.class);
      job.setDurability(true);
      job.getJobDataMap().put("task", this);

      //Strating scheduler
      quartz.start();
      quartz.addJob(job,true);

    } catch (SchedulerException e) {
      log.warning(e.getLocalizedMessage());
    }


  }

  public void schedule() {
    if (quartz == null || schedulerPeriod <= 0) {
      log.info("Cancel scheduling of deleting task due to invalid scheduling period");
      return;
    }
    try {
      //Creating schedule with trigger, only one execution
      SimpleTriggerImpl trigger = new SimpleTriggerImpl();
      trigger.setStartTime(new Date(System.currentTimeMillis() + schedulerPeriod));
      trigger.setRepeatCount(0);
      trigger.setRepeatInterval(0L);
      trigger.setJobName("DeleteExpired");
      trigger.setName("DeleteExpiredTrigger");
      quartz.scheduleJob(trigger);
    } catch (SchedulerException e) {
      log.warning("Error scheduling Quartz Job");
    }
  }

  public S3BigDataUploadServiceBean getS3BigDataUploadService() {
    return s3BigDataUploadService;
  }

  public String getOnTimeUrl() {
    return onTimeUrl;
  }

  public static class Runner implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      try {
        execute((DeleteOneTimeUrlTask) jobExecutionContext.getJobDetail().getJobDataMap().get("task"));
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while getting new Series");
      }
    }

    private void execute(DeleteOneTimeUrlTask task) {
      task.getS3BigDataUploadService().deleteS3BigDataUploadByUrl(task.getOnTimeUrl());
    }
  }


}
