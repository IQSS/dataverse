package edu.harvard.iq.dataverse.api.dto;

import com.google.gson.annotations.SerializedName;

public class RorEntryDTO {

    private String id;

    private String name;

    private Country country;

    @SerializedName("addresses")
    private City[] cities;

    private String[] links;

    private String[] acronyms;

    private String[] aliases;

    private Label[] labels;

    // -------------------- CONSTRUCTORS --------------------

    public RorEntryDTO() { }

    // -------------------- GETTERS --------------------

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Country getCountry() {
        return country;
    }

    public City[] getCities() {
        return cities;
    }

    public String[] getLinks() {
        return links;
    }

    public String[] getAcronyms() {
        return acronyms;
    }

    public String[] getAliases() {
        return aliases;
    }

    public Label[] getLabels() {
        return labels;
    }

    // -------------------- SETTERS --------------------

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public void setCities(City[] cities) {
        this.cities = cities;
    }

    public void setLinks(String[] links) {
        this.links = links;
    }

    public void setAcronyms(String[] acronyms) {
        this.acronyms = acronyms;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public void setLabels(Label[] labels) {
        this.labels = labels;
    }

    // -------------------- INNER CLASSES --------------------

    public static class City {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public static class Label {
        private String label;
        private String iso639;

        public String getLabel() {
            return label;
        }

        public String getIso639() {
            return iso639;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setIso639(String iso639) {
            this.iso639 = iso639;
        }
    }

    public static class Country {
        @SerializedName("country_name")
        private String countryName;

        @SerializedName("country_code")
        private String countryCode;

        public String getCountryName() {
            return countryName;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryName(String countryName) {
            this.countryName = countryName;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }
}
