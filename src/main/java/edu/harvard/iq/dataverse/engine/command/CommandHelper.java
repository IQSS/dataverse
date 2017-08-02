package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.authorization.Permission;
import java.util.*;

/**
 * Helper object for the Command class. In java8, these will probably be static
 * methods in the {@link Command} interface.
 *
 * @author michael
 */
public class CommandHelper {
	
	public static final CommandHelper CH = new CommandHelper();
	
	/**
     * Given a {@link Command} sub-class, returns the set of permissions needed
     * to be able to execute it. Needed permissions are specified by annotating
     * the command's class with the {@link RequiredPermissions} annotation.
	 * 
	 * @param cmdClass A class of command
     * @return Set of permissions, or {@code null} if the command's class was
     * not annotated.
	 */
    public Map<String, Set<Permission>> permissionsRequired(Class<? extends Command> cmdClass) {
		RequiredPermissions requiredPerms = cmdClass.getAnnotation(RequiredPermissions.class);
        if (requiredPerms == null) {
			// try for the permission map
            RequiredPermissionsMap reqPermMap = cmdClass.getAnnotation(RequiredPermissionsMap.class);
            if (reqPermMap == null) {
                // No annotations here. Look up the class hierachy
                Class superClass = cmdClass.getSuperclass();
                if (superClass != null) {
                    return permissionsRequired(superClass);
                } else {
                    throw new IllegalArgumentException("Command class " + cmdClass.getCanonicalName() 
                     + ", and its superclasses, do not declare required permissions.");
                }
            }
			Map<String, Set<Permission>> retVal = new TreeMap<>();
            for (RequiredPermissions rp : reqPermMap.value()) {
                retVal.put(rp.dataverseName(), asPermissionSet(rp.value()));
			}
			return retVal;
			
		} else {
			Permission[] required = requiredPerms.value();
			return Collections.singletonMap(requiredPerms.dataverseName(),
                    asPermissionSet(required));
		}
	}
	
	/**
     * Given a command, returns the set of permissions needed to be able to
     * execute it. Needed permissions are specified by annotating the command's
     * class with the {@link RequiredPermissions} annotation.
	 * 
	 * @param c The command
     * @return Set of permissions, or {@code null} if the command's class was
     * not annotated.
	 */
    public Map<String, Set<Permission>> permissionsRequired(Command c) {
		return permissionsRequired(c.getClass());
	}
	
    private Set<Permission> asPermissionSet(Permission[] permissionArray) {
        return (permissionArray.length == 0) ? EnumSet.noneOf(Permission.class)
                : (permissionArray.length == 1) ? EnumSet.of(permissionArray[0])
															: EnumSet.of(permissionArray[0], permissionArray);
	}
}
