package edu.harvard.iq.dataverse.api;

import com.amazonaws.services.glacier.model.MissingParameterValueException;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.OptionalAccessService;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIOConstants;
import edu.harvard.iq.dataverse.datafile.FilePermissionsService;
import edu.harvard.iq.dataverse.datafile.page.WholeDatasetDownloadLogger;
import edu.harvard.iq.dataverse.dataset.EmbargoAccessService;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestAccessCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenServiceBean;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;


/**
 * @author Leonid Andreev
 * <p>
 * The data (file) access API is based on the DVN access API v.1.0 (that came
 * with the v.3.* of the DVN app) and extended for DVN 4.0 to include some
 * extra fancy functionality, such as subsetting individual columns in tabular
 * data files and more.
 */

@Path("access")
public class Access extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Access.class.getCanonicalName());

    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    DatasetVersionServiceBean versionService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    VariableServiceBean variableService;
    @Inject
    SettingsServiceBean settingsService;
    @EJB
    DDIExportServiceBean ddiExportService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    DataverseSession session;
    @EJB
    WorldMapTokenServiceBean worldMapTokenServiceBean;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    DataverseRoleServiceBean roleService;
    @EJB
    UserNotificationService userNotificationService;
    @EJB
    private FilePermissionsService filePermissionsService;
    @Inject
    private EmbargoAccessService embargoAccessService;
    @Inject
    private WholeDatasetDownloadLogger wholeDatasetDownloadLogger;
    @Inject
    private ImageThumbConverter imageThumbConverter;
    @Inject
    private CitationFactory citationFactory;
    @Inject
    private JsonPrinter jsonPrinter;
    @Inject
    private RoleAssigneeServiceBean roleAssigneeSvc;


    // TODO:
    // versions? -- L.A. 4.0 beta 10
    @Path("datafile/bundle/{fileId}")
    @GET
    @Produces({"application/zip"})
    public BundleDownloadInstance datafileBundle(@PathParam("fileId") String fileId, @QueryParam("gbrecs") Boolean gbrecs) {


        GuestbookResponse gbr = null;

        DataFile datafile = findDataFileOrDieWrapper(fileId);

        // This will throw a ForbiddenException if access isn't authorized:
        checkAuthorization(datafile);

        if (gbrecs == null && datafile.isReleased()) {
            // Write Guestbook record if not done previously and file is released
            User apiTokenUser = getApiTokenUserWithGuestFallbackOnInvalidToken();
            gbr = guestbookResponseService.initAPIGuestbookResponse(datafile.getOwner(), datafile, session, apiTokenUser);
            guestbookResponseService.save(gbr);
        }

        DownloadInfo dInfo = new DownloadInfo(datafile);
        BundleDownloadInstance downloadInstance = new BundleDownloadInstance(dInfo);

        FileMetadata fileMetadata = datafile.getFileMetadata();

        downloadInstance.setFileCitationEndNote(citationFactory.create(fileMetadata).toEndNoteString());
        downloadInstance.setFileCitationRIS(citationFactory.create(fileMetadata).toRISString());
        downloadInstance.setFileCitationBibtex(citationFactory.create(fileMetadata).toBibtexString());

        ByteArrayOutputStream outStream = null;
        outStream = new ByteArrayOutputStream();
        Long dfId = datafile.getId();
        try {
            ddiExportService.exportDataFile(dfId, outStream, null, null);
            downloadInstance.setFileDDIXML(outStream.toString());
        } catch (Exception ex) {
            // if we can't generate the DDI, it's ok, we'll just generate the bundle without it.
            logger.log(Level.WARNING,"Exception during DDI generation.", ex);
        }

        wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(Collections.singletonList(datafile));
        return downloadInstance;

    }

    //Added a wrapper method since the original method throws a wrapped response
    //the access methods return files instead of responses so we convert to a WebApplicationException

    private DataFile findDataFileOrDieWrapper(String fileId) {

        DataFile df = null;

        try {
            df = findDataFileOrDie(fileId);
        } catch (WrappedResponse ex) {
            logger.warning("Access: datafile service could not locate a DataFile object for id " + fileId + "!");
            throw new NotFoundException();
        }
        return df;
    }


    @Path("datafile/{fileId}")
    @GET
    @Produces({"application/xml"})
    public DownloadInstance datafile(@PathParam("fileId") String fileId, @QueryParam("gbrecs") Boolean gbrecs, @Context UriInfo uriInfo, @Context HttpServletResponse response) {

        DataFile df = findDataFileOrDieWrapper(fileId);
        GuestbookResponse gbr = null;

        if (df.isHarvested()) {
            String errorMessage = "Datafile " + fileId + " is a harvested file that cannot be accessed in this Dataverse";
            throw new NotFoundException(errorMessage);
            // (nobody should ever be using this API on a harvested DataFile)!
        }

        if (gbrecs == null && df.isReleased()) {
            // Write Guestbook record if not done previously and file is released
            User apiTokenUser = getApiTokenUserWithGuestFallbackOnInvalidToken();
            gbr = guestbookResponseService.initAPIGuestbookResponse(df.getOwner(), df, session, apiTokenUser);
        }

        // This will throw a ForbiddenException if access isn't authorized:
        checkAuthorization(df);

        DownloadInfo dInfo = new DownloadInfo(df);

        logger.fine("checking if thumbnails are supported on this file.");
        if (FileUtil.isThumbnailSupported(df)) {
            dInfo.addServiceAvailable(new OptionalAccessService("thumbnail", "image/png", "imageThumb=true", "Image Thumbnail (64x64)"));
        }

        if (df.isTabularData()) {
            String originalMimeType = df.getDataTable().getOriginalFileFormat();
            dInfo.addServiceAvailable(new OptionalAccessService("original", originalMimeType, "format=original", "Saved original (" + originalMimeType + ")"));

            dInfo.addServiceAvailable(new OptionalAccessService("R", "application/x-rlang-transport", "format=RData", "Data in R format"));
            dInfo.addServiceAvailable(new OptionalAccessService("preprocessed", "application/json", "format=prep", "Preprocessed data in JSON"));
            dInfo.addServiceAvailable(new OptionalAccessService("subset", "text/tab-separated-values", "variables=&lt;LIST&gt;", "Column-wise Subsetting"));
        }
        DownloadInstance downloadInstance = new DownloadInstance(dInfo);

        if (gbr != null) {
            downloadInstance.setGbr(gbr);
            downloadInstance.setDataverseRequestService(dvRequestService);
            downloadInstance.setCommand(engineSvc);
        }
        for (String key : uriInfo.getQueryParameters().keySet()) {
            String value = uriInfo.getQueryParameters().getFirst(key);
            logger.fine("is download service supported? key=" + key + ", value=" + value);

            if (downloadInstance.checkIfServiceSupportedAndSetConverter(key, value)) {
                // this automatically sets the conversion parameters in
                // the download instance to key and value;
                // TODO: I should probably set these explicitly instead.
                logger.fine("yes!");

                if (downloadInstance.getConversionParam().equals("subset")) {
                    String subsetParam = downloadInstance.getConversionParamValue();
                    String[] variableIdParams = subsetParam.split(",");
                    if (variableIdParams != null && variableIdParams.length > 0) {
                        logger.fine(variableIdParams.length + " tokens;");
                        for (int i = 0; i < variableIdParams.length; i++) {
                            logger.fine("token: " + variableIdParams[i]);
                            String token = variableIdParams[i].replaceFirst("^v", "");
                            Long variableId = null;
                            try {
                                variableId = new Long(token);
                            } catch (NumberFormatException nfe) {
                                variableId = null;
                            }
                            if (variableId != null) {
                                logger.fine("attempting to look up variable id " + variableId);
                                if (variableService != null) {
                                    DataVariable variable = variableService.find(variableId);
                                    if (variable != null) {
                                        if (downloadInstance.getExtraArguments() == null) {
                                            downloadInstance.setExtraArguments(new ArrayList<Object>());
                                        }
                                        logger.fine("putting variable id " + variable.getId() + " on the parameters list of the download instance.");
                                        downloadInstance.getExtraArguments().add(variable);

                                        //if (!variable.getDataTable().getDataFile().getId().equals(sf.getId())) {
                                        //variableList.add(variable);
                                        //}
                                    }
                                } else {
                                    logger.fine("variable service is null.");
                                }
                            }
                        }
                    }
                }

                logger.fine("downloadInstance: " + downloadInstance.getConversionParam() + "," + downloadInstance.getConversionParamValue());

                break;
            } else {
                // Service unknown/not supported/bad arguments, etc.:
                // TODO: throw new ServiceUnavailableException();
            }
        }

        /*
         * Provide "Access-Control-Allow-Origin" header:
         */
        response.setHeader("Access-Control-Allow-Origin", "*");

        /*
         * Provide some browser-friendly headers: (?)
         */
        //return retValue;
        return downloadInstance;
    }


    /*
     * Variants of the Access API calls for retrieving datafile-level
     * Metadata.
     */


    // Metadata format defaults to DDI:
    @Path("datafile/{fileId}/metadata")
    @GET
    @Produces({"text/xml"})
    public String tabularDatafileMetadata(@PathParam("fileId") String fileId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpServletResponse response) throws NotFoundException, ServiceUnavailableException {
        return tabularDatafileMetadataDDI(fileId, exclude, include, response);
    }

    /*
     * This has been moved here, under /api/access, from the /api/meta hierarchy
     * which we are going to retire.
     */
    @Path("datafile/{fileId}/metadata/ddi")
    @GET
    @Produces({"text/xml"})
    public String tabularDatafileMetadataDDI(@PathParam("fileId") String fileId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpServletResponse response) throws NotFoundException, ServiceUnavailableException {
        String retValue = "";

        DataFile dataFile;

        dataFile = findDataFileOrDieWrapper(fileId);

        if (!dataFile.isTabularData()) {
            throw new BadRequestException("tabular data required");
        }

        if (dataFile.isHarvested()) {
            String errorMessage = "Datafile " + fileId + " is a harvested file that cannot be accessed in this Dataverse";
            throw new NotFoundException(errorMessage);
        }

        // This will throw a ForbiddenException if access isn't authorized:
        checkAuthorization(dataFile);

        response.setHeader("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");

        String fileName = dataFile.getFileMetadata().getLabel().replaceAll("\\.tab$", "-ddi.xml");
        response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "application/xml; name=\"" + fileName + "\"");

        ByteArrayOutputStream outStream = null;
        outStream = new ByteArrayOutputStream();
        Long dataFileId = dataFile.getId();
        try {
            ddiExportService.exportDataFile(
                    dataFileId,
                    outStream,
                    exclude,
                    include);

            retValue = outStream.toString();

        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial
            // metadata record requested.
            // We return Service Unavailable.
            throw new ServiceUnavailableException();
        }

        response.setHeader("Access-Control-Allow-Origin", "*");

        return retValue;
    }

    @Path("variable/{varId}/metadata/ddi")
    @GET
    @Produces({"application/xml"})
    public String dataVariableMetadataDDI(@PathParam("varId") Long varId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpServletResponse response) {
        String retValue = "";

        getDatasetFromDataVariable(varId)
                .filter(dt -> embargoAccessService.isRestrictedByEmbargo(dt))
                .ifPresent(dt -> { throw new ForbiddenException(); });

        ByteArrayOutputStream outStream = null;
        try {
            outStream = new ByteArrayOutputStream();

            ddiExportService.exportDataVariable(
                    varId,
                    outStream,
                    exclude,
                    include);
        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial
            // metadata record requested. We simply return an empty string.
            return retValue;
        }

        retValue = outStream.toString();

        response.setHeader("Access-Control-Allow-Origin", "*");

        return retValue;
    }

    /*
     * "Preprocessed data" metadata format:
     * (this was previously provided as a "format conversion" option of the
     * file download form of the access API call)
     */

    @Path("datafile/{fileId}/metadata/preprocessed")
    @GET
    @Produces({"text/xml"})
    public DownloadInstance tabularDatafileMetadataPreprocessed(@PathParam("fileId") String fileId, @Context HttpServletResponse response) throws ServiceUnavailableException {

        DataFile df = findDataFileOrDieWrapper(fileId);

        // This will throw a ForbiddenException if access isn't authorized:
        checkAuthorization(df);
        DownloadInfo dInfo = new DownloadInfo(df);

        if (df.isTabularData()) {
            dInfo.addServiceAvailable(new OptionalAccessService("preprocessed", "application/json", "format=prep", "Preprocessed data in JSON"));
        } else {
            throw new BadRequestException("tabular data required");
        }
        DownloadInstance downloadInstance = new DownloadInstance(dInfo);
        if (downloadInstance.checkIfServiceSupportedAndSetConverter("format", "prep")) {
            logger.fine("Preprocessed data for tabular file " + fileId);
        }

        response.setHeader("Access-Control-Allow-Origin", "*");

        return downloadInstance;
    }

    /*
     * API method for downloading zipped bundles of multiple files:
     */

    // TODO: Rather than only supporting looking up files by their database IDs, consider supporting persistent identifiers.
    @Path("datafiles/{fileIds}")
    @GET
    @Produces({"application/zip"})
    @ApiWriteOperation
    public Response datafiles(@PathParam("fileIds") String fileIds, @QueryParam("gbrecs") Boolean gbrecs,
                              @Context UriInfo uriInfo, @Context HttpServletResponse response) throws WebApplicationException {
        assertOrThrowBadRequest(() -> StringUtils.isNotBlank(fileIds));

        final long zipDownloadSizeLimit = determineDownloadSizeLimit();
        logger.fine("setting zip download size limit to " + zipDownloadSizeLimit + " bytes.");

        User apiTokenUser = getApiTokenUserWithGuestFallbackOnInvalidToken(); //for use in adding gb records if necessary

        final boolean sendOriginalFormat = isOriginalFormatRequested(uriInfo.getQueryParameters());

        StreamingOutput stream = (OutputStream outputStream) -> {
            String[] fileIdParams = fileIds.split(",");
            assertOrThrowBadRequest(() -> fileIdParams.length > 0);
            logger.fine(fileIdParams.length + " tokens;");

            ZipperWrapper zipperWrapper = new ZipperWrapper();
            long sizeTotal = 0L;
            List<DataFile> filesToDownload = new ArrayList<>();

            for (String fileIdParam : fileIdParams) {
                logger.fine("token: " + fileIdParam);

                Long fileId;
                try {
                    fileId = new Long(fileIdParam);
                } catch (NumberFormatException nfe) {
                    logger.log(Level.WARNING, "Cannot parse file id [{0}]. Skipped.", fileIdParam);
                    continue;
                }

                logger.fine("attempting to look up file id " + fileId);
                DataFile file = dataFileService.find(fileId);
                if (file == null) {
                    continue;
                }

                if (isAccessAuthorized(file)) {
                    logger.fine("adding datafile (id=" + file.getId() + ") to the download list of the ZippedDownloadInstance.");
                    if (gbrecs == null && file.isReleased()) {
                        GuestbookResponse gbr = guestbookResponseService.initAPIGuestbookResponse(file.getOwner(), file, session, apiTokenUser);
                        guestbookResponseService.save(gbr);
                    }

                    if (zipperWrapper.isEmpty()) {
                        // This is the first file we can serve - so we now know that we are going to be able
                        // to produce some output.
                        zipperWrapper.init(outputStream);
                        response.setHeader("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");
                        response.setHeader("Content-Type", "application/zip; name=\"dataverse_files.zip\"");
                    }

                    long size = computeFileSize(file, sendOriginalFormat);
                    if (size < (zipDownloadSizeLimit - sizeTotal)) {
                        sizeTotal += zipperWrapper.getZipper().addFileToZipStream(file, sendOriginalFormat);
                        filesToDownload.add(file);
                    } else {
                        String fileName = file.getFileMetadata().getLabel();
                        String mimeType = file.getContentType();
                        zipperWrapper.addToManifest(fileName + " (" + mimeType +
                                ") skipped because the total size of the download bundle exceeded the limit of "
                                + zipDownloadSizeLimit + " bytes.\r\n");
                    }
                } else if (embargoAccessService.isRestrictedByEmbargo(file.getOwner())) {
                    Supplier<MissingParameterValueException> exception = () ->
                            new MissingParameterValueException("[Couldn't retrive embargo date for file id=]" + file.getId());
                    zipperWrapper.addToManifest("File with id=" + file.getId() + " IS EMBARGOED UNTIL "
                            + file.getOwner().getEmbargoDate().getOrElseThrow(exception).toInstant() + "\r\n");
                } else if (file.getLatestFileMetadata().getTermsOfUse().getTermsOfUseType() == TermsOfUseType.RESTRICTED) {
                    zipperWrapper.addToManifest(file.getFileMetadata().getLabel() + " IS RESTRICTED AND CANNOT BE DOWNLOADED\r\n");
                } else {
                    // As of now this errors out. This is bad because the user ends up with a broken zip and manifest
                    // This is good in that the zip ends early so the user does not wait for the results
                    String errorMessage = "Datafile " + fileId + ": no such object available";
                    throw new NotFoundException(errorMessage);
                }
            }

            if (zipperWrapper.isEmpty()) {
                // If the DataFileZipper object is still NULL, it means that there were file ids supplied - but none of
                // the corresponding files were accessible for this user.
                // In which case we don't bother generating any output, and just give them a 403:
                throw new ForbiddenException();
            }

            // Check whether some subset of downloaded files is equal to some whole
            // set of files of some version and log if so
            wholeDatasetDownloadLogger.incrementLogIfDownloadingWholeDataset(filesToDownload);

            // This will add the generated File Manifest to the zipped output, then flush and close the stream:
            zipperWrapper.getZipper().finalizeZipStream();
        };
        return Response.ok(stream).build();
    }

    private long determineDownloadSizeLimit() {
        long limit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipDownloadLimit);
        if (limit == -1) {
            throw new BadRequestException("Download zipped bundles of multiple files option is disabled in this installation");
        }
        return limit != 0 ? limit : Long.MAX_VALUE;
    }

    private void assertOrThrowBadRequest(Supplier<Boolean> constraint) {
        if (!constraint.get()) {
            throw new BadRequestException();
        }
    }

    private boolean isOriginalFormatRequested(MultivaluedMap<String, String> queryParameters) {
        return queryParameters
                .keySet().stream()
                .filter("format"::equals)
                .map(queryParameters::getFirst)
                .anyMatch("original"::equals);
    }

    private long computeFileSize(DataFile file, boolean getOriginal) throws IOException {
        long size;
        // is the original format requested, and is this a tabular datafile, with a preserved original?
        if (getOriginal && file.isTabularData() && StringUtil.nonEmpty(file.getDataTable().getOriginalFileFormat())) {

            // We now store the size of the original file in the database (in DataTable), so we get it for free.
            // However, there may still be legacy datatables for which the size is not saved. so the "inefficient" code
            // is kept, below, as a fallback solution.
            // -- L.A., 4.10

            if (file.getDataTable().getOriginalFileSize() != null) {
                size = file.getDataTable().getOriginalFileSize();
            } else {
                StorageIO<DataFile> storageIO = DataAccess.dataAccess().getStorageIO(file);
                storageIO.open();
                size = storageIO.getAuxObjectSize(StorageIOConstants.SAVED_ORIGINAL_FILENAME_EXTENSION);

                // save it permanently:
                file.getDataTable().setOriginalFileSize(size);
                fileService.saveDataTable(file.getDataTable());
            }
            if (size == 0L) {
                throw new IOException("Invalid file size or accessObject when checking limits of zip file");
            }
        } else {
            size = file.getFilesize();
        }
        return size;
    }


    // TODO: Rather than only supporting looking up files by their database IDs, consider supporting persistent identifiers.
    @Path("fileCardImage/{fileId}")
    @GET
    @Produces({"image/png"})
    public InputStream fileCardImage(@PathParam("fileId") Long fileId) {


        DataFile df = dataFileService.find(fileId);

        if (df == null) {
            logger.warning("Preview: datafile service could not locate a DataFile object for id " + fileId + "!");
            return null;
        }

        if(embargoAccessService.isRestrictedByEmbargo(df.getOwner())) {
            logger.warning("Preview: datafile id[" + fileId + "] is restricted by embargo");
            return null;
        }

        StorageIO<DataFile> thumbnailDataAccess = null;

        try {
            if ("application/pdf".equalsIgnoreCase(df.getContentType())
                    || df.isImage()
                    || "application/zipped-shapefile".equalsIgnoreCase(df.getContentType())) {

                thumbnailDataAccess = imageThumbConverter.getImageThumbnailAsInputStream(df, 48);
                if (thumbnailDataAccess != null && thumbnailDataAccess.getInputStream() != null) {
                    return thumbnailDataAccess.getInputStream();
                }
            }
        } catch (IOException ioEx) {
            return null;
        }

        return null;
    }

    // Note:
    // the Dataverse page is no longer using this method.
    @Path("dsCardImage/{versionId}")
    @GET
    @Produces({"image/png"})
    public InputStream dsCardImage(@PathParam("versionId") Long versionId) {


        DatasetVersion datasetVersion = versionService.getById(versionId);

        if (datasetVersion == null) {
            logger.warning("Preview: Version service could not locate a DatasetVersion object for id " + versionId + "!");
            return null;
        }

        //String imageThumbFileName = null;
        StorageIO thumbnailDataAccess = null;

        // First, check if this dataset has a designated thumbnail image:

        if (datasetVersion.getDataset() != null) {

            DataFile logoDataFile = datasetVersion.getDataset().getThumbnailFile();
            if (logoDataFile != null) {

                try {
                    thumbnailDataAccess = imageThumbConverter.getImageThumbnailAsInputStream(logoDataFile, 48);
                    if (thumbnailDataAccess != null && thumbnailDataAccess.getInputStream() != null) {
                        return thumbnailDataAccess.getInputStream();
                    }
                } catch (IOException ioEx) {
                    thumbnailDataAccess = null;
                }
            }


            // If not, we'll try to use one of the files in this dataset version:
            /*
            if (thumbnailDataAccess == null) {

                if (!datasetVersion.getDataset().isHarvested()) {
                    thumbnailDataAccess = getThumbnailForDatasetVersion(datasetVersion);
                }
            }*/

        }

        return null;
    }

    @Path("dvCardImage/{dataverseId}")
    @GET
    @Produces({"image/png"})
    public InputStream dvCardImage(@PathParam("dataverseId") Long dataverseId) {
        logger.fine("entering dvCardImage");

        Dataverse dataverse = dataverseDao.find(dataverseId);

        if (dataverse == null) {
            logger.warning("Preview: Version service could not locate a DatasetVersion object for id " + dataverseId + "!");
            return null;
        }

        // First, check if the dataverse has a defined logo:

        if (dataverse.getDataverseTheme() != null && dataverse.getDataverseTheme().getLogo() != null && !dataverse.getDataverseTheme().getLogo().equals("")) {
            File dataverseLogoFile = getLogo(dataverse);
            if (dataverseLogoFile != null) {
                logger.fine("dvCardImage: logo file found");
                InputStream in = null;

                try {
                    if (dataverseLogoFile.exists()) {
                        String logoThumbNailPath = dataverseDao.getDataverseLogoThumbnailFilePath(dataverse.getId());
                        if (logoThumbNailPath != null) {
                            in = new FileInputStream(logoThumbNailPath);
                        }
                    }
                } catch (Exception ex) {
                    in = null;
                }
                if (in != null) {
                    logger.fine("dvCardImage: successfully obtained thumbnail for dataverse logo.");
                    return in;
                }
            }
        }

        return null;
    }

    // TODO:
    // put this method into the dataverseservice; use it there
    // -- L.A. 4.0 beta14

    private File getLogo(Dataverse dataverse) {
        if (dataverse.getId() == null) {
            return null;
        }

        DataverseTheme theme = dataverse.getDataverseTheme();
        if (theme != null && theme.getLogo() != null && !theme.getLogo().equals("")) {
            Properties p = System.getProperties();
            String domainRoot = p.getProperty("com.sun.aas.instanceRoot");

            if (domainRoot != null && !"".equals(domainRoot)) {
                return new File(domainRoot + File.separator +
                                        "docroot" + File.separator +
                                        "logos" + File.separator +
                                        dataverse.getLogoOwnerId() + File.separator +
                                        theme.getLogo());
            }
        }

        return null;
    }

    /**
     * Request Access to Restricted File
     *
     * @param fileToRequestAccessId
     * @param headers
     * @return
     * @author sekmiller
     */
    @PUT
    @ApiWriteOperation
    @Path("/datafile/{id}/requestAccess")
    public Response requestFileAccess(@PathParam("id") String fileToRequestAccessId) {

        DataverseRequest dataverseRequest;
        DataFile dataFile;

        try {
            dataFile = findDataFileOrDie(fileToRequestAccessId);
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.fileNotFound", fileToRequestAccessId));
        }

        AuthenticatedUser requestor;

        try {
            requestor = findAuthenticatedUserOrDie();
            dataverseRequest = createDataverseRequest(requestor);
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.fileAccess.failure.noUser", wr.getLocalizedMessage()));
        }

        if (isAccessAuthorized(dataFile)) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.failure.invalidRequest"));
        }

        if (dataFile.getFileAccessRequesters().contains(requestor)) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.failure.requestExists"));
        }

        try {
            engineSvc.submit(new RequestAccessCommand(dataverseRequest, dataFile));

            filePermissionsService.sendRequestFileAccessNotification(dataFile.getOwner(), dataFile.getId(), requestor);
        } catch (CommandException ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.failure.commandError", dataFile.getDisplayName(), ex.getLocalizedMessage()));
        }

        return ok(BundleUtil.getStringFromBundle("access.api.requestAccess.success.for.single.file", dataFile.getDisplayName()));

    }

    /*
     * List Reqeusts to restricted file
     *
     * @author sekmiller
     *
     * @param fileToRequestAccessId
     * @param apiToken
     * @param headers
     * @return
     */
    @GET
    @Path("/datafile/{id}/listRequests")
    public Response listFileAccessRequests(@PathParam("id") String fileToRequestAccessId) {

        DataverseRequest dataverseRequest;

        DataFile dataFile;
        try {
            dataFile = findDataFileOrDie(fileToRequestAccessId);
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestList.fileNotFound", fileToRequestAccessId));
        }

        try {
            dataverseRequest = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.fileAccess.failure.noUser", wr.getLocalizedMessage()));
        }

        if (!(dataverseRequest.getAuthenticatedUser().isSuperuser() ||
                permissionService.requestOn(dataverseRequest, dataFile.getOwner()).has(Permission.ManageDatasetPermissions) ||
                permissionService.requestOn(dataverseRequest, dataFile.getOwner()).has(Permission.ManageMinorDatasetPermissions))) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.rejectAccess.failure.noPermissions"));
        }

        List<AuthenticatedUser> requesters = dataFile.getFileAccessRequesters();

        if (requesters == null || requesters.isEmpty()) {
            List<String> args = Arrays.asList(dataFile.getDisplayName());
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestList.noRequestsFound"));
        }

        JsonArrayBuilder userArray = Json.createArrayBuilder();

        for (AuthenticatedUser au : requesters) {
            userArray.add(jsonPrinter.json(au));
        }

        return ok(userArray);

    }

    /**
     * Grant Access to Restricted File
     *
     * @param fileToRequestAccessId
     * @param identifier
     * @return
     * @author sekmiller
     */
    @PUT
    @ApiWriteOperation
    @Path("/datafile/{id}/grantAccess/{identifier}")
    public Response grantFileAccess(@PathParam("id") String fileToRequestAccessId, @PathParam("identifier") String identifier) {

        DataverseRequest dataverseRequest;
        DataFile dataFile;

        try {
            dataFile = findDataFileOrDie(fileToRequestAccessId);
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.fileNotFound", fileToRequestAccessId));
        }

        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(identifier);

        if (ra == null) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.grantAccess.noAssigneeFound", identifier));
        }

        try {
            dataverseRequest = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.fileAccess.failure.noUser", identifier));
        }

        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(BuiltInRole.FILE_DOWNLOADER);

        try {
            engineSvc.submit(new AssignRoleCommand(ra, fileDownloaderRole, dataFile, dataverseRequest, null));
            if (dataFile.getFileAccessRequesters().remove(ra)) {
                dataFileService.save(dataFile);
            }

        } catch (CommandException ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.grantAccess.failure.commandError", dataFile.getDisplayName(), ex.getLocalizedMessage()));
        }

        try {
            AuthenticatedUser au = (AuthenticatedUser) ra;
            userNotificationService.sendNotificationWithEmail(au, new Timestamp(new Date().getTime()), NotificationType.GRANTFILEACCESS,
                                                              dataFile.getOwner().getId(), NotificationObjectType.AUTHENTICATED_USER);
        } catch (ClassCastException e) {
            //nothing to do here - can only send a notification to an authenticated user
        }

        return ok(BundleUtil.getStringFromBundle("access.api.grantAccess.success.for.single.file", dataFile.getDisplayName()));

    }

    /**
     * Revoke Previously Granted Access to Restricted File
     *
     * @param fileToRequestAccessId
     * @param identifier
     * @return
     * @author sekmiller
     */
    @DELETE
    @ApiWriteOperation
    @Path("/datafile/{id}/revokeAccess/{identifier}")
    public Response revokeFileAccess(@PathParam("id") String fileToRequestAccessId, @PathParam("identifier") String identifier) {

        DataverseRequest dataverseRequest;
        DataFile dataFile;

        try {
            dataFile = findDataFileOrDie(fileToRequestAccessId);
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.fileNotFound", fileToRequestAccessId));
        }

        try {
            dataverseRequest = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.fileAccess.failure.noUser", wr.getLocalizedMessage()));
        }

        if (identifier == null || identifier.equals("")) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.noKey"));
        }

        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(identifier);
        if (ra == null) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.grantAccess.noAssigneeFound", identifier));
        }

        DataverseRole fileDownloaderRole = roleService.findBuiltinRoleByAlias(BuiltInRole.FILE_DOWNLOADER);
        TypedQuery<RoleAssignment> query = em.createNamedQuery(
                "RoleAssignment.listByAssigneeIdentifier_DefinitionPointId_RoleId",
                RoleAssignment.class);
        query.setParameter("assigneeIdentifier", ra.getIdentifier());
        query.setParameter("definitionPointId", dataFile.getId());
        query.setParameter("roleId", fileDownloaderRole.getId());
        List<RoleAssignment> roles = query.getResultList();

        if (roles == null || roles.isEmpty()) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.revokeAccess.noRoleFound", identifier));
        }

        try {
            for (RoleAssignment role : roles) {
                execCommand(new RevokeRoleCommand(role, dataverseRequest));
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        return ok(BundleUtil.getStringFromBundle("access.api.revokeAccess.success.for.single.file", ra.getIdentifier(), dataFile.getDisplayName()));

    }

    /**
     * Reject Access request to Restricted File
     *
     * @param fileToRequestAccessId
     * @param identifier
     * @return
     * @author sekmiller
     */
    @PUT
    @ApiWriteOperation
    @Path("/datafile/{id}/rejectAccess/{identifier}")
    public Response rejectFileAccess(@PathParam("id") String fileToRequestAccessId, @PathParam("identifier") String identifier) {

        DataverseRequest dataverseRequest;
        DataFile dataFile;

        try {
            dataFile = findDataFileOrDie(fileToRequestAccessId);
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.requestAccess.fileNotFound", fileToRequestAccessId));
        }

        RoleAssignee ra = roleAssigneeSvc.getRoleAssignee(identifier);

        if (ra == null) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.grantAccess.noAssigneeFound", identifier));
        }

        try {
            dataverseRequest = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.fileAccess.failure.noUser", identifier));
        }

        if (!(dataverseRequest.getAuthenticatedUser().isSuperuser() ||
                permissionService.requestOn(dataverseRequest, dataFile.getOwner()).has(Permission.ManageDatasetPermissions) ||
                permissionService.requestOn(dataverseRequest, dataFile.getOwner()).has(Permission.ManageMinorDatasetPermissions))) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.rejectAccess.failure.noPermissions"));
        }

        if (dataFile.getFileAccessRequesters().contains(ra)) {
            dataFile.getFileAccessRequesters().remove(ra);
            dataFileService.save(dataFile);

            try {
                AuthenticatedUser au = (AuthenticatedUser) ra;
                userNotificationService.sendNotificationWithEmail(au, new Timestamp(new Date().getTime()), NotificationType.REJECTFILEACCESS,
                                                                  dataFile.getOwner().getId(), NotificationObjectType.AUTHENTICATED_USER);
            } catch (ClassCastException e) {
                //nothing to do here - can only send a notification to an authenticated user
            }

            return ok(BundleUtil.getStringFromBundle("access.api.rejectAccess.success.for.single.file", dataFile.getDisplayName()));

        } else {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("access.api.fileAccess.rejectFailure.noRequest", dataFile.getDisplayName(), ra.getDisplayInfo().getTitle()));
        }
    }

    // checkAuthorization is a convenience method; it calls the boolean method
    // isAccessAuthorized(), the actual workhorse, tand throws a 403 exception if not.

    private void checkAuthorization(DataFile df) throws WebApplicationException {

        if (!isAccessAuthorized(df)) {
            throw new ForbiddenException();
        }
    }


    protected boolean isAccessAuthorized(DataFile df) {

        boolean isRestrictedByEmbargo = embargoAccessService.isRestrictedByEmbargo(df.getOwner());

        if(isRestrictedByEmbargo) {
            return false;
        }

        boolean fileIsInsideAnyReleasedDsVersion = false;

        // We don't need to check permissions on files that are
        // from released Dataset versions and not restricted:
        for (FileMetadata fm : df.getFileMetadatas()) {
            if (fm.getDatasetVersion().isReleased()) {
                if (fm.getTermsOfUse().getTermsOfUseType() != TermsOfUseType.RESTRICTED) {
                    return true;
                }
                fileIsInsideAnyReleasedDsVersion = true;
            }
        }

        // TODO: (IMPORTANT!)
        // Business logic like this should NOT be maintained in individual
        // application fragments.
        // At the moment it is duplicated here, and inside the Dataset page.
        // There are also stubs for file-level permission lookups and caching
        // inside Gustavo's view-scoped PermissionsWrapper.
        // All this logic needs to be moved to the PermissionServiceBean where it will be
        // centrally maintained; with the PermissionsWrapper providing
        // efficient cached lookups to the pages (that often need to make
        // repeated lookups on the same files). Care will need to be taken
        // to preserve the slight differences in logic utilized by the page and
        // this Access call (the page checks the restriction flag on the
        // filemetadata, not the datafile - as it needs to reflect the permission
        // status of the file in the version history).
        // I will open a 4.[34] ticket.
        //
        // -- L.A. 4.2.1

        User apiTokenUser = getApiTokenUserWithGuestFallbackOnInvalidToken();
        User sessionUser = getSessionUserWithGuestFallback();
        
        if (!GuestUser.get().equals(apiTokenUser) && isAccessAuthorizedForUser(apiTokenUser, df, fileIsInsideAnyReleasedDsVersion)) {
            logger.log(Level.FINE, "Token-based auth: user {0} has access rights on the datafile with id: {1}.",
                    new Object[] { apiTokenUser.getIdentifier(), df.getId() });
            return true;
        } else if (!GuestUser.get().equals(sessionUser) && isAccessAuthorizedForUser(sessionUser, df, fileIsInsideAnyReleasedDsVersion)) {
            logger.log(Level.FINE, "Session-based auth: user {0} has access rights on the datafile with id: {1}.",
                    new Object[] { sessionUser.getIdentifier(), df.getId() });
            return true;
        } else if (isAccessAuthorizedForUser(GuestUser.get(), df, fileIsInsideAnyReleasedDsVersion)) {
            logger.log(Level.FINE, "Guest user has access rights on the datafile with id: {1}.", df.getId());
            return true;
        }

        // And if all that failed, we'll still check if the download can be authorized based
        // on the special WorldMap token:

        String apiToken = getRequestApiKey();
        if ((apiToken != null) && (apiToken.length() == 64)) {
            /*
            WorldMap token check
            - WorldMap tokens are 64 chars in length

            - Use the worldMapTokenServiceBean to verify token
                and check permissions against the requested DataFile
             */
            boolean authorizedByWorldMapToken = worldMapTokenServiceBean.isWorldMapTokenAuthorizedForDataFileDownload(apiToken, df);
            logger.fine("WorldMap token-based auth: Token is valid for the requested datafile");
            return authorizedByWorldMapToken;
        }

        if (!GuestUser.get().equals(sessionUser)) {
            logger.log(Level.FINE, "Session-based auth: user {0} has NO access rights on the requested datafile.", sessionUser.getIdentifier());
        } else if (!GuestUser.get().equals(apiTokenUser)) {
            logger.log(Level.FINE, "Token-based auth: user {0} has NO access rights on the requested datafile.", apiTokenUser.getIdentifier());
        } else {
            logger.log(Level.FINE, "Guest user has NO access rights on the requested datafile.");
        }

        return false;
    }

    private boolean isAccessAuthorizedForUser(User user, DataFile datafile, boolean fileIsInsideAnyReleasedDsVersion) {
        if (fileIsInsideAnyReleasedDsVersion) {
            return permissionService.requestOn(createDataverseRequest(user), datafile).has(Permission.DownloadFile);
        } else {
            return permissionService.requestOn(createDataverseRequest(user), datafile.getOwner()).has(Permission.ViewUnpublishedDataset);
        }
    }

    private User getApiTokenUserWithGuestFallbackOnInvalidToken() {
        return Try.of(this::findUserOrDie)
                .onFailure(throwable -> logger.log(Level.FINE, "Failed finding user with apiToken", throwable))
                .getOrElse(GuestUser.get());
    }

    private User getSessionUserWithGuestFallback() {
        return Option.of(session)
                .map(DataverseSession::getUser)
                .peek(user -> logger.log(Level.FINE, "User associated with the session is {0}", user.getIdentifier()))
                .getOrElse(() -> {
                    logger.fine("Session is null. Assuming guest user");
                    return GuestUser.get();
                });
    }

    private Optional<Dataset> getDatasetFromDataVariable(Long dataVariableId) {
        return Optional.ofNullable(variableService.find(dataVariableId).getDataTable().getDataFile().getOwner());
    }

}
