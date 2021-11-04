package edu.harvard.iq.dataverse.persistence.dataset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author skraffmi
 */
public class DatasetFieldTypeTest {


    @Test
    public void testIsSanitizeHtml() {
        System.out.println("isSanitizeHtml");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(FieldType.TEXT);
        Boolean result = instance.isSanitizeHtml();
        assertThat(result).isFalse();

        //if textbox then sanitize - allow tags
        instance.setFieldType(FieldType.TEXTBOX);
        result = instance.isSanitizeHtml();
        assertThat(result).isTrue();

        //if textbox then don't sanitize - allow tags
        instance.setFieldType(FieldType.EMAIL);
        result = instance.isSanitizeHtml();
        assertThat(result).isFalse();

        //URL, too
        instance.setFieldType(FieldType.URL);
        result = instance.isSanitizeHtml();
        assertThat(result).isTrue();
    }

    @Test
    public void testIsEscapeOutputText() {
        System.out.println("testIsEscapeOutputText");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(FieldType.TEXT);
        Boolean result = instance.isEscapeOutputText();
        assertThat(result).isTrue();

        //if Disaplay Format includes a link then don't escape
        instance.setDisplayFormat("'<a target=\"_blank\" href=\"http://www.rcsb.org/pdb/explore/explore.do?structureId=#VALUE\">PDB (RCSB) #VALUE</a>'");
        result = instance.isEscapeOutputText();
        assertThat(result).isFalse();

        //if textbox then sanitize - allow tags
        instance.setFieldType(FieldType.TEXTBOX);
        result = instance.isEscapeOutputText();
        assertThat(result).isFalse();

        //if textbox then don't sanitize - allow tags
        instance.setFieldType(FieldType.EMAIL);
        result = instance.isEscapeOutputText();
        assertThat(result).isTrue();

        //URL, too
        instance.setFieldType(FieldType.URL);
        result = instance.isEscapeOutputText();
        assertThat(result).isFalse();

    }

    @Test
    public void isParentAllowsMutlipleValues_parentReturnsTrue() {
        //given
        DatasetFieldType parentDsf = new DatasetFieldType();
        parentDsf.setAllowMultiples(true);

        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setParentDatasetFieldType(parentDsf);

        //when
        boolean parentAllowsMutlipleValues = datasetFieldType.isThisOrParentAllowsMultipleValues();

        //then
        assertThat(parentAllowsMutlipleValues).isTrue();
    }

    @Test
    public void isParentAllowsMutlipleValues_parentReturnsFalse() {
        //given
        DatasetFieldType parentDsf = new DatasetFieldType();
        parentDsf.setAllowMultiples(false);

        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setParentDatasetFieldType(parentDsf);

        //when
        boolean parentAllowsMutlipleValues = datasetFieldType.isThisOrParentAllowsMultipleValues();

        //then
        assertThat(parentAllowsMutlipleValues).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "true, true, true, true",
            "true, true, false, false",
            "true, false, true, true",
            "true, false, false, false",
            "false, true, true, false",
            "false, true, false, false",
            "false, false, true, false",
            "false, false, false, false",
    })
    void isSeparableOnGui(boolean allowMultiples, boolean compound, boolean containsTextbox, boolean shouldBeSeparable) {
        // given
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setAllowMultiples(allowMultiples);
        if (compound) {
            DatasetFieldType child = new DatasetFieldType();
            child.setFieldType(containsTextbox ? FieldType.TEXTBOX : FieldType.TEXT);
            fieldType.getChildDatasetFieldTypes().add(child);
        } else {
            fieldType.setFieldType(containsTextbox ? FieldType.TEXTBOX : FieldType.TEXT);
        }

        // when
        boolean separableOnGui = fieldType.isSeparableOnGui();

        // then
        assertThat(separableOnGui).isEqualTo(shouldBeSeparable);
    }
}
