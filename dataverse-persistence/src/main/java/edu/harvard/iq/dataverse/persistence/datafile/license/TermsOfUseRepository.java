package edu.harvard.iq.dataverse.persistence.datafile.license;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class TermsOfUseRepository extends JpaRepository<Long, FileTermsOfUse> {

    public TermsOfUseRepository() {
        super(FileTermsOfUse.class);
    }

    public Optional<FileTermsOfUse> retrieveFirstFileTermsOfUse(Long datasetVersionId) {
        return getSingleResult(em.createQuery("SELECT termsOfUse FROM DatasetVersion dv JOIN dv.fileMetadatas file " +
                                                      "JOIN file.termsOfUse termsOfUse WHERE dv.id = :dvId ORDER BY file.label ASC",
                                              FileTermsOfUse.class)
                                 .setParameter("dvId", datasetVersionId)
                                 .setMaxResults(1));
    }
}
