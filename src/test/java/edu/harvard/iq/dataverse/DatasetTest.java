package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.mocks.MocksFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author michael
 */
public class DatasetTest {

    /**
     * Test of isLockedFor method, of class Dataset.
     */
    @Test
    public void testIsLockedFor() {
        Dataset sut = new Dataset();
        assertFalse(sut.isLockedFor(DatasetLock.Reason.Ingest), "Initially verify that the dataset is not locked because data being ingested");

        DatasetLock dl = new DatasetLock(DatasetLock.Reason.Ingest, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        sut.addLock(dl);
        assertTrue(sut.isLockedFor(DatasetLock.Reason.Ingest), "Verify that the dataset now has an ingest lock");
        assertFalse(sut.isLockedFor(DatasetLock.Reason.Workflow), "Verify that the dataset does not have a workflow lock");
    }
    
    @Test
    public void testLocksManagement() {
        Dataset sut = new Dataset();
        assertFalse(sut.isLocked(), "Initially verify that the dataset is not locked");
        
        DatasetLock dlIngest = new DatasetLock(DatasetLock.Reason.Ingest, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        dlIngest.setId(MocksFactory.nextId());
        sut.addLock(dlIngest);
        assertTrue(sut.isLocked(), "After adding an ingest lock, verify that the dataset is locked");

        final DatasetLock dlInReview = new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("jane", "doe"));
        dlInReview.setId(MocksFactory.nextId());
        sut.addLock(dlInReview);
        assertEquals(2, sut.getLocks().size(), "After adding a review lock, verify that the dataset is locked by two locks");
        
        DatasetLock retrievedDl = sut.getLockFor(DatasetLock.Reason.Ingest);
        assertEquals( dlIngest, retrievedDl );
        sut.removeLock(dlIngest);
        assertNull(sut.getLockFor(DatasetLock.Reason.Ingest), "After removing the ingest lock, verify that the dataset does not have any ingest locks");
        
        assertTrue(sut.isLocked(), "After removing the ingest lock, verify that the dataset is still locked (review lock)");
        
        sut.removeLock(dlInReview);
        assertFalse(sut.isLocked(), "After removing the review lock, verify that the dataset is not locked anymore");
        
    }

    // ****************************************************************************************
    // The following tests test the isDeaccessioned method aiming for 100% prime path coverage.
    // For the test cases below, different dataset versions and different combinations of those
    // versions are used to execute different paths of the method under test.
    //
    // 11 test paths (= 11 test methods) are needed.
    // 4 different dataset versions are needed:
    // - DEACCESSIONED
    // - DRAFT
    // - RELEASED
    // - non of the above (= ARCHIVED)
    //
    // See details here: https://github.com/IQSS/dataverse/pull/5703
    // ****************************************************************************************

    private DatasetVersion archivedVersion;
    private DatasetVersion deaccessionedVersion;
    private DatasetVersion draftVersion;
    private DatasetVersion releasedVersion;

    @BeforeEach
    public void before() {
        this.archivedVersion = new DatasetVersion();
        this.archivedVersion.setVersionState(VersionState.ARCHIVED);

        this.deaccessionedVersion = new DatasetVersion();
        this.deaccessionedVersion.setVersionState(VersionState.DEACCESSIONED);

        this.draftVersion = new DatasetVersion();
        this.draftVersion.setVersionState(VersionState.DRAFT);

        this.releasedVersion = new DatasetVersion();
        this.releasedVersion.setVersionState(VersionState.RELEASED);
    }

    @AfterEach
    public void after() {
        this.archivedVersion = null;
        this.deaccessionedVersion = null;
        this.draftVersion = null;
        this.releasedVersion = null;
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithNoVersions() {
        // path [1,2,3,4]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithOneReleasedVersion() {
        // path [1,2,3,5,6]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.releasedVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithOneDraftVersion() {
        // path [1,2,3,5,7,8]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.draftVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithArchivedAndDraftVersions() {
        // path [1,2,3,5,7,9,11,3,5,7,8]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.archivedVersion);
        versionList.add(this.draftVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithDeaccessionedAndReleasedVersions() {
        // path [1,2,3,5,7,9,10,11,3,5,6]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.deaccessionedVersion);
        versionList.add(this.releasedVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithArchivedAndReleasedVersions() {
        // path [1,2,3,5,7,9,11,3,5,6]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.archivedVersion);
        versionList.add(this.releasedVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithDeaccessionedAndDraftVersions() {
        // path [1,2,3,5,7,9,10,11,3,5,7,8]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.deaccessionedVersion);
        versionList.add(this.draftVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldNotBeDeaccessionedWithArchivedAndArchivedVersions() {
        // path [1,2,3,5,7,9,11,3,5,7,9,11,3,4]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.archivedVersion);
        versionList.add(this.archivedVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertFalse(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldBeDeaccessionedWithArchivedAndDeaccessedVersions() {
        // path [1,2,3,5,7,9,11,3,5,7,9,10,11,3,4]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.archivedVersion);
        versionList.add(this.deaccessionedVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertTrue(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldBeDeaccessionedWithDeaccessionedAndArchivedVersions() {
        // path [1,2,3,5,7,9,10,11,3,5,7,9,11,3,4]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.deaccessionedVersion);
        versionList.add(this.archivedVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertTrue(dataset.isDeaccessioned());
    }

    @Test
    public void datasetShouldBeDeaccessionedWithDeaccessionedAndDeaccessionedVersions() {
        // path [1,2,3,5,7,9,10,11,3,5,7,9,10,11,3,4]
        List<DatasetVersion> versionList = new ArrayList<DatasetVersion>();
        versionList.add(this.deaccessionedVersion);
        versionList.add(this.deaccessionedVersion);

        Dataset dataset = new Dataset();
        dataset.setVersions(versionList);

        assertTrue(dataset.isDeaccessioned());
    }
 
}
