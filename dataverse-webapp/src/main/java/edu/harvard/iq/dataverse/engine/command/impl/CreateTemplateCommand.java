
package edu.harvard.iq.dataverse.engine.command.impl;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
/**
 *
 * @author skraffmiller
 */
@RequiredPermissions( Permission.EditDataverse )
public class CreateTemplateCommand extends AbstractCommand<Template> {
    	private final Template created;
	private final Dataverse dv;
	
	public CreateTemplateCommand(Template template, DataverseRequest aRequest, Dataverse anAffectedDataverse) {
		super(aRequest, anAffectedDataverse);
		created = template;
		dv = anAffectedDataverse;
	}

	@Override
	public Template execute(CommandContext ctxt) throws CommandException {
                
		return ctxt.templates().save(created);
	}
    
}
