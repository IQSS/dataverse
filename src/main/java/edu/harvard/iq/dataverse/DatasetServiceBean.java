/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Dataset find(Object pk) {
        return em.find(Dataset.class, pk);
    }

    public List<Dataset> findByOwnerId(Long ownerId) {
        return findByOwnerId(ownerId, false);
    }

    public List<Dataset> findByOwnerId(Long ownerId, Boolean omitDeaccessioned) {
        List<Dataset> retList = new ArrayList();
        Query query = em.createQuery("select object(o) from Dataset as o where o.owner.id =:ownerId order by o.id");
        query.setParameter("ownerId", ownerId);
        if (!omitDeaccessioned) {
            return query.getResultList();
        } else {
            for (Object o : query.getResultList()) {
                Dataset ds = (Dataset) o;
                for (DatasetVersion dsv : ds.getVersions()) {
                    if (!dsv.isDeaccessioned()) {
                        retList.add(ds);
                        break;
                    }
                }
            }
            return retList;
        }
    }

    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id").getResultList();
    }

    /**
     * @todo write this method for real. Don't just iterate through every single
     * dataset! See https://redmine.hmdc.harvard.edu/issues/3988
     */
    public Dataset findByGlobalId(String globalId) {
        Dataset foundDataset = null;
        if (globalId != null) {
            Query query = em.createQuery("select object(o) from Dataset as o order by o.id");
            List<Dataset> datasets = query.getResultList();
            for (Dataset dataset : datasets) {
                if (globalId.equals(dataset.getGlobalId())) {
                    foundDataset = dataset;
                }
            }
        }
        return foundDataset;
    }

    public String generateIdentifierSequence(String protocol, String authority) {

        String identifier = null;
        do {
            identifier = ((Long) em.createNativeQuery("select nextval('dvobject_id_seq')").getSingleResult()).toString();

        } while (!isUniqueIdentifier(identifier, protocol, authority));

        return identifier;

    }

    /**
     * Check that a studyId entered by the user is unique (not currently used
     * for any other study in this Dataverse Network)
     */
    private boolean isUniqueIdentifier(String userIdentifier, String protocol, String authority) {
        String query = "SELECT d FROM Dataset d WHERE d.identifier = '" + userIdentifier + "'";
        query += " and d.protocol ='" + protocol + "'";
        query += " and d.authority = '" + authority + "'";
        boolean u = em.createQuery(query).getResultList().size() == 0;
        return u;
    }

    public String getRISFormat(DatasetVersion version) {
        String publisher = version.getRootDataverseNameforCitation();
        List<DatasetAuthor> authorList = version.getDatasetAuthors();
        String retString = "Provider: " + publisher + "\r\n";
        retString += "Content: text/plain; charset=\"us-ascii\"" + "\r\n";
        retString += "TY  - DATA" + "\r\n";
        retString += "T1  - " + version.getTitle() + "\r\n";
        for (DatasetAuthor author : authorList) {
            retString += "AU  - " + author.getName().getDisplayValue() + "\r\n";
        }
        retString += "DO  - " + version.getDataset().getProtocol() + "/" + version.getDataset().getAuthority() + "/" + version.getDataset().getId() + "\r\n";
        retString += "PY  - " + version.getVersionYear() + "\r\n";
        retString += "UR  - " + version.getDataset().getPersistentURL() + "\r\n";
        retString += "PB  - " + publisher + "\r\n";
        retString += "ER  - \r\n";
        return retString;
    }

    private XMLOutputFactory xmlOutputFactory = null;

    public void createXML(OutputStream os, DatasetVersion datasetVersion) {

        xmlOutputFactory = javax.xml.stream.XMLOutputFactory.newInstance();
        XMLStreamWriter xmlw = null;
        try {
            xmlw = xmlOutputFactory.createXMLStreamWriter(os);
            xmlw.writeStartDocument();
            createEndNoteXML(xmlw, datasetVersion);
            xmlw.writeEndDocument();
        } catch (XMLStreamException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred during creating endnote xml.", ex);
        } finally {
            try {
                if (xmlw != null) {
                    xmlw.close();
                }
            } catch (XMLStreamException ex) {
            }
        }
    }

    private void createEndNoteXML(XMLStreamWriter xmlw, DatasetVersion version) throws XMLStreamException {

        String title = version.getTitle();
        String versionYear = version.getVersionYear();
        String publisher = version.getRootDataverseNameforCitation();

        List<DatasetAuthor> authorList = version.getDatasetAuthors();

        xmlw.writeStartElement("xml");
        xmlw.writeStartElement("records");

        xmlw.writeStartElement("record");

        //<ref-type name="Dataset">59</ref-type> - How do I get that and xmlw.write it?
        xmlw.writeStartElement("ref-type");
        xmlw.writeAttribute("name","Dataset");
        xmlw.writeCharacters(version.getDataset().getId().toString());
        xmlw.writeEndElement(); // ref-type

        xmlw.writeStartElement("contributors");
        xmlw.writeStartElement("authors");
        for (DatasetAuthor author : authorList) {
            xmlw.writeStartElement("author");
            xmlw.writeCharacters(author.getName().getDisplayValue());
            xmlw.writeEndElement(); // author                    
        }
        xmlw.writeEndElement(); // authors 
        xmlw.writeEndElement(); // contributors 

        xmlw.writeStartElement("titles");
        xmlw.writeStartElement("title");
        xmlw.writeCharacters(title);
        xmlw.writeEndElement(); // title
        xmlw.writeEndElement(); // titles

        xmlw.writeStartElement("section");
        String sectionString ="";
        if (version.getDataset().isReleased()){
            sectionString = new SimpleDateFormat("yyyy-MM-dd").format(version.getDataset().getPublicationDate());
        } else {
            sectionString = new SimpleDateFormat("yyyy-MM-dd").format(version.getLastUpdateTime());
        }
        
        xmlw.writeCharacters(sectionString);
        xmlw.writeEndElement(); // publisher

        xmlw.writeStartElement("dates");
        xmlw.writeStartElement("year");
        xmlw.writeCharacters(versionYear);
        xmlw.writeEndElement(); // year
        xmlw.writeEndElement(); // dates

        xmlw.writeStartElement("publisher");
        xmlw.writeCharacters(publisher);
        xmlw.writeEndElement(); // publisher

        xmlw.writeStartElement("urls");
        xmlw.writeStartElement("related-urls");
        xmlw.writeStartElement("url");
        xmlw.writeCharacters(version.getDataset().getPersistentURL());
        xmlw.writeEndElement(); // url
        xmlw.writeEndElement(); // related-urls
        xmlw.writeEndElement(); // urls

        xmlw.writeStartElement("electronic-resource-num");
        String electResourceNum = version.getDataset().getProtocol() + "/" + version.getDataset().getAuthority() + "/" + version.getDataset().getId();
        xmlw.writeCharacters(electResourceNum);
        xmlw.writeEndElement();
        //<electronic-resource-num>10.3886/ICPSR03259.v1</electronic-resource-num>                  
        xmlw.writeEndElement(); // record

        xmlw.writeEndElement(); // records
        xmlw.writeEndElement(); // xml

    }

    public DatasetVersionDatasetUser getDatasetVersionDatasetUser(DatasetVersion version, DataverseUser user) {

        DatasetVersionDatasetUser ddu = null;
        Query query = em.createQuery("select object(o) from DatasetVersionDatasetUser as o "
                + "where o.datasetversionid =:versionId and o.dataverseuserid =:userId");
        query.setParameter("versionId", version.getId());
        query.setParameter("userId", user.getId());
        System.out.print("versionId: " + version.getId());
        System.out.print("userId: " + user.getId());
        System.out.print(query.toString());
        try {
            ddu = (DatasetVersionDatasetUser) query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return ddu;
    }

    public List<DatasetLock> getDatasetLocks() {
        String query = "SELECT sl FROM DatasetLock sl";
        return (List<DatasetLock>) em.createQuery(query).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addDatasetLock(Long datasetId, Long userId, String info) {

        Dataset dataset = em.find(Dataset.class, datasetId);
        DatasetLock lock = new DatasetLock();
        lock.setDataset(dataset);
        lock.setInfo(info);
        lock.setStartTime(new Date());

        if (userId != null) {
            DataverseUser user = em.find(DataverseUser.class, userId);
            lock.setUser(user);
            if (user.getDatasetLocks() == null) {
                user.setDatasetLocks(new ArrayList());
            }
            user.getDatasetLocks().add(lock);
        }

        dataset.setDatasetLock(lock);
        em.persist(lock);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeDatasetLock(Long datasetId) {
        Dataset dataset = em.find(Dataset.class, datasetId);
        //em.refresh(dataset); (?)
        DatasetLock lock = dataset.getDatasetLock();
        if (lock != null) {
            DataverseUser user = lock.getUser();
            dataset.setDatasetLock(null);
            user.getDatasetLocks().remove(lock);
            /* 
             * TODO - ?
             * throw an exception if for whatever reason we can't remove the lock?
             try {
             */
            em.remove(lock);
            /*
             } catch (TransactionRequiredException te) {
             ...
             } catch (IllegalArgumentException iae) {
             ...
             }
             */
        }
    }

    /*
     public Study getStudyByGlobalId(String identifier) {
     String protocol = null;
     String authority = null;
     String studyId = null;
     int index1 = identifier.indexOf(':');
     int index2 = identifier.indexOf('/');
     int index3 = 0;
     if (index1 == -1) {
     throw new EJBException("Error parsing identifier: " + identifier + ". ':' not found in string");
     } else {
     protocol = identifier.substring(0, index1);
     }
     if (index2 == -1) {
     throw new EJBException("Error parsing identifier: " + identifier + ". '/' not found in string");

     } else {
     authority = identifier.substring(index1 + 1, index2);
     }
     if (protocol.equals("doi")){
     index3 = identifier.indexOf('/', index2 + 1 );
     if (index3== -1){
     studyId = identifier.substring(index2 + 1).toUpperCase();  
     } else {
     authority = identifier.substring(index1 + 1, index3);
     studyId = identifier.substring(index3 + 1).toUpperCase();  
     }
     }  else {
     studyId = identifier.substring(index2 + 1).toUpperCase(); 
     }      

     String queryStr = "SELECT s from Study s where s.studyId = :studyId  and s.protocol= :protocol and s.authority= :authority";

     Study study = null;
     try {
     Query query = em.createQuery(queryStr);
     query.setParameter("studyId", studyId);
     query.setParameter("protocol", protocol);
     query.setParameter("authority", authority);
     study = (Study) query.getSingleResult();
     } catch (javax.persistence.NoResultException e) {
     // DO nothing, just return null.
     }
     return study;
     }
     */
}
