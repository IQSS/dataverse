package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.List;

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
		ctxt.index().indexDataverse(result);
		
        return result;
	}
	
}
