/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 * @author skraffmiller
 */
public class DatasetAuthor {
       
    public static Comparator<DatasetAuthor> DisplayOrder = new Comparator<DatasetAuthor>(){
        @Override
        public int compare(DatasetAuthor o1, DatasetAuthor o2) {
            return o1.getDisplayOrder()-o2.getDisplayOrder();
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
        if ((this.idType == null || this.idType.isEmpty()) && (this.idValue != null && !this.idValue.isEmpty())){
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
        return ( (affiliation==null || affiliation.getValue().trim().equals(""))
            && (name==null || name.getValue().trim().equals(""))
           );
    }

    public static final Map<String, LinkTemplate> linkSchemeTemplates = Map.of(
        // https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
        "ORCID",
            new LinkTemplate("https://orcid.org/%s", "^\\d{4}-\\d{4}-\\d{4}-(\\d{4}|\\d{3}X)$"),
        "ISNI",
            new LinkTemplate("http://www.isni.org/isni/%s", "^\\d*$"),
        "LCNA",
            new LinkTemplate("http://id.loc.gov/authorities/names/%s", "^[a-z]+\\d+$"),
        "VIAF",
            new LinkTemplate("https://viaf.org/viaf/%s", "^\\d*$"),
        // GND regex from https://www.wikidata.org/wiki/Property:P227
        "GND",
            new LinkTemplate("https://d-nb.info/gnd/%s", "^1[01]?\\d{7}[0-9X]|[47]\\d{6}-\\d|[1-9]\\d{0,7}-[0-9X]|3\\d{7}[0-9X]$"),
        // DAI is missing from this list
        "ResearcherID",
            new LinkTemplate("https://publons.com/researcher/%s/", "^[A-Z\\d][A-Z\\d-]+[A-Z\\d]$"),
        "ScopusID",
            new LinkTemplate("https://www.scopus.com/authid/detail.uri?authorId=%s", "^\\d*$")
    );

    /**
     * Each author identification type has its own valid pattern/syntax.
     */
    public static Pattern getValidPattern(String regex) {
        return Pattern.compile(regex);
    }

    public String getIdentifierAsUrl() {
        if (idType != null && !idType.isEmpty() && idValue != null && !idValue.isEmpty()) {
            return getIdentifierAsUrl(idType, idValue);
        }
        return null;
    }

    public static String getIdentifierAsUrl(String idType, String idValue) {
        if (idType != null && !idType.isEmpty() && idValue != null && !idValue.isEmpty()) {
            DatasetFieldValueValidator datasetFieldValueValidator = new DatasetFieldValueValidator();
            if (linkSchemeTemplates.containsKey(idType)) {
                LinkTemplate template = linkSchemeTemplates.get(idType);
                if (datasetFieldValueValidator.isValidAuthorIdentifier(idValue, template.getPattern())) {
                    return String.format(template.getTemplate(), idValue);
                }
            }
        }
        return null;
    }
}
