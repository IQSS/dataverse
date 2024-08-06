
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFeaturedDataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author stephenkraffmiller
 */
public class ListFeaturedCollectionsCommand extends AbstractCommand<List<Dataverse>> {
    
    private final Dataverse dv;

    public ListFeaturedCollectionsCommand(DataverseRequest aRequest, Dataverse aDataverse) {
        super(aRequest, aDataverse);
        dv = aDataverse;
    }

    @Override
    public List<Dataverse> execute(CommandContext ctxt) throws CommandException {
        List<Dataverse> featuredTarget = new ArrayList<>();
        List<DataverseFeaturedDataverse> featuredList = ctxt.featuredDataverses().findByDataverseId(dv.getId());
            for (DataverseFeaturedDataverse dfd : featuredList) {
                Dataverse fd = dfd.getFeaturedDataverse();
                featuredTarget.add(fd);
            }
        return featuredTarget;
        
    }
    
    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dv.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
    
}
