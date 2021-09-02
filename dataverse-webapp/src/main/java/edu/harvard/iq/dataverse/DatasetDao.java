package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.lang.RandomStringUtils;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author skraffmiller
 */


@Stateless
public class DatasetDao implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetDao.class.getCanonicalName());

    @Inject
    SettingsServiceBean settingsService;

    @EJB
    DvObjectServiceBean dvObjectService;

    @EJB
    AuthenticationServiceBean authentication;

    @EJB
    DataFileServiceBean fileService;

    @EJB
    EjbDataverseEngine commandEngine;

    @EJB
    private DatasetRepository datasetRepository;

    @EJB
    private DatasetThumbnailService datasetThumbnailService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    public Dataset find(Object pk) {
        return em.find(Dataset.class, pk);
    }

    public List<Dataset> findByOwnerId(Long ownerId) {
        return datasetRepository.findByOwnerId(ownerId);
    }

    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id", Dataset.class).getResultList();
    }


    public List<Long> findAllLocalDatasetIds() {
        return em.createQuery("SELECT o.id FROM Dataset o WHERE o.harvestedFrom IS null ORDER BY o.id", Long.class).getResultList();
    }

    public List<Long> findAllUnindexed() {
        return em.createQuery("SELECT o.id FROM Dataset o WHERE o.indexTime IS null ORDER BY o.id DESC", Long.class).getResultList();
    }

    /**
     * For docs, see the equivalent method on the DataverseServiceBean.
     *
     * @param numPartitions
     * @param partitionId
     * @param skipIndexed
     * @return a list of datasets
     * @see DataverseDao#findAllOrSubset(long, long, boolean)
     */
    public List<Long> findAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed) {
        if (numPartitions < 1) {
            long saneNumPartitions = 1;
            numPartitions = saneNumPartitions;
        }
        String skipClause = skipIndexed ? "AND o.indexTime is null " : "";
        TypedQuery<Long> typedQuery = em.createQuery("SELECT o.id FROM Dataset o WHERE MOD( o.id, :numPartitions) = :partitionId " +
                                                             skipClause +
                                                             "ORDER BY o.id", Long.class);
        typedQuery.setParameter("numPartitions", numPartitions);
        typedQuery.setParameter("partitionId", partitionId);
        return typedQuery.getResultList();
    }

    /**
     * Merges the passed dataset to the persistence context.
     *
     * @param ds the dataset whose new state we want to persist.
     * @return The managed entity representing {@code ds}.
     */
    public Dataset merge(Dataset ds) {
        return em.merge(ds);
    }

    public Dataset mergeAndFlush(Dataset ds) {
        Dataset merged = em.merge(ds);
        em.flush();
        return merged;
    }

    public Dataset findByGlobalId(String globalId) {
        Dataset retVal = (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset");
        if (retVal != null) {
            return retVal;
        } else {
            //try to find with alternative PID
            return (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset", true);
        }
    }

    public String generateDatasetIdentifier(Dataset dataset) {
        String identifierType = settingsService.getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle);
        String shoulder = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder);

        switch (identifierType) {
            case "randomString":
                return generateIdentifierAsRandomString(dataset, shoulder);
            case "sequentialNumber":
                return generateIdentifierAsSequentialNumber(dataset, shoulder);
            default:
                /* Should we throw an exception instead?? -- L.A. 4.6.2 */
                return generateIdentifierAsRandomString(dataset, shoulder);
        }
    }

    private String generateIdentifierAsRandomString(Dataset dataset, String shoulder) {
        String identifier = null;
        do {
            identifier = shoulder + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (!isIdentifierLocallyUnique(identifier, dataset));

        return identifier;
    }

    private String generateIdentifierAsSequentialNumber(Dataset dataset, String shoulder) {

        String identifier;
        do {
            StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery("Dataset.generateIdentifierAsSequentialNumber");
            query.execute();
            Integer identifierNumeric = (Integer) query.getOutputParameterValue(1);
            // some diagnostics here maybe - is it possible to determine that it's failing
            // because the stored procedure hasn't been created in the database?
            if (identifierNumeric == null) {
                return null;
            }
            identifier = shoulder + identifierNumeric.toString();
        } while (!isIdentifierLocallyUnique(identifier, dataset));

        return identifier;
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network) also check for duplicate
     * in EZID if needed
     *
     * @param userIdentifier
     * @param dataset
     * @param persistentIdSvc
     * @return {@code true} if the identifier is unique, {@code false} otherwise.
     */
    public boolean isIdentifierUnique(String userIdentifier, Dataset dataset, GlobalIdServiceBean persistentIdSvc) {
        if (!isIdentifierLocallyUnique(userIdentifier, dataset)) {
            return false; // duplication found in local database
        }

        // not in local DB, look in the persistent identifier service
        try {
            return !persistentIdSvc.alreadyExists(dataset);
        } catch (Exception e) {
            //we can live with failure - means identifier not found remotely
        }

        return true;
    }

    public boolean isIdentifierLocallyUnique(Dataset dataset) {
        return isIdentifierLocallyUnique(dataset.getIdentifier(), dataset);
    }

    public boolean isIdentifierLocallyUnique(String identifier, Dataset dataset) {
        return em.createNamedQuery("Dataset.findByIdentifierAuthorityProtocol")
                .setParameter("identifier", identifier)
                .setParameter("authority", dataset.getAuthority())
                .setParameter("protocol", dataset.getProtocol())
                .getResultList().isEmpty();
    }

    public DatasetVersion storeVersion(DatasetVersion dsv) {
        em.persist(dsv);
        return dsv;
    }


    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {

        TypedQuery<DatasetVersionUser> query = em.createNamedQuery("DatasetVersionUser.findByVersionIdAndUserId", DatasetVersionUser.class);
        query.setParameter("versionId", version.getId());
        String identifier = user.getIdentifier();
        identifier = identifier.startsWith("@") ? identifier.substring(1) : identifier;
        AuthenticatedUser au = authentication.getAuthenticatedUser(identifier);
        query.setParameter("userId", au.getId());
        try {
            return query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }

    public List<DatasetVersionUser> getDatasetVersionUsersByAuthenticatedUser(AuthenticatedUser user){

        TypedQuery<DatasetVersionUser> typedQuery =
                em.createQuery("SELECT u from DatasetVersionUser u where u.authenticatedUser.id = :authenticatedUserId",
                        DatasetVersionUser.class);
        typedQuery.setParameter("authenticatedUserId", user.getId());
        return typedQuery.getResultList();
    }

    public boolean checkDatasetLock(Long datasetId) {
        TypedQuery<DatasetLock> lockCounter = em.createNamedQuery("DatasetLock.getLocksByDatasetId", DatasetLock.class);
        lockCounter.setParameter("datasetId", datasetId);
        lockCounter.setMaxResults(1);
        List<DatasetLock> lock = lockCounter.getResultList();
        return lock.size() > 0;
    }

    public List<DatasetLock> getDatasetLocksByUser( AuthenticatedUser user) {

        TypedQuery<DatasetLock> query =
                em.createNamedQuery("DatasetLock.getLocksByAuthenticatedUserId", DatasetLock.class);
        query.setParameter("authenticatedUserId", user.getId());
        try {
            return query.getResultList();
        } catch (javax.persistence.NoResultException e) {
            return Collections.emptyList();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DatasetLock addDatasetLock(Dataset dataset, DatasetLock lock) {
        lock.setDataset(dataset);
        dataset.addLock(lock);
        lock.setStartTime(new Date());
        em.persist(lock);
        em.merge(dataset);
        return lock;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) /*?*/
    public DatasetLock addDatasetLock(Long datasetId, DatasetLock.Reason reason, Long userId, String info) {

        Dataset dataset = em.find(Dataset.class, datasetId);

        AuthenticatedUser user = null;
        if (userId != null) {
            user = em.find(AuthenticatedUser.class, userId);
        }

        // Check if the dataset is already locked for this reason:
        // (to prevent multiple, duplicate locks on the dataset!)
        DatasetLock lock = dataset.getLockFor(reason);
        if (lock != null) {
            return lock;
        }

        // Create new:
        lock = new DatasetLock(reason, user);
        lock.setDataset(dataset);
        lock.setInfo(info);
        lock.setStartTime(new Date());

        if (userId != null) {
            lock.setUser(user);
            if (user.getDatasetLocks() == null) {
                user.setDatasetLocks(new ArrayList<>());
            }
            user.getDatasetLocks().add(lock);
        }

        return addDatasetLock(dataset, lock);
    }

    /**
     * Removes all {@link DatasetLock}s for the dataset whose id is passed and reason
     * is {@code aReason}.
     *
     * @param dataset the dataset whose locks (for {@code aReason}) will be removed.
     * @param aReason The reason of the locks that will be removed.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeDatasetLocks(Dataset dataset, DatasetLock.Reason aReason) {
        if (dataset != null) {
            new HashSet<>(dataset.getLocks()).stream()
                    .filter(l -> l.getReason() == aReason)
                    .forEach(lock -> {
                        lock = em.merge(lock);
                        dataset.removeLock(lock);

                        AuthenticatedUser user = lock.getUser();
                        user.getDatasetLocks().remove(lock);

                        em.remove(lock);
                    });
        }
    }

    /*
    getTitleFromLatestVersion methods use native query to return a dataset title

        There are two versions:
     1) The version with datasetId param only will return the title regardless of version state
     2)The version with the param 'includeDraft' boolean  will return the most recently published title if the param is set to false
    If no Title found return empty string - protects against calling with
    include draft = false with no published version
    */

    public String getTitleFromLatestVersion(Long datasetId) {
        return getTitleFromLatestVersion(datasetId, true);
    }

    public String getTitleFromLatestVersion(Long datasetId, boolean includeDraft) {

        String whereDraft = "";
        //This clause will exclude draft versions from the select
        if (!includeDraft) {
            whereDraft = " and v.versionstate !='DRAFT' ";
        }

        try {
            return (String) em.createNativeQuery("select df.fieldvalue  from dataset d "
                                                         + " join datasetversion v on d.id = v.dataset_id "
                                                         + " join datasetfield df on v.id = df.datasetversion_id "
                                                         + " join datasetfieldtype dft on df.datasetfieldtype_id  = dft.id "
                                                         + " where dft.name = '" + DatasetFieldConstant.title + "' and  v.dataset_id =" + datasetId
                                                         + " and df.source = '" + DatasetField.DEFAULT_SOURCE + "'"
                                                         + whereDraft
                                                         + " order by v.versionnumber desc, v.minorVersionNumber desc limit 1 "
                                                         + ";").getSingleResult();

        } catch (Exception ex) {
            logger.log(Level.INFO, "exception trying to get title from latest version: {0}", ex);
            return "";
        }

    }

    public Dataset getDatasetByHarvestInfo(Dataverse dataverse, String harvestIdentifier) {
        String queryStr = "SELECT d FROM Dataset d, DvObject o WHERE d.id = o.id AND o.owner.id = " + dataverse.getId() + " and d.harvestIdentifier = '" + harvestIdentifier + "'";
        Query query = em.createQuery(queryStr);
        List resultList = query.getResultList();
        Dataset dataset = null;
        if (resultList.size() > 1) {
            throw new EJBException("More than one dataset found in the dataverse (id= " + dataverse.getId() + "), with harvestIdentifier= " + harvestIdentifier);
        }
        if (resultList.size() == 1) {
            dataset = (Dataset) resultList.get(0);
        }
        return dataset;

    }

    public Long getDatasetVersionCardImage(Long versionId, User user) {
        if (versionId == null) {
            return null;
        }


        return null;
    }

    public void updateLastExportTimeStamp(Long datasetId) {
        Date now = new Date();
        em.createNativeQuery("UPDATE Dataset SET lastExportTime='" + now.toString() + "' WHERE id=" + datasetId).executeUpdate();
    }

    public Dataset setNonDatasetFileAsThumbnail(Dataset dataset, InputStream inputStream) {
        if (dataset == null) {
            logger.fine("In setNonDatasetFileAsThumbnail but dataset is null! Returning null.");
            return null;
        }
        if (inputStream == null) {
            logger.fine("In setNonDatasetFileAsThumbnail but inputStream is null! Returning null.");
            return null;
        }
        dataset = datasetThumbnailService.persistDatasetLogoToStorageAndCreateThumbnail(dataset, inputStream);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(false);
        return merge(dataset);
    }

    public Dataset setDatasetFileAsThumbnail(Dataset dataset, DataFile datasetFileThumbnailToSwitchTo) {
        if (dataset == null) {
            logger.fine("In setDatasetFileAsThumbnail but dataset is null! Returning null.");
            return null;
        }
        if (datasetFileThumbnailToSwitchTo == null) {
            logger.fine("In setDatasetFileAsThumbnail but dataset is null! Returning null.");
            return null;
        }
        datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(datasetFileThumbnailToSwitchTo);
        dataset.setUseGenericThumbnail(false);
        return merge(dataset);
    }

    public Dataset removeDatasetThumbnail(Dataset dataset) {
        if (dataset == null) {
            logger.fine("In removeDatasetThumbnail but dataset is null! Returning null.");
            return null;
        }
        datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(true);
        return merge(dataset);
    }

    public void assignDatasetThumbnailByNativeQuery(Dataset dataset, DataFile dataFile) {
        try {
            em.createNativeQuery("UPDATE dataset SET thumbnailfile_id=" + dataFile.getId() + " WHERE id=" + dataset.getId()).executeUpdate();
        } catch (Exception ex) {
            // it's ok to just ignore...
        }
    }

    public WorkflowComment addWorkflowComment(WorkflowComment workflowComment) {
        em.persist(workflowComment);
        return workflowComment;
    }

    @Asynchronous
    public void callFinalizePublishCommandAsynchronously(Long datasetId, CommandContext ctxt, DataverseRequest request, boolean isPidPrePublished)  {

        // Since we are calling the next command asynchronously anyway - sleep here
        // for a few seconds, just in case, to make sure the database update of
        // the dataset initiated by the PublishDatasetCommand has finished,
        // to avoid any concurrency/optimistic lock issues.
        try {
            Thread.sleep(15000);
        } catch (Exception ex) {
            logger.warning("Failed to sleep for 15 seconds.");
        }
        logger.fine("Running FinalizeDatasetPublicationCommand, asynchronously");
        Dataset theDataset = find(datasetId);
        commandEngine.submit(new FinalizeDatasetPublicationCommand(theDataset, request, isPidPrePublished));
    }

    /**
     * @return true if dataset is In Review locked state
     */
    public boolean isInReview(Dataset dataset) {
        if(dataset.getLocks().isEmpty()) {
            return false;
        }

        List<DatasetLock> locks = em.createNamedQuery("DatasetLock.getLocksByDatasetId", DatasetLock.class)
                .setParameter("datasetId", dataset.getId())
                .getResultList();

        for(DatasetLock lock : locks) {
            if(lock.getReason().equals(DatasetLock.Reason.InReview)){
                return true;
            }
        }
        return false;
    }

    public void updateAllLastChangeForExporterTime() {
        Date date = new Date();
        Query query = em.createQuery(
                "UPDATE Dataset ds SET ds.lastChangeForExporterTime=:date WHERE ds.harvestedFrom IS NULL");
        query.setParameter("date", date, TemporalType.TIMESTAMP);
        query.executeUpdate();
    }
}
