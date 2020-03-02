/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.persistence.dataset;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.regex.Pattern;


/**
 * @author skraffmiller
 */

public class DatasetAuthor {

    public static Comparator<DatasetAuthor> DisplayOrder = new Comparator<DatasetAuthor>() {
        @Override
        public int compare(DatasetAuthor o1, DatasetAuthor o2) {
            return o1.getDisplayOrder() - o2.getDisplayOrder();
        }
    };

    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion metadata) {
        this.datasetVersion = metadata;
    }

    //@NotBlank(message = "Please enter an Author Name for your dataset.")
    private DatasetField name;

    public DatasetField getName() {
        return this.name;
    }

    public void setName(DatasetField name) {
        this.name = name;
    }

    private int displayOrder;

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private DatasetField affiliation;

    public DatasetField getAffiliation() {
        return this.affiliation;
    }

    public void setAffiliation(DatasetField affiliation) {
        this.affiliation = affiliation;
    }

    private String idType;

    public String getIdType() {
        if ((this.idType == null || this.idType.isEmpty()) && (this.idValue != null && !this.idValue.isEmpty())) {
            return ("ORCID");
        } else {
            return idType;
        }
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    private String idValue;


    public String getIdValue() {
        return idValue;
    }

    public void setIdValue(String idValue) {
        this.idValue = idValue;
    }

    public boolean isEmpty() {
        return ((affiliation == null || StringUtils.isBlank(affiliation.getValue()))
                && (name == null || StringUtils.isBlank(name.getValue()))
        );
    }

    /**
     * https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
     */
    final public static String REGEX_ORCID = "^\\d{4}-\\d{4}-\\d{4}-(\\d{4}|\\d{3}X)$";
    final public static String REGEX_ISNI = "^\\d*$";
    final public static String REGEX_LCNA = "^[a-z]+\\d+$";
    final public static String REGEX_VIAF = "^\\d*$";
    /**
     * GND regex from https://www.wikidata.org/wiki/Property:P227
     */
    final public static String REGEX_GND = "^1[01]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X]$";

    /**
     * Each author identification type has its own valid pattern/syntax.
     */
    public static Pattern getValidPattern(String regex) {
        return Pattern.compile(regex);
    }

    public String getIdentifierAsUrl() {
        if (idType != null && !idType.isEmpty() && idValue != null && !idValue.isEmpty()) {
            DatasetFieldValidator datasetFieldValueValidator = new DatasetFieldValidator();
            switch (idType) {
                case "ORCID":
                    if (datasetFieldValueValidator.isValidAuthorIdentifier(idValue, getValidPattern(REGEX_ORCID))) {
                        return "https://orcid.org/" + idValue;
                    }
                    break;
                case "ISNI":
                    if (datasetFieldValueValidator.isValidAuthorIdentifier(idValue, getValidPattern(REGEX_ISNI))) {
                        return "http://www.isni.org/isni/" + idValue;
                    }
                    break;
                case "LCNA":
                    if (datasetFieldValueValidator.isValidAuthorIdentifier(idValue, getValidPattern(REGEX_LCNA))) {
                        return "http://id.loc.gov/authorities/names/" + idValue;
                    }
                    break;
                case "VIAF":
                    if (datasetFieldValueValidator.isValidAuthorIdentifier(idValue, getValidPattern(REGEX_VIAF))) {
                        return "https://viaf.org/viaf/" + idValue;
                    }
                    break;
                case "GND":
                    if (datasetFieldValueValidator.isValidAuthorIdentifier(idValue, getValidPattern(REGEX_GND))) {
                        return "https://d-nb.info/gnd/" + idValue;
                    }
                    break;
                default:
                    break;
            }
        }
        return null;
    }

}
