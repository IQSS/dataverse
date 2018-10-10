/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.AbstractGlobalIdServiceBean.GlobalIdMetadataTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author luopc
 */
@Stateless
public class DOIDataCiteRegisterService {

    private static final Logger logger = Logger.getLogger(DOIDataCiteRegisterService.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    private DOIDataCiteServiceBean doiDataCiteServiceBean;
    
    private DataCiteRESTfullClient openClient() throws IOException {
        return new DataCiteRESTfullClient(System.getProperty("doi.baseurlstring"), System.getProperty("doi.username"), System.getProperty("doi.password"));
    }

    public String createIdentifierLocal(String identifier, Map<String, String> metadata, DvObject dvObject) {

        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        String status = metadata.get("_status").trim();
        String target = metadata.get("_target");
        String retString = "";
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        if (rc == null) {
            rc = new DOIDataCiteRegisterCache();
            rc.setDoi(identifier);
            rc.setXml(xmlMetadata);
            rc.setStatus("reserved");
            rc.setUrl(target);
            em.persist(rc);
        } else {
            rc.setDoi(identifier);
            rc.setXml(xmlMetadata);
            rc.setStatus("reserved");
            rc.setUrl(target);
        }
        retString = "success to reserved " + identifier;

        return retString;
    }

    public String registerIdentifier(String identifier, Map<String, String> metadata, DvObject dvObject) throws IOException {
        String retString = "";
        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        String target = metadata.get("_target");
        if (rc != null) {
            rc.setDoi(identifier);
            rc.setXml(xmlMetadata);
            rc.setStatus("public");
            if (target == null || target.trim().length() == 0) {
                target = rc.getUrl();
            } else {
                rc.setUrl(target);
            }
            try (DataCiteRESTfullClient client = openClient()) {
                retString = client.postMetadata(xmlMetadata);
                client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(DOIDataCiteRegisterService.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try (DataCiteRESTfullClient client = openClient()) {
                retString = client.postMetadata(xmlMetadata);
                client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(DOIDataCiteRegisterService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return retString;
    }

    public String deactivateIdentifier(String identifier, HashMap<String, String> metadata, DvObject dvObject) {
        String retString = "";
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        try (DataCiteRESTfullClient client = openClient()) {
            if (rc != null) {
                rc.setStatus("unavailable");
                retString = client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));
            }
        } catch (IOException io) {

        }
        return retString;
    }

    private String getMetadataFromDvObject(String identifier, Map<String, String> metadata, DvObject dvObject) {

        Dataset dataset = null;

        if (dvObject instanceof Dataset) {
            dataset = (Dataset) dvObject;
        } else {
            dataset = (Dataset) dvObject.getOwner();
        }
        
        DOIDataCiteServiceBean.GlobalIdMetadataTemplate metadataTemplate = doiDataCiteServiceBean.new GlobalIdMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setCreators(Util.getListFromStr(metadata.get("datacite.creator")));
        metadataTemplate.setAuthors(dataset.getLatestVersion().getDatasetAuthors());
        if (dvObject.isInstanceofDataset()) {
            metadataTemplate.setDescription(dataset.getLatestVersion().getDescriptionPlainText());
        }
        if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            String fileDescription = df.getDescription();
            metadataTemplate.setDescription(fileDescription == null ? "" : fileDescription);
            String datasetPid = df.getOwner().getGlobalId().asString();
            metadataTemplate.setDatasetIdentifier(datasetPid);
        } else {
            metadataTemplate.setDatasetIdentifier("");
        }

        metadataTemplate.setContacts(dataset.getLatestVersion().getDatasetContacts());
        metadataTemplate.setProducers(dataset.getLatestVersion().getDatasetProducers());
        metadataTemplate.setTitle(dvObject.getDisplayName());
        String producerString = dataverseService.findRootDataverse().getName();
        if (producerString.isEmpty()) {
            producerString = ":unav";
        }
        metadataTemplate.setPublisher(producerString);
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = metadataTemplate.generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    public String modifyIdentifier(String identifier, HashMap<String, String> metadata, DvObject dvObject) throws IOException {

        String xmlMetadata = getMetadataFromDvObject(identifier, metadata, dvObject);

        logger.fine("XML to send to DataCite: " + xmlMetadata);

        String status = metadata.get("_status").trim();
        String target = metadata.get("_target");
        String retString = "";
        if (status.equals("reserved")) {
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if (rc == null) {
                rc = new DOIDataCiteRegisterCache();
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("reserved");
                rc.setUrl(target);
                em.persist(rc);
            } else {
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("reserved");
                rc.setUrl(target);
            }
            retString = "success to reserved " + identifier;
        } else if (status.equals("public")) {
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if (rc != null) {
                rc.setDoi(identifier);
                rc.setXml(xmlMetadata);
                rc.setStatus("public");
                if (target == null || target.trim().length() == 0) {
                    target = rc.getUrl();
                } else {
                    rc.setUrl(target);
                }
                try (DataCiteRESTfullClient client = openClient()) {
                    retString = client.postMetadata(xmlMetadata);
                    client.postUrl(identifier.substring(identifier.indexOf(":") + 1), target);

                } catch (UnsupportedEncodingException ex) {
                    logger.log(Level.SEVERE, null, ex);

                } catch (RuntimeException rte) {
                    logger.log(Level.SEVERE, "Error creating DOI at DataCite: {0}", rte.getMessage());
                    logger.log(Level.SEVERE, "Exception", rte);

                }
            }
        } else if (status.equals("unavailable")) {
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            try (DataCiteRESTfullClient client = openClient()) {
                if (rc != null) {
                    rc.setStatus("unavailable");
                    retString = client.inactiveDataset(identifier.substring(identifier.indexOf(":") + 1));
                }
            } catch (IOException io) {

            }
        }
        return retString;
    }

    public boolean testDOIExists(String identifier) {
        boolean doiExists;
        try (DataCiteRESTfullClient client = openClient()) {
            doiExists = client.testDOIExists(identifier.substring(identifier.indexOf(":") + 1));
        } catch (Exception e) {
            logger.log(Level.INFO, identifier, e);
            return false;
        }
        return doiExists;
    }

    public HashMap<String, String> getMetadata(String identifier) throws IOException {
        HashMap<String, String> metadata = new HashMap<>();
        try (DataCiteRESTfullClient client = openClient()) {
            String xmlMetadata = client.getMetadata(identifier.substring(identifier.indexOf(":") + 1));
            DOIDataCiteServiceBean.GlobalIdMetadataTemplate template = doiDataCiteServiceBean.new GlobalIdMetadataTemplate(xmlMetadata);
            metadata.put("datacite.creator", Util.getStrFromList(template.getCreators()));
            metadata.put("datacite.title", template.getTitle());
            metadata.put("datacite.publisher", template.getPublisher());
            metadata.put("datacite.publicationyear", template.getPublisherYear());
            DOIDataCiteRegisterCache rc = findByDOI(identifier);
            if (rc != null) {
                metadata.put("_status", rc.getStatus());
            }
        } catch (RuntimeException e) {
            logger.log(Level.INFO, identifier, e);
        }
        return metadata;
    }

    public DOIDataCiteRegisterCache findByDOI(String doi) {
        TypedQuery<DOIDataCiteRegisterCache> query = em.createNamedQuery("DOIDataCiteRegisterCache.findByDoi",
                DOIDataCiteRegisterCache.class);
        query.setParameter("doi", doi);
        List<DOIDataCiteRegisterCache> rc = query.getResultList();
        if (rc.size() == 1) {
            return rc.get(0);
        }
        return null;
    }

    public void deleteIdentifier(String identifier) {
        DOIDataCiteRegisterCache rc = findByDOI(identifier);
        if (rc != null) {
            em.remove(rc);
        }
    }

}

class Util {

    public static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("Fail to close InputStream");
            }
        }
    }

    public static String readAndClose(InputStream inStream, String encoding) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buf = new byte[128];
        String data;
        try {
            int cnt;
            while ((cnt = inStream.read(buf)) >= 0) {
                outStream.write(buf, 0, cnt);
            }
            data = outStream.toString(encoding);
        } catch (IOException ioe) {
            throw new RuntimeException("IOException");
        } finally {
            close(inStream);
        }
        return data;
    }

    public static List<String> getListFromStr(String str) {
        return Arrays.asList(str.split("; "));
//        List<String> authors = new ArrayList();
//        int preIdx = 0;
//        for(int i=0;i<str.length();i++){
//            if(str.charAt(i)==';'){
//                authors.add(str.substring(preIdx,i).trim());
//                preIdx = i+1;
//            }
//        }
//        return authors;
    }

    public static String getStrFromList(List<String> authors) {
        StringBuilder str = new StringBuilder();
        for (String author : authors) {
            if (str.length() > 0) {
                str.append("; ");
            }
            str.append(author);
        }
        return str.toString();
    }
}
