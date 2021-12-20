package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.util.logging.Logger;

/**
 * Simple class to reflectively get an instance of the desired class for
 * archiving.
 */
public class ArchiverUtil {
    private static final Logger logger = Logger.getLogger(ArchiverUtil.class.getName());

    // -------------------- CONSTRUCTORS --------------------

    public ArchiverUtil() { }

    // -------------------- LOGIC --------------------

    public static AbstractSubmitToArchiveCommand createSubmitToArchiveCommand(
            String className, DataverseRequest dvr, DatasetVersion version, AuthenticationServiceBean authenticationService, Clock clock) {
        if (StringUtils.isEmpty(className)) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(className);
            if (AbstractSubmitToArchiveCommand.class.isAssignableFrom(cls)) {
                Constructor<?> ctor
                        = cls.getConstructor(DataverseRequest.class, DatasetVersion.class, AuthenticationServiceBean.class, Clock.class);
                return (AbstractSubmitToArchiveCommand) ctor.newInstance(new Object[] { dvr, version, authenticationService, clock });
            }
        } catch (Exception e) {
            logger.warning("Unable to instantiate an Archiver of class: " + className);
        }
        return null;
    }
}
