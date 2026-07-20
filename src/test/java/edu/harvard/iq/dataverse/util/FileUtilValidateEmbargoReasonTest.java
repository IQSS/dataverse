package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@LocalJvmSettings
public class FileUtilValidateEmbargoReasonTest {

    @Mock
    private FacesContext facesContext;

    @Mock
    private UIComponent component;

    @Mock
    private ExternalContext externalContext;

    private Map<String, String> requestParameterMap;
    private AutoCloseable mocks;

    @BeforeEach
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        requestParameterMap = new HashMap<>();
        
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getRequestParameterMap()).thenReturn(requestParameterMap);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void validateEmbargoReason_shouldSkipValidation_whenRemovingEmbargo() {
        // Arrange
        boolean removeEmbargo = true;
        requestParameterMap.put("jakarta.faces.source", "fileEmbargoPopupSaveButton");

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> 
            FileUtil.validateEmbargoReason(facesContext, component, null, removeEmbargo)
        );
    }

    @Test
    public void validateEmbargoReason_shouldSkipValidation_whenSourceIsNull() {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", null);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> 
            FileUtil.validateEmbargoReason(facesContext, component, null, removeEmbargo)
        );
    }

    @Test
    public void validateEmbargoReason_shouldSkipValidation_whenSourceIsNotSaveButton() {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", "someOtherButton");

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> 
            FileUtil.validateEmbargoReason(facesContext, component, null, removeEmbargo)
        );
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "require-embargo-reason")
    public void validateEmbargoReason_shouldThrowException_whenReasonIsNullAndFlagEnabled() {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", "fileEmbargoPopupSaveButton");
        
        // Act & Assert
        ValidatorException exception = assertThrows(ValidatorException.class, () ->
            FileUtil.validateEmbargoReason(facesContext, component, null, removeEmbargo)
        );
        
        assertEquals(FacesMessage.SEVERITY_ERROR, exception.getFacesMessage().getSeverity());
        assertEquals(BundleUtil.getStringFromBundle("embargo.reason.required"), 
            exception.getFacesMessage().getSummary());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "require-embargo-reason")
    public void validateEmbargoReason_shouldNotThrowException_whenReasonIsNullAndFlagDisabled() {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", "fileEmbargoPopupSaveButton");
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> 
            FileUtil.validateEmbargoReason(facesContext, component, null, removeEmbargo)
        );
    }

    @ParameterizedTest
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "require-embargo-reason")
    @ValueSource(strings = {"", "   ", "\t", "\n", "  \t\n  "})
    public void validateEmbargoReason_shouldThrowException_whenReasonIsBlank(String blankReason) {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", "fileEmbargoPopupSaveButton");

        // Act & Assert
        ValidatorException exception = assertThrows(ValidatorException.class, () ->
            FileUtil.validateEmbargoReason(facesContext, component, blankReason, removeEmbargo)
        );

        assertEquals(FacesMessage.SEVERITY_ERROR, exception.getFacesMessage().getSeverity());
        assertEquals(BundleUtil.getStringFromBundle("embargo.reason.blank"), 
            exception.getFacesMessage().getSummary());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "require-embargo-reason")
    public void validateEmbargoReason_shouldHandleComplexScenario_flagEnabledBlankReasonSaveButton() {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", "fileEmbargoPopupSaveButton");
        
        // Act & Assert - blank reason should still fail even when flag is disabled
        ValidatorException exception = assertThrows(ValidatorException.class, () ->
            FileUtil.validateEmbargoReason(facesContext, component, "  ", removeEmbargo)
        );
        
        assertEquals(BundleUtil.getStringFromBundle("embargo.reason.blank"), 
            exception.getFacesMessage().getSummary());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Valid embargo reason",
        "  Valid reason with spaces  "
    })
    public void validateEmbargoReason_shouldNotThrowException_whenReasonIsValid(String validReason) {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", "fileEmbargoPopupSaveButton");

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> 
            FileUtil.validateEmbargoReason(facesContext, component, validReason, removeEmbargo)
        );
    }

    @ParameterizedTest
    @MethodSource("provideSaveButtonVariations")
    public void validateEmbargoReason_shouldValidate_whenSaveButtonTriggersRequest(String buttonId) {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", buttonId);

        // Act & Assert
        ValidatorException exception = assertThrows(ValidatorException.class, () ->
            FileUtil.validateEmbargoReason(facesContext, component, "", removeEmbargo)
        );

        assertEquals(FacesMessage.SEVERITY_ERROR, exception.getFacesMessage().getSeverity());
    }

    static Stream<Arguments> provideSaveButtonVariations() {
        return Stream.of(
            Arguments.of("fileEmbargoPopupSaveButton"),
            // button in any context
            Arguments.of("form:fileEmbargoPopupSaveButton"),
            // or any suffix
            Arguments.of("fileEmbargoPopupSaveButton:anything")
        );
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "require-embargo-reason")
    public void validateEmbargoReason_shouldHandleComplexScenario_flagEnabledNullReasonSaveButton() {
        // Arrange
        boolean removeEmbargo = false;
        requestParameterMap.put("jakarta.faces.source", "form:fileEmbargoPopupSaveButton");
        
        // Act & Assert
        ValidatorException exception = assertThrows(ValidatorException.class, () ->
            FileUtil.validateEmbargoReason(facesContext, component, null, removeEmbargo)
        );
        
        assertEquals(BundleUtil.getStringFromBundle("embargo.reason.required"), 
            exception.getFacesMessage().getSummary());
    }

}