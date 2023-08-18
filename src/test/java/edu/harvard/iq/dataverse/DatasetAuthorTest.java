package edu.harvard.iq.dataverse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetAuthorTest {

    @ParameterizedTest
    @CsvSource(value = {
        "ORCID,0000-0002-1825-0097,https://orcid.org/0000-0002-1825-0097",
        "ISNI,0000000121032683,http://www.isni.org/isni/0000000121032683",
        "LCNA,n82058243,http://id.loc.gov/authorities/names/n82058243",
        "VIAF,172389567,https://viaf.org/viaf/172389567",
        "GND,4079154-3,https://d-nb.info/gnd/4079154-3",
        "ResearcherID,634082,https://publons.com/researcher/634082/",
        "ResearcherID,AAW-9289-2021,https://publons.com/researcher/AAW-9289-2021/",
        "ResearcherID,J-9733-2013,https://publons.com/researcher/J-9733-2013/",
        "ScopusID,6602344670,https://www.scopus.com/authid/detail.uri?authorId=6602344670",
        "NULL,NULL,NULL"
    }, nullValues = "NULL")
    void getIdentifierAsUrl(String idType, String idValue, String expectedIdentifierAsUrl) {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        if (idType !=null && idValue != null) {
            datasetAuthor.setIdType(idType);
            datasetAuthor.setIdValue(idValue);
        }
        assertEquals(expectedIdentifierAsUrl, datasetAuthor.getIdentifierAsUrl());
    }
}
