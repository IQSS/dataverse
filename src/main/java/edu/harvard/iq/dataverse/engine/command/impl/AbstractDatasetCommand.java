package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import javax.validation.ConstraintViolation;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.pidproviders.FakePidProviderServiceBean;

/**
 *
 * Base class for commands that deal with {@code Dataset}s.Mainly here as a code
 * re-use mechanism.
 *
 * @author michael
 * @param <T> The type of the command's result. Normally {@link Dataset}.
 */
public abstract class AbstractDatasetCommand<T> extends AbstractCommand<T> {

    private static final Logger logger = Logger.getLogger(AbstractDatasetCommand.class.getName());
    private static final int FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT = 2 ^ 8;
    private Dataset dataset;
    private final Timestamp timestamp = new Timestamp(new Date().getTime());

    public AbstractDatasetCommand(DataverseRequest aRequest, Dataset aDataset, Dataverse parent) {
        super(aRequest, parent);
        if (aDataset == null) {
            throw new IllegalArgumentException("aDataset cannot be null");
        }
        dataset = aDataset;
    }

    public AbstractDatasetCommand(DataverseRequest aRequest, Dataset aDataset) {
        super(aRequest, aDataset);
        if (aDataset == null) {
            throw new IllegalArgumentException("aDataset cannot be null");
        }
        dataset = aDataset;
    }

    /**
     * Creates/updates the {@link DatasetVersionUser} for our {@link #dataset}. After
     * calling this method, there is a {@link DatasetUser} object connecting
     * {@link #dataset} and the {@link AuthenticatedUser} who issued this
     * command, with the {@code lastUpdate} field containing {@link #timestamp}.
     *
     * @param ctxt The command context in which this command runs.
     */
    protected void updateDatasetUser(CommandContext ctxt) {
        DatasetVersionUser datasetDataverseUser = ctxt.datasets().getDatasetVersionUser(getDataset().getLatestVersion(), getUser());

        if (datasetDataverseUser != null) {
            // Update existing dataset-user
            datasetDataverseUser.setLastUpdateDate(getTimestamp());
            ctxt.em().merge(datasetDataverseUser);

        } else {
            // create a new dataset-user
            createDatasetUser(ctxt);
        }
    }
    
    protected void createDatasetUser(CommandContext ctxt) {
        DatasetVersionUser datasetDataverseUser = new DatasetVersionUser();
        datasetDataverseUser.setDatasetVersion(getDataset().getLatestVersion());
        datasetDataverseUser.setLastUpdateDate(getTimestamp());
        datasetDataverseUser.setAuthenticatedUser((AuthenticatedUser) getUser());
        ctxt.em().persist(datasetDataverseUser);
    }
    
    /**
     * Validates the fields of the {@link DatasetVersion} passed. Throws an
     * informational error if validation fails.
     *
     * @param dsv The dataset version whose fields we validate
     * @param lenient when {@code true}, invalid fields are populated with N/A
     * value.
     * @throws CommandException if and only if {@code lenient=false}, and field
     * validation failed.
     */
    protected void validateOrDie(DatasetVersion dsv, Boolean lenient) throws CommandException {
        Set<ConstraintViolation> constraintViolations = dsv.validate();
        if (!constraintViolations.isEmpty()) {
            if (lenient) {
                // populate invalid fields with N/A
                constraintViolations.stream()
                    .map(cv -> ((DatasetField) cv.getRootBean()))
                    .forEach(f -> f.setSingleValue(DatasetField.NA_VALUE));

            } else {
                // explode with a helpful message
                String validationMessage = constraintViolations.stream()
                    .map(cv -> cv.getMessage() + " (Invalid value:" + cv.getInvalidValue() + ")")
                    .collect(joining(", ", "Validation Failed: ", "."));

                throw new IllegalCommandException(validationMessage, this);
            }
        }
    }

    /**
     * Removed empty fields, sets field value display order.
     *
     * @param dsv the dataset version show fields we want to tidy up.
     */
    protected void tidyUpFields(DatasetVersion dsv) {
        Iterator<DatasetField> dsfIt = dsv.getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (dsfIt.next().removeBlankDatasetFieldValues()) {
                dsfIt.remove();
            }
        }
        Iterator<DatasetField> dsfItSort = dsv.getDatasetFields().iterator();
        while (dsfItSort.hasNext()) {
            dsfItSort.next().setValueDisplayOrder();
        }
        Iterator<DatasetField> dsfItTrim = dsv.getDatasetFields().iterator();
        while (dsfItTrim.hasNext()) {
            dsfItTrim.next().trimTrailingSpaces();
        }
    }

    /**
     * Whether it's EZID or DataCite, if the registration is refused because the
     * identifier already exists, we'll generate another one and try to register
     * again... but only up to some reasonably high number of times - so that we
     * don't go into an infinite loop here, if EZID is giving us these duplicate
     * messages in error.
     *
     * (and we do want the limit to be a "reasonably high" number! true, if our
     * identifiers are randomly generated strings, then it is highly unlikely
     * that we'll ever run into a duplicate race condition repeatedly; but if
     * they are sequential numeric values, than it is entirely possible that a
     * large enough number of values will be legitimately registered by another
     * entity sharing the same authority...)
     *
     * @param theDataset
     * @param ctxt
     * @throws CommandException
     */
    protected void registerExternalIdentifier(Dataset theDataset, CommandContext ctxt) throws CommandException {
        if (!theDataset.isIdentifierRegistered()) {
            GlobalIdServiceBean globalIdServiceBean = GlobalIdServiceBean.getBean(theDataset.getProtocol(), ctxt);
            if ( globalIdServiceBean != null ) {
                if (globalIdServiceBean instanceof FakePidProviderServiceBean) {
                    try {
                        globalIdServiceBean.createIdentifier(theDataset);
                    } catch (Throwable ex) {
                        logger.warning("Problem running createIdentifier for FakePidProvider: " + ex);
                    }
                    theDataset.setGlobalIdCreateTime(getTimestamp());
                    theDataset.setIdentifierRegistered(true);
                    return;
                }
                try {
                    if (globalIdServiceBean.alreadyExists(theDataset)) {
                        int attempts = 0;

                        while (globalIdServiceBean.alreadyExists(theDataset) && attempts < FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {
                            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, globalIdServiceBean));
                            logger.log(Level.INFO, "Attempting to register external identifier for dataset {0} (trying: {1}).",
                                new Object[]{theDataset.getId(), theDataset.getIdentifier()});
                            attempts++;
                        }

                        if (globalIdServiceBean.alreadyExists(theDataset)) {
                            throw new CommandExecutionException("This dataset may not be published because its identifier is already in use by another dataset; "
                                + "gave up after " + attempts + " attempts. Current (last requested) identifier: " + theDataset.getIdentifier(), this);
                        }
                    }
                    // Invariant: Dataset identifier does not exist in the remote registry
                    globalIdServiceBean.createIdentifier(theDataset);
                    theDataset.setGlobalIdCreateTime(getTimestamp());
                    theDataset.setIdentifierRegistered(true);

                } catch (Throwable e) {
                    throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", globalIdServiceBean.getProviderInformation()), this);
                }
            } else {
                throw new IllegalCommandException("This dataset may not be published because its id registry service is not supported.", this);
            }

        }
    }

    protected Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    /**
     * The time the command instance was created. Note: This is not the time the
     * command was submitted to the engine. If the difference can be large
     * enough, consider using another timestamping mechanism. This is a
     * convenience method fit for most cases.
     *
     * @return the time {@code this} command was created.
     */
    protected Timestamp getTimestamp() {
        return timestamp;
    }
}
