package edu.harvard.iq.dataverse.globus;

public class GlobusTask {

    private String DATA_TYPE;
    private String type;
    private String status;
    private String owner_id;
    private String request_time;
    private String task_id;
    private String destination_endpoint_display_name;
    private boolean skip_source_errors;
    private String nice_status;
    private String nice_status_short_description;

    public String getDestination_endpoint_display_name() {
        return destination_endpoint_display_name;
    }

    public void setDestination_endpoint_display_name(String destination_endpoint_display_name) {
        this.destination_endpoint_display_name = destination_endpoint_display_name;
    }

    public void setRequest_time(String request_time) {
        this.request_time = request_time;
    }

    public String getRequest_time() {
        return request_time;
    }

    public String getTask_id() {
        return task_id;
    }

    public void setTask_id(String task_id) {
        this.task_id = task_id;
    }

    public String getDATA_TYPE() {
        return DATA_TYPE;
    }

    public void setDATA_TYPE(String DATA_TYPE) {
        this.DATA_TYPE = DATA_TYPE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(String owner_id) {
        this.owner_id = owner_id;
    }

    public Boolean getSkip_source_errors() {
        return skip_source_errors;
    }

    public void setSkip_source_errors(Boolean skip_source_errors) {
        this.skip_source_errors = skip_source_errors;
    }

    public String getNice_status() {
        return nice_status;
    }

    public void setNice_status(String nice_status) {
        this.nice_status = nice_status;
    }

    public String getNice_status_short_description() {
        return nice_status_short_description;
    }

}