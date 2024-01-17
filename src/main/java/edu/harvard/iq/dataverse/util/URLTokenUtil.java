package edu.harvard.iq.dataverse.util;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonValue;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;

public class URLTokenUtil {

    protected static final Logger logger = Logger.getLogger(URLTokenUtil.class.getCanonicalName());
    protected final DataFile dataFile;
    protected final Dataset dataset;
    protected final FileMetadata fileMetadata;
    protected ApiToken apiToken;
    protected String localeCode;

    /**
     * File level
     *
     * @param dataFile     Required.
     * @param apiToken     The apiToken can be null
     * @param fileMetadata Required.
     * @param localeCode   optional.
     * 
     */
    public URLTokenUtil(DataFile dataFile, ApiToken apiToken, FileMetadata fileMetadata, String localeCode)
            throws IllegalArgumentException {
        if (dataFile == null) {
            String error = "A DataFile is required.";
            logger.warning("Error in URLTokenUtil constructor: " + error);
            throw new IllegalArgumentException(error);
        }
        if (fileMetadata == null) {
            String error = "A FileMetadata is required.";
            logger.warning("Error in URLTokenUtil constructor: " + error);
            throw new IllegalArgumentException(error);
        }
        this.dataFile = dataFile;
        this.dataset = fileMetadata.getDatasetVersion().getDataset();
        this.fileMetadata = fileMetadata;
        this.apiToken = apiToken;
        this.localeCode = localeCode;
    }

    /**
     * Dataset level
     *
     * @param dataset  Required.
     * @param apiToken The apiToken can be null
     */
    public URLTokenUtil(Dataset dataset, ApiToken apiToken, String localeCode) {
        this(dataset, null, apiToken, localeCode);
    }

    /**
     * Dataset level
     *
     * @param dataset  Required.
     * @param datafile Optional.
     * @param apiToken Optional The apiToken can be null
     * @localeCode     Optional
     * 
     */
    public URLTokenUtil(Dataset dataset, DataFile datafile, ApiToken apiToken, String localeCode) {
        if (dataset == null) {
            String error = "A Dataset is required.";
            logger.warning("Error in URLTokenUtil constructor: " + error);
            throw new IllegalArgumentException(error);
        }
        this.dataset = dataset;
        this.dataFile = datafile;
        this.fileMetadata = null;
        this.apiToken = apiToken;
        this.localeCode = localeCode;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public String getLocaleCode() {
        return localeCode;
    }
    
    public JsonValue getParam(String value) {
        String tokenValue = null;
        tokenValue = getTokenValue(value);
        if (tokenValue != null && !tokenValue.isBlank()) {
            try{
                int x =Integer.parseInt(tokenValue);
                return Json.createValue(x);
            } catch (NumberFormatException nfe){
                return Json.createValue(tokenValue);
            }
        } else {
            return null;
        }
    }

    /**
     * Tries to replace all occurrences of {<text>} with the value for the
     * corresponding ReservedWord
     * 
     * @param url - the input string in which to replace tokens, normally a url
     * @throws IllegalArgumentException if there is no matching ReservedWord or if
     *                                  the configuation of this instance doesn't
     *                                  have values for this ReservedWord (e.g.
     *                                  asking for FILE_PID when using the dataset
     *                                  constructor, etc.)
     */
    public String replaceTokensWithValues(String url) {
        String newUrl = url;
        Pattern pattern = Pattern.compile("(\\{.*?\\})");
        Matcher matcher = pattern.matcher(url);
        while(matcher.find()) {
            String token = matcher.group(1);
            ReservedWord reservedWord = ReservedWord.fromString(token);
            String tValue = getTokenValue(token);
            logger.fine("Replacing " + reservedWord.toString() + " with " + tValue + " in " + newUrl);
            newUrl = newUrl.replace(reservedWord.toString(), tValue);
        }
        return newUrl;
    }

    private String getTokenValue(String value) {
        ReservedWord reservedWord = ReservedWord.fromString(value);
        switch (reservedWord) {
        case FILE_ID:
            // getDataFile is never null for file tools because of the constructor
            return getDataFile().getId().toString();
        case FILE_PID:
            GlobalId filePid = getDataFile().getGlobalId();
            if (filePid != null) {
                return getDataFile().getGlobalId().asString();
            }
            break;
        case SITE_URL:
            return SystemConfig.getDataverseSiteUrlStatic();
        case API_TOKEN:
            String apiTokenString = null;
            ApiToken theApiToken = getApiToken();
            if (theApiToken != null) {
                apiTokenString = theApiToken.getTokenString();
            }
            return apiTokenString;
        case DATASET_ID:
            return dataset.getId().toString();
        case DATASET_PID:
            return dataset.getGlobalId().asString();
        case DATASET_VERSION:
            String versionString = null;
            if (fileMetadata != null) { // true for file case
                versionString = fileMetadata.getDatasetVersion().getFriendlyVersionNumber();
            } else { // Dataset case - return the latest visible version (unless/until the dataset
                     // case allows specifying a version)
                if (getApiToken() != null) {
                    versionString = dataset.getLatestVersion().getFriendlyVersionNumber();
                } else {
                    versionString = dataset.getLatestVersionForCopy().getFriendlyVersionNumber();
                }
            }
            if (("DRAFT").equals(versionString)) {
                versionString = ":draft"; // send the token needed in api calls that can be substituted for a numeric
                                          // version.
            }
            return versionString;
        case FILE_METADATA_ID:
            if (fileMetadata != null) { // true for file case
                return fileMetadata.getId().toString();
            }
        case LOCALE_CODE:
            return getLocaleCode();
        default:
            break;
        }
        throw new IllegalArgumentException("Cannot replace reserved word: " + value);
    }
    
    public static String getScriptForUrl(String url) {
        String msg = BundleUtil.getStringFromBundle("externaltools.enable.browser.popups");
        String script = "const newWin = window.open('" + url + "', target='_blank'); if (!newWin || newWin.closed || typeof newWin.closed == \"undefined\") {alert(\"" + msg + "\");}";
        return script;
   }

    public enum ReservedWord {

        // TODO: Research if a format like "{reservedWord}" is easily parse-able or if
        // another format would be
        // better. The choice of curly braces is somewhat arbitrary, but has been
        // observed in documentation for
        // various REST APIs. For example, "Variable substitutions will be made when a
        // variable is named in {brackets}."
        // from https://swagger.io/specification/#fixed-fields-29 but that's for URLs.
        FILE_ID("fileId"), FILE_PID("filePid"), SITE_URL("siteUrl"), API_TOKEN("apiToken"),
        // datasetId is the database id
        DATASET_ID("datasetId"),
        // datasetPid is the DOI or Handle
        DATASET_PID("datasetPid"), DATASET_VERSION("datasetVersion"), FILE_METADATA_ID("fileMetadataId"),
        LOCALE_CODE("localeCode");

        private final String text;
        private final String START = "{";
        private final String END = "}";

        private ReservedWord(final String text) {
            this.text = START + text + END;
        }

        /**
         * This is a centralized method that enforces that only reserved words are
         * allowed to be used by external tools. External tool authors cannot pass their
         * own query parameters through Dataverse such as "mode=mode1".
         *
         * @throws IllegalArgumentException
         */
        public static ReservedWord fromString(String text) throws IllegalArgumentException {
            if (text != null) {
                for (ReservedWord reservedWord : ReservedWord.values()) {
                    if (text.equals(reservedWord.text)) {
                        return reservedWord;
                    }
                }
            }
            // TODO: Consider switching to a more informative message that enumerates the
            // valid reserved words.
            boolean moreInformativeMessage = false;
            if (moreInformativeMessage) {
                throw new IllegalArgumentException(
                        "Unknown reserved word: " + text + ". A reserved word must be one of these values: "
                                + Arrays.asList(ReservedWord.values()) + ".");
            } else {
                throw new IllegalArgumentException("Unknown reserved word: " + text);
            }
        }

        @Override
        public String toString() {
            return text;
        }
    }
}