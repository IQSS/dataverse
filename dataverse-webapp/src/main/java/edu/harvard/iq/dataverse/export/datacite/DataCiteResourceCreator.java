package edu.harvard.iq.dataverse.export.datacite;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Affiliation;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Contributor;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.ContributorType;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Creator;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Description;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.FunderIdentifier;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.FundingReference;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Identifier;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.NameIdentifier;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.RelatedIdentifier;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetAuthor;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.Tuple;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

public class DataCiteResourceCreator {

    // -------------------- LOGIC --------------------

    public DataCiteResource create(String identifier, String publicationYear, DvObject dvObject) {
        Dataset dataset = getDataset(dvObject);
        DataCiteResource resource = new DataCiteResource();

        resource.setIdentifier(extractIdentifier(identifier));
        resource.setCreators(extractCreators(dataset));
        resource.setTitles(Collections.singletonList(dvObject.getDisplayName()));

        String publisher = dataset.getLatestVersion().getRootDataverseNameForCitation();
        resource.setPublisher(StringUtils.isEmpty(publisher) ? ":unav" : publisher);

        // Can't use "UNKNOWN" here because DataCite will respond with "[facet 'pattern']
        // the value 'unknown' is not accepted by the pattern '[\d]{4}'"
        resource.setPublicationYear(StringUtils.isEmpty(publicationYear) ? "9999" : publicationYear);
        resource.setRelatedIdentifiers(extractRelatedIdentifiers(dvObject));

        resource.setDescriptions(extractDescription(dvObject));
        resource.setContributors(extractContributors(dataset));
        resource.setFundingReferences(extractFundingReferences(dataset));

        return resource;
    }

    // -------------------- PRIVATE --------------------

    private Dataset getDataset(DvObject dvObject) {
        return (Dataset) (dvObject.isInstanceofDataset() ? dvObject : dvObject.getOwner());
    }

    private Identifier extractIdentifier(String identifier) {
        return new Identifier(identifier != null
                ? identifier.substring(identifier.indexOf(":") + 1).trim()
                : StringUtils.EMPTY);
    }

    private List<Creator> extractCreators(Dataset dataset) {
        List<DatasetAuthor> authors = dataset.getLatestVersion().getDatasetAuthors();
        return authors.stream()
                .map(this::extractCreator)
                .collect(Collectors.toList());
    }

    private Creator extractCreator(DatasetAuthor author) {
        Creator creator = new Creator(author.getName().getDisplayValue());
        if (StringUtils.isNotBlank(author.getIdType())
                && StringUtils.isNotBlank(author.getIdValue())
                && author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {
            NameIdentifier nameIdentifier;
            switch (author.getIdType()) {
                case "ORCID":
                    nameIdentifier = new NameIdentifier(author.getIdValue(), "https://orcid.org/", "ORICD");
                    break;
                case "ISNI":
                    nameIdentifier = new NameIdentifier(author.getIdValue(), "http://isni.org/isni/", "ISNI");
                    break;
                case "LCNA":
                    nameIdentifier = new NameIdentifier(author.getIdValue(), "http://id.loc.gov/authorities/names/", "LCNA");
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized id type: " + author.getIdType());
            }
            creator.setNameIdentifier(nameIdentifier);
        }
        if (isNotEmpty(author.getAffiliation())) {
            Affiliation affiliation = new Affiliation(author.getAffiliation().getFieldValue().get());
            if (isNotEmpty(author.getAffiliationIdentifier())) {
                affiliation.setAffiliationIdentifier(author.getAffiliationIdentifier().getFieldValue().get());
                affiliation.setAffiliationIdentifierScheme("ROR");
            }
            creator.setAffiliation(affiliation);
        }
        return creator;
    }

    private boolean isNotEmpty(DatasetField field) {
        return field != null && !field.getFieldValue().isEmpty();
    }

    private List<RelatedIdentifier> extractRelatedIdentifiers(DvObject dvObject) {
        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;
            if (!dataset.hasActiveEmbargo() && !dataset.getFiles().isEmpty()
                    && dataset.getFiles().get(0).getIdentifier() != null) {
                return dataset.getFiles().stream()
                        .filter(f -> StringUtils.isNotEmpty(f.getGlobalId().asString()))
                        .map(f -> new RelatedIdentifier(f.getGlobalId().asString(),  "HasPart"))
                        .collect(Collectors.toList());
            }
        } else if (dvObject.isInstanceofDataFile()) {
            DataFile dataFile = (DataFile) dvObject;
            return Collections.singletonList(
                    new RelatedIdentifier(dataFile.getOwner().getGlobalId().asString(), "IsPartOf"));
        }
        return Collections.emptyList();
    }

    private List<Description> extractDescription(DvObject dvObject) {
        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = getDataset(dvObject);
            String description = unescapeHtml4(dataset.getLatestVersion().getDescriptionPlainText());
            return Collections.singletonList(new Description(description));
        } else if (dvObject.isInstanceofDataFile()) {
            String fileDescription = unescapeHtml4(((DataFile) dvObject).getDescription());
            return Collections.singletonList(new Description(fileDescription != null ? fileDescription : ""));
        }
        return Collections.emptyList();
    }

    private List<Contributor> extractContributors(Dataset dataset) {
        List<Contributor> contributors = new ArrayList<>();
        contributors.addAll(extractDatasetContactsAsContributors(dataset));
        contributors.addAll(extractDatasetProducersAsContributors(dataset));
        return contributors;
    }

    private List<Contributor> extractDatasetContactsAsContributors(Dataset dataset) {
        return dataset.getLatestVersion().extractSubfields(DatasetFieldConstant.datasetContact,
                Arrays.asList(DatasetFieldConstant.datasetContactName, DatasetFieldConstant.datasetContactAffiliation))
                .stream()
                .filter(e -> {
                    DatasetField contact = e.get(DatasetFieldConstant.datasetContactName);
                    return contact != null && !contact.isEmptyForDisplay() && StringUtils.isNotEmpty(contact.getDisplayValue());
                })
                .map(e -> Tuple.of(e.get(DatasetFieldConstant.datasetContactName), e.get(DatasetFieldConstant.datasetContactAffiliation)))
                .map(t -> Tuple.of(t._1.getDisplayValue(),
                        t._2 != null ? t._2.getFieldValue().getOrElse(StringUtils.EMPTY) : StringUtils.EMPTY))
                .map(t -> new Contributor(ContributorType.ContactPerson, t._1,
                        StringUtils.isNotEmpty(t._2) ? new Affiliation(t._2) : null))
                .collect(Collectors.toList());
    }

    public List<Contributor> extractDatasetProducersAsContributors(Dataset dataset) {
        return dataset.getLatestVersion().extractSubfields(DatasetFieldConstant.producer,
                Arrays.asList(DatasetFieldConstant.producerName, DatasetFieldConstant.producerAffiliation))
                .stream()
                .filter(e -> {
                    DatasetField name = e.get(DatasetFieldConstant.producerName);
                    return name != null && !name.isEmptyForDisplay() && StringUtils.isNotEmpty(name.getDisplayValue());
                })
                .map(e -> Tuple.of(e.get(DatasetFieldConstant.producerName), e.get(DatasetFieldConstant.producerAffiliation)))
                .map(t -> Tuple.of(t._1.getDisplayValue(),
                        t._2 != null ? t._2.getFieldValue().getOrElse(StringUtils.EMPTY) : StringUtils.EMPTY))
                .map(t -> new Contributor(ContributorType.Producer, t._1,
                        StringUtils.isNotEmpty(t._2) ? new Affiliation(t._2) : null))
                .collect(Collectors.toList());
    }

    private List<FundingReference> extractFundingReferences(Dataset dataset) {
        DatasetVersion version = dataset.getLatestVersion();
        return version.extractSubfields(DatasetFieldConstant.grantNumber,
                Arrays.asList(DatasetFieldConstant.grantNumberAgency, DatasetFieldConstant.grantNumberAgencyShortName,
                        DatasetFieldConstant.grantNumberAgencyIdentifier, DatasetFieldConstant.grantNumberProgram,
                        DatasetFieldConstant.grantNumberValue))
                .stream()
                .map(this::extractFundingReference)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private FundingReference extractFundingReference(Map<String, DatasetField> fieldMap) {
        FundingReference fundingReference = new FundingReference();
        DatasetField agencyName = fieldMap.get(DatasetFieldConstant.grantNumberAgency);
        agencyName = agencyName != null ? agencyName : fieldMap.get(DatasetFieldConstant.grantNumberAgencyShortName);
        if (agencyName == null || agencyName.isEmpty()) {
            return null;
        }
        fundingReference.setFunderName(agencyName.getValue());
        DatasetField agencyIdentifier = fieldMap.get(DatasetFieldConstant.grantNumberAgencyIdentifier);
        if (agencyIdentifier != null && !agencyIdentifier.isEmpty()) {
            fundingReference.setFunderIdentifier(new FunderIdentifier(agencyIdentifier.getValue()));
        }
        DatasetField programIdentifier = fieldMap.get(DatasetFieldConstant.grantNumberValue);
        if (programIdentifier != null && !programIdentifier.isEmpty()) {
            fundingReference.setAwardNumber(programIdentifier.getValue());
        }
        return fundingReference;
    }
}
