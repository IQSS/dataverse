package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.fillAuthorField;
import static edu.harvard.iq.dataverse.persistence.MockMetadataFactory.makeAuthorFieldType;

class DatasetFieldDiffTest {

    @Test
    public void generatePairs() {
        //given
        DatasetField authorField = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(new MetadataBlock()), null);
        fillAuthorField(authorField,  "John Doe", "John Aff");

        DatasetField secondAuthor = DatasetField.createNewEmptyDatasetField(makeAuthorFieldType(new MetadataBlock()), null);
        fillAuthorField(secondAuthor,  "John 2", "John 2");

        //when
        Tuple2<String, String> valuePairs = new DatasetFieldDiff(authorField, secondAuthor, authorField.getDatasetFieldType()).generatePairOfJoinedValues();

        //then

        Assertions.assertAll(() -> Assert.assertEquals("John Doe; John Aff", valuePairs._1),
                             () -> Assert.assertEquals("John 2; John 2", valuePairs._2));
    }
}