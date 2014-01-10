package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissionsMap;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

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
	
	private CommandContext ctxt;
	
	
	public <R> R submit(Command<R> aCommand) throws CommandException {
		
		if ( false ) {
			// Currently not in use
			// Check permissions - or throw an exception
			Map<String,? extends Set<Permission>> requiredMap = permissionsRequired(aCommand);
			if ( requiredMap == null ) {
				throw new RuntimeException("Command class " + aCommand.getClass() + " does not define required permissions. "
											+ "Please use the RequiredPermissions annotation.");
			}

			DataverseUser user = aCommand.getUser();

			for ( Map.Entry<String, Dataverse> pair : aCommand.getAffectedDataverses().entrySet() ) {
				Set<Permission> granted = DataverseRole.permissionSet( roleService.effectiveRoles(user, pair.getValue()) );
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
	
	/**
	 * Given a command, returns the set of permissions needed to be able to execute it.
	 * Needed permissions are specified by annotating the command's class with
	 * the {@link RequiredPermissions} annotation.
	 * 
	 * @param c The command
	 * @return Set of permissions, or {@code null} if the command's class was not annotated.
	 */
	private Map<String,? extends Set<Permission>> permissionsRequired( Command c ) {
		RequiredPermissions requiredPerms = c.getClass().getAnnotation(RequiredPermissions.class);
		if ( requiredPerms == null ) {
			// try for the permission map
			RequiredPermissionsMap reqPermMap = c.getClass().getAnnotation( RequiredPermissionsMap.class );
			Map<String, Set<Permission>> retVal = new TreeMap<>();
			for ( RequiredPermissions rp : reqPermMap.value() ) {
				retVal.put( rp.dataverseName(), asPermissionSet(rp.value()) );
			}
			return retVal;
			
		} else {
			Permission[] required = requiredPerms.value();
			return Collections.singletonMap(requiredPerms.dataverseName(),
											asPermissionSet(required) );
		}
	}
	
	private Set<Permission> asPermissionSet( Permission[] permissionArray ) {
		return (permissionArray.length==0) ? EnumSet.noneOf(Permission.class)
									   : (permissionArray.length==1) ? EnumSet.of(permissionArray[0]) 
															: EnumSet.of(permissionArray[0], permissionArray);
	}
	
	private CommandContext getContext() {
		if ( ctxt == null ) { 
			ctxt = new CommandContext() {
				@Override
				public DatasetServiceBean datasets() { return datasetService; }
				
				@Override
				public DataverseServiceBean dataverses() { return dataverseService; }
			};
		}
		
		return ctxt;
	}
	
}
