package edu.harvard.iq.dataverse.provenance;

import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand;
import edu.harvard.pass.cpl.CPL;
import java.util.logging.Logger;

public class ProvUtil {

    private static final Logger logger = Logger.getLogger(CreatePrivateUrlCommand.class.getCanonicalName());

    public static String getCplVersion() {
        return CPL.VERSION_STR;
    }

}
