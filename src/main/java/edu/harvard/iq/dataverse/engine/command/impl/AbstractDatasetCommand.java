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
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import javax.validation.ConstraintViolation;

/**
 *  
 * Base class for commands that deal with {@code Dataset}s.Mainly here as a code
 re-use mechanism.
 * 
 * @author michael
 * @param <T> The type of the command's result. Normally {@link Dataset}.
 */
public abstract class AbstractDatasetCommand<T> extends AbstractCommand<T> {
    private static final Logger logger = Logger.getLogger(AbstractDatasetCommand.class.getName());
    private Dataset dataset;
    private final Timestamp timestamp = new Timestamp(new Date().getTime());
    
    
    public AbstractDatasetCommand(DataverseRequest aRequest, Dataset aDataset, Dataverse parent) {
        super( aRequest, parent );
        if ( aDataset == null ) {
            throw new IllegalArgumentException("aDataset cannot be null");
        }
        dataset = aDataset;
    }
    
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
            createDatasetUser(ctxt);
        }
    }
    
    protected void createDatasetUser( CommandContext ctxt ) {
        DatasetVersionUser datasetDataverseUser = new DatasetVersionUser();
        datasetDataverseUser.setDatasetVersion(getDataset().getLatestVersion());
        datasetDataverseUser.setLastUpdateDate(getTimestamp());
        datasetDataverseUser.setAuthenticatedUser((AuthenticatedUser) getUser());
        ctxt.em().persist(datasetDataverseUser);
    }
    
    /**
     * Validates the fields of the {@link DatasetVersion} passed. Throws an informational
     * error if validation fails.
     * @param dsv The dataset version whose fields we validate
     * @param lenient when {@code true}, invalid fields are populated with N/A value.
     * @throws CommandException if and only if {@code lenient=false}, and field validation failed.
     */
    protected void validateOrDie( DatasetVersion dsv, Boolean lenient )  throws CommandException {
        Set<ConstraintViolation> constraintViolations = dsv.validate();
        if (!constraintViolations.isEmpty()) {
            if ( lenient ) {
                // populate invalid fields with N/A
                constraintViolations.stream()
                    .map( cv -> ((DatasetField)cv.getRootBean()) )
                    .forEach( f -> f.setSingleValue(DatasetField.NA_VALUE));
                 
            } else {
                // explode with a helpful message
                String validationMessage = constraintViolations.stream()
                    .map( cv->cv.getMessage() + " (Invalid value:" + cv.getInvalidValue() + ")")
                    .collect( joining(", ", "Validation Failed: ", "."));

                throw new IllegalCommandException(validationMessage, this);
            }
        }
    }
    
    /**
     * Removed empty fields, sets field value display order.
     * @param dsv the dataset version show fields we want to tidy up.
     */
    protected void tidyUpFields( DatasetVersion dsv ) {
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
    }
    
    protected Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
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
