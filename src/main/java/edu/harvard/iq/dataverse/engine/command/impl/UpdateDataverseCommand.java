package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.search.IndexResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import javax.persistence.TypedQuery;

/**
 * Update an existing dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.EditDataverse )
public class UpdateDataverseCommand extends AbstractCommand<Dataverse> {
	
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
        Dataverse result = (Dataverse) r;
        Future<String> indResponse = ctxt.index().indexDataverse(result);
        
        List<Dataset> datasets = ctxt.datasets().findByOwnerId(result.getId());
        ctxt.index().asyncIndexDatasetList(datasets, true);

        return true;
    }
}
