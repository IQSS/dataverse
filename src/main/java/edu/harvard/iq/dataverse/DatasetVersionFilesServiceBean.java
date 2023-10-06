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

import edu.harvard.iq.dataverse.FileSearchCriteria.FileAccessStatus;

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
    public enum FileOrderCriteria {
        NameAZ, NameZA, Newest, Oldest, Size, Type
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
     * Given a DatasetVersion, returns its file metadata count per FileAccessStatus
     *
     * @param datasetVersion the DatasetVersion to access
     * @return Map<FileAccessStatus, Long> of file metadata counts per FileAccessStatus
     */
    public Map<FileAccessStatus, Long> getFileMetadataCountPerAccessStatus(DatasetVersion datasetVersion) {
        Map<FileAccessStatus, Long> allCounts = new HashMap<>();
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.Public);
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.Restricted);
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.EmbargoedThenPublic);
        addAccessStatusCountToTotal(datasetVersion, allCounts, FileAccessStatus.EmbargoedThenRestricted);
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

        applyOrderCriteriaToGetFileMetadatasQuery(baseQuery, orderCriteria);

        if (limit != null) {
            baseQuery.limit(limit);
        }
        if (offset != null) {
            baseQuery.offset(offset);
        }

        return baseQuery.fetch();
    }

    private void addAccessStatusCountToTotal(DatasetVersion datasetVersion, Map<FileAccessStatus, Long> totalCounts, FileAccessStatus dataFileAccessStatus) {
        long fileMetadataCount = getFileMetadataCountByAccessStatus(datasetVersion, dataFileAccessStatus);
        if (fileMetadataCount > 0) {
            totalCounts.put(dataFileAccessStatus, fileMetadataCount);
        }
    }

    private long getFileMetadataCountByAccessStatus(DatasetVersion datasetVersion, FileAccessStatus accessStatus) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        return queryFactory
                .selectFrom(fileMetadata)
                .where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()).and(createGetFileMetadatasAccessStatusExpression(accessStatus)))
                .stream().count();
    }

    private JPAQuery<FileMetadata> createGetFileMetadatasBaseQuery(DatasetVersion datasetVersion, FileOrderCriteria orderCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<FileMetadata> baseQuery = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()));
        if (orderCriteria == FileOrderCriteria.Newest || orderCriteria == FileOrderCriteria.Oldest) {
            baseQuery.from(dvObject).where(dvObject.id.eq(fileMetadata.dataFile.id));
        }
        return baseQuery;
    }

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
                query.orderBy(fileMetadata.dataFile.contentType.asc());
                break;
            default:
                query.orderBy(fileMetadata.label.asc());
                break;
        }
    }
}
