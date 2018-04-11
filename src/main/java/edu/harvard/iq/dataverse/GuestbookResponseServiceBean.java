/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.application.FacesMessage;
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
    private static final Logger logger = Logger.getLogger(GuestbookResponseServiceBean.class.getCanonicalName());
    
    // The query below is used for retrieving guestbook responses used to download 
    // the collected data, in CSV format, from the manage-guestbooks and 
    // guestbook-results pages. (for entire dataverses, and for the individual 
    // guestbooks within dataverses, respectively). -- L.A. 
    private static final String BASE_QUERY_STRING_FOR_DOWNLOAD_AS_CSV = "select r.id, g.name, v.value,  r.responsetime, r.downloadtype,"
                + " m.label, r.dataFile_id, r.name, r.email, r.institution, r.position "
                + "from guestbookresponse r, datasetfieldvalue v, filemetadata m, dvobject o, guestbook g  "
                + "where "  
                + " v.datasetfield_id = (select id from datasetfield f where datasetfieldtype_id = 1 "
                + " and datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id )) "
                + " and m.datasetversion_id = (select max(datasetversion_id) from filemetadata where datafile_id =r.datafile_id ) "
                + " and m.datafile_id = r.datafile_id "
                + " and r.dataset_id = o.id "
                + " and r.guestbook_id = g.id ";
    
    // And this query is used for retrieving guestbook responses for displaying 
    // on the guestbook-results.xhtml page (the info we show on the page is 
    // less detailed than what we let the users download as CSV files, so this 
    // query has fewer fields than the one above). -- L.A.
    private static final String BASE_QUERY_STRING_FOR_PAGE_DISPLAY = "select  r.id, v.value, r.responsetime, r.downloadtype,  m.label, r.name "
                + "from guestbookresponse r, datasetfieldvalue v, filemetadata m , dvobject o "
                + "where "  
                + " v.datasetfield_id = (select id from datasetfield f where datasetfieldtype_id = 1 "
                + " and datasetversion_id = (select max(id) from datasetversion where dataset_id =r.dataset_id )) "
                + " and m.datasetversion_id = (select max(datasetversion_id) from filemetadata where datafile_id =r.datafile_id ) "
                + " and m.datafile_id = r.datafile_id "
                + " and r.dataset_id = o.id ";
    
    // And a custom query for retrieving *all* the custom question responses, for 
    // a given dataverse, or for an individual guestbook within the dataverse:
    private static final String BASE_QUERY_CUSTOM_QUESTION_ANSWERS = "select q.questionstring, r.response, g.id "
                + "from customquestionresponse r, customquestion q, guestbookresponse g, dvobject o "
                + "where q.id = r.customquestion_id "
                + "and r.guestbookResponse_id = g.id "
                + "and g.dataset_id = o.id ";

    
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/d/yyyy");
    
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
    
    /* 
       This method is used for streaming downloads of guestbook responses, in 
       CSV format, both for individual guestbooks, and for entire dataverses
       (with guestbookId = null).
     */
    private static final String SEPARATOR = ",";
    private static final String NEWLINE = "\n";
    public void streamResponsesByDataverseIdAndGuestbookId(OutputStream out, Long dataverseId, Long guestbookId) throws IOException {
        
        // Before we do anything else, create a map of all the custom question responses
        // available for this dataverse (or, this individual guestbook within the
        // dataverse; see the comment below, how it's saving us a metric f-ton
        // of queries now) -- L.A. 
        
        Map<Integer, Object> customQandAs = mapCustomQuestionAnswersAsStrings(dataverseId, guestbookId);
        
        String queryString = BASE_QUERY_STRING_FOR_DOWNLOAD_AS_CSV
                + " and  o.owner_id = " 
                + dataverseId.toString();
        
        if (guestbookId != null) {
            queryString+= (" and r.guestbook_id = " + guestbookId.toString());
        }
        
        queryString += ";";
        
        List<Object[]> guestbookResults = em.createNativeQuery(queryString).getResultList();

        // the CSV header:
        out.write("Guestbook, Dataset, Date, Type, File Name, File Id, User Name, Email, Institution, Position, Custom Questions\n".getBytes());
        
        for (Object[] result : guestbookResults) {
            Integer guestbookResponseId = (Integer)result[0];
            
            StringBuilder sb = new StringBuilder();
            
            // Since we are formatting the output as comma-separated values, 
            // we should go to the trouble of removing any commas from the 
            // string fields, or the structure of the file will be broken. -- L.A.
            
            // Guestbook name: 
            sb.append(((String)result[1]).replace(',', ' '));
            sb.append(SEPARATOR);

            
            // Dataset name: 
            sb.append(((String)result[2]).replace(',', ' '));
            sb.append(SEPARATOR);
            
            if (result[3] != null) {
                sb.append(DATE_FORMAT.format((Date) result[3]));
            } else {
                sb.append("N/A");
            }
            sb.append(SEPARATOR);
            
            // type: (download, etc.)
            sb.append(result[4]);
            sb.append(SEPARATOR);

            // file name: 
            sb.append(((String)result[5]).replace(',', ' '));
            sb.append(SEPARATOR);

            // file id (numeric):
            sb.append(result[6] == null ? "" : result[6]);
            sb.append(SEPARATOR);
            
            // name supplied in the guestbook response: 
            sb.append(result[7] == null ? "" : ((String)result[7]).replace(',', ' '));
            sb.append(SEPARATOR);
            
            // email: 
            sb.append(result[8] == null ? "" : result[8]);
            sb.append(SEPARATOR);
            
            // institution:
            sb.append(result[9] == null ? "" : ((String)result[9]).replace(',', ' '));
            sb.append(SEPARATOR);
            
            // position: 
            sb.append(result[10] == null ? "" : ((String)result[10]).replace(',', ' '));
            
            // Finally, custom questions and answers, if present:
            
            // (the old implementation, below, would run one extra query FOR EVERY SINGLE
            // guestbookresponse entry! -- instead, we are now pre-caching all the 
            // available custom question responses, with a single native query at 
            // the top of this method. -- L.A.)
            
            /*String cqString = "select q.questionstring, r.response  from customquestionresponse r, customquestion q where q.id = r.customquestion_id and r.guestbookResponse_id = " + result[0];
            List<Object[]> customResponses = em.createNativeQuery(cqString).getResultList();
            if (customResponses != null) {
                for (Object[] response : customResponses) {
                    sb.append(SEPARATOR);
                    sb.append(response[0]);
                    sb.append(SEPARATOR);
                    sb.append(response[1] == null ? "" : response[1]);
                }
            }*/
            
            if (customQandAs.containsKey(guestbookResponseId)) {
                sb.append(customQandAs.get(guestbookResponseId)); 
            } 

            sb.append(NEWLINE);

            // Finally, write the line out: 
            // (i.e., we are writing one guestbook response at a time, thus allowing the 
            // whole thing to stream in real time -- L.A.)
            out.write(sb.toString().getBytes());
            out.flush();
        }
    }
    
    /*
      This method is used to produce an array of guestbook responses for displaying 
      on the guestbook-responses page. 
    */
    public List<Object[]> findArrayByGuestbookIdAndDataverseId (Long guestbookId, Long dataverseId, Long limit){

        Guestbook gbIn = em.find(Guestbook.class, guestbookId);
        boolean hasCustomQuestions = gbIn.getCustomQuestions() != null;
        List<Object[]> retVal =  new ArrayList<>();

        String queryString = BASE_QUERY_STRING_FOR_PAGE_DISPLAY
                + " and  o.owner_id = "
                + dataverseId.toString()
                + " and  r.guestbook_id = "
                + guestbookId.toString();
        
        queryString += " order by r.id desc";
        
        if (limit != null) {
            queryString += (" limit " + limit);
        } 
        
        queryString += ";";
        
        logger.info("search query: "+queryString);
        
        List<Object[]> guestbookResults = em.createNativeQuery(queryString).getResultList();
        
        if (guestbookResults == null || guestbookResults.size() == 0) {
            return retVal; 
        }
        
        Map<Integer, Object> customQandAs = null;
        
        if (hasCustomQuestions) {
            // if this Guestbook has custom questions, let's look up all the 
            // custom question-and-answer pairs for this dataverse and guestbook.
            if (limit == null || guestbookResults.size() < limit) {
                customQandAs = mapCustomQuestionAnswersAsLists(dataverseId, guestbookId, null, null);
            } else {
                // only select the custom question responses for the current range of 
                // guestbook responses - can make a difference on a guestbook with 
                // an insane amount of responses. -- L.A.
                customQandAs = mapCustomQuestionAnswersAsLists(dataverseId, 
                        guestbookId, 
                        (Integer)(guestbookResults.get(0)[0]), 
                        (Integer)(guestbookResults.get(guestbookResults.size()-1)[0]));
            }
        }

        for (Object[] result : guestbookResults) {
            Object[] singleResult = new Object[6];

            Integer guestbookResponseId = (Integer) result[0];

            if (guestbookResponseId != null) {
                singleResult[0] = result[1];
                if (result[2] != null) {
                    singleResult[1] = new SimpleDateFormat("yyyy-MM-dd").format((Date) result[2]);
                } else {
                    singleResult[1] = "N/A";
                }

                singleResult[2] = result[3];
                singleResult[3] = result[4];
                singleResult[4] = result[5];

                // Finally, custom questions and answers, if present:
                if (hasCustomQuestions) {
                    // (the old implementation, below, would run one extra query FOR EVERY SINGLE
                    // guestbookresponse entry! -- instead, we are now pre-caching all the 
                    // available custom question responses, with a single native query, above.
                    // -- L.A.)

                    /*String cqString = "select q.questionstring, r.response  from customquestionresponse r, customquestion q where q.id = r.customquestion_id and r.guestbookResponse_id = " + result[0];
                    singleResult[5]   = em.createNativeQuery(cqString).getResultList();*/
                    if (customQandAs.containsKey(guestbookResponseId)) {
                        singleResult[5] = customQandAs.get(guestbookResponseId);
                    }
                }
                retVal.add(singleResult);
            }
        }
        
        return retVal;
    }
    
    /*
       The 3 methods below are for caching all the custom question responses for this
       guestbook and/or dataverse.
       The results are saved in maps, and later re-combined with the individual 
       "normal" guestbook responses, retrieved from GuestbookResponse table. -- L.A. 
    */
    private Map<Integer, Object> mapCustomQuestionAnswersAsLists(Long dataverseId, Long guestbookId, Integer firstResponse, Integer lastResponse) {
        return selectCustomQuestionAnswers(dataverseId, guestbookId, false, firstResponse, lastResponse);
    }
    
    private Map<Integer, Object> mapCustomQuestionAnswersAsStrings(Long dataverseId, Long guestbookId) {
        return selectCustomQuestionAnswers(dataverseId, guestbookId, true, null, null);
    }
    
    private Map<Integer, Object> selectCustomQuestionAnswers(Long dataverseId, Long guestbookId, boolean asString, Integer lastResponse, Integer firstResponse) {
        Map<Integer, Object> ret = new HashMap<>();

        int count = 0;

        String cqString = BASE_QUERY_CUSTOM_QUESTION_ANSWERS
                + "and o.owner_id = " + dataverseId;
                
        if (guestbookId != null) {
            cqString += ( "and g.guestbook_id = " + guestbookId);
        }
        
        if (firstResponse != null) {
            cqString += (" and r.guestbookResponse_id >= " + firstResponse);
        }
        
        if (lastResponse != null) {
            cqString += (" and r.guestbookResponse_id <= " + lastResponse);
        }
        
        cqString += ";";

        List<Object[]> customResponses = em.createNativeQuery(cqString).getResultList();

        if (customResponses != null) {
            for (Object[] response : customResponses) {
                Integer responseId = (Integer) response[2];

                if (asString) {
                    // as combined strings of comma-separated question and answer values
                    
                    String qa = SEPARATOR + ((String)response[0]).replace(',', ' ') + SEPARATOR + (response[1] == null ? "" : ((String)response[1]).replace(',', ' '));

                    if (ret.containsKey(responseId)) {
                        ret.put(responseId, ret.get(responseId) + qa);
                    } else {
                        ret.put(responseId, qa);
                    }
                } else {
                    // as a list of Object[]s - this is for display on the custom-responses page
                    
                    if (!ret.containsKey(responseId)) {
                        ret.put(responseId, new ArrayList<>());
                    }
                    ((List) ret.get(responseId)).add(response);
                }

                count++;
            }
        }

        logger.info("Found " + count + " responses to custom questions");

        return ret;
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
        String queryString;
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
            item[index] = (long) item[index];
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
        StringBuilder sb = new StringBuilder();
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
        return "Guest";
    }

    public String getUserEMail(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getEmail();
        }
        return "";
    }

    public String getUserInstitution(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getAffiliation();
        }
        return "";
    }

    public String getUserPosition(User user) {
        if (user.isAuthenticated()) {
            AuthenticatedUser authUser = (AuthenticatedUser) user;
            return authUser.getPosition();
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
        
        DatasetVersion workingVersion;
        if (fileMetadata != null){
            workingVersion = fileMetadata.getDatasetVersion();
        } else {
            workingVersion = dataset.getLatestVersion();
        }
       
       
        GuestbookResponse guestbookResponse = new GuestbookResponse();
        
        if(workingVersion != null && workingVersion.isDraft()){           
            guestbookResponse.setWriteResponse(false);
        } 
        
       // guestbookResponse.setDatasetVersion(workingVersion);
        
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
            /**
             * TODO: Investigate this "if downloadFormat=explore" and think
             * about deleting it. When is downloadFormat "explore"? When is this
             * method called? Previously we were passing "explore" to
             * modifyDatafileAndFormat for TwoRavens but now we pass
             * "externalTool" for all external tools, including TwoRavens. When
             * clicking "Explore" and then the name of the tool, we want the
             * name of the exploration tool (i.e. "TwoRavens", "Data Explorer",
             * etc.) to be persisted as the downloadType. We execute
             * guestbookResponse.setDownloadtype(externalTool.getDisplayName())
             * over in the "explore" method of FileDownloadServiceBean just
             * before the guestbookResponse is written.
             */
            guestbookResponse.setDownloadtype("Explore");
        }
        guestbookResponse.setDataset(dataset);
        
        return guestbookResponse;
    }
    
    private void initCustomQuestions(GuestbookResponse guestbookResponse, Dataset dataset) {
        guestbookResponse.setCustomQuestionResponses(new ArrayList<>());
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
    
    private void setUserDefaultResponses(GuestbookResponse guestbookResponse, DataverseSession session, User userIn) {
        User user;
        User sessionUser = session.getUser();
        
        if (userIn != null){
            user = userIn;
        } else{
            user = sessionUser;
        }
        
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
    
    public GuestbookResponse initAPIGuestbookResponse(Dataset dataset, DataFile dataFile, DataverseSession session, User user) {
        GuestbookResponse guestbookResponse = new GuestbookResponse();
        Guestbook datasetGuestbook = dataset.getGuestbook();
        
        if(datasetGuestbook == null){
            guestbookResponse.setGuestbook(findDefaultGuestbook());
        } else { 
            guestbookResponse.setGuestbook(datasetGuestbook);            
        }

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
        setUserDefaultResponses(guestbookResponse, session, user);
        return guestbookResponse;
    }
    
    public boolean guestbookResponseValidator( UIInput toValidate, String value) {
        if (value != null && value.length() > 255) {
            (toValidate).setValid(false);
            FacesContext.getCurrentInstance().addMessage((toValidate).getClientId(),
                           new FacesMessage( FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.guestbookResponse.guestbook.responseTooLong"), null));
            return false;
        }
        return true;
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

    /**
     * This method was added because on the dataset page when a popup is
     * required, ExternalTool is null in the poup itself. We store ExternalTool
     * in the GuestbookResponse as a transient variable so we have access to it
     * later in the popup.
     */
    public GuestbookResponse modifyDatafileAndFormat(GuestbookResponse in, FileMetadata fm, String format, ExternalTool externalTool) {
        if (in != null && externalTool != null) {
            in.setExternalTool(externalTool);
        }
        return modifyDatafileAndFormat(in, fm, format);
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
        List<SelectItem> retList = new ArrayList<>();
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
