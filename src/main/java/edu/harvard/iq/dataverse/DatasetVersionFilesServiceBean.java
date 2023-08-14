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
     * @param orderCriteria  a FileMetadatasOrderCriteria to order the results
     * @return a FileMetadata list of the specified DatasetVersion
     */
    public List<FileMetadata> getFileMetadatas(DatasetVersion datasetVersion, Integer limit, Integer offset, String fileType, String fileAccess, String fileTag, FileMetadatasOrderCriteria orderCriteria) {
        JPAQuery<FileMetadata> query = createQueryFromCriteria(datasetVersion, orderCriteria);
        if (limit != null) {
            query.limit(limit);
        }
        if (offset != null) {
            query.offset(offset);
        }
        return query.fetch();
    }

    // TODO: Refactor
    private JPAQuery<FileMetadata> createQueryFromCriteria(DatasetVersion datasetVersion, FileMetadatasOrderCriteria orderCriteria) {
        QFileMetadata fileMetadata = QFileMetadata.fileMetadata;
        QDvObject dvObject = QDvObject.dvObject;
        DateTimeExpression<Timestamp> orderByLifetimeExpression = new CaseBuilder().when(dvObject.publicationDate.isNotNull()).then(dvObject.publicationDate).otherwise(dvObject.createDate);
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        JPAQuery<FileMetadata> query;
        switch (orderCriteria) {
            case NameZA:
                query = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).orderBy(fileMetadata.label.desc());
                break;
            case Newest:
                query = queryFactory.select(fileMetadata).from(fileMetadata, dvObject).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).where(dvObject.id.eq(fileMetadata.dataFile.id)).orderBy(orderByLifetimeExpression.desc());
                break;
            case Oldest:
                query = queryFactory.select(fileMetadata).from(fileMetadata, dvObject).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).where(dvObject.id.eq(fileMetadata.dataFile.id)).orderBy(orderByLifetimeExpression.asc());
                break;
            case Size:
                query = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).orderBy(fileMetadata.dataFile.filesize.asc());
                break;
            case Type:
                query = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).orderBy(fileMetadata.dataFile.contentType.asc());
                break;
            default:
                query = queryFactory.selectFrom(fileMetadata).where(fileMetadata.datasetVersion.id.eq(datasetVersion.getId())).orderBy(fileMetadata.label.asc());
                break;
        }
        return query;
    }
} // end class
