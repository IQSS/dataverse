package edu.harvard.iq.dataverse.persistence.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author michael
 */
public class DatasetTest {

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @MethodSource("isDeacessionedArguments")
    public void isDeacessioned(boolean expectedIsDeacessioned, List<VersionState> versionStates) {
        // given
        Dataset dataset = new Dataset();
        dataset.getVersions().clear();
        versionStates.forEach(state -> dataset.getVersions().add(buildVersionWithState(state)));

        // when & then
        assertEquals(expectedIsDeacessioned, dataset.isDeaccessioned());
    }
    
    @ParameterizedTest
    @MethodSource("containsReleasedVersionArguments__positive")
    public void containsReleasedVersion__positive(List<VersionState> versionStates) {
        // given
        Dataset dataset = new Dataset();
        dataset.setPublicationDate(Timestamp.from(Instant.now()));
        dataset.getVersions().clear();
        versionStates.forEach(state -> dataset.getVersions().add(buildVersionWithState(state)));

        // when & then
        assertTrue(dataset.containsReleasedVersion());
    }
    
    @Test
    public void containsReleasedVersion__negative_for_dataset_that_was_deaccessioned() {
        // given
        Dataset dataset = new Dataset();
        dataset.getVersions().clear();
        dataset.getVersions().add(buildVersionWithState(VersionState.DRAFT));
        dataset.getVersions().add(buildVersionWithState(VersionState.DEACCESSIONED));
        dataset.getVersions().add(buildVersionWithState(VersionState.DEACCESSIONED));

        // when & then
        assertFalse(dataset.containsReleasedVersion());
    }
    
    
    @Test
    public void containsReleasedVersion__negative_for_dataset_that_was_never_published() {
        // given
        Dataset dataset = new Dataset();
        dataset.getVersions().clear();
        dataset.getVersions().add(buildVersionWithState(VersionState.DRAFT));

        // when & then
        assertFalse(dataset.containsReleasedVersion());
    }
    
    /**
     * Test of isLockedFor method, of class Dataset.
     */
    @Test
    public void testIsLockedFor() {
        Dataset sut = new Dataset();
        assertFalse(sut.isLockedFor(DatasetLock.Reason.Ingest));
        DatasetLock dl = new DatasetLock(DatasetLock.Reason.Ingest, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        sut.addLock(dl);
        assertTrue(sut.isLockedFor(DatasetLock.Reason.Ingest));
        assertFalse(sut.isLockedFor(DatasetLock.Reason.Workflow));
    }

    @Test
    public void testLocksManagement() {
        Dataset sut = new Dataset();
        assertFalse(sut.isLocked());

        DatasetLock dlIngest = new DatasetLock(DatasetLock.Reason.Ingest, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        dlIngest.setId(MocksFactory.nextId());
        sut.addLock(dlIngest);
        assertTrue(sut.isLocked());

        final DatasetLock dlInReview = new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        dlInReview.setId(MocksFactory.nextId());
        sut.addLock(dlInReview);
        assertEquals(2, sut.getLocks().size());

        DatasetLock retrievedDl = sut.getLockFor(DatasetLock.Reason.Ingest);
        assertEquals(dlIngest, retrievedDl);
        sut.removeLock(dlIngest);
        assertNull(sut.getLockFor(DatasetLock.Reason.Ingest));

        assertTrue(sut.isLocked());

        sut.removeLock(dlInReview);
        assertFalse(sut.isLocked());

    }

    // -------------------- PRIVATE --------------------

    private static Stream<Arguments> isDeacessionedArguments() {
        return Stream.of(
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT)),
                Arguments.of(false, Lists.newArrayList(VersionState.RELEASED)),
                Arguments.of(true, Lists.newArrayList(VersionState.DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT, VersionState.RELEASED, VersionState.RELEASED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT, VersionState.RELEASED, VersionState.DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DRAFT, VersionState.DEACCESSIONED, VersionState.RELEASED)),
                Arguments.of(false, Lists.newArrayList(VersionState.RELEASED, VersionState.RELEASED)),
                Arguments.of(false, Lists.newArrayList(VersionState.RELEASED, VersionState.DEACCESSIONED)),
                Arguments.of(false, Lists.newArrayList(VersionState.DEACCESSIONED, VersionState.RELEASED)),
                Arguments.of(true, Lists.newArrayList(VersionState.DEACCESSIONED, VersionState.DEACCESSIONED))
        );
    }
    
    private static Stream<Arguments> containsReleasedVersionArguments__positive() {
        return Stream.of(
                Arguments.of(Lists.newArrayList(VersionState.RELEASED)),
                Arguments.of(Lists.newArrayList(VersionState.DRAFT, VersionState.RELEASED, VersionState.DEACCESSIONED)),
                Arguments.of(Lists.newArrayList(VersionState.RELEASED, VersionState.RELEASED))
        );
    }
    
    
    private DatasetVersion buildVersionWithState(VersionState state) {
        DatasetVersion version = new DatasetVersion();
        version.setVersionState(state);
        return version;
    }
}
