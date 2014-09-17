package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
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
import java.util.EnumSet;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
	BuiltinUserServiceBean usersService;
	
	@EJB
	IndexServiceBean indexService;
	
	@EJB
	SearchServiceBean searchService;
	
	@EJB
	PermissionServiceBean permissionService;
	
	@EJB
	DvObjectServiceBean dvObjectService;
	
	@EJB
	DataverseFacetServiceBean dataverseFacetService; 

        @EJB
	FeaturedDataverseServiceBean featuredDataverseService; 
        
        @EJB
	DataFileServiceBean dataFileService; 
        
        @EJB
	TemplateServiceBean templateService; 

	@PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
	
	private CommandContext ctxt;
	
	public <R> R submit(Command<R> aCommand) throws CommandException {
		
		// Currently not in use
		// Check permissions - or throw an exception
		Map<String,? extends Set<Permission>> requiredMap = CH.permissionsRequired(aCommand);
		if ( requiredMap == null ) {
			throw new RuntimeException("Command class " + aCommand.getClass() + " does not define required permissions. "
										+ "Please use the RequiredPermissions annotation.");
		}

		User user = aCommand.getUser();
		
        Map<String, DvObject> affectedDataverses = aCommand.getAffectedDvObjects();
		
		for ( Map.Entry<String, ? extends Set<Permission>> pair : requiredMap.entrySet() ) {
			String dvName = pair.getKey();
			if ( ! affectedDataverses.containsKey(dvName) ) {
				throw new RuntimeException("Command instance " + aCommand + " does not have a DvObject named '" + dvName +"'" );
			}
			DvObject dvo = affectedDataverses.get(dvName);
			
			Set<Permission> granted = (dvo!=null) ? permissionService.permissionsFor(user, dvo)
												  : EnumSet.allOf(Permission.class);
			Set<Permission> required = requiredMap.get(dvName);
			if ( ! granted.containsAll(required) ) {
				required.removeAll(granted);
				throw new PermissionException("Can't execute command " + aCommand 
					+ ", because user " + aCommand.getUser() 
					+ " is missing permissions " + required 
					+ " on Object " + dvo.accept( DvObject.NamePrinter ),
					aCommand,
					required, dvo);
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
				public BuiltinUserServiceBean users() { return usersService; }

				@Override
				public IndexServiceBean index() { return indexService; }

				@Override
				public SearchServiceBean search() { return searchService; }

				@Override
				public PermissionServiceBean permissions() { return permissionService; }
				
				@Override
				public DvObjectServiceBean dvObjects() { return dvObjectService; }
                                
                                @Override
				public DataFileServiceBean files() { return dataFileService; }
				
				@Override public EntityManager em() { return em; }

				@Override
				public DataverseFacetServiceBean facets() {
					return dataverseFacetService;
				}
                                
				@Override
				public FeaturedDataverseServiceBean featuredDataverses() {
					return featuredDataverseService;
				}
 
                                @Override
				public TemplateServiceBean templates() {
					return templateService;
				}
				@Override
				public DataverseEngine engine() { 
					return new DataverseEngine() {
						@Override
						public <R> R submit(Command<R> aCommand) throws CommandException {
							return EjbDataverseEngine.this.submit(aCommand);
						}
					};
				}
			};
		}
		
		return ctxt;
	}
	
}
