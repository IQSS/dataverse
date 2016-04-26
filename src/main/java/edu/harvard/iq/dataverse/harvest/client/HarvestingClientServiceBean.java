package edu.harvard.iq.dataverse.harvest.client;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.bean.ManagedBean;
import javax.inject.Named;

/**
 *
 * @author Leonid Andreev
 * 
 * Dedicated service for managing Harvesting Client Configurations
 */
@Stateless(name = "harvesterService")
@Named
@ManagedBean
public class HarvestingClientServiceBean {
    @EJB
    DataverseServiceBean dataverseService;
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvestingClinetServiceBean");    
}
