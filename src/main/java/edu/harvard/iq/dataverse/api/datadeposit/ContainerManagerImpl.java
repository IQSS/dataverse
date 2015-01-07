package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.abdera.parser.ParseException;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ContainerManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordEntry;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

public class ContainerManagerImpl implements ContainerManager {

    private static final Logger logger = Logger.getLogger(ContainerManagerImpl.class.getCanonicalName());

    @EJB
    protected EjbDataverseEngine engineSvc;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    IndexServiceBean indexService;
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;
    @EJB
    ForeignMetadataImportServiceBean foreignMetadataImportService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;
    SwordConfigurationImpl swordConfiguration = new SwordConfigurationImpl();
    @EJB
    SwordServiceBean swordService;

    @Override
    public DepositReceipt getEntry(String uri, Map<String, String> map, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordServerException, SwordError, SwordAuthException {
        AuthenticatedUser user = swordAuth.auth(authCredentials);
        logger.fine("getEntry called with url: " + uri);
        urlManager.processUrl(uri);
        String targetType = urlManager.getTargetType();
        if (!targetType.isEmpty()) {
            logger.fine("operating on target type: " + urlManager.getTargetType());
            if ("study".equals(targetType)) {
                String globalId = urlManager.getTargetIdentifier();
                Dataset dataset = datasetService.findByGlobalId(globalId);
                if (dataset != null) {
                    Dataverse dvThatOwnsDataset = dataset.getOwner();
                    if (swordAuth.hasAccessToModifyDataverse(user, dvThatOwnsDataset)) {
                        ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                        String baseUrl = urlManager.getHostnamePlusBaseUrlPath(uri);
                        DepositReceipt depositReceipt = receiptGenerator.createDatasetReceipt(baseUrl, dataset);
                        if (depositReceipt != null) {
                            return depositReceipt;
                        } else {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not generate deposit receipt.");
                        }
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + user.getDisplayInfo().getTitle() + " is not authorized to retrieve entry for " + dataset.getGlobalId());
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataset based on URL: " + uri);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unsupported target type (" + targetType + ") in URL: " + uri);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine target type from URL: " + uri);
        }
    }

    @Override
    public DepositReceipt replaceMetadata(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        AuthenticatedUser user = swordAuth.auth(authCredentials);
        logger.fine("replaceMetadata called with url: " + uri);
        urlManager.processUrl(uri);
        String targetType = urlManager.getTargetType();
        if (!targetType.isEmpty()) {
            logger.fine("operating on target type: " + urlManager.getTargetType());
            if ("dataverse".equals(targetType)) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Metadata replace of dataverse is not supported.");
            } else if ("study".equals(targetType)) {
                logger.fine("replacing metadata for dataset");
                // do a sanity check on the XML received
                try {
                    SwordEntry swordEntry = deposit.getSwordEntry();
                    logger.fine("deposit XML received by replaceMetadata():\n" + swordEntry);
                } catch (ParseException ex) {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Can not replace dataset metadata due to malformed Atom entry: " + ex);
                }

                String globalId = urlManager.getTargetIdentifier();
                Dataset dataset = datasetService.findByGlobalId(globalId);
                if (dataset != null) {
                    SwordUtil.datasetLockCheck(dataset);
                    Dataverse dvThatOwnsDataset = dataset.getOwner();
                    if (swordAuth.hasAccessToModifyDataverse(user, dvThatOwnsDataset)) {
                        DatasetVersion datasetVersion = dataset.getEditVersion();
                        // erase all metadata before creating populating dataset version
                        List<DatasetField> emptyDatasetFields = new ArrayList<>();
                        datasetVersion.setDatasetFields(emptyDatasetFields);
                        String foreignFormat = SwordUtil.DCTERMS;
                        try {
                            foreignMetadataImportService.importXML(deposit.getSwordEntry().toString(), foreignFormat, datasetVersion);
                        } catch (Exception ex) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "problem calling importXML: " + ex);
                        }
                        swordService.addDatasetContact(datasetVersion, user);
                        swordService.addDatasetSubject(datasetVersion);
                        try {
                            engineSvc.submit(new UpdateDatasetCommand(dataset, user));
                        } catch (CommandException ex) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "problem updating dataset: " + ex);
                        }
                        ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                        String baseUrl = urlManager.getHostnamePlusBaseUrlPath(uri);
                        DepositReceipt depositReceipt = receiptGenerator.createDatasetReceipt(baseUrl, dataset);
                        return depositReceipt;
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + user.getDisplayInfo().getTitle() + " is not authorized to modify dataverse " + dvThatOwnsDataset.getAlias());
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataset based on global id (" + globalId + ") in URL: " + uri);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unknown target type specified on which to replace metadata: " + uri);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No target specified on which to replace metadata: " + uri);
        }
    }

    @Override
    public DepositReceipt replaceMetadataAndMediaResource(String string, Deposit dpst, AuthCredentials ac, SwordConfiguration sc) throws SwordError, SwordServerException, SwordAuthException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DepositReceipt addMetadataAndResources(String string, Deposit dpst, AuthCredentials ac, SwordConfiguration sc) throws SwordError, SwordServerException, SwordAuthException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DepositReceipt addMetadata(String string, Deposit dpst, AuthCredentials ac, SwordConfiguration sc) throws SwordError, SwordServerException, SwordAuthException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DepositReceipt addResources(String string, Deposit dpst, AuthCredentials ac, SwordConfiguration sc) throws SwordError, SwordServerException, SwordAuthException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteContainer(String uri, AuthCredentials authCredentials, SwordConfiguration sc) throws SwordError, SwordServerException, SwordAuthException {
        AuthenticatedUser user = swordAuth.auth(authCredentials);
        logger.fine("deleteContainer called with url: " + uri);
        urlManager.processUrl(uri);
        logger.fine("original url: " + urlManager.getOriginalUrl());
        if (!"edit".equals(urlManager.getServlet())) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "edit servlet expected, not " + urlManager.getServlet());
        }
        String targetType = urlManager.getTargetType();
        if (!targetType.isEmpty()) {
            logger.fine("operating on target type: " + urlManager.getTargetType());

            if ("dataverse".equals(targetType)) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Dataverses can not be deleted via the Data Deposit API but other Dataverse APIs may support this operation.");
            } else if ("study".equals(targetType)) {
                String globalId = urlManager.getTargetIdentifier();
                logger.info("globalId: " + globalId);
                if (globalId != null) {
                    Dataset dataset = dataset = datasetService.findByGlobalId(globalId);
                    if (dataset != null) {
                        SwordUtil.datasetLockCheck(dataset);
                        Dataverse dvThatOwnsDataset = dataset.getOwner();
                        if (!swordAuth.hasAccessToModifyDataverse(user, dvThatOwnsDataset)) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + user.getDisplayInfo().getTitle() + " is not authorized to modify " + dvThatOwnsDataset.getAlias());
                        }
                        DatasetVersion.VersionState datasetVersionState = dataset.getLatestVersion().getVersionState();
                        if (dataset.isReleased()) {
                            if (datasetVersionState.equals(DatasetVersion.VersionState.DRAFT)) {
                                logger.info("destroying working copy version of dataset " + dataset.getGlobalId());
                                try {
                                    engineSvc.submit(new DeleteDatasetVersionCommand(user, dataset));
                                } catch (CommandException ex) {
                                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Can't delete dataset version for " + dataset.getGlobalId() + ": " + ex);
                                }
                                logger.info("dataset version deleted for dataset id " + dataset.getId());
                            } else if (datasetVersionState.equals(DatasetVersion.VersionState.RELEASED)) {
                                throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "Deaccessioning a dataset is no longer supported as of Data Deposit API version in URL (" + swordConfiguration.getBaseUrlPathV1() + ") Equivalent functionality is being developed at https://github.com/IQSS/dataverse/issues/778");
                            } else if (datasetVersionState.equals(DatasetVersion.VersionState.DEACCESSIONED)) {
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Lastest version of dataset " + dataset.getGlobalId() + " has already been deaccessioned.");
                            } else if (datasetVersionState.equals(DatasetVersion.VersionState.ARCHIVED)) {
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Lastest version of dataset " + dataset.getGlobalId() + " has been archived and can not be deleted or deaccessioned.");
                            } else if (datasetVersionState.equals(DatasetVersion.VersionState.IN_REVIEW)) {
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Lastest version of dataset " + dataset.getGlobalId() + " is in review and can not be deleted or deaccessioned.");
                            } else {
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Operation not valid for dataset " + dataset.getGlobalId() + " in state " + datasetVersionState);
                            }
                        } else {
                            // dataset has never been published, this is just a sanity check (should always be draft)
                            if (datasetVersionState.equals(DatasetVersion.VersionState.DRAFT)) {
                                try {
                                    engineSvc.submit(new DeleteDatasetCommand(dataset, user));
                                    logger.info("dataset deleted");
                                } catch (CommandExecutionException ex) {
                                    // internal error
                                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Can't delete dataset: " + ex.getMessage());
                                } catch (CommandException ex) {
                                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Can't delete dataset: " + ex.getMessage());
                                }
                            } else {
                                // we should never get here. throw an error explaining why
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "dataset is in illegal state (not released yet not in draft)");
                            }
                        }
                    } else {
                        throw new SwordError(404);
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataset to delete from URL: " + uri);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unsupported delete target in URL:" + uri);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No target for deletion specified");
        }
    }

    @Override
    public DepositReceipt useHeaders(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        logger.fine("uri was " + uri);
        logger.fine("isInProgress:" + deposit.isInProgress());
        AuthenticatedUser user = swordAuth.auth(authCredentials);
        urlManager.processUrl(uri);
        String targetType = urlManager.getTargetType();
        if (!targetType.isEmpty()) {
            logger.fine("operating on target type: " + urlManager.getTargetType());
            if ("study".equals(targetType)) {
                String globalId = urlManager.getTargetIdentifier();
                if (globalId != null) {
                    Dataset dataset = null;
                    try {
                        dataset = datasetService.findByGlobalId(globalId);
                    } catch (EJBException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataset based on global id (" + globalId + ") in URL: " + uri);
                    }
                    if (dataset != null) {
                        Dataverse dvThatOwnsDataset = dataset.getOwner();
                        if (swordAuth.hasAccessToModifyDataverse(user, dvThatOwnsDataset)) {
                            if (!deposit.isInProgress()) {
                                /**
                                 * We are considering a draft version of a study
                                 * to be incomplete and are saying that sending
                                 * isInProgress=false means the study version is
                                 * complete and can be released.
                                 *
                                 * 9.2. Deposit Incomplete
                                 *
                                 * "If In-Progress is true, the server SHOULD
                                 * expect the client to provide further updates
                                 * to the item some undetermined time in the
                                 * future. Details of how this is implemented is
                                 * dependent on the server's purpose. For
                                 * example, a repository system may hold items
                                 * which are marked In-Progress in a workspace
                                 * until such time as a client request indicates
                                 * that the deposit is complete." --
                                 * http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#continueddeposit_incomplete
                                 */
                                if (!dataset.getLatestVersion().getVersionState().equals(DatasetVersion.VersionState.RELEASED)) {
                                    Command<Dataset> cmd;
                                    try {
                                        boolean doMinorVersionBump = false;
                                        if (dataset.getLatestVersion().isMinorUpdate()) {
                                            doMinorVersionBump = true;
                                        } else {
                                            doMinorVersionBump = false;
                                        }
                                        cmd = new PublishDatasetCommand(dataset, user, doMinorVersionBump);
                                        dataset = engineSvc.submit(cmd);
                                    } catch (CommandException ex) {
                                        String msg = "Unable to publish dataset: " + ex;
                                        logger.severe(msg + ": " + ex.getMessage());
                                        throw SwordUtil.throwRegularSwordErrorWithoutStackTrace(msg);
                                    }
                                    ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                                    String baseUrl = urlManager.getHostnamePlusBaseUrlPath(uri);
                                    DepositReceipt depositReceipt = receiptGenerator.createDatasetReceipt(baseUrl, dataset);
                                    return depositReceipt;
                                } else {
                                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Latest version of dataset " + globalId + " has already been released.");
                                }
                            } else {
                                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Pass 'In-Progress: false' header to release a dataset.");
                            }
                        } else {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + user.getDisplayInfo().getTitle() + " is not authorized to modify dataverse " + dvThatOwnsDataset.getAlias());
                        }
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataset using globalId " + globalId);
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to find globalId for dataset in URL:" + uri);
                }
            } else if ("dataverse".equals(targetType)) {
                String dvAlias = urlManager.getTargetIdentifier();
                if (dvAlias != null) {
                    Dataverse dvToRelease = dataverseService.findByAlias(dvAlias);
                    if (dvToRelease != null) {
                        if (!swordAuth.hasAccessToModifyDataverse(user, dvToRelease)) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + user.getDisplayInfo().getTitle() + " is not authorized to modify dataverse " + dvAlias);
                        }
                        if (deposit.isInProgress()) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unpublishing a dataverse is not supported.");
                        }
                        PublishDataverseCommand cmd = new PublishDataverseCommand(user, dvToRelease);
                        try {
                            engineSvc.submit(cmd);
                            ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                            String baseUrl = urlManager.getHostnamePlusBaseUrlPath(uri);
                            DepositReceipt depositReceipt = receiptGenerator.createDataverseReceipt(baseUrl, dvToRelease);
                            return depositReceipt;
                        } catch (CommandException ex) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't publish dataverse " + dvAlias + ": " + ex);
                        }
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataverse based on alias in URL: " + uri);
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to find dataverse alias in URL: " + uri);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "unsupported target type (" + targetType + ") in URL:" + uri);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Target type missing from URL: " + uri);
        }
    }

    @Override
    public boolean isStatementRequest(String uri, Map<String, String> map, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        urlManager.processUrl(uri);
        String servlet = urlManager.getServlet();
        if (servlet != null) {
            return servlet.equals("statement");
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine requested IRI from URL: " + uri);
        }

    }

}
