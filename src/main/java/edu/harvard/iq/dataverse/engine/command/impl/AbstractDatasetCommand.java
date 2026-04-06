package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionDifference;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.fake.FakeDOIProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;

import jakarta.ejb.EJB;
import jakarta.validation.ConstraintViolation;
import edu.harvard.iq.dataverse.settings.JvmSettings;

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
                    .filter(cv -> cv.getRootBean() instanceof DatasetField)
                    .map(cv -> ((DatasetField) cv.getRootBean()))
                    .forEach(f -> f.setSingleValue(DatasetField.NA_VALUE));

            } else {
                // explode with a helpful message
                String validationMessage = constraintViolations.stream()
                    .map(cv -> cv.getMessage() + " (Invalid value:" + cv.getInvalidValue() + ")")
                    .collect(joining(", ", "Validation Failed: ", "."));
                
                validationMessage  += constraintViolations.stream()
                    .filter(cv -> cv.getRootBean() instanceof TermsOfUseAndAccess)
                    .map(cv -> cv.toString());
                
                for (ConstraintViolation cv : constraintViolations){
                    if (cv.getRootBean() instanceof TermsOfUseAndAccess){
                        throw new IllegalCommandException(validationMessage,  this);
                    }
                }

                throw new IllegalCommandException(validationMessage, this);
            }
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
     * @param dvObject
     * @param ctxt
     * @throws CommandException
     */
    protected void registerExternalIdentifier(DvObject dvObject, CommandContext ctxt, boolean retry) throws CommandException {
        if (!dvObject.isIdentifierRegistered()) {
            PidProvider pidProvider = PidUtil.getPidProvider(dvObject.getGlobalId().getProviderId());
            if ( pidProvider != null ) {
                try {
                    if (pidProvider.alreadyRegistered(dvObject)) {
                        int attempts = 0;
                        if(retry) {
                            do  {
                                pidProvider.generatePid(dvObject);
                                logger.log(Level.INFO, "Attempting to register external identifier for dataset {0} (trying: {1}).",
                                    new Object[]{dvObject.getId(), dvObject.getIdentifier()});
                                attempts++;
                            } while (pidProvider.alreadyRegistered(dvObject) && attempts <= FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT);
                        }
                        if(!retry) {
                            logger.warning("Reserving PID for: "  + getDataset().getId() + " failed.");
                            throw new CommandExecutionException(BundleUtil.getStringFromBundle("abstractDatasetCommand.pidNotReserved", Arrays.asList(dvObject.getIdentifier())), this);
                        }
                        if(attempts > FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {
                            //Didn't work - we existed the loop with too many tries
                            throw new CommandExecutionException(BundleUtil.getStringFromBundle("abstractDatasetCommand.pidReservationRetryExceeded", Arrays.asList(Integer.toString(attempts), dvObject.getIdentifier())), this);
                        }
                    }
                    // Invariant: Dataset identifier does not exist in the remote registry
                    try {
                        pidProvider.createIdentifier(dvObject);
                        dvObject.setGlobalIdCreateTime(getTimestamp());
                        dvObject.setIdentifierRegistered(true);
                    } catch (Throwable ex) {
                        logger.info("Call to globalIdServiceBean.createIdentifier failed: " + ex);
                    }

                } catch (Throwable e) {
                    if (e instanceof CommandException) {
                        throw (CommandException) e;
                    }
                    throw new CommandException(BundleUtil.getStringFromBundle("dataset.publish.error", pidProvider.getProviderInformation()), this);
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

    protected void registerFilePidsIfNeeded(Dataset theDataset, CommandContext ctxt, boolean b) throws CommandException {
        // Register file PIDs if needed
        PidProvider pidGenerator = ctxt.dvObjects().getEffectivePidGenerator(getDataset());
        boolean shouldRegister = !pidGenerator.registerWhenPublished() &&
                ctxt.systemConfig().isFilePIDsEnabledForCollection(getDataset().getOwner()) &&
                pidGenerator.canCreatePidsLike(getDataset().getGlobalId());
        if (shouldRegister) {
            for (DataFile dataFile : theDataset.getFiles()) {
                logger.fine(dataFile.getId() + " is registered?: " + dataFile.isIdentifierRegistered());
                if (!dataFile.isIdentifierRegistered()) {
                    // pre-register a persistent id
                    registerFileExternalIdentifier(dataFile, pidGenerator, ctxt, true);
                }
            }
        }
    }

    private void registerFileExternalIdentifier(DataFile dataFile, PidProvider pidProvider, CommandContext ctxt, boolean retry) throws CommandException {

        if (!dataFile.isIdentifierRegistered()) {

            if (pidProvider instanceof FakeDOIProvider) {
                retry = false; // No reason to allow a retry with the FakeProvider (even if it allows
                               // pre-registration someday), so set false for efficiency
            }
            try {
                if (pidProvider.alreadyRegistered(dataFile)) {
                    int attempts = 0;
                    if (retry) {
                        do {
                            pidProvider.generatePid(dataFile);
                            logger.log(Level.INFO, "Attempting to register external identifier for datafile {0} (trying: {1}).",
                                    new Object[] { dataFile.getId(), dataFile.getIdentifier() });
                            attempts++;
                        } while (pidProvider.alreadyRegistered(dataFile) && attempts <= FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT);
                    }
                    if (!retry) {
                        logger.warning("Reserving File PID for: " + getDataset().getId() + ", fileId: " + dataFile.getId() + ", during publication failed.");
                        throw new CommandExecutionException(BundleUtil.getStringFromBundle("abstractDatasetCommand.filePidNotReserved", Arrays.asList(getDataset().getIdentifier())), this);
                    }
                    if (attempts > FOOLPROOF_RETRIAL_ATTEMPTS_LIMIT) {
                        // Didn't work - we existed the loop with too many tries
                        throw new CommandExecutionException("This dataset may not be published because its identifier is already in use by another dataset; "
                                + "gave up after " + attempts + " attempts. Current (last requested) identifier: " + dataFile.getIdentifier(), this);
                    }
                }
                // Invariant: DataFile identifier does not exist in the remote registry
                try {
                    pidProvider.createIdentifier(dataFile);
                    dataFile.setGlobalIdCreateTime(getTimestamp());
                    dataFile.setIdentifierRegistered(true);
                } catch (Throwable ex) {
                    logger.info("Call to globalIdServiceBean.createIdentifier failed: " + ex);
                }

            } catch (Throwable e) {
                if (e instanceof CommandException) {
                    throw (CommandException) e;
                }
                throw new CommandException(BundleUtil.getStringFromBundle("file.register.error", pidProvider.getProviderInformation()), this);
            }
        } else {
            throw new IllegalCommandException("This datafile may not have a PID because its id registry service is not supported.", this);
        }

    }

    protected void checkSystemMetadataKeyIfNeeded(DatasetVersion newVersion, DatasetVersion persistedVersion) throws IllegalCommandException {
        Set<MetadataBlock> changedMDBs = DatasetVersionDifference.getBlocksWithChanges(newVersion, persistedVersion);
        for (MetadataBlock mdb : changedMDBs) {
            logger.fine(mdb.getName() + " has been changed");
            String smdbString = JvmSettings.MDB_SYSTEM_KEY_FOR.lookupOptional(mdb.getName())
                    .orElse(null);
            if (smdbString != null) {
                logger.fine("Found key: " + smdbString);
                String mdKey = getRequest().getSystemMetadataBlockKeyFor(mdb.getName());
                logger.fine("Found supplied key: " + mdKey);
                if (mdKey == null || !mdKey.equalsIgnoreCase(smdbString)) {
                    throw new IllegalCommandException("Updating system metadata in block " + mdb.getName() + " requires a valid key", this);
                }
            }
        }
    }

    protected void registerExternalVocabValuesIfAny(CommandContext ctxt, DatasetVersion newVersion) {
        for (DatasetField df : newVersion.getFlatDatasetFields()) {
            logger.fine("Found id: " + df.getDatasetFieldType().getId());
            if (ctxt.dsField().getCVocConf(true).containsKey(df.getDatasetFieldType().getId())) {
                ctxt.dsField().registerExternalVocabValues(df);
            }
        }
    }
}
