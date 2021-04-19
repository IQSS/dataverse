package edu.harvard.iq.dataverse;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseHeaderFragment.Breadcrumb;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(MockitoExtension.class)
public class DataverseHeaderFragmentTest {

    @InjectMocks
    private DataverseHeaderFragment dataverseHeaderFragment;
    @Mock
    private WidgetWrapper widgetWrapper;

    // -------------------- TESTS --------------------

    @Test
    void initBreadcrumbsForFileMetadata() {
        // given
        Dataverse rootDataverse = buildDataverse("root", "root name", null);
        Dataverse dataverse = buildDataverse("dvalias", "DV name", rootDataverse);
        Dataset dataset = buildDataset("ds title", dataverse);
        DataFile datafile = buildDataFile(101L, "filename.txt", dataset);
        // when
        dataverseHeaderFragment.initBreadcrumbsForFileMetadata(datafile.getLatestFileMetadata());
        // then
        List<Breadcrumb> breadcumbs = dataverseHeaderFragment.getBreadcrumbs();

        assertThat(breadcumbs)
            .extracting(Breadcrumb::getUrl, Breadcrumb::getBreadcrumbText)
            .containsExactly(
                tuple("/dataverse/root", "root name"),
                tuple("/dataverse/dvalias", "DV name"),
                tuple("/dataset.xhtml?persistentId=doi:10.1000/ABC&version=DRAFT", "ds title"),
                tuple("/file.xhtml?fileId=101&version=DRAFT", "filename.txt"));
    }

    @Test
    void initBreadcrumbs() {
        // given
        Dataverse rootDataverse = buildDataverse("root", "root name", null);
        Dataset dataset = buildDataset("ds title", rootDataverse);
        DataFile datafile = buildDataFile(101L, "filename.txt", dataset);
        // when
        dataverseHeaderFragment.initBreadcrumbs(datafile);
        // then
        List<Breadcrumb> breadcumbs = dataverseHeaderFragment.getBreadcrumbs();

        assertThat(breadcumbs)
            .extracting(Breadcrumb::getUrl, Breadcrumb::getBreadcrumbText)
            .containsExactly(
                tuple("/dataverse/root", "root name"),
                tuple("/dataset.xhtml?persistentId=doi:10.1000/ABC", "ds title"),
                tuple("/file.xhtml?fileId=101", "filename.txt"));
    }

    // -------------------- PRIVATE --------------------

    private Dataverse buildDataverse(String alias, String name, Dataverse owner) {
        Dataverse dataverse = new Dataverse();
        dataverse.setAlias(alias);
        dataverse.setName(name);
        dataverse.setOwner(owner);
        return dataverse;
    }

    private Dataset buildDataset(String title, Dataverse owner) {
        Dataset dataset = new Dataset();
        dataset.setGlobalId(new GlobalId("doi", "10.1000", "ABC"));
        DatasetFieldType titleFieldType = new DatasetFieldType("title", FieldType.TEXT, false);
        DatasetField titleField = new DatasetField();
        titleField.setDatasetFieldType(titleFieldType);
        titleField.setValue(title);
        dataset.getLatestVersion().setDatasetFields(Lists.newArrayList(titleField));
        dataset.setOwner(owner);
        return dataset;
    }

    private DataFile buildDataFile(Long id, String label, Dataset owner) {
        DataFile datafile = new DataFile();
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel(label);
        fileMetadata.setDatasetVersion(owner.getLatestVersion());
        fileMetadata.setDataFile(datafile);
        datafile.getFileMetadatas().add(fileMetadata);
        datafile.setId(id);
        datafile.setOwner(owner);
        
        return datafile;
    }
}
