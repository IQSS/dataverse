package edu.harvard.iq.dataverse.harvest.client;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockWithFieldsDTO;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.globalid.DataCiteFindDoiResponse;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.control.Option;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps Datacite api response to internal dataset representation.
 */
@Stateless
public class DataciteDatasetMapper {

    private static final Set<String> SUPPORTED_IDENTIFIER_TYPES = Sets.newHashSet("ORCID", "ISNI", "LCNA");

    // -------------------- LOGIC --------------------

    public DatasetDTO toDataset(DataCiteFindDoiResponse response) {
        DataCiteFindDoiResponse.Attributes attributes = response.getAttributes();
        DatasetDTO dto = new DatasetDTO();
        dto.setProtocol(GlobalId.DOI_PROTOCOL);
        dto.setAuthority(attributes.getPrefix());
        dto.setIdentifier(attributes.getSuffix());
        dto.setPublisher(attributes.getPublisher());
        dto.setPublicationDate(Option.of(attributes.getPublicationYear()).map(year -> year + "-01-01").getOrNull());
        DatasetVersionDTO datasetVersion = new DatasetVersionDTO();
        dto.setDatasetVersion(datasetVersion);

        MetadataBlockWithFieldsDTO citationBlock = new MetadataBlockWithFieldsDTO();
        citationBlock.setFields(new ArrayList<>());
        HashMap<String, MetadataBlockWithFieldsDTO> metadataBlocks = new HashMap<>();
        datasetVersion.setMetadataBlocks(metadataBlocks);
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED.name());

        metadataBlocks.put("citation", citationBlock);

        addPrimitiveToBlock(citationBlock, DatasetFieldConstant.title, extractFirstTitle(getTitlesWithoutType(attributes)));
        addPrimitiveToBlock(citationBlock, DatasetFieldConstant.alternativeTitle, extractFirstTitle(getTitlesByType(attributes, DataCiteFindDoiResponse.TitleType.AlternativeTitle)));
        addPrimitiveToBlock(citationBlock, DatasetFieldConstant.subTitle, extractFirstTitle(getTitlesByType(attributes, DataCiteFindDoiResponse.TitleType.Subtitle)));

        addCompoundToBlock(citationBlock, DatasetFieldConstant.titleTranslation, map(getTitlesByType(attributes, DataCiteFindDoiResponse.TitleType.TranslatedTitle), this::extractTitleTranslations));
        addCompoundToBlock(citationBlock, DatasetFieldConstant.author, map(attributes.getCreators(), this::extractAuthorFields));
        addCompoundToBlock(citationBlock, DatasetFieldConstant.relatedMaterial, map(attributes.getRelatedIdentifiers(), this::extractRelatedMaterialFields));
        addCompoundToBlock(citationBlock, DatasetFieldConstant.producer, map(getContributorsOfType(attributes, DataCiteFindDoiResponse.ContributorType.Producer), this::extractProducerFields));
        addCompoundToBlock(citationBlock, DatasetFieldConstant.datasetContact, map(getContributorsOfType(attributes, DataCiteFindDoiResponse.ContributorType.ContactPerson), this::extractDatasetContactFields));
        addCompoundToBlock(citationBlock, DatasetFieldConstant.grantNumber, map(attributes.getFundingReferences(), this::extractGrantNumberFields));
        addCompoundToBlock(citationBlock, DatasetFieldConstant.description, map(attributes.getDescriptions(), this::extractDescriptionFields));

        return dto;
    }

    // -------------------- PRIVATE --------------------

    private List<DataCiteFindDoiResponse.Contributor> getContributorsOfType(DataCiteFindDoiResponse.Attributes attributes, DataCiteFindDoiResponse.ContributorType contributorType) {
        return attributes.getContributors().stream()
                .filter(contributor -> contributorType.equals(contributor.getContributorType())).collect(Collectors.toList());
    }

    private String extractFirstTitle(List<DataCiteFindDoiResponse.ResourceTitle> titles) {
        return titles.stream()
                .map(DataCiteFindDoiResponse.ResourceTitle::getTitle)
                .findFirst()
                .orElse(null);
    }

    private List<DataCiteFindDoiResponse.ResourceTitle> getTitlesWithoutType(DataCiteFindDoiResponse.Attributes attributes) {
        if (attributes.getTitles() == null) {
            return Collections.emptyList();
        }

        return attributes.getTitles().stream()
                .filter(title -> title.getTitleType() == null)
                .collect(Collectors.toList());
    }

    private List<DataCiteFindDoiResponse.ResourceTitle> getTitlesByType(DataCiteFindDoiResponse.Attributes attributes, DataCiteFindDoiResponse.TitleType titleType) {
        if (attributes.getTitles() == null) {
            return Collections.emptyList();
        }

        return attributes.getTitles().stream()
                .filter(title -> titleType.equals(title.getTitleType()))
                .collect(Collectors.toList());
    }

    private Set<DatasetFieldDTO> extractTitleTranslations(DataCiteFindDoiResponse.ResourceTitle title) {
        Set<DatasetFieldDTO> translation = new HashSet<>();

        if (title.getLang() != null) {
            addToSet(translation, DatasetFieldConstant.titleTranslationText, title.getTitle());
            addToSet(translation, DatasetFieldConstant.titleTranslationLanguage, title.getLang());
        }

        return translation;
    }

    private Set<DatasetFieldDTO> extractAuthorFields(DataCiteFindDoiResponse.Creator creator) {
        Set<DatasetFieldDTO> author = new HashSet<>();

        addToSet(author, DatasetFieldConstant.authorName, creator.getName());

        creator.getAffiliation().stream()
                .findFirst()
                .ifPresent(affiliation -> addToSet(author, DatasetFieldConstant.authorAffiliation, affiliation));

        creator.getNameIdentifiers().stream()
                .filter(ni -> SUPPORTED_IDENTIFIER_TYPES.contains(ni.getNameIdentifierScheme()))
                .findFirst()
                .ifPresent(nameIdentifier -> {
                    addToSet(author, DatasetFieldConstant.authorIdValue, nameIdentifier.getNameIdentifier());
                    addToSet(author, DatasetFieldConstant.authorIdType, nameIdentifier.getNameIdentifierScheme());
                });

        return author;
    }

    private Set<DatasetFieldDTO> extractDatasetContactFields(DataCiteFindDoiResponse.Contributor contactPerson) {
        Set<DatasetFieldDTO> contactFields = new HashSet<>();

        addToSet(contactFields, DatasetFieldConstant.datasetContactName, contactPerson.getName());

        contactPerson.getAffiliation().stream()
                .findFirst()
                .ifPresent(affiliation -> addToSet(contactFields, DatasetFieldConstant.datasetContactAffiliation, affiliation));

        return contactFields;
    }

    private Set<DatasetFieldDTO> extractProducerFields(DataCiteFindDoiResponse.Contributor producer) {
        Set<DatasetFieldDTO> producerFields = new HashSet<>();

        addToSet(producerFields, DatasetFieldConstant.producerName, producer.getName());

        producer.getAffiliation().stream()
                .findFirst()
                .ifPresent(affiliation -> addToSet(producerFields, DatasetFieldConstant.producerAffiliation, affiliation));

        return producerFields;
    }

    private Set<DatasetFieldDTO> extractGrantNumberFields(DataCiteFindDoiResponse.FundingReference fundingReference) {
        Set<DatasetFieldDTO> grantNumberFields = new HashSet<>();

        addToSet(grantNumberFields, DatasetFieldConstant.grantNumberAgency, fundingReference.getFunderName());
        // TODO: Check if we should only accept ROR fundingReference.getFunderIdentifierType
        addToSet(grantNumberFields, DatasetFieldConstant.grantNumberAgencyIdentifier, fundingReference.getFunderIdentifier());
        addToSet(grantNumberFields, DatasetFieldConstant.grantNumberValue, fundingReference.getAwardNumber());

        return grantNumberFields;
    }

    private Set<DatasetFieldDTO> extractRelatedMaterialFields(DataCiteFindDoiResponse.RelatedIdentifier ri) {
        Set<DatasetFieldDTO> relatedMaterial = new HashSet<>();

        addToSet(relatedMaterial, DatasetFieldConstant.relatedMaterialIDNumber, ri.getRelatedIdentifier());
        addToSet(relatedMaterial, DatasetFieldConstant.relatedMaterialIDType, ri.getRelatedIdentifierType());
        addToSet(relatedMaterial, DatasetFieldConstant.relatedMaterialRelationType, ri.getRelationType());

        return relatedMaterial;
    }

    private Set<DatasetFieldDTO> extractDescriptionFields(DataCiteFindDoiResponse.Description description) {
        Set<DatasetFieldDTO> descriptionFields = new HashSet<>();
        addToSet(descriptionFields, DatasetFieldConstant.descriptionText, description.getDescription());
        return descriptionFields;
    }

    private void addToSet(Set<DatasetFieldDTO> set, String typeName, String value) {
        if (StringUtils.isNotBlank(value)) {
            set.add(DatasetFieldDTOFactory.createPrimitive(typeName, value));
        }
    }

    private void addPrimitiveToBlock(MetadataBlockWithFieldsDTO block, String typeName, String value) {
        if (StringUtils.isNotBlank(value)) {
            block.getFields().add(DatasetFieldDTOFactory.createPrimitive(typeName, value));
        }
    }

    private void addCompoundToBlock(MetadataBlockWithFieldsDTO block, String typeName, List<Set<DatasetFieldDTO>> fieldSets) {
        if (!fieldSets.isEmpty()) {
            block.getFields().add(DatasetFieldDTOFactory.createMultipleCompound(typeName, fieldSets));
        }
    }

    private <T> List<Set<DatasetFieldDTO>> map(List<T> list, Function<T, Set<DatasetFieldDTO>> mapper) {
        if (list == null) {
            return Collections.emptyList();
        }

        return list.stream().map(mapper).filter(fields -> !fields.isEmpty()).collect(Collectors.toList());
    }
}
