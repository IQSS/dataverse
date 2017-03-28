/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xoai.model.xoai.Element;
import com.lyncode.xoai.dataprovider.repository.SetRepository;
import com.lyncode.xoai.dataprovider.handlers.results.ListSetsResult;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.model.xoai.XOAIMetadata;
import edu.harvard.iq.dataverse.harvest.server.OAISet;
import edu.harvard.iq.dataverse.harvest.server.OAISetServiceBean;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 */
public class XsetRepository implements SetRepository {
    private static Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.XsetRepository");
    
    private OAISetServiceBean setService;

    public XsetRepository (OAISetServiceBean setService) {
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
        List<OAISet> dataverseOAISets = setService.findAll();
        
        if (dataverseOAISets == null || dataverseOAISets.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public ListSetsResult retrieveSets(int offset, int length) {
        logger.fine("calling retrieveSets()");
        List<OAISet> dataverseOAISets = setService.findAll();
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
