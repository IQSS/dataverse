package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Stateless
public class SolrSearchResultsService {
    private static final Logger logger = LoggerFactory.getLogger(SolrSearchResultsService.class);

    private static final String DATAVERSES_QUERY_BASE_NAME = "Dataverse.findDataForSolrResults";

    private static final int DATAVERSES_QUERY_ID = 0;
    private static final int DATAVERSES_QUERY_AFFILIATION = 1;
    private static final int DATAVERSES_QUERY_ALIAS = 2;
    private static final int DATAVERSES_QUERY_PARENT_ALIAS = 3;

    private static final String DATASETS_QUERY_BASE_NAME = "Dataset.findDataForSolrResults";

    private static final int DATASETS_QUERY_VERSION_ID = 0;
    private static final int DATASETS_QUERY_VERSION_STATE = 1;
    private static final int DATASETS_QUERY_DATAVERSE_ALIAS = 2;
    private static final int DATASETS_QUERY_THUMBNAIL_ID = 3;
    private static final int DATASETS_QUERY_USE_GENERIC_THUMBNAIL = 4;
    private static final int DATASETS_QUERY_STORAGE_ID = 5;

    private static final String DATAFILES_QUERY_BASE_NAME = "Datafile.findDataForSolrResults";

    private static final int DATAFILES_QUERY_FILE_ID = 0;
    private static final int DATAFILES_QUERY_FILE_CREATEDATE = 1;
    private static final int DATAFILES_QUERY_FILE_PUBLICATIONDATE = 2;
    private static final int DATAFILES_QUERY_FILE_PREVIEWIMAGEAVAILABLE = 3;
    private static final int DATAFILES_QUERY_FILE_STORAGEIDENTIFIER = 4;
    private static final int DATAFILES_QUERY_FILE_AUTHORITY = 5;
    private static final int DATAFILES_QUERY_FILE_PROTOCOL = 6;
    private static final int DATAFILES_QUERY_FILE_IDENTIFIER = 7;
    private static final int DATAFILES_QUERY_FILE_CONTENTTYPE = 8;
    private static final int DATAFILES_QUERY_FILE_FILESIZE = 9;
    private static final int DATAFILES_QUERY_FILE_INGESTSTATUS = 10;
    private static final int DATAFILES_QUERY_FILE_CHECKSUMVALUE = 11;
    private static final int DATAFILES_QUERY_FILE_CHECKSUMTYPE = 12;
    private static final int DATAFILES_QUERY_DATASET_ID = 13;
    private static final int DATAFILES_QUERY_DATASET_IDENTIFIER = 14;
    private static final int DATAFILES_QUERY_DATASET_AUTHORITY = 15;
    private static final int DATAFILES_QUERY_DATASET_STORAGEIDENTIFIER = 16;
    private static final int DATAFILES_QUERY_DATATABLE_ID = 17;
    private static final int DATAFILES_QUERY_DATATABLE_UNF = 18;
    private static final int DATAFILES_QUERY_DATATABLE_CASEQUANTITY = 19;
    private static final int DATAFILES_QUERY_DATATABLE_VARQUANTITY = 20;

    private static final String DATAFILETAGS_QUERY_BASE_NAME = "DataFileTag.findData";

    private static final int DATAFILETAGS_QUERY_DATAFILE_ID = 0;
    private static final int DATAFILETAGS_QUERY_TYPE = 1;

    /*
     * IMPORTANT: ANY change to the values below REQUIRES changing
     * names and contents of appropriate named queries!
     */
    enum Size {
        MAX(10, "10"),
        MID(6, "6"),
        MIN(2, "2");

        private int value;
        private String querySuffix;

        Size(int value, String querySuffix) {
            this.value = value;
            this.querySuffix = querySuffix;
        }

        public int value() { return value; }
        public String querySuffix() { return querySuffix; }
    }

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- LOGIC --------------------

    public void populateDataverseSearchCard(Collection<SolrSearchResult> solrResults) {
        Set<Long> ids = extractNonNullValuesSet(solrResults, SolrSearchResult::getEntityId);

        Map<Long, Object[]> dataverses = callNamedNativeQueryWithIds(DATAVERSES_QUERY_BASE_NAME, ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[DATAVERSES_QUERY_ID], Function.identity(), (prev, next) -> next));

        for (SolrSearchResult result : solrResults) {
            Object[] dataverseData;
            if (result.getEntityId() == null || (dataverseData = dataverses.get(result.getEntityId())) == null) {
                continue;
            }
            setIfNotNull((String) dataverseData[DATAVERSES_QUERY_AFFILIATION], result::setDataverseAffiliation);
            setIfNotNull((String) dataverseData[DATAVERSES_QUERY_ALIAS], result::setDataverseAlias);
            setIfNotNull((String) dataverseData[DATAVERSES_QUERY_PARENT_ALIAS], result::setDataverseParentAlias);
        }
    }

    public void populateDatasetSearchCard(Collection<SolrSearchResult> solrResults) {
        Set<Long> ids = extractNonNullValuesSet(solrResults, SolrSearchResult::getDatasetVersionId);

        Map<Integer, Object[]> datasets = callNamedNativeQueryWithIds(DATASETS_QUERY_BASE_NAME, ids).stream()
                .collect(Collectors.toMap(r -> (Integer) r[DATASETS_QUERY_VERSION_ID], Function.identity(), (prev, next) -> next));

        Set<Long> thumbnailIds = datasets.values().stream()
                .map(v -> (Long) v[DATASETS_QUERY_THUMBNAIL_ID])
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, DataFile> thumbnails = findDatafiles(thumbnailIds);

        for (SolrSearchResult result : solrResults) {
            Object[] datasetData;
            if (result.getDatasetVersionId() == null
                    || (datasetData = datasets.get(result.getDatasetVersionId().intValue())) == null) {
                continue;
            }
            if ("DEACCESSIONED".equals(datasetData[DATASETS_QUERY_VERSION_STATE])) {
                result.setDeaccessionedState(true);
            }
            setIfNotNull((String) datasetData[DATASETS_QUERY_DATAVERSE_ALIAS], result::setDataverseAlias);
            setIfNotNull(result.getDeaccessionReason(), result::setDescriptionNoSnippet);

            Dataset entity = createAndFillDataset(result.getIdentifier(), datasetData, thumbnails);
            result.setEntity(entity);
        }
    }

    public void populateDatafileSearchCard(Collection<SolrSearchResult> solrResults) {
        Set<Long> ids = extractNonNullValuesSet(solrResults, SolrSearchResult::getEntityId);
        Map<Long, DataFile> datafiles = findDatafiles(ids);
        for (SolrSearchResult result : solrResults) {
            DataFile datafile;
            if ((datafile = datafiles.get(result.getEntityId())) != null) {
                result.setEntity(datafile);
            }
        }
    }

    public Map<Long, DataFile> findDatafiles(Collection<Long> ids) {
        Collection<Object[]> rawFilesData = callNamedNativeQueryWithIds(DATAFILES_QUERY_BASE_NAME, ids);
        Set<Integer> idsWithTags = rawFilesData.stream()
                .filter(d -> TextMimeType.TSV.getMimeValue().equalsIgnoreCase((String) d[DATAFILES_QUERY_FILE_CONTENTTYPE]))
                .map(d -> (Integer) d[DATAFILES_QUERY_FILE_ID])
                .collect(Collectors.toSet());
        Map<Long, List<Integer>> datafileTags = callNamedNativeQueryWithIds(DATAFILETAGS_QUERY_BASE_NAME, idsWithTags).stream()
                .collect(Collectors.groupingBy(d -> (Long) d[DATAFILETAGS_QUERY_DATAFILE_ID],
                        Collectors.mapping(d -> (Integer) d[DATAFILETAGS_QUERY_TYPE], Collectors.toList())));
        List<String> tagLabels = DataFileTag.listTags();

        Map<Long, DataFile> datafiles = new HashMap<>();
        for (Object[] fileData : rawFilesData) {
            DataFile dataFile = createAndFillDataFile(fileData);
            datafiles.put(dataFile.getId(), dataFile);
            fillTabularDataIfNeeded(dataFile, fileData, datafileTags, tagLabels);
        }
        return datafiles;
    }

    // -------------------- PRIVATE --------------------

    private <T, U> Set<U> extractNonNullValuesSet(Collection<T> source, Function<T, U> valueMapper) {
        return source != null
                ? source.stream()
                    .map(valueMapper)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                : Collections.emptySet();
    }

    /**
     * This and {@link SolrSearchResultsService#callSingleBatchForIds(String, Collection)}
     * are used to circumvent the deficiency of native queries that don't allow
     * to pass a collection for sql operator IN. Moreover according to
     * https://www.jooq.org/doc/3.15/manual/sql-building/dsl-context/custom-settings/settings-in-list-padding/
     * some DBs can optimize execution of queries by caching their execution
     * plans – for that case we should reduce the amount of different queries
     * that are sent to DB (queries with different number of parameters in IN
     * clause are treated as different). So here we're using 3 different sizes
     * of IN params list (they're chosen to reflect currently used search page
     * size): 2, 6 and 10, and we do some calculations and some value padding to
     * properly choose and fill such query. From postgres docs it's hard to tell
     * whether these optimizations are in fact done, but nevertheless it's worth
     * a try as enabling the use of collections for IN in native queries would
     * be only a bit simpler.
     */
    private Collection<Object[]> callNamedNativeQueryWithIds(String queryBaseName, Collection<? extends Number> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.size() > Size.MAX.value()
                ? ListUtils.partition(new ArrayList<>(ids), Size.MAX.value()).stream()
                .map(l -> callSingleBatchForIds(queryBaseName, l))
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
                : callSingleBatchForIds(queryBaseName, ids);
    }

    private Collection<Object[]> callSingleBatchForIds(String queryBaseName, Collection <? extends Number> ids) {
        int size = ids.size();
        Size selectedSize = size > Size.MID.value()
                ? Size.MAX
                : size > Size.MIN.value()
                    ? Size.MID : Size.MIN;
        Query query = em.createNamedQuery(queryBaseName + selectedSize.querySuffix());
        int count = 1;
        Number currentId = 0; // ids cannot be empty (as long as it's called from callNamedNativeQueryWithIds),
                              // so the value will be overwritten
        for (Number id : ids) {
            currentId = id;
            query.setParameter(count, currentId);
            ++count;
        }
        for (; count <= selectedSize.value(); count++) {
            query.setParameter(count, currentId);
        }
        return (Collection<Object[]>) query.getResultList();
    }

    private <T> void setIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private Dataset createAndFillDataset(String identifier, Object[] datasetData, Map<Long, DataFile> thumbnails) {
        Dataset entity = new Dataset();
        GlobalId globalId = new GlobalId(identifier);
        entity.setProtocol(globalId.getProtocol());
        entity.setAuthority(globalId.getAuthority());
        entity.setIdentifier(globalId.getIdentifier());
        if (datasetData[DATASETS_QUERY_STORAGE_ID] != null) {
            entity.setStorageIdentifier(datasetData[DATASETS_QUERY_STORAGE_ID].toString());
        }
        DataFile thumbnail;
        Long thumbnailId = (Long) datasetData[DATASETS_QUERY_THUMBNAIL_ID];
        if (thumbnailId != null && (thumbnail = thumbnails.get(thumbnailId)) != null) {
            entity.setThumbnailFile(thumbnail);
        }
        Boolean useGenericThumbnail = (Boolean) datasetData[DATASETS_QUERY_USE_GENERIC_THUMBNAIL];
        entity.setUseGenericThumbnail(useGenericThumbnail != null ? useGenericThumbnail : false);
        return entity;
    }

    private DataFile createAndFillDataFile(Object[] fileData) {
        DataFile dataFile = new DataFile();
        dataFile.setMergeable(false);
        dataFile.setId(((Integer) fileData[DATAFILES_QUERY_FILE_ID]).longValue());
        dataFile.setCreateDate((Timestamp) fileData[DATAFILES_QUERY_FILE_CREATEDATE]);
        dataFile.setPublicationDate((Timestamp) fileData[DATAFILES_QUERY_FILE_PUBLICATIONDATE]);
        setIfNotNull((Boolean) fileData[DATAFILES_QUERY_FILE_PREVIEWIMAGEAVAILABLE], dataFile::setPreviewImageAvailable);
        setIfNotNull((String) fileData[DATAFILES_QUERY_FILE_CONTENTTYPE], dataFile::setContentType);
        setIfNotNull((String) fileData[DATAFILES_QUERY_FILE_STORAGEIDENTIFIER], dataFile::setStorageIdentifier);
        setIfNotNull((Long) fileData[DATAFILES_QUERY_FILE_FILESIZE], dataFile::setFilesize);
        String ingestStatus = (String) fileData[DATAFILES_QUERY_FILE_INGESTSTATUS];
        if (ingestStatus != null) {
            dataFile.setIngestStatus(ingestStatus.charAt(0));
        }
        setIfNotNull((String) fileData[DATAFILES_QUERY_FILE_CHECKSUMVALUE], dataFile::setChecksumValue);
        String checksumType = (String) fileData[DATAFILES_QUERY_FILE_CHECKSUMTYPE];
        if (checksumType != null) {
            try {
                DataFile.ChecksumType type = DataFile.ChecksumType.valueOf(checksumType);
                dataFile.setChecksumType(type);
            } catch (IllegalArgumentException iae) {
                logger.info(String.format("Cannot convert [%s] to ChecksumType", checksumType), iae);
            }
        }
        setIfNotNull((String) fileData[DATAFILES_QUERY_FILE_AUTHORITY], dataFile::setAuthority);
        setIfNotNull((String) fileData[DATAFILES_QUERY_FILE_PROTOCOL], dataFile::setProtocol);
        setIfNotNull((String) fileData[DATAFILES_QUERY_FILE_IDENTIFIER], dataFile::setIdentifier);
        Dataset owner = new Dataset();
        Integer ownerId = (Integer) fileData[DATAFILES_QUERY_DATASET_ID];
        if (ownerId != null) {
            owner.setId(ownerId.longValue());
        }
        setIfNotNull((String) fileData[DATAFILES_QUERY_DATASET_AUTHORITY], owner::setAuthority);
        setIfNotNull((String) fileData[DATAFILES_QUERY_DATASET_IDENTIFIER], owner::setIdentifier);
        setIfNotNull((String) fileData[DATAFILES_QUERY_DATASET_STORAGEIDENTIFIER], owner::setStorageIdentifier);
        dataFile.setOwner(owner);
        return dataFile;
    }

    private void fillTabularDataIfNeeded(DataFile dataFile, Object[] fileData,
                                         Map<Long, List<Integer>> datafileTags, List<String> tagLabels) {
        if (dataFile.getContentType() == null
                || (!TextMimeType.TSV.getMimeValue().equalsIgnoreCase(dataFile.getContentType())
                && !TextMimeType.TSV_ALT.getMimeValue().equalsIgnoreCase(dataFile.getContentType()))) {
            return;
        }
        DataTable dataTable = new DataTable();
        Integer dataTableId = (Integer) fileData[DATAFILES_QUERY_DATATABLE_ID];
        if (dataTableId != null) {
            dataTable.setId(dataTableId.longValue());
        }
        setIfNotNull((String) fileData[DATAFILES_QUERY_DATATABLE_UNF], dataTable::setUnf);
        setIfNotNull((Long) fileData[DATAFILES_QUERY_DATATABLE_CASEQUANTITY], dataTable::setCaseQuantity);
        setIfNotNull((Long) fileData[DATAFILES_QUERY_DATATABLE_VARQUANTITY], dataTable::setVarQuantity);
        dataTable.setDataFile(dataFile);
        dataFile.setDataTable(dataTable);
        List<Integer> tagIds;
        if ((tagIds = datafileTags.get(dataFile.getId())) != null) {
            for (Integer tagId : tagIds) {
                DataFileTag tag = new DataFileTag();
                tag.setTypeByLabel(tagLabels.get(tagId));
                tag.setDataFile(dataFile);
                dataFile.addTag(tag);
            }
        }
    }
}
