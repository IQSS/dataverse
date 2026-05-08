package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.ApiConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload for exporting multiple datasets with a single exporter.
 *
 * <p>Each request identifies the exporter to use and a list of dataset selections.
 * Every dataset selection couples a persistent identifier with a version
 * identifier so the server can resolve the exact dataset version to export.
 *
 * <p>This request model is intended for JSON request bodies sent to a bulk or
 * multi-dataset export endpoint.
 *
 * @param exporter the name or identifier of the exporter to use; must not be blank
 * @param datasets the datasets to export; must not be {@code null} and at least have 1 element.
 *                 The list is defensively copied into an unmodifiable snapshot.
 */
public record MultiDatasetExportRequest(
    @NotBlank String exporter,
    @NotNull @Size(min = 1) List<ExportItem> datasets
) {
    /**
     * Creates a new multi-dataset export request. The supplied dataset list is copied to preserve
     * immutability and prevent later external modification of the request contents.
     * @throws NullPointerException if {@code datasets} is {@code null}
     */
    public MultiDatasetExportRequest {
        // Make sure to create a readonly copy, but keep null around to have bean validation catch it and complain later
        datasets = datasets == null ? null : List.copyOf(datasets);
    }
    
    /**
     * A single dataset selection within a {@link MultiDatasetExportRequest}.
     *
     * <p>Each item identifies one dataset to export by its persistent identifier and the specific version to resolve.
     * The version may be one of the symbolic dataset version identifiers supported by the API, or a numeric version
     * identifier, as validated by {@link ApiConstants#DS_VERSION_IDENTIFIER_REGEX}.
     *
     * @param persistentId the persistent identifier of the dataset to export; must not be blank
     * @param version the dataset version identifier to export;
     *                must not be blank and must match the API-supported dataset version syntax
     */
    public record ExportItem(
        @NotBlank String persistentId,
        @NotBlank
        @Pattern(
            regexp = ApiConstants.DS_VERSION_IDENTIFIER_REGEX,
            // TODO: replace message with bundle reference
            message = ApiConstants.DS_VERSION_IDENTIFIER_MESSAGE
        )
        String version
    ) {
        // If omitted or blank, apply a default version to look up (aligned with GET endpoint behavior)
        public ExportItem {
            if (version == null || version.isBlank()) {
                version = ApiConstants.DS_VERSION_LATEST_PUBLISHED;
            }
        }
    }
}
