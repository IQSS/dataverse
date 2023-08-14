package edu.harvard.iq.dataverse;

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
    public List<FileMetadata> getFileMetadatas(DatasetVersion datasetVersion, Integer limit, Integer offset, String contentType, String fileAccess, String fileTag, FileMetadatasOrderCriteria orderCriteria) {
        JPAQuery<FileMetadata> query = createQueryFromOrderCriteria(datasetVersion, orderCriteria);
        if (contentType != null) {
            query.where(fileMetadata.dataFile.contentType.eq(contentType));
        }
        if (limit != null) {
            query.limit(limit);
        }
        if (offset != null) {
            query.offset(offset);
        }
        return query.fetch();
    }

    private JPAQuery<FileMetadata> createQueryFromOrderCriteria(DatasetVersion datasetVersion, FileMetadatasOrderCriteria orderCriteria) {
        DateTimeExpression<Timestamp> orderByLifetimeExpression = new CaseBuilder().when(dvObject.publicationDate.isNotNull()).then(dvObject.publicationDate).otherwise(dvObject.createDate);
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<FileMetadata> query;
        switch (orderCriteria) {
            case NameZA:
                query = createFileMetadataBaseQuery(datasetVersion, queryFactory).orderBy(fileMetadata.label.desc());
                break;
            case Newest:
                query = createFileMetadataDvObjectBaseQuery(datasetVersion, queryFactory).orderBy(orderByLifetimeExpression.desc());
                break;
            case Oldest:
                query = createFileMetadataDvObjectBaseQuery(datasetVersion, queryFactory).orderBy(orderByLifetimeExpression.asc());
                break;
            case Size:
                query = createFileMetadataBaseQuery(datasetVersion, queryFactory).orderBy(fileMetadata.dataFile.filesize.asc());
                break;
            case Type:
                query = createFileMetadataBaseQuery(datasetVersion, queryFactory).orderBy(fileMetadata.dataFile.contentType.asc());
                break;
            default:
                query = createFileMetadataBaseQuery(datasetVersion, queryFactory).orderBy(fileMetadata.label.asc());
                break;
        }
        return query;
    }

    private JPAQuery<FileMetadata> createFileMetadataDvObjectBaseQuery(DatasetVersion datasetVersion, JPAQueryFactory queryFactory) {
        return queryFactory.select(fileMetadata).from(fileMetadata, dvObject).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).where(dvObject.id.eq(fileMetadata.dataFile.id));
    }

    private JPAQuery<FileMetadata> createFileMetadataBaseQuery(DatasetVersion datasetVersion, JPAQueryFactory queryFactory) {
        return queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId()));
    }
}
