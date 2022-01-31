package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.create;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.makeFileMetadata;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author michael
 * @author tjanek
 */
 class DatasetVersionTest {

    private DatasetVersion datasetVersion;
    private FileMetadata fileMetadata;

    @BeforeEach
    void setUp() {
        datasetVersion = new DatasetVersion();
        fileMetadata = new FileMetadata();
        fileMetadata.setLabel("foo.png");
    }

    // -------------------- TESTS --------------------

    @Test
    void compareByVersionComparator() {
        // given
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

        // when
        actual.sort(DatasetVersion.compareByVersion);

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void isInReview() {
        // given
        Dataset ds = MocksFactory.makeDataset();

        // when
        DatasetVersion draft = ds.getLatestVersion();
        draft.setVersionState(DatasetVersion.VersionState.DRAFT);
        ds.addLock(new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("Lauren", "Ipsumowitch")));

        // then
        assertThat(draft.isInReview()).isTrue();

        // when
        DatasetVersion nonDraft = new DatasetVersion();
        nonDraft.setVersionState(DatasetVersion.VersionState.RELEASED);

        // then
        assertThat(nonDraft.isInReview()).isFalse();

        // when
        ds.addLock(null);

        // then
        assertThat(nonDraft.isInReview()).isFalse();
    }

    @Test
    void shouldSortFileMetadataByDisplayOrder() {
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
    void shouldAddNewFileMetadataWithProperDisplayOrder() {
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
    void shouldAddNewFileMetadataOnEmptyMetadatasWithZeroIndex() {
        // given
        DatasetVersion version = new DatasetVersion();
        FileMetadata toAdd = makeFileMetadata(40L, "file1.png", -5); // fake -5 displayOrder

        // when
        version.addFileMetadata(toAdd);

        // then
        verifyDisplayOrder(version.getFileMetadatas(), 0, "file1.png", 0);
    }

	@Test
	void validate_emptyFileMetadata() {
		datasetVersion.setFileMetadatas(new ArrayList<>());
		Set<ConstraintViolation<FileMetadata>> violations2 = datasetVersion.validateFileMetadata();
		assertThat(violations2).isEmpty();
	}

	@Test
	void validate_noDirectoryLabel_expectedNoViolation() {
		checkConstraintViolations(null, 0);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/has/leading/slash", "has/trailing/slash/", "/leadingAndTrailing/"})
	void validate_expectedViolationWithMessage(String directoryLabel) {
		Set<ConstraintViolation<FileMetadata>> violations = checkConstraintViolations(directoryLabel, 1);
		assertThat(violations.iterator().next().getMessageTemplate()).isEqualTo("{directoryname.illegalCharacters}");
	}

	@ParameterizedTest
	@ValueSource(strings = {"just/right", "", "a"})
	void validate_expectedNoViolation(String directoryLabel) {
		checkConstraintViolations(directoryLabel, 0);
	}


	@Test
    void getRelatedPublications() {
        // given
        DatasetVersion version = new DatasetVersion();
        version.setDatasetFields(Arrays.asList(
                create(DatasetFieldConstant.publication, "",
                        create(DatasetFieldConstant.publicationCitation, "publication-citation"),
                        create(DatasetFieldConstant.publicationIDNumber, "publication-id-number"),
                        create(DatasetFieldConstant.publicationIDType, "publication-id-type"),
                        create(DatasetFieldConstant.publicationURL, "publication-url")),
                create(DatasetFieldConstant.publication, "",
                        create(DatasetFieldConstant.publicationIDNumber, "publication-id-number"),
                        create(DatasetFieldConstant.publicationIDType, null),
                        create(DatasetFieldConstant.publicationURL, "publication-url"))));

        // when
        List<DatasetRelPublication> publications = version.getRelatedPublications();

        // then
        assertThat(publications)
                .extracting(DatasetRelPublication::getText, DatasetRelPublication::getIdNumber, DatasetRelPublication::getIdType,
                        DatasetRelPublication::getUrl)
                .containsExactly(
                        tuple("publication-citation", "publication-id-number", "publication-id-type", "publication-url"),
                        tuple(null, "publication-id-number", null, "publication-url"));
    }

    // -------------------- PRIVATE --------------------

    private void verifySortOrder(List<FileMetadata> metadatas, String label, int expectedOrderIndex) {
        assertThat(metadatas.get(expectedOrderIndex).getLabel()).isEqualTo(label);
    }

    private void verifyDisplayOrder(List<FileMetadata> metadatas, int index, String label, int displayOrder) {
        assertThat(metadatas.get(index).getLabel()).isEqualTo(label);
        assertThat(metadatas.get(index).getDisplayOrder()).isEqualTo(displayOrder);
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

    private Set<ConstraintViolation<FileMetadata>> checkConstraintViolations(String directoryLabel, int expectedViolationsNumber) {
        fileMetadata.setDirectoryLabel(directoryLabel);
        datasetVersion.getFileMetadatas().add(fileMetadata);

        Set<ConstraintViolation<FileMetadata>> violations = datasetVersion.validateFileMetadata();
        assertThat(violations.size()).isEqualTo(expectedViolationsNumber);
        return violations;
    }
}
