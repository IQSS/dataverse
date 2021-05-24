package edu.harvard.iq.dataverse.datafile.file;

import com.amazonaws.services.glacier.model.MissingParameterValueException;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.ZipperWrapper;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.dashboard.DashboardUsersService;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIOConstants;
import edu.harvard.iq.dataverse.datafile.page.WholeDatasetDownloadLogger;
import edu.harvard.iq.dataverse.dataset.EmbargoAccessService;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenServiceBean;
import io.vavr.control.Try;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class FileDownloadAPIHandler {
    private static final Logger logger = Logger.getLogger(FileDownloadAPIHandler.class.getCanonicalName());

    private DatasetVersionRepository datasetVersionRepository;
    private SettingsServiceBean settingsService;
    private GuestbookResponseServiceBean guestbookResponseService;
    private PermissionServiceBean permissionService;
    private DataverseRequestServiceBean dataverseRequestService;
    private EmbargoAccessService embargoAccessService;
    private WholeDatasetDownloadLogger wholeDatasetDownloadLogger;
    private DataverseSession session;
    private DataFileServiceBean fileService;
    private PrivateUrlServiceBean privateUrlSvc;
    private HttpServletRequest httpRequest;
    private AuthenticationServiceBean authenticationService;
    private SystemConfig systemConfig;
    private UserServiceBean userService;
    private WorldMapTokenServiceBean worldMapTokenService;

    public FileDownloadAPIHandler() {
    }

    @Inject
    public FileDownloadAPIHandler(DatasetVersionRepository datasetVersionRepository, SettingsServiceBean settingsService,
                                  GuestbookResponseServiceBean guestbookResponseService, PermissionServiceBean permissionService,
                                  DataverseRequestServiceBean dataverseRequestService, EmbargoAccessService embargoAccessService,
                                  WholeDatasetDownloadLogger wholeDatasetDownloadLogger, DataverseSession session, DataFileServiceBean fileService,
                                  PrivateUrlServiceBean privateUrlSvc, HttpServletRequest httpRequest, AuthenticationServiceBean authenticationService,
                                  SystemConfig systemConfig, UserServiceBean userService, WorldMapTokenServiceBean worldMapTokenService) {
        this.datasetVersionRepository = datasetVersionRepository;
        this.settingsService = settingsService;
        this.guestbookResponseService = guestbookResponseService;
        this.permissionService = permissionService;
        this.dataverseRequestService = dataverseRequestService;
        this.embargoAccessService = embargoAccessService;
        this.wholeDatasetDownloadLogger = wholeDatasetDownloadLogger;
        this.session = session;
        this.fileService = fileService;
        this.privateUrlSvc = privateUrlSvc;
        this.httpRequest = httpRequest;
        this.authenticationService = authenticationService;
        this.systemConfig = systemConfig;
        this.userService = userService;
        this.worldMapTokenService = worldMapTokenService;
    }

    public StreamingOutput downloadFiles(User apiTokenUser,
                                         String apiToken,
                                         String versionId,
                                         boolean originalFileFormat,
                                         boolean gbrecs) {

        final long zipDownloadSizeLimit = determineDownloadSizeLimit();

        Optional<DatasetVersion> dsvFound = datasetVersionRepository.findById(Long.parseLong(versionId));
        DatasetVersion dsv = dsvFound.orElseThrow(() -> new NotFoundException("DatasetVersion with id:" + versionId + " was not found."));
        List<DataFile> files = dsv.getFileMetadatas().stream()
                                  .map(FileMetadata::getDataFile)
                                  .collect(Collectors.toList());

        return (OutputStream outputStream) -> {

            ZipperWrapper zipperWrapper = new ZipperWrapper();
            long sizeTotal = 0L;
            List<DataFile> filesToDownload = new ArrayList<>();

            if (isAccessAuthorized(dsv, apiToken)) {

                for (DataFile file : files) {

                    if (embargoAccessService.isRestrictedByEmbargo(file.getOwner())) {
                        Instant embargoDate = file
                                .getOwner()
                                .getEmbargoDate()
                                .getOrElseThrow(() -> new MissingParameterValueException("[Couldn't retrive embargo date for file id=]" + file
                                        .getId()))
                                .toInstant();

                        zipperWrapper.addToManifest("File with id=" + file.getId() + " IS EMBARGOED UNTIL "
                                                            + embargoDate + "\r\n");
                    } else if (isFileRestricted(file)) {
                        zipperWrapper.addToManifest(file
                                                            .getFileMetadata()
                                                            .getLabel() + " IS RESTRICTED AND CANNOT BE DOWNLOADED\r\n");
                    } else {

                        if (gbrecs && file.isReleased()) {
                            GuestbookResponse gbr = guestbookResponseService.initAPIGuestbookResponse(file.getOwner(), file, session, apiTokenUser);
                            guestbookResponseService.save(gbr);
                        }

                        if (zipperWrapper.isEmpty()) {
                            // This is the first file we can serve - so we now know that we are going to be able
                            // to produce some output.
                            zipperWrapper.init(outputStream);
                        }

                        long size = computeFileSize(file, originalFileFormat);
                        if (size < (zipDownloadSizeLimit - sizeTotal)) {
                            sizeTotal += zipperWrapper.getZipper().addFileToZipStream(file, originalFileFormat);
                            filesToDownload.add(file);
                        } else {
                            String fileName = file.getFileMetadata().getLabel();
                            String mimeType = file.getContentType();
                            zipperWrapper.addToManifest(fileName + " (" + mimeType +
                                                                ") skipped because the total size of the download bundle exceeded the limit of "
                                                                + zipDownloadSizeLimit + " bytes.\r\n");
                        }

                    }
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
            wholeDatasetDownloadLogger.incrementLogForDownloadingWholeDataset(dsv);

            // This will add the generated File Manifest to the zipped output, then flush and close the stream:
            zipperWrapper.getZipper().finalizeZipStream();
        };
    }

    private boolean isFileRestricted(DataFile file) {
        return file.getFileMetadata()
                   .getTermsOfUse()
                   .getTermsOfUseType() == FileTermsOfUse.TermsOfUseType.RESTRICTED;
    }

    private long determineDownloadSizeLimit() {
        long limit = settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.ZipDownloadLimit);
        if (limit == -1) {
            throw new BadRequestException("Download zipped bundles of multiple files option is disabled in this installation");
        }
        return limit != 0 ? limit : Long.MAX_VALUE;
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

    private boolean isAccessAuthorized(DatasetVersion dsv, String apiToken) {
        Dataset ds = dsv.getDataset();

        Optional<User> apiTokenUser = Try.of(this::findUserOrDie)
                                         .onFailure(throwable -> logger.log(Level.FINE, "Failed finding user for provided token", throwable))
                                         .toJavaOptional();

        boolean isRestrictedByEmbargo = apiTokenUser
                .map(user -> embargoAccessService.isRestrictedByEmbargo(ds))
                .orElse(false);

        if (isRestrictedByEmbargo) {
            return false;
        }

        // First, check if the file belongs to a released Dataset version:
        boolean published = dsv.isPublished();

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

        User user = null;

        /**
         * Authentication/authorization:
         *
         * note that the fragment below - that retrieves the session object
         * and tries to find the user associated with the session - is really
         * for logging/debugging purposes only; for practical purposes, it
         * would be enough to just call "permissionService.on(df).has(Permission.DownloadFile)"
         * and the method does just that, tries to authorize for the user in
         * the current session (or guest user, if no session user is available):
         */

        if (session != null) {
            if (session.getUser().isAuthenticated()) {
                user = session.getUser();
            } else {
                logger.fine("User associated with the session is not an authenticated user.");
                if (session.getUser() instanceof PrivateUrlUser) {
                    logger.fine("User associated with the session is a PrivateUrlUser user.");
                    user = session.getUser();
                }
                if (session.getUser() instanceof GuestUser) {
                    logger.fine("User associated with the session is indeed a guest user.");
                }
            }
        } else {
            logger.fine("Session is null.");
        }

        // OK, let's revisit the case of non-restricted files, this time in
        // an unpublished version:
        // (if (published) was already addressed above)

        if (!published) {
            // If the file is not published, they can still download the file, if the user
            // has the permission to view unpublished versions:

            if (user != null) {
                // used in JSF context
                if (permissionService
                        .requestOn(dataverseRequestService.getDataverseRequest(), ds)
                        .has(Permission.ViewUnpublishedDataset)) {
                    // it's not unthinkable, that a null user (i.e., guest user) could be given
                    // the ViewUnpublished permission!
                    logger.log(Level.FINE, "Session-based auth: user {0} has access rights on the non-restricted, unpublished datafile.", user
                            .getIdentifier());
                    return true;
                }
            }

            if (apiTokenUser.isPresent()) {
                // used in an API context
                if (permissionService
                        .requestOn(createDataverseRequest(apiTokenUser.get()), ds)
                        .has(Permission.ViewUnpublishedDataset)) {
                    logger.log(Level.FINE, "Session-based auth: user {0} has access rights on the non-restricted, unpublished datafile.", apiTokenUser
                            .get()
                            .getIdentifier());
                    return true;
                }
            }

            // last option - guest user in either contexts
            // Guset user is impled by the code above.
            if (permissionService
                    .requestOn(dataverseRequestService.getDataverseRequest(), ds)
                    .has(Permission.ViewUnpublishedDataset)) {
                return true;
            }


            // We don't want to return false just yet.
            // If all else fails, we'll want to use the special WorldMapAuth
            // token authentication before we give up.
            //return false;
        } else {

            // OK, this is a restricted file.

            boolean hasAccessToRestrictedBySession = false;
            boolean hasAccessToRestrictedByToken = false;

            if (session != null && permissionService
                    .requestOn(createDataverseRequest(session.getUser()), ds)
                    .has(Permission.DownloadFile)) {
                hasAccessToRestrictedBySession = true;
            } else if (apiTokenUser.isPresent() && permissionService
                    .requestOn(createDataverseRequest(apiTokenUser.get()), ds)
                    .has(Permission.DownloadFile)) {
                hasAccessToRestrictedByToken = true;
            }

            if (hasAccessToRestrictedBySession || hasAccessToRestrictedByToken) {
                if (hasAccessToRestrictedBySession) {
                    if (user != null) {
                        logger.log(Level.FINE, "Session-based auth: user {0} is granted access to the restricted, published datafile.", user
                                .getIdentifier());
                    } else {
                        logger.fine("Session-based auth: guest user is granted access to the restricted, published datafile.");
                    }
                } else {
                    logger.log(Level.FINE, "Token-based auth: user {0} is granted access to the restricted, published datafile.", apiTokenUser
                            .get()
                            .getIdentifier());
                }
                return true;
            }
        }
        if (apiTokenUser.isPresent()) {
            user = apiTokenUser.get();
            if (permissionService.requestOn(createDataverseRequest(user), ds).has(Permission.DownloadFile)) {
                if (published) {
                    logger.log(Level.FINE, "API token-based auth: User {0} has rights to access the datafile.", user.getIdentifier());
                    return true;
                } else {
                    // if the file is NOT published, we will let them download the
                    // file ONLY if they also have the permission to view
                    // unpublished versions:
                    if (permissionService
                            .requestOn(createDataverseRequest(user), ds)
                            .has(Permission.ViewUnpublishedDataset)) {
                        logger.log(Level.FINE, "API token-based auth: User {0} has rights to access the (unpublished) datafile.", user
                                .getIdentifier());
                        return true;
                    } else {
                        logger.log(Level.FINE, "API token-based auth: User {0} is not authorized to access the (unpublished) datafile.", user
                                .getIdentifier());
                    }
                }
            } else {
                logger.log(Level.FINE, "API token-based auth: User {0} is not authorized to access the datafile.", user.getIdentifier());
            }

            boolean isWorldMapTokenPresentAndAuthorized = dsv.getFileMetadatas()
                                                             .stream()
                                                             .map(FileMetadata::getDataFile)
                                                             .anyMatch(df -> worldMapTokenService.isWorldMapTokenAuthorizedForDataFileDownload(apiToken, df));

            if (!isWorldMapTokenPresentAndAuthorized) {
                return false;
            }

            logger.fine("WorldMap token-based auth: Token is valid for the requested datafile");
            return true;

        }

        return false;
    }

    private DataverseRequest createDataverseRequest(User u) {
        return new DataverseRequest(u, httpRequest);
    }

    protected User findUserOrDie() throws AbstractApiBean.WrappedResponse {
        final String requestApiKey = getRequestApiKey();
        if (requestApiKey == null) {
            return GuestUser.get();
        }
        PrivateUrlUser privateUrlUser = privateUrlSvc.getPrivateUrlUserFromToken(requestApiKey);
        if (privateUrlUser != null) {
            return privateUrlUser;
        }
        return findAuthenticatedUserOrDie(requestApiKey);
    }

    protected String getRequestApiKey() {
        String headerParamApiKey = httpRequest.getHeader("X-Dataverse-key");
        String queryParamApiKey = httpRequest.getParameter("key");

        return headerParamApiKey != null ? headerParamApiKey : queryParamApiKey;
    }

    private AuthenticatedUser findAuthenticatedUserOrDie(String key) throws AbstractApiBean.WrappedResponse {
        AuthenticatedUser authUser = authenticationService.lookupUser(key);
        if (authUser != null) {
            if (!systemConfig.isReadonlyMode()) {
                authUser = userService.updateLastApiUseTime(authUser);
            }

            return authUser;
        }
        throw new AbstractApiBean.WrappedResponse(badApiKey(key));
    }

    protected Response badApiKey(String apiKey) {
        return error(Response.Status.UNAUTHORIZED, (apiKey != null) ?
                "Bad api key " :
                "Please provide a key query parameter (?key=XXX) or via the HTTP header " + "X-Dataverse-key");
    }

    protected static Response error(Response.Status sts, String msg) {
        return Response.status(sts)
                       .entity(NullSafeJsonBuilder.jsonObjectBuilder()
                                                  .add("status", "ERROR")
                                                  .add("message", msg).build()
                       ).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
