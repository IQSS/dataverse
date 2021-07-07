package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
                .setTitle(dsv.getTitle());

        if (!dsv.getDataset().isHarvested()) {
            data.getProducers().addAll(extractProducers(dsv));
            data.getDistributors().addAll(dsv.getDatasetFieldValuesByTypeName(DatasetFieldConstant.distributorName));
            data.getFunders().addAll(dsv.getUniqueGrantAgencyValues());
            data.getKindsOfData().addAll(dsv.getKindOfData());
            data.getDatesOfCollection().addAll(dsv.getDatesOfCollection());
            data.getLanguages().addAll(dsv.getLanguages());
            data.getSpatialCoverages().addAll(dsv.getSpatialCoverages());
            data.getKeywords().addAll(dsv.getKeywords());
            data.getOtherIds().addAll(dsv.getDatasetFieldValuesByTypeName(DatasetFieldConstant.otherIdValue));

            data.setDate(dataDate)
                    .setProductionPlace(extractField(dsv, DatasetFieldConstant.productionPlace))
                    .setProductionDate(extractProductionDate(dsv))
                    .setReleaseYear(extractReleaseYear(dsv))
                    .setRootDataverseName(dsv.getRootDataverseNameforCitation())
                    .setSeriesTitle(dsv.getSeriesTitle())
                    .setPublisher(extractPublisher(dsv))
                    .setVersion(extractVersion(dsv));
        }
    }

    private String extractField(DatasetVersion dsv, String typeName) {
        return dsv.getDatasetFieldValueByTypeName(typeName)
                .orElse(null);
    }

    private String extractProductionDate(DatasetVersion dsv) {
        String rawDate = dsv.getDatasetFieldValueByTypeName(DatasetFieldConstant.productionDate)
                .orElse(StringUtils.EMPTY);
        Pattern year = Pattern.compile("\\d{4}");
        Matcher yearMatcher = year.matcher(rawDate);
        if (yearMatcher.find()) {
            return yearMatcher.group();
        }
        return StringUtils.EMPTY;
    }

    private List<CitationData.Producer> extractProducers(DatasetVersion dsv) {
        return dsv.getDatasetProducers(DatasetField::getValue).stream()
                .map(p -> new CitationData.Producer(p[0], p[1]))
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
            citationDate = dsv.getCitationDate();
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

    private String extractPublisher(DatasetVersion dsv) {
        return dsv.getRootDataverseNameforCitation();
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
