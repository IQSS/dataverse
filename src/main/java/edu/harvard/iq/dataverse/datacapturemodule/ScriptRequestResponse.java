package edu.harvard.iq.dataverse.datacapturemodule;

public class ScriptRequestResponse {

    private final int httpStatusCode;
    private final long datasetId;
    private final long userId;
    private final String script;

    public ScriptRequestResponse(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        this.datasetId = -1;
        this.userId = -1;
        this.script = null;
    }

    public ScriptRequestResponse(int httpStatusCode, long datasetId, long userId, String script) {
        this.httpStatusCode = httpStatusCode;
        this.datasetId = datasetId;
        this.userId = userId;
        this.script = script;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public long getDatasetId() {
        return datasetId;
    }

    public long getUserId() {
        return userId;
    }

    public String getScript() {
        return script;
    }

}
