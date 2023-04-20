package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AbstractGlobalIdServiceBeanTest {
    
    private static final int idLen = 8;
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_STYLE, value = "dataset")
    void generateDatasetVersionIdentifierWithStyleDataset() {
        // given
        TestIdService sut = new TestIdService();
        Dataset dataset = MocksFactory.makeDataset();
        String datasetIdentifier = dataset.getIdentifier();
    
        // We assume that this code will be called with a dataset version that is about to be published
        // and thus is not in status released. The random generated dataset is assumed to have the version number
        // "1", so we can compare things properly.
        assumeFalse(dataset.getLatestVersion().isReleased());
        assumeTrue(dataset.getLatestVersion().getVersionNumber() == 1);
        assumeTrue(dataset.getLatestVersion().getMinorVersionNumber() == 0);
        
        // when
        String versionIdentifier = sut.generateDatasetVersionIdentifier(dataset.getLatestVersion());
        
        // then
        assertNotEquals(datasetIdentifier, versionIdentifier);
        assertFalse(versionIdentifier.contains("."));
        assertEquals(idLen, versionIdentifier.length());
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_STYLE, value = "suffix")
    void generateDatasetVersionIdentifierWithStyleSuffix() {
        // given
        TestIdService sut = new TestIdService();
        Dataset dataset = MocksFactory.makeDataset();
        String datasetIdentifier = dataset.getIdentifier();
        
        // We assume that this code will be called with a dataset version that is about to be published
        // and thus is not in status released. The random generated dataset is assumed to have the version number
        // "1", so we can compare things properly.
        assumeFalse(dataset.getLatestVersion().isReleased());
        assumeTrue(dataset.getLatestVersion().getVersionNumber() == 1);
        assumeTrue(dataset.getLatestVersion().getMinorVersionNumber() == 0);
        
        // when
        String versionIdentifier = sut.generateDatasetVersionIdentifier(dataset.getLatestVersion());
        
        // then
        assertNotEquals(datasetIdentifier, versionIdentifier);
        assertTrue(versionIdentifier.contains("."));
        assertEquals(versionIdentifier, datasetIdentifier + ".1");
    }
    
    @Test
    void generateDatasetVersionIdentifierFailsWithMinorVersion() {
        // given
        TestIdService sut = new TestIdService();
        Dataset dataset = MocksFactory.makeDataset();
    
        assumeFalse(dataset.getLatestVersion().isReleased());
    
        // Set to non-allowed combination
        dataset.getLatestVersion().setMinorVersionNumber(1L);
        
        // when & then (split retrieval to avoid ambiguous lambda exceptions)
        DatasetVersion version = dataset.getLatestVersion();
        assertThrows(IllegalArgumentException.class, () -> sut.generateDatasetVersionIdentifier(version));
    }
    
    @Test
    void generateDatasetVersionIdentifierFailsWithReleasedVersion() {
        // given
        TestIdService sut = new TestIdService();
        Dataset dataset = MocksFactory.makeDataset();
        String datasetIdentifier = dataset.getIdentifier();
        
        assumeTrue(dataset.getLatestVersion().getMinorVersionNumber() == 0);
        
        // Set to non-allowed combination
        dataset.getLatestVersion().setVersionState(DatasetVersion.VersionState.RELEASED);
        
        // when & then (split retrieval to avoid ambiguous lambda exceptions)
        DatasetVersion version = dataset.getLatestVersion();
        assertThrows(IllegalArgumentException.class, () -> sut.generateDatasetVersionIdentifier(version));
    }
    
    @ParameterizedTest
    @JvmSetting(key = JvmSettings.PID_VERSIONS_STYLE, value = "suffix")
    @NullAndEmptySource
    void generateDatasetVersionIdentifierFailsWithSuffixStyleAndEmptyDatasetIdentifier(String pid) {
        // given
        TestIdService sut = new TestIdService();
        Dataset dataset = MocksFactory.makeDataset();
        
        assumeTrue(dataset.getLatestVersion().getMinorVersionNumber() == 0);
        
        // Set to non-allowed combination
        dataset.setIdentifier(pid);
        
        // when & then (split retrieval to avoid ambiguous lambda exceptions)
        DatasetVersion version = dataset.getLatestVersion();
        assertThrows(IllegalArgumentException.class, () -> sut.generateDatasetVersionIdentifier(version));
    }
    
    
    static class TestIdService extends AbstractGlobalIdServiceBean {
    
        @Override
        public boolean alreadyExists(GlobalId globalId) throws Exception {
            return false;
        }
    
        @Override
        public boolean registerWhenPublished() {
            return false;
        }
    
        @Override
        public List<String> getProviderInformation() {
            return null;
        }
    
        @Override
        public String createIdentifier(DvObject dvo) throws Throwable {
            return RandomStringUtils.randomAlphanumeric(idLen).toUpperCase();
        }
    
        @Override
        public String generateDatasetIdentifier(Dataset dataset) {
            return RandomStringUtils.randomAlphanumeric(idLen).toUpperCase();
        }
    
        @Override
        public Map<String, String> getIdentifierMetadata(DvObject dvo) {
            return null;
        }
    
        @Override
        public String modifyIdentifierTargetURL(DvObject dvo) throws Exception {
            return null;
        }
    
        @Override
        public void deleteIdentifier(DvObject dvo) throws Exception {
        
        }
    
        @Override
        public boolean publicizeIdentifier(DvObject studyIn) {
            return false;
        }
    
        @Override
        public String getUrlPrefix() {
            return null;
        }
    }
    
}