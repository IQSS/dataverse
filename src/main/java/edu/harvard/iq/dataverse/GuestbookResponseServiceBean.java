/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

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
        return em.createQuery("select object(o) from GuestbookResponse as o order by o.responseTime desc", GuestbookResponse.class).getResultList();
    }

    public List<Long> findAllIds() {
        return findAllIds(null);
    }

    public List<Long> findAllIds(Long dataverseId) {
        if (dataverseId == null) {
            return em.createQuery("select o.id from GuestbookResponse as o order by o.responseTime desc", Long.class).getResultList();
        }
        return em.createQuery("select o.id from GuestbookResponse  o, Dataset d where o.dataset.id = d.id and d.owner.id = " + dataverseId + " order by o.responseTime desc", Long.class).getResultList();
    }

    public List<GuestbookResponse> findAllByGuestbookId(Long guestbookId) {

        if (guestbookId == null) {
        } else {
            return em.createQuery("select o from GuestbookResponse as o where o.guestbook.id = " + guestbookId + " order by o.responseTime desc", GuestbookResponse.class).getResultList();
        }
        return null;
    }
    
    public List<Object[]> findArrayByDataverseId (Long dataverseId){

        String queryString = "select r.id, g.name, v.value, r.responsetime, r.downloadtype,  m.label, r.name, r.email, r.institution, r.position from guestbookresponse r,"
                + " datasetfieldvalue v, filemetadata m, dvobject o, guestbook g  "
                + " where "  
                + " v.datasetfield_id = (select id from datasetfield f where datasetfieldtype_id = 1 "
                + " and datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id )) "
                + " and m.datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id ) "
                + "  and m.datafile_id = r.datafile_id "
                + "  and r.dataset_id = o.id "
                + "  and r.guestbook_id = g.id "
                + " and  o.owner_id = "
                + dataverseId.toString()
                + ";";           
        
        return findArray(queryString);       
    }
    
    public List<Object[]> findArrayByDataverseIdAndGuestbookId (Long dataverseId, Long guestbookId){

        String queryString = "select r.id, g.name, v.value, r.responsetime, r.downloadtype,  m.label, r.name, r.email, r.institution, r.position from guestbookresponse r,"
                + " datasetfieldvalue v, filemetadata m, dvobject o, guestbook g  "
                + " where "  
                + " v.datasetfield_id = (select id from datasetfield f where datasetfieldtype_id = 1 "
                + " and datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id )) "
                + " and m.datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id ) "
                + "  and m.datafile_id = r.datafile_id "
                + "  and r.dataset_id = o.id "
                + "  and r.guestbook_id = g.id "
                + " and  o.owner_id = "
                + dataverseId.toString()
                + " and r.guestbook_id = "
                + guestbookId.toString()
                + ";";           
        
        return findArray(queryString);       
    }

    
    
    
    
    private List<Object[]> findArray (String queryString){

        List<Object[]> retVal =  new ArrayList<>();

        List<Object[]> guestbookResults = em.createNativeQuery(queryString).getResultList();
        
        for (Object[] result : guestbookResults) {
            Object[] singleResult = new Object[10];
            singleResult[0] = result[1];
            singleResult[1] = result[2];
            if (result[3] != null){
                singleResult[2] = new SimpleDateFormat("MM/d/yyyy").format((Date) result[3]); 
            } else {
                singleResult[2] = "N/A";
            }

            singleResult[3] = result[4];
            singleResult[4] = result[5];
            singleResult[5] = result[6];
            singleResult[6] = result[7];
            singleResult[7] = result[8];
            singleResult[8] = result[9];
            String cqString = "select q.questionstring, r.response  from customquestionresponse r, customquestion q where q.id = r.customquestion_id and r.guestbookResponse_id = " + (Integer) result[0];
                singleResult[9]  = em.createNativeQuery(cqString).getResultList();
            
            /*
            List<CustomQuestionResponse> customResponses = em.createQuery("select o from CustomQuestionResponse as  o where o.guestbookResponse.id = " + (Integer) result[0] + " order by o.customQuestion.id ", CustomQuestionResponse.class).getResultList();
            if(!customResponses.isEmpty()){
                singleResult[8] =  customResponses;
            }*/
            
            retVal.add(singleResult);
        }
        guestbookResults = null;       
        
        return retVal;
        
    }
    

    
    public List<Object[]> findArrayByGuestbookIdAndDataverseId (Long guestbookId, Long dataverseId){

        Guestbook gbIn = em.find(Guestbook.class, guestbookId);
        boolean hasCustomQuestions = gbIn.getCustomQuestions() != null;
        List<Object[]> retVal =  new ArrayList<>();

        String queryString = "select  r.id, v.value, r.responsetime, r.downloadtype,  m.label, r.name from guestbookresponse r,"
                + " datasetfieldvalue v, filemetadata m , dvobject o    "
                + " where "  
                + " v.datasetfield_id = (select id from datasetfield f where datasetfieldtype_id = 1 "
                + " and datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id )) "
                + " and m.datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id ) "
                + "  and m.datafile_id = r.datafile_id "
                + "  and r.dataset_id = o.id "
                + " and  o.owner_id = "
                + dataverseId.toString()
                + " and  r.guestbook_id = "
                + guestbookId.toString()
                + ";";
        List<Object[]> guestbookResults = em.createNativeQuery(queryString).getResultList();

        for (Object[] result : guestbookResults) {
            Object[] singleResult = new Object[6];
            singleResult[0] = result[1];
            if (result[2] != null){
                            singleResult[1] = new SimpleDateFormat("MMMM d, yyyy").format((Date) result[2]);
            } else {
                singleResult[1] =  "N/A";
            }

            singleResult[2] = result[3];
            singleResult[3] = result[4];
            singleResult[4] = result[5];
            if(hasCustomQuestions){
                String cqString = "select q.questionstring, r.response  from customquestionresponse r, customquestion q where q.id = r.customquestion_id and r.guestbookResponse_id = " + (Integer) result[0];
                singleResult[5]   = em.createNativeQuery(cqString).getResultList();
            }
            retVal.add(singleResult);
        }
        guestbookResults = null;       
        
        return retVal;
    }
    
    public Long findCountByGuestbookId(Long guestbookId, Long dataverseId) {

        if (guestbookId == null) {
                    return 0L;
        } else if ( dataverseId == null) {
            String queryString = "select count(o) from GuestbookResponse as o where o.guestbook_id = " + guestbookId;
            Query query = em.createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        } else  {
            String queryString = "select count(o) from GuestbookResponse as o, Dataset d, DvObject obj where o.dataset_id = d.id and d.id = obj.id and obj.owner_id = " + dataverseId + "and o.guestbook_id = " + guestbookId;
            Query query = em.createNativeQuery(queryString);
            return (Long) query.getSingleResult();            
        }

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

        return em.createQuery(queryString, Long.class).getResultList();
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
            queryString = "select count(o.id) from GuestbookResponse  o,  DvObject v where o.dataset_id = v.id and v.owner_id = " + dataverseId + " ";
        } else {
            queryString = "select count(o.id) from GuestbookResponse  o ";
        }

        Query query = em.createNativeQuery(queryString);
        return (Long) query.getSingleResult();
    }

    public List<GuestbookResponse> findAllByDataverse(Long dataverseId) {
        return em.createQuery("select object(o) from GuestbookResponse  o, Dataset d where o.dataset.id = d.id and d.owner.id = " + dataverseId + " order by o.responseTime desc", GuestbookResponse.class).getResultList();
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
        TypedQuery<GuestbookResponse> query = em.createQuery(queryString, GuestbookResponse.class);

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

    private String generateIDsforTempInsert(List<Long> idList) {
        int count = 0;
        StringBuffer sb = new StringBuffer();
        Iterator<Long> iter = idList.iterator();
        while (iter.hasNext()) {
            Long id = iter.next();
            sb.append("(").append(id).append(",").append(count++).append(")");
            if (iter.hasNext()) {
                sb.append(",");
            }
        }

        return sb.toString();
    }


    public List<Object[]> findCustomResponsePerGuestbookResponse(Long gbrId) {

        String gbrCustomQuestionQueryString = "select response, cq.id "
                + " from guestbookresponse gbr, customquestion cq, customquestionresponse cqr "
                + "where gbr.guestbook_id = cq.guestbook_id "
                + " and gbr.id = cqr.guestbookresponse_id "
                + "and cq.id = cqr.customquestion_id "
                + " and cqr.guestbookresponse_id =  " + gbrId;
        TypedQuery<Object[]> query = em.createQuery(gbrCustomQuestionQueryString, Object[].class);

        return convertIntegerToLong(query.getResultList(), 1);
    }

    private Guestbook findDefaultGuestbook() {
        Guestbook guestbook = new Guestbook();
        String queryStr = "SELECT object(o) FROM Guestbook as o WHERE o.dataverse.id = null";
        List<Guestbook> resultList = em.createQuery(queryStr, Guestbook.class).getResultList();

        if (resultList.size() >= 1) {
            guestbook = resultList.get(0);
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
    
    
    public GuestbookResponse initGuestbookResponseForFragment(Dataset dataset, FileMetadata fileMetadata, DataverseSession session){   
        
        DatasetVersion workingVersion = null;
        if (fileMetadata != null){
            workingVersion = fileMetadata.getDatasetVersion();
        } else {
            workingVersion = dataset.getLatestVersion();
        }

       
        GuestbookResponse guestbookResponse = new GuestbookResponse();
        
        if(workingVersion != null && workingVersion.isDraft()){           
            guestbookResponse.setWriteResponse(false);
        } 
        
        if (fileMetadata != null){
           guestbookResponse.setDataFile(fileMetadata.getDataFile());
        }

        if (dataset.getGuestbook() != null) {
            guestbookResponse.setGuestbook(workingVersion.getDataset().getGuestbook());
            setUserDefaultResponses(guestbookResponse, session);
            if (fileMetadata != null){
                guestbookResponse.setDataFile(fileMetadata.getDataFile());
            }            
        } else {
            if (fileMetadata != null){
                 guestbookResponse = initDefaultGuestbookResponse(dataset, fileMetadata.getDataFile(),  session);
            } else {
                 guestbookResponse = initDefaultGuestbookResponse(dataset, null, session);
            }          
        }
        if (dataset.getGuestbook() != null && !dataset.getGuestbook().getCustomQuestions().isEmpty()) {
            initCustomQuestions(guestbookResponse, dataset);
        }
        guestbookResponse.setDownloadtype("Download");

        guestbookResponse.setDataset(dataset);
        
        return guestbookResponse;
    }
    
    public GuestbookResponse initGuestbookResponseForFragment(FileMetadata fileMetadata, DataverseSession session){    

        return initGuestbookResponseForFragment(fileMetadata.getDatasetVersion().getDataset(), fileMetadata, session);
    }
    
    public void initGuestbookResponse(FileMetadata fileMetadata, String downloadType, DataverseSession session){
         initGuestbookResponse(fileMetadata, downloadType, null, session);
    }
    
    public GuestbookResponse initGuestbookResponse(FileMetadata fileMetadata, String downloadFormat, String selectedFileIds, DataverseSession session) {
        Dataset dataset;              
        DatasetVersion workingVersion = null;
        if (fileMetadata != null){
            workingVersion = fileMetadata.getDatasetVersion();
        }


        
        GuestbookResponse guestbookResponse = new GuestbookResponse();
        
        if(workingVersion != null && workingVersion.isDraft()){
            guestbookResponse.setWriteResponse(false);
        }
        
        dataset = workingVersion.getDataset();
        
        if (fileMetadata != null){
           guestbookResponse.setDataFile(fileMetadata.getDataFile());
        }

        if (dataset.getGuestbook() != null) {
            guestbookResponse.setGuestbook(workingVersion.getDataset().getGuestbook());
            setUserDefaultResponses(guestbookResponse, session);
            if (fileMetadata != null){
                guestbookResponse.setDataFile(fileMetadata.getDataFile());
            }            
        } else {
            if (fileMetadata != null){
                 guestbookResponse = initDefaultGuestbookResponse(dataset, fileMetadata.getDataFile(),  session);
            } else {
                 guestbookResponse = initDefaultGuestbookResponse(dataset, null, session);
            }          
        }
        if (dataset.getGuestbook() != null && !dataset.getGuestbook().getCustomQuestions().isEmpty()) {
            initCustomQuestions(guestbookResponse, dataset);
        }
        guestbookResponse.setDownloadtype("Download");
        if(downloadFormat.toLowerCase().equals("subset")){
            guestbookResponse.setDownloadtype("Subset");
        }
        if(downloadFormat.toLowerCase().equals("explore")){
            guestbookResponse.setDownloadtype("Explore");
        }
        guestbookResponse.setDataset(dataset);
        
        return guestbookResponse;
    }
    
    private void initCustomQuestions(GuestbookResponse guestbookResponse, Dataset dataset) {
        guestbookResponse.setCustomQuestionResponses(new ArrayList());
        for (CustomQuestion cq : dataset.getGuestbook().getCustomQuestions()) {
            CustomQuestionResponse cqr = new CustomQuestionResponse();
            cqr.setGuestbookResponse(guestbookResponse);
            cqr.setCustomQuestion(cq);
            cqr.setResponse("");
            if (cq.getQuestionType().equals("options")) {
                //response select Items
                cqr.setResponseSelectItems(setResponseUISelectItems(cq));
            }
            guestbookResponse.getCustomQuestionResponses().add(cqr);
        }
    }
    
    private void setUserDefaultResponses(GuestbookResponse guestbookResponse, DataverseSession session) {
        User user = session.getUser();
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
        guestbookResponse.setSessionId(session.toString());
    }

    public GuestbookResponse initDefaultGuestbookResponse(Dataset dataset, DataFile dataFile, DataverseSession session) {
        GuestbookResponse guestbookResponse = new GuestbookResponse();
        guestbookResponse.setGuestbook(findDefaultGuestbook());
       if(dataset.getLatestVersion() != null && dataset.getLatestVersion().isDraft()){
            guestbookResponse.setWriteResponse(false);
        }
        if (dataFile != null){
            guestbookResponse.setDataFile(dataFile);
        }        
        guestbookResponse.setDataset(dataset);
        guestbookResponse.setResponseTime(new Date());
        guestbookResponse.setSessionId(session.toString());
        guestbookResponse.setDownloadtype("Download");
        setUserDefaultResponses(guestbookResponse, session);
        return guestbookResponse;
    }
    
    public void guestbookResponseValidator(FacesContext context, UIComponent toValidate, Object value) {
        String response = (String) value;

        if (response != null && response.length() > 255) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, JH.localize("dataset.guestbookResponse.guestbook.responseTooLong"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }
    
    public GuestbookResponse modifyDatafile(GuestbookResponse in, FileMetadata fm) {
        if (in != null && fm.getDataFile() != null) {
            in.setDataFile(fm.getDataFile());
        }
        if (in != null && fm.getDatasetVersion() != null && fm.getDatasetVersion().isDraft() ) {
            in.setWriteResponse(false);
        }
        return in;
    }
    
    public GuestbookResponse modifySelectedFileIds(GuestbookResponse in, String fileIds) {
        if (in != null && fileIds != null) {
            in.setSelectedFileIds(fileIds);
        }
        return in;
    }

    public GuestbookResponse modifyDatafileAndFormat(GuestbookResponse in, FileMetadata fm, String format) {
        if (in != null && fm.getDataFile() != null) {
            in.setFileFormat(format);
            in.setDataFile(fm.getDataFile());
        }
        if (in != null && fm.getDatasetVersion() != null && fm.getDatasetVersion().isDraft() ) {
            in.setWriteResponse(false);
        }
        
        return in;
    }

    public Boolean validateGuestbookResponse(GuestbookResponse guestbookResponse, String type) {
        boolean valid = true;
        Dataset dataset = guestbookResponse.getDataset();
        if (dataset.getGuestbook() != null) {
            if (dataset.getGuestbook().isNameRequired()) {
                if (guestbookResponse.getName() == null) {
                    valid = false;
                } else {
                    valid &= !guestbookResponse.getName().isEmpty();
                }
            }
            if (dataset.getGuestbook().isEmailRequired()) {
                if (guestbookResponse.getEmail() == null) {
                    valid = false;
                } else {
                    valid &= !guestbookResponse.getEmail().isEmpty();
                }
            }
            if (dataset.getGuestbook().isInstitutionRequired()) {
                if (guestbookResponse.getInstitution() == null) {
                    valid = false;
                } else {
                    valid &= !guestbookResponse.getInstitution().isEmpty();
                }
            }
            if (dataset.getGuestbook().isPositionRequired()) {
                if (guestbookResponse.getPosition() == null) {
                    valid = false;
                } else {
                    valid &= !guestbookResponse.getPosition().isEmpty();
                }
            }
        }

        if (dataset.getGuestbook() != null && !dataset.getGuestbook().getCustomQuestions().isEmpty()) {
            for (CustomQuestion cq : dataset.getGuestbook().getCustomQuestions()) {
                if (cq.isRequired()) {
                    for (CustomQuestionResponse cqr : guestbookResponse.getCustomQuestionResponses()) {
                        if (cqr.getCustomQuestion().equals(cq)) {
                            valid &= (cqr.getResponse() != null && !cqr.getResponse().isEmpty());
                        }
                    }
                }
            }
        }
            
        return valid;
    }
    
    private List<SelectItem> setResponseUISelectItems(CustomQuestion cq) {
        List<SelectItem> retList = new ArrayList();
        for (CustomQuestionValue cqv : cq.getCustomQuestionValues()) {
            SelectItem si = new SelectItem(cqv.getValueString(), cqv.getValueString());
            retList.add(si);
        }
        return retList;
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
