package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Year;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DDIExporterTest {

    MockDatasetFieldSvc datasetFieldTypeSvc = null;

    @Before
    public void setUp() {
        datasetFieldTypeSvc = new MockDatasetFieldSvc();

        DatasetFieldType titleType = datasetFieldTypeSvc.add(new DatasetFieldType("title", FieldType.TEXTBOX, false));
        DatasetFieldType authorType = datasetFieldTypeSvc.add(new DatasetFieldType("author", FieldType.TEXT, true));
        Set<DatasetFieldType> authorChildTypes = new HashSet<>();
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorName", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorAffiliation", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifier", FieldType.TEXT, false)));
        DatasetFieldType authorIdentifierSchemeType = datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifierScheme", FieldType.TEXT, false));
        authorIdentifierSchemeType.setAllowControlledVocabulary(true);
        authorIdentifierSchemeType.setControlledVocabularyValues(Arrays.asList(
                // Why aren't these enforced? Should be ORCID, etc.
                new ControlledVocabularyValue(1l, "ark", authorIdentifierSchemeType),
                new ControlledVocabularyValue(2l, "doi", authorIdentifierSchemeType),
                new ControlledVocabularyValue(3l, "url", authorIdentifierSchemeType)
        ));
        authorChildTypes.add(datasetFieldTypeSvc.add(authorIdentifierSchemeType));
        for (DatasetFieldType t : authorChildTypes) {
            t.setParentDatasetFieldType(authorType);
        }
        authorType.setChildDatasetFieldTypes(authorChildTypes);

        DatasetFieldType datasetContactType = datasetFieldTypeSvc.add(new DatasetFieldType("datasetContact", FieldType.TEXT, true));
        Set<DatasetFieldType> datasetContactTypes = new HashSet<>();
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactEmail", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactName", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false)));
        for (DatasetFieldType t : datasetContactTypes) {
            t.setParentDatasetFieldType(datasetContactType);
        }
        datasetContactType.setChildDatasetFieldTypes(datasetContactTypes);

        DatasetFieldType dsDescriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("dsDescription", FieldType.TEXT, true));
        Set<DatasetFieldType> dsDescriptionTypes = new HashSet<>();
        dsDescriptionTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("dsDescriptionValue", FieldType.TEXT, false)));
        for (DatasetFieldType t : dsDescriptionTypes) {
            t.setParentDatasetFieldType(dsDescriptionType);
        }
        dsDescriptionType.setChildDatasetFieldTypes(dsDescriptionTypes);

        DatasetFieldType keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", FieldType.TEXT, true));
        DatasetFieldType descriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("description", FieldType.TEXTBOX, false));

        DatasetFieldType subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));

        DatasetFieldType pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));

        DatasetFieldType compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lat", FieldType.TEXT, false)));
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lon", FieldType.TEXT, false)));

        for (DatasetFieldType t : childTypes) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
    }

    @Test
    public void testCitation() throws Exception {
        System.out.println("testCitation");

        byte[] file = Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource("json/dataset-finch1.json").toURI()));
        String datasetVersionAsJson = new String(file);
        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));

        JsonObject json = jsonReader.readObject();
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        DatasetVersion version = jsonParser.parseDatasetVersion(json.getJsonObject("datasetVersion"));
        version.setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset dataset = new Dataset();
        version.setDataset(dataset);
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);
        String citation = version.getCitation();
        System.out.println("citation: " + citation);
        int currentYear = Year.now().getValue();
        assertEquals("Finch, Fiona, " + currentYear + ", \"Darwin's Finches\", DRAFT VERSION", citation);
    }

    static class MockDatasetFieldSvc extends DatasetFieldServiceBean {

        Map<String, DatasetFieldType> fieldTypes = new HashMap<>();
        long nextId = 1;

        public DatasetFieldType add(DatasetFieldType t) {
            if (t.getId() == null) {
                t.setId(nextId++);
            }
            fieldTypes.put(t.getName(), t);
            return t;
        }

        @Override
        public DatasetFieldType findByName(String name) {
            return fieldTypes.get(name);
        }

        @Override
        public DatasetFieldType findByNameOpt(String name) {
            return findByName(name);
        }

        @Override
        public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(DatasetFieldType dsft, String strValue, boolean lenient) {
            ControlledVocabularyValue cvv = new ControlledVocabularyValue();
            cvv.setDatasetFieldType(dsft);
            cvv.setStrValue(strValue);
            return cvv;
        }

    }

}
