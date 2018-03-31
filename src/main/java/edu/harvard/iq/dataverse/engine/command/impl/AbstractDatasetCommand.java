package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.sql.Timestamp;
import java.util.Date;

/**
 *  
 * Base class for commands that deal with {@code Dataset}s. Mainly here as a code
 * re-use mechanism.
 * 
 * @author michael
 */
public abstract class AbstractDatasetCommand extends AbstractCommand<Dataset> {
    
    private final Dataset dataset;
    private final Timestamp timestamp = new Timestamp(new Date().getTime());
    
    public AbstractDatasetCommand(DataverseRequest aRequest, Dataset aDataset) {
        super(aRequest, aDataset);
        if ( aDataset == null ) {
            throw new IllegalArgumentException("aDataset cannot be null");
        }
        dataset = aDataset;
    }
    
    /**
     * Updates the {@link DatasetVersionUser} for our {@link #dataset}. After
     * calling this method, there is a {@link DatasetUser} object connecting
     * {@link #dataset} and the {@link AuthenticatedUser} who issued this command,
     * with the {@code lastUpdate} field containing {@link #timestamp}.
     * 
     * @param ctxt The command context in which this command runs.
     */
    protected void updateDatasetUser( CommandContext ctxt ) {
        DatasetVersionUser datasetDataverseUser = ctxt.datasets().getDatasetVersionUser(dataset.getLatestVersion(), getUser());
        if (datasetDataverseUser != null) {
            datasetDataverseUser.setLastUpdateDate(getTimestamp());
            ctxt.em().merge(datasetDataverseUser);
        
        } else {
            datasetDataverseUser = new DatasetVersionUser();
            datasetDataverseUser.setDatasetVersion(getDataset().getLatestVersion());
            datasetDataverseUser.setLastUpdateDate(new Timestamp(new Date().getTime()));
            String id = getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            datasetDataverseUser.setAuthenticatedUser(au);
            ctxt.em().merge(datasetDataverseUser);
        }
    }
    
    protected Dataset getDataset() {
        return dataset;
    }
    
    /**
     * The time the command instance was created. Note: This is not the time
     * the command was submitted to the engine. If the difference can be large
     * enough, consider using another timestamping mechanism. This is a 
     * convenience method fit for most cases.
     * 
     * @return the time {@code this} command was created.
     */
    protected Timestamp getTimestamp() {
        return timestamp;
    }
}
