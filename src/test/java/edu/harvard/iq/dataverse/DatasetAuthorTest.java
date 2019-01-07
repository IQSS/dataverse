package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DatasetAuthorTest {

    @Test
    public void testGetIdentifierAsUrlOrcid() {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        datasetAuthor.setIdType("ORCID");
        datasetAuthor.setIdValue("0000-0002-1825-0097");
        String result = datasetAuthor.getIdentifierAsUrl();
        assertEquals("https://orcid.org/0000-0002-1825-0097", result);
    }

    @Test
    public void testGetIdentifierAsUrlIsni() {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        datasetAuthor.setIdType("ISNI");
        datasetAuthor.setIdValue("0000000121032683");
        String result = datasetAuthor.getIdentifierAsUrl();
        assertEquals("http://www.isni.org/isni/0000000121032683", result);
    }

    @Test
    public void testGetIdentifierAsUrlLcna() {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        datasetAuthor.setIdType("LCNA");
        datasetAuthor.setIdValue("n82058243");
        String result = datasetAuthor.getIdentifierAsUrl();
        assertEquals("http://id.loc.gov/authorities/names/n82058243", result);
    }

    @Test
    public void testGetIdentifierAsUrlViaf() {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        datasetAuthor.setIdType("VIAF");
        datasetAuthor.setIdValue("172389567");
        String result = datasetAuthor.getIdentifierAsUrl();
        assertEquals("https://viaf.org/viaf/172389567", result);
    }

    @Test
    public void testGetIdentifierAsUrlGnd() {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        datasetAuthor.setIdType("GND");
        datasetAuthor.setIdValue("4079154-3");
        String result = datasetAuthor.getIdentifierAsUrl();
        assertEquals("https://d-nb.info/gnd/4079154-3", result);
    }

    @Test
    public void testGetIdentifierAsUrlNull() {
        DatasetAuthor datasetAuthor = new DatasetAuthor();
        String result = datasetAuthor.getIdentifierAsUrl();
        assertEquals(null, result);
    }

}
