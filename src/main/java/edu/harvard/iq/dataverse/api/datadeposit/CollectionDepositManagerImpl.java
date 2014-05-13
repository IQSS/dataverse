package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValidator;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.apache.commons.io.FileUtils;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

public class CollectionDepositManagerImpl implements CollectionDepositManager {

    private static final Logger logger = Logger.getLogger(CollectionDepositManagerImpl.class.getCanonicalName());
    @EJB
    DataverseServiceBean dataverseService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;
    @EJB
    EjbDataverseEngine engineSvc;
    @EJB
    DatasetFieldServiceBean datasetFieldService;

    @Override
    public DepositReceipt createNew(String collectionUri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {

        DataverseUser dataverseUser = swordAuth.auth(authCredentials);

        urlManager.processUrl(collectionUri);
        String dvAlias = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("dataverse") && dvAlias != null) {

            logger.fine("attempting deposit into this dataverse alias: " + dvAlias);

            Dataverse dvThatWillOwnDataset = dataverseService.findByAlias(dvAlias);

            if (dvThatWillOwnDataset != null) {

                if (swordAuth.hasAccessToModifyDataverse(dataverseUser, dvThatWillOwnDataset)) {

                    logger.fine("multipart: " + deposit.isMultipart());
                    logger.fine("binary only: " + deposit.isBinaryOnly());
                    logger.fine("entry only: " + deposit.isEntryOnly());
                    logger.fine("in progress: " + deposit.isInProgress());
                    logger.fine("metadata relevant: " + deposit.isMetadataRelevant());

                    if (deposit.isEntryOnly()) {
                        logger.fine("deposit XML received by createNew():\n" + deposit.getSwordEntry());
                        // require title *and* exercise the SWORD jar a bit
                        Map<String, List<String>> dublinCore = deposit.getSwordEntry().getDublinCore();
                        if (dublinCore.get("title") == null || dublinCore.get("title").get(0) == null || dublinCore.get("title").get(0).isEmpty()) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "title field is required");
                        }

                        if (dublinCore.get("date") != null) {
                            String date = dublinCore.get("date").get(0);
                            if (date != null) {
                                /**
                                 * @todo re-enable this. use
                                 * datasetFieldValidator.isValid?
                                 */
//                                boolean isValid = DateUtil.validateDate(date);
//                                if (!isValid) {
//                                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid date: '" + date + "'.  Valid formats are YYYY-MM-DD, YYYY-MM, or YYYY.");
//                                }
                            }
                        }

                        /**
                         * @todo think about the implications of no longer using
                         * importStudy(), such as the comment below... do we
                         * really need to write the XML to disk?
                         */
                        // instead of writing a tmp file, maybe importStudy() could accept an InputStream?
                        String tmpDirectory = config.getTempDirectory();
                        if (tmpDirectory == null) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not determine temp directory");
                        }
                        String uploadDirPath = tmpDirectory + File.separator + "import" + File.separator + dvThatWillOwnDataset.getId();
                        File uploadDir = new File(uploadDirPath);
                        if (!uploadDir.exists()) {
                            if (!uploadDir.mkdirs()) {
                                logger.info("couldn't create directory: " + uploadDir.getAbsolutePath());
                                throw new SwordServerException("Couldn't create upload directory.");
                            }
                        }
                        String tmpFilePath = uploadDirPath + File.separator + "newStudyViaSwordv2.xml";
                        File tmpFile = new File(tmpFilePath);
                        try {
                            FileUtils.writeStringToFile(tmpFile, deposit.getSwordEntry().getEntry().toString());
                        } catch (IOException ex) {
                            logger.info("couldn't write temporary file: " + tmpFile.getAbsolutePath());
                            throw new SwordServerException("Couldn't write temporary file");
                        } finally {
                            uploadDir.delete();
                        }

                        /**
                         * @todo properly create a dataset and datasetVersion
                         */
                        Dataset dataset = new Dataset();
                        dataset.setOwner(dvThatWillOwnDataset);
                        dataset.setIdentifier("myIdentifier");
                        dataset.setProtocol("myProtocol");

                        DatasetVersion newDatasetVersion = dataset.getVersions().get(0);
                        newDatasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);

                        List<DatasetField> datasetFields = new ArrayList<>();
                        DatasetField titleDatasetField = new DatasetField();
                        DatasetFieldType titleDatasetFieldType = datasetFieldService.findByName("title");
                        titleDatasetField.setDatasetFieldType(titleDatasetFieldType);
                        List<DatasetFieldValue> datasetFieldValues = new ArrayList<>();
                        DatasetFieldValue titleDatasetFieldValue = new DatasetFieldValue(titleDatasetField, dublinCore.get("title").get(0));
                        datasetFieldValues.add(titleDatasetFieldValue);
                        titleDatasetField.setDatasetFieldValues(datasetFieldValues);
                        datasetFields.add(titleDatasetField);

                        newDatasetVersion.setDatasetFields(datasetFields);

                        try {
                            // there is no importStudy method in 4.0 :(
                            // study = studyService.importStudy(tmpFile, dcmiTermsFormatId, dvThatWillOwnStudy.getId(), vdcUser.getId());
                            engineSvc.submit(new CreateDatasetCommand(dataset, dataverseUser));
                        } catch (Exception ex) {
//                            StringWriter stringWriter = new StringWriter();
//                            ex.printStackTrace(new PrintWriter(stringWriter));
//                            String stackTrace = stringWriter.toString();
                            /**
                             * @todo in DVN 3.x we printed the whole stack trace
                             * here. Is that really necessary?
                             */
//                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't import study: " + stackTrace);

                            Throwable cause = ex;
                            StringBuilder sb = new StringBuilder();
                            sb.append(ex.getLocalizedMessage());
                            while (cause.getCause() != null) {
                                cause = cause.getCause();
                                if (cause instanceof ConstraintViolationException) {
                                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                                        sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                                                .append(violation.getPropertyPath()).append(" at ")
                                                .append(violation.getLeafBean()).append(" - ")
                                                .append(violation.getMessage());
                                    }
                                }
                            }
                            logger.info(sb.toString());
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't create dataset: " + sb.toString());
                        } finally {
                            tmpFile.delete();
                            uploadDir.delete();
                        }
                        ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                        String baseUrl = urlManager.getHostnamePlusBaseUrlPath(collectionUri);
                        DepositReceipt depositReceipt = receiptGenerator.createReceipt(baseUrl, dataset);
                        return depositReceipt;
                    } else if (deposit.isBinaryOnly()) {
                        // get here with this:
                        // curl --insecure -s --data-binary "@example.zip" -H "Content-Disposition: filename=example.zip" -H "Content-Type: application/zip" https://sword:sword@localhost:8181/dvn/api/data-deposit/v1/swordv2/collection/dataverse/sword/
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Binary deposit to the collection IRI via POST is not supported. Please POST an Atom entry instead.");
                    } else if (deposit.isMultipart()) {
                        // get here with this:
                        // wget https://raw.github.com/swordapp/Simple-Sword-Server/master/tests/resources/multipart.dat
                        // curl --insecure --data-binary "@multipart.dat" -H 'Content-Type: multipart/related; boundary="===============0670350989=="' -H "MIME-Version: 1.0" https://sword:sword@localhost:8181/dvn/api/data-deposit/v1/swordv2/collection/dataverse/sword/hdl:1902.1/12345
                        // but...
                        // "Yeah, multipart is critically broken across all implementations" -- http://www.mail-archive.com/sword-app-tech@lists.sourceforge.net/msg00327.html
                        throw new UnsupportedOperationException("Not yet implemented");
                    } else {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "expected deposit types are isEntryOnly, isBinaryOnly, and isMultiPart");
                    }
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "user " + dataverseUser.getUserName() + " is not authorized to modify dataset");
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not find dataverse: " + dvAlias);
            }
        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Could not determine target type or identifier from URL: " + collectionUri);
        }
    }

}
