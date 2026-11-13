package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.CustomQuestion;
import edu.harvard.iq.dataverse.CustomQuestionValue;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ManagedFieldsValidatorTest {
    
    @Test
    public void testValidateManagedFields_Guestbook_NestedIds() throws JsonParseException {
        Guestbook existing = new Guestbook();
        existing.setId(1L);
        CustomQuestion cq = new CustomQuestion();
        cq.setId(2L);
        CustomQuestionValue cqv = new CustomQuestionValue();
        cqv.setId(3L);
        List<CustomQuestionValue> values = new ArrayList<>();
        values.add(cqv);
        cq.setCustomQuestionValues(values);
        List<CustomQuestion> questions = new ArrayList<>();
        questions.add(cq);
        existing.setCustomQuestions(questions);

        Guestbook incoming = new Guestbook();
        incoming.setId(1L);
        CustomQuestion cqIn = new CustomQuestion();
        cqIn.setId(2L);
        CustomQuestionValue cqvIn = new CustomQuestionValue();
        cqvIn.setId(3L);
        List<CustomQuestionValue> valuesIn = new ArrayList<>();
        valuesIn.add(cqvIn);
        cqIn.setCustomQuestionValues(valuesIn);
        List<CustomQuestion> questionsIn = new ArrayList<>();
        questionsIn.add(cqIn);
        incoming.setCustomQuestions(questionsIn);

        // Pass with matching
        ManagedFieldsValidator.validateManagedFields(incoming, existing);

        // Fail with mismatched question ID
        cqIn.setId(99L);
        assertThrows(JsonParseException.class, () -> ManagedFieldsValidator.validateManagedFields(incoming, existing));

        // Fail with mismatched value ID
        cqIn.setId(2L);
        cqvIn.setId(99L);
        assertThrows(JsonParseException.class, () -> ManagedFieldsValidator.validateManagedFields(incoming, existing));
        
        // Pass with new question (null ID)
        cqIn.setId(null);
        cqvIn.setId(null);
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
    }

    @Test
    public void testValidateManagedFields_DatasetVersion() throws JsonParseException {
        DatasetVersion existing = new DatasetVersion();
        existing.setId(1L);
        
        DatasetVersion incoming = new DatasetVersion();
        
        // Pass with nulls
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        // Pass with matching
        incoming.setId(1L);
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        // Fail with mismatched
        incoming.setId(2L);
        assertThrows(JsonParseException.class, () -> ManagedFieldsValidator.validateManagedFields(incoming, existing));
    }

    @Test
    public void testValidateManagedFields_Dataset() throws JsonParseException {
        Dataset existing = new Dataset();
        existing.setId(1L);
        existing.setAuthority("10.5072");
        
        Dataset incoming = new Dataset();
        
        // Pass with nulls
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        // Pass with matching
        incoming.setAuthority("10.5072");
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        // Fail with mismatched
        incoming.setAuthority("10.1234");
        assertThrows(JsonParseException.class, () -> ManagedFieldsValidator.validateManagedFields(incoming, existing));
    }

    @Test
    public void testValidateManagedFields_Dataset_DatasetType() throws JsonParseException {
        Dataset existing = new Dataset();
        // existing has no ID, so it's a new dataset
        
        Dataset incoming = new Dataset();
        DatasetType type = new DatasetType();
        type.setName("software");
        incoming.setDatasetType(type);
        
        // Pass on create even with different type
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        // Fail on update if type is different
        existing.setId(1L);
        DatasetType type2 = new DatasetType();
        type2.setName("dataset");
        existing.setDatasetType(type2);
        
        assertThrows(JsonParseException.class, () -> ManagedFieldsValidator.validateManagedFields(incoming, existing));
        
        // Pass on update if type is same
        incoming.setDatasetType(type2);
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
    }

    @Test
    public void testValidateManagedFields_Guestbook() throws JsonParseException {
        Guestbook existing = new Guestbook();
        existing.setId(123L);
        
        Guestbook incoming = new Guestbook();
        
        // Pass with nulls
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        // Pass with matching
        incoming.setId(123L);
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        // Fail with mismatched
        incoming.setId(456L);
        assertThrows(JsonParseException.class, () -> ManagedFieldsValidator.validateManagedFields(incoming, existing));
    }

    @Test
    public void testValidateManagedFields_TimeMatch() throws JsonParseException {
        DatasetVersion existing = new DatasetVersion();
        Date now = new Date();
        existing.setReleaseTime(now);
        
        DatasetVersion incoming = new DatasetVersion();
        incoming.setReleaseTime(new Date(now.getTime()));
        
        ManagedFieldsValidator.validateManagedFields(incoming, existing);
        
        incoming.setReleaseTime(new Date(now.getTime() + 1000));
        assertThrows(JsonParseException.class, () -> ManagedFieldsValidator.validateManagedFields(incoming, existing));
    }
}
