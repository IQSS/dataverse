
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author stephenkraffmiller
 */
public class GetTemplateCommand extends AbstractCommand<Template>  {
    
    private final Template template;
    
    public GetTemplateCommand (DataverseRequest aRequest, Template template){
        super(aRequest, template.getDataverse());
        this.template = template;
    }


    
    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        return template;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        //at least need add dataset if not published also - view unpublished
        Set<Permission> requiredPermissions = new HashSet<>();

        requiredPermissions.add(Permission.AddDataset);
        if (!template.getDataverse().isReleased()) {
            requiredPermissions.add(Permission.ViewUnpublishedDataverse);
        }

        return Collections.singletonMap("", requiredPermissions);

    }
    
    
}
