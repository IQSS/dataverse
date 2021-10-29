package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;

/**
 * ;
 * An abstract base class for commands that creates {@link Dataset}s.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
public abstract class AbstractCreateDatasetCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(AbstractCreateDatasetCommand.class.getCanonicalName());

    private final boolean registrationRequired;

    public AbstractCreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        this(theDataset, aRequest, false);
    }

    public AbstractCreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean isRegistrationRequired) {
        super(aRequest, theDataset);
        registrationRequired = isRegistrationRequired;
    }

    protected void additionalParameterTests(CommandContext ctxt)  {
        // base class - do nothing.
    }

    protected DatasetVersion getVersionToPersist(Dataset theDataset) {
        return theDataset.getLatestVersion();
    }

    /**
     * Called after the dataset has been persisted, but before the persistence context
     * has been flushed.
     *
     * @param theDataset The em-managed dataset.
     * @param ctxt
     * @throws edu.harvard.iq.dataverse.engine.command.exception.CommandException
     */
    protected void postPersist(Dataset theDataset, CommandContext ctxt)  {
        // base class - default to nothing.
    }

    protected abstract void handlePid(Dataset theDataset, CommandContext ctxt) ;

    @Override
    public Dataset execute(CommandContext ctxt)  {

        additionalParameterTests(ctxt);

        Dataset theDataset = getDataset();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(ctxt);
        if (isEmpty(theDataset.getIdentifier())) {
            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset));
        }

        DatasetVersion dsv = getVersionToPersist(theDataset);
        // This re-uses the state setup logic of CreateDatasetVersionCommand, but
        // without persisting the new version, or altering its files.
        new CreateDatasetVersionCommand(getRequest(), theDataset, dsv).prepareDatasetAndVersion(ctxt);

        theDataset.setCreator((AuthenticatedUser) getRequest().getUser());

        theDataset.setCreateDate(getTimestamp());

        theDataset.setModificationTime(getTimestamp());
        for (DataFile dataFile : theDataset.getFiles()) {
            dataFile.setCreator((AuthenticatedUser) getRequest().getUser());
            dataFile.setCreateDate(theDataset.getCreateDate());
        }

        if (theDataset.getProtocol() == null) {
            theDataset.setProtocol(ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol));
        }
        if (theDataset.getAuthority() == null) {
            theDataset.setAuthority(ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority));
        }
        if (theDataset.getStorageIdentifier() == null) {
            try {
                ctxt.dataAccess().createNewStorageIO(theDataset);
            } catch (IOException ioex) {
                throw new CommandExecutionException("Dataset storage could not be created", ioex, this);
            }
        }
        if (theDataset.getIdentifier() == null) {
            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset));
        }

        // Attempt the registration if importing dataset through the API, or the app (but not harvest)
        handlePid(theDataset, ctxt);

        if (registrationRequired && (theDataset.getGlobalIdCreateTime() == null)) {
            throw new CommandExecutionException("Dataset could not be created.  Registration failed", this);
        }

        ctxt.em().persist(theDataset);

        postPersist(theDataset, ctxt);

        createDatasetUser(ctxt);

        theDataset = ctxt.em().merge(theDataset); // store last updates

        // DB updates - done.

        // Now we need the acutal dataset id, so we can start indexing.
        ctxt.em().flush();

        // TODO: switch to asynchronous version when JPA sync works
        // ctxt.index().asyncIndexDataset(theDataset.getId(), true);
        ctxt.index().indexDataset(theDataset, true);
        ctxt.solrIndex().indexPermissionsOnSelfAndChildren(theDataset.getId());

        /*
        if (DataCaptureModuleUtil.rsyncSupportEnabled(ctxt.settings().getValueForKey(SettingsServiceBean.Key.UploadMethods))) {
            logger.fine("Requesting rsync support.");
            try {
                ScriptRequestResponse scriptRequestResponse = ctxt.engine().submit(new RequestRsyncScriptCommand(getRequest(), theDataset));
                logger.log(Level.FINE, "script: {0}", scriptRequestResponse.getScript());
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "Problem getting rsync script: {0}", ex.getLocalizedMessage());
            }
            logger.fine("Done with rsync request.");
        }*/
        return theDataset;
    }

    @Override
    public int hashCode() {
        return 97 + Objects.hashCode(getDataset());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractCreateDatasetCommand)) {
            return false;
        }
        final AbstractCreateDatasetCommand other = (AbstractCreateDatasetCommand) obj;
        return Objects.equals(getDataset(), other.getDataset());
    }

    @Override
    public String toString() {
        return "[DatasetCreate dataset:" + getDataset().getId() + "]";
    }

}
