package edu.harvard.iq.dataverse.persistence.dataset;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetAuthorTest {

    public static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of("ORCID", "0000-0002-1825-0097", "https://orcid.org/0000-0002-1825-0097"),
                Arguments.of("ISNI", "0000000121032683", "http://www.isni.org/isni/0000000121032683"),
                Arguments.of("LCNA", "n82058243", "http://id.loc.gov/authorities/names/n82058243"),
                Arguments.of("VIAF", "172389567", "https://viaf.org/viaf/172389567"),
                Arguments.of("GND", "4079154-3", "https://d-nb.info/gnd/4079154-3"),
                Arguments.of(null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void getIdentifierAsUrl(String idType, String idValue, String expectedIdentifierAsUrl) {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        if (idType != null && idValue != null) {
            datasetAuthor.setIdType(idType);
            datasetAuthor.setIdValue(idValue);
        }
        assertEquals(expectedIdentifierAsUrl, datasetAuthor.getIdentifierAsUrl());
    }

}
