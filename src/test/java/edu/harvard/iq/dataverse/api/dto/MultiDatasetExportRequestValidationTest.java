package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.validation.ValidationUtil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MultiDatasetExportRequestValidationTest {
    
    private static ValidatorFactory validatorFactory;
    private static Validator validator;
    
    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }
    
    @AfterAll
    static void closeValidatorFactory() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }
    
    @Nested
    class ValidRequests {
        
        @ParameterizedTest
        @ValueSource(strings = {
            ApiConstants.DS_VERSION_LATEST,
            ApiConstants.DS_VERSION_DRAFT,
            ApiConstants.DS_VERSION_LATEST_PUBLISHED,
            "1.0",
            "2.3",
            "123.0"
        })
        void acceptsSupportedDatasetVersions(String version) {
            var request = new MultiDatasetExportRequest(
                "oai_ddi",
                List.of(validItem(version))
            );
            
            assertThat(validate(request)).isEmpty();
        }
        
        @Test
        void normalizesNullAndBlankVersionsAndAcceptsThem() {
            var nullVersion = new MultiDatasetExportRequest.ExportItem(
                "doi:10.5072/FK2/ABCDEF",
                null
            );
            var blankVersion = new MultiDatasetExportRequest.ExportItem(
                "doi:10.5072/FK2/ABCDEF",
                "   "
            );
            
            assertThat(nullVersion.version())
                .isEqualTo(ApiConstants.DS_VERSION_LATEST_PUBLISHED);
            assertThat(blankVersion.version())
                .isEqualTo(ApiConstants.DS_VERSION_LATEST_PUBLISHED);
            
            var request = new MultiDatasetExportRequest(
                "oai_ddi",
                List.of(nullVersion, blankVersion)
            );
            
            assertThat(validate(request)).isEmpty();
        }
    }
    
    @Nested
    class InvalidTopLevelProperties {
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void rejectsInvalidExporter(String exporter) {
            var request = new MultiDatasetExportRequest(
                exporter,
                List.of(validItem("1.0"))
            );
            
            assertThat(validate(request)).containsExactly("exporter");
        }
        
        @Test
        void rejectsNullDatasets() {
            var request = new MultiDatasetExportRequest("oai_ddi", null);
            
            assertThat(validate(request)).containsExactly("datasets");
        }
        
        @Test
        void rejectsEmptyDatasets() {
            var request = new MultiDatasetExportRequest("oai_ddi", List.of());
            
            assertThat(validate(request)).containsExactly("datasets");
        }
        
        @Test
        void reportsMultipleTopLevelViolations() {
            var request = new MultiDatasetExportRequest("", List.of());
            
            assertThat(validate(request)).containsExactlyInAnyOrder("exporter", "datasets");
        }
    }
    
    @Nested
    class InvalidDatasetItems {
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   "})
        void rejectsInvalidPersistentId(String persistentId) {
            var request = new MultiDatasetExportRequest(
                "oai_ddi",
                List.of(
                    new MultiDatasetExportRequest.ExportItem(persistentId, "1.0")
                )
            );
            
            assertThat(validate(request)).containsExactly("datasets[0].persistentId");
        }
        
        @ParameterizedTest
        @ValueSource(strings = {":foo", "1", "v1", "1.0.1", "1.", "-"})
        void rejectsUnsupportedVersions(String version) {
            var request = new MultiDatasetExportRequest(
                "oai_ddi",
                List.of(validItem(version))
            );
            
            assertThat(validate(request)).containsExactly("datasets[0].version");
        }
        
        @Test
        void reportsNestedViolationsWithIndexes() {
            var invalidPersistentId = new MultiDatasetExportRequest.ExportItem(null, "1.0");
            var invalidVersion = new MultiDatasetExportRequest.ExportItem(
                "doi:10.5072/FK2/ABCDEF",
                ":nope"
            );
            
            var request = new MultiDatasetExportRequest(
                "oai_ddi",
                List.of(invalidPersistentId, invalidVersion)
            );
            
            assertThat(validate(request)).containsExactlyInAnyOrder(
                "datasets[0].persistentId",
                "datasets[1].version"
            );
        }
        
        @Test
        void reportsMultipleViolationsWithinTheSameNestedItem() {
            var request = new MultiDatasetExportRequest(
                "oai_ddi",
                List.of(
                    new MultiDatasetExportRequest.ExportItem(" ", ":nope")
                )
            );
            
            assertThat(validate(request)).containsExactlyInAnyOrder(
                "datasets[0].persistentId",
                "datasets[0].version"
            );
        }
    }
    
    private static MultiDatasetExportRequest.ExportItem validItem(String version) {
        return new MultiDatasetExportRequest.ExportItem(
            "doi:10.5072/FK2/ABCDEF",
            version
        );
    }
    
    private static <T> Set<String> validate(T target) {
        return validator.validate(target).stream()
            .map(ValidationUtil::propertyPath)
            .collect(Collectors.toSet());
    }
}