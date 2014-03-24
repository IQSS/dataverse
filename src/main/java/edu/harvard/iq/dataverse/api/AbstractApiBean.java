package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import javax.ejb.EJB;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Base class for API beans
 * @author michael
 */
public abstract class AbstractApiBean {
	
	@EJB
	protected EjbDataverseEngine engineSvc;
	
	@EJB
	protected DataverseUserServiceBean userSvc;
	
	@EJB 
	protected DataverseServiceBean dataverseSvc;
	
	@PersistenceContext(unitName = "VDCNet-ejbPU")
	EntityManager em;
	
	@EJB
	DataverseRoleServiceBean rolesSvc;
	
	
	protected DataverseUser findUser( String userIdtf ) {
		return isNumeric(userIdtf) ? engineSvc.getContext().users().find(Long.parseLong(userIdtf))
	 							  : engineSvc.getContext().users().findByUserName(userIdtf);
	}
	
	protected Dataverse findDataverse( String idtf ) {
		return isNumeric(idtf) ? dataverseSvc.find(Long.parseLong(idtf))
	 							  : dataverseSvc.findByAlias(idtf);
	}
	
	protected DvObject findDvo( Long id ) {
		return em.createNamedQuery("DvObject.findById", DvObject.class)
				.setParameter("id", id)
				.getSingleResult();
	}
	
	protected boolean isNumeric( String str ) { return Util.isNumeric(str); };
	protected String error( String msg ) { return Util.error(msg); }
	protected String ok( String msg ) { return Util.ok(msg); }
	protected String ok( JsonObject jo ) { return Util.ok(jo); }
	protected String ok( JsonArray jo ) { return Util.ok(jo); }
	protected String ok( JsonObjectBuilder jo ) { return ok(jo.build()); }
	protected String ok( JsonArrayBuilder jo ) { return ok(jo.build()); }
}
