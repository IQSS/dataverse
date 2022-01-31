package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Stateless
public class CitationDataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(CitationDataExtractor.class);

    // -------------------- LOGIC --------------------

    public CitationData create(DatasetVersion datasetVersion) {
        CitationData data = new CitationData();
        extractAndWriteCommonValues(datasetVersion, data);

        data.setDirect(false)
                .setPersistentId(extractPID(datasetVersion, datasetVersion.getDataset(), false)) // Global Id: always part of citation for local datasets & some harvested
                .setPidOfDataset(extractDatasetPID(datasetVersion));

        return data;
    }

    public CitationData create(FileMetadata fileMetadata, boolean direct) {
        CitationData data = new CitationData();
        DatasetVersion dsv = fileMetadata.getDatasetVersion();
        extractAndWriteCommonValues(dsv, data);

        DataFile df = fileMetadata.getDataFile();

        data.setDirect(direct)
                .setFileTitle(fileMetadata.getLabel())
                .setPersistentId(extractPID(dsv, df, direct)) // Global Id of datafile (if published & isDirect==true) or dataset as appropriate
                .setPidOfDataset(extractDatasetPID(dsv))
                .setPidOfFile(extractFilePID(dsv, df, direct));
        return data;
    }

    // -------------------- PRIVATE --------------------

    private void extractAndWriteCommonValues(DatasetVersion dsv, CitationData data) {
        Date dataDate = extractCitationDate(dsv);

        data.getAuthors().addAll(extractAuthors(dsv));
        data.setYear(new SimpleDateFormat("yyyy").format(dataDate))
                .setTitle(dsv.getParsedTitle());

        if (!dsv.getDataset().isHarvested()) {
            data.getProducers().addAll(extractProducers(dsv));
            data.getDistributors().addAll(getDatasetFieldValuesByTypeName(dsv, DatasetFieldConstant.distributorName));
            data.getFunders().addAll(getUniqueGrantAgencyValues(dsv));
            data.getKindsOfData().addAll(dsv.extractFieldValues(DatasetFieldConstant.kindOfData));
            data.getDatesOfCollection().addAll(getDatesOfCollection(dsv));
            data.getLanguages().addAll(dsv.extractFieldValues(DatasetFieldConstant.language));
            data.getSpatialCoverages().addAll(extractSpatialCoverages(dsv));
            data.getKeywords().addAll(dsv.getKeywords());
            data.getOtherIds().addAll(getDatasetFieldValuesByTypeName(dsv, DatasetFieldConstant.otherIdValue));

            data.setDate(dataDate)
                    .setProductionPlace(extractField(dsv, DatasetFieldConstant.productionPlace))
                    .setProductionDate(extractProductionDate(dsv))
                    .setReleaseYear(extractReleaseYear(dsv))
                    .setRootDataverseName(dsv.getRootDataverseNameForCitation())
                    .setSeriesTitle(getSeriesTitle(dsv))
                    .setPublisher(extractPublisher(dsv))
                    .setVersion(extractVersion(dsv));
        }
    }

    public List<String> getDatasetFieldValuesByTypeName(DatasetVersion dsv, String datasetFieldTypeName) {
        return dsv.streamDatasetFieldsByTypeName(datasetFieldTypeName)
                .map(DatasetField::getValue)
                .collect(Collectors.toList());
    }

    private String extractField(DatasetVersion dsv, String typeName) {
        return dsv.getDatasetFieldByTypeName(typeName)
                .map(DatasetField::getValue)
                .orElse(null);
    }

    private List<String> getUniqueGrantAgencyValues(DatasetVersion version) {
        // Since only grant agency names are returned, use distinct() to avoid repeats
        // (e.g. if there are two grants from the same agency)
        return version.getCompoundChildFieldValues(DatasetFieldConstant.grantNumber,
                Collections.singletonList(DatasetFieldConstant.grantNumberAgency)).stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getDatesOfCollection(DatasetVersion dsv) {
        return dsv.extractSubfields(DatasetFieldConstant.dateOfCollection,
                Arrays.asList(DatasetFieldConstant.dateOfCollectionStart, DatasetFieldConstant.dateOfCollectionEnd))
                .stream()
                .map(e -> Tuple.of(e.get(DatasetFieldConstant.dateOfCollectionStart), e.get(DatasetFieldConstant.dateOfCollectionEnd)))
                .filter(t -> t._1 != null && !t._1.isEmptyForDisplay() && t._2 != null && !t._2.isEmptyForDisplay())
                .map(t -> t._1.getValue() + "/" + t._2.getValue())
                .collect(Collectors.toList());
    }

    public List<String> extractSpatialCoverages(DatasetVersion version) {
        List<String> subfields = Arrays.asList(DatasetFieldConstant.country, DatasetFieldConstant.state,
                DatasetFieldConstant.city, DatasetFieldConstant.otherGeographicCoverage);
        return version.extractSubfields(DatasetFieldConstant.geographicCoverage, subfields).stream()
                .map(s -> subfields.stream()
                        .map(s::get)
                        .filter(v -> v != null && !v.isEmptyForDisplay())
                        .map(DatasetField::getValue)
                        .collect(Collectors.joining(",")))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    private String extractProductionDate(DatasetVersion dsv) {
        String rawDate = dsv.getDatasetFieldByTypeName(DatasetFieldConstant.productionDate)
                .map(DatasetField::getValue)
                .orElse(StringUtils.EMPTY);
        Pattern year = Pattern.compile("\\d{4}");
        Matcher yearMatcher = year.matcher(rawDate);
        if (yearMatcher.find()) {
            return yearMatcher.group();
        }
        return StringUtils.EMPTY;
    }

    private List<CitationData.Producer> extractProducers(DatasetVersion dsv) {
        return getProducers(dsv).stream()
                .map(p -> new CitationData.Producer(p._1, p._2))
                .collect(Collectors.toList());
    }

    private List<Tuple2<String, String>> getProducers(DatasetVersion dsv) {
        return dsv.extractSubfields(DatasetFieldConstant.producer,
                Arrays.asList(DatasetFieldConstant.producerName, DatasetFieldConstant.producerAffiliation))
                .stream()
                .filter(e -> {
                    DatasetField name = e.get(DatasetFieldConstant.producerName);
                    return name != null && !name.isEmptyForDisplay();
                })
                .map(e -> Tuple.of(e.get(DatasetFieldConstant.producerName), e.get(DatasetFieldConstant.producerAffiliation)))
                .map(t -> Tuple.of(t._1.getValue(),
                        t._2 != null ? t._2.getValue() : StringUtils.EMPTY))
                .collect(Collectors.toList());
    }

    private String extractReleaseYear(DatasetVersion dsv) {
        return dsv.getReleaseTime() != null
                ? new SimpleDateFormat("yyyy").format(dsv.getReleaseTime())
                : StringUtils.EMPTY;
    }

    private List<String> extractAuthors(DatasetVersion dsv) {
        return dsv.getDatasetAuthors().stream()
                .filter(a -> !a.isEmpty())
                .map(a -> a.getName().getDisplayValue().trim())
                .collect(Collectors.toList());
    }

    private GlobalId extractPID(DatasetVersion dsv, DvObject dv, boolean direct) {
        if (shouldCreateGlobalId(dsv)) {
            if (!direct && isNotEmpty(dsv.getDataset().getIdentifier())) {
                return new GlobalId(dsv.getDataset());
            } else if (direct && isNotEmpty(dv.getIdentifier())) {
                return new GlobalId(dv);
            }
        }
        return null;
    }

    private GlobalId extractDatasetPID(DatasetVersion dsv) {
        return shouldCreateGlobalId(dsv) && isNotEmpty(dsv.getDataset().getIdentifier())
                ? new GlobalId(dsv.getDataset()) : null;
    }

    private GlobalId extractFilePID(DatasetVersion dsv, DataFile datafile, boolean direct) {
        return shouldCreateGlobalId(dsv) && !direct && isNotEmpty(datafile.getIdentifier())
                ? new GlobalId(datafile) : null;
    }

    private boolean shouldCreateGlobalId(DatasetVersion dsv) {
        String harvestStyle = Optional.ofNullable(dsv.getDataset().getHarvestedFrom())
                .map(HarvestingClient::getHarvestStyle)
                .orElse(StringUtils.EMPTY);

        return !dsv.getDataset().isHarvested()
                || HarvestingClient.HARVEST_STYLE_VDC.equals(harvestStyle)
                || HarvestingClient.HARVEST_STYLE_ICPSR.equals(harvestStyle)
                || HarvestingClient.HARVEST_STYLE_DATAVERSE.equals(harvestStyle);
    }

    private Date extractCitationDate(DatasetVersion dsv) {
        Date citationDate = null;
        if (!dsv.getDataset().isHarvested()) {
            citationDate = getCitationDate(dsv);
            if (citationDate == null) {
                citationDate = dsv.getDataset().getPublicationDate() != null
                        ? dsv.getDataset().getPublicationDate()
                        : dsv.getLastUpdateTime(); // for drafts
            }
        } else {
            try {
                citationDate = dsv.getDistributionDate() != null
                        ? new SimpleDateFormat("yyyy").parse(dsv.getDistributionDate())
                        : null;
            } catch (ParseException pe) {
                logger.warn(String.format("Error parsing date [%s]", dsv.getDistributionDate()), pe);
            }
        }
        if (citationDate == null) {
            logger.warn("Unable to find citation date for datasetversion: {}", dsv.getId());
            citationDate = new Date(); // As a last resort, pick the current date
        }
        return citationDate;
    }

    private Date getCitationDate(DatasetVersion dsv) {
        DatasetFieldType citationDateType = dsv.getDataset().getCitationDateDatasetFieldType();
        DatasetField citationDate = citationDateType != null
                ? dsv.getDatasetFieldByTypeName(citationDateType.getName()).orElse(null) : null;
        if (citationDate != null && FieldType.DATE.equals(citationDate.getDatasetFieldType().getFieldType())) {
            try {
                return new SimpleDateFormat("yyyy").parse(citationDate.getValue());
            } catch (ParseException ex) {
                logger.warn("Date parsing exception: ", ex);
            }
        }
        return null;
    }

    private String getSeriesTitle(DatasetVersion version) {
     return version.getCompoundChildFieldValues(DatasetFieldConstant.series,
                Collections.singletonList(DatasetFieldConstant.seriesName))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String extractPublisher(DatasetVersion dsv) {
        return dsv.getRootDataverseNameForCitation();
    }

    private String extractVersion(DatasetVersion dsv) {
        String version = StringUtils.EMPTY;
        if (dsv.isDraft()) {
            version = BundleUtil.getStringFromBundle("draftversion");
        } else if (dsv.getVersionNumber() != null) {
            version = "V" + dsv.getVersionNumber()
                    + (dsv.isDeaccessioned()
                    ? ", " + BundleUtil.getStringFromBundle("deaccessionedversion")
                    : StringUtils.EMPTY);
        }
        return version;
    }
}
