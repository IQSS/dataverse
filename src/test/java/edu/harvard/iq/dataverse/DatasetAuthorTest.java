package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DatasetAuthorTest {

    public String idType;
    public String idValue;
    public String expectedIdentifierAsUrl;

    public DatasetAuthorTest(String idType, String idValue, String expectedIdentifierAsUrl) {
        this.idType = idType;
        this.idValue = idValue;
        this.expectedIdentifierAsUrl = expectedIdentifierAsUrl;
    }

    @Parameters
    public static Collection<String[]> parameters() {
        return Arrays.asList(new String[][] {
            { "ORCID", "0000-0002-1825-0097", "https://orcid.org/0000-0002-1825-0097" },
            { "ISNI", "0000000121032683", "http://www.isni.org/isni/0000000121032683"},
            { "LCNA", "n82058243", "http://id.loc.gov/authorities/names/n82058243" },
            { "VIAF", "172389567", "https://viaf.org/viaf/172389567" },
            { "GND", "4079154-3", "https://d-nb.info/gnd/4079154-3" },
            { null, null, null, },
        });
    }

    @Test
    public void getIdentifierAsUrl() {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        if (idType !=null && idValue != null) {
            datasetAuthor.setIdType(idType);
            datasetAuthor.setIdValue(idValue);
        }
        assertEquals(expectedIdentifierAsUrl, datasetAuthor.getIdentifierAsUrl());
    }

}
