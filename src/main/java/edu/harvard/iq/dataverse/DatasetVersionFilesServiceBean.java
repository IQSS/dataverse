package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.FileSearchCriteria.FileAccessStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

import static edu.harvard.iq.dataverse.DataFileTag.TagLabelToTypes;

@Stateless
@Named
public class DatasetVersionFilesServiceBean implements Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    @EJB
    DataFileServiceBean datafileService;

    /**
     * Different criteria to sort the results of FileMetadata queries used in {@link DatasetVersionFilesServiceBean#getFileMetadatas}
     */
    public enum FileOrderCriteria {
        NameAZ, NameZA, Newest, Oldest, Size, Type
    }

    /**
     * Mode to base the search in {@link DatasetVersionFilesServiceBean#getFilesDownloadSize(DatasetVersion, FileSearchCriteria, FileDownloadSizeMode)}
     * <p>
     * All: Includes both archival and original sizes for tabular files
     * Archival: Includes only the archival size for tabular files
     * Original: Includes only the original size for tabular files
     * <p>
     * All the modes include archival sizes for non-tabular files
     */
    public enum FileDownloadSizeMode {
        All, Original, Archival
    }

    /**
     * Given a DatasetVersion, returns its total file metadata count
     *
     * @param datasetVersion the DatasetVersion to access
     * @return long value of total file metadata count
     */
    public long getFileMetadataCount(DatasetVersion datasetVersion) {
        return getFileMetadataCount(datasetVersion, new FileSearchCriteria(null, null, null, null, null));
    }

    /**
     * Given a DatasetVersion, returns its total file metadata count
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for counting only files matching this criteria
     * @return long value of total file metadata count
     */
    public long getFileMetadataCount(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        criteriaQuery
                .select(criteriaBuilder.count(fileMetadataRoot))
                .where(createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot));
        return em.createQuery(criteriaQuery).getSingleResult();
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per content type
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for counting only files matching this criteria
     * @return Map<String, Long> of file metadata counts per content type
     */
    public Map<String, Long> getFileMetadataCountPerContentType(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Path<String> contentType = fileMetadataRoot.get("dataFile").get("contentType");
        criteriaQuery
                .multiselect(contentType, criteriaBuilder.count(contentType))
                .where(createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot))
                .groupBy(contentType);
        return getStringLongMapResultFromQuery(criteriaQuery);
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per category name
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for counting only files matching this criteria
     * @return Map<String, Long> of file metadata counts per category name
     */
    public Map<String, Long> getFileMetadataCountPerCategoryName(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Root<DataFileCategory> dataFileCategoryRoot = criteriaQuery.from(DataFileCategory.class);
        Path<String> categoryName = dataFileCategoryRoot.get("name");
        criteriaQuery
                .multiselect(categoryName, criteriaBuilder.count(fileMetadataRoot))
                .where(criteriaBuilder.and(
                        createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot),
                        dataFileCategoryRoot.in(fileMetadataRoot.get("fileCategories"))))
                .groupBy(categoryName);
        return getStringLongMapResultFromQuery(criteriaQuery);
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per DataFileTag.TagType
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for counting only files matching this criteria
     * @return Map<DataFileTag.TagType, Long> of file metadata counts per DataFileTag.TagType
     */
    public Map<DataFileTag.TagType, Long> getFileMetadataCountPerTabularTagName(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Root<DataFileTag> dataFileTagRoot = criteriaQuery.from(DataFileTag.class);
        Path<DataFileTag.TagType> dataFileTagType = dataFileTagRoot.get("type");
        criteriaQuery
                .multiselect(dataFileTagType, criteriaBuilder.count(fileMetadataRoot))
                .where(criteriaBuilder.and(
                        createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot),
                        dataFileTagRoot.in(fileMetadataRoot.get("dataFile").get("dataFileTags"))))
                .groupBy(dataFileTagType);
        List<Tuple> tagNameOccurrences = em.createQuery(criteriaQuery).getResultList();
        Map<DataFileTag.TagType, Long> result = new HashMap<>();
        for (Tuple occurrence : tagNameOccurrences) {
            result.put(occurrence.get(0, DataFileTag.TagType.class), occurrence.get(1, Long.class));
        }
        return result;
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per FileAccessStatus
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for counting only files matching this criteria
     * @return Map<FileAccessStatus, Long> of file metadata counts per FileAccessStatus
     */
    public Map<FileAccessStatus, Long> getFileMetadataCountPerAccessStatus(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        Map<FileAccessStatus, Long> allCounts = new HashMap<>();
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.Public, searchCriteria);
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.Restricted, searchCriteria);
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.EmbargoedThenPublic, searchCriteria);
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.EmbargoedThenRestricted, searchCriteria);
        return allCounts;
    }

    /**
     * Returns a FileMetadata list of files in the specified DatasetVersion
     *
     * @param datasetVersion the DatasetVersion to access
     * @param limit          for pagination, can be null
     * @param offset         for pagination, can be null
     * @param searchCriteria for retrieving only files matching this criteria
     * @param orderCriteria  a FileOrderCriteria to order the results
     * @return a FileMetadata list from the specified DatasetVersion
     */
    public List<FileMetadata> getFileMetadatas(DatasetVersion datasetVersion, Integer limit, Integer offset, FileSearchCriteria searchCriteria, FileOrderCriteria orderCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<FileMetadata> criteriaQuery = criteriaBuilder.createQuery(FileMetadata.class);
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        criteriaQuery
                .select(fileMetadataRoot)
                .where(createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot))
                .orderBy(createGetFileMetadatasOrder(criteriaBuilder, orderCriteria, fileMetadataRoot));
        TypedQuery<FileMetadata> typedQuery = em.createQuery(criteriaQuery);
        if (limit != null) {
            typedQuery.setMaxResults(limit);
        }
        if (offset != null) {
            typedQuery.setFirstResult(offset);
        }
        List<FileMetadata> fms = typedQuery.getResultList();
        // populate the entries in the list with the deleted/replaced flag per https://github.com/IQSS/dataverse/issues/11384
        fms.forEach(fm -> {
            DataFile df = fm.getDataFile();
            if (df.getDeleted() == null) {
                df.setDeleted(datafileService.hasBeenDeleted(df));
            }
        });
        return fms;
    }

    /**
     * Returns the total download size of all files for a particular DatasetVersion
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for retrieving only files matching this criteria
     * @param mode           a FileDownloadSizeMode to base the search on
     * @return long value of total file download size
     */
    public long getFilesDownloadSize(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria, FileDownloadSizeMode mode) {
        return switch (mode) {
            case All ->
                    Long.sum(getOriginalTabularFilesSize(datasetVersion, searchCriteria), getArchivalFilesSize(datasetVersion, false, searchCriteria));
            case Original ->
                    Long.sum(getOriginalTabularFilesSize(datasetVersion, searchCriteria), getArchivalFilesSize(datasetVersion, true, searchCriteria));
            case Archival -> getArchivalFilesSize(datasetVersion, false, searchCriteria);
        };
    }

    /**
     * Determines whether or not a DataFile is present in a DatasetVersion
     *
     * @param datasetVersion the DatasetVersion to check
     * @param dataFile the DataFile to check
     * @return boolean value
     */
    public boolean isDataFilePresentInDatasetVersion(DatasetVersion datasetVersion, DataFile dataFile) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<DataFile> dataFileRoot = criteriaQuery.from(DataFile.class);
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Root<DatasetVersion> datasetVersionRoot = criteriaQuery.from(DatasetVersion.class);
        criteriaQuery
                .select(criteriaBuilder.count(dataFileRoot))
                .where(criteriaBuilder.and(
                        criteriaBuilder.equal(dataFileRoot.get("id"), dataFile.getId()),
                        criteriaBuilder.equal(datasetVersionRoot.get("id"), datasetVersion.getId()),
                        fileMetadataRoot.in(dataFileRoot.get("fileMetadatas")),
                        fileMetadataRoot.in(datasetVersionRoot.get("fileMetadatas"))
                        )
                );
        Long count = em.createQuery(criteriaQuery).getSingleResult();
        return count != null && count > 0;
    }

    private void addAccessStatusCountToTotal(DatasetVersion datasetVersion, Map<FileAccessStatus, Long> totalCounts, FileAccessStatus dataFileAccessStatus, FileSearchCriteria searchCriteria) {
        long fileMetadataCount = getFileMetadataCountByAccessStatus(datasetVersion, dataFileAccessStatus, searchCriteria);
        if (fileMetadataCount > 0) {
            totalCounts.put(dataFileAccessStatus, fileMetadataCount);
        }
    }

    private long getFileMetadataCountByAccessStatus(DatasetVersion datasetVersion, FileAccessStatus accessStatus, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        criteriaQuery
                .select(criteriaBuilder.count(fileMetadataRoot))
                .where(criteriaBuilder.and(
                        createSearchCriteriaAccessStatusPredicate(accessStatus, criteriaBuilder, fileMetadataRoot),
                        createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot)));
        return em.createQuery(criteriaQuery).getSingleResult();
    }

    private Predicate createSearchCriteriaAccessStatusPredicate(FileAccessStatus accessStatus, CriteriaBuilder criteriaBuilder, Root<FileMetadata> fileMetadataRoot) {
        Path<Object> dataFile = fileMetadataRoot.get("dataFile");
        Path<Object> retention = dataFile.get("retention");
        Predicate retentionExpiredPredicate = criteriaBuilder.lessThan(retention.<Date>get("dateUnavailable"), criteriaBuilder.currentDate());
        Path<Object> embargo = dataFile.get("embargo");
        Predicate activelyEmbargoedPredicate = criteriaBuilder.greaterThanOrEqualTo(embargo.<Date>get("dateAvailable"), criteriaBuilder.currentDate());
        Predicate inactivelyEmbargoedPredicate = criteriaBuilder.isNull(embargo);
        Path<Boolean> isRestricted = dataFile.get("restricted");
        Predicate isRestrictedPredicate = criteriaBuilder.isTrue(isRestricted);
        Predicate isUnrestrictedPredicate = criteriaBuilder.isFalse(isRestricted);
        return switch (accessStatus) {
            case RetentionPeriodExpired -> criteriaBuilder.and(retentionExpiredPredicate);
            case EmbargoedThenRestricted -> criteriaBuilder.and(activelyEmbargoedPredicate, isRestrictedPredicate);
            case EmbargoedThenPublic -> criteriaBuilder.and(activelyEmbargoedPredicate, isUnrestrictedPredicate);
            case Restricted -> criteriaBuilder.and(inactivelyEmbargoedPredicate, isRestrictedPredicate);
            case Public -> criteriaBuilder.and(inactivelyEmbargoedPredicate, isUnrestrictedPredicate);
        };
    }

    private Predicate createSearchCriteriaPredicate(DatasetVersion datasetVersion,
                                                    FileSearchCriteria searchCriteria,
                                                    CriteriaBuilder criteriaBuilder,
                                                    CriteriaQuery<?> criteriaQuery,
                                                    Root<FileMetadata> fileMetadataRoot) {
        List<Predicate> predicates = new ArrayList<>();
        Predicate basePredicate = criteriaBuilder.equal(fileMetadataRoot.get("datasetVersion").<String>get("id"), datasetVersion.getId());
        predicates.add(basePredicate);
        String contentType = searchCriteria.getContentType();
        if (contentType != null) {
            predicates.add(criteriaBuilder.equal(fileMetadataRoot.get("dataFile").<String>get("contentType"), contentType));
        }
        FileAccessStatus accessStatus = searchCriteria.getAccessStatus();
        if (accessStatus != null) {
            predicates.add(createSearchCriteriaAccessStatusPredicate(accessStatus, criteriaBuilder, fileMetadataRoot));
        }
        String categoryName = searchCriteria.getCategoryName();
        if (categoryName != null) {
            Root<DataFileCategory> dataFileCategoryRoot = criteriaQuery.from(DataFileCategory.class);
            predicates.add(criteriaBuilder.equal(dataFileCategoryRoot.get("name"), categoryName));
            predicates.add(dataFileCategoryRoot.in(fileMetadataRoot.get("fileCategories")));
        }
        String tabularTagName = searchCriteria.getTabularTagName();
        if (tabularTagName != null) {
            Root<DataFileTag> dataFileTagRoot = criteriaQuery.from(DataFileTag.class);
            predicates.add(criteriaBuilder.equal(dataFileTagRoot.get("type"), TagLabelToTypes.get(tabularTagName)));
            predicates.add(dataFileTagRoot.in(fileMetadataRoot.get("dataFile").get("dataFileTags")));
        }
        String searchText = searchCriteria.getSearchText();
        if (searchText != null && !searchText.isEmpty()) {
            searchText = searchText.trim().toLowerCase();
            predicates.add(criteriaBuilder.like(fileMetadataRoot.get("label"), "%" + searchText + "%"));
        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
    }

    private List<Order> createGetFileMetadatasOrder(CriteriaBuilder criteriaBuilder,
                                                    FileOrderCriteria orderCriteria,
                                                    Root<FileMetadata> fileMetadataRoot) {
        Path<Object> label = fileMetadataRoot.get("label");
        Path<Object> dataFile = fileMetadataRoot.get("dataFile");
        Path<Timestamp> publicationDate = dataFile.get("publicationDate");
        Path<Timestamp> createDate = dataFile.get("createDate");
        Expression<Object> orderByLifetimeExpression = criteriaBuilder.selectCase().when(publicationDate.isNotNull(), publicationDate).otherwise(createDate);
        List<Order> orderList = new ArrayList<>();
        switch (orderCriteria) {
            case NameZA -> orderList.add(criteriaBuilder.desc(label));
            case Newest -> orderList.add(criteriaBuilder.desc(orderByLifetimeExpression));
            case Oldest -> orderList.add(criteriaBuilder.asc(orderByLifetimeExpression));
            case Size -> orderList.add(criteriaBuilder.asc(dataFile.get("filesize")));
            case Type -> {
                orderList.add(criteriaBuilder.asc(dataFile.get("contentType")));
                orderList.add(criteriaBuilder.asc(label));
            }
            default -> orderList.add(criteriaBuilder.asc(label));
        }
        return orderList;
    }

    private long getOriginalTabularFilesSize(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Root<DataTable> dataTableRoot = criteriaQuery.from(DataTable.class);
        criteriaQuery
                .select(criteriaBuilder.sum(dataTableRoot.get("originalFileSize")))
                .where(criteriaBuilder.and(
                        criteriaBuilder.equal(dataTableRoot.get("dataFile"), fileMetadataRoot.get("dataFile")),
                        createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot)));
        Long result = em.createQuery(criteriaQuery).getSingleResult();
        return (result == null) ? 0 : result;
    }

    private long getArchivalFilesSize(DatasetVersion datasetVersion, boolean ignoreTabular, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Predicate searchCriteriaPredicate = createSearchCriteriaPredicate(datasetVersion, searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot);
        Predicate wherePredicate;
        if (ignoreTabular) {
            wherePredicate = criteriaBuilder.and(searchCriteriaPredicate, criteriaBuilder.isEmpty(fileMetadataRoot.get("dataFile").get("dataTables")));
        } else {
            wherePredicate = searchCriteriaPredicate;
        }
        criteriaQuery
                .select(criteriaBuilder.sum(fileMetadataRoot.get("dataFile").get("filesize")))
                .where(wherePredicate);
        Long result = em.createQuery(criteriaQuery).getSingleResult();
        return (result == null) ? 0 : result;
    }

    private Map<String, Long> getStringLongMapResultFromQuery(CriteriaQuery<Tuple> criteriaQuery) {
        List<Tuple> categoryNameOccurrences = em.createQuery(criteriaQuery).getResultList();
        Map<String, Long> result = new HashMap<>();
        for (Tuple occurrence : categoryNameOccurrences) {
            result.put(occurrence.get(0, String.class), occurrence.get(1, Long.class));
        }
        return result;
    }
}
