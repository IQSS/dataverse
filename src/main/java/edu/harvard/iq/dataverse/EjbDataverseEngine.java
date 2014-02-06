package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;

/**
 * An EJB capable of executing {@link Command}s in a JEE environment.
 * 
 * @author michael
 */
@Stateless
@Named
public class EjbDataverseEngine {
	
	@EJB
	DatasetServiceBean datasetService;

	@EJB
	DataverseServiceBean dataverseService;
	
	@EJB
	DataverseRoleServiceBean roleService;
	
	@EJB
	DataverseRoleServiceBean rolesService;
	
	@EJB
	DataverseUserServiceBean usersService;
	
	@EJB
	IndexServiceBean indexService;
	
	@EJB
	SearchServiceBean searchService;
	
	@EJB
	PermissionServiceBean permissionService;
	
	private CommandContext ctxt;
	
	
	public <R> R submit(Command<R> aCommand) throws CommandException {
		
		if ( false ) {
			// Currently not in use
			// Check permissions - or throw an exception
			Map<String,? extends Set<Permission>> requiredMap = CH.permissionsRequired(aCommand);
			if ( requiredMap == null ) {
				throw new RuntimeException("Command class " + aCommand.getClass() + " does not define required permissions. "
											+ "Please use the RequiredPermissions annotation.");
			}

			DataverseUser user = aCommand.getUser();

			for ( Map.Entry<String, Dataverse> pair : aCommand.getAffectedDataverses().entrySet() ) {
				Set<Permission> granted = roleService.roleAssignments(user, pair.getValue()).getPermissions();
				Set<Permission> required = requiredMap.get( pair.getKey() );
				if ( ! granted.containsAll(required) ) {
					required.removeAll(granted);
					throw new PermissionException("Can't execute command " + aCommand 
						+ ", because user " + aCommand.getUser() 
						+ " is missing permissions " + required 
						+ " on Dataverse " + pair.getValue().getName(),
						aCommand,
						required,
						pair.getValue());
				}
			}
		}
		return aCommand.execute(getContext());
	}
	
	public CommandContext getContext() {
		if ( ctxt == null ) { 
			ctxt = new CommandContext() {
				@Override
				public DatasetServiceBean datasets() { return datasetService; }
				
				@Override
				public DataverseServiceBean dataverses() { return dataverseService; }

				@Override
				public DataverseRoleServiceBean roles() { return rolesService; }

				@Override
				public DataverseUserServiceBean users() { return usersService; }

				@Override
				public IndexServiceBean indexing() { return indexService; }

				@Override
				public SearchServiceBean search() { return searchService; }

				@Override
				public PermissionServiceBean permissions() { return permissionService; }
			};
		}
		
		return ctxt;
	}
	
}
