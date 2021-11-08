package edu.harvard.iq.dataverse.harvest.server.xoai;

import org.dspace.xoai.dataprovider.handlers.results.ListSetsResult;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.xoai.Element;
import org.dspace.xoai.model.xoai.XOAIMetadata;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;
import edu.harvard.iq.dataverse.persistence.harvest.OAISet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Leonid Andreev
 */
public class XsetRepository implements SetRepository {
    private static Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.XsetRepository");

    private OAISetServiceBean setService;

    // -------------------- CONSTRUCTORS --------------------

    public XsetRepository(OAISetServiceBean setService) {
        super();
        this.setService = setService;
    }

    // -------------------- GETTERS --------------------

    public OAISetServiceBean getSetService() {
        return setService;
    }

    // -------------------- LOGIC --------------------

    @Override
    public boolean supportSets() {
        logger.fine("calling supportSets()");
        List<OAISet> dataverseOAISets = setService.findAllNamedSets();

        return dataverseOAISets != null && !dataverseOAISets.isEmpty();
    }

    @Override
    public ListSetsResult retrieveSets(int offset, int length) {
        logger.fine("calling retrieveSets()");
        List<OAISet> dataverseOAISets = setService.findAllNamedSets();
        List<Set> XOAISets = new ArrayList<Set>();

        if (dataverseOAISets != null) {
            for (int i = 0; i < dataverseOAISets.size(); i++) {
                OAISet dataverseSet = dataverseOAISets.get(i);
                Set xoaiSet = new Set(dataverseSet.getSpec());
                xoaiSet.withName(dataverseSet.getName());
                XOAIMetadata xMetadata = new XOAIMetadata();
                Element element = new Element("description");
                element.withField("description", dataverseSet.getDescription());
                xMetadata.getElements().add(element);
                xoaiSet.withDescription(xMetadata);
                XOAISets.add(xoaiSet);
            }
        }

        return new ListSetsResult(offset + length < XOAISets.size(),
                XOAISets.subList(offset, Math.min(offset + length, XOAISets.size())));
    }

    @Override
    public boolean exists(String setSpec) {
        List<OAISet> sets = setService.findAllNamedSets();
        return sets.stream()
                .map(OAISet::getName)
                .anyMatch(setSpec::equals);
    }

    // -------------------- SETTERS --------------------

    public void setSetService(OAISetServiceBean setService) {
        this.setService = setService;
    }
}
