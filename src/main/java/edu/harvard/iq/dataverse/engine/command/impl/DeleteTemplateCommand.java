/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.List;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions( Permission.EditDataverse )
public class DeleteTemplateCommand extends AbstractCommand<Dataverse> {

    private final Template doomed;
    private final Dataverse editedDv;
    private final List<Dataverse> dvWDefaultTemplate;
    
    public DeleteTemplateCommand(DataverseRequest aRequest, Dataverse editedDv , Template doomed, List<Dataverse> dvWDefaultTemplate) {  
        super(aRequest, editedDv);
        this.editedDv = editedDv;
        this.doomed = doomed;
        this.dvWDefaultTemplate = dvWDefaultTemplate;
    }

    @Override
    public Dataverse execute(CommandContext ctxt) throws CommandException {
        Dataverse merged = ctxt.em().merge(editedDv);
        if (!dvWDefaultTemplate.isEmpty()){
            for (Dataverse remove: dvWDefaultTemplate){
                remove.setDefaultTemplate(null);
                ctxt.em().merge(remove);
            }                
        }
        Template doomedAndMerged = ctxt.em().merge(doomed);
        ctxt.em().remove(doomedAndMerged);
        return merged;
    }
    
}
