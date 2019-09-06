package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class UserDataFieldFillerTest {

    @InjectMocks
    private UserDataFieldFiller fieldFiller;
    
    @Mock
    private DatasetFieldServiceBean fieldService;
    
    private AuthenticatedUser user;
    
    @BeforeEach
    public void beforeEach() {
        user = new AuthenticatedUser();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAffiliation("Aff");
        user.setEmail("example@domain.com");
        user.setAuthenticatedUserLookup(new AuthenticatedUserLookup("some_id", "provider"));
                
    }
    
    // -------------------- TESTS --------------------
    
    @Test
    public void fillUserDataInDatasetFields__FILL_DEPOSITOR() {
        // given
        DatasetFieldType depositorType = new DatasetFieldType(DatasetFieldConstant.depositor, FieldType.TEXT, false);
        DatasetField depositorField = DatasetField.createNewEmptyDatasetField(depositorType, new DatasetVersion());
        
        List<DatasetField> datasetFields = Lists.newArrayList(depositorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("Doe, John", datasetFields.get(0).getRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__DO_NOT_OVERRIDE_DEPOSITOR() {
        // given
        DatasetFieldType depositorType = new DatasetFieldType(DatasetFieldConstant.depositor, FieldType.TEXT, false);
        DatasetField depositorField = DatasetField.createNewEmptyDatasetField(depositorType, new DatasetVersion());
        depositorField.setSingleValue("Old depositor");
        
        List<DatasetField> datasetFields = Lists.newArrayList(depositorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("Old depositor", datasetFields.get(0).getRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__FILL_DATE_OF_DEPOSIT() {
        // given
        fieldFiller.setClock(Clock.fixed(Instant.ofEpochSecond(1513767172), ZoneId.of("UTC"))); //  20 December 2017 10:52:52
        
        DatasetFieldType depositorType = new DatasetFieldType(DatasetFieldConstant.dateOfDeposit, FieldType.TEXT, false);
        DatasetField depositorField = DatasetField.createNewEmptyDatasetField(depositorType, new DatasetVersion());
        
        List<DatasetField> datasetFields = Lists.newArrayList(depositorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("2017-12-20", datasetFields.get(0).getRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__DO_NOT_OVERRIDE_DATE_OF_DEPOSIT() {
        // given
        
        DatasetFieldType depositorType = new DatasetFieldType(DatasetFieldConstant.dateOfDeposit, FieldType.TEXT, false);
        DatasetField depositorField = DatasetField.createNewEmptyDatasetField(depositorType, new DatasetVersion());
        depositorField.setSingleValue("Old date");
        
        List<DatasetField> datasetFields = Lists.newArrayList(depositorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("Old date", datasetFields.get(0).getRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__FILL_DATASET_CONTACT() {
        // given
        DatasetFieldType datasetContact = new DatasetFieldType(DatasetFieldConstant.datasetContact, FieldType.TEXT, false);
        
        datasetContact.getChildDatasetFieldTypes().addAll(Lists.newArrayList(
                new DatasetFieldType(DatasetFieldConstant.datasetContactName, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.datasetContactAffiliation, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.datasetContactEmail, FieldType.TEXT, false)
                ));
        
        DatasetField authorField = DatasetField.createNewEmptyChildDatasetField(datasetContact, new DatasetFieldCompoundValue());
        
        List<DatasetField> datasetFields = Lists.newArrayList(authorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("Doe, John; Aff; example@domain.com", datasetFields.get(0).getCompoundRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__DO_NOT_OVERRIDE_DATASET_CONTACT() {
        // given
        DatasetFieldType datasetContact = new DatasetFieldType(DatasetFieldConstant.datasetContact, FieldType.TEXT, false);
        
        datasetContact.getChildDatasetFieldTypes().addAll(Lists.newArrayList(
                new DatasetFieldType(DatasetFieldConstant.datasetContactName, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.datasetContactAffiliation, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.datasetContactEmail, FieldType.TEXT, false)
                ));
        
        DatasetField contactField = DatasetField.createNewEmptyChildDatasetField(datasetContact, new DatasetFieldCompoundValue());
        contactField.getDatasetFieldCompoundValues().get(0).getChildDatasetFields().get(0).setSingleValue("ContactName");
        
        List<DatasetField> datasetFields = Lists.newArrayList(contactField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("ContactName; ; ", datasetFields.get(0).getCompoundRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__FILL_AUTHOR() {
        // given
        DatasetFieldType authorType = new DatasetFieldType(DatasetFieldConstant.author, FieldType.TEXT, false);
        authorType.getChildDatasetFieldTypes().addAll(Lists.newArrayList(
                new DatasetFieldType(DatasetFieldConstant.authorName, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorAffiliation, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorIdValue, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorIdType, FieldType.TEXT, false)
                ));
        
        DatasetField authorField = DatasetField.createNewEmptyChildDatasetField(authorType, new DatasetFieldCompoundValue());
        
        List<DatasetField> datasetFields = Lists.newArrayList(authorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("Doe, John; Aff; ; ", datasetFields.get(0).getCompoundRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__FILL_AUTHOR_WITH_ORCID() {
        // given
        user.setAuthenticatedUserLookup(new AuthenticatedUserLookup("orcid_id", "orcid"));
        
        DatasetFieldType authorIdTypeDatasetFieldType = new DatasetFieldType(DatasetFieldConstant.authorIdType, FieldType.TEXT, false);
        Mockito.when(fieldService.findByName(DatasetFieldConstant.authorIdType)).thenReturn(authorIdTypeDatasetFieldType);
        
        ControlledVocabularyValue vocabValue = Mockito.mock(ControlledVocabularyValue.class);
        Mockito.when(vocabValue.getStrValue()).thenReturn("ORCID");
        Mockito.when(fieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(authorIdTypeDatasetFieldType, "ORCID", true))
            .thenReturn(vocabValue);
        
        DatasetFieldType authorType = new DatasetFieldType(DatasetFieldConstant.author, FieldType.TEXT, false);
        
        DatasetFieldType authorIdType = new DatasetFieldType(DatasetFieldConstant.authorIdType, FieldType.TEXT, false);
        authorIdType.setControlledVocabularyValues(Lists.newArrayList(vocabValue));
        
        authorType.getChildDatasetFieldTypes().addAll(Lists.newArrayList(
                new DatasetFieldType(DatasetFieldConstant.authorName, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorAffiliation, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorIdValue, FieldType.TEXT, false),
                authorIdType
                ));
        
        DatasetField authorField = DatasetField.createNewEmptyChildDatasetField(authorType, new DatasetFieldCompoundValue());
        
        List<DatasetField> datasetFields = Lists.newArrayList(authorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("Doe, John; Aff; orcid_id; ORCID", datasetFields.get(0).getCompoundRawValue());
    }
    
    @Test
    public void fillUserDataInDatasetFields__DO_NOT_OVERRIDE_AUTHOR() {
        // given
        DatasetFieldType authorType = new DatasetFieldType(DatasetFieldConstant.author, FieldType.TEXT, false);
        authorType.getChildDatasetFieldTypes().addAll(Lists.newArrayList(
                new DatasetFieldType(DatasetFieldConstant.authorName, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorAffiliation, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorIdValue, FieldType.TEXT, false),
                new DatasetFieldType(DatasetFieldConstant.authorIdType, FieldType.TEXT, false)
                ));
        
        DatasetField authorField = DatasetField.createNewEmptyChildDatasetField(authorType, new DatasetFieldCompoundValue());
        authorField.getDatasetFieldCompoundValues().get(0).getChildDatasetFields().get(0).setSingleValue("AuthorName");
        
        List<DatasetField> datasetFields = Lists.newArrayList(authorField);
        
        // when
        fieldFiller.fillUserDataInDatasetFields(datasetFields, user);
        
        // then
        assertEquals(1, datasetFields.size());
        assertEquals("AuthorName; ; ; ", datasetFields.get(0).getCompoundRawValue());
    }
    
    
}
