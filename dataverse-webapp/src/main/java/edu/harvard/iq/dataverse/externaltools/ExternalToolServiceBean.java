package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.ReservedWord;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalToolRepository;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class ExternalToolServiceBean {

    private static final Logger logger = Logger.getLogger(ExternalToolServiceBean.class.getCanonicalName());

    private ExternalToolRepository repository;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ExternalToolServiceBean() { }

    @Inject
    public ExternalToolServiceBean(ExternalToolRepository repository) {
        this.repository = repository;
    }

    // -------------------- LOGIC --------------------

    public List<ExternalTool> findAll() {
        return repository.findAll();
    }

    /**
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findByType(Type type) {
        return repository.findByType(type, null);
    }

    /**
     * @return A list of tools or an empty list.
     */
    public List<ExternalTool> findByType(Type type, String contentType) {
        return repository.findByType(type, contentType);
    }

    public boolean delete(long doomedId) {
        if (repository.findById(doomedId).isPresent()) {
            repository.deleteById(doomedId);
            return true;
        }
        return false;
    }

    public ExternalTool save(ExternalTool externalTool) {
        return repository.save(externalTool);
    }

    /**
     * Should be used only in REST (ie. where it's currently used). For the other
     * cases use the method {@link ExternalToolServiceBean#findExternalToolsByFileAndVersion(List, DataFile, DatasetVersion)}
     */
    public static List<ExternalTool> findExternalToolsByFile(List<ExternalTool> allExternalTools, DataFile file) {
        // Map tabular data to it's mimetype (the isTabularData() check assures that this code works the same as before,
        // but it may need to change if tabular data is split into subtypes with differing mimetypes)
        final String contentType = file.isTabularData() ? TextMimeType.TSV_ALT.getMimeValue() : file.getContentType();

        return allExternalTools.stream()
                .filter(t -> t.getContentType().equals(contentType))
                .collect(Collectors.toList());
    }

    /**
     * This method takes a list of tools, a file and a dataset version and
     * returns which tools that file supports. The list of tools is passed in
     * so it doesn't query the database each time
     */
    public List<ExternalTool> findExternalToolsByFileAndVersion(
            List<ExternalTool> allExternalTools, DataFile file, DatasetVersion datasetVersion) {

        // Map tabular data to it's mimetype (the isTabularData() check assures that this code works the same as before,
        // but it may need to change if tabular data is split into subtypes with differing mimetypes)
        final String contentType = file.isTabularData() ? TextMimeType.TSV_ALT.getMimeValue() : file.getContentType();

        return allExternalTools.stream()
                .filter(t -> t.getContentType().equals(contentType))
                .filter(t -> !isNonPublicOrNotIngestedTsvFile(file, datasetVersion))
                .collect(Collectors.toList());
    }

    public List<ExternalTool> findExternalTools(Type type, String contentType, DataFile file, DatasetVersion version) {
        return findExternalToolsByFileAndVersion(findByType(type, contentType), file, version);
    }

    public ExternalTool parseAddExternalToolManifest(String manifest) {
        if (manifest == null || manifest.isEmpty()) {
            throw new IllegalArgumentException("External tool manifest was null or empty!");
        }
        JsonReader jsonReader = Json.createReader(new StringReader(manifest));
        JsonObject jsonObject = jsonReader.readObject();
        // Note: ExternalToolServiceBeanTest tests are dependent on the order of these retrievals
        String displayName = getRequiredTopLevelField(jsonObject, ExternalTool.DISPLAY_NAME);
        String description = getRequiredTopLevelField(jsonObject, ExternalTool.DESCRIPTION);
        String typeUserInput = getRequiredTopLevelField(jsonObject, ExternalTool.TYPE);
        String contentType = getOptionalTopLevelField(jsonObject, ExternalTool.CONTENT_TYPE);
        // Legacy support - assume tool manifests without any mimetype are for tabular data
        if (contentType == null) {
            contentType = TextMimeType.TSV_ALT.getMimeValue();
        }

        // Allow IllegalArgumentException to bubble up from ExternalTool.Type.fromString
        ExternalTool.Type type = ExternalTool.Type.fromString(typeUserInput);
        String toolUrl = getRequiredTopLevelField(jsonObject, ExternalTool.TOOL_URL);
        JsonObject toolParametersObj = jsonObject.getJsonObject(ExternalTool.TOOL_PARAMETERS);
        JsonArray queryParams = toolParametersObj.getJsonArray("queryParameters");
        boolean allRequiredReservedWordsFound = false;
        for (JsonObject queryParam : queryParams.getValuesAs(JsonObject.class)) {
            Set<String> keyValuePair = queryParam.keySet();
            for (String key : keyValuePair) {
                String value = queryParam.getString(key);
                ReservedWord reservedWord = ReservedWord.fromString(value);
                if (reservedWord.equals(ReservedWord.FILE_ID)) {
                    allRequiredReservedWordsFound = true;
                }
            }
        }
        if (!allRequiredReservedWordsFound) {
            // Some day there might be more reserved words than just {fileId}.
            throw new IllegalArgumentException("Required reserved word not found: " + ReservedWord.FILE_ID.toString());
        }
        String toolParameters = toolParametersObj.toString();
        return new ExternalTool(displayName, description, type, toolUrl, toolParameters, contentType);
    }

    // -------------------- PRIVATE --------------------

    private boolean isNonPublicOrNotIngestedTsvFile(DataFile file, DatasetVersion datasetVersion) {
        boolean isTsvAltContentType = TextMimeType.TSV_ALT.getMimeValue()
                .equals(file.isTabularData() ? TextMimeType.TSV_ALT.getMimeValue() : file.getContentType());

        return isTsvAltContentType && (!isFilePublic(file, datasetVersion) || !file.isTabularData());
    }

    private boolean isFilePublic(DataFile file, DatasetVersion datasetVersion) {
        boolean released = datasetVersion.isReleased();
        boolean embargoed = file.getOwner().hasActiveEmbargo();
        boolean restricted = file.getFileMetadata().getTermsOfUse().getTermsOfUseType() == FileTermsOfUse.TermsOfUseType.RESTRICTED;

        return released && !embargoed && !restricted;
    }

    private String getRequiredTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(key + " is required.");
        }
    }

    private String getOptionalTopLevelField(JsonObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (NullPointerException ex) {
            return null;
        }
    }
}
