package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.common.files.mime.ImageMimeType;
import edu.harvard.iq.dataverse.common.files.mime.MimePrefix;
import edu.harvard.iq.dataverse.common.files.mime.MimeType;
import edu.harvard.iq.dataverse.common.files.mime.PackageMimeType;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.harvard.iq.dataverse.util.FileUtil.canIngestAsTabular;
import static java.util.stream.Collectors.toList;

/**
 * @author Leonid Andreev
 *
 * Basic skeleton of the new DataFile service for DVN 4.0
 */

@Stateless
@Named
public class DataFileServiceBean implements java.io.Serializable {

    private static final Logger logger = LoggerFactory.getLogger(DataFileServiceBean.class.getCanonicalName());
    @EJB
    DvObjectServiceBean dvObjectService;
    @Inject
    SettingsServiceBean settingsService;
    @Inject
    private ImageThumbConverter imageThumbConverter;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private DataAccess dataAccess = DataAccess.dataAccess();

    // -------------------- LOGIC --------------------

    public DataFile find(Object pk) {
        return em.find(DataFile.class, pk);
    }

    public DataFile findByGlobalId(String globalId) {
        return (DataFile) dvObjectService.findByGlobalId(globalId, DataFile.DATAFILE_DTYPE_STRING);
    }

    public DataFile findReplacementFile(Long previousFileId) {
        Query query = em.createQuery("select object(o) from DataFile as o where o.previousDataFileId = :previousFileId");
        query.setParameter("previousFileId", previousFileId);
        try {
            return (DataFile) query.getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public DataFile findPreviousFile(DataFile df) {
        TypedQuery<DataFile> query = em.createQuery("select o from DataFile o" + " WHERE o.id = :dataFileId", DataFile.class);
        query.setParameter("dataFileId", df.getPreviousDataFileId());
        try {
            return query.getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public List<DataFile> findAllRelatedByRootDatafileId(Long datafileId) {
        // Get all files with the same root datafile id
        // the first file has its own id as root so only one query needed.
        String qr = "select o from DataFile o where o.rootDataFileId = :datafileId order by o.id";
        return em.createQuery(qr, DataFile.class)
                 .setParameter("datafileId", datafileId).getResultList();
    }

    public List<DataFile> findDataFilesByFileMetadataIds(Collection<Long> fileMetadataIds) {
        return em.createQuery("SELECT d FROM FileMetadata f JOIN f.dataFile d WHERE f.id IN :fileMetadataIds", DataFile.class)
                .setParameter("fileMetadataIds", fileMetadataIds)
                .setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE")
                .getResultList();
    }

    public DataFile findByStorageIdAndDatasetVersion(String storageId, DatasetVersion dv) {
        try {
            Query query = em.createNativeQuery("select o.id from dvobject o, filemetadata m " +
                    "where o.storageidentifier = '" + storageId +
                    "' and o.id = m.datafile_id and m.datasetversion_id = " + dv.getId() + "");
            query.setMaxResults(1);
            if (query.getResultList().isEmpty()) {
                return null;
            } else {
                return findCheapAndEasy((Long) query.getSingleResult());
                // Pretty sure the above return will always error due to a conversion error
                // I "reverted" my change because I ended up not using this, but here is the fix below --MAD
                //      Integer qr = (Integer) query.getSingleResult();
                //      return findCheapAndEasy(qr.longValue());
            }
        } catch (Exception e) {
            logger.error("Error finding datafile by storageID and DataSetVersion: " + e.getMessage(), e);
            return null;
        }
    }

    public List<FileMetadata> findFileMetadataByDatasetVersionId(Long datasetVersionId, int maxResults, FileSortFieldAndOrder sortFieldAndOrder) {
        if (maxResults < 0) {
            // return all results if user asks for negative number of results
            maxResults = 0;
        }
        String sortFieldString = sortFieldAndOrder.getSortField();
        String sortOrderString = sortFieldAndOrder.getSortOrder() == SortOrder.desc ? "desc" : "asc";
        String qr = "select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId order by o." +
                sortFieldString + " " + sortOrderString;
        return em.createQuery(qr, FileMetadata.class)
                 .setParameter("datasetVersionId", datasetVersionId)
                 .setMaxResults(maxResults)
                 .getResultList();
    }

    public List<Integer> findFileMetadataIdsByDatasetVersionIdLabelSearchTerm(Long datasetVersionId, String searchTerm,
                                                                              FileSortFieldAndOrder sortFieldAndOrder) {
        String searchClause = "";
        if (searchTerm != null && !searchTerm.isEmpty()) {
            searchClause = " and  (lower(o.label) like '%" + searchTerm.toLowerCase() + "%' or lower(o.description) like '%" + searchTerm
                    .toLowerCase() + "%')";
        }

        // the createNativeQuary takes persistant entities, which Integer.class is not,
        // which is causing the exception. Hence, this query does not need an Integer.class
        // as the second parameter.
        return em.createNativeQuery("select o.id from FileMetadata o where o.datasetVersion_id = " + datasetVersionId +
                searchClause + " order by o." + sortFieldAndOrder.getSortField() + " " +
                (sortFieldAndOrder.getSortOrder() == SortOrder.desc ? "desc" : "asc"))
                .getResultList();
    }

    public FileMetadata findFileMetadata(Long fileMetadataId) {
        return em.find(FileMetadata.class, fileMetadataId);
    }

    public FileMetadata findFileMetadataByDatasetVersionIdAndDataFileId(Long datasetVersionId, Long dataFileId) {

        Query query = em.createQuery("select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId  and o.dataFile.id = :dataFileId");
        query.setParameter("datasetVersionId", datasetVersionId);
        query.setParameter("dataFileId", dataFileId);
        try {
            return (FileMetadata) query.getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public FileMetadata findMostRecentVersionFileIsIn(DataFile file) {
        if (file == null) {
            return null;
        }
        List<FileMetadata> fileMetadatas = file.getFileMetadatas();
        return fileMetadatas != null && !fileMetadatas.isEmpty()
                ? fileMetadatas.get(0) : null;
    }

    public DataFile findCheapAndEasy(Long id) {
        DataFile dataFile;
        Object[] result;
        try {
            result = (Object[]) em.createNativeQuery("SELECT t0.ID, t0.CREATEDATE, t0.INDEXTIME, t0.MODIFICATIONTIME, " +
                    "t0.PERMISSIONINDEXTIME, t0.PERMISSIONMODIFICATIONTIME, t0.PUBLICATIONDATE, t0.CREATOR_ID, t0.RELEASEUSER_ID, " +
                    "t0.PREVIEWIMAGEAVAILABLE, t1.CONTENTTYPE, t0.STORAGEIDENTIFIER, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, " +
                    "t3.ID, t2.AUTHORITY, t2.IDENTIFIER, t1.CHECKSUMTYPE, t1.PREVIOUSDATAFILEID, t1.ROOTDATAFILEID, t0.AUTHORITY, " +
                    "T0.PROTOCOL, T0.IDENTIFIER FROM DVOBJECT t0, DATAFILE t1, DVOBJECT t2, DATASET t3 WHERE ((t0.ID = " + id + ") " +
                    "AND (t0.OWNER_ID = t2.ID) AND (t2.ID = t3.ID) AND (t1.ID = t0.ID))")
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }

        if (result == null) {
            return null;
        }

        Integer file_id = (Integer) result[0];
        dataFile = new DataFile();
        dataFile.setMergeable(false);
        dataFile.setId(file_id.longValue());

        Timestamp createDate = (Timestamp) result[1];
        Timestamp indexTime = (Timestamp) result[2];
        Timestamp modificationTime = (Timestamp) result[3];
        Timestamp permissionIndexTime = (Timestamp) result[4];
        Timestamp permissionModificationTime = (Timestamp) result[5];
        Timestamp publicationDate = (Timestamp) result[6];

        dataFile.setCreateDate(createDate);
        dataFile.setIndexTime(indexTime);
        dataFile.setModificationTime(modificationTime);
        dataFile.setPermissionIndexTime(permissionIndexTime);
        dataFile.setPermissionModificationTime(permissionModificationTime);
        dataFile.setPublicationDate(publicationDate);

        Boolean previewAvailable = (Boolean) result[9];
        if (previewAvailable != null) {
            dataFile.setPreviewImageAvailable(previewAvailable);
        }

        String contentType = (String) result[10];
        if (contentType != null) {
            dataFile.setContentType(contentType);
        }

        String storageIdentifier = (String) result[11];
        if (storageIdentifier != null) {
            dataFile.setStorageIdentifier(storageIdentifier);
        }

        Long fileSize = (Long) result[12];
        if (fileSize != null) {
            dataFile.setFilesize(fileSize);
        }

        if (result[13] != null) {
            String ingestStatusString = (String) result[13];
            dataFile.setIngestStatus(ingestStatusString.charAt(0));
        }

        String md5 = (String) result[14];
        if (md5 != null) {
            dataFile.setChecksumValue(md5);
        }

        Dataset owner = new Dataset();

        // TODO: check for nulls
        owner.setId((Long) result[15]);
        owner.setAuthority((String) result[16]);
        owner.setIdentifier((String) result[17]);

        String checksumType = (String) result[18];
        if (checksumType != null) {
            try {
                // In the database we store "SHA1" rather than "SHA-1".
                DataFile.ChecksumType typeFromStringInDatabase = DataFile.ChecksumType.valueOf(checksumType);
                dataFile.setChecksumType(typeFromStringInDatabase);
            } catch (IllegalArgumentException ex) {
                logger.info("Exception trying to convert " + checksumType + " to enum: " + ex);
            }
        }

        Long previousDataFileId = (Long) result[19];
        if (previousDataFileId != null) {
            dataFile.setPreviousDataFileId(previousDataFileId);
        }

        Long rootDataFileId = (Long) result[20];
        if (rootDataFileId != null) {
            dataFile.setRootDataFileId(rootDataFileId);
        }

        String authority = (String) result[21];
        if (authority != null) {
            dataFile.setAuthority(authority);
        }

        String protocol = (String) result[22];
        if (protocol != null) {
            dataFile.setProtocol(protocol);
        }

        String identifier = (String) result[23];
        if (identifier != null) {
            dataFile.setIdentifier(identifier);
        }

        dataFile.setOwner(owner);

        // If content type indicates it's tabular data, spend 2 extra queries
        // looking up the data table and tabular tags objects:

        if (TextMimeType.TSV.getMimeValue().equalsIgnoreCase(contentType)) {
            Object[] dtResult;
            try {
                dtResult = (Object[]) em
                        .createNativeQuery("SELECT ID, UNF, CASEQUANTITY, VARQUANTITY, ORIGINALFILEFORMAT, ORIGINALFILESIZE FROM dataTable WHERE DATAFILE_ID = " + id)
                        .getSingleResult();
            } catch (Exception ex) {
                dtResult = null;
            }

            if (dtResult == null) {
                return dataFile;
            }
            DataTable dataTable = new DataTable();
            dataTable.setId(((Integer) dtResult[0]).longValue());
            dataTable.setUnf((String) dtResult[1]);
            dataTable.setCaseQuantity((Long) dtResult[2]);
            dataTable.setVarQuantity((Long) dtResult[3]);
            dataTable.setOriginalFileFormat((String) dtResult[4]);
            dataTable.setOriginalFileSize((Long) dtResult[5]);
            dataTable.setDataFile(dataFile);
            dataFile.setDataTable(dataTable);

            // tabular tags:
            List<Object[]> tagResults;
            try {
                tagResults = em.createNativeQuery("SELECT t.TYPE, t.DATAFILE_ID FROM DATAFILETAG t WHERE t.DATAFILE_ID = " + id)
                        .getResultList();
            } catch (Exception ex) {
                logger.info("EXCEPTION looking up tags.");
                tagResults = null;
            }

            if (tagResults != null) {
                List<String> fileTagLabels = DataFileTag.listTags();

                for (Object[] tagResult : tagResults) {
                    Integer tagId = (Integer) tagResult[0];
                    DataFileTag tag = new DataFileTag();
                    tag.setTypeByLabel(fileTagLabels.get(tagId));
                    tag.setDataFile(dataFile);
                    dataFile.addTag(tag);
                }
            }
        }
        return dataFile;
    }

    public DataTable findDataTableByFileId(Long fileId) {
        Query query = em.createQuery("select object(o) from DataTable as o where o.dataFile.id =:fileId order by o.id");
        query.setParameter("fileId", fileId);
        try {
            return (DataTable) query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public List<DataFile> findAll() {
        return em.createQuery("select object(o) from DataFile as o order by o.id", DataFile.class).getResultList();
    }

    public DataFile save(DataFile dataFile) {
        if (dataFile.isMergeable()) {
            return em.merge(dataFile);
        } else {
            throw new IllegalArgumentException("This DataFile object has been set to NOT MERGEABLE; please ensure " +
                    "a MERGEABLE object is passed to the save method.");
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DataFile saveInNewTransaction(DataFile dataFile) {
        return save(dataFile);
    }

    public void deleteFromVersion(DatasetVersion d, DataFile f) {
        em.createNamedQuery("DataFile.removeFromDatasetVersion")
          .setParameter("versionId", d.getId()).setParameter("fileId", f.getId())
          .executeUpdate();
    }

    /*
     Convenience methods for merging and removingindividual file metadatas,
     without touching the rest of the DataFile object:
    */
    public void removeFileMetadata(FileMetadata fileMetadata) {
        FileMetadata mergedFM = em.merge(fileMetadata);
        em.remove(mergedFM);
    }

    // Same, for DataTables:
    public DataTable saveDataTable(DataTable dataTable) {
        DataTable merged = em.merge(dataTable);
        em.flush();
        return merged;
    }

    public List<DataFile> findHarvestedFilesByClient(HarvestingClient harvestingClient) {
        String qr = "SELECT d FROM DataFile d, DvObject o, Dataset s WHERE o.id = d.id AND o.owner.id = s.id " +
                "AND s.harvestedFrom.id = :harvestingClientId";
        return em.createQuery(qr, DataFile.class)
                 .setParameter("harvestingClientId", harvestingClient.getId())
                 .getResultList();
    }

    public boolean isSpssPorFile(DataFile file) {
        return hasMimeType(file, ApplicationMimeType.SPSS_POR);
    }

    public boolean isSpssSavFile(DataFile file) {
        return hasMimeType(file, ApplicationMimeType.SPSS_SAV);
    }

    public boolean isCsvFile(DataFile file) {
        return hasMimeType(file, TextMimeType.CSV, TextMimeType.CSV_ALT);
    }

    public boolean isSelectivelyIngestableFile(DataFile file) {
        return hasMimeType(file, ApplicationMimeType.XLSX, TextMimeType.TSV, TextMimeType.TSV_ALT,
                TextMimeType.CSV, TextMimeType.CSV_ALT);
    }

    /**
     * This method will return true if the thumbnail is *actually available* and
     * ready to be downloaded. (it will try to generate a thumbnail for supported
     * file types, if not yet available)
     */
    public boolean isThumbnailAvailable(DataFile file) {
        if (file == null) {
            return false;
        }

        // If this file already has the "thumbnail generated" flag set, we'll just trust that:
        if (file.isPreviewImageAvailable()) {
            logger.trace("returning true");
            return true;
        }

        // Checking the permission here was resulting in extra queries; it is now the responsibility
        // of the client - such as the DatasetPage - to make sure the permission check out, before
        // calling this method. (or *after* calling this method? - checking permissions costs db queries;
        // checking if the thumbnail is available may cost cpu time, if it has to be generated on the fly
        //  - so you have to figure out which is more important...
        if (imageThumbConverter.isThumbnailAvailable(file)) {
            file = find(file.getId());
            file.setPreviewImageAvailable(true);
            save(file);
            return true;
        }

        return false;
    }

    public String getFileClass(DataFile file) {
        if (isFileClassImage(file)) {
            return MimePrefix.IMAGE.getPrefixValue();
        } else if (isFileClassVideo(file)) {
            return MimePrefix.VIDEO.getPrefixValue();
        } else if (isFileClassAudio(file)) {
            return MimePrefix.AUDIO.getPrefixValue();
        } else if (isFileClassCode(file)) {
            return MimePrefix.CODE.getPrefixValue();
        } else if (isFileClassDocument(file)) {
            return MimePrefix.DOCUMENT.getPrefixValue();
        } else if (isFileClassAstro(file)) {
            return MimePrefix.ASTRO.getPrefixValue();
        } else if (isFileClassNetwork(file)) {
            return MimePrefix.NETWORK.getPrefixValue();
        } else if (isFileClassGeo(file)) {
            return MimePrefix.GEO.getPrefixValue();
        } else if (isFileClassTabularData(file)) {
            return MimePrefix.TABULAR.getPrefixValue();
        } else if (isFileClassPackage(file)) {
            return MimePrefix.PACKAGE.getPrefixValue();
        }
        return MimePrefix.OTHER.getPrefixValue();
    }

    public boolean isFileClassImage(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        // Some browsers (Chrome?) seem to identify FITS files as mime
        // type "image/fits" on upload; this is both incorrect (the official
        // mime type for FITS is "application/fits", and problematic: then
        // the file is identified as an image, and the page will attempt to
        // generate a preview - which of course is going to fail...
        if (ImageMimeType.FITSIMAGE.getMimeValue().equalsIgnoreCase(contentType)) {
            return false;
        }
        // besides most image/* types, we can generate thumbnails for pdf and "world map" files:
        return (contentType != null && (contentType.toLowerCase().startsWith("image/")));
    }

    public boolean isFileClassAudio(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        // TODO:
        // verify that there are no audio types that don't start with "audio/" - some exotic mp[34]... ?
        return (contentType != null && (contentType.toLowerCase().startsWith("audio/")));
    }

    public boolean isFileClassCode(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        // The following are the "control card/syntax" formats that we recognize as "code":
        return ApplicationMimeType.R_SYNTAX.getMimeValue().equalsIgnoreCase(contentType)
                || TextMimeType.STATA_SYNTAX.getMimeValue().equalsIgnoreCase(contentType)
                || TextMimeType.SAS_SYNTAX.getMimeValue().equalsIgnoreCase(contentType)
                || TextMimeType.SPSS_CCARD.getMimeValue().equalsIgnoreCase(contentType);

    }

    public boolean isFileClassDocument(DataFile file) {
        if (file == null) {
            return false;
        }
        // "Documents": PDF, assorted MS docs, etc.
        String contentType = file.getContentType();
        int scIndex;
        if (contentType != null && (scIndex = contentType.indexOf(';')) > 0) {
            contentType = contentType.substring(0, scIndex);
        }
        return TextMimeType.PLAIN_TEXT.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_PDF.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_MSWORD.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_MSEXCEL.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_MSWORD_OPENXML.getMimeValue().equalsIgnoreCase(contentType);

    }

    public boolean isFileClassAstro(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        // The only known/supported "Astro" file type is FITS, so far:
        return (ApplicationMimeType.FITS.getMimeValue().equalsIgnoreCase(contentType) || ImageMimeType.FITSIMAGE
                .getMimeValue()
                .equalsIgnoreCase(contentType));
    }

    public boolean isFileClassNetwork(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        // The only known/supported Network Data type is GRAPHML, so far:
        return TextMimeType.NETWORK_GRAPHML.getMimeValue().equalsIgnoreCase(contentType);
    }

    public boolean isFileClassGeo(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        // The only known/supported Geo Data type is SHAPE, so far:
        return ApplicationMimeType.GEO_SHAPE.getMimeValue().equalsIgnoreCase(contentType);
    }

    public boolean isFileClassTabularData(DataFile file) {
        if (file == null) {
            return false;
        }
        // "Tabular data" is EITHER an INGESTED tabular data file, i.e.
        // a file with a DataTable and DataVariables; or a DataFile
        // of one of the many known tabular data formats - SPSS, Stata, etc.
        // that for one reason or another didn't get ingested:
        if (file.isTabularData()) {
            return true;
        }
        // The formats we know how to ingest:
        if (canIngestAsTabular(file)) {
            return true;
        }
        String contentType = file.getContentType();
        // And these are the formats we DON'T know how to ingest,
        // but nevertheless recognize as "tabular data":
        return (TextMimeType.TSV.getMimeValue().equalsIgnoreCase(contentType)
                || TextMimeType.FIXED_FIELD.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.SAS_TRANSPORT.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.SAS_SYSTEM.getMimeValue().equalsIgnoreCase(contentType));

    }

    public boolean isFileClassVideo(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        // TODO:
        // check if there are video types that don't start with "audio/" -
        // some exotic application/... formats ?
        return contentType != null && contentType.toLowerCase().startsWith("video/");
    }

    public boolean isFileClassPackage(DataFile file) {
        if (file == null) {
            return false;
        }
        String contentType = file.getContentType();
        return PackageMimeType.DATAVERSE_PACKAGE.getMimeValue().equalsIgnoreCase(contentType);
    }

    /**
     * Does this file have a replacement.
     * Any file should have AT MOST 1 replacement
     *
     * @throws java.lang.Exception if a DataFile has more than 1 replacement
     *                             or is unpublished and has a replacement.
     */
    public boolean hasReplacement(DataFile df) throws Exception {
        if (df.getId() == null) {
            // An unsaved file cannot have a replacment
            return false;
        }
        List<DataFile> dataFiles = em.createQuery("select o from DataFile o WHERE o.previousDataFileId = :dataFileId", DataFile.class)
                .setParameter("dataFileId", df.getId())
                .getResultList();

        if (dataFiles.isEmpty()) {
            return false;
        }
        if (!df.isReleased()) {
            // An unpublished SHOULD NOT have a replacment
            String errMsg = "DataFile with id: [" + df.getId() + "] is UNPUBLISHED with a REPLACEMENT.  This should NOT happen.";
            logger.error(errMsg);
            throw new Exception(errMsg);
        } else if (dataFiles.size() == 1) {
            return true;
        } else {
            String errMsg = "DataFile with id: [" + df.getId() + "] has more than one replacment!";
            logger.error(errMsg);
            throw new Exception(errMsg);
        }

    }

    public boolean hasBeenDeleted(DataFile df) {
        Dataset dataset = df.getOwner();
        DatasetVersion dsv = dataset.getLatestVersion();
        return findFileMetadataByDatasetVersionIdAndDataFileId(dsv.getId(), df.getId()) == null;
    }

    /**
     * Is this a replacement file??
     * The indication of a previousDataFileId says that it is
     */
    public boolean isReplacementFile(DataFile df) {
        if (df.getPreviousDataFileId() == null) {
            return false;
        } else if (df.getPreviousDataFileId() < 1) {
            String errMSg = "Stop! previousDataFileId should either be null or a number greater than 0";
            logger.error(errMSg);
            return false;
        } else {
            return df.getPreviousDataFileId() > 0;
        }
    }

    public List<Long> selectFilesWithMissingOriginalTypes() {
        Query query = em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id " +
                "AND (t.originalfileformat='" + TextMimeType.TSV.getMimeValue() + "' OR t.originalfileformat IS NULL) ORDER BY f.id");
        try {
            return query.getResultList();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public List<Long> selectFilesWithMissingOriginalSizes() {
        Query query = em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id " +
                "AND (t.originalfilesize IS NULL) AND (t.originalfileformat IS NOT NULL) ORDER BY f.id");
        try {
            return query.getResultList();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public String generateDataFileIdentifier(DataFile datafile, GlobalIdServiceBean idServiceBean) {
        String doiIdentifierType = settingsService.getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle);
        String doiDataFileFormat = settingsService.getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat);

        String prepend = "";
        if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.DEPENDENT.toString())) {
            // If format is dependent then pre-pend the dataset identifier
            prepend = datafile.getOwner().getIdentifier() + "/";
        } else {
            // If there's a shoulder prepend independent identifiers with it
            prepend = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder);
        }

        switch (doiIdentifierType) {
            case "randomString":
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
            case "sequentialNumber":
                return doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.INDEPENDENT.toString())
                        ? generateIdentifierAsIndependentSequentialNumber(datafile, idServiceBean, prepend)
                        : generateIdentifierAsDependentSequentialNumber(datafile, idServiceBean, prepend);
            default:
                // Should we throw an exception instead?? -- L.A. 4.6.2
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
        }
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network). Also check for duplicate
     * in the remote PID service if needed
     *
     * @return {@code true} iff the global identifier is unique.
     */
    public boolean isGlobalIdUnique(String userIdentifier, DataFile datafile, GlobalIdServiceBean idServiceBean) {
        String testAuthority = datafile.getAuthority() != null
                ? datafile.getAuthority()
                : settingsService.getValueForKey(SettingsServiceBean.Key.Authority);
        String testProtocol = datafile.getProtocol() != null
                ? datafile.getProtocol()
                : settingsService.getValueForKey(SettingsServiceBean.Key.Protocol);

        boolean unique = em.createNamedQuery("DvObject.findByProtocolIdentifierAuthority")
                      .setParameter("protocol", testProtocol)
                      .setParameter("authority", testAuthority)
                      .setParameter("identifier", userIdentifier)
                      .getResultList().isEmpty();

        try {
            if (idServiceBean.alreadyExists(new GlobalId(testProtocol, testAuthority, userIdentifier))) {
                unique = false;
            }
        } catch (Exception e) {
            // we can live with failure - means identifier not found remotely
        }
        return unique;
    }

    /**
     * (File service will double-check that the datafile no
     * longer exists in the database, before proceeding to
     * delete the physical file)
     */
    public void finalizeFileDelete(Long dataFileId, String storageLocation) throws IOException {
        // Verify that the DataFile no longer exists:
        if (find(dataFileId) != null) {
            throw new IOException("Attempted to permanently delete a physical file still associated with an existing DvObject "
                                          + "(id: " + dataFileId + ", location: " + storageLocation);
        }
        StorageIO directStorageAccess = dataAccess.getDirectStorageIO(storageLocation);
        directStorageAccess.delete();
    }

    public void finalizeFileDeletes(Map<Long, String> storageLocations) {
        storageLocations.keySet().stream().forEach(dataFileId -> {
            String storageLocation = storageLocations.get(dataFileId);
            try {
                finalizeFileDelete(dataFileId, storageLocation);
            } catch (IOException ioex) {
                logger.warn("Failed to delete the physical file associated with the deleted datafile id="
                                    + dataFileId + ", storage location: " + storageLocation, ioex);
            }
        });
    }

    public Map<Long, String> getPhysicalFilesToDelete(DatasetVersion datasetVersion) {
        return getPhysicalFilesToDelete(datasetVersion, false);
    }

    public Map<Long, String> getPhysicalFilesToDelete(DatasetVersion datasetVersion, boolean destroy) {
        // Gather the locations of the physical files associated with DRAFT
        // (unpublished) DataFiles (or ALL the DataFiles, if "destroy") in the
        // DatasetVersion, that will need to be deleted once the
        // DeleteDatasetVersionCommand execution has been finalized:

        return getPhysicalFilesToDelete(
                datasetVersion.getFileMetadatas().stream()
                              .map(FileMetadata::getDataFile)
                              .collect(toList()),
                destroy);
    }

    public Map<Long, String> getPhysicalFilesToDelete(List<DataFile> filesToDelete) {
        return getPhysicalFilesToDelete(filesToDelete, false);
    }

    public Map<Long, String> getPhysicalFilesToDelete(List<DataFile> filesToDelete, boolean destroy) {
        Map<Long, String> deleteStorageLocations = new HashMap<>();

        filesToDelete.stream()
                     .filter(file -> !file.isReleased() || destroy)
                     .forEach(file -> deleteStorageLocations.put(file.getId(), getPhysicalFileToDelete(file)));

        return deleteStorageLocations;
    }

    public Map<Long, String> getPhysicalFilesToDelete(Dataset dataset) {
        // Gather the locations of ALL the physical files associated with
        // a DATASET that is being DESTROYED, that will need to be deleted
        // once the DestroyDataset command execution has been finalized.
        // Once again, note that we are selecting all the files from the dataset
        // - not just drafts.

        Map<Long, String> deleteStorageLocations = new HashMap<>();

        for (DataFile df : dataset.getFiles()) {
            String storageLocation = getPhysicalFileToDelete(df);
            if (storageLocation != null) {
                deleteStorageLocations.put(df.getId(), storageLocation);
            }
        }
        return deleteStorageLocations;
    }

    public String getPhysicalFileToDelete(DataFile dataFile) {
        try {
            StorageIO<DataFile> storageIO = dataAccess.getStorageIO(dataFile);
            return storageIO.getStorageLocation();
        } catch (IOException ioe) {
            // something potentially wrong with the physical file,
            // or connection to the physical storage?
            // we don't care (?) - we'll still try to delete the datafile from the database.
            logger.warn("IO issue encountered:", ioe);
        }
        return null;
    }

    public boolean isSameTermsOfUse(FileTermsOfUse termsOfUse1, FileTermsOfUse termsOfUse2) {
        if (termsOfUse1.getTermsOfUseType() != termsOfUse2.getTermsOfUseType()) {
            return false;
        }
        if (termsOfUse1.getTermsOfUseType() == TermsOfUseType.LICENSE_BASED) {
            return termsOfUse1.getLicense().getId().equals(termsOfUse2.getLicense().getId());
        }
        if (termsOfUse1.getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
            return termsOfUse1.getRestrictType() == termsOfUse2.getRestrictType() &&
                    StringUtils.equals(termsOfUse1.getRestrictCustomText(), termsOfUse2.getRestrictCustomText());
        }
        return true;
    }

    // -------------------- PRIVATE --------------------

    private boolean hasMimeType(DataFile file, MimeType... mimeTypes) {
        if (file == null) {
            return false;
        }
        for (MimeType mimeType : mimeTypes) {
            if (mimeType.getMimeValue().equalsIgnoreCase(file.getContentType())) {
                return true;
            }
        }
        return false;
    }

    private String generateIdentifierAsRandomString(DataFile datafile, GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier = null;
        do {
            identifier = prepend + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));
        return identifier;
    }

    private String generateIdentifierAsIndependentSequentialNumber(DataFile datafile, GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier;
        do {
            StoredProcedureQuery query = em.createNamedStoredProcedureQuery("Dataset.generateIdentifierAsSequentialNumber");
            query.execute();
            Integer identifierNumeric = (Integer) query.getOutputParameterValue(1);
            // some diagnostics here maybe - is it possible to determine that it's failing
            // because the stored procedure hasn't been created in the database?
            if (identifierNumeric == null) {
                return null;
            }
            identifier = prepend + identifierNumeric.toString();
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));

        return identifier;
    }

    private String generateIdentifierAsDependentSequentialNumber(DataFile datafile, GlobalIdServiceBean idServiceBean, String prepend) {
        String identifier;
        long retVal = 0L;
        do {
            retVal++;
            identifier = prepend + retVal;
        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));

        return identifier;
    }
}
