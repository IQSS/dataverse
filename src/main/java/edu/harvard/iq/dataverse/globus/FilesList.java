package edu.harvard.iq.dataverse.globus;

import java.util.ArrayList;

public class FilesList {
    private ArrayList<FileG> DATA;
    private String DATA_TYPE;
    private String absolute_path;
    private String endpoint;
    private String length;
    private String path;

    public String getEndpoint() {
        return endpoint;
    }

    public ArrayList<FileG> getDATA() {
        return DATA;
    }

    public String getAbsolute_path() {
        return absolute_path;
    }

    public String getDATA_TYPE() {
        return DATA_TYPE;
    }

    public String getLength() {
        return length;
    }

    public String getPath() {
        return path;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setDATA(ArrayList<FileG> DATA) {
        this.DATA = DATA;
    }

    public void setAbsolute_path(String absolute_path) {
        this.absolute_path = absolute_path;
    }

    public void setDATA_TYPE(String DATA_TYPE) {
        this.DATA_TYPE = DATA_TYPE;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
