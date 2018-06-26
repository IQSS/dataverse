package edu.harvard.iq.dataverse;

import java.util.Locale;

public class DataverseLocale {

    private int id;
    private String displayName;
    private Object name;

    public DataverseLocale() {}

    public DataverseLocale(int id, String displayName, Locale name) {
        this.id = id;
        this.displayName = displayName;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Object getName() {
        return name;
    }

    public void setName(Locale name) {
        this.name = name;
    }


}
