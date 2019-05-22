package edu.harvard.iq.dataverse.util;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple class to reflectively get an instance of the desired class for
 * archiving.
 * 
 */
public class ArchiverUtil {

    private static final Logger logger = Logger.getLogger(ArchiverUtil.class.getName());

    public ArchiverUtil() {
    }

    public static AbstractSubmitToArchiveCommand createSubmitToArchiveCommand(String className, DataverseRequest dvr, DatasetVersion version) {
        if (StringUtils.isNotEmpty(className)) {
            try {
                Class<?> clazz = Class.forName(className);
                if (AbstractSubmitToArchiveCommand.class.isAssignableFrom(clazz)) {
                    Constructor<?> ctor;
                    ctor = clazz.getConstructor(DataverseRequest.class, DatasetVersion.class);
                    return (AbstractSubmitToArchiveCommand) ctor.newInstance(new Object[] { dvr, version });
                }
            } catch (Exception e) {
                logger.warning("Unable to instantiate an Archiver of class: " + className);
                e.printStackTrace();
            }
        }
        return null;
    }
}
