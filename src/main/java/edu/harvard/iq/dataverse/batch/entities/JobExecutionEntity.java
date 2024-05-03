package edu.harvard.iq.dataverse.batch.entities;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JobExecutionEntity {

    private long id;

    private String name;

    private BatchStatus status;

    private String exitStatus;

    private Date createTime;

    private Date endTime;

    private Date lastUpdateTime;

    private Date startTime;

    private Map<String, String> properties;

    private List<StepExecutionEntity> steps;


    public JobExecutionEntity() { }

    public static JobExecutionEntity create(final JobExecution jobExecution) {

        JobOperator jobOperator = BatchRuntime.getJobOperator();

        final JobExecutionEntity result = new JobExecutionEntity();
        result.id = jobExecution.getExecutionId();
        result.name = jobExecution.getJobName();
        result.status = jobExecution.getBatchStatus();
        result.createTime = new Date(jobExecution.getCreateTime().getTime());
        result.lastUpdateTime = new Date(jobExecution.getLastUpdatedTime().getTime());
        result.startTime = new Date(jobExecution.getStartTime().getTime());

        if (jobExecution.getExitStatus() != null) {
            result.exitStatus = jobExecution.getExitStatus();
            result.endTime = new Date(jobExecution.getEndTime().getTime());
        }

        // job parameters
        result.properties = new LinkedHashMap<>();
        final Properties props = jobOperator.getParameters(jobExecution.getExecutionId());
        if (props != null) {
            for (String name : props.stringPropertyNames()) {
                result.properties.put(name, props.getProperty(name));
            }
        }

        // steps
        result.steps = new ArrayList<>();
        List<StepExecution> stepExecutionList = jobOperator.getStepExecutions(jobExecution.getExecutionId());
        if (stepExecutionList.size() > 0) {
            for (StepExecution step : stepExecutionList) {
                result.steps.add(StepExecutionEntity.create(step));
            }
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<StepExecutionEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<StepExecutionEntity> steps) {
        this.steps = steps;
    }
}