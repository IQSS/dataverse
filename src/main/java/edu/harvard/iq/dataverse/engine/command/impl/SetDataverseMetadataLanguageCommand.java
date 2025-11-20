package edu.harvard.iq.dataverse.engine.command.impl;

import java.util.Collections;
import java.util.Map;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

@RequiredPermissions(Permission.EditDataverse)
public class SetDataverseMetadataLanguageCommand extends AbstractCommand<Map<String,String>> {

    private Dataverse dv;
    private String lang;

    public SetDataverseMetadataLanguageCommand(DataverseRequest aRequest, Dataverse dv, String lang) {
        super(aRequest, dv);
        this.dv = dv;
        this.lang = lang;
    }

    @Override
    public Map<String, String> execute(CommandContext ctxt) throws CommandException {
        dv.setMetadataLanguage(lang);
        return Collections.singletonMap(lang, ctxt.settings().getBaseMetadataLanguageMap(null, true).get(lang));
    }
    
}
