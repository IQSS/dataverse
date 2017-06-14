package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author gdurand
 */
@Stateless
@Named
public class DataverseServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataverseLinkingServiceBean dataverseLinkingService;

    @EJB
    DatasetLinkingServiceBean datasetLinkingService;
    
    @EJB
    SystemConfig systemConfig;

    @Inject
    DataverseSession session;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Dataverse save(Dataverse dataverse) {
       
        dataverse.setModificationTime(new Timestamp(new Date().getTime()));
        Dataverse savedDataverse = em.merge(dataverse);
        /**
         * @todo check the result to see if indexing was successful or not
         */
        Future<String> indexingResult = indexService.indexDataverse(savedDataverse);
        return savedDataverse;
    }

    public Dataverse find(Object pk) {
        return (Dataverse) em.find(Dataverse.class, pk);
    }

    public List<Dataverse> findAll() {
        return em.createQuery("select object(o) from Dataverse as o order by o.name").getResultList();
    }

    /**
     * @param numPartitions The number of partitions you intend to split the
     * indexing job into. Perhaps you have three Glassfish servers and you'd
     * like each one to operate on a subset of dataverses.
     *
     * @param partitionId Maybe "partitionId" is the wrong term but it's what we
     * call in the (text) UI. If you've specified three partitions the three
     * partitionIds are 0, 1, and 2. We do `dataverseId % numPartitions =
     * partitionId` to figure out which partition the dataverseId falls into.
     * 
     * @param skipIndexed If true, will skip any dvObjects that have a indexTime set 
     *
     * @return All dataverses if you say numPartitions=1 and partitionId=0.
     * Otherwise, a subset of dataverses.
     */
    public List<Dataverse> findAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed) {
        if (numPartitions < 1) {
            long saneNumPartitions = 1;
            numPartitions = saneNumPartitions;
        }
        String skipClause = skipIndexed ? "AND o.indexTime is null " : "";
        TypedQuery<Dataverse> typedQuery = em.createQuery("SELECT OBJECT(o) FROM Dataverse AS o WHERE MOD( o.id, :numPartitions) = :partitionId " +
                skipClause +
                "ORDER BY o.id", Dataverse.class);
        typedQuery.setParameter("numPartitions", numPartitions);
        typedQuery.setParameter("partitionId", partitionId);
        return typedQuery.getResultList();
    }

    public List<Dataverse> findByOwnerId(Long ownerId) {
        return em.createNamedQuery("Dataverse.findByOwnerId")
                 .setParameter("ownerId", ownerId)
                 .getResultList();
    }
    
    public List<Dataverse> findPublishedByOwnerId(Long ownerId) {
        return em.createNamedQuery("Dataverse.findPublishedByOwnerId")
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    /**
     * @return 
     * @todo Do we really want this method to sometimes throw a
     * NoResultException which is a RuntimeException?
     */
    public Dataverse findRootDataverse() {
        return (Dataverse) em.createNamedQuery("Dataverse.findRoot").getSingleResult();
    }
    
    public List<Dataverse> findAllPublishedByOwnerId(Long ownerId) {
        List<Dataverse> retVal = new ArrayList();       
        List<Dataverse> previousLevel = findPublishedByOwnerId(ownerId);
        
        retVal.addAll(previousLevel);
        /*
        if (!previousLevel.isEmpty()) {
            for (Dataverse dv : previousLevel) {
                retVal.addAll(findPublishedByOwnerId(dv.getId()));
            }
        }*/
        return retVal;
    }

    /**
     * A lookup of a dataverse alias should be case insensitive. If "cfa"
     * belongs to the Center for Astrophysics, we don't want to allow Code for
     * America to start using "CFA". Force all queries to be lower case.
     * @param anAlias Alias of teh sought dataverse
     * @return The dataverse, or {@code null}.
     */
    public Dataverse findByAlias(String anAlias) {
        try {
            return (anAlias.toLowerCase().equals(":root"))
				? findRootDataverse()
				: em.createNamedQuery("Dataverse.findByAlias", Dataverse.class)
					.setParameter("alias", anAlias.toLowerCase())
					.getSingleResult();
        } catch ( NoResultException|NonUniqueResultException ex ) {
            logger.log(Level.FINE, "Unable to find a single dataverse using alias \"{0}\": {1}", new Object[]{anAlias, ex});
            return null;
        }
    }
	
	public boolean hasData( Dataverse dv ) {
		TypedQuery<Long> amountQry = em.createNamedQuery("Dataverse.ownedObjectsById", Long.class)
								.setParameter("id", dv.getId());
		
		return (amountQry.getSingleResult()>0);
	}
	
    public boolean isRootDataverseExists() {
        long count = em.createNamedQuery("Dataverse.countRoot", Long.class).getSingleResult();
        return (count == 1);
    }

    public String determineDataversePath(Dataverse dataverse) {
        List<String> dataversePathSegments = new ArrayList();
        indexService.findPathSegments(dataverse, dataversePathSegments);
        StringBuilder dataversePath = new StringBuilder();
        for (String segment : dataversePathSegments) {
            dataversePath.append("/").append(segment);
        }
        return dataversePath.toString();
    }

    public MetadataBlock findMDB(Long id) {
        return (MetadataBlock) em.find(MetadataBlock.class, id);
    }

    public MetadataBlock findMDBByName(String name) {
        return em.createNamedQuery("MetadataBlock.findByName", MetadataBlock.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    public List<MetadataBlock> findAllMetadataBlocks() {
        return em.createNamedQuery("MetadataBlock.listAll").getResultList();
    }
    
    public List<MetadataBlock> findSystemMetadataBlocks(){
        return em.createNamedQuery("MetadataBlock.findSystemBlocks").getResultList();
    }
    
    public List<MetadataBlock> findMetadataBlocksByDataverseId(Long dataverse_id) {
        return em.createNamedQuery("MetadataBlock.findSystemDataverseId")
                .setParameter("dataverseId", dataverse_id).getResultList();
    }
    
    public DataverseFacet findFacet(Long id) {
        return (DataverseFacet) em.find(DataverseFacet.class, id);
    }
    
    public String getDataverseLogoThumbnailAsBase64(Dataverse dataverse, User user) {
        
        if (dataverse == null) {
            return null;
        }

        File dataverseLogoFile = getLogo(dataverse);
        if (dataverseLogoFile != null) {
            String logoThumbNailPath = null;

            if (dataverseLogoFile.exists()) {
                logoThumbNailPath = ImageThumbConverter.generateImageThumbnailFromFile(dataverseLogoFile.getAbsolutePath(), 48);
                if (logoThumbNailPath != null) {
                    return ImageThumbConverter.getImageAsBase64FromFile(new File(logoThumbNailPath));

                }
            }
        } 
        return null;
    }
    
    public String getDataverseLogoThumbnailAsBase64ById(Long dvId) {
     
        File dataverseLogoFile = getLogoById(dvId);
        
        if (dataverseLogoFile != null) {
            String logoThumbNailPath = null;

            if (dataverseLogoFile.exists()) {
                logoThumbNailPath = ImageThumbConverter.generateImageThumbnailFromFile(dataverseLogoFile.getAbsolutePath(), 48);
                if (logoThumbNailPath != null) {
                    return ImageThumbConverter.getImageAsBase64FromFile(new File(logoThumbNailPath));

                }
            }
        } 
        return null;
    }
    
    private File getLogo(Dataverse dataverse) {
        if (dataverse.getId() == null) {
            return null; 
        }
        
        DataverseTheme theme = dataverse.getDataverseTheme(); 
        if (theme != null && theme.getLogo() != null && !theme.getLogo().equals("")) {
            Properties p = System.getProperties();
            String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
  
            if (domainRoot != null && !"".equals(domainRoot)) {
                return new File (domainRoot + File.separator + 
                    "docroot" + File.separator + 
                    "logos" + File.separator + 
                    dataverse.getLogoOwnerId() + File.separator + 
                    theme.getLogo());
            }
        }
            
        return null;         
    }
    
    private File getLogoById(Long id) {
        if (id == null) {
            return null; 
        }
        
        String logoFileName;
        
        try {
           logoFileName = em.createNamedQuery("DataverseTheme.findLogoByDataverseId", String.class)
                            .setParameter("dataverseId", id).getSingleResult();
            
        } catch (Exception ex) {
            return null;
        }
        
        if (logoFileName != null && !logoFileName.equals("")) {
            Properties p = System.getProperties();
            String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
  
            if (domainRoot != null && !"".equals(domainRoot)) {
                return new File (domainRoot + File.separator + 
                    "docroot" + File.separator + 
                    "logos" + File.separator + 
                    id + File.separator + 
                    logoFileName);
            }
        }
            
        return null;         
    }
    
    public DataverseTheme findDataverseThemeByIdQuick(Long id) {
        if (id == null) {
            return null; 
        }
        
        Object[] result = null;
        
        try {
            DataverseTheme dvt = em.createNamedQuery("DataverseTheme.findByDataverseId", DataverseTheme.class)
                                   .setParameter("dataverseId", id)
                                   .getSingleResult();
            result = new Object[]{dvt.getLogo(), dvt.getLogoFormat()};
            
        } catch (Exception ex) {
            return null;
        }
        
        if (result == null) {
            return null;
        }
        
        DataverseTheme theme = new DataverseTheme();
        
        if (result[0] != null) {
            theme.setLogo((String) result[0]);
        }

        if (result[1] != null) {
            String format = (String) result[1];
            if ("RECTANGLE".equals(format)) {
                theme.setLogoFormat(DataverseTheme.ImageFormat.RECTANGLE);
            } else if ("SQUARE".equals(format)) {
                theme.setLogoFormat(DataverseTheme.ImageFormat.SQUARE);
            }
        }
        
        return theme;
    }

    public List<Dataverse> findDataversesThisIdHasLinkedTo(long dataverseId) {
        return dataverseLinkingService.findLinkedDataverses(dataverseId);
    }

    public List<Dataverse> findDataversesThatLinkToThisDvId(long dataverseId) {
        return dataverseLinkingService.findLinkingDataverses(dataverseId);
    }

    public List<Dataset> findDatasetsThisIdHasLinkedTo(long dataverseId) {
        return datasetLinkingService.findDatasetsThisDataverseIdHasLinkedTo(dataverseId);
    }

    public List<Dataverse> findDataversesThatLinkToThisDatasetId(long datasetId) {
        return datasetLinkingService.findLinkingDataverses(datasetId);
    }
    
    public List<Dataverse> filterByAliasQuery(String filterQuery) {
        //Query query = em.createNativeQuery("select o from Dataverse o where o.alias LIKE '" + filterQuery + "%' order by o.alias");
        //Query query = em.createNamedQuery("Dataverse.filterByAlias", Dataverse.class).setParameter("alias", filterQuery.toLowerCase() + "%");
        Query query = em.createNamedQuery("Dataverse.filterByAliasNameAffiliation", Dataverse.class)
                .setParameter("alias", filterQuery.toLowerCase() + "%")
                .setParameter("name", "%" + filterQuery.toLowerCase() + "%")
                .setParameter("affiliation", "%" + filterQuery.toLowerCase() + "%");
        //logger.info("created native query: select o from Dataverse o where o.alias LIKE '" + filterQuery + "%' order by o.alias");
        logger.info("created named query");
        List<Dataverse> ret = query.getResultList();
        if (ret != null) {
            logger.info("results list: "+ret.size()+" results.");
        }
        return ret;
    }
    
    public void populateDvSearchCard(SolrSearchResult solrSearchResult) {
  
        Long dvId = solrSearchResult.getEntityId();
        
        if (dvId == null) {
            return;
        }
        
        Long parentDvId = null;
        String parentId = solrSearchResult.getParent().get("id");
        if (parentId != null) {
            try {
                parentDvId = Long.parseLong(parentId);
            } catch (Exception ex) {
                parentDvId = null;
            }
        }
        
        Object[] searchResult = null;
        
        try {
            if (parentDvId == null) {
                searchResult = (Object[]) em.createNativeQuery("SELECT t0.AFFILIATION, t0.ALIAS FROM DATAVERSE t0 WHERE t0.ID = " + dvId).getSingleResult();
            } else {
                searchResult = (Object[]) em.createNativeQuery("SELECT t0.AFFILIATION, t0.ALIAS, t2.ALIAS FROM DATAVERSE t0, DVOBJECT t1, DATAVERSE t2, DVOBJECT t3 WHERE (t0.ID = t1.ID) AND (t1.OWNER_ID = t3.ID) AND (t2.ID = t3.ID) AND (t0.ID = " + dvId + ")").getSingleResult();
            }
        } catch (Exception ex) {
            return;
        }

        if (searchResult == null) {
            return;
        }
        
        if (searchResult[0] != null) {
            solrSearchResult.setDataverseAffiliation((String) searchResult[0]);
        }

        if (searchResult[1] != null) {
            solrSearchResult.setDataverseAlias((String) searchResult[1]);
        }
        
        if (parentDvId != null) {
            if (searchResult[2] != null) {
                solrSearchResult.setDataverseParentAlias((String) searchResult[2]);
            }
        }
    }
}  
