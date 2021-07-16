package edu.harvard.iq.dataverse.search.ror;

import org.apache.solr.client.solrj.beans.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * Class dedicated to be an result from solr query.
 * There are strict rules for auto mapping from solr (public, non arg constructor, setters).
 */
public class RorDto {

    @Field
    private String rorId;
    @Field
    private String rorUrl;
    @Field
    private String name;
    @Field
    private String countryName;
    @Field
    private String countryCode;
    @Field
    private String city;
    @Field
    private String website;
    @Field("nameAlias")
    private List<String> nameAliases = new ArrayList<>();
    @Field("acronym")
    private List<String> acronyms = new ArrayList<>();
    @Field("label")
    private List<String> labels = new ArrayList<>();

    public RorDto() {
    }

    public RorDto(String rorId, String rorUrl, String name, String countryName, String countryCode, String city,
           String website, List<String> nameAliases, List<String> acronyms, List<String> labels) {
        this.rorId = rorId;
        this.rorUrl = rorUrl;
        this.name = name;
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.city = city;
        this.website = website;
        this.nameAliases = nameAliases;
        this.acronyms = acronyms;
        this.labels = labels;
    }

    public String getRorId() {
        return rorId;
    }

    public String getRorUrl() {
        return rorUrl;
    }

    public String getName() {
        return name;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCity() {
        return city;
    }

    public String getWebsite() {
        return website;
    }

    public List<String> getNameAliases() {
        return nameAliases;
    }

    public List<String> getAcronyms() {
        return acronyms;
    }

    public List<String> getLabels() {
        return labels;
    }

    public RorDto setRorId(String rorId) {
        this.rorId = rorId;
        return this;
    }

    public RorDto setRorUrl(String rorUrl) {
        this.rorUrl = rorUrl;
        return this;
    }

    public RorDto setName(String name) {
        this.name = name;
        return this;
    }

    public RorDto setCountryName(String countryName) {
        this.countryName = countryName;
        return this;
    }

    public RorDto setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public RorDto setCity(String city) {
        this.city = city;
        return this;
    }

    public RorDto setWebsite(String website) {
        this.website = website;
        return this;
    }

    public RorDto setNameAliases(List<String> nameAliases) {
        this.nameAliases = nameAliases;
        return this;
    }

    public RorDto setAcronyms(List<String> acronyms) {
        this.acronyms = acronyms;
        return this;
    }

    public RorDto setLabels(List<String> labels) {
        this.labels = labels;
        return this;
    }
}
