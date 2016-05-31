/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.persistence.TypedQuery;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.lang.RandomStringUtils;
import org.ocpsoft.common.util.Strings;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @EJB
    DOIEZIdServiceBean doiEZIdServiceBean;

    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    DatasetVersionServiceBean versionService;
    
    @EJB
    AuthenticationServiceBean authentication;
    
    @EJB
    DataFileServiceBean fileService; 
    
    @EJB
    PermissionServiceBean permissionService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Dataset find(Object pk) {
        return em.find(Dataset.class, pk);
    }

    public List<Dataset> findByOwnerId(Long ownerId) {
        return findByOwnerId(ownerId, false);
    }
    
    public List<Dataset> findPublishedByOwnerId(Long ownerId) {
        return findByOwnerId(ownerId, true);
    }    

    private List<Dataset> findByOwnerId(Long ownerId, boolean onlyPublished) {
        List<Dataset> retList = new ArrayList();
        TypedQuery<Dataset>  query = em.createQuery("select object(o) from Dataset as o where o.owner.id =:ownerId order by o.id", Dataset.class);
        query.setParameter("ownerId", ownerId);
        if (!onlyPublished) {
            return query.getResultList();
        } else {
            for (Dataset ds : query.getResultList()) {
                if (ds.isReleased() && !ds.isDeaccessioned()) {
                    retList.add(ds);
                }
            }
            return retList;
        }
    }

    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id").getResultList();
    }

    /**
     * For docs, see the equivalent method on the DataverseServiceBean.
     * @see DataverseServiceBean#findAllOrSubset(long, long, boolean)
     */     
    public List<Dataset> findAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed) {
        if (numPartitions < 1) {
            long saneNumPartitions = 1;
            numPartitions = saneNumPartitions;
        }
        String skipClause = skipIndexed ? "AND o.indexTime is null " : "";
        TypedQuery<Dataset> typedQuery = em.createQuery("SELECT OBJECT(o) FROM Dataset AS o WHERE MOD( o.id, :numPartitions) = :partitionId " +
                skipClause +
                "ORDER BY o.id", Dataset.class);
        typedQuery.setParameter("numPartitions", numPartitions);
        typedQuery.setParameter("partitionId", partitionId);
        return typedQuery.getResultList();
    }
    
    /**
     * Merges the passed dataset to the persistence context.
     * @param ds the dataset whose new state we want to persist.
     * @return The managed entity representing {@code ds}.
     */
    public Dataset merge( Dataset ds ) {
        return em.merge(ds);
    }
    
    public Dataset findByGlobalId(String globalId) {

        String protocol = "";
        String authority = "";
        String identifier = "";
        int index1 = globalId.indexOf(':');
        String nonNullDefaultIfKeyNotFound = ""; 
        // This is kind of wrong right here: we should not assume that this is *our* DOI - 
        // it can be somebody else's registered DOI that we harvested. And they can 
        // have their own separator characters defined - so we should not assume 
        // that everybody's DOIs will look like ours! 
        // Also, this separator character gets applied to handles lookups too, below. 
        // Which is probably wrong too...
        // -- L.A. 4.2.4
        String separator = settingsService.getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);        
        int index2 = globalId.indexOf(separator, index1 + 1);
        int index3 = 0;
        if (index1 == -1) {            
            logger.info("Error parsing identifier: " + globalId + ". ':' not found in string");
            return null;
        } else {
            protocol = globalId.substring(0, index1);
        }
        if (index2 == -1 ) {
            logger.info("Error parsing identifier: " + globalId + ". Second separator not found in string");
            return null;
        } else {
            authority = globalId.substring(index1 + 1, index2);
        }
        if (protocol.equals("doi")) {

            index3 = globalId.indexOf(separator, index2 + 1);
            if (index3 == -1 ) {
                // As of now (4.2.4, Feb. 2016) the ICPSR DOIs are the only 
                // use case where the authority has no "shoulder", so there's only 
                // 1 slash in the full global id string... hence, we get here. 
                // Their DOIs also have some lower case characters (for ex., 
                // 10.3886/ICPSR04599.v1), and that's how are they saved in the 
                // IQSS production database. So the .toUpperCase() below is 
                // causing a problem. -- L.A. 
                identifier = globalId.substring(index2 + 1); //.toUpperCase();
            } else {
                if (index3 > -1) {
                    authority = globalId.substring(index1 + 1, index3);
                    identifier = globalId.substring(index3 + 1).toUpperCase();
                }
            }
        } else {
            identifier = globalId.substring(index2 + 1).toUpperCase();
        }
        String queryStr = "SELECT s from Dataset s where s.identifier = :identifier  and s.protocol= :protocol and s.authority= :authority";
        Dataset foundDataset = null;
        try {
            Query query = em.createQuery(queryStr);
            query.setParameter("identifier", identifier);
            query.setParameter("protocol", protocol);
            query.setParameter("authority", authority);
            foundDataset = (Dataset) query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            logger.info("no ds found: " + globalId);
            // DO nothing, just return null.
        }
        return foundDataset;
    }

    public String generateIdentifierSequence(String protocol, String authority, String separator) {

        String identifier = null;
        do {
            identifier = RandomStringUtils.randomAlphanumeric(6).toUpperCase();  
        } while (!isUniqueIdentifier(identifier, protocol, authority, separator));

        return identifier;
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network) alos check for duplicate
     * in EZID if needed
     */
    public boolean isUniqueIdentifier(String userIdentifier, String protocol, String authority, String separator) {
        String query = "SELECT d FROM Dataset d WHERE d.identifier = '" + userIdentifier + "'";
        query += " and d.protocol ='" + protocol + "'";
        query += " and d.authority = '" + authority + "'";
        boolean u = em.createQuery(query).getResultList().size() == 0;
        String nonNullDefaultIfKeyNotFound = "";
        String doiProvider = settingsService.getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        if (doiProvider.equals("EZID")) {
            if (!doiEZIdServiceBean.lookupMetadataFromIdentifier(protocol, authority, separator, userIdentifier).isEmpty()) {
                u = false;
            }
        }
        return u;
    }

    public String createCitationRIS(DatasetVersion version) {
        return createCitationRIS(version, null);
    } 
    
    public String createCitationRIS(DatasetVersion version, FileMetadata fileMetadata) {
        String publisher = version.getRootDataverseNameforCitation();
        List<DatasetAuthor> authorList = version.getDatasetAuthors();
        String retString = "Provider: " + publisher + "\r\n";
        retString += "Content: text/plain; charset=\"us-ascii\"" + "\r\n";
        // Using type "DBASE" - "Online Database", for consistency with 
        // EndNote (see the longer comment in the EndNote section below)> 
        
        retString += "TY  - DBASE" + "\r\n";
        retString += "T1  - " + version.getTitle() + "\r\n";
        for (DatasetAuthor author : authorList) {
            retString += "AU  - " + author.getName().getDisplayValue() + "\r\n";
        }
        retString += "DO  - " + version.getDataset().getProtocol() + "/" + version.getDataset().getAuthority() + version.getDataset().getDoiSeparator() + version.getDataset().getIdentifier() + "\r\n";
        retString += "PY  - " + version.getVersionYear() + "\r\n";
        retString += "UR  - " + version.getDataset().getPersistentURL() + "\r\n";
        retString += "PB  - " + publisher + "\r\n";
        
        // a DataFile citation also includes filename und UNF, if applicable:
        if (fileMetadata != null) { 
            retString += "C1  - " + fileMetadata.getLabel() + "\r\n";
            
            if (fileMetadata.getDataFile().isTabularData()) {
                if (fileMetadata.getDataFile().getUnf() != null) {
                    retString += "C2  - " + fileMetadata.getDataFile().getUnf() + "\r\n";
                }
            }
        }
        
        // closing element: 
        retString += "ER  - \r\n";

        return retString;
    }


    private XMLOutputFactory xmlOutputFactory = null;

    public String createCitationXML(DatasetVersion datasetVersion, FileMetadata fileMetadata) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        createEndNoteCitation(outStream, datasetVersion, fileMetadata);
        String xml = outStream.toString();
        return xml; 
    } 
    
    public void createEndNoteCitation(OutputStream os, DatasetVersion datasetVersion, FileMetadata fileMetadata) {

        xmlOutputFactory = javax.xml.stream.XMLOutputFactory.newInstance();
        XMLStreamWriter xmlw = null;
        try {
            xmlw = xmlOutputFactory.createXMLStreamWriter(os);
            xmlw.writeStartDocument();
            createEndNoteXML(xmlw, datasetVersion, fileMetadata);
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
    
    private void createEndNoteXML(XMLStreamWriter xmlw, DatasetVersion version, FileMetadata fileMetadata) throws XMLStreamException {

        String title = version.getTitle();
        String versionYear = version.getVersionYear();
        String publisher = version.getRootDataverseNameforCitation();

        List<DatasetAuthor> authorList = version.getDatasetAuthors();

        xmlw.writeStartElement("xml");
        xmlw.writeStartElement("records");

        xmlw.writeStartElement("record");

        // "Ref-type" indicates which of the (numerous!) available EndNote
        // schemas this record will be interpreted as. 
        // This is relatively important. Certain fields with generic 
        // names like "custom1" and "custom2" become very specific things
        // in specific schemas; for example, custom1 shows as "legal notice"
        // in "Journal Article" (ref-type 84), or as "year published" in 
        // "Government Document". 
        // We don't want the UNF to show as a "legal notice"! 
        // We have found a ref-type that works ok for our purposes - 
        // "Online Database" (type 45). In this one, the fields Custom1 
        // and Custom2 are not translated and just show as is. 
        // And "Custom1" still beats "legal notice". 
        // -- L.A. 12.12.2014 beta 10
        
        xmlw.writeStartElement("ref-type");
        xmlw.writeAttribute("name", "Online Database");
        xmlw.writeCharacters("45");
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
        String sectionString = "";
        if (version.getDataset().isReleased()) {
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
        
        // a DataFile citation also includes the filename and (for Tabular
        // files) the UNF signature, that we put into the custom1 and custom2 
        // fields respectively:
        
        
        if (fileMetadata != null) {
            xmlw.writeStartElement("custom1");
            xmlw.writeCharacters(fileMetadata.getLabel());
            xmlw.writeEndElement(); // custom1
            
            if (fileMetadata.getDataFile().isTabularData()) {
                if (fileMetadata.getDataFile().getUnf() != null) {
                    xmlw.writeStartElement("custom2");
                    xmlw.writeCharacters(fileMetadata.getDataFile().getUnf());
                    xmlw.writeEndElement(); // custom2
                }
            }
        }

        xmlw.writeStartElement("electronic-resource-num");
        String electResourceNum = version.getDataset().getProtocol() + "/" + version.getDataset().getAuthority() + version.getDataset().getDoiSeparator() + version.getDataset().getIdentifier();
        xmlw.writeCharacters(electResourceNum);
        xmlw.writeEndElement();
        //<electronic-resource-num>10.3886/ICPSR03259.v1</electronic-resource-num>                  
        xmlw.writeEndElement(); // record

        xmlw.writeEndElement(); // records
        xmlw.writeEndElement(); // xml

    }

    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {

        DatasetVersionUser ddu = null;
        Query query = em.createQuery("select object(o) from DatasetVersionUser as o "
                + "where o.datasetVersion.id =:versionId and o.authenticatedUser.id =:userId");
        query.setParameter("versionId", version.getId());
        String identifier = user.getIdentifier();
        identifier = identifier.startsWith("@") ? identifier.substring(1) : identifier;
        AuthenticatedUser au = authentication.getAuthenticatedUser(identifier);
        query.setParameter("userId", au.getId());
        try {
            ddu = (DatasetVersionUser) query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return ddu;
    }

    public List<DatasetLock> getDatasetLocks() {
        String query = "SELECT sl FROM DatasetLock sl";
        return (List<DatasetLock>) em.createQuery(query).getResultList();
    }

    public boolean checkDatasetLock(Long datasetId) {
        String nativeQuery = "SELECT sl.id FROM DatasetLock sl WHERE sl.dataset_id = " + datasetId + " LIMIT 1;";
        Integer lockId = null; 
        try {
            lockId = (Integer)em.createNativeQuery(nativeQuery).getSingleResult();
        } catch (Exception ex) {
            lockId = null; 
        }
        
        if (lockId != null) {
            return true;
        }
        
        return false;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addDatasetLock(Long datasetId, Long userId, String info) {

        Dataset dataset = em.find(Dataset.class, datasetId);
        DatasetLock lock = new DatasetLock();
        lock.setDataset(dataset);
        lock.setInfo(info);
        lock.setStartTime(new Date());

        if (userId != null) {
            AuthenticatedUser user = em.find(AuthenticatedUser.class, userId);
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
            AuthenticatedUser user = lock.getUser();
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
    getTitleFromLatestVersion methods use native query to return a dataset title
    
        There are two versions:
     1) The version with datasetId param only will return the title regardless of version state
     2)The version with the param 'includeDraft' boolean  will return the most recently published title if the param is set to false
    If no Title found return empty string - protects against calling with
    include draft = false with no published version
    */
    
    public String getTitleFromLatestVersion(Long datasetId){
        return getTitleFromLatestVersion(datasetId, true);
    }
    
    public String getTitleFromLatestVersion(Long datasetId, boolean includeDraft){

        String whereDraft = "";
        //This clause will exclude draft versions from the select
        if (!includeDraft) {
            whereDraft = " and v.versionstate !='DRAFT' ";
        }
        
        try {
            return (String) em.createNativeQuery("select dfv.value  from dataset d "
                + " join datasetversion v on d.id = v.dataset_id "
                + " join datasetfield df on v.id = df.datasetversion_id "
                + " join datasetfieldvalue dfv on df.id = dfv.datasetfield_id "
                + " join datasetfieldtype dft on df.datasetfieldtype_id  = dft.id "
                + " where dft.name = '" + DatasetFieldConstant.title + "' and  v.dataset_id =" + datasetId
                + whereDraft
                + " order by v.versionnumber desc, v.minorVersionNumber desc limit 1 "
                + ";").getSingleResult();

        } catch (Exception ex) {
            logger.info("exception trying to get title from latest version: " + ex);
            return "";
        }

    }
    
    public Long getDatasetVersionCardImage(Long versionId, User user) {
        if (versionId == null) {
            return null;
        }
        
        
        
        return null;
    }
    
    /**
     * Used to identify and properly display Harvested objects on the dataverse page.
     * 
     * @return 
     */
    public Map<Long, String> getHarvestingDescriptionsForHarvestedDatasets(Set<Long> datasetIds){
        if (datasetIds == null || datasetIds.size() < 1) {
            return null;
        }
        
        String datasetIdStr = Strings.join(datasetIds, ", ");
        
        String qstr = "SELECT d.id, h.archiveDescription FROM harvestingDataverseConfig h, dataset d, dvobject o WHERE d.id = o.id AND h.dataverse_id = o.owner_id AND d.id IN (" + datasetIdStr + ")";
        List<Object[]> searchResults = null;
        
        try {
            searchResults = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            searchResults = null;
        }
        
        if (searchResults == null) {
            return null;
        }
        
        Map<Long, String> ret = new HashMap<>();
        
        for (Object[] result : searchResults) {
            Long dsId = null;
            if (result[0] != null) {
                try {
                    dsId = (Long)result[0];
                } catch (Exception ex) {
                    dsId = null;
                }
                if (dsId == null) {
                    continue;
                }
                
                ret.put(dsId, (String)result[1]);
            }
        }
        
        return ret;        
    }
    
    
    
    public boolean isDatasetCardImageAvailable(DatasetVersion datasetVersion, User user) {        
        if (datasetVersion == null) {
            return false; 
        }
                
        // First, check if this dataset has a designated thumbnail image: 
        
        if (datasetVersion.getDataset() != null) {
            DataFile dataFile = datasetVersion.getDataset().getThumbnailFile();
            if (dataFile != null) {
                return ImageThumbConverter.isThumbnailAvailable(dataFile, 48);
            }
        }
        
        // If not, we'll try to use one of the files in this dataset version:
        // (the first file with an available thumbnail, really)
        
        List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();

        for (FileMetadata fileMetadata : fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            
            // TODO: use permissionsWrapper here - ? 
            // (we are looking up these download permissions on individual files, 
            // true, and those are unique... but the wrapper may be able to save 
            // us some queries when it determines the download permission on the
            // dataset as a whole? -- L.A. 4.2.1
            
            if (fileService.isThumbnailAvailable(dataFile) && permissionService.userOn(user, dataFile).has(Permission.DownloadFile)) { //, user)) {
                return true;
            }
 
        }
        
        return false;
    }
}
