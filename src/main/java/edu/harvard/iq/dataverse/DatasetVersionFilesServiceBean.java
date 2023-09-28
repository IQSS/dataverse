package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.QDataFileCategory;
import edu.harvard.iq.dataverse.QDvObject;
import edu.harvard.iq.dataverse.QEmbargo;
import edu.harvard.iq.dataverse.QFileMetadata;

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
import java.util.List;

@Stateless
@Named
public class DatasetVersionFilesServiceBean implements Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private final QFileMetadata fileMetadata = QFileMetadata.fileMetadata;
    private final QDvObject dvObject = QDvObject.dvObject;
    private final QDataFileCategory dataFileCategory = QDataFileCategory.dataFileCategory;

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
     * Returns a FileMetadata list of files in the specified DatasetVersion
     *
     * @param datasetVersion the DatasetVersion to access
     * @param limit          for pagination, can be null
     * @param offset         for pagination, can be null
     * @param contentType    for retrieving only files with this content type
     * @param accessStatus   for retrieving only files with this DataFileAccessStatus
     * @param categoryName   for retrieving only files categorized with this category name
     * @param searchText     for retrieving only files that contain the specified text within their labels or descriptions
     * @param orderCriteria  a FileMetadatasOrderCriteria to order the results
     * @return a FileMetadata list from the specified DatasetVersion
     */
    public List<FileMetadata> getFileMetadatas(DatasetVersion datasetVersion, Integer limit, Integer offset, String contentType, DataFileAccessStatus accessStatus, String categoryName, String searchText, FileMetadatasOrderCriteria orderCriteria) {
        JPAQuery<FileMetadata> baseQuery = createBaseQuery(datasetVersion, orderCriteria);

        if (contentType != null) {
            baseQuery.where(fileMetadata.dataFile.contentType.eq(contentType));
        }
        if (accessStatus != null) {
            baseQuery.where(createAccessStatusExpression(accessStatus));
        }
        if (categoryName != null) {
            baseQuery.from(dataFileCategory).where(dataFileCategory.name.eq(categoryName).and(fileMetadata.fileCategories.contains(dataFileCategory)));
        }
        if (searchText != null && !searchText.isEmpty()) {
            searchText = searchText.trim().toLowerCase();
            baseQuery.where(fileMetadata.label.lower().contains(searchText).or(fileMetadata.description.lower().contains(searchText)));
        }

        applyOrderCriteriaToQuery(baseQuery, orderCriteria);

        if (limit != null) {
            baseQuery.limit(limit);
        }
        if (offset != null) {
            baseQuery.offset(offset);
        }

        return baseQuery.fetch();
    }

    private JPAQuery<FileMetadata> createBaseQuery(DatasetVersion datasetVersion, FileMetadatasOrderCriteria orderCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<FileMetadata> baseQuery = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()));
        if (orderCriteria == FileMetadatasOrderCriteria.Newest || orderCriteria == FileMetadatasOrderCriteria.Oldest) {
            baseQuery.from(dvObject).where(dvObject.id.eq(fileMetadata.dataFile.id));
        }
        return baseQuery;
    }

    private BooleanExpression createAccessStatusExpression(DataFileAccessStatus accessStatus) {
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

    private void applyOrderCriteriaToQuery(JPAQuery<FileMetadata> query, FileMetadatasOrderCriteria orderCriteria) {
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
