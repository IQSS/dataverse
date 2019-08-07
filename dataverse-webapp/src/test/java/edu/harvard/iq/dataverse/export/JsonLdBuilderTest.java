package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.StringReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class JsonLdBuilderTest {

    /**
     * See also SchemaDotOrgExporterTest.java for more extensive tests.
     */
    @Test
    public void testGetJsonLd() throws ParseException {
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        assertEquals("", datasetVersion.getPublicationDateAsString());
        // Only published datasets return any JSON.
        assertEquals("", JsonLdBuilder.buildJsonLd(datasetVersion, "localhost", "true"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        datasetVersion.setReleaseTime(publicationDate);
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
        datasetVersion.setTermsOfUseAndAccess(terms);
        String jsonLd = JsonLdBuilder.buildJsonLd(datasetVersion, "localhost", "true");
        System.out.println("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertEquals("Dataset", obj.getJsonObject("license").getString("@type"));
        assertEquals("CC0", obj.getJsonObject("license").getString("text"));
        assertEquals("https://creativecommons.org/publicdomain/zero/1.0/", obj.getJsonObject("license").getString("url"));
        assertEquals("1955-11-05", obj.getString("dateModified"));
        assertEquals("1955-11-05", obj.getString("datePublished"));
        assertEquals("1", obj.getString("version"));
        // TODO: if it ever becomes easier to mock a dataset title, test it.
        assertEquals("", obj.getString("name"));
        // TODO: If it ever becomes easier to mock authors, test them.
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assertEquals(emptyArray, obj.getJsonArray("creator"));
        assertEquals(emptyArray, obj.getJsonArray("author"));
        // TODO: If it ever becomes easier to mock subjects, test them.
        assertEquals(emptyArray, obj.getJsonArray("keywords"));
        assertEquals("Organization", obj.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("publisher").getString("name"));
        assertEquals("Organization", obj.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("provider").getString("name"));
        assertEquals("LibraScholar", obj.getJsonObject("includedInDataCatalog").getString("name"));
    }

    @Test
    public void testGetJsonLdNonCC0License() throws ParseException {
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        assertEquals("", datasetVersion.getPublicationDateAsString());
        // Only published datasets return any JSON.
        assertEquals("", JsonLdBuilder.buildJsonLd(datasetVersion, "localhost", "true"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        datasetVersion.setReleaseTime(publicationDate);
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(TermsOfUseAndAccess.License.NONE);
        terms.setTermsOfUse("Call me maybe");
        datasetVersion.setTermsOfUseAndAccess(terms);

        String jsonLd = JsonLdBuilder.buildJsonLd(datasetVersion, "localhost", "true");
        System.out.println("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertEquals("Dataset", obj.getJsonObject("license").getString("@type"));
        assertEquals("Call me maybe", obj.getJsonObject("license").getString("text"));
        assertEquals("1955-11-05", obj.getString("dateModified"));
        assertEquals("1955-11-05", obj.getString("datePublished"));
        assertEquals("1", obj.getString("version"));
        // TODO: if it ever becomes easier to mock a dataset title, test it.
        assertEquals("", obj.getString("name"));
        // TODO: If it ever becomes easier to mock authors, test them.
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assertEquals(emptyArray, obj.getJsonArray("creator"));
        assertEquals(emptyArray, obj.getJsonArray("author"));
        // TODO: If it ever becomes easier to mock subjects, test them.
        assertEquals(emptyArray, obj.getJsonArray("keywords"));
        assertEquals("Organization", obj.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("publisher").getString("name"));
        assertEquals("Organization", obj.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("provider").getString("name"));
        assertEquals("LibraScholar", obj.getJsonObject("includedInDataCatalog").getString("name"));
    }
    
}
