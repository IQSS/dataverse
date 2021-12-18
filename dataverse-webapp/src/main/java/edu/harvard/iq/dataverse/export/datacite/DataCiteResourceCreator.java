package edu.harvard.iq.dataverse.export.datacite;

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
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFundingReference;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DataCiteResourceCreator {

    // -------------------- LOGIC --------------------

    public DataCiteResource create(String identifier, String publicationYear, DvObject dvObject) {
        Dataset dataset = getDataset(dvObject);
        DataCiteResource resource = new DataCiteResource();

        resource.setIdentifier(extractIdentifier(identifier));
        resource.setCreators(extractCreators(dataset));
        resource.setTitles(Collections.singletonList(dvObject.getDisplayName()));

        String publisher = dataset.getLatestVersion().getRootDataverseNameforCitation();
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
                    && !(dataset.getFiles().get(0).getIdentifier() == null)) {
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
            String description = StringEscapeUtils.unescapeHtml4(dataset.getLatestVersion().getDescriptionPlainText());
            return Collections.singletonList(new Description(description));
        } else if (dvObject.isInstanceofDataFile()) {
            String fileDescription = ((DataFile) dvObject).getDescription();
            return Collections.singletonList(new Description(fileDescription != null ? fileDescription : ""));
        }
        return Collections.emptyList();
    }

    private List<Contributor> extractContributors(Dataset dataset) {
        List<Contributor> contributors = new ArrayList<>();
        List<Contributor> contacts = dataset.getLatestVersion().getDatasetContacts().stream()
                .filter(c -> StringUtils.isNotEmpty(c[0]))
                .map(c -> new Contributor(ContributorType.ContactPerson, c[0],
                        StringUtils.isNotEmpty(c[1]) ? new Affiliation(c[1]) : null))
                .collect(Collectors.toList());
        List<Contributor> producers = dataset.getLatestVersion().getDatasetProducers().stream()
                .filter(p -> StringUtils.isNotEmpty(p[0]))
                .map(p -> new Contributor(ContributorType.Producer, p[0],
                        StringUtils.isNotEmpty(p[1]) ? new Affiliation(p[1]) : null))
                .collect(Collectors.toList());
        if (!contacts.isEmpty() || !producers.isEmpty()) {
            contributors.addAll(contacts);
            contributors.addAll(producers);
        }
        return contributors;
    }

    private List<FundingReference> extractFundingReferences(Dataset dataset) {
        List<DatasetFundingReference> fundingReferencesFromDataset = dataset.getLatestVersion().getFundingReferences();
        return fundingReferencesFromDataset.stream()
                .filter(r -> r.getAgency() != null && !r.getAgency().getValue().isEmpty())
                .map(this::extractFundingReference)
                .collect(Collectors.toList());
    }

    private FundingReference extractFundingReference(DatasetFundingReference reference) {
        FundingReference fundingReference = new FundingReference();
        fundingReference.setFunderName(reference.getAgency().getValue());
        if (reference.getAgencyIdentifier() != null && !reference.getAgencyIdentifier().isEmpty()) {
            fundingReference.setFunderIdentifier(
                    new FunderIdentifier(reference.getAgencyIdentifier().getValue()));
        }
        if (reference.getProgramIdentifier() != null && !reference.getProgramIdentifier().isEmpty()) {
            fundingReference.setAwardNumber(reference.getProgramIdentifier().getValue());
        }
        return fundingReference;
    }
}
