package edu.harvard.iq.dataverse.batch.entities;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StepExecutionEntity {

    private long id;

    private String name;

    private BatchStatus status;

    private String exitStatus;

    private Date endTime;

    private Date startTime;

    private Map<String, Long> metrics;

    private String persistentUserData;

    public static StepExecutionEntity create(final StepExecution stepExecution) {

        final StepExecutionEntity result = new StepExecutionEntity();
        result.id = stepExecution.getStepExecutionId();
        result.name = stepExecution.getStepName();
        result.status = stepExecution.getBatchStatus();
        result.exitStatus = stepExecution.getExitStatus();
        result.endTime = stepExecution.getEndTime();
        result.startTime = stepExecution.getStartTime();

        // metrics
        result.metrics = new HashMap<>();
        final Metric[] metricArr = stepExecution.getMetrics();
        for (Metric m : metricArr) {
            result.metrics.put(m.getType().name().toLowerCase(), m.getValue());
        }

        if (stepExecution.getPersistentUserData() != null) {
            result.setPersistentUserData(stepExecution.getPersistentUserData().toString());
        }

        return result;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Map<String, Long> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Long> metrics) {
        this.metrics = metrics;
    }

    public String getPersistentUserData() {
        return persistentUserData;
    }

    public void setPersistentUserData(String persistentUserData) {
        this.persistentUserData = persistentUserData;
    }
}
