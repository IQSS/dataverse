package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Test implementation of the dataverse engine service. Does not check permissions.
 * @author michael
 */
public class TestDataverseEngine implements DataverseEngine {
	
	private final TestCommandContext ctxt;
    
    private final Map<DvObject, Set<Permission>> requiredPermissionsForObjects = new HashMap<>();
    
	public TestDataverseEngine(TestCommandContext ctxt) {
		this.ctxt = ctxt;
	}
	
	@Override
	public <R> R submit(Command<R> aCommand) throws CommandException {
        Map<String, DvObject> affectedDvs = aCommand.getAffectedDvObjects();
        final Map<String, Set<Permission>> requiredPermissions = aCommand.getRequiredPermissions();
        aCommand.getRequest();
        for ( String dvObjKey : affectedDvs.keySet() ) {
            requiredPermissionsForObjects.put( affectedDvs.get(dvObjKey), requiredPermissions.get(dvObjKey) );
        }
		return aCommand.execute(ctxt);
	}

    public Map<DvObject, Set<Permission>> getReqiredPermissionsForObjects() {
        return requiredPermissionsForObjects;
    }
	
}
