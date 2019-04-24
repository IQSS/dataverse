/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.export.openaire.OpenAireExportUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Scanner;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class OpenAireExportUtilTest {

    public OpenAireExportUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test: 1, Identifier (with mandatory type sub-property) (M)
     *
     * identifier
     */
    @Test
    public void testWriteIdentifierElement() throws XMLStreamException {
        System.out.println("writeIdentifierElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        // doi
        String persistentAgency = "doi";
        String persistentAuthority = "10.123";
        String persistentId = "123";
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId);
        OpenAireExportUtil.writeIdentifierElement(xmlw, globalId.toURL().toString(), null);
        xmlw.close();
        Assert.assertEquals("<identifier identifierType=\"DOI\">"
                + persistentAuthority + "/" + persistentId + "</identifier>",
                sw.toString());

        // handle
        sw = new StringWriter();
        xmlw = f.createXMLStreamWriter(sw);
        persistentAgency = "hdl";
        persistentAuthority = "1902.1";
        persistentId = "111012";
        globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId);
        OpenAireExportUtil.writeIdentifierElement(xmlw, globalId.toURL().toString(), null);
        xmlw.close();
        Assert.assertEquals("<identifier identifierType=\"Handle\">"
                + persistentAuthority + "/" + persistentId + "</identifier>",
                sw.toString());
    }

    /**
     * Test: 2, Creator (with optional given name, family name, name identifier
     * and affiliation sub-properties) (M)
     *
     * creators
     */
    @Test
    public void testWriteCreatorsElement() throws XMLStreamException, FileNotFoundException {
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-simplified.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeCreatorsElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<creators>"
                + "<creator>"
                + "<creatorName nameType=\"Personal\">Privileged, Pete</creatorName>"
                + "<givenName>Pete</givenName>"
                + "<familyName>Privileged</familyName>"
                + "<nameIdentifier nameIdentifierScheme=\"ORCID\">ellenid</nameIdentifier>"
                + "<affiliation>Top</affiliation>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Personal\">Awesome, Audrey</creatorName>"
                + "<givenName>Audrey</givenName>"
                + "<familyName>Awesome</familyName>"
                + "<nameIdentifier nameIdentifierScheme=\"DAISY\">audreyId</nameIdentifier>"
                + "<affiliation>Bottom</affiliation>"
                + "</creator>"
                + "<creator>"
                + "<creatorName>Apache Foundation</creatorName>"
                + "<nameIdentifier nameIdentifierScheme=\"DAISY\">audreyId</nameIdentifier>"
                + "<affiliation>Bottom</affiliation>"
                + "</creator>"
                + "</creators>",
                sw.toString());   
    }

    /**
     * Test: 3, Title (with optional type sub-properties) (M)
     *
     * titles
     */
    @Test
    public void testWriteTitleElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeTotlesElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-simplified.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeTitlesElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<titles><title>My Dataset</title></titles>",
                sw.toString());
    }

    /**
     * Test: 4, Publisher (M)
     *
     * publisher
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWritePublisherElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writePublisherElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        DatasetDTO datasetDto = new DatasetDTO();
        datasetDto.setPublisher("Publisher01");
        String publisher = datasetDto.getPublisher();
        OpenAireExportUtil.writeFullElement(xmlw, null, "publisher", null, publisher, null);

        xmlw.close();
        Assert.assertEquals("<publisher>Publisher01</publisher>",
                sw.toString());
    }

    /**
     * Test: 5, PublicationYear (M)
     *
     * publicationYear
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWritePublicationYearElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writePublicationYearElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-simplified.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writePublicationYearElement(xmlw, dto, null, null);

        xmlw.close();
        Assert.assertEquals("<publicationYear>2014</publicationYear>",
                sw.toString());
    }

    /**
     * Test: 6, Subject (with scheme sub-property) R
     *
     * subjects
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testSubjectsElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeSubjectsElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeSubjectsElement(xmlw, dto, null);
        xmlw.close();
        Assert.assertEquals("<subjects>"
                + "<subject>Agricultural Sciences</subject>"
                + "<subject>Business and Management</subject>"
                + "<subject>Engineering</subject>"
                + "<subject>Law</subject>"
                + "<subject schemeURI=\"http://KeywordVocabularyURL1.org\" "
                + "subjectScheme=\"KeywordVocabulary1\">KeywordTerm1</subject>"
                + "<subject schemeURI=\"http://KeywordVocabularyURL2.org\" "
                + "subjectScheme=\"KeywordVocabulary2\">KeywordTerm2</subject>"
                + "</subjects>",
                sw.toString());
    }

    /**
     * Test: 7, Contributor (with optional given name, family name, name
     * identifier and affiliation sub-properties)
     *
     * contributors
     */
    @Test
    public void testWriteContributorsElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeContributorsElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-simplified.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeContributorsElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<contributors>"
                + "<contributor contributorType=\"ContactPerson\"><contributorName>pete@malinator.com</contributorName>"
                + "</contributor></contributors>",
                sw.toString());
    }

        /**
     * Test: 7, Contributor (with optional given name, family name, name
     * identifier and affiliation sub-properties)
     *
     * contributors
     */
    @Test
    public void testWriteContributorsElementComplete() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeContributorsElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeContributorsElement(xmlw, dto, null);

        xmlw.close();
        System.out.println(sw.toString());
        Assert.assertEquals("<contributors>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName>LastContact1, FirstContact1</contributorName>"
                + "<affiliation>ContactAffiliation1</affiliation>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Personal\">Condon, Kevin</contributorName>"
                + "<givenName>Kevin</givenName><familyName>Condon</familyName>"
                + "<affiliation>ContactAffiliation2</affiliation>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Personal\">Philip Durbin</contributorName>"
                + "<givenName>Philip</givenName><familyName>Durbin</familyName>"
                + "<affiliation>ContactAffiliation3</affiliation>"
                + "</contributor>"
                + "<contributor contributorType=\"Producer\">"
                + "<contributorName>LastProducer1, FirstProducer1</contributorName>"
                + "<affiliation>ProducerAffiliation1</affiliation>"
                + "</contributor><contributor contributorType=\"Producer\">"
                + "<contributorName>LastProducer2, FirstProducer2</contributorName>"
                + "<affiliation>ProducerAffiliation2</affiliation>"
                + "</contributor>"
                + "<contributor contributorType=\"DataCollector\">"
                + "<contributorName>LastContributor1, FirstContributor1</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"DataCurator\">"
                + "<contributorName>LastContributor2, FirstContributor2</contributorName>"
                + "</contributor><contributor contributorType=\"Distributor\">"
                + "<contributorName>LastDistributor1, FirstDistributor1</contributorName>"
                + "<affiliation>DistributorAffiliation1</affiliation>"
                + "</contributor>"
                + "<contributor contributorType=\"Distributor\">"
                + "<contributorName>LastDistributor2, FirstDistributor2</contributorName>"
                + "<affiliation>DistributorAffiliation2</affiliation>"
                + "</contributor>"
                + "</contributors>",
                sw.toString());
    }
    
    /**
     * Test: 8, Date (with type sub-property) (R)
     *
     * dates
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteDatesElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeDatesElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeDatesElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<dates>"
                + "<date dateType=\"Issued\">1004-01-01</date>"
                + "<date dateType=\"Created\">1003-01-01</date>"
                + "<date dateType=\"Submitted\">1002-01-01</date>"
                + "<date dateType=\"Updated\">2015-09-29</date>"
                + "<date dateType=\"Collected\">1006-01-01/1006-01-01</date>"
                + "<date dateType=\"Collected\">1006-02-01/1006-02-02</date>"
                + "</dates>",
                sw.toString());
    }

    /**
     * Test: 9, Language (O)
     *
     * language
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteLanguageElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeRelatedIdentifierElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        String language = OpenAireExportUtil.getLanguage(xmlw, dto);
        OpenAireExportUtil.writeFullElement(xmlw, null, "language", null, language, null);

        xmlw.close();
        Assert.assertEquals("<language>it</language>",
                sw.toString());
    }

    /**
     * Test: 10, ResourceType (with mandatory general type description sub-
     * property) (M)
     *
     * resourceType
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteResourceTypeElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeResourceTypeElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeResourceTypeElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<resourceType resourceTypeGeneral=\"Dataset\">"
                + "KindOfData1</resourceType>",
                sw.toString());
    }

    /**
     * Test: 11 AlternateIdentifier (with type sub-property) (O)
     *
     * alternateIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteAlternateIdentifierElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeAlternateIdentifierElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        OpenAireExportUtil.writeAlternateIdentifierElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<alternateIdentifiers>"
                + "<alternateIdentifier alternateIdentifierType=\"OtherIDAgency1\">"
                + "OtherIDIdentifier1</alternateIdentifier>"
                + "<alternateIdentifier alternateIdentifierType=\"OtherIDAgency2\">"
                + "OtherIDIdentifier2</alternateIdentifier>"
                + "</alternateIdentifiers>",
                sw.toString());
    }

    /**
     * Test: 12, RelatedIdentifier (with type and relation type sub-properties)
     * (R)
     *
     * relatedIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteRelatedIdentifierElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeRelatedIdentifierElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeRelatedIdentifierElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<relatedIdentifiers>"
                + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"ARK\">"
                + "RelatedPublicationIDNumber1</relatedIdentifier>"
                + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"arXiv\">"
                + "RelatedPublicationIDNumber2</relatedIdentifier>"
                + "</relatedIdentifiers>",
                sw.toString());
    }

    /**
     * Test: 13, Size (O)
     *
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteEmptySizeElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeEmptySizeElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // set an empty file list
            dto.setFiles(new ArrayList<FileDTO>());

            // Fragment must be enclosed in a fake root element.
            xmlw.writeStartElement("root");
            OpenAireExportUtil.writeSizeElement(xmlw, dto, null);

            xmlw.writeEndElement();
        }
        xmlw.close();
        Assert.assertEquals("<root />",
                sw.toString());
    }

    /**
     * Test: 13, Size (O)
     *
     * relatedIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteSizeElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeSizeElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeSizeElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<sizes>"
                + "<size>1000</size>"
                + "<size>20</size>"
                + "</sizes>",
                sw.toString());
    }

    /**
     * Test: 14, Format (O)
     *
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteEmptyFormatElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeEmptyFormatElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // set an empty file list
            dto.setFiles(new ArrayList<FileDTO>());

            // Fragment must be enclosed in a fake root element.
            xmlw.writeStartElement("root");
            OpenAireExportUtil.writeFormatElement(xmlw, dto, null);

            xmlw.writeEndElement();
        }

        xmlw.close();
        Assert.assertEquals("<root />",
                sw.toString());
    }

    /**
     * Test: 14, Format (O)
     *
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteFormatElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeFormatElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeFormatElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<formats>"
                + "<format>application/pdf</format>"
                + "<format>application/xml</format>"
                + "</formats>",
                sw.toString());
    }

    /**
     * Test: 15, Version (O)
     *
     * version
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteEmptyVersionElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeEmptyVersionElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // Fragment must be enclosed in a fake root element.
            xmlw.writeStartElement("root");
            OpenAireExportUtil.writeVersionElement(xmlw, dto, null);

            xmlw.writeEndElement();
        }

        xmlw.close();
        Assert.assertEquals("<root><version>1.0</version></root>",
                sw.toString());
    }

    /**
     * Test: 15, Version (O)
     *
     * version
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteVersionElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeVersionElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            dto.setVersionNumber(2L);
            dto.setMinorVersionNumber(1L);
        }
        OpenAireExportUtil.writeVersionElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<version>2.1</version>",
                sw.toString());
    }

    /**
     * Test: 16 Rights (O)
     *
     * rights
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteAccessRightElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeAccessRightElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeAccessRightsElement(xmlw, dto, null);
        xmlw.close();
        Assert.assertEquals("<rightsList>"
                + "<rights rightsURI=\"info:eu-repo/semantics/closedAccess\" />"
                + "<rights rightsURI=\"https://creativecommons.org/publicdomain/zero/1.0/\">"
                + "CC0 Waiver</rights></rightsList>",
                sw.toString());
    }

    /**
     * Test: 16 Rights (O)
     *
     * rights
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteRestrictedAccessRightElementWithRequestAccessEnabled() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeRestrictedAccessRightElementWithRequestAccessEnabled");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            dto.setLicense(null);
            dto.setTermsOfUse(null);
            dto.setFileAccessRequest(true);
        }

        OpenAireExportUtil.writeAccessRightsElement(xmlw, dto, null);
        xmlw.close();
        Assert.assertEquals("<rightsList>"
                + "<rights rightsURI=\"info:eu-repo/semantics/restrictedAccess\" />"
                + "<rights /></rightsList>",
                sw.toString());
    }

    /**
     * Test: 16 Rights (O)
     *
     * rights
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteRestrictedAccessRightElementWithRequestAccessDisabled() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeWriteRestrictedAccessRightElementWithRequestAccessDisabled");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            dto.setLicense(null);
            dto.setTermsOfUse(null);
            dto.setFileAccessRequest(false);
        }

        OpenAireExportUtil.writeAccessRightsElement(xmlw, dto, null);
        xmlw.close();
        Assert.assertEquals("<rightsList>"
                + "<rights rightsURI=\"info:eu-repo/semantics/closedAccess\" />"
                + "<rights /></rightsList>",
                sw.toString());
    }

    /**
     * Test: 17, Description (R)
     *
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteDescriptionsElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeDescriptionsElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeDescriptionsElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<descriptions>"
                + "<description descriptionType=\"Abstract\">DescriptionText 1"
                + "</description>"
                + "<description descriptionType=\"Abstract\">DescriptionText2"
                + "</description>"
                + "<description descriptionType=\"Methods\">SoftwareName1, SoftwareVersion1"
                + "</description>"
                + "<description descriptionType=\"Methods\">SoftwareName2, SoftwareVersion2"
                + "</description>"
                + "<description descriptionType=\"Methods\">OriginOfSources"
                + "</description>"
                + "<description descriptionType=\"Methods\">CharacteristicOfSourcesNoted"
                + "</description>"
                + "<description descriptionType=\"Methods\">DocumentationAndAccessToSources"
                + "</description>"
                + "<description descriptionType=\"SeriesInformation\">SeriesInformation"
                + "</description>"
                + "<description descriptionType=\"Other\">Notes1"
                + "</description></descriptions>",
                sw.toString());
    }

    /**
     * Test: 18, GeoLocation (with point, box and polygon sub-properties) (R)
     *
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteGeoLocationElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeGeoLocationElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeGeoLocationsElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<geoLocations>"
                + "<geoLocation>"
                + "<geoLocationPlace>ProductionPlace</geoLocationPlace>"
                + "<geoLocationBox>"
                + "<westBoundLongitude>10</westBoundLongitude>"
                + "<eastBoundLongitude>20</eastBoundLongitude>"
                + "<northBoundLatitude>30</northBoundLatitude>"
                + "<southBoundLatitude>40</southBoundLatitude>"
                + "</geoLocationBox>"
                + "</geoLocation>"
                + "<geoLocation>"
                + "<geoLocationPlace>ProductionPlace</geoLocationPlace>"
                + "<geoLocationBox>"
                + "<southBoundLatitude>80</southBoundLatitude>"
                + "<northBoundLatitude>70</northBoundLatitude>"
                + "<eastBoundLongitude>60</eastBoundLongitude>"
                + "<westBoundLongitude>50</westBoundLongitude>"
                + "</geoLocationBox>"
                + "</geoLocation></geoLocations>",
                sw.toString());
    }

    /**
     * Test: 18, GeoLocation (with point, box and polygon sub-properties) (R)
     *
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteGeoLocationElement2() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeGeoLocationElement2");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-simplified.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeGeoLocationsElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<geoLocations>"
                + "<geoLocation>"
                + "<geoLocationBox>"
                + "<eastBoundLongitude>23</eastBoundLongitude>"
                + "<northBoundLatitude>786</northBoundLatitude>"
                + "<westBoundLongitude>45</westBoundLongitude>"
                + "<southBoundLatitude>34</southBoundLatitude>"
                + "</geoLocationBox>"
                + "</geoLocation></geoLocations>",
                sw.toString());
    }

    /**
     * Test: 19, FundingReference (with name, identifier, and award related sub-
     * properties) (O)
     *
     * fundingReference
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteFundingReferencesElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeFundingReferencesElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeFundingReferencesElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<fundingReferences><fundingReference>"
                + "<funderName>GrantInformationGrantAgency1</funderName>"
                + "<awardNumber>GrantInformationGrantNumber1</awardNumber>"
                + "</fundingReference>"
                + "<fundingReference>"
                + "<funderName>GrantInformationGrantAgency2</funderName>"
                + "<awardNumber>GrantInformationGrantNumber2</awardNumber>"
                + "</fundingReference></fundingReferences>",
                sw.toString());
    }

    /**
     * Test 19.1, funderName Name of the funding provider.
     *
     * funderName
     */
    @Test
    public void testWriteFunderNamePropertyNotInContributor() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeFunderNamePropertyNotInContributor");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/datase-updated.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // Fragment must be enclosed in a fake root element.
            xmlw.writeStartElement("root");
            OpenAireExportUtil.writeContributorsElement(xmlw, dto, null);

            xmlw.writeEndElement();
        }
        xmlw.close();
        Assert.assertEquals("<root />",
                sw.toString());
    }
    
    /**
     * Test 19.1, funderName Name of the funding provider.
     *
     * funderName
     */
    @Test
    public void testWriteFunderNamePropertyInFundingReferencesElement() throws XMLStreamException, FileNotFoundException {
        System.out.println("writeFunderNamePropertyInFundingReferencesElement");
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter();
        XMLStreamWriter xmlw = f.createXMLStreamWriter(sw);

        File file = new File("src/test/java/edu/harvard/iq/dataverse/export/datase-updated.txt");
        String text = new Scanner(file).useDelimiter("\\Z").next();
        Gson gson = new Gson();
        DatasetDTO datasetDto = gson.fromJson(text, DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeFundingReferencesElement(xmlw, dto, null);

        xmlw.close();
        Assert.assertEquals("<fundingReferences>"
                + "<fundingReference><funderName>Dennis</funderName></fundingReference>"
                + "<fundingReference><funderName>NIH</funderName><awardNumber>NIH1231245154</awardNumber></fundingReference>"
                + "<fundingReference><funderName>NIH</funderName><awardNumber>NIH99999999</awardNumber></fundingReference>"
                + "</fundingReferences>",
                sw.toString());
    }
}
