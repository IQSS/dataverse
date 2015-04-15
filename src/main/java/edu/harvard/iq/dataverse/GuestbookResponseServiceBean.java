/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class GuestbookResponseServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<GuestbookResponse> findAll() {
        return em.createQuery("select object(o) from GuestbookResponse as o order by o.responseTime desc").getResultList();
    }

    public List<Long> findAllIds() {
        return findAllIds(null);
    }

    public List<Long> findAllIds(Long dataverseId) {
        if (dataverseId == null) {
            return em.createQuery("select o.id from GuestbookResponse as o order by o.responseTime desc").getResultList();
        }
        return em.createQuery("select o.id from GuestbookResponse  o, Dataset d where o.dataset.id = d.id and d.owner.id = " + dataverseId + " order by o.responseTime desc").getResultList();
    }

    public List<GuestbookResponse> findAllByGuestbookId(Long guestbookId) {

        if (guestbookId == null) {
        } else {
            return em.createQuery("select o from GuestbookResponse as o where o.guestbook.id = " + guestbookId + " order by o.responseTime desc").getResultList();
        }
        return null;
    }
    
    public Long findCountByGuestbookId(Long guestbookId) {

        if (guestbookId == null) {
        } else {
            String queryString = "select count(o) from GuestbookResponse as o where o.guestbook_id = " + guestbookId;
            Query query = em.createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        }
        return new Long(0);
    }

    public List<Long> findAllIds30Days() {
        return findAllIds30Days(null);
    }

    public List<Long> findAllIds30Days(Long dataverseId) {
        String beginTime;
        String endTime;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        beginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());  // Use yesterday as default value
        cal.add(Calendar.DAY_OF_YEAR, 31);
        endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
        String queryString = "select o.id from GuestbookResponse as o  ";
        if (dataverseId != null) {
            queryString += ", Dataset d where o.dataset.id = d.id and d.owner.id = " + dataverseId + " and ";
        } else {
            queryString += " where ";
        }
        queryString += " o.responseTime >='" + beginTime + "'";
        queryString += " and o.responseTime<='" + endTime + "'";
        queryString += "  order by o.responseTime desc";
        Query query = em.createQuery(queryString);

        return query.getResultList();
    }

    public Long findCount30Days() {
        return findCount30Days(null);
    }

    public Long findCount30Days(Long dataverseId) {
        String beginTime;
        String endTime;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        beginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());  // Use yesterday as default value
        cal.add(Calendar.DAY_OF_YEAR, 31);
        endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
        String queryString = "select count(o.id) from GuestbookResponse as o  ";
        if (dataverseId != null) {
            queryString += ", DvObject v where o.dataset_id = v.id and v.owner_id = " + dataverseId + " and ";
        } else {
            queryString += " where ";
        }
        queryString += " o.responseTime >='" + beginTime + "'";
        queryString += " and o.responseTime<='" + endTime + "'";
        Query query = em.createNativeQuery(queryString);
        return (Long) query.getSingleResult();
    }

    public Long findCountAll() {
        return findCountAll(null);
    }

    public Long findCountAll(Long dataverseId) {
        String queryString = "";
        if (dataverseId != null) {
            queryString = "select count(o.id) from GuestbookResponse  o,  DvObject v, where o.dataset_id = v.id and v.owner_id = " + dataverseId + " ";
        } else {
            queryString = "select count(o.id) from GuestbookResponse  o ";
        }

        Query query = em.createNativeQuery(queryString);
        return (Long) query.getSingleResult();
    }

    public List<GuestbookResponse> findAllByDataverse(Long dataverseId) {
        return em.createQuery("select object(o) from GuestbookResponse  o, Dataset d where o.dataset.id = d.id and d.owner.id = " + dataverseId + " order by o.responseTime desc").getResultList();
    }

    public List<GuestbookResponse> findAllWithin30Days() {
        return findAllWithin30Days(null);
    }

    public List<GuestbookResponse> findAllWithin30Days(Long dataverseId) {
        String beginTime;
        String endTime;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        beginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());  // Use yesterday as default value
        cal.add(Calendar.DAY_OF_YEAR, 31);
        endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
        String queryString = "select object(o) from GuestbookResponse as o  ";
        if (dataverseId != null) {
            queryString += ", Dataset d where o.dataset.id = d.id and d.owner.id = " + dataverseId + " and ";
        } else {
            queryString += " where ";
        }
        queryString += " o.responseTime >='" + beginTime + "'";
        queryString += " and o.responseTime<='" + endTime + "'";
        queryString += "  order by o.responseTime desc";
        Query query = em.createQuery(queryString);

        return query.getResultList();
    }

    private List<Object[]> convertIntegerToLong(List<Object[]> list, int index) {
        for (Object[] item : list) {
            item[index] = new Long((Integer) item[index]);
        }

        return list;
    }

    private String generateTempTableString(List<Long> datasetIds) {
        // first step: create the temp table with the ids

        em.createNativeQuery(" BEGIN; SET TRANSACTION READ WRITE; DROP TABLE IF EXISTS tempid; END;").executeUpdate();
        em.createNativeQuery(" BEGIN; SET TRANSACTION READ WRITE; CREATE TEMPORARY TABLE tempid (tempid integer primary key, orderby integer); END;").executeUpdate();
        em.createNativeQuery(" BEGIN; SET TRANSACTION READ WRITE; INSERT INTO tempid VALUES " + generateIDsforTempInsert(datasetIds) + "; END;").executeUpdate();
        return "select tempid from tempid";
    }

    private String generateIDsforTempInsert(List idList) {
        int count = 0;
        StringBuffer sb = new StringBuffer();
        Iterator iter = idList.iterator();
        while (iter.hasNext()) {
            Long id = (Long) iter.next();
            sb.append("(").append(id).append(",").append(count++).append(")");
            if (iter.hasNext()) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public List<Object[]> findDownloadInfoAll(List<Long> gbrIds) {
        //this query will return multiple rows per response where the study name has changed over version
        //these multiples are filtered out by the method that actually writes the download csv,
        String varString = "(" + generateTempTableString(gbrIds) + ") ";
        String gbrDownloadQueryString = "select u.username, gbr.sessionid, "
                + " gbr.firstname, gbr.lastname, gbr.email, gbr.institution, "
                + " vdc.name, s.protocol, s.authority, m.title, fmd.label, gbr.responsetime, gbr.position, gbr.study_id, gbr.id, gbr.downloadType "
                + " from guestbookresponse gbr LEFT OUTER JOIN vdcuser u ON  "
                + "(gbr.vdcuser_id =u.id),  "
                + " vdc, study s, studyversion sv, metadata m, filemetadata  fmd  "
                + "where gbr.study_id = s.id  "
                + "and s.owner_id = vdc.id  "
                + "and s.id = sv.study_id  "
                + "and sv.metadata_id = m.id "
                + "and gbr.studyfile_id = fmd.studyfile_id "
                + "and sv.id = fmd.studyversion_id "
                + "and sv.id = gbr.studyversion_id "
                + " and gbr.id in " + varString
                + " group by u.username, gbr.sessionid, "
                + " gbr.firstname, gbr.lastname, gbr.email, gbr.institution, "
                + " vdc.name, s.protocol, s.authority, m.title, fmd.label, gbr.responsetime, gbr.position, gbr.study_id, gbr.id, s.id, gbr.downloadType  "
                + "order by s.id, gbr.id";
        System.out.print(gbrDownloadQueryString);
        Query query = em.createNativeQuery(gbrDownloadQueryString);

        return convertIntegerToLong(query.getResultList(), 14);
    }

    public List<Object[]> findCustomResponsePerGuestbookResponse(Long gbrId) {

        String gbrCustomQuestionQueryString = "select response, cq.id "
                + " from guestbookresponse gbr, customquestion cq, customquestionresponse cqr "
                + "where gbr.guestbook_id = cq.guestbook_id "
                + " and gbr.id = cqr.guestbookresponse_id "
                + "and cq.id = cqr.customquestion_id "
                + " and cqr.guestbookresponse_id =  " + gbrId;
        Query query = em.createNativeQuery(gbrCustomQuestionQueryString);

        return convertIntegerToLong(query.getResultList(), 1);
    }

    private Guestbook findDefaultGuestbook() {
        Guestbook guestbook = new Guestbook();
        String queryStr = "SELECT object(o) FROM Guestbook as o WHERE o.dataverse.id = null";
        Query query = em.createQuery(queryStr);
        List resultList = query.getResultList();

        if (resultList.size() >= 1) {
            guestbook = (Guestbook) resultList.get(0);
        }
        return guestbook;

    }

    public String getUserName(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getName();
        }

        try {
            if (user.isBuiltInUser()) {
                BuiltinUser builtinUser = (BuiltinUser) user;
                return builtinUser.getDisplayName();
            }
        } catch (Exception e) {
            return "";
        }
        return "Guest";
    }

    public String getUserEMail(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getEmail();
        }
        try {
            if (user.isBuiltInUser()) {
                BuiltinUser builtinUser = (BuiltinUser) user;
                return builtinUser.getEmail();
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public String getUserInstitution(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getAffiliation();
        }

        try {
            if (user.isBuiltInUser()) {
                BuiltinUser builtinUser = (BuiltinUser) user;
                return builtinUser.getAffiliation();
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public String getUserPosition(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getPosition();
        }
        try {
            if (user.isBuiltInUser()) {
                BuiltinUser builtinUser = (BuiltinUser) user;
                return builtinUser.getPosition();
            }
        } catch (Exception e) {
            return "";
        }

        return "";
    }

    public AuthenticatedUser getAuthenticatedUser(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser;
        }
        return null;
    }

    public GuestbookResponse initDefaultGuestbookResponse(Dataset dataset, DataFile dataFile, User user, DataverseSession session) {
        GuestbookResponse guestbookResponse = new GuestbookResponse();
        guestbookResponse.setGuestbook(findDefaultGuestbook());
        if (dataFile != null){
            guestbookResponse.setDataFile(dataFile);
        }        
        guestbookResponse.setDataset(dataset);
        guestbookResponse.setResponseTime(new Date());
        guestbookResponse.setSessionId(session.toString());

        if (user != null) {
            guestbookResponse.setEmail(getUserEMail(user));
            guestbookResponse.setName(getUserName(user));
            guestbookResponse.setInstitution(getUserInstitution(user));
            guestbookResponse.setPosition(getUserPosition(user));
            guestbookResponse.setAuthenticatedUser(getAuthenticatedUser(user));
        } else {
            guestbookResponse.setEmail("");
            guestbookResponse.setName("");
            guestbookResponse.setInstitution("");
            guestbookResponse.setPosition("");
            guestbookResponse.setAuthenticatedUser(null);
        }
        return guestbookResponse;
    }

    public GuestbookResponse findById(Long id) {
        return em.find(GuestbookResponse.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void save(GuestbookResponse guestbookResponse) {
        em.persist(guestbookResponse);
    }
    
    
    public Long getCountGuestbookResponsesByDataFileId(Long dataFileId) {
        // datafile id is null, will return 0
        Query query = em.createNativeQuery("select count(o.id) from GuestbookResponse  o  where o.datafile_id  = " + dataFileId);
        return (Long) query.getSingleResult();
    }
    
    public Long getCountGuestbookResponsesByDatasetId(Long datasetId) {
        // dataset id is null, will return 0        
        Query query = em.createNativeQuery("select count(o.id) from GuestbookResponse  o  where o.dataset_id  = " + datasetId);
        return (Long) query.getSingleResult();
    }    

    public Long getCountOfAllGuestbookResponses() {
        // dataset id is null, will return 0        
        Query query = em.createNativeQuery("select count(o.id) from GuestbookResponse  o;");
        return (Long) query.getSingleResult();
    }
    
}
