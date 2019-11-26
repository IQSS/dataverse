package edu.harvard.iq.dataverse.dataverse.themewidget;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseThemeCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;

@Stateless
public class ThemeWidgetService {

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ThemeWidgetService() {
    }

    @Inject
    public ThemeWidgetService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
    }

    // -------------------- LOGIC --------------------

    public Dataverse saveOrUpdateUploadedTheme(Dataverse dataverse, File themeFile) {
        return commandEngine.submit(new UpdateDataverseThemeCommand(dataverse, themeFile, dvRequestService.getDataverseRequest()));
    }

    public Dataverse inheritThemeFromRoot(Dataverse dataverse) {
        dataverse.setDataverseTheme(null);

        return commandEngine.submit(new UpdateDataverseThemeCommand(dataverse, null, dvRequestService.getDataverseRequest()));
    }
}
