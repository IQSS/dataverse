/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import io.gdcc.xoai.model.xoai.Element;
import io.gdcc.xoai.dataprovider.repository.SetRepository;
import io.gdcc.xoai.dataprovider.handlers.results.ListSetsResult;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.model.xoai.XOAIMetadata;
import edu.harvard.iq.dataverse.harvest.server.OAISet;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 */
public class DataverseXoaiSetRepository implements SetRepository {
    private static Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.DataverseXoaiSetRepository");
    
    private OAISetServiceBean setService;

    public DataverseXoaiSetRepository (OAISetServiceBean setService) {
        super();
        this.setService = setService;
    }
    
    public OAISetServiceBean getSetService() {
        return setService;
    }
    
    public void setSetService(OAISetServiceBean setService) {
        this.setService = setService;
    }


    @Override
    public boolean supportSets() {
        logger.fine("calling supportSets()");
        List<OAISet> dataverseOAISets = setService.findAllNamedSets();
        
        if (dataverseOAISets == null || dataverseOAISets.isEmpty()) {
            return false;
        }
        return true;
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
                XOAIMetadata xoaiMetadata = new XOAIMetadata();
                Element element = new Element("description");
                element.withField("description", dataverseSet.getDescription());
                xoaiMetadata.getElements().add(element);
                xoaiSet.withDescription(xoaiMetadata);
                XOAISets.add(xoaiSet);
            }
        }
        
        return new ListSetsResult(offset + length < XOAISets.size(), XOAISets.subList(offset, Math.min(offset + length, XOAISets.size())));
    }

    @Override
    public boolean exists(String setSpec) {
        //for (Set s : this.sets)
        //    if (s.getSpec().equals(setSpec))
        //        return true;

        return false;
    }
    
}
