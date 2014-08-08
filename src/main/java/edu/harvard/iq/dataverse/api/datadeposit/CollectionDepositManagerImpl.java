package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.ForeignMetadataFormatMapping;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
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
    @EJB
    DatasetServiceBean datasetService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;
    @EJB
    EjbDataverseEngine engineSvc;
    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @EJB
    ForeignMetadataImportServiceBean foreignMetadataImportService;

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
                            /**
                             * @todo make sure business rules such as required
                             * fields are enforced deeper in the system:
                             * https://github.com/IQSS/dataverse/issues/605
                             */
//                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "title field is required");
                        }

                        Dataset dataset = new Dataset();
                        dataset.setOwner(dvThatWillOwnDataset);

                        /**
                         * @todo check in on DatasetPage to see when it stops
                         * hard coding protocol and authority. For now, re-use
                         * the exact strings it uses. The ticket to get away
                         * from these hard-coded protocol and authority values:
                         * https://github.com/IQSS/dataverse/issues/757
                         */
                        dataset.setProtocol(DatasetPage.fixMeDontHardCodeProtocol);
                        dataset.setAuthority(DatasetPage.fixMeDontHardCodeAuthority);

                        /**
                         * @todo why is generateIdentifierSequence off by one?
                         * (10 vs. 9):
                         * https://github.com/IQSS/dataverse/issues/758
                         */
                        dataset.setIdentifier(datasetService.generateIdentifierSequence(DatasetPage.fixMeDontHardCodeProtocol, DatasetPage.fixMeDontHardCodeAuthority));

                        DatasetVersion newDatasetVersion = dataset.getEditVersion();

                        String foreignFormat = SwordUtil.DCTERMS;
                        try {
                            foreignMetadataImportService.importXML(deposit.getSwordEntry().toString(), foreignFormat, newDatasetVersion);
                        } catch (Exception ex) {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "problem calling importXML: " + ex);
                        }

                        List<String> requiredFields = new ArrayList<>();
                        final List<DatasetFieldType> requiredDatasetFieldTypes = datasetFieldService.findAllRequiredFields();
                        for (DatasetFieldType requiredField : requiredDatasetFieldTypes) {
                            requiredFields.add(requiredField.getName());
                        }
                        logger.info("required fields: " + requiredFields);

                        DatasetField emailDatasetField = new DatasetField();
                        DatasetFieldType emailDatasetFieldType = datasetFieldService.findByName(DatasetFieldConstant.datasetContact);
                        List<DatasetFieldValue> values = new ArrayList<>();
                        values.add(new DatasetFieldValue(emailDatasetField, dvThatWillOwnDataset.getContactEmail()));
                        emailDatasetField.setDatasetFieldValues(values);
                        emailDatasetField.setDatasetFieldType(emailDatasetFieldType);
                        List<DatasetField> fieldList = newDatasetVersion.getDatasetFields();
                        fieldList.add(emailDatasetField);

                        List<String> createdFields = new ArrayList<>();
                        final List<DatasetField> createdDatasetFields = newDatasetVersion.getFlatDatasetFields();
                        for (DatasetField createdField : createdDatasetFields) {
                            createdFields.add(createdField.getDatasetFieldType().getName());
                            logger.info(createdField.getDatasetFieldType().getName() + ":" + createdField.getValue());
                        }
                        logger.info("created fields: " + createdFields);

                        boolean doRequiredFieldCheck = true;
                        if (doRequiredFieldCheck) {
                            for (String requiredField : requiredFields) {
                                if (requiredField.equals("subject")) {
                                    /**
                                     * @todo the plan, for now anyway, is to
                                     * silently choose "Other" for the user
                                     */
                                    logger.info("WARNING: required field \"subject\" not populated!");
                                    break;
                                }
                                if (!createdFields.contains(requiredField)) {
                                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Can't create/update dataset. " + SwordUtil.DCTERMS + " equivalent of required field not found: " + requiredField);
                                }
                            }
                        }

                        Dataset createdDataset = null;
                        try {
                            createdDataset = engineSvc.submit(new CreateDatasetCommand(dataset, dataverseUser));
                        } catch (EJBException | CommandException ex) {
                            Throwable cause = ex;
                            StringBuilder sb = new StringBuilder();
                            sb.append(ex.getLocalizedMessage());
                            while (cause.getCause() != null) {
                                cause = cause.getCause();
                                /**
                                 * @todo move this ConstraintViolationException
                                 * check to CreateDatasetCommand. Can be
                                 * triggered if you don't call
                                 * dataset.setIdentifier() or if you feed it
                                 * date format we don't like. Once this is done
                                 * we should be able to drop EJBException from
                                 * the catch above and only catch
                                 * CommandException
                                 */
                                if (cause instanceof ConstraintViolationException) {
                                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                                        sb.append(" Invalid value: '").append(violation.getInvalidValue()).append("' for ")
                                                .append(violation.getPropertyPath()).append(" at ")
                                                .append(violation.getLeafBean()).append(" - ")
                                                .append(violation.getMessage());
                                    }
                                }
                            }
                            logger.info(sb.toString());
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Couldn't create dataset: " + sb.toString());
                        }
                        if (createdDataset != null) {
                            ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                            String baseUrl = urlManager.getHostnamePlusBaseUrlPath(collectionUri);
                            DepositReceipt depositReceipt = receiptGenerator.createReceipt(baseUrl, createdDataset);
                            return depositReceipt;
                        } else {
                            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Problem creating dataset. Null returned.");
                        }
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
