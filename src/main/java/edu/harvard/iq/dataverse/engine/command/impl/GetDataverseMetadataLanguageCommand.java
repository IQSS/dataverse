package edu.harvard.iq.dataverse.engine.command.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

public class GetDataverseMetadataLanguageCommand extends AbstractCommand<Map<String, String>> {

    private final Dataverse dv;
    
    public GetDataverseMetadataLanguageCommand(DataverseRequest aRequest, Dataverse dv) {
        super(aRequest, dv);
        this.dv = dv;
    }

    @Override
    public Map<String, String> execute(CommandContext ctxt) throws CommandException {
        Map<String, String> langMap = ctxt.settings().getBaseMetadataLanguageMap(null, true);
        String dvMetadataLanguage = dv.getMetadataLanguage();
        if (!dvMetadataLanguage.equals(DvObjectContainer.UNDEFINED_CODE)) {
            return Collections.singletonMap(dvMetadataLanguage, langMap.get(dvMetadataLanguage));
        }
        return langMap;

    }
    
    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dv.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }  
}
