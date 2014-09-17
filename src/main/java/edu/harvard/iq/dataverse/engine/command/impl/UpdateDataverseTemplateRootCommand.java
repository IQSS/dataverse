

package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions( Permission.EditMetadata )
public class UpdateDataverseTemplateRootCommand extends AbstractCommand<Dataverse> {

    	private final boolean newValue;
        private  Dataverse dv;
        
    public UpdateDataverseTemplateRootCommand(boolean newValue, DataverseUser aUser, Dataverse anAffectedDataverse) {
        super(aUser, anAffectedDataverse);
        this.newValue = newValue;
        this.dv = anAffectedDataverse;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        	if ( dv.isTemplateRoot() != newValue ) {
			dv.setTemplateRoot(newValue);
			dv = ctxt.dataverses().save(dv);
		}
		return dv;
    }
    
}
