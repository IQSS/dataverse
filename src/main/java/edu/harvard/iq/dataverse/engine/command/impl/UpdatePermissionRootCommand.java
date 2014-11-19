package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Updates the permission root-ness of a DvObjectContainer.
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class UpdatePermissionRootCommand extends AbstractCommand<DvObjectContainer> {
    
	private final boolean newValue;
	private final DvObjectContainer dvoc;
    
	public UpdatePermissionRootCommand(boolean newValue, User aUser, DvObjectContainer anAffectedDvObjectContainer) {
		super(aUser, anAffectedDvObjectContainer);
		this.newValue = newValue;
		dvoc = anAffectedDvObjectContainer;
	}
	
	@Override
	public DvObjectContainer execute( final CommandContext ctxt) throws CommandException {
		if ( dvoc.isPermissionRoot() == newValue ) {
            return dvoc;

        } else {
			dvoc.setPermissionRoot(newValue);
			return dvoc.accept(new DvObject.Visitor<DvObjectContainer>() {

                    @Override
                    public DvObjectContainer visit(Dataverse dv) {
                        return ctxt.dataverses().save(dv);
                    }

                    @Override
                    public DvObjectContainer visit(Dataset ds) {
                        if ( ds.getId() == null ) {
                            ctxt.em().persist(ds);
                            return ds;
                        } else {
                            return ctxt.em().merge(ds);
                        }
                    }

                    @Override
                    public DvObjectContainer visit(DataFile df) {
                        throw new UnsupportedOperationException("DataFiles should never get here.");
                    }
                });
		}
	}

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset
        return Collections.singletonMap("",
                dvoc instanceof Dataverse ? Collections.singleton(Permission.ManageDataversePermissions)
                : Collections.singleton(Permission.ManageDatasetPermissions));
    }
        
}
