package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.QDataFileCategory;
import edu.harvard.iq.dataverse.QDvObject;
import edu.harvard.iq.dataverse.QFileMetadata;

import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Guillermo Portas
 * TODO
 */
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
     * Returns a FileMetadata list of files in the specified DatasetVersion
     *
     * @param datasetVersion the DatasetVersion to access
     * @param limit          for pagination, can be null
     * @param offset         for pagination, can be null
     * @param contentType    for retrieving only files with this content type
     * @param orderCriteria  a FileMetadatasOrderCriteria to order the results
     * @return a FileMetadata list of the specified DatasetVersion
     */
    public List<FileMetadata> getFileMetadatas(DatasetVersion datasetVersion, Integer limit, Integer offset, String contentType, String fileAccess, String categoryName, FileMetadatasOrderCriteria orderCriteria) {
        JPAQuery<FileMetadata> query = createBaseQuery(datasetVersion, orderCriteria);

        if (contentType != null) {
            query.where(fileMetadata.dataFile.contentType.eq(contentType));
        }
//        TODO
//        if (categoryName != null) {
//            query.from(dataFileCategory).where(dataFileCategory.name.eq(categoryName).and(fileMetadata.fileCategories.contains(dataFileCategory)));
//        }

        applyOrderCriteriaToQuery(query, orderCriteria);

        if (limit != null) {
            query.limit(limit);
        }
        if (offset != null) {
            query.offset(offset);
        }

        return query.fetch();
    }

    private JPAQuery<FileMetadata> createBaseQuery(DatasetVersion datasetVersion, FileMetadatasOrderCriteria orderCriteria) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        if (orderCriteria == FileMetadatasOrderCriteria.Newest || orderCriteria == FileMetadatasOrderCriteria.Oldest) {
            return queryFactory.select(fileMetadata).from(fileMetadata, dvObject).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).where(dvObject.id.eq(fileMetadata.dataFile.id));
        }
        return queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()));
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
