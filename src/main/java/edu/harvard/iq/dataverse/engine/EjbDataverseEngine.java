package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.EnumSet;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * An EJB capable of executing {@link Command}s in a JEE environment.
 * 
 * @author michael
 */
@Stateless
@Named("DataverseEngine")
public class EjbDataverseEngine implements DataverseEngine {
	
	@EJB
	DataverseSession sessionService;
	
	@EJB
	DatasetServiceBean datasetService;
	
	private CommandContext ctxt;
	
	@Override
	public <R> R submit(Command<R> aCommand) throws CommandException {
		// Check permissions - or throw an exception
		Set<Permission> required = permissionsRequired(aCommand);
		if ( required == null ) {
			throw new RuntimeException("Command class " + aCommand.getClass() + " does not define required permissions. "
										+ "Please use the RequiredPermissions annotation.");
		}
		
		Dataverse affected = aCommand.getAffectedDataverse();
		CHECK PERMISSIONS
				
		
		// Execute
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
	private Set<Permission> permissionsRequired( Command c ) {
		RequiredPermissions requiredPerms = c.getClass().getAnnotation(RequiredPermissions.class);
		if ( requiredPerms == null ) return null;
		
		Permission[] needed = requiredPerms.value();
		if ( needed.length == 0 ) return EnumSet.noneOf( Permission.class );
		return (needed.length==1) ? EnumSet.of(needed[0]) : EnumSet.of(needed[0], needed);
	}
	
	private CommandContext getContext() {
		if ( ctxt == null ) {
			ctxt = new CommandContext() {
				@Override
				public DatasetServiceBean datasets() { return datasetService; }
			};
		}
		
		return ctxt;
	}
	
}
