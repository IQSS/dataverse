package edu.harvard.iq.dataverse.datacapturemodule;

public class ScriptRequestResponse {

    private final int httpStatusCode;
    private final long datasetId;//TODO[pm] - depreciate
    private final String datasetIdentifier;
    private final long userId;
    private final String script;

    public ScriptRequestResponse(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        this.datasetId = -1;
        this.userId = -1;
        this.script = null;
        this.datasetIdentifier = null;
    }
    public ScriptRequestResponse(int httpStatusCode, String datasetIdentifier, long userId, String script)
    {
        this.httpStatusCode = httpStatusCode;
        this.datasetIdentifier = datasetIdentifier;
        this.userId = userId;
        this.script = script;
        this.datasetId = -1;
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

    public String getDatasetIdentifier()
    {
        return datasetIdentifier;
    }

}
