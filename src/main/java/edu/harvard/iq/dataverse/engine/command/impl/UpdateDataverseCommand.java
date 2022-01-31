package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import static edu.harvard.iq.dataverse.dataverse.DataverseUtil.validateDataverseMetadataExternally;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.search.IndexResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.persistence.TypedQuery;
import org.apache.solr.client.solrj.SolrServerException;

/**
 * Update an existing dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.EditDataverse )
public class UpdateDataverseCommand extends AbstractCommand<Dataverse> {
        private static final Logger logger = Logger.getLogger(UpdateDataverseCommand.class.getName());
	
	private final Dataverse editedDv;
	private final List<DatasetFieldType> facetList;
        private final List<Dataverse> featuredDataverseList;
        private final List<DataverseFieldTypeInputLevel> inputLevelList;

	public UpdateDataverseCommand(Dataverse editedDv, List<DatasetFieldType> facetList, List<Dataverse> featuredDataverseList, 
                    DataverseRequest aRequest,  List<DataverseFieldTypeInputLevel> inputLevelList ) {
            super(aRequest, editedDv);
            this.editedDv = editedDv;
            // add update template uses this command but does not
            // update facet list or featured dataverses
            if (facetList != null){
               this.facetList = new ArrayList<>(facetList); 
            } else {
               this.facetList = null;
            }
            if (featuredDataverseList != null){
                this.featuredDataverseList = new ArrayList<>(featuredDataverseList);
            } else {
                this.featuredDataverseList = null;
            }
            if (inputLevelList != null){
               this.inputLevelList = new ArrayList<>(inputLevelList); 
            } else {
               this.inputLevelList = null;
            }
	}
	
	@Override
	public Dataverse execute(CommandContext ctxt) throws CommandException {
            logger.fine("Entering update dataverse command");
            
            // Perform any optional validation steps, if defined:
            if (ctxt.systemConfig().isExternalDataverseValidationEnabled()) {
                // For admins, an override of the external validation step may be enabled: 
                if (!(getUser().isSuperuser() && ctxt.systemConfig().isExternalValidationAdminOverrideEnabled())) {
                    String executable = ctxt.systemConfig().getDataverseValidationExecutable();
                    boolean result = validateDataverseMetadataExternally(editedDv, executable, getRequest());

                    if (!result) {
                        String rejectionMessage = ctxt.systemConfig().getDataverseUpdateValidationFailureMsg();
                        throw new IllegalCommandException(rejectionMessage, this);
                    }
                }
            }
            
            DataverseType oldDvType = ctxt.dataverses().find(editedDv.getId()).getDataverseType();
            String oldDvAlias = ctxt.dataverses().find(editedDv.getId()).getAlias();
            String oldDvName = ctxt.dataverses().find(editedDv.getId()).getName();
            Dataverse result = ctxt.dataverses().save(editedDv);
            
            if ( facetList != null ) {
                ctxt.facets().deleteFacetsFor(result);
                int i=0;
                for ( DatasetFieldType df : facetList ) {
                    ctxt.facets().create(i++, df.getId(), result.getId());
                }
            }
            if ( featuredDataverseList != null ) {
                ctxt.featuredDataverses().deleteFeaturedDataversesFor(result);
                int i=0;
                for ( Object obj : featuredDataverseList ) {
                    Dataverse dv = (Dataverse) obj;
                    ctxt.featuredDataverses().create(i++, dv.getId(), result.getId());
                }
            }
            if ( inputLevelList != null ) {
                ctxt.fieldTypeInputLevels().deleteFacetsFor(result);
                for ( DataverseFieldTypeInputLevel obj : inputLevelList ) {               
                    ctxt.fieldTypeInputLevels().create(obj);
                }
            }
            
            
            return result;
	}
        
    @Override
    public boolean onSuccess(CommandContext ctxt, Object r) {
        
        // first kick of async index of datasets
        // TODO: is this actually needed? Is there a better way to handle
        try {
            Dataverse result = (Dataverse) r;
            List<Dataset> datasets = ctxt.datasets().findByOwnerId(result.getId());
            ctxt.index().asyncIndexDatasetList(datasets, true);
        } catch (IOException | SolrServerException e) {
            // these datasets are being indexed asynchrounously, so not sure how to handle errors here
        }
        
        return ctxt.dataverses().index((Dataverse) r);
    }  

}

