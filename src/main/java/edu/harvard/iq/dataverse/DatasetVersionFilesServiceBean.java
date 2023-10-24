package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.QDataFileCategory;
import edu.harvard.iq.dataverse.QDataFileTag;
import edu.harvard.iq.dataverse.QDataTable;
import edu.harvard.iq.dataverse.QDvObject;
import edu.harvard.iq.dataverse.QEmbargo;
import edu.harvard.iq.dataverse.QFileMetadata;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static edu.harvard.iq.dataverse.DataFileTag.TagLabelToTypes;

import edu.harvard.iq.dataverse.FileSearchCriteria.FileAccessStatus;
import jakarta.persistence.criteria.*;

@Stateless
@Named
public class DatasetVersionFilesServiceBean implements Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private final QFileMetadata fileMetadata = QFileMetadata.fileMetadata;
    private final QDvObject dvObject = QDvObject.dvObject;
    private final QDataFileCategory dataFileCategory = QDataFileCategory.dataFileCategory;
    private final QDataFileTag dataFileTag = QDataFileTag.dataFileTag;
    private final QDataTable dataTable = QDataTable.dataTable;

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
     * @param searchCriteria for counting only files matching this criteria
     * @return long value of total file metadata count
     */
    public long getFileMetadataCount(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Predicate basePredicate = criteriaBuilder.equal(fileMetadataRoot.get("datasetVersion").<String>get("id"), datasetVersion.getId());
        Predicate searchCriteriaPredicate = createSearchCriteriaPredicate(searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot);
        criteriaQuery.select(criteriaBuilder.count(fileMetadataRoot)).where(criteriaBuilder.and(basePredicate, searchCriteriaPredicate));
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
        CriteriaQuery<jakarta.persistence.Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<FileMetadata> fileMetadataRoot = criteriaQuery.from(FileMetadata.class);
        Path<String> contentType = fileMetadataRoot.get("dataFile").get("contentType");
        Predicate basePredicate = criteriaBuilder.equal(fileMetadataRoot.get("datasetVersion").<String>get("id"), datasetVersion.getId());
        Predicate searchCriteriaPredicate = createSearchCriteriaPredicate(searchCriteria, criteriaBuilder, criteriaQuery, fileMetadataRoot);
        criteriaQuery.multiselect(contentType, criteriaBuilder.count(contentType)).where(criteriaBuilder.and(basePredicate, searchCriteriaPredicate)).groupBy(contentType);
        List<jakarta.persistence.Tuple> contentTypeOccurrences = em.createQuery(criteriaQuery).getResultList();
        Map<String, Long> result = new HashMap<>();
        for (jakarta.persistence.Tuple occurrence : contentTypeOccurrences) {
            result.put(occurrence.get(0, String.class), occurrence.get(1, Long.class));
        }
        return result;
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per category name
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for counting only files matching this criteria
     * @return Map<String, Long> of file metadata counts per category name
     */
    public Map<String, Long> getFileMetadataCountPerCategoryName(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<Tuple> baseQuery = queryFactory
                .select(dataFileCategory.name, fileMetadata.count())
                .from(dataFileCategory, fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()).and(fileMetadata.fileCategories.contains(dataFileCategory)))
                .groupBy(dataFileCategory.name);
        applyFileSearchCriteriaToQuery(baseQuery, searchCriteria);
        List<Tuple> categoryNameOccurrences = baseQuery.fetch();
        Map<String, Long> result = new HashMap<>();
        for (Tuple occurrence : categoryNameOccurrences) {
            result.put(occurrence.get(dataFileCategory.name), occurrence.get(fileMetadata.count()));
        }
        return result;
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per DataFileTag.TagType
     *
     * @param datasetVersion the DatasetVersion to access
     * @param searchCriteria for counting only files matching this criteria
     * @return Map<DataFileTag.TagType, Long> of file metadata counts per DataFileTag.TagType
     */
    public Map<DataFileTag.TagType, Long> getFileMetadataCountPerTabularTagName(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<Tuple> baseQuery = queryFactory
                .select(dataFileTag.type, fileMetadata.count())
                .from(dataFileTag, fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()).and(fileMetadata.dataFile.dataFileTags.contains(dataFileTag)))
                .groupBy(dataFileTag.type);
        applyFileSearchCriteriaToQuery(baseQuery, searchCriteria);
        List<Tuple> tagNameOccurrences = baseQuery.fetch();
        Map<DataFileTag.TagType, Long> result = new HashMap<>();
        for (Tuple occurrence : tagNameOccurrences) {
            result.put(occurrence.get(dataFileTag.type), occurrence.get(fileMetadata.count()));
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
        JPAQuery<FileMetadata> baseQuery = createGetFileMetadatasBaseQuery(datasetVersion, orderCriteria);
        applyFileSearchCriteriaToQuery(baseQuery, searchCriteria);
        applyOrderCriteriaToGetFileMetadatasQuery(baseQuery, orderCriteria);
        if (limit != null) {
            baseQuery.limit(limit);
        }
        if (offset != null) {
            baseQuery.offset(offset);
        }
        return baseQuery.fetch();
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

    private void addAccessStatusCountToTotal(DatasetVersion datasetVersion, Map<FileAccessStatus, Long> totalCounts, FileAccessStatus dataFileAccessStatus, FileSearchCriteria searchCriteria) {
        long fileMetadataCount = getFileMetadataCountByAccessStatus(datasetVersion, dataFileAccessStatus, searchCriteria);
        if (fileMetadataCount > 0) {
            totalCounts.put(dataFileAccessStatus, fileMetadataCount);
        }
    }

    private long getFileMetadataCountByAccessStatus(DatasetVersion datasetVersion, FileAccessStatus accessStatus, FileSearchCriteria searchCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<FileMetadata> baseQuery = queryFactory
                .selectFrom(fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()).and(createGetFileMetadatasAccessStatusExpression(accessStatus)));
        applyFileSearchCriteriaToQuery(baseQuery, searchCriteria);
        return baseQuery.stream().count();
    }

    private JPAQuery<FileMetadata> createGetFileMetadatasBaseQuery(DatasetVersion datasetVersion, FileOrderCriteria orderCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<FileMetadata> baseQuery = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()));
        if (orderCriteria == FileOrderCriteria.Newest || orderCriteria == FileOrderCriteria.Oldest) {
            baseQuery.from(dvObject).where(dvObject.id.eq(fileMetadata.dataFile.id));
        }
        return baseQuery;
    }

    private Predicate createSearchCriteriaAccessStatusPredicate(FileAccessStatus accessStatus, CriteriaBuilder criteriaBuilder, Root<FileMetadata> fileMetadataRoot) {
        Path<Object> dataFile = fileMetadataRoot.get("dataFile");
        Path<Object> embargo = dataFile.get("embargo");
        Predicate activelyEmbargoedPredicate = criteriaBuilder.greaterThanOrEqualTo(embargo.<Date>get("dateAvailable"), criteriaBuilder.currentDate());
        Predicate inactivelyEmbargoedPredicate = criteriaBuilder.isNull(embargo);
        Path<Boolean> isRestricted = dataFile.get("restricted");
        Predicate isRestrictedPredicate = criteriaBuilder.isTrue(isRestricted);
        Predicate isUnrestrictedPredicate = criteriaBuilder.isFalse(isRestricted);
        return switch (accessStatus) {
            case EmbargoedThenRestricted -> criteriaBuilder.and(activelyEmbargoedPredicate, isRestrictedPredicate);
            case EmbargoedThenPublic -> criteriaBuilder.and(activelyEmbargoedPredicate, isUnrestrictedPredicate);
            case Restricted -> criteriaBuilder.and(inactivelyEmbargoedPredicate, isRestrictedPredicate);
            case Public -> criteriaBuilder.and(inactivelyEmbargoedPredicate, isUnrestrictedPredicate);
        };
    }

    @Deprecated
    private BooleanExpression createGetFileMetadatasAccessStatusExpression(FileAccessStatus accessStatus) {
        QEmbargo embargo = fileMetadata.dataFile.embargo;
        BooleanExpression activelyEmbargoedExpression = embargo.dateAvailable.goe(DateExpression.currentDate(LocalDate.class));
        BooleanExpression inactivelyEmbargoedExpression = embargo.isNull();
        BooleanExpression accessStatusExpression;
        switch (accessStatus) {
            case EmbargoedThenRestricted:
                accessStatusExpression = activelyEmbargoedExpression.and(fileMetadata.dataFile.restricted.isTrue());
                break;
            case EmbargoedThenPublic:
                accessStatusExpression = activelyEmbargoedExpression.and(fileMetadata.dataFile.restricted.isFalse());
                break;
            case Restricted:
                accessStatusExpression = inactivelyEmbargoedExpression.and(fileMetadata.dataFile.restricted.isTrue());
                break;
            case Public:
                accessStatusExpression = inactivelyEmbargoedExpression.and(fileMetadata.dataFile.restricted.isFalse());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + accessStatus);
        }
        return accessStatusExpression;
    }

    private Predicate createSearchCriteriaPredicate(FileSearchCriteria searchCriteria, CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Root<FileMetadata> fileMetadataRoot) {
        List<Predicate> predicates = new ArrayList<>();
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

    @Deprecated
    private void applyFileSearchCriteriaToQuery(JPAQuery<?> baseQuery, FileSearchCriteria searchCriteria) {
        String contentType = searchCriteria.getContentType();
        if (contentType != null) {
            baseQuery.where(fileMetadata.dataFile.contentType.eq(contentType));
        }
        FileAccessStatus accessStatus = searchCriteria.getAccessStatus();
        if (accessStatus != null) {
            baseQuery.where(createGetFileMetadatasAccessStatusExpression(accessStatus));
        }
        String categoryName = searchCriteria.getCategoryName();
        if (categoryName != null) {
            baseQuery.from(dataFileCategory).where(dataFileCategory.name.eq(categoryName).and(fileMetadata.fileCategories.contains(dataFileCategory)));
        }
        String tabularTagName = searchCriteria.getTabularTagName();
        if (tabularTagName != null) {
            baseQuery.from(dataFileTag).where(dataFileTag.type.eq(TagLabelToTypes.get(tabularTagName)).and(fileMetadata.dataFile.dataFileTags.contains(dataFileTag)));
        }
        String searchText = searchCriteria.getSearchText();
        if (searchText != null && !searchText.isEmpty()) {
            searchText = searchText.trim().toLowerCase();
            baseQuery.where(fileMetadata.label.lower().contains(searchText).or(fileMetadata.description.lower().contains(searchText)));
        }
    }

    private void applyOrderCriteriaToGetFileMetadatasQuery(JPAQuery<FileMetadata> query, FileOrderCriteria orderCriteria) {
        DateTimeExpression<Timestamp> orderByLifetimeExpression = new CaseBuilder().when(dvObject.publicationDate.isNotNull()).then(dvObject.publicationDate).otherwise(dvObject.createDate);
        switch (orderCriteria) {
            case NameZA:
                query.orderBy(fileMetadata.label.desc());
                break;
            case Newest:
                query.orderBy(orderByLifetimeExpression.desc());
                break;
            case Oldest:
                query.orderBy(orderByLifetimeExpression.asc());
                break;
            case Size:
                query.orderBy(fileMetadata.dataFile.filesize.asc());
                break;
            case Type:
                query.orderBy(fileMetadata.dataFile.contentType.asc(), fileMetadata.label.asc());
                break;
            default:
                query.orderBy(fileMetadata.label.asc());
                break;
        }
    }

    private long getOriginalTabularFilesSize(DatasetVersion datasetVersion, FileSearchCriteria searchCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<?> baseQuery = queryFactory
                .from(fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()))
                .from(dataTable)
                .where(dataTable.dataFile.eq(fileMetadata.dataFile));
        applyFileSearchCriteriaToQuery(baseQuery, searchCriteria);
        Long result = baseQuery.select(dataTable.originalFileSize.sum()).fetchFirst();
        return (result == null) ? 0 : result;
    }

    private long getArchivalFilesSize(DatasetVersion datasetVersion, boolean ignoreTabular, FileSearchCriteria searchCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<?> baseQuery = queryFactory
                .from(fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()));
        applyFileSearchCriteriaToQuery(baseQuery, searchCriteria);
        Long result;
        if (ignoreTabular) {
            result = baseQuery.where(fileMetadata.dataFile.dataTables.isEmpty()).select(fileMetadata.dataFile.filesize.sum()).fetchFirst();
        } else {
            result = baseQuery.select(fileMetadata.dataFile.filesize.sum()).fetchFirst();
        }
        return (result == null) ? 0 : result;
    }
}
