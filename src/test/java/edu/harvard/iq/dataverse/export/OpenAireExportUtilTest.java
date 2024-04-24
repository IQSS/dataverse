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
import edu.harvard.iq.dataverse.export.openaire.OpenAireExportUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.AbstractDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.handle.HandlePidProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenAireExportUtilTest {

    private static final Logger logger = Logger.getLogger(OpenAireExportUtilTest.class.getCanonicalName());
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    private final StringWriter stringWriter = new StringWriter();
    private XMLStreamWriter xmlWriter;

    @BeforeEach
    private void setup() throws XMLStreamException {
        xmlWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
    }
    @AfterEach
    private void teardown() throws XMLStreamException {
        stringWriter.flush();
        xmlWriter.close();
    }

    /**
     * Test: 1a, Identifier (with mandatory type sub-property) (M) - DOI version
     */
    @Test
    public void testWriteIdentifierElementDoi() throws XMLStreamException {
        // given
        String persistentAgency = "doi";
        String persistentAuthority = "10.123";
        String persistentId = "123";
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId, null, AbstractDOIProvider.DOI_RESOLVER_URL, null);

        // when
        OpenAireExportUtil.writeIdentifierElement(xmlWriter, globalId.asURL(), null);
        xmlWriter.flush();

        // then
        assertEquals(String.format("<identifier identifierType=\"DOI\">%s/%s</identifier>", persistentAuthority, persistentId),
                stringWriter.toString());
    }

    /**
     * Test: 1b, Identifier (with mandatory type sub-property) (M) - Handle version
     */
    @Test
    public void testWriteIdentifierElementHandle() throws XMLStreamException {
        // given
        String persistentAgency = "hdl";
        String persistentAuthority = "1902.1";
        String persistentId = "111012";
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId, null, HandlePidProvider.HDL_RESOLVER_URL, null);

        // when
        OpenAireExportUtil.writeIdentifierElement(xmlWriter, globalId.asURL(), null);
        xmlWriter.flush();

        // then
        assertEquals(String.format("<identifier identifierType=\"Handle\">%s/%s</identifier>", persistentAuthority, persistentId),
                stringWriter.toString());
    }

    /**
     * Test: 2, Creator (with optional given name, family name, name identifier
     * and affiliation sub-properties) (M)
     *
     * creators
     */
    @Test
    public void testWriteCreatorsElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-simplified.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeCreatorsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        // then
        assertEquals("<creators>"
                + "<creator>"
                + "<creatorName nameType=\"Personal\">Privileged, Pete</creatorName>"
                + "<givenName>Pete</givenName>"
                + "<familyName>Privileged</familyName>"
                + "<nameIdentifier nameIdentifierScheme=\"ORCID\">ellenid</nameIdentifier>"
                + "<affiliation>Top</affiliation>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Personal\">Smith, John</creatorName>"
                + "<givenName>John</givenName>"
                + "<familyName>Smith</familyName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Personal\">John Smith</creatorName>"
                + "<givenName>John</givenName>"
                + "<familyName>Smith</familyName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Personal\">Awesome, Audrey</creatorName>"
                + "<givenName>Audrey</givenName>"
                + "<familyName>Awesome</familyName>"
                + "<nameIdentifier nameIdentifierScheme=\"DAISY\">audreyId</nameIdentifier>"
                + "<affiliation>Bottom</affiliation>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">Apache Foundation</creatorName>"
                + "<nameIdentifier nameIdentifierScheme=\"DAISY\">audreyId</nameIdentifier>"
                + "<affiliation>Bottom</affiliation>"
                + "</creator>"
                + "</creators>",
                stringWriter.toString());
    }

    /**
     * Test: 2, Creator (with optional given name, family name, name identifier
     * and affiliation sub-properties) (M)
     *
     * nameType="Organizational"
     * 
     * creators
     */
    @Test
    public void testWriteCreatorsElementWithOrganizations() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-organizations.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeCreatorsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        // then
        assertEquals("<creators>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">IBM</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">Harvard University</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">The Institute for Quantitative Social Science</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">The Ford Foundation</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">United Nations Economic and Social Commission for Asia and the Pacific (UNESCAP)</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">Michael J. Fox Foundation for Parkinson's Research</creatorName>"
                + "</creator>"
                + "</creators>",
                stringWriter.toString());
    }
    
    /**
     * Test: 2, Creator (with optional given name, family name, name identifier
     * and affiliation sub-properties) (M)
     *
     * nameType="Organizational"
     * 
     * creators
     */
    @Test
    public void testWriteCreatorsElementWithOrganizationsAndComma() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-organizations-comma.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeCreatorsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        // then
        assertEquals("<creators>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">Digital Archive of Massachusetts Anti-Slavery and Anti-Segregation Petitions, Massachusetts Archives, Boston MA</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">U.S. Department of Commerce, Bureau of the Census, Geography Division</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">Harvard Map Collection, Harvard College Library</creatorName>"
                + "</creator>"
                + "<creator>"
                + "<creatorName nameType=\"Organizational\">Geographic Data Technology, Inc. (GDT)</creatorName>"
                + "</creator>"
                + "</creators>",
                stringWriter.toString());
    }
    
    /**
     * Test: 3, Title (with optional type sub-properties) (M)
     *
     * titles
     */
    @Test
    public void testWriteTitleElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-simplified.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeTitlesElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<titles><title>My Dataset</title></titles>", stringWriter.toString());
    }

    /**
     * Test: 4, Publisher (M)
     *
     * publisher
     *
     * @throws javax.xml.stream.XMLStreamException
     */
    @Test
    public void testWritePublisherElement() throws XMLStreamException {
        // given
        DatasetDTO datasetDto = new DatasetDTO();
        datasetDto.setPublisher("Publisher01");
        String publisher = datasetDto.getPublisher();

        // when
        OpenAireExportUtil.writeFullElement(xmlWriter, null, "publisher", null, publisher, null);
        xmlWriter.flush();

        //then
        assertEquals("<publisher>Publisher01</publisher>", stringWriter.toString());
    }

    /**
     * Test: 5, PublicationYear (M)
     *
     * publicationYear
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWritePublicationYearElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-simplified.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writePublicationYearElement(xmlWriter, dto, null, null);
        xmlWriter.flush();

        // then
        assertEquals("<publicationYear>2014</publicationYear>", stringWriter.toString());
    }

    /**
     * Test: 6, Subject (with scheme sub-property) R
     *
     * subjects
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testSubjectsElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeSubjectsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        // then
        assertEquals("<subjects>"
                + "<subject>Agricultural Sciences</subject>"
                + "<subject>Business and Management</subject>"
                + "<subject>Engineering</subject>"
                + "<subject>Law</subject>"
                + "<subject schemeURI=\"http://KeywordVocabularyURL1.org\" "
                + "subjectScheme=\"KeywordVocabulary1\">KeywordTerm1</subject>"
                + "<subject schemeURI=\"http://KeywordVocabularyURL2.org\" "
                + "subjectScheme=\"KeywordVocabulary2\">KeywordTerm2</subject>"
                + "</subjects>",
                stringWriter.toString());
    }

    /**
     * Test: 7, Contributor (with optional given name, family name, name
     * identifier and affiliation sub-properties)
     *
     * contributors
     */
    @Test
    public void testWriteContributorsElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-simplified.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeContributorsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        // then
        assertEquals("<contributors>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Personal\">Smith, John</contributorName>"
                + "<givenName>John</givenName><familyName>Smith</familyName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Personal\">John Smith</contributorName>"
                + "<givenName>John</givenName><familyName>Smith</familyName></contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName>pete@malinator.com</contributorName>"
                + "</contributor>"
                + "</contributors>",
                stringWriter.toString());
    }
    
    /**
     * Test: 7, Contributor ((with optional given name, family name, name
     * identifier and affiliation sub-properties)
     * 
     * nameType="Organizational"
     *
     * contributors
     */
    @Test
    public void testWriteContributorsElementWithOrganizations() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-organizations.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeContributorsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<contributors>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">IBM</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">Harvard University</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">The Institute for Quantitative Social Science</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">The Ford Foundation</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">United Nations Economic and Social Commission for Asia and the Pacific (UNESCAP)</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">Michael J. Fox Foundation for Parkinson's Research</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName>pete@malinator.com</contributorName>"
                + "</contributor>"
                + "</contributors>",
                stringWriter.toString());
    }
    
    /**
     * Test: 7, Contributor ((with optional given name, family name, name
     * identifier and affiliation sub-properties)
     * 
     * nameType="Organizational"
     *
     * contributors
     */
    @Test
    public void testWriteContributorsElementWithOrganizationsAndComma() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-organizations-comma.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeContributorsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        // then
        assertEquals("<contributors>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">Digital Archive of Massachusetts Anti-Slavery and Anti-Segregation Petitions, Massachusetts Archives, Boston MA</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">U.S. Department of Commerce, Bureau of the Census, Geography Division</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">Harvard Map Collection, Harvard College Library</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">Geographic Data Technology, Inc. (GDT)</contributorName>"
                + "</contributor>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName>pete@malinator.com</contributorName>"
                + "</contributor>"
                + "</contributors>",
                stringWriter.toString());
    }

    /**
     * Test: 7, Contributor (with optional given name, family name, name
     * identifier and affiliation sub-properties)
     *
     * contributors
     */
    @Test
    public void testWriteContributorsElementComplete() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeContributorsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        // then
        assertEquals("<contributors>"
                + "<contributor contributorType=\"ContactPerson\">"
                + "<contributorName nameType=\"Organizational\">LastContact1, FirstContact1</contributorName>"
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
                + "<contributorName nameType=\"Personal\">LastProducer1, FirstProducer1</contributorName>"
                + "<givenName>FirstProducer1</givenName><familyName>LastProducer1</familyName>"
                + "<affiliation>ProducerAffiliation1</affiliation>"
                + "</contributor><contributor contributorType=\"Producer\">"
                + "<contributorName nameType=\"Personal\">LastProducer2, FirstProducer2</contributorName>"
                + "<givenName>FirstProducer2</givenName><familyName>LastProducer2</familyName>"
                + "<affiliation>ProducerAffiliation2</affiliation>"
                + "</contributor>"
                + "<contributor contributorType=\"DataCollector\">"
                + "<contributorName nameType=\"Personal\">LastContributor1, FirstContributor1</contributorName>"
                + "<givenName>FirstContributor1</givenName><familyName>LastContributor1</familyName>"
                + "</contributor>"
                + "<contributor contributorType=\"DataCurator\">"
                + "<contributorName nameType=\"Personal\">LastContributor2, FirstContributor2</contributorName>"
                + "<givenName>FirstContributor2</givenName><familyName>LastContributor2</familyName>"
                + "</contributor><contributor contributorType=\"Distributor\">"
                + "<contributorName nameType=\"Personal\">LastDistributor1, FirstDistributor1</contributorName>"
                + "<givenName>FirstDistributor1</givenName><familyName>LastDistributor1</familyName>"
                + "<affiliation>DistributorAffiliation1</affiliation>"
                + "</contributor>"
                + "<contributor contributorType=\"Distributor\">"
                + "<contributorName nameType=\"Personal\">LastDistributor2, FirstDistributor2</contributorName>"
                + "<givenName>FirstDistributor2</givenName><familyName>LastDistributor2</familyName>"
                + "<affiliation>DistributorAffiliation2</affiliation>"
                + "</contributor>"
                + "</contributors>",
                stringWriter.toString());
    }

    /**
     * Test: 8, Date (with type sub-property) (R)
     *
     * dates
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteDatesElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeDatesElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<dates>"
                + "<date dateType=\"Issued\">1004-01-01</date>"
                + "<date dateType=\"Created\">1003-01-01</date>"
                + "<date dateType=\"Submitted\">1002-01-01</date>"
                + "<date dateType=\"Updated\">2015-09-29</date>"
                + "<date dateType=\"Collected\">1006-01-01/1006-01-01</date>"
                + "<date dateType=\"Collected\">1006-02-01/1006-02-02</date>"
                + "</dates>",
                stringWriter.toString());
    }

    /**
     * Test: 9, Language (O)
     *
     * language
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteLanguageElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        String language = OpenAireExportUtil.getLanguage(xmlWriter, dto);

        // when
        OpenAireExportUtil.writeFullElement(xmlWriter, null, "language", null, language, null);
        xmlWriter.flush();

        // then
        assertEquals("<language>it</language>", stringWriter.toString());
    }

    /**
     * Test: 10, ResourceType (with mandatory general type description sub-
     * property) (M)
     *
     * resourceType
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteResourceTypeElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeResourceTypeElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<resourceType resourceTypeGeneral=\"Dataset\">KindOfData1</resourceType>", stringWriter.toString());
    }

    /**
     * Test: 11 AlternateIdentifier (with type sub-property) (O)
     *
     * alternateIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteAlternateIdentifierElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeAlternateIdentifierElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<alternateIdentifiers>"
                + "<alternateIdentifier alternateIdentifierType=\"OtherIDAgency1\">"
                + "OtherIDIdentifier1</alternateIdentifier>"
                + "<alternateIdentifier alternateIdentifierType=\"OtherIDAgency2\">"
                + "OtherIDIdentifier2</alternateIdentifier>"
                + "</alternateIdentifiers>",
                stringWriter.toString());
    }

    /**
     * Test: 12, RelatedIdentifier (with type and relation type sub-properties)
     * (R)
     *
     * relatedIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteRelatedIdentifierElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeRelatedIdentifierElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<relatedIdentifiers>"
                + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"ARK\">"
                + "RelatedPublicationIDNumber1</relatedIdentifier>"
                + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"arXiv\">"
                + "RelatedPublicationIDNumber2</relatedIdentifier>"
                + "</relatedIdentifiers>",
                stringWriter.toString());
    }

    /**
     * Test: 13, Size (O)
     *
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteEmptySizeElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        // set an empty file list
        dto.setFiles(new ArrayList<>());

        // when
        // note: fragment must be enclosed in a fake root element.
        xmlWriter.writeStartElement("root");
        OpenAireExportUtil.writeSizeElement(xmlWriter, dto, null);
        xmlWriter.writeEndElement();
        xmlWriter.flush();

        //then
        assertEquals("<root/>", stringWriter.toString());
    }

    /**
     * Test: 13, Size (O)
     *
     * relatedIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteSizeElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        // note: fragment must be enclosed in a fake root element.
        OpenAireExportUtil.writeSizeElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<sizes>"
                + "<size>1000</size>"
                + "<size>20</size>"
                + "</sizes>",
                stringWriter.toString());
    }

    /**
     * Test: 14, Format (O)
     *
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteEmptyFormatElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        // set an empty file list
        dto.setFiles(new ArrayList<>());

        // when
        // note: fragment must be enclosed in a fake root element.
        xmlWriter.writeStartElement("root");
        OpenAireExportUtil.writeFormatElement(xmlWriter, dto, null);
        xmlWriter.writeEndElement();
        xmlWriter.flush();

        //then
        assertEquals("<root/>", stringWriter.toString());
    }

    /**
     * Test: 14, Format (O)
     *
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteFormatElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeFormatElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<formats>"
                + "<format>application/pdf</format>"
                + "<format>application/xml</format>"
                + "</formats>",
                stringWriter.toString());
    }

    /**
     * Test: 15, Version (O)
     *
     * version
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteEmptyVersionElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        // note: fragment must be enclosed in a fake root element.
        xmlWriter.writeStartElement("root");
        OpenAireExportUtil.writeVersionElement(xmlWriter, dto, null);
        xmlWriter.writeEndElement();
        xmlWriter.flush();

        //then
        assertEquals("<root><version>1.0</version></root>", stringWriter.toString());
    }

    /**
     * Test: 15, Version (O)
     *
     * version
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteVersionElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        dto.setVersionNumber(2L);
        dto.setMinorVersionNumber(1L);

        // when
        OpenAireExportUtil.writeVersionElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<version>2.1</version>", stringWriter.toString());
    }

    /**
     * Test: 16 Rights (O)
     *
     * rights
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteAccessRightElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeAccessRightsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<rightsList>"
                + "<rights rightsURI=\"info:eu-repo/semantics/closedAccess\"/>"
                + "<rights rightsURI=\"http://creativecommons.org/publicdomain/zero/1.0/\">"
                + "CC0 1.0</rights></rightsList>",
                stringWriter.toString());
    }

    /**
     * Test: 16 Rights (O)
     *
     * rights
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteRestrictedAccessRightElementWithRequestAccessEnabled() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        dto.setLicense(null);
        dto.setTermsOfUse(null);
        dto.setFileAccessRequest(true);

        // when
        OpenAireExportUtil.writeAccessRightsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<rightsList>"
                + "<rights rightsURI=\"info:eu-repo/semantics/restrictedAccess\"/>"
                + "<rights/></rightsList>",
                stringWriter.toString());
    }

    /**
     * Test: 16 Rights (O)
     *
     * rights
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteRestrictedAccessRightElementWithRequestAccessDisabled() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        dto.setLicense(null);
        dto.setTermsOfUse(null);
        dto.setFileAccessRequest(false);

        // when
        OpenAireExportUtil.writeAccessRightsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<rightsList>"
                + "<rights rightsURI=\"info:eu-repo/semantics/closedAccess\"/>"
                + "<rights/></rightsList>",
                stringWriter.toString());
    }

    /**
     * Test: 17, Description (R)
     *
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteDescriptionsElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeDescriptionsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<descriptions>"
                + "<description descriptionType=\"Abstract\">DescriptionText 1"
                + "</description>"
                + "<description descriptionType=\"Abstract\">DescriptionText2"
                + "</description>"
                + "<description descriptionType=\"TechnicalInfo\">SoftwareName1, SoftwareVersion1"
                + "</description>"
                + "<description descriptionType=\"TechnicalInfo\">SoftwareName2, SoftwareVersion2"
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
                stringWriter.toString());
    }

    /**
     * Test: 18, GeoLocation (with point, box and polygon sub-properties) (R)
     *
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteGeoLocationElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeGeoLocationsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<geoLocations>"
                + "<geoLocation>"
                + "<geoLocationPlace>ProductionPlace</geoLocationPlace></geoLocation>"
                + "<geoLocation>"
                + "<geoLocationBox>"
                + "<westBoundLongitude>10</westBoundLongitude>"
                + "<eastBoundLongitude>20</eastBoundLongitude>"
                + "<southBoundLatitude>40</southBoundLatitude>"
                + "<northBoundLatitude>30</northBoundLatitude>"
                + "</geoLocationBox>"
                + "</geoLocation>"
                + "<geoLocation>"
                + "<geoLocationBox>"
                + "<eastBoundLongitude>60</eastBoundLongitude>"
                + "<southBoundLatitude>80</southBoundLatitude>"
                + "<northBoundLatitude>70</northBoundLatitude>"
                + "<westBoundLongitude>50</westBoundLongitude>"
                + "</geoLocationBox>"
                + "</geoLocation></geoLocations>",
                stringWriter.toString());
    }

    /**
     * Test: 18, GeoLocation (with point, box and polygon sub-properties) (R)
     *
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteGeoLocationElement2() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-simplified.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeGeoLocationsElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<geoLocations>"
                + "<geoLocation>"
                + "<geoLocationBox>"
                + "<eastBoundLongitude>23</eastBoundLongitude>"
                + "<northBoundLatitude>786</northBoundLatitude>"
                + "<southBoundLatitude>34</southBoundLatitude>"
                + "<westBoundLongitude>45</westBoundLongitude>"
                + "</geoLocationBox>"
                + "</geoLocation></geoLocations>",
                stringWriter.toString());
    }

    /**
     * Test: 19, FundingReference (with name, identifier, and award related sub-
     * properties) (O)
     *
     * fundingReference
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    @Test
    public void testWriteFundingReferencesElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-all-defaults.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeFundingReferencesElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<fundingReferences><fundingReference>"
                + "<funderName>GrantInformationGrantAgency1</funderName>"
                + "<awardNumber>GrantInformationGrantNumber1</awardNumber>"
                + "</fundingReference>"
                + "<fundingReference>"
                + "<funderName>GrantInformationGrantAgency2</funderName>"
                + "<awardNumber>GrantInformationGrantNumber2</awardNumber>"
                + "</fundingReference></fundingReferences>",
                stringWriter.toString());
    }

    /**
     * Test 19.1, funderName Name of the funding provider.
     *
     * funderName
     */
    @Test
    public void testWriteFunderNamePropertyNotInContributor() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-updated.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        // note: fragment must be enclosed in a fake root element.
        xmlWriter.writeStartElement("root");
        OpenAireExportUtil.writeContributorsElement(xmlWriter, dto, null);
        xmlWriter.writeEndElement();
        xmlWriter.flush();

        //then
        assertEquals("<root/>", stringWriter.toString());
    }

    /**
     * Test 19.1, funderName Name of the funding provider.
     *
     * funderName
     */
    @Test
    public void testWriteFunderNamePropertyInFundingReferencesElement() throws XMLStreamException, IOException {
        // given
        DatasetDTO datasetDto = mapObjectFromJsonTestFile("export/dataset-updated.txt", DatasetDTO.class);
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        // when
        OpenAireExportUtil.writeFundingReferencesElement(xmlWriter, dto, null);
        xmlWriter.flush();

        //then
        assertEquals("<fundingReferences>"
                + "<fundingReference><funderName>Dennis</funderName></fundingReference>"
                + "<fundingReference><funderName>NIH</funderName><awardNumber>NIH1231245154</awardNumber></fundingReference>"
                + "<fundingReference><funderName>NIH</funderName><awardNumber>NIH99999999</awardNumber></fundingReference>"
                + "</fundingReferences>",
                stringWriter.toString());
    }

    // private static final Jsonb jsonb = JsonbBuilder.create();
    private static final Gson gson = new Gson();

    public static <T> T mapObjectFromJsonTestFile(String subPath, Class<T> klass) throws IOException {
        Path file = Path.of("src/test/java/edu/harvard/iq/dataverse", subPath);
        String json = Files.readString(file, StandardCharsets.UTF_8);
        // Jakarta JSON-B is no capable to map many of the DTO fields... :-( Needs Gson for now.
        // return jsonb.fromJson(json, klass);
        return gson.fromJson(json, klass);
    }
}
