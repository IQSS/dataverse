package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import java.util.Optional;

@Singleton
public class DatasetVersionRepository extends JpaRepository<Long, DatasetVersion> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetVersionRepository() {
        super(DatasetVersion.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<DatasetVersion> findByDatasetIdAndVersionNumber(long datasetId, long majorVersionNumber) {
        return getSingleResult(em.createQuery(
                        "select v " +
                                "from DatasetVersion v " +
                                "where v.dataset.id = :datasetId " +
                                "and v.versionNumber= :majorVersionNumber " +
                                "order by v.minorVersionNumber desc",
                        DatasetVersion.class)
                .setParameter("datasetId", datasetId)
                .setParameter("majorVersionNumber", majorVersionNumber)
                .setMaxResults(1));
    }

    public Optional<DatasetVersion> findByDatasetIdAndVersionNumber(DatasetVersionIdentifier versionIdentifier) {
        return getSingleResult(em.createQuery(
                        "select v " +
                                "from DatasetVersion v " +
                                "where v.dataset.id = :datasetId " +
                                "and v.versionNumber= :versionNumber " +
                                "and v.minorVersionNumber= :minorVersionNumber",
                        DatasetVersion.class)
                .setParameter("datasetId", versionIdentifier.getDatasetId())
                .setParameter("versionNumber", versionIdentifier.getVersionNumber())
                .setParameter("minorVersionNumber", versionIdentifier.getMinorVersionNumber()));
    }
}
