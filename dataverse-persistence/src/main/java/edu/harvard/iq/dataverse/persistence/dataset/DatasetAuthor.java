package edu.harvard.iq.dataverse.persistence.dataset;

import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.regex.Pattern;


/**
 * @author skraffmiller
 */
public class DatasetAuthor {

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

    public static Comparator<DatasetAuthor> displayOrderComparator = Comparator.comparingInt(DatasetAuthor::getDisplayOrder);


    private DatasetVersion datasetVersion;

    private DatasetField name;

    private int displayOrder;

    private DatasetField affiliation;

    private DatasetField affiliationIdentifier;

    private String idType;

    private String idValue;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetAuthor() { }

    public DatasetAuthor(int displayOrder) {
        this.displayOrder = displayOrder;
    }



    // -------------------- GETTERS --------------------

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public DatasetField getName() {
        return this.name;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public DatasetField getAffiliation() {
        return this.affiliation;
    }

    public DatasetField getAffiliationIdentifier() {
        return affiliationIdentifier;
    }

    public String getIdValue() {
        return idValue;
    }

    // -------------------- LOGIC --------------------

    public String getIdType() {
        if ((this.idType == null || this.idType.isEmpty()) && (this.idValue != null && !this.idValue.isEmpty())) {
            return ("ORCID");
        } else {
            return idType;
        }
    }

    public boolean isEmpty() {
        return ((affiliation == null || StringUtils.isBlank(affiliation.getValue()))
                && (name == null || StringUtils.isBlank(name.getValue()))
        );
    }

    /**
     * Each author identification type has its own valid pattern/syntax.
     */
    public static Pattern getValidPattern(String regex) {
        return Pattern.compile(regex);
    }

    public String getIdentifierAsUrl() {
        if (idType != null && !idType.isEmpty() && idValue != null && !idValue.isEmpty()) {
            return getIdentifierAsUrl(idType, idValue)
                    .getOrNull();
        }
        return null;
    }

    /**
     * Returns the URL for the provided id value of the given type, or none if:
     * - unknown type
     * - invalid id
     */
    public static Option<String> getIdentifierAsUrl(String idType, String idValue) {
            switch (idType) {
                case "ORCID":
                    if (isValidAuthorIdentifier(idValue, getValidPattern(REGEX_ORCID))) {
                        return Option.some("https://orcid.org/" + idValue);
                    }
                    break;
                case "ISNI":
                    if (isValidAuthorIdentifier(idValue, getValidPattern(REGEX_ISNI))) {
                        return Option.some("http://www.isni.org/isni/" + idValue);
                    }
                    break;
                case "LCNA":
                    if (isValidAuthorIdentifier(idValue, getValidPattern(REGEX_LCNA))) {
                        return Option.some("http://id.loc.gov/authorities/names/" + idValue);
                    }
                    break;
                case "VIAF":
                    if (isValidAuthorIdentifier(idValue, getValidPattern(REGEX_VIAF))) {
                        return Option.some("https://viaf.org/viaf/" + idValue);
                    }
                    break;
                case "GND":
                    if (isValidAuthorIdentifier(idValue, getValidPattern(REGEX_GND))) {
                        return Option.some("https://d-nb.info/gnd/" + idValue);
                    }
                    break;
                default:
                    break;
            }
            return Option.none();
    }

    // -------------------- PRIVATE --------------------

    private static boolean isValidAuthorIdentifier(String userInput, Pattern pattern) {
        return pattern.matcher(userInput).matches();
    }

    // -------------------- SETTERS --------------------

    public void setDatasetVersion(DatasetVersion metadata) {
        this.datasetVersion = metadata;
    }

    public void setName(DatasetField name) {
        this.name = name;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setAffiliation(DatasetField affiliation) {
        this.affiliation = affiliation;
    }

    public void setAffiliationIdentifier(DatasetField affiliationIdentifier) {
        this.affiliationIdentifier = affiliationIdentifier;
    }

    public void setIdValue(String idValue) {
        this.idValue = idValue;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }
}
