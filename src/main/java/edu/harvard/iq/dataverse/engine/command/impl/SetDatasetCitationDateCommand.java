package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

/**
 *
 * @author gdurand
 */
@RequiredPermissions( Permission.PublishDataset )
public class SetDatasetCitationDateCommand extends AbstractCommand<Dataset>{
    

	private final DatasetFieldType dsfType;
	private final Dataset dataset;
	
	public SetDatasetCitationDateCommand( DataverseRequest aRequest, Dataset dataset, DatasetFieldType dsfType ) {
		super( aRequest, dataset );
		this.dataset = dataset;
		this.dsfType = dsfType;
	}
	
	@Override
	public Dataset execute(CommandContext ctxt) throws CommandException {
            if ( dsfType == null || dsfType.getFieldType().equals(FieldType.DATE) ) {
                dataset.setCitationDateDatasetFieldType(dsfType);           
            } else {
                throw new IllegalCommandException("Provided DatasetFieldtype is not a Date", this);
            }
            
            Dataset savedDataset = ctxt.datasets().merge(dataset);
            ctxt.index().indexDataset(savedDataset, false); 
            return savedDataset;
	}
	
}

