package edu.harvard.iq.dataverse;

import com.github.junrar.ContentDescription;
import com.github.junrar.Junrar;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.ApplicationMimeType;
import edu.harvard.iq.dataverse.common.files.mime.ImageMimeType;
import edu.harvard.iq.dataverse.common.files.mime.MimePrefix;
import edu.harvard.iq.dataverse.common.files.mime.PackageMimeType;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datafile.FileService;
import edu.harvard.iq.dataverse.datasetutility.FileExceedsMaxSizeException;
import edu.harvard.iq.dataverse.datasetutility.VirusFoundException;
import edu.harvard.iq.dataverse.ingest.IngestServiceShapefileHelper;
import edu.harvard.iq.dataverse.license.TermsOfUseFactory;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestError;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.harvest.HarvestingClient;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ExternalRarDataUtil;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static edu.harvard.iq.dataverse.common.FileSizeUtil.bytesToHumanReadable;
import static edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestReport.createIngestFailureReport;
import static edu.harvard.iq.dataverse.util.FileUtil.calculateChecksum;
import static edu.harvard.iq.dataverse.util.FileUtil.canIngestAsTabular;
import static edu.harvard.iq.dataverse.util.FileUtil.determineFileType;
import static edu.harvard.iq.dataverse.util.FileUtil.getFilesTempDirectory;
import static java.util.stream.Collectors.toList;

/**
 * @author Leonid Andreev
 * <p>
 * Basic skeleton of the new DataFile service for DVN 4.0
 */

@Stateless
@Named
public class DataFileServiceBean implements java.io.Serializable {

    private static final Logger logger = LoggerFactory.getLogger(DataFileServiceBean.class.getCanonicalName());
    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    SettingsServiceBean settingsService;
    @Inject
    private TermsOfUseFactory termsOfUseFactory;
    @Inject
    private TermsOfUseFormMapper termsOfUseFormMapper;
    @Inject
    private ImageThumbConverter imageThumbConverter;
    @Inject
    private FileService fileService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private DataAccess dataAccess = DataAccess.dataAccess();


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
            DataFile retVal = (DataFile) query.getSingleResult();
            return retVal;
        } catch (Exception ex) {
            return null;
        }
    }


    public DataFile findPreviousFile(DataFile df) {
        TypedQuery<DataFile> query = em.createQuery("select o from DataFile o" + " WHERE o.id = :dataFileId", DataFile.class);
        query.setParameter("dataFileId", df.getPreviousDataFileId());
        try {
            DataFile retVal = query.getSingleResult();
            return retVal;
        } catch (Exception ex) {
            return null;
        }
    }

    public List<DataFile> findAllRelatedByRootDatafileId(Long datafileId) {
        /*
         Get all files with the same root datafile id
         the first file has its own id as root so only one query needed.
        */
        String qr = "select o from DataFile o where o.rootDataFileId = :datafileId order by o.id";
        return em.createQuery(qr, DataFile.class)
                 .setParameter("datafileId", datafileId).getResultList();
    }

    public List<DataFile> findDataFilesByFileMetadataIds(Collection<Long> fileMetadataIds) {
        return em
                .createQuery("SELECT d FROM FileMetadata f JOIN f.dataFile d WHERE f.id IN :fileMetadataIds", DataFile.class)
                .setParameter("fileMetadataIds", fileMetadataIds)
                .setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE")
                .getResultList();
    }

    public DataFile findByStorageIdandDatasetVersion(String storageId, DatasetVersion dv) {
        try {
            Query query = em.createNativeQuery("select o.id from dvobject o, filemetadata m " +
                                                       "where o.storageidentifier = '" + storageId + "' and o.id = m.datafile_id and m.datasetversion_id = " +
                                                       dv.getId() + "");
            query.setMaxResults(1);
            if (query.getResultList().size() < 1) {
                return null;
            } else {
                return findCheapAndEasy((Long) query.getSingleResult());
                //Pretty sure the above return will always error due to a conversion error
                //I "reverted" my change because I ended up not using this, but here is the fix below --MAD
//                Integer qr = (Integer) query.getSingleResult();
//                return findCheapAndEasy(qr.longValue());
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
        String qr = "select o from FileMetadata o where o.datasetVersion.id = :datasetVersionId order by o." + sortFieldString + " " + sortOrderString;
        return em.createQuery(qr, FileMetadata.class)
                 .setParameter("datasetVersionId", datasetVersionId)
                 .setMaxResults(maxResults)
                 .getResultList();
    }

    public List<Integer> findFileMetadataIdsByDatasetVersionIdLabelSearchTerm(Long datasetVersionId,
                                                                              String searchTerm,
                                                                              FileSortFieldAndOrder sortFieldAndOrder) {

        String searchClause = "";
        if (searchTerm != null && !searchTerm.isEmpty()) {
            searchClause = " and  (lower(o.label) like '%" + searchTerm.toLowerCase() + "%' or lower(o.description) like '%" + searchTerm
                    .toLowerCase() + "%')";
        }

        //the createNativeQuary takes persistant entities, which Integer.class is not,
        //which is causing the exception. Hence, this query does not need an Integer.class
        //as the second parameter.
        return em.createNativeQuery("select o.id from FileMetadata o where o.datasetVersion_id = " + datasetVersionId
                                            + searchClause
                                            + " order by o." + sortFieldAndOrder.getSortField() + " " + (
                sortFieldAndOrder.getSortOrder() == SortOrder.desc ?
                        "desc" :
                        "asc"))
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
            FileMetadata retVal = (FileMetadata) query.getSingleResult();
            return retVal;
        } catch (Exception ex) {
            return null;
        }
    }

    public FileMetadata findMostRecentVersionFileIsIn(DataFile file) {
        if (file == null) {
            return null;
        }
        List<FileMetadata> fileMetadatas = file.getFileMetadatas();
        if (fileMetadatas == null || fileMetadatas.isEmpty()) {
            return null;
        } else {
            return fileMetadatas.get(0);
        }
    }

    public DataFile findCheapAndEasy(Long id) {
        DataFile dataFile;

        Object[] result;

        try {
            result = (Object[]) em
                    .createNativeQuery("SELECT t0.ID, t0.CREATEDATE, t0.INDEXTIME, t0.MODIFICATIONTIME, t0.PERMISSIONINDEXTIME, t0.PERMISSIONMODIFICATIONTIME, t0.PUBLICATIONDATE, t0.CREATOR_ID, t0.RELEASEUSER_ID, t0.PREVIEWIMAGEAVAILABLE, t1.CONTENTTYPE, t0.STORAGEIDENTIFIER, t1.FILESIZE, t1.INGESTSTATUS, t1.CHECKSUMVALUE, t3.ID, t2.AUTHORITY, t2.IDENTIFIER, t1.CHECKSUMTYPE, t1.PREVIOUSDATAFILEID, t1.ROOTDATAFILEID, t0.AUTHORITY, T0.PROTOCOL, T0.IDENTIFIER FROM DVOBJECT t0, DATAFILE t1, DVOBJECT t2, DATASET t3 WHERE ((t0.ID = " + id + ") AND (t0.OWNER_ID = t2.ID) AND (t2.ID = t3.ID) AND (t1.ID = t0.ID))")
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

        // no support for users yet!
        // (no need to - so far? -- L.A. 4.2.2)
        /*
         Long creatorId = (Long) result[7];
         if (creatorId != null) {
         AuthenticatedUser creator = userMap.get(creatorId);
         if (creator == null) {
         creator = userService.find(creatorId);
         if (creator != null) {
         userMap.put(creatorId, creator);
         }
         }
         if (creator != null) {
         dataFile.setCreator(creator);
         }
         }

         Long releaseUserId = (Long) result[8];
         if (releaseUserId != null) {
         AuthenticatedUser releaseUser = userMap.get(releaseUserId);
         if (releaseUser == null) {
         releaseUser = userService.find(releaseUserId);
         if (releaseUser != null) {
         userMap.put(releaseUserId, releaseUser);
         }
         }
         if (releaseUser != null) {
         dataFile.setReleaseUser(releaseUser);
         }
         }
         */
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

            if (dtResult != null) {
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
                    tagResults = em
                            .createNativeQuery("SELECT t.TYPE, t.DATAFILE_ID FROM DATAFILETAG t WHERE t.DATAFILE_ID = " + id)
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
        }

        return dataFile;
    }

    public List<DataFile> findIngestsInProgress() {
        if (em.isOpen()) {
            String qr = "select object(o) from DataFile as o where o.ingestStatus =:scheduledStatusCode or o.ingestStatus =:progressStatusCode order by o.id";
            return em.createQuery(qr, DataFile.class)
                     .setParameter("scheduledStatusCode", DataFile.INGEST_STATUS_SCHEDULED)
                     .setParameter("progressStatusCode", DataFile.INGEST_STATUS_INPROGRESS)
                     .getResultList();
        } else {
            return Collections.emptyList();
        }
    }


    public DataTable findDataTableByFileId(Long fileId) {
        Query query = em.createQuery("select object(o) from DataTable as o where o.dataFile.id =:fileId order by o.id");
        query.setParameter("fileId", fileId);

        Object singleResult;

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
            DataFile savedDataFile = em.merge(dataFile);
            return savedDataFile;
        } else {
            throw new IllegalArgumentException("This DataFile object has been set to NOT MERGEABLE; please ensure a MERGEABLE object is passed to the save method.");
        }
    }

    private void msg(String m) {
        System.out.println(m);
    }

    private void dashes() {
        msg("----------------");
    }

    private void msgt(String m) {
        dashes();
        msg(m);
        dashes();
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
        msgt("removeFileMetadata: fileMetadata");
        FileMetadata mergedFM = em.merge(fileMetadata);
        em.remove(mergedFM);
    }

    /*
     * Same, for DataTables:
     */

    public DataTable saveDataTable(DataTable dataTable) {
        DataTable merged = em.merge(dataTable);
        em.flush();
        return merged;
    }

    public List<DataFile> findHarvestedFilesByClient(HarvestingClient harvestingClient) {
        String qr = "SELECT d FROM DataFile d, DvObject o, Dataset s WHERE o.id = d.id AND o.owner.id = s.id AND s.harvestedFrom.id = :harvestingClientId";
        return em.createQuery(qr, DataFile.class)
                 .setParameter("harvestingClientId", harvestingClient.getId())
                 .getResultList();
    }

    /*moving to the fileutil*/

    public boolean isSpssPorFile(DataFile file) {
        return (file != null) && ApplicationMimeType.SPSS_POR.getMimeValue().equalsIgnoreCase(file.getContentType());
    }

    public boolean isSpssSavFile(DataFile file) {
        return (file != null) && ApplicationMimeType.SPSS_SAV.getMimeValue().equalsIgnoreCase(file.getContentType());
    }

    /*
    public boolean isSpssPorFile (FileMetadata fileMetadata) {
        if (fileMetadata != null && fileMetadata.getDataFile() != null) {
            return isSpssPorFile(fileMetadata.getDataFile());
        }
        return false;
    }
    */

    /*
     * This method will return true if the thumbnail is *actually available* and
     * ready to be downloaded. (it will try to generate a thumbnail for supported
     * file types, if not yet available)
     */
    public boolean isThumbnailAvailable(DataFile file) {
        if (file == null) {
            return false;
        }

        // If this file already has the "thumbnail generated" flag set,
        // we'll just trust that:
        if (file.isPreviewImageAvailable()) {
            logger.info("returning true");
            return true;
        }

        /*
         Checking the permission here was resulting in extra queries;
         it is now the responsibility of the client - such as the DatasetPage -
         to make sure the permission check out, before calling this method.
         (or *after* calling this method? - checking permissions costs db
         queries; checking if the thumbnail is available may cost cpu time, if
         it has to be generated on the fly - so you have to figure out which
         is more important...

        */


        if (imageThumbConverter.isThumbnailAvailable(file)) {
            file = this.find(file.getId());
            file.setPreviewImageAvailable(true);
            this.save(file);
            return true;
        }

        return false;
    }

    public String getFileClass(DataFile file) {
        if (isFileClassImage(file)) {
            return MimePrefix.IMAGE.getPrefixValue();
        }

        if (isFileClassVideo(file)) {
            return MimePrefix.VIDEO.getPrefixValue();
        }

        if (isFileClassAudio(file)) {
            return MimePrefix.AUDIO.getPrefixValue();
        }

        if (isFileClassCode(file)) {
            return MimePrefix.CODE.getPrefixValue();
        }

        if (isFileClassDocument(file)) {
            return MimePrefix.DOCUMENT.getPrefixValue();
        }

        if (isFileClassAstro(file)) {
            return MimePrefix.ASTRO.getPrefixValue();
        }

        if (isFileClassNetwork(file)) {
            return MimePrefix.NETWORK.getPrefixValue();
        }

        if (isFileClassGeo(file)) {
            return MimePrefix.GEO.getPrefixValue();
        }

        if (isFileClassTabularData(file)) {
            return MimePrefix.TABULAR.getPrefixValue();
        }

        if (isFileClassPackage(file)) {
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
        // besides most image/* types, we can generate thumbnails for
        // pdf and "world map" files:

        return (contentType != null && (contentType.toLowerCase().startsWith("image/")));
    }

    public boolean isFileClassAudio(DataFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();

        // TODO:
        // verify that there are no audio types that don't start with "audio/" -
        //  some exotic mp[34]... ?

        return (contentType != null && (contentType.toLowerCase().startsWith("audio/")));
    }

    public boolean isFileClassCode(DataFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();

        // The following are the "control card/syntax" formats that we recognize
        // as "code":

        return (ApplicationMimeType.R_SYNTAX.getMimeValue().equalsIgnoreCase(contentType)
                || TextMimeType.STATA_SYNTAX.getMimeValue().equalsIgnoreCase(contentType)
                || TextMimeType.SAS_SYNTAX.getMimeValue().equalsIgnoreCase(contentType)
                || TextMimeType.SPSS_CCARD.getMimeValue().equalsIgnoreCase(contentType));

    }

    public boolean isFileClassDocument(DataFile file) {
        if (file == null) {
            return false;
        }

        // "Documents": PDF, assorted MS docs, etc.

        String contentType = file.getContentType();
        int scIndex = 0;
        if (contentType != null && (scIndex = contentType.indexOf(';')) > 0) {
            contentType = contentType.substring(0, scIndex);
        }

        return (TextMimeType.PLAIN_TEXT.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_PDF.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_MSWORD.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_MSEXCEL.getMimeValue().equalsIgnoreCase(contentType)
                || ApplicationMimeType.DOCUMENT_MSWORD_OPENXML.getMimeValue().equalsIgnoreCase(contentType));

    }

    public boolean isFileClassAstro(DataFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();

        // The only known/supported "Astro" file type is FITS,
        // so far:

        return (ApplicationMimeType.FITS.getMimeValue().equalsIgnoreCase(contentType) || ImageMimeType.FITSIMAGE
                .getMimeValue()
                .equalsIgnoreCase(contentType));

    }

    public boolean isFileClassNetwork(DataFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();

        // The only known/supported Network Data type is GRAPHML,
        // so far:

        return TextMimeType.NETWORK_GRAPHML.getMimeValue().equalsIgnoreCase(contentType);

    }

    /*
     * we don't really need a method for "other" -
     * it's "other" if it fails to identify as any specific class...
     * (or do we?)
    public boolean isFileClassOther (DataFile file) {
        if (file == null) {
            return false;
        }

    }
    */

    public boolean isFileClassGeo(DataFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();

        // The only known/supported Geo Data type is SHAPE,
        // so far:

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

        return (contentType != null && (contentType.toLowerCase().startsWith("video/")));

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
     * @param df
     * @return
     * @throws java.lang.Exception if a DataFile has more than 1 replacement
     *                             or is unpublished and has a replacement.
     */
    public boolean hasReplacement(DataFile df) throws Exception {

        if (df.getId() == null) {
            // An unsaved file cannot have a replacment
            return false;
        }


        List<DataFile> dataFiles = em.createQuery("select o from DataFile o" +
                                                          " WHERE o.previousDataFileId = :dataFileId", DataFile.class)
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
     * <p>
     * The indication of a previousDataFileId says that it is
     *
     * @param df
     * @return
     */
    public boolean isReplacementFile(DataFile df) {

        if (df.getPreviousDataFileId() == null) {
            return false;
        } else if (df.getPreviousDataFileId() < 1) {
            String errMSg = "Stop! previousDataFileId should either be null or a number greater than 0";
            logger.error(errMSg);
            return false;
            // blow up -- this shouldn't happen!
            //throw new FileReplaceException(errMSg);
        } else {
            return df.getPreviousDataFileId() > 0;
        }
    }  // end: isReplacementFile

    public List<Long> selectFilesWithMissingOriginalTypes() {
        Query query = em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id AND (t.originalfileformat='" + TextMimeType.TSV
                .getMimeValue() + "' OR t.originalfileformat IS NULL) ORDER BY f.id");

        try {
            return query.getResultList();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public List<Long> selectFilesWithMissingOriginalSizes() {
        Query query = em.createNativeQuery("SELECT f.id FROM datafile f, datatable t where t.datafile_id = f.id AND (t.originalfilesize IS NULL ) AND (t.originalfileformat IS NOT NULL) ORDER BY f.id");

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
            //If format is dependent then pre-pend the dataset identifier
            prepend = datafile.getOwner().getIdentifier() + "/";
        } else {
            //If there's a shoulder prepend independent identifiers with it
            prepend = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder);
        }

        switch (doiIdentifierType) {
            case "randomString":
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
            case "sequentialNumber":
                if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.INDEPENDENT.toString())) {
                    return generateIdentifierAsIndependentSequentialNumber(datafile, idServiceBean, prepend);
                } else {
                    return generateIdentifierAsDependentSequentialNumber(datafile, idServiceBean, prepend);
                }
            default:
                /* Should we throw an exception instead?? -- L.A. 4.6.2 */
                return generateIdentifierAsRandomString(datafile, idServiceBean, prepend);
        }
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
            StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery("Dataset.generateIdentifierAsSequentialNumber");
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
        Long retVal;

        retVal = new Long(0);

        do {
            retVal++;
            identifier = prepend + retVal.toString();

        } while (!isGlobalIdUnique(identifier, datafile, idServiceBean));

        return identifier;
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network). Also check for duplicate
     * in the remote PID service if needed
     *
     * @param userIdentifier
     * @param datafile
     * @param idServiceBean
     * @return {@code true} iff the global identifier is unique.
     */
    public boolean isGlobalIdUnique(String userIdentifier, DataFile datafile, GlobalIdServiceBean idServiceBean) {
        String testProtocol = "";
        String testAuthority = "";
        if (datafile.getAuthority() != null) {
            testAuthority = datafile.getAuthority();
        } else {
            testAuthority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority);
        }
        if (datafile.getProtocol() != null) {
            testProtocol = datafile.getProtocol();
        } else {
            testProtocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol);
        }

        boolean u = em.createNamedQuery("DvObject.findByProtocolIdentifierAuthority")
                      .setParameter("protocol", testProtocol)
                      .setParameter("authority", testAuthority)
                      .setParameter("identifier", userIdentifier)
                      .getResultList().isEmpty();

        try {
            if (idServiceBean.alreadyExists(new GlobalId(testProtocol, testAuthority, userIdentifier))) {
                u = false;
            }
        } catch (Exception e) {
            //we can live with failure - means identifier not found remotely
        }


        return u;
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
        storageLocations.keySet().stream().forEach((dataFileId) -> {
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
                              .map(fm -> fm.getDataFile())
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

        Iterator<DataFile> dfIt = dataset.getFiles().iterator();
        while (dfIt.hasNext()) {
            DataFile df = dfIt.next();

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

        } catch (IOException ioex) {
            // something potentially wrong with the physical file,
            // or connection to the physical storage?
            // we don't care (?) - we'll still try to delete the datafile from the database.
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

    public List<DataFile> createDataFiles(DatasetVersion version, InputStream inputStream, String fileName, String suppliedContentType) throws IOException {
        List<DataFile> datafiles = new ArrayList<>();

        IngestError errorKey = null;

        // save the file, in the temporary location for now:
        Path tempFile = null;

        Long fileSizeLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);
        Long uncompressedSize = 0L;
        if (getFilesTempDirectory() != null) {
            tempFile = saveInputStreamInTempFile(inputStream, fileSizeLimit).toPath();
        } else {
            throw new IOException("Temp directory is not configured.");
        }

        if (settingsService.isTrueForKey(SettingsServiceBean.Key.AntivirusScannerEnabled) &&
                tempFile
                        .toFile()
                        .length() <= settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.AntivirusScannerMaxFileSize)) {

            String scannerMessage = fileService.scan(tempFile.toFile());

            if (scannerMessage.contains("FOUND")) {
                logger.warn("There was an attempt to upload file infected with virus. Scanner message: {} Dataset version: {}", scannerMessage, version);
                if (!tempFile.toFile().delete()) {
                    logger.warn("Unable to remove temporary file {}", tempFile.toFile().getAbsolutePath());
                }
                throw new VirusFoundException(scannerMessage);
            }
        }

        logger.info("mime type supplied: " + suppliedContentType);
        // Let's try our own utilities (Jhove, etc.) to determine the file type
        // of the uploaded file. (We may already have a mime type supplied for this
        // file - maybe the type that the browser recognized on upload; or, if
        // it's a harvest, maybe the remote server has already given us the type
        // for this file... with our own type utility we may or may not do better
        // than the type supplied:
        //  -- L.A.
        String recognizedType = null;
        String finalType = null;
        try {
            recognizedType = determineFileType(tempFile.toFile(), fileName);
            logger.info("File utility recognized the file as " + recognizedType);
            if (recognizedType != null && !recognizedType.equals("")) {
                // is it any better than the type that was supplied to us,
                // if any?
                // This is not as trivial a task as one might expect...
                // We may need a list of "good" mime types, that should always
                // be chosen over other choices available. Maybe it should
                // even be a weighed list... as in, "application/foo" should
                // be chosen over "application/foo-with-bells-and-whistles".

                // For now the logic will be as follows:
                //
                // 1. If the contentType supplied (by the browser, most likely)
                // is some form of "unknown", we always discard it in favor of
                // whatever our own utilities have determined;
                // 2. We should NEVER trust the browser when it comes to the
                // following "ingestable" types: Stata, SPSS, R;
                // 2a. We are willing to TRUST the browser when it comes to
                //  the CSV and XSLX ingestable types.
                // 3. We should ALWAYS trust our utilities when it comes to
                // ingestable types.

                if (suppliedContentType == null
                        || suppliedContentType.equals("")
                        || suppliedContentType.equalsIgnoreCase(ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue())
                        || suppliedContentType.equalsIgnoreCase(ApplicationMimeType.UNDETERMINED_BINARY.getMimeValue())
                        || (canIngestAsTabular(suppliedContentType)
                        && !suppliedContentType.equalsIgnoreCase(TextMimeType.CSV.getMimeValue())
                        && !suppliedContentType.equalsIgnoreCase(TextMimeType.CSV_ALT.getMimeValue())
                        && !suppliedContentType.equalsIgnoreCase(ApplicationMimeType.XLSX.getMimeValue()))
                        || canIngestAsTabular(recognizedType)
                        || recognizedType.equals("application/fits-gzipped")
                        || recognizedType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)
                        || recognizedType.equals(ApplicationMimeType.ZIP.getMimeValue())) {
                    finalType = recognizedType;
                }
            }

        } catch (Exception ex) {
            logger.warn("Failed to run the file utility mime type check on file " + fileName, ex);
        }

        if (finalType == null) {
            finalType = (suppliedContentType == null || suppliedContentType.equals(""))
                    ? ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue()
                    : suppliedContentType;
        }

        // A few special cases:

        // if this is a gzipped FITS file, we'll uncompress it, and ingest it as
        // a regular FITS file:

        if (finalType.equals("application/fits-gzipped")) {

            InputStream uncompressedIn = null;
            String finalFileName = fileName;
            // if the file name had the ".gz" extension, remove it,
            // since we are going to uncompress it:
            if (fileName != null && fileName.matches(".*\\.gz$")) {
                finalFileName = fileName.replaceAll("\\.gz$", "");
            }

            DataFile datafile = null;
            try {
                uncompressedIn = new GZIPInputStream(new FileInputStream(tempFile.toFile()));
                File unZippedTempFile = saveInputStreamInTempFile(uncompressedIn, fileSizeLimit);
                DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
                datafile = createSingleDataFile(version, unZippedTempFile, finalFileName, ApplicationMimeType.UNDETERMINED_DEFAULT
                        .getMimeValue(), checksumType, uncompressedSize);
            } catch (IOException | FileExceedsMaxSizeException ioex) {
                datafile = null;
            } finally {
                if (uncompressedIn != null) {
                    try {
                        uncompressedIn.close();
                    } catch (IOException e) {
                    }
                }
            }

            // If we were able to produce an uncompressed file, we'll use it
            // to create and return a final DataFile; if not, we're not going
            // to do anything - and then a new DataFile will be created further
            // down, from the original, uncompressed file.
            if (datafile != null) {
                // remove the compressed temp file:
                try {
                    tempFile.toFile().delete();
                } catch (SecurityException ex) {
                    logger.warn("Failed to delete temporary file " + tempFile.toString(), ex);
                }

                datafiles.add(datafile);
                return datafiles;
            }
        } else if ("application/vnd.rar".equals(finalType)) {
            try {
                List<ContentDescription> contentsDescription = Junrar.getContentsDescription(tempFile.toFile());
                uncompressedSize = contentsDescription.stream()
                        .mapToLong(d -> d.size)
                        .sum();
            } catch (UnsupportedRarV5Exception r5e) {
                uncompressedSize = new ExternalRarDataUtil(
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataUtilCommand),
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataUtilOpts),
                    settingsService.getValueForKey(SettingsServiceBean.Key.RarDataLineBeforeResultDelimiter))
                    .checkRarExternally(tempFile);
            } catch (RarException re) {
                logger.warn("Exception during rar file scan: " + tempFile.toString(), re);
            }
        }
        // If it's a ZIP file, we are going to unpack it and create multiple
        // DataFile objects from its contents:
        else if (finalType.equals("application/zip") && settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipUploadFilesLimit) > 0) {

            ZipInputStream unZippedIn = null;
            ZipEntry zipEntry = null;

            long fileNumberLimit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipUploadFilesLimit);

            try {
                unZippedIn = new ZipInputStream(new FileInputStream(tempFile.toFile()));

                while (true) {
                    try {
                        zipEntry = unZippedIn.getNextEntry();
                    } catch (IllegalArgumentException iaex) {
                        // Note:
                        // ZipInputStream documentation doesn't even mention that
                        // getNextEntry() throws an IllegalArgumentException!
                        // but that's what happens if the file name of the next
                        // entry is not valid in the current CharSet.
                        //      -- L.A.
                        errorKey = IngestError.UNZIP_FAIL;
                        logger.warn("Failed to unpack Zip file. (Unknown Character Set used in a file name?) Saving the file as is.", iaex);
                        throw new IOException();
                    }

                    if (zipEntry == null) {
                        break;
                    }
                    // Note that some zip entries may be directories - we
                    // simply skip them:

                    if (!zipEntry.isDirectory()) {
                        if (datafiles.size() >= fileNumberLimit) {
                            logger.warn("Zip upload - too many files.");
                            errorKey = IngestError.UNZIP_FILE_LIMIT_FAIL;
                            throw new IOException();
                        }

                        String fileEntryName = zipEntry.getName();
                        logger.info("ZipEntry, file: " + fileEntryName);

                        if (fileEntryName != null && !fileEntryName.equals("")) {

                            String shortName = fileEntryName.replaceFirst("^.*[\\/]", "");

                            // Check if it's a "fake" file - a zip archive entry
                            // created for a MacOS X filesystem element: (these
                            // start with "._")
                            if (!shortName.startsWith("._") && !shortName.startsWith(".DS_Store") && !"".equals(shortName)) {
                                // OK, this seems like an OK file entry - we'll try
                                // to read it and create a DataFile with it:

                                File unZippedTempFile = saveInputStreamInTempFile(unZippedIn, fileSizeLimit, false);
                                DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
                                DataFile datafile = createSingleDataFile(version, unZippedTempFile, shortName, ApplicationMimeType.UNDETERMINED_DEFAULT
                                        .getMimeValue(), checksumType, uncompressedSize,false);

                                if (!fileEntryName.equals(shortName)) {
                                    // If the filename looks like a hierarchical folder name (i.e., contains slashes and backslashes),
                                    // we'll extract the directory name, then a) strip the leading and trailing slashes;
                                    // and b) replace all the back slashes with regular ones and b) replace any multiple
                                    // slashes with a single slash:
                                    String directoryName = fileEntryName
                                            .replaceFirst("[\\/][\\/]*[^\\/]*$", "")
                                            .replaceFirst("^[\\/]*", "")
                                            .replaceAll("[\\/][\\/]*", "/");
                                    if (!"".equals(directoryName)) {
                                        logger.info("setting the directory label to " + directoryName);
                                        datafile.getFileMetadata().setDirectoryLabel(directoryName);
                                    }
                                }

                                if (datafile != null) {
                                    // We have created this datafile with the mime type "unknown";
                                    // Now that we have it saved in a temporary location,
                                    // let's try and determine its real type:

                                    String tempFileName = getFilesTempDirectory() + "/" + datafile.getStorageIdentifier();

                                    try {
                                        recognizedType = determineFileType(new File(tempFileName), shortName);
                                        logger.info("File utility recognized unzipped file as " + recognizedType);
                                        if (recognizedType != null && !recognizedType.equals("")) {
                                            datafile.setContentType(recognizedType);
                                        }
                                    } catch (Exception ex) {
                                        logger.warn("Failed to run the file utility mime type check on file " + fileName, ex);
                                    }

                                    datafiles.add(datafile);
                                }
                            }
                        }
                    }
                    unZippedIn.closeEntry();

                }

            } catch (IOException ioex) {
                // just clear the datafiles list and let
                // ingest default to creating a single DataFile out
                // of the unzipped file.
                logger.warn("Failed to unzip the file. Saving the file as is.", ioex);
                if (errorKey == IngestError.UNZIP_FILE_LIMIT_FAIL) {
                    uncompressedSize = extractZipContentsSize(tempFile);
                } else if (errorKey == null) {
                    errorKey = IngestError.UNZIP_FAIL;
                }

                datafiles.clear();
            } catch (FileExceedsMaxSizeException femsx) {
                logger.warn("One of the unzipped files exceeds the size limit resorting to saving the file as is, unzipped.", femsx);
                errorKey = IngestError.UNZIP_SIZE_FAIL;
                datafiles.clear();
            } finally {
                if (unZippedIn != null) {
                    try {
                        unZippedIn.close();
                    } catch (Exception zEx) {
                    }
                }
            }
            if (datafiles.size() > 0) {
                // remove the uploaded zip file:
                try {
                    Files.delete(tempFile);
                } catch (IOException ioex) {
                    // do nothing - it's just a temp file.
                    logger.warn("Could not remove temp file " + tempFile.getFileName().toString(), ioex);
                }
                // and return:
                return datafiles;
            }
        } else if (finalType.equals("application/zip")
                && settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipUploadFilesLimit) == 0) {
            uncompressedSize = extractZipContentsSize(tempFile);
        } else if ("application/x-7z-compressed".equals(finalType)) {
            long size = 0L;
            try {
                SevenZFile archive = new SevenZFile(tempFile.toFile());
                SevenZArchiveEntry entry;
                while ((entry = archive.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        size += entry.getSize();
                    }
                }
            } catch (IOException ioe) {
                logger.warn("Exception while checking contents of 7z file: " + tempFile.getFileName(), ioe);
            }
            uncompressedSize = size;
        } else if (finalType.equalsIgnoreCase(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
            // Shape files may have to be split into multiple files,
            // one zip archive per each complete set of shape files:

            //File rezipFolder = new File(this.getFilesTempDirectory());
            File rezipFolder = getShapefileUnzipTempDirectory();

            IngestServiceShapefileHelper shpIngestHelper;
            shpIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), rezipFolder);

            boolean didProcessWork = shpIngestHelper.processFile();
            if (!(didProcessWork)) {
                logger.error("Processing of zipped shapefile failed.");
                return null;
            }

            try {
                for (File finalFile : shpIngestHelper.getFinalRezippedFiles()) {
                    FileInputStream finalFileInputStream = new FileInputStream(finalFile);
                    finalType = determineContentType(finalFile);
                    if (finalType == null) {
                        logger.warn("Content type is null; but should default to 'MIME_TYPE_UNDETERMINED_DEFAULT'");
                        continue;
                    }

                    File unZippedShapeTempFile = saveInputStreamInTempFile(finalFileInputStream, fileSizeLimit);
                    DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
                    DataFile new_datafile = createSingleDataFile(version, unZippedShapeTempFile, finalFile.getName(),
                            finalType, checksumType, uncompressedSize);
                    if (new_datafile != null) {
                        datafiles.add(new_datafile);
                    } else {
                        logger.error("Could not add part of rezipped shapefile. new_datafile was null: " + finalFile.getName());
                    }
                    finalFileInputStream.close();

                }
            } catch (FileExceedsMaxSizeException femsx) {
                logger.error("One of the unzipped shape files exceeded the size limit; giving up. " + femsx.getMessage(), femsx);
                datafiles.clear();
            }

            // Delete the temp directory used for unzipping
            FileUtils.deleteDirectory(rezipFolder);

            if (datafiles.size() > 0) {
                // remove the uploaded zip file:
                try {
                    Files.delete(tempFile);
                } catch (IOException ioex) {
                    // do nothing - it's just a temp file.
                    logger.warn("Could not remove temp file " + tempFile.getFileName().toString(), ioex);
                } catch (SecurityException se) {
                    logger.warn("Unable to delete: " + tempFile.toString() + "due to Security Exception: "
                                        + se.getMessage(), se);
                }
                return datafiles;
            } else {
                logger.error("No files added from directory of rezipped shapefiles");
            }
            return null;

        }
        // Finally, if none of the special cases above were applicable (or
        // if we were unable to unpack an uploaded file, etc.), we'll just
        // create and return a single DataFile:
        DataFile.ChecksumType checksumType = DataFile.ChecksumType.fromString(settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm));
        DataFile datafile = createSingleDataFile(version, tempFile.toFile(), fileName, finalType, checksumType, uncompressedSize);

        if (datafile != null) {

            if (errorKey != null) {

                if (errorKey.equals(IngestError.UNZIP_FILE_LIMIT_FAIL) && fileSizeLimit != null) {
                    datafile.setIngestReport(createIngestFailureReport(datafile, errorKey, fileSizeLimit.toString()));
                } else {
                    datafile.setIngestReport(createIngestFailureReport(datafile, errorKey));
                }

                datafile.SetIngestProblem();
            }
            datafiles.add(datafile);

            return datafiles;
        }

        return null;
    } // end createDataFiles
    private long extractZipContentsSize(Path tempFile) {
        long size = 0;
        try (ZipInputStream unZippedIn = new ZipInputStream(new FileInputStream(tempFile.toFile()))) {
            ZipEntry zipEntry;
            while (true) {
                try {
                    zipEntry = unZippedIn.getNextEntry();
                } catch (IllegalArgumentException iaex) {
                    logger.warn("Failed to unpack Zip file. (Unknown Character Set used in a file name?) Saving the file as is.", iaex);
                    zipEntry = null;
                }
                if (zipEntry == null) {
                    break;
                }
                if (!zipEntry.isDirectory()) {
                    size += zipEntry.getSize();
                }
                unZippedIn.closeEntry();
            }
        } catch (IOException ioe) {
            logger.warn("Exception encountered: ", ioe);
        }
        return size;
    }

    private File saveInputStreamInTempFile(InputStream inputStream, Long fileSizeLimit)
            throws IOException, FileExceedsMaxSizeException {
        return saveInputStreamInTempFile(inputStream, fileSizeLimit, true);
    }

    private File saveInputStreamInTempFile(InputStream inputStream, Long fileSizeLimit, boolean closeStream)
            throws IOException, FileExceedsMaxSizeException {
        Path tempFile = Files.createTempFile(Paths.get(getFilesTempDirectory()), "tmp", "upload");

        try (FileOutputStream outStream = new FileOutputStream(tempFile.toFile())) {
            logger.info("Will attempt to save the file as: " + tempFile.toString());

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (fileSizeLimit != null && totalBytesRead > fileSizeLimit) {
                    try {
                        tempFile.toFile().delete();
                    } catch (Exception ex) {
                        logger.error("There was a problem with deleting temporary file");
                    }

                    throw new FileExceedsMaxSizeException(MessageFormat.format(BundleUtil.getStringFromBundle("file.addreplace.error.file_exceeds_limit"),
                                                                               bytesToHumanReadable(fileSizeLimit)));
                }
                outStream.write(buffer, 0, bytesRead);
            }
        } finally {
            if (closeStream) {
                inputStream.close();
            }
        }

        return tempFile.toFile();
    }

    /*
     * This method creates a DataFile;
     * The bytes from the suppplied InputStream have already been saved in the temporary location.
     * This method should only be called by the upper-level methods that handle
     * file upload and creation for individual use cases - a single file upload,
     * an upload of a zip archive that needs to be unpacked and turned into
     * individual files, etc., and once the file name and mime type have already
     * been figured out.
     */

    private DataFile createSingleDataFile(DatasetVersion version, File tempFile, String fileName, String contentType,
                          DataFile.ChecksumType checksumType, Long uncompressedSize) {
        return createSingleDataFile(version, tempFile, fileName, contentType, checksumType, uncompressedSize, false);
    }

    private DataFile createSingleDataFile(DatasetVersion version, File tempFile, String fileName, String contentType, DataFile.ChecksumType checksumType, Long uncompressedSize, boolean addToDataset) {

        if (tempFile == null) {
            return null;
        }

        DataFile datafile = new DataFile(contentType);
        datafile.setCreateDate(new Timestamp(new Date().getTime()));
        datafile.setModificationTime(new Timestamp(new Date().getTime()));
        /**
         * @todo Think more about when permissions on files are modified.
         * Obviously, here at create time files have some sort of permissions,
         * even if these permissions are *implied*, by ViewUnpublishedDataset at
         * the dataset level, for example.
         */
        datafile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile.setUncompressedSize(uncompressedSize);
        FileMetadata fmd = new FileMetadata();

        // TODO: add directoryLabel?
        fmd.setLabel(fileName);

        FileTermsOfUse termsOfUse = termsOfUseFactory.createTermsOfUse();
        fmd.setTermsOfUse(termsOfUse);

        fmd.setTermsOfUseForm(termsOfUseFormMapper.mapToForm(termsOfUse));

        if (addToDataset) {
            datafile.setOwner(version.getDataset());
        }
        fmd.setDataFile(datafile);
        datafile.getFileMetadatas().add(fmd);
        if (addToDataset) {
            if (version.getFileMetadatas() == null) {
                version.setFileMetadatas(new ArrayList<>());
            }
            version.addFileMetadata(fmd);
            fmd.setDatasetVersion(version);
            version.getDataset().getFiles().add(datafile);
        }

        datafile.setStorageIdentifier(FileUtil.generateStorageIdentifier());
        if (!tempFile.renameTo(new File(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier()))) {
            return null;
        }

        try {
            // We persist "SHA1" rather than "SHA-1".
            datafile.setChecksumType(checksumType);
            datafile.setChecksumValue(calculateChecksum(getFilesTempDirectory() + "/" + datafile.getStorageIdentifier(), datafile
                    .getChecksumType()));
        } catch (Exception cksumEx) {
            logger.warn("Could not calculate " + checksumType + " signature for the new file " + fileName, cksumEx);
        }

        return datafile;
    }


    /**
     * For the restructuring of zipped shapefiles, create a timestamped directory.
     * This directory is deleted after successful restructuring.
     * <p>
     * Naming convention: getFilesTempDirectory() + "shp_" + "yyyy-MM-dd-hh-mm-ss-SSS"
     */
    private static File getShapefileUnzipTempDirectory() {

        String tempDirectory = getFilesTempDirectory();
        if (tempDirectory == null) {
            logger.error("Failed to retrieve tempDirectory, null was returned");
            return null;
        }
        String datestampedFileName = "shp_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(new Date());
        String datestampedFolderName = tempDirectory + "/" + datestampedFileName;

        File datestampedFolder = new File(datestampedFolderName);
        if (!datestampedFolder.isDirectory()) {
            /* Note that "createDirectories()" must be used - not
             * "createDirectory()", to make sure all the parent
             * directories that may not yet exist are created as well.
             */
            try {
                Files.createDirectories(Paths.get(datestampedFolderName));
            } catch (IOException ex) {
                logger.error("Failed to create temp. directory to unzip shapefile: " + datestampedFolderName, ex);
                return null;
            }
        }
        return datestampedFolder;
    }

    /**
     * Returns a content type string for a FileObject
     */
    private String determineContentType(File fileObject) {
        if (fileObject == null) {
            return null;
        }
        String contentType;
        try {
            contentType = determineFileType(fileObject, fileObject.getName());
        } catch (Exception ex) {
            logger.warn("FileUtil.determineFileType failed for file with name: " + fileObject.getName(), ex);
            contentType = null;
        }

        if ((contentType == null) || (contentType.equals(""))) {
            contentType = ApplicationMimeType.UNDETERMINED_DEFAULT.getMimeValue();
        }
        return contentType;

    }
}
