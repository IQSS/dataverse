package edu.harvard.iq.dataverse.export.openaire;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author francesco.cadili@4science.it
 */
public class OpenAireExportUtilTest {

    // -------------------- TESTS --------------------

    /**
     * Test: 1A, Identifier (with mandatory type sub-property) (M)
     * <p>
     * identifier
     */
    @Test
    public void testWriteIdentifierElement_doi() throws XMLStreamException, IOException {
        Writer writer = new Writer();

        String persistentAgency = "doi";
        String persistentAuthority = "10.123";
        String persistentId = "123";
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId);
        OpenAireExportUtil.writeIdentifierElement(writer.xml, globalId.toURL().toString(), null);
        writer.close();
        Assert.assertEquals("<identifier identifierType=\"DOI\">"
                                    + persistentAuthority + "/" + persistentId + "</identifier>",
                            writer.toString());
    }

    /**
     * Test: 1B, Identifier (with mandatory type sub-property) (M)
     * <p>
     * identifier
     */
    @Test
    public void testWriteIdentifierElement_handle() throws XMLStreamException, IOException {
        Writer writer = new Writer();

        String persistentAgency = "hdl";
        String persistentAuthority = "1902.1";
        String persistentId = "111012";
        GlobalId globalId = new GlobalId(persistentAgency, persistentAuthority, persistentId);
        OpenAireExportUtil.writeIdentifierElement(writer.xml, globalId.toURL().toString(), null);
        writer.close();
        Assert.assertEquals("<identifier identifierType=\"Handle\">"
                                    + persistentAuthority + "/" + persistentId + "</identifier>",
                            writer.toString());
    }

    /**
     * Test: 2, Creator (with optional given name, family name, name identifier
     * and affiliation sub-properties) (M)
     * <p>
     * creators
     */
    @Test
    public void testWriteCreatorsElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();
        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-simplified.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeCreatorsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<creators>"
                        + "<creator>"
                        + "<creatorName>Privileged, Pete</creatorName>"
                        + "<nameIdentifier nameIdentifierScheme=\"ORCID\">ellenid</nameIdentifier>"
                        + "<affiliation>Top</affiliation>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>Smith, John</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>John Smith</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>Awesome, Audrey</creatorName>"
                        + "<nameIdentifier nameIdentifierScheme=\"DAISY\">audreyId</nameIdentifier>"
                        + "<affiliation>Bottom</affiliation>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>Apache Foundation</creatorName>"
                        + "<nameIdentifier nameIdentifierScheme=\"DAISY\">audreyId</nameIdentifier>"
                        + "<affiliation>Bottom</affiliation>"
                        + "</creator>"
                        + "</creators>",
                            writer.toString());
    }

    /**
     * Test: 2, Creator (with optional given name, family name, name identifier
     * and affiliation sub-properties) (M)
     * <p>
     * nameType="Organizational"
     * <p>
     * creators
     */
    @Test
    public void testWriteCreatorsElementWithOrganizations() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();
        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-organizations.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeCreatorsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<creators>"
                        + "<creator>"
                        + "<creatorName>IBM</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>Harvard University</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>The Institute for Quantitative Social Science</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>The Ford Foundation</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>United Nations Economic and Social Commission for Asia and the Pacific (UNESCAP)</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>Michael J. Fox Foundation for Parkinson's Research</creatorName>"
                        + "</creator>"
                        + "</creators>",
                            writer.toString());
    }

    /**
     * Test: 2, Creator (with optional given name, family name, name identifier
     * and affiliation sub-properties) (M)
     * <p>
     * nameType="Organizational"
     * <p>
     * creators
     */
    @Test
    public void testWriteCreatorsElementWithOrganizationsAndComma()
            throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-organizations-comma.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeCreatorsElement(writer.xml, dto, null);
        writer.close();
        Assert.assertEquals("<creators>"
                        + "<creator>"
                        + "<creatorName>Digital Archive of Massachusetts Anti-Slavery and Anti-Segregation Petitions, Massachusetts Archives, Boston MA</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>U.S. Department of Commerce, Bureau of the Census, Geography Division</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>Harvard Map Collection, Harvard College Library</creatorName>"
                        + "</creator>"
                        + "<creator>"
                        + "<creatorName>Geographic Data Technology, Inc. (GDT)</creatorName>"
                        + "</creator>"
                        + "</creators>",
                            writer.toString());
    }

    /**
     * Test: 3, Title (with optional type sub-properties) (M)
     * <p>
     * titles
     */
    @Test
    public void testWriteTitleElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-simplified.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeTitlesElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<titles><title>My Dataset</title></titles>",
                            writer.toString());
    }

    /**
     * Test: 4, Publisher (M)
     * <p>
     * publisher
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWritePublisherElement() throws XMLStreamException, IOException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = new DatasetDTO();
        datasetDto.setPublisher("Publisher01");
        String publisher = datasetDto.getPublisher();
        OpenAireExportUtil.writeFullElement(writer.xml, null, "publisher", null, publisher, null);

        writer.close();
        Assert.assertEquals("<publisher>Publisher01</publisher>",
                            writer.toString());
    }

    /**
     * Test: 5, PublicationYear (M)
     * <p>
     * publicationYear
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWritePublicationYearElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-simplified.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writePublicationYearElement(writer.xml, dto, null, null);

        writer.close();
        Assert.assertEquals("<publicationYear>2014</publicationYear>",
                            writer.toString());
    }

    /**
     * Test: 6, Subject (with scheme sub-property) R
     * <p>
     * subjects
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testSubjectsElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeSubjectsElement(writer.xml, dto, null);
        writer.close();
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
                            writer.toString());
    }

    /**
     * Test: 7, Contributor (with optional given name, family name, name
     * identifier and affiliation sub-properties)
     * <p>
     * contributors
     */
    @Test
    public void testWriteContributorsElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-simplified.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeContributorsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<contributors>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>Smith, John</contributorName>"
                        + "</contributor>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>John Smith</contributorName>"
                        + "</contributor>"
                        + "</contributors>",
                            writer.toString());
    }

    /**
     * Test: 7, Contributor ((with optional given name, family name, name
     * identifier and affiliation sub-properties)
     * <p>
     * nameType="Organizational"
     * <p>
     * contributors
     */
    @Test
    public void testWriteContributorsElementWithOrganizations()
            throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-organizations.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeContributorsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<contributors>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>IBM</contributorName>"
                        + "</contributor>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>Harvard University</contributorName>"
                        + "</contributor>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>The Institute for Quantitative Social Science</contributorName>"
                        + "</contributor>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>The Ford Foundation</contributorName>"
                        + "</contributor>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>United Nations Economic and Social Commission for Asia and the Pacific (UNESCAP)</contributorName>"
                        + "</contributor>"
                        + "<contributor contributorType=\"ContactPerson\">"
                        + "<contributorName>Michael J. Fox Foundation for Parkinson's Research</contributorName>"
                        + "</contributor>"
                        + "</contributors>",
                            writer.toString());
    }

    /**
     * Test: 7, Contributor ((with optional given name, family name, name
     * identifier and affiliation sub-properties)
     * <p>
     * nameType="Organizational"
     * <p>
     * contributors
     */
    @Test
    public void testWriteContributorsElementWithOrganizationsAndComma()
            throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-organizations-comma.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeContributorsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<contributors><contributor " +
                        "contributorType=\"ContactPerson\">" +
                        "<contributorName>Smith, John</contributorName>" +
                        "</contributor>" +
                        "<contributor contributorType=\"ContactPerson\"><contributorName>John Smith</contributorName>" +
                        "</contributor>" +
                        "</contributors>",
                            writer.toString());
    }

    /**
     * Test: 7, Contributor (with optional given name, family name, name
     * identifier and affiliation sub-properties)
     * <p>
     * contributors
     */
    @Test
    public void testWriteContributorsElementComplete()
            throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeContributorsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<contributors>" +
                        "<contributor contributorType=\"ContactPerson\">" +
                        "<contributorName>LastContact1, FirstContact1</contributorName>" +
                        "<affiliation>ContactAffiliation1</affiliation>" +
                        "</contributor>" +
                        "<contributor contributorType=\"ContactPerson\">" +
                        "<contributorName>Condon, Kevin</contributorName>" +
                        "<affiliation>ContactAffiliation2</affiliation>" +
                        "</contributor>" +
                        "<contributor contributorType=\"ContactPerson\">" +
                        "<contributorName>Philip Durbin</contributorName>" +
                        "<affiliation>ContactAffiliation3</affiliation>" +
                        "</contributor>" +
                        "<contributor contributorType=\"Producer\">" +
                        "<contributorName>LastProducer1, FirstProducer1</contributorName>" +
                        "<affiliation>ProducerAffiliation1</affiliation>" +
                        "</contributor>" +
                        "<contributor contributorType=\"Producer\">" +
                        "<contributorName>LastProducer2, FirstProducer2</contributorName>" +
                        "<affiliation>ProducerAffiliation2</affiliation>" +
                        "</contributor>" +
                        "<contributor contributorType=\"DataCollector\">" +
                        "<contributorName>LastContributor1, FirstContributor1</contributorName>" +
                        "</contributor>" +
                        "<contributor contributorType=\"DataCurator\">" +
                        "<contributorName>LastContributor2, FirstContributor2</contributorName>" +
                        "</contributor>" +
                        "<contributor contributorType=\"Funder\">" +
                        "<contributorName>GrantInformationGrantAgency1</contributorName>" +
                        "<nameIdentifier nameIdentifierScheme=\"info\">info:eu-repo/grantAgreement/grantNumberAgencyShortName1/grantNumberProgram1/GrantInformationGrantNumber1</nameIdentifier>" +
                        "</contributor>" +
                        "<contributor contributorType=\"Distributor\">" +
                        "<contributorName>LastDistributor1, FirstDistributor1</contributorName>" +
                        "<affiliation>DistributorAffiliation1</affiliation>" +
                        "</contributor>" +
                        "<contributor contributorType=\"Distributor\">" +
                        "<contributorName>LastDistributor2, FirstDistributor2</contributorName>" +
                        "<affiliation>DistributorAffiliation2</affiliation>" +
                        "</contributor>" +
                        "</contributors>",
                writer.toString());
    }

    /**
     * Test: 8, Date (with type sub-property) (R)
     * <p>
     * dates
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteDatesElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        OpenAireExportUtil.writeDatesElement(writer.xml, datasetDto, null);

        writer.close();
        Assert.assertEquals("<dates>"
                                    + "<date dateType=\"Issued\">1004-01-01</date>"
                                    + "<date dateType=\"Created\">1003-01-01</date>"
                                    + "<date dateType=\"Submitted\">1002-01-01</date>"
                                    + "<date dateType=\"Updated\">2015-09-29</date>"
                                    + "<date dateType=\"Collected\">1006-01-01/1006-01-01</date>"
                                    + "<date dateType=\"Collected\">1006-02-01/1006-02-02</date>"
                                    + "</dates>",
                            writer.toString());
    }

    @Test
    public void testWriteDatesElement_withEmbargo() throws XMLStreamException, IOException, URISyntaxException {
        // given
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        datasetDto.setEmbargoDate("2020-01-01");
        datasetDto.setPublicationDate("2019-01-01");
        datasetDto.setEmbargoActive(true);

        // when
        OpenAireExportUtil.writeDatesElement(writer.xml, datasetDto, null);
        writer.close();

        // then
        Assert.assertEquals("<dates>"
                                    + "<date dateType=\"Accepted\">2019-01-01</date>"
                                    + "<date dateType=\"Available\">2020-01-01</date>"
                                    + "</dates>",
                            writer.toString());
    }

    /**
     * Test: 9, Language (O)
     * <p>
     * language
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteLanguageElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        String language = OpenAireExportUtil.getLanguage(writer.xml, dto);
        OpenAireExportUtil.writeFullElement(writer.xml, null, "language", null, language, null);

        writer.close();
        Assert.assertEquals("<language>it</language>",
                            writer.toString());
    }

    /**
     * Test: 10, ResourceType (with mandatory general type description sub-
     * property) (M)
     * <p>
     * resourceType
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteResourceTypeElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeResourceTypeElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<resourceType resourceTypeGeneral=\"Dataset\">"
                                    + "KindOfData1</resourceType>",
                            writer.toString());
    }

    /**
     * Test: 11 AlternateIdentifier (with type sub-property) (O)
     * <p>
     * alternateIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteAlternateIdentifierElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();

        OpenAireExportUtil.writeAlternateIdentifierElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<alternateIdentifiers>"
                                    + "<alternateIdentifier alternateIdentifierType=\"OtherIDAgency1\">"
                                    + "OtherIDIdentifier1</alternateIdentifier>"
                                    + "<alternateIdentifier alternateIdentifierType=\"OtherIDAgency2\">"
                                    + "OtherIDIdentifier2</alternateIdentifier>"
                                    + "</alternateIdentifiers>",
                            writer.toString());
    }

    /**
     * Test: 12, RelatedIdentifier (with type and relation type sub-properties)
     * (R)
     * <p>
     * relatedIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteRelatedIdentifierElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeRelatedIdentifierElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<relatedIdentifiers>"
                                    + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"ARK\">"
                                    + "RelatedPublicationIDNumber1</relatedIdentifier>"
                                    + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"arXiv\">"
                                    + "RelatedPublicationIDNumber2</relatedIdentifier>"
                                    + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"ARK\">RelatedMaterialIDNumber1</relatedIdentifier>"
                                    + "<relatedIdentifier relationType=\"IsCitedBy\" relatedIdentifierType=\"ARK\">RelatedDatasetIDNumber1</relatedIdentifier>"
                                    + "</relatedIdentifiers>",
                            writer.toString());
    }

    /**
     * Test: 13, Size (O)
     * <p>
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteEmptySizeElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // set an empty file list
            dto.setFiles(new ArrayList<FileDTO>());

            // Fragment must be enclosed in a fake root element.
            writer.xml.writeStartElement("root");
            OpenAireExportUtil.writeSizeElement(writer.xml, dto, null);

            writer.xml.writeEndElement();
        }
        writer.close();
        Assert.assertEquals("<root />",
                            writer.toString());
    }

    /**
     * Test: 13, Size (O)
     * <p>
     * relatedIdentifier
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteSizeElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeSizeElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<sizes>"
                                    + "<size>1000</size>"
                                    + "<size>20</size>"
                                    + "</sizes>",
                            writer.toString());
    }

    /**
     * Test: 14, Format (O)
     * <p>
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteEmptyFormatElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // set an empty file list
            dto.setFiles(new ArrayList<FileDTO>());

            // Fragment must be enclosed in a fake root element.
            writer.xml.writeStartElement("root");
            OpenAireExportUtil.writeFormatElement(writer.xml, dto, null);

            writer.xml.writeEndElement();
        }

        writer.close();
        Assert.assertEquals("<root />",
                            writer.toString());
    }

    /**
     * Test: 14, Format (O)
     * <p>
     * size
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteFormatElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeFormatElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<formats>"
                                    + "<format>application/pdf</format>"
                                    + "<format>application/xml</format>"
                                    + "</formats>",
                            writer.toString());
    }

    /**
     * Test: 15, Version (O)
     * <p>
     * version
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteEmptyVersionElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // Fragment must be enclosed in a fake root element.
            writer.xml.writeStartElement("root");
            OpenAireExportUtil.writeVersionElement(writer.xml, dto, null);

            writer.xml.writeEndElement();
        }

        writer.close();
        Assert.assertEquals("<root><version>1.0</version></root>",
                            writer.toString());
    }

    /**
     * Test: 15, Version (O)
     * <p>
     * version
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteVersionElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            dto.setVersionNumber(2L);
            dto.setVersionMinorNumber(1L);
        }
        OpenAireExportUtil.writeVersionElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<version>2.1</version>",
                            writer.toString());
    }

    /**
     * Test: 16 Rights (O)
     * <p>
     * rights
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteAccessRightElement_openAccess_sameLicenses() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        datasetDto.setHasActiveGuestbook(false);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/openAccess\" />"
                                    + "<rights rightsURI=\"https://creativecommons.org/publicdomain/zero/1.0/legalcode\">"
                                    + "CC0 Creative Commons Zero 1.0 Waiver</rights></rightsList>",
                            writer.toString());
    }

    /**
     * Test: 17, Description (R)
     * <p>
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteDescriptionsElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeDescriptionsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<descriptions>"
                                    + "<description descriptionType=\"Abstract\">DescriptionText 1"
                                    + "</description>"
                                    + "<description descriptionType=\"Abstract\">Description 2  text  with html"
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
                            writer.toString());
    }

    /**
     * Test: 18, GeoLocation (with point, box and polygon sub-properties) (R)
     * <p>
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteGeoLocationElement() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeFullGeoLocationsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<geoLocations>" +
                        "<geoLocation>" +
                        "<geoLocationBox>40 10 30 20</geoLocationBox>" +
                        "<geoLocationPlace>ProductionPlace</geoLocationPlace>" +
                        "</geoLocation>" +
                        "<geoLocation>" +
                        "<geoLocationBox>80 50 70 60</geoLocationBox>" +
                        "<geoLocationPlace>ProductionPlace</geoLocationPlace>" +
                        "</geoLocation>" +
                        "</geoLocations>",
                writer.toString());
    }

    /**
     * Test: 18, GeoLocation (with box) (R)
     * <p>
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteGeoLocationElement2() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-simplified.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeFullGeoLocationsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<geoLocations>"
                        + "<geoLocation>"
                        + "<geoLocationBox>34 45 89 23</geoLocationBox>"
                        + "</geoLocation></geoLocations>",
                writer.toString());
    }

    /**
     * Test: 18, GeoLocation (without box, with place) (R)
     * <p>
     * description
     *
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testWriteGeoLocationElement3() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-updated.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        OpenAireExportUtil.writeFullGeoLocationsElement(writer.xml, dto, null);

        writer.close();
        Assert.assertEquals("<geoLocations>"
                        + "<geoLocation>"
                        + "<geoLocationPlace>Cambridge, UK</geoLocationPlace>"
                        + "</geoLocation></geoLocations>",
                writer.toString());
    }

    /**
     * funderName Name of the funding provider.
     * <p>
     * funderName
     */
    @Test
    public void testWriteFunderNamePropertyInContributor()
            throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-updated.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        {
            // Fragment must be enclosed in a fake root element.
            writer.xml.writeStartElement("root");
            OpenAireExportUtil.writeContributorsElement(writer.xml, dto, null);

            writer.xml.writeEndElement();
        }
        writer.close();
        Assert.assertEquals("<root>" +
                        "<contributors>" +
                        "<contributor contributorType=\"Funder\">" +
                        "<contributorName>Dennis</contributorName>" +
                        "</contributor>" +
                        "</contributors></root>",
                writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_restrictedAccess_withGuestbook() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        datasetDto.setHasActiveGuestbook(true);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/restrictedAccess\" />"
                                    + "<rights rightsURI=\"https://creativecommons.org/publicdomain/zero/1.0/legalcode\">"
                                    + "CC0 Creative Commons Zero 1.0 Waiver</rights></rightsList>",
                            writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_restrictedAccess_withoutGuestbook() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-with-one-restrictedFile.txt");
        datasetDto.setHasActiveGuestbook(false);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/restrictedAccess\" />"
                                    + "<rights>Different licenses and/or terms apply to individual files in the dataset. Access to some files in the dataset is restricted.</rights></rightsList>",
                            writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_restrictedAccess_allFilesRestricted() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-with-all-restrictedFiles.txt");
        datasetDto.setHasActiveGuestbook(false);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/restrictedAccess\" />"
                                    + "<rights>Access to all files in the dataset is restricted.</rights></rightsList>",
                            writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_openAccess_allFilesAllRightsReserved() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson(
                "txt/export/openaire/dataset-with-all-allRightsReservedFiles.txt");
        datasetDto.setHasActiveGuestbook(false);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/openAccess\" />"
                                    + "<rights>All rights reserved.</rights></rightsList>",
                            writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_openAccess_differentLicenses() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        dto.getFiles().get(1).setLicenseName("\"Apache Software License 2.0\"");
        dto.getFiles().get(1).setLicenseUrl("https://www.apache.org/licenses/LICENSE-2.0");
        datasetDto.setHasActiveGuestbook(false);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/openAccess\" />"
                                    + "<rights>Different licenses and/or terms apply to individual files in the dataset.</rights></rightsList>",
                            writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_openAccess_licenseAndAllRightsReserved() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        dto.getFiles().get(1).setTermsOfUseType(FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED.toString());
        dto.getFiles().get(1).setLicenseName("");
        dto.getFiles().get(1).setLicenseUrl("");
        datasetDto.setHasActiveGuestbook(false);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/openAccess\" />"
                                    + "<rights>Different licenses and/or terms apply to individual files in the dataset.</rights></rightsList>",
                            writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_restrictedAccess_withRestrictedFile() throws XMLStreamException, IOException, URISyntaxException {
        Writer writer = new Writer();

        DatasetDTO datasetDto = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        DatasetVersionDTO dto = datasetDto.getDatasetVersion();
        dto.getFiles().get(1).setTermsOfUseType(FileTermsOfUse.TermsOfUseType.RESTRICTED.toString());
        dto.getFiles().get(1).setLicenseName("");
        dto.getFiles().get(1).setLicenseUrl("");
        datasetDto.setHasActiveGuestbook(false);
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, datasetDto);
        writer.close();
        Assert.assertEquals("<rightsList>"
                                    + "<rights rightsURI=\"info:eu-repo/semantics/restrictedAccess\" />"
                                    + "<rights>Different licenses and/or terms apply to individual files in the dataset. Access to some files in the dataset is restricted.</rights></rightsList>",
                            writer.toString());
    }

    @Test
    public void testWriteAccessRightElement_withEmbargo() throws XMLStreamException, IOException, URISyntaxException {
        // given
        Writer writer = new Writer();

        DatasetDTO dataset = createDatasetDTOFromJson("txt/export/openaire/dataset-all-defaults.txt");
        dataset.setEmbargoActive(true);

        // when
        OpenAireExportUtil.writeAccessRightsElement(writer.xml, dataset);
        writer.close();

        // then
        Assert.assertEquals("<rightsList>" +
                                    "<rights rightsURI=\"info:eu-repo/semantics/embargoedAccess\" />" +
                                    "</rightsList>",
                            writer.toString());
    }

    // -------------------- PRIVATE ---------------------

    private DatasetDTO createDatasetDTOFromJson(String filePath) throws URISyntaxException, IOException {
        File file = new File(Paths.get(getClass().getClassLoader()
                                               .getResource(filePath).toURI()).toUri());
        String text = FileUtils.readFileToString(file, Charset.defaultCharset());
        Gson gson = new Gson();
        return gson.fromJson(text, DatasetDTO.class);
    }

    // -------------------- INNER CLASSES --------------------

    private static class Writer {
        private final StringWriter string;
        public final XMLStreamWriter xml;

        public Writer() throws XMLStreamException {
            this.string = new StringWriter();
            this.xml = XMLOutputFactory.newInstance()
                    .createXMLStreamWriter(string);
        }

        public void close() throws XMLStreamException, IOException {
            string.close();
            xml.close();
        }

        @Override
        public String toString() {
            return string.toString();
        }
    }
}
