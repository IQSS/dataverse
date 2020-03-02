/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author skraffmi
 */
public class DatasetFieldValidatorTest {

    @ParameterizedTest
    @MethodSource("datasetFieldValues")
    public void testIsValid(String fieldValue, Option<FieldType> fieldType, Option<String> validationFormat,boolean expectedResult) {

        //given
        DatasetField df = new DatasetField();
        DatasetVersion datasetVersion = new DatasetVersion();
        Dataset dataset = new Dataset();
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);
        datasetVersion.setDataset(dataset);
        DatasetFieldType dft = new DatasetFieldType();
        dft.setFieldType(FieldType.TEXT);
        //Test Text against regular expression that takes a 5 character string
        dft.setValidationFormat("^[a-zA-Z ]{5,5}$");
        df.setDatasetFieldType(dft);
        df.setFieldValue("asdfg");
        df.setDatasetVersion(datasetVersion);

        final ConstraintValidatorContext ctx =
                Mockito.mock(ConstraintValidatorContext.class);

        //when & then
        DatasetFieldValidator instance = new DatasetFieldValidator();
        boolean expResult = true;
        boolean result = instance.isValid(df, ctx);
        assertEquals(expResult, result);

        df.setFieldValue(fieldValue);
        fieldType.peek(dft::setFieldType);
        validationFormat.peek(dft::setValidationFormat);
        result = instance.isValid(df, ctx);
        assertEquals(expectedResult, result);

    }

    @Test
    public void testIsValidAuthorIdentifierOrcid() {
        //given
        DatasetFieldValidator validator = new DatasetFieldValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_ORCID);

        //when & then
        assertTrue(validator.isValidAuthorIdentifier("0000-0002-1825-0097", pattern));
        // An "X" at the end of an ORCID is less common but still valid.
        assertTrue(validator.isValidAuthorIdentifier("0000-0002-1694-233X", pattern));
        assertFalse(validator.isValidAuthorIdentifier("0000 0002 1825 0097", pattern));
        assertFalse(validator.isValidAuthorIdentifier(" 0000-0002-1825-0097", pattern));
        assertFalse(validator.isValidAuthorIdentifier("0000-0002-1825-0097 ", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierIsni() {
        //given
        DatasetFieldValidator validator = new DatasetFieldValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_ISNI);

        //when & then
        assertTrue(validator.isValidAuthorIdentifier("0000000121032683", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierLcna() {
        //given
        DatasetFieldValidator validator = new DatasetFieldValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_LCNA);

        //when & then
        assertTrue(validator.isValidAuthorIdentifier("n82058243", pattern));
        assertTrue(validator.isValidAuthorIdentifier("foobar123", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierViaf() {
        //given
        DatasetFieldValidator validator = new DatasetFieldValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_VIAF);

        //when & then
        assertTrue(validator.isValidAuthorIdentifier("172389567", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierGnd() {
        //given
        DatasetFieldValidator validator = new DatasetFieldValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_GND);

        //when & then
        assertTrue(validator.isValidAuthorIdentifier("4079154-3", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    private static Stream<Arguments> datasetFieldValues() {
        return Stream.of(
                Arguments.of("value", Option.none(), Option.none(), true),
                Arguments.of("", Option.none(), Option.none(), true),
                Arguments.of("asdf", Option.none(), Option.none(), false),
                Arguments.of("asdfgx", Option.none(), Option.none(), false),
                Arguments.of("1999AD", Option.of(FieldType.DATE), Option.some(null), true),
                Arguments.of("44BCE", Option.of(FieldType.DATE), Option.none(), true),
                Arguments.of("2004-10-27", Option.of(FieldType.DATE), Option.none(), true),
                Arguments.of("2002-08", Option.of(FieldType.DATE), Option.none(), true),
                Arguments.of("[1999?]", Option.of(FieldType.DATE), Option.none(), true),
                Arguments.of("Blergh", Option.none(), Option.none(), false),
                Arguments.of("44", Option.of(FieldType.FLOAT), Option.none(), true),
                Arguments.of("44 1/2", Option.of(FieldType.FLOAT), Option.none(), false),
                Arguments.of("44", Option.of(FieldType.INT), Option.none(), true),
                Arguments.of("-44", Option.of(FieldType.INT), Option.none(), true),
                Arguments.of("12.14", Option.of(FieldType.INT), Option.none(), false),
                Arguments.of("http://cnn.com", Option.of(FieldType.URL), Option.none(), true),
                Arguments.of("espn.com", Option.none(), Option.none(), false)
        );
    }

}
