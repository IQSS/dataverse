package edu.harvard.iq.dataverse.globus;

import java.util.ArrayList;

public class AccessList {
    private int length;
    private String endpoint;
    private ArrayList<Permissions> DATA;

    public void setDATA(ArrayList<Permissions> DATA) {
        this.DATA = DATA;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public ArrayList<Permissions> getDATA() {
        return DATA;
    }

    public int getLength() {
        return length;
    }
}
