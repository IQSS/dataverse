package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.List;

/**
 * Update an existing dataverse.
 * @author michael
 */
@RequiredPermissions( Permission.EditMetadata )
public class UpdateDataverseCommand extends AbstractCommand<Dataverse> {
	
	private final Dataverse editedDv;
	private final List<DatasetFieldType> facetList;
        private final List<Dataverse> featuredDataverseList;

	public UpdateDataverseCommand(Dataverse editedDv, List<DatasetFieldType> facetList, List<Dataverse> featuredDataverseList, DataverseUser aUser) {
		super(aUser, editedDv);
		this.editedDv = editedDv;
		this.facetList = new ArrayList<>(facetList);
                this.featuredDataverseList = new ArrayList<>(featuredDataverseList);
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
            System.out.print("List not null");
            ctxt.featuredDataverses().deleteFeaturedDataversesFor(result);
            int i=0;
            for ( Object obj : featuredDataverseList ) {
                System.out.print("in loop");
                System.out.print(obj);
                System.out.print("string " + obj.toString());
                System.out.print("class " + obj.getClass());
                Dataverse dv = (Dataverse) obj;
                ctxt.featuredDataverses().create(i++, dv.getId(), result.getId());
            }
        }
		ctxt.index().indexDataverse(result);
		
        return result;
	}
	
}
