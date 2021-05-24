package edu.harvard.iq.dataverse.globalid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("data")
public class DataCiteFindDoiResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("attributes")
    private Attributes attributes = new Attributes();

    // -------------------- GETTERS --------------------

    public String getId() {
        return id;
    }

    public int getCitationCount() {
        return attributes.getCitationCount();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCitationCount(int citationCount) {
        attributes.setCitationCount(citationCount);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "DataCiteFindDoiResponse [id=" + id + ", citationCount=" + attributes.getCitationCount() + "]";
    }

    // -------------------- INNER CLASSES --------------------

    public static class Attributes {

        @JsonProperty("citationCount")
        private int citationCount;

        public int getCitationCount() {
            return citationCount;
        }
        public void setCitationCount(int citationCount) {
            this.citationCount = citationCount;
        }
    }
}
