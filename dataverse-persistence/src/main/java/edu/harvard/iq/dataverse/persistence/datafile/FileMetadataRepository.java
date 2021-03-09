package edu.harvard.iq.dataverse.persistence.datafile;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;

@Stateless
public class FileMetadataRepository extends JpaRepository<Long, FileMetadata> {

    // -------------------- CONSTRUCTORS --------------------

    public FileMetadataRepository() {
        super(FileMetadata.class);
    }

    // -------------------- LOGIC --------------------

    /**
     * Retrieves fileMetadata with pagination.
     *
     * @param pageNumber page number that starts with 0 (important for calculation).
     * @return List of fileMetadata
     */
    public List<FileMetadata> findFileMetadataByDatasetVersionIdWithPagination(long dsvId, int pageNumber, int amountToFetch) {
        return em.createQuery("SELECT f FROM FileMetadata f JOIN f.datasetVersion v " +
                                      " WHERE v.id = :dsvId ORDER BY f.displayOrder", FileMetadata.class)
                 .setParameter("dsvId", dsvId)
                 .setFirstResult(pageNumber * amountToFetch)
                 .setMaxResults(amountToFetch)
                 .getResultList();
    }

    /**
     * Retrieves fileMetadata with pagination and search term.
     *
     * @param pageNumber page number that starts with 0 (important for calculation).
     * @return List of fileMetadata
     */
    public List<FileMetadata> findSearchedFileMetadataByDatasetVersionIdWithPagination(long dsvId, int pageNumber, int amountToFetch, String searchTerm) {
        return em.createQuery("SELECT f FROM FileMetadata f JOIN f.datasetVersion v " +
                                      " WHERE v.id = :dsvId AND (lower(f.label) LIKE :searchTerm OR lower(f.description) LIKE :searchTerm)" +
                                      " ORDER BY f.displayOrder asc", FileMetadata.class)
                 .setParameter("dsvId", dsvId)
                 .setParameter("searchTerm", "%" + searchTerm + "%")
                 .setFirstResult(pageNumber * amountToFetch)
                 .setMaxResults(amountToFetch)
                 .getResultList();
    }

    /**
     * Finds fileMetadata ids attached to dataset version.
     */
    public List<Long> findFileMetadataIdsByDatasetVersionId(long dsvId) {
        return em.createQuery("SELECT f.id FROM FileMetadata f JOIN f.datasetVersion v WHERE v.id = :dsvId", Long.class)
                 .setParameter("dsvId", dsvId)
                 .getResultList();
    }

    /**
     * Finds files with provided id's along with cache enabled.
     */
    public List<FileMetadata> findFileMetadata(List<Long> fileMetadataIds) {
        return em.createQuery("SELECT f FROM FileMetadata f WHERE f.id IN :fileMetadatas", FileMetadata.class)
                 .setParameter("fileMetadatas", fileMetadataIds)
                 .setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE")
                 .getResultList();
    }

    /**
     * Finds files which are restricted by license.
     */
    public List<FileMetadata> findRestrictedFileMetadata(Collection<Long> filteredFileIds) {
        return em.createQuery("SELECT f" +
                                      " FROM FileMetadata f JOIN f.termsOfUse t " +
                                      "WHERE f.id IN :fileIds AND  t.restrictType != null", FileMetadata.class)
                 .setParameter("fileIds", filteredFileIds)
                 .getResultList();
    }
}
