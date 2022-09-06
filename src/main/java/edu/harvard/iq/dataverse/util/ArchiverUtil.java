package edu.harvard.iq.dataverse.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

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
        if (className != null) {
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
    
    public static boolean onlySingleVersionArchiving(Class<? extends AbstractSubmitToArchiveCommand> clazz, SettingsServiceBean settingsService) {
        Method m;
        try {
            m = clazz.getMethod("isSingleVersion", SettingsServiceBean.class);
            Object[] params = { settingsService };
            return (Boolean) m.invoke(null, params);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return (AbstractSubmitToArchiveCommand.isSingleVersion(settingsService));
    }

    public static boolean isSomeVersionArchived(Dataset dataset) {
        boolean someVersionArchived = false;
        for (DatasetVersion dv : dataset.getVersions()) {
            if (dv.getArchivalCopyLocation() != null) {
                someVersionArchived = true;
                break;
            }
        }

        return someVersionArchived;
    }

}