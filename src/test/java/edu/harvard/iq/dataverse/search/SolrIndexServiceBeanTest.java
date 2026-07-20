
package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SolrIndexServiceBeanTest {

    @InjectMocks
    private SolrIndexServiceBean solrIndexServiceBean;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDatasetVersionsToBuildCardsFor_OnlyDraft() {
        // Arrange
        Dataset dataset = mock(Dataset.class);
        DatasetVersion draftVersion = createMockVersion(1L, DatasetVersion.VersionState.DRAFT, true);
        
        when(dataset.getLatestVersion()).thenReturn(draftVersion);
        when(dataset.getReleasedVersion()).thenReturn(null);
        
        // Act
        Set<DatasetVersion> result = invokeDatasetVersionsToBuildCardsFor(dataset);
        
        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(draftVersion));
    }

    @Test
    public void testDatasetVersionsToBuildCardsFor_OnlyDeaccessioned() {
        // Arrange
        Dataset dataset = mock(Dataset.class);
        DatasetVersion deaccessionedVersion = createMockVersion(1L, DatasetVersion.VersionState.DEACCESSIONED, false);
        
        when(dataset.getLatestVersion()).thenReturn(deaccessionedVersion);
        when(dataset.getReleasedVersion()).thenReturn(null);
        
        // Act
        Set<DatasetVersion> result = invokeDatasetVersionsToBuildCardsFor(dataset);
        
        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(deaccessionedVersion));
    }

    @Test
    public void testDatasetVersionsToBuildCardsFor_OnlyReleased() {
        // Arrange
        Dataset dataset = mock(Dataset.class);
        DatasetVersion releasedVersion = createMockVersion(1L, DatasetVersion.VersionState.RELEASED, false);
        
        when(dataset.getLatestVersion()).thenReturn(releasedVersion);
        when(dataset.getReleasedVersion()).thenReturn(releasedVersion);
        
        // Act
        Set<DatasetVersion> result = invokeDatasetVersionsToBuildCardsFor(dataset);
        
        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(releasedVersion));
    }

    @Test
    public void testDatasetVersionsToBuildCardsFor_ReleasedAndDraft() {
        // Arrange
        Dataset dataset = mock(Dataset.class);
        DatasetVersion releasedVersion = createMockVersion(1L, DatasetVersion.VersionState.RELEASED, false);
        DatasetVersion draftVersion = createMockVersion(2L, DatasetVersion.VersionState.DRAFT, true);
        
        when(dataset.getLatestVersion()).thenReturn(draftVersion);
        when(dataset.getReleasedVersion()).thenReturn(releasedVersion);
        
        // Act
        Set<DatasetVersion> result = invokeDatasetVersionsToBuildCardsFor(dataset);
        
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(releasedVersion));
        assertTrue(result.contains(draftVersion));
    }

    @Test
    public void testDatasetVersionsToBuildCardsFor_ReleasedAndDeaccessioned() {
        // Arrange
        Dataset dataset = mock(Dataset.class);
        DatasetVersion releasedVersion = createMockVersion(1L, DatasetVersion.VersionState.RELEASED, false);
        DatasetVersion deaccessionedVersion = createMockVersion(2L, DatasetVersion.VersionState.DEACCESSIONED, false);
        
        // Latest is deaccessioned, but there's a released version
        when(dataset.getLatestVersion()).thenReturn(deaccessionedVersion);
        when(dataset.getReleasedVersion()).thenReturn(releasedVersion);
        
        // Act
        Set<DatasetVersion> result = invokeDatasetVersionsToBuildCardsFor(dataset);
        
        // Assert
        // Should only return the released version, not the deaccessioned one
        assertEquals(1, result.size());
        assertTrue(result.contains(releasedVersion));
        assertFalse(result.contains(deaccessionedVersion));
    }

    // Helper method to create mock DatasetVersion
    private DatasetVersion createMockVersion(Long id, DatasetVersion.VersionState state, boolean isDraft) {
        DatasetVersion version = mock(DatasetVersion.class);
        when(version.getId()).thenReturn(id);
        when(version.getVersionState()).thenReturn(state);
        when(version.isDraft()).thenReturn(isDraft);
        when(version.isReleased()).thenReturn(state == DatasetVersion.VersionState.RELEASED);
        when(version.isDeaccessioned()).thenReturn(state == DatasetVersion.VersionState.DEACCESSIONED);
        return version;
    }

    // Helper method to invoke the private method using reflection
    @SuppressWarnings("unchecked")
    private Set<DatasetVersion> invokeDatasetVersionsToBuildCardsFor(Dataset dataset) {
        try {
            java.lang.reflect.Method method = SolrIndexServiceBean.class.getDeclaredMethod(
                "datasetVersionsToBuildCardsFor", Dataset.class);
            method.setAccessible(true);
            return (Set<DatasetVersion>) method.invoke(solrIndexServiceBean, dataset);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method", e);
        }
    }
}