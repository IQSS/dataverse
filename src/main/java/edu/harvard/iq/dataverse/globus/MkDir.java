package edu.harvard.iq.dataverse.globus;

public class MkDir {
    private String DATA_TYPE;
    private String path;

    public void setDataType(String DATA_TYPE) {
        this.DATA_TYPE = DATA_TYPE;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDataType() {
        return DATA_TYPE;
    }

    public String getPath() {
        return path;
    }
}
