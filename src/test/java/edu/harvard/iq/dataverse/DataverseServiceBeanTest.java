package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.pidproviders.VersionPidMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataverseServiceBeanTest {
    
    DataverseServiceBean dataverseServiceBean = new DataverseServiceBean();
    
    private static final Dataverse root = MocksFactory.makeDataverse();
    private static final Dataverse intermediate = MocksFactory.makeDataverse();
    private static final Dataverse child = MocksFactory.makeDataverse();
    
    @BeforeAll
    static void setup() {
        intermediate.setOwner(root);
        child.setOwner(intermediate);
    }
    
    static Stream<Arguments> versionPidCombinationsForAdminMajor() {
        return Stream.of(
            Arguments.of(false, CollectionConduct.SKIP, false),
            Arguments.of(false, CollectionConduct.SKIP, true),
            Arguments.of(true, CollectionConduct.MAJOR, false),
            Arguments.of(false, CollectionConduct.MAJOR, true),
            Arguments.of(true, CollectionConduct.MINOR, false),
            Arguments.of(false, CollectionConduct.MINOR, true)
        );
    }
    
    @ParameterizedTest
    @MethodSource("versionPidCombinationsForAdminMajor")
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "allow-major")
    void versionPidCollectionConductModesWithAdminMajorOnly(boolean expected, CollectionConduct rootConduct, boolean willBeMinor) {
        // given
        root.setDatasetVersionPidConduct(rootConduct);
        
        // when
        assertEquals(expected, dataverseServiceBean.wantsDatasetVersionPids(child, willBeMinor));
    }
    
    static Stream<Arguments> versionPidCombinationsForAdminMinor() {
        return Stream.of(
            Arguments.of(false, CollectionConduct.SKIP, false),
            Arguments.of(false, CollectionConduct.SKIP, true),
            Arguments.of(true, CollectionConduct.MAJOR, false),
            Arguments.of(false, CollectionConduct.MAJOR, true),
            Arguments.of(true, CollectionConduct.MINOR, false),
            Arguments.of(true, CollectionConduct.MINOR, true)
        );
    }
    
    @ParameterizedTest
    @MethodSource("versionPidCombinationsForAdminMinor")
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "allow-minor")
    void versionPidCollectionConductModesWithAdminMinor(boolean expected, CollectionConduct rootConduct, boolean willBeMinor) {
        // given
        root.setDatasetVersionPidConduct(rootConduct);
        
        // when
        assertEquals(expected, dataverseServiceBean.wantsDatasetVersionPids(child, willBeMinor));
    }
    
    @Test
    void versionPidCollectionMayNotBeNull() {
        assertThrows(NullPointerException.class, () -> dataverseServiceBean.wantsDatasetVersionPids(null, false));
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "off")
    void versionPidCollectionAdminDisabled() {
        assertFalse(dataverseServiceBean.wantsDatasetVersionPids(child, false));
        assertFalse(dataverseServiceBean.wantsDatasetVersionPids(child, true));
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "allow-major")
    void versionPidCollectionAdminMajorOnly() {
        assertFalse(dataverseServiceBean.wantsDatasetVersionPids(child, true));
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "allow-major")
    void versionPidNoCollectionConductButAdminMajorOnly() {
        // given
        Dataverse collection = MocksFactory.makeDataverse();
        collection.setDatasetVersionPidConduct(CollectionConduct.INHERIT);
        
        // when & then
        assertTrue(dataverseServiceBean.wantsDatasetVersionPids(collection, false));
        assertFalse(dataverseServiceBean.wantsDatasetVersionPids(collection, true));
    }
    
    @Test
    @JvmSetting(key = JvmSettings.PID_VERSIONS_MODE, value = "allow-minor")
    void versionPidNoCollectionConductButAdminMinor() {
        // given
        Dataverse collection = MocksFactory.makeDataverse();
        collection.setDatasetVersionPidConduct(CollectionConduct.INHERIT);
        
        // when & then
        assertTrue(dataverseServiceBean.wantsDatasetVersionPids(collection, false));
        assertTrue(dataverseServiceBean.wantsDatasetVersionPids(collection, true));
    }
}