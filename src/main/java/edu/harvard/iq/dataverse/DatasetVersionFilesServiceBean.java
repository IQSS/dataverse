package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.QDataFileCategory;
import edu.harvard.iq.dataverse.QDataFileTag;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.harvard.iq.dataverse.DataFileTag.TagLabelToTypes;

@Stateless
@Named
public class DatasetVersionFilesServiceBean implements Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private final QFileMetadata fileMetadata = QFileMetadata.fileMetadata;
    private final QDvObject dvObject = QDvObject.dvObject;
    private final QDataFileCategory dataFileCategory = QDataFileCategory.dataFileCategory;
    private final QDataFileTag dataFileTag = QDataFileTag.dataFileTag;

    /**
     * Different criteria to sort the results of FileMetadata queries used in {@link DatasetVersionFilesServiceBean#getFileMetadatas}
     */
    public enum FileMetadatasOrderCriteria {
        NameAZ, NameZA, Newest, Oldest, Size, Type
    }

    /**
     * Status of the particular DataFile based on active embargoes and restriction state used in {@link DatasetVersionFilesServiceBean#getFileMetadatas}
     */
    public enum DataFileAccessStatus {
        Public, Restricted, EmbargoedThenRestricted, EmbargoedThenPublic
    }

    /**
     * Given a DatasetVersion, returns its total file metadata count
     *
     * @param datasetVersion the DatasetVersion to access
     * @return long value of total file metadata count
     */
    public long getFileMetadataCount(DatasetVersion datasetVersion) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        return queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).stream().count();
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per content type
     *
     * @param datasetVersion the DatasetVersion to access
     * @return Map<String, Long> of file metadata counts per content type
     */
    public Map<String, Long> getFileMetadataCountPerContentType(DatasetVersion datasetVersion) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> contentTypeOccurrences = queryFactory
                .select(fileMetadata.dataFile.contentType, fileMetadata.count())
                .from(fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()))
                .groupBy(fileMetadata.dataFile.contentType).fetch();
        Map<String, Long> result = new HashMap<>();
        for (Tuple occurrence : contentTypeOccurrences) {
            result.put(occurrence.get(fileMetadata.dataFile.contentType), occurrence.get(fileMetadata.count()));
        }
        return result;
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per category name
     *
     * @param datasetVersion the DatasetVersion to access
     * @return Map<String, Long> of file metadata counts per category name
     */
    public Map<String, Long> getFileMetadataCountPerCategoryName(DatasetVersion datasetVersion) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> categoryNameOccurrences = queryFactory
                .select(dataFileCategory.name, fileMetadata.count())
                .from(dataFileCategory, fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()).and(fileMetadata.fileCategories.contains(dataFileCategory)))
                .groupBy(dataFileCategory.name).fetch();
        Map<String, Long> result = new HashMap<>();
        for (Tuple occurrence : categoryNameOccurrences) {
            result.put(occurrence.get(dataFileCategory.name), occurrence.get(fileMetadata.count()));
        }
        return result;
    }

    /**
     * Given a DatasetVersion, returns its file metadata count per DataFileAccessStatus
     *
     * @param datasetVersion the DatasetVersion to access
     * @return Map<DataFileAccessStatus, Long> of file metadata counts per DataFileAccessStatus
     */
    public Map<DataFileAccessStatus, Long> getFileMetadataCountPerAccessStatus(DatasetVersion datasetVersion) {
        Map<DataFileAccessStatus, Long> allCounts = new HashMap<>();
        addAccessStatusCountToTotal(datasetVersion, allCounts, DataFileAccessStatus.Public);
        addAccessStatusCountToTotal(datasetVersion, allCounts, DataFileAccessStatus.Restricted);
        addAccessStatusCountToTotal(datasetVersion, allCounts, DataFileAccessStatus.EmbargoedThenPublic);
        addAccessStatusCountToTotal(datasetVersion, allCounts, DataFileAccessStatus.EmbargoedThenRestricted);
        return allCounts;
    }

    /**
     * Returns a FileMetadata list of files in the specified DatasetVersion
     *
     * @param datasetVersion the DatasetVersion to access
     * @param limit          for pagination, can be null
     * @param offset         for pagination, can be null
     * @param contentType    for retrieving only files with this content type
     * @param accessStatus   for retrieving only files with this DataFileAccessStatus
     * @param categoryName   for retrieving only files categorized with this category name
     * @param tabularTagName for retrieving only files categorized with this tabular tag name
     * @param searchText     for retrieving only files that contain the specified text within their labels or descriptions
     * @param orderCriteria  a FileMetadatasOrderCriteria to order the results
     * @return a FileMetadata list from the specified DatasetVersion
     */
    public List<FileMetadata> getFileMetadatas(DatasetVersion datasetVersion, Integer limit, Integer offset, String contentType, DataFileAccessStatus accessStatus, String categoryName, String tabularTagName, String searchText, FileMetadatasOrderCriteria orderCriteria) {
        JPAQuery<FileMetadata> baseQuery = createGetFileMetadatasBaseQuery(datasetVersion, orderCriteria);

        if (contentType != null) {
            baseQuery.where(fileMetadata.dataFile.contentType.eq(contentType));
        }
        if (accessStatus != null) {
            baseQuery.where(createGetFileMetadatasAccessStatusExpression(accessStatus));
        }
        if (categoryName != null) {
            baseQuery.from(dataFileCategory).where(dataFileCategory.name.eq(categoryName).and(fileMetadata.fileCategories.contains(dataFileCategory)));
        }
        if (tabularTagName != null) {
            baseQuery.from(dataFileTag).where(dataFileTag.type.eq(TagLabelToTypes.get(tabularTagName)).and(fileMetadata.dataFile.dataFileTags.contains(dataFileTag)));
        }
        if (searchText != null && !searchText.isEmpty()) {
            searchText = searchText.trim().toLowerCase();
            baseQuery.where(fileMetadata.label.lower().contains(searchText).or(fileMetadata.description.lower().contains(searchText)));
        }

        applyOrderCriteriaToGetFileMetadatasQuery(baseQuery, orderCriteria);

        if (limit != null) {
            baseQuery.limit(limit);
        }
        if (offset != null) {
            baseQuery.offset(offset);
        }

        return baseQuery.fetch();
    }

    private void addAccessStatusCountToTotal(DatasetVersion datasetVersion, Map<DataFileAccessStatus, Long> totalCounts, DataFileAccessStatus dataFileAccessStatus) {
        long fileMetadataCount = getFileMetadataCountByAccessStatus(datasetVersion, dataFileAccessStatus);
        if (fileMetadataCount > 0) {
            totalCounts.put(dataFileAccessStatus, fileMetadataCount);
        }
    }

    private long getFileMetadataCountByAccessStatus(DatasetVersion datasetVersion, DataFileAccessStatus accessStatus) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        return queryFactory
                .selectFrom(fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()).and(createGetFileMetadatasAccessStatusExpression(accessStatus)))
                .stream().count();
    }

    private JPAQuery<FileMetadata> createGetFileMetadatasBaseQuery(DatasetVersion datasetVersion, FileMetadatasOrderCriteria orderCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<FileMetadata> baseQuery = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()));
        if (orderCriteria == FileMetadatasOrderCriteria.Newest || orderCriteria == FileMetadatasOrderCriteria.Oldest) {
            baseQuery.from(dvObject).where(dvObject.id.eq(fileMetadata.dataFile.id));
        }
        return baseQuery;
    }

    private BooleanExpression createGetFileMetadatasAccessStatusExpression(DataFileAccessStatus accessStatus) {
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

    private void applyOrderCriteriaToGetFileMetadatasQuery(JPAQuery<FileMetadata> query, FileMetadatasOrderCriteria orderCriteria) {
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
                query.orderBy(fileMetadata.dataFile.contentType.asc());
                break;
            default:
                query.orderBy(fileMetadata.label.asc());
                break;
        }
    }
}
