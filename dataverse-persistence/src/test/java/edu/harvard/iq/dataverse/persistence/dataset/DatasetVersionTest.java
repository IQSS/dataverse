package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.ConstraintViolation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeFileMetadata;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author michael
 * @author tjanek
 */
public class DatasetVersionTest {

    private DatasetVersion datasetVersion;
    private FileMetadata fileMetadata;

    @BeforeEach
    void setUp() {
        datasetVersion = new DatasetVersion();
        fileMetadata = new FileMetadata();
        fileMetadata.setLabel("foo.png");
    }

    @Test
    public void compareByVersionComparator() {
        //given
        DatasetVersion ds1_0 = new DatasetVersion();
        ds1_0.setId(0L);
        ds1_0.setVersionNumber(1L);
        ds1_0.setMinorVersionNumber(0L);
        ds1_0.setVersionState(DatasetVersion.VersionState.RELEASED);

        DatasetVersion ds1_1 = new DatasetVersion();
        ds1_1.setId(1L);
        ds1_1.setVersionNumber(1L);
        ds1_1.setMinorVersionNumber(1L);
        ds1_1.setVersionState(DatasetVersion.VersionState.RELEASED);

        DatasetVersion ds2_0 = new DatasetVersion();
        ds2_0.setId(2L);
        ds2_0.setVersionNumber(2L);
        ds2_0.setMinorVersionNumber(0L);
        ds2_0.setVersionState(DatasetVersion.VersionState.RELEASED);

        DatasetVersion ds_draft = new DatasetVersion();
        ds_draft.setId(3L);
        ds_draft.setVersionState(DatasetVersion.VersionState.DRAFT);

        List<DatasetVersion> expected = Arrays.asList(ds1_0, ds1_1, ds2_0, ds_draft);
        List<DatasetVersion> actual = Arrays.asList(ds2_0, ds1_0, ds_draft, ds1_1);

        //when
        actual.sort(DatasetVersion.compareByVersion);

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void isInReview() {
        Dataset ds = MocksFactory.makeDataset();

        DatasetVersion draft = ds.getLatestVersion();
        draft.setVersionState(DatasetVersion.VersionState.DRAFT);
        ds.addLock(new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("Lauren", "Ipsumowitch")));
        assertTrue(draft.isInReview());

        DatasetVersion nonDraft = new DatasetVersion();
        nonDraft.setVersionState(DatasetVersion.VersionState.RELEASED);
        assertFalse(nonDraft.isInReview());

        ds.addLock(null);
        assertFalse(nonDraft.isInReview());
    }

    @Test
    public void getOnlyFilesMetadataNotUnderEmbargoSorted() {
        // given
        Dataset ds = MocksFactory.makeDataset();
        ds.setEmbargoDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

        DatasetVersion datasetVersion = ds.getLatestVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        // when
        List<FileMetadata> fileMetadata = datasetVersion.getOnlyFilesMetadataNotUnderEmbargoSorted();

        // then
        assertTrue(fileMetadata.isEmpty());
    }

    @Test
    public void shouldSortFileMetadataByDisplayOrder() {
        // given
        DatasetVersion version = withUnSortedFiles();

        // when
        List<FileMetadata> orderedMetadatas = version.getAllFilesMetadataSorted();

        // then
        verifySortOrder(orderedMetadatas, "file4.png", 0);
        verifySortOrder(orderedMetadatas, "file3.png", 1);
        verifySortOrder(orderedMetadatas, "file5.png", 2);
        verifySortOrder(orderedMetadatas, "file2.png", 3);
        verifySortOrder(orderedMetadatas, "file6.png", 4);
        verifySortOrder(orderedMetadatas, "file1.png", 5);
    }

    @Test
    public void shouldAddNewFileMetadataWithProperDisplayOrder() {
        // given
        DatasetVersion version = withFilesAndCustomDisplayOrder();
        FileMetadata toAdd = makeFileMetadata(40L, "file4.png", 0);

        // when
        version.addFileMetadata(toAdd);

        // then
        verifyDisplayOrder(version.getFileMetadatas(), 0, "file1.png", 1);
        verifyDisplayOrder(version.getFileMetadatas(), 1, "file2.png", 6);
        verifyDisplayOrder(version.getFileMetadatas(), 2, "file3.png", 8);
        verifyDisplayOrder(version.getFileMetadatas(), 3, "file4.png", 9);
    }

    @Test
    public void shouldAddNewFileMetadataOnEmptyMetadatasWithZeroIndex() {
        // given
        DatasetVersion version = new DatasetVersion();
        FileMetadata toAdd = makeFileMetadata(40L, "file1.png", -5); // fake -5 displayOrder

        // when
        version.addFileMetadata(toAdd);

        // then
        verifyDisplayOrder(version.getFileMetadatas(), 0, "file1.png", 0);
    }

	@Test
	public void validate_emptyFileMetadata() {
		datasetVersion.setFileMetadatas(new ArrayList<>());
		Set<ConstraintViolation> violations2 = datasetVersion.validate();
		assertEquals(0, violations2.size());
	}

	@Test
	public void validate_noDirectoryLabel_expectedNoViolation() {
		checkConstraintViolations(null, 0);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/has/leading/slash", "has/trailing/slash/", "/leadingAndTrailing/"})
	public void validate_expectedViolationWithMessage(String directoryLabel) {
		Set<ConstraintViolation> violations = checkConstraintViolations(directoryLabel, 1);
		assertEquals("{directoryname.illegalCharacters}", violations.iterator().next().getMessageTemplate());
	}

	@ParameterizedTest
	@ValueSource(strings = {"just/right", "", "a"})
	public void validate_expectedNoViolation(String directoryLabel) {
		checkConstraintViolations(directoryLabel, 0);
	}

    private void verifySortOrder(List<FileMetadata> metadatas, String label, int expectedOrderIndex) {
        assertEquals(label, metadatas.get(expectedOrderIndex).getLabel());
    }

    private void verifyDisplayOrder(List<FileMetadata> metadatas, int index, String label, int displayOrder) {
        assertEquals(label, metadatas.get(index).getLabel());
        assertEquals(displayOrder, metadatas.get(index).getDisplayOrder());
    }

    private DatasetVersion withUnSortedFiles() {
        DatasetVersion datasetVersion = new DatasetVersion();

        datasetVersion.setFileMetadatas(newArrayList(
                makeFileMetadata(10L, "file2.png", 3),
                makeFileMetadata(20L, "file1.png", 5),
                makeFileMetadata(30L, "file3.png", 1),
                makeFileMetadata(40L, "file4.png", 0),
                makeFileMetadata(50L, "file5.png", 2),
                makeFileMetadata(60L, "file6.png", 4)
        ));

        return datasetVersion;
    }

    private DatasetVersion withFilesAndCustomDisplayOrder() {
        DatasetVersion datasetVersion = new DatasetVersion();

        datasetVersion.setFileMetadatas(newArrayList(
                makeFileMetadata(10L, "file1.png", 1),
                makeFileMetadata(20L, "file2.png", 6),
                makeFileMetadata(30L, "file3.png", 8)
        ));

        return datasetVersion;
    }

    private Set<ConstraintViolation> checkConstraintViolations(String directoryLabel, int expectedViolationsNumber) {
        fileMetadata.setDirectoryLabel(directoryLabel);
        datasetVersion.getFileMetadatas().add(fileMetadata);

        Set<ConstraintViolation> violations = datasetVersion.validate();
        assertEquals(expectedViolationsNumber, violations.size());
        return violations;
    }

}
