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

@Stateless
public class CitationDataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(CitationDataExtractor.class);

    // -------------------- LOGIC --------------------

    public CitationData create(DatasetVersion datasetVersion) {
        CitationData data = new CitationData();
        extractAndWriteCommonValues(datasetVersion, data);

        data.setDirect(false)
                .setPersistentId(extractPID(datasetVersion, datasetVersion.getDataset(), false)) // Global Id: always part of citation for local datasets & some harvested
                .setUNF(datasetVersion.getUNF());

        return data;
    }

    public CitationData create(FileMetadata fileMetadata, boolean direct) {
        CitationData data = new CitationData();
        DatasetVersion dsv = fileMetadata.getDatasetVersion();
        extractAndWriteCommonValues(dsv, data);

        DataFile df = fileMetadata.getDataFile();

        data.setDirect(direct)
                .setFileTitle(fileMetadata.getLabel())
                .setDescription(fileMetadata.getDescription())
                .setPersistentId(extractPID(dsv, df, direct)); // Global Id of datafile (if published & isDirect==true) or dataset as appropriate

        if (df.isTabularData() && df.getUnf() != null && !df.getUnf().isEmpty()) {
            data.setUNF(df.getUnf());
        }
        return data;
    }

    // -------------------- PRIVATE --------------------

    private void extractAndWriteCommonValues(DatasetVersion dsv, CitationData data) {
        data.getAuthors().addAll(extractAuthors(dsv));
        data.getProducers().addAll(extractProducers(dsv));
        data.getDistributors().addAll(dsv.getDatasetFieldValuesByTypeName(DatasetFieldConstant.distributorName));
        data.getFunders().addAll(dsv.getUniqueGrantAgencyValues());
        data.getKindsOfData().addAll(dsv.getKindOfData());
        data.getDatesOfCollection().addAll(dsv.getDatesOfCollection());
        data.getLanguages().addAll(dsv.getLanguages());
        data.getSpatialCoverages().addAll(dsv.getSpatialCoverages());
        data.getKeywords().addAll(dsv.getKeywords());
        data.getOtherIds().addAll(dsv.getDatasetFieldValuesByTypeName(DatasetFieldConstant.otherIdValue));

        data.setDate(extractCitationDate(dsv))
                .setYear(new SimpleDateFormat("yyyy").format(data.getDate()))
                .setProductionPlace(extractField(dsv, DatasetFieldConstant.productionPlace))
                .setProductionDate(extractProductionDate(dsv))
                .setReleaseYear(extractReleaseYear(dsv))
                .setRootDataverseName(dsv.getRootDataverseNameforCitation())
                .setTitle(dsv.getTitle())
                .setSeriesTitle(dsv.getSeriesTitle())
                .setPublisher(extractPublisher(dsv))
                .setVersion(extractVersion(dsv));
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
            if (direct && StringUtils.isNotEmpty(dsv.getDataset().getIdentifier())) {
                return new GlobalId(dsv.getDataset());
            } else if (!direct && StringUtils.isNotEmpty(dv.getIdentifier())) {
                return new GlobalId(dv);
            }
        }
        return null;
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
                citationDate = new SimpleDateFormat("yyyy")
                        .parse(dsv.getDistributionDate());
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
        return !dsv.getDataset().isHarvested()
                ? dsv.getRootDataverseNameforCitation()
                : dsv.getDistributorName();
    }

    private String extractVersion(DatasetVersion dsv) {
        if (dsv.getDataset().isHarvested()) {
            return StringUtils.EMPTY;
        }
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
