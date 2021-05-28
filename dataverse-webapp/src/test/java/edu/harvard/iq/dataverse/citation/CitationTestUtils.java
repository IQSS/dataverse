package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class CitationTestUtils {

    // -------------------- LOGIC --------------------

    public DatasetVersion createATestDatasetVersion(String withTitle, boolean withAuthor) throws ParseException {
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");

        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        dataset.setOwner(dataverse);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);

        List<DatasetField> fields = new ArrayList<>();
        if (withTitle != null) {
            fields.add(createTitleField(withTitle));
        }
        if (withAuthor) {
            fields.add(createAuthorField("First Last"));
        }

        if (!fields.isEmpty()) {
            datasetVersion.setDatasetFields(fields);
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");

        datasetVersion.setReleaseTime(publicationDate);

        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));

        return datasetVersion;
    }

    public DatasetVersion createHarvestedTestDatasetVersion(String withTitle, boolean withAuthor) {
        try {
            DatasetVersion datasetVersion = createATestDatasetVersion(withTitle, withAuthor);
            datasetVersion.getDataset().setHarvestedFrom(new HarvestingClient());
            return datasetVersion;
        } catch (ParseException pe) {
            return null;
        }
    }

    public DatasetVersion createHarvestedTestDatasetVersionWithDistributionDate(String withTitle, boolean withAuthor) {
        DatasetVersion datasetVersion = createHarvestedTestDatasetVersion(withTitle, withAuthor);
        datasetVersion.getDataset().setHarvestedFrom(new HarvestingClient());
        List<DatasetField> fields = datasetVersion.getFlatDatasetFields();
        fields.add(createDistributionDateField("2020-01-12"));
        datasetVersion.setDatasetFields(fields);
        return datasetVersion;
    }

    // -------------------- PRIVATE --------------------

    private DatasetField createAuthorField(String value) {
        DatasetField author = new DatasetField();
        author.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.author, FieldType.TEXT, false));
        DatasetField authorName = constructPrimitive(DatasetFieldConstant.authorName, value);
        authorName.setDatasetFieldParent(author);
        author.getDatasetFieldsChildren().add(authorName);

        return author;
    }

    private DatasetField createDistributionDateField(String value) {
        DatasetField distributionDate = new DatasetField();
        distributionDate.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.distributionDate, FieldType.DATE, false));
        distributionDate.setFieldValue(value);
        return distributionDate;
    }

    private DatasetField createTitleField(String value) {
        return constructPrimitive(DatasetFieldConstant.title, value);
    }

    DatasetField constructPrimitive(String fieldName, String value) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(
                new DatasetFieldType(fieldName, FieldType.TEXT, false));
        field.setFieldValue(value);
        return field;
    }
}
