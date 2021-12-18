package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataverse.DataverseLinkingService;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * @author gdurand
 */
@Stateless
@Named
public class DataverseDao implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseDao.class.getCanonicalName());
    @EJB
    private IndexServiceBean indexService;

    @EJB
    private AuthenticationServiceBean authService;

    @EJB
    private DatasetRepository datasetRepository;

    @EJB
    private DataverseLinkingService dataverseLinkingService;

    @EJB
    private DatasetLinkingServiceBean datasetLinkingService;

    @EJB
    private GroupServiceBean groupService;

    @EJB
    private DataverseRoleServiceBean rolesService;

    @EJB
    private PermissionServiceBean permissionService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private ImageThumbConverter imageThumbConverter;

    public Dataverse save(Dataverse dataverse) {

        dataverse.setModificationTime(new Timestamp(new Date().getTime()));
        Dataverse savedDataverse = em.merge(dataverse);
        /**
         * @todo check the result to see if indexing was successful or not
         */
        Future<String> indexingResult = indexService.indexDataverse(savedDataverse);
//        logger.log(Level.INFO, "during dataverse save, indexing result was: {0}", indexingResult);
        return savedDataverse;
    }

    public Dataverse find(Object pk) {
        return em.find(Dataverse.class, pk);
    }

    public List<Dataverse> findAll() {
        return em.createNamedQuery("Dataverse.findAll").getResultList();
    }

    /**
     * @param numPartitions The number of partitions you intend to split the
     *                      indexing job into. Perhaps you have three Glassfish servers and you'd
     *                      like each one to operate on a subset of dataverses.
     * @param partitionId   Maybe "partitionId" is the wrong term but it's what we
     *                      call in the (text) UI. If you've specified three partitions the three
     *                      partitionIds are 0, 1, and 2. We do `dataverseId % numPartitions =
     *                      partitionId` to figure out which partition the dataverseId falls into.
     * @param skipIndexed   If true, will skip any dvObjects that have a indexTime set
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

    public List<Long> findDataverseIdsForIndexing(boolean skipIndexed) {
        if (skipIndexed) {
            return em.createQuery("SELECT o.id FROM Dataverse o WHERE o.indexTime IS null ORDER BY o.id", Long.class).getResultList();
        }
        return em.createQuery("SELECT o.id FROM Dataverse o ORDER BY o.id", Long.class).getResultList();

    }

    public List<Dataverse> findByOwnerId(Long ownerId) {
        return em.createNamedQuery("Dataverse.findByOwnerId").setParameter("ownerId", ownerId).getResultList();
    }

    public List<Long> findIdsByOwnerId(Long ownerId) {
        String qr = "select o.id from Dataverse as o where o.owner.id =:ownerId order by o.id";
        return em.createQuery(qr, Long.class).setParameter("ownerId", ownerId).getResultList();
    }

    /**
     * @return the root dataverse
     * @todo Do we really want this method to sometimes throw a
     * NoResultException which is a RuntimeException?
     */
    public Dataverse findRootDataverse() {
        return em.createNamedQuery("Dataverse.findRoot", Dataverse.class).getSingleResult();
    }

    /**
     * A lookup of a dataverse alias should be case insensitive. If "cfa"
     * belongs to the Center for Astrophysics, we don't want to allow Code for
     * America to start using "CFA". Force all queries to be lower case.
     */
    public Dataverse findByAlias(String anAlias) {
        try {
            return (anAlias.equalsIgnoreCase(":root"))
                    ? findRootDataverse()
                    : em.createNamedQuery("Dataverse.findByAlias", Dataverse.class)
                    .setParameter("alias", anAlias.toLowerCase())
                    .getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.fine("Unable to find a single dataverse using alias \"" + anAlias + "\": " + ex);
            return null;
        }
    }

    public boolean hasData(Dataverse dv) {
        TypedQuery<Long> amountQry = em.createNamedQuery("Dataverse.ownedObjectsById", Long.class)
                .setParameter("id", dv.getId());

        return (amountQry.getSingleResult() > 0);
    }

    public boolean isRootDataverseExists() {
        long count = em.createQuery("SELECT count(dv) FROM Dataverse dv WHERE dv.owner.id=null", Long.class).getSingleResult();
        return (count == 1);
    }

    public String determineDataversePath(Dataverse dataverse) {
        List<String> dataversePathSegments = indexService.findPathSegments(dataverse);
        StringBuilder dataversePath = new StringBuilder();
        for (String segment : dataversePathSegments) {
            dataversePath.append("/").append(segment);
        }
        return dataversePath.toString();
    }

    public MetadataBlock findMDB(Long id) {
        return em.find(MetadataBlock.class, id);
    }

    public MetadataBlock findMDBByName(String name) {
        return em.createQuery("select m from MetadataBlock m WHERE m.name=:name", MetadataBlock.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    public List<MetadataBlock> findAllMetadataBlocks() {
        return em.createQuery("select object(o) from MetadataBlock as o order by o.id", MetadataBlock.class).getResultList();
    }

    public List<MetadataBlock> findSystemMetadataBlocks() {
        String qr = "select object(o) from MetadataBlock as o where o.owner.id=null  order by o.id";
        return em.createQuery(qr, MetadataBlock.class).getResultList();
    }

    public List<MetadataBlock> findMetadataBlocksByDataverseId(Long dataverse_id) {
        String qr = "select object(o) from MetadataBlock as o where o.owner.id=:dataverse_id order by o.id";
        return em.createQuery(qr, MetadataBlock.class)
                .setParameter("dataverse_id", dataverse_id).getResultList();
    }

    public String getDataverseLogoThumbnailFilePath(Long dvId) {

        File dataverseLogoFile = getLogoById(dvId);

        if (dataverseLogoFile != null) {
            String logoThumbNailPath = dataverseLogoFile + ".thumb" + 48;

            if (new File(logoThumbNailPath).exists()) {
                return logoThumbNailPath;
            } else {
                imageThumbConverter.generateImageThumbnailFromFile(dataverseLogoFile.getAbsolutePath(), 48, logoThumbNailPath);

                if (new File(logoThumbNailPath).exists()) {
                    return logoThumbNailPath;
                }
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
            logoFileName = (String) em.createNativeQuery("SELECT logo FROM dataversetheme WHERE dataverse_id = " + id).getSingleResult();

        } catch (Exception ex) {
            return null;
        }

        if (logoFileName != null && !logoFileName.isEmpty()) {
            Properties p = System.getProperties();
            String domainRoot = p.getProperty("com.sun.aas.instanceRoot");

            if (domainRoot != null && !"".equals(domainRoot)) {
                return new File(domainRoot + File.separator +
                                        "docroot" + File.separator +
                                        "logos" + File.separator +
                                        id + File.separator +
                                        logoFileName);
            }
        }

        return null;
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
        List<Dataverse> ret = em.createNamedQuery("Dataverse.filterByAliasNameAffiliation", Dataverse.class)
                .setParameter("alias", filterQuery.toLowerCase() + "%")
                .setParameter("name", "%" + filterQuery.toLowerCase() + "%")
                .setParameter("affiliation", "%" + filterQuery.toLowerCase() + "%").getResultList();
        //logger.info("created native query: select o from Dataverse o where o.alias LIKE '" + filterQuery + "%' order by o.alias");
        logger.info("created named query");
        if (ret != null) {
            logger.info("results list: " + ret.size() + " results.");
        }
        return ret;
    }

    public List<Dataverse> filterDataversesForLinking(String query, DataverseRequest req, Dataset dataset) {

        List<Dataverse> dataverseList = new ArrayList<>();

        List<Dataverse> results = em.createNamedQuery("Dataverse.filterByName", Dataverse.class)
                .setParameter("name", "%" + query.toLowerCase() + "%")
                .getResultList();

        List<Object> alreadyLinkeddv_ids = em.createNativeQuery(
                "SELECT linkingdataverse_id   FROM datasetlinkingdataverse WHERE dataset_id = " + dataset.getId())
                .getResultList();
        List<Dataverse> toRemove = new ArrayList<>();

        if (alreadyLinkeddv_ids != null && !alreadyLinkeddv_ids.isEmpty()) {
            alreadyLinkeddv_ids.stream().map(this::find).forEachOrdered(toRemove::add);
        }

        for (Dataverse res : results) {
            if (!toRemove.contains(res)) {
                if (permissionService.requestOn(req, res).has(Permission.PublishDataset)) {
                    dataverseList.add(res);
                }
            }
        }

        return dataverseList;
    }

    public Long countDataverses() {
        TypedQuery<Long> countQuery = em.createQuery("SELECT count(dv) FROM Dataverse dv", Long.class);
        return countQuery.getSingleResult();
    }

    public List<Object[]> getParentAliasesForIds(List<Long> ids) {
        return em.createQuery("SELECT o.id, dv.alias FROM Dataverse dv, DvObject o " +
                "WHERE dv.id = o.owner.id AND o.id IN :ids", Object[].class)
                .setParameter("ids", ids)
                .getResultList();
    }

    /**
     * Method to recursively find ids of all children of a dataverse that
     * are also of type dataverse
     */
    public List<Long> findAllDataverseDataverseChildren(Long dvId) {
        // get list of Dataverse children
        List<Long> dataverseChildren = findIdsByOwnerId(dvId);

        if (dataverseChildren == null) {
            return dataverseChildren;
        } else {
            List<Long> newChildren = new ArrayList<>();
            for (Long childDvId : dataverseChildren) {
                newChildren.addAll(findAllDataverseDataverseChildren(childDvId));
            }
            dataverseChildren.addAll(newChildren);
            return dataverseChildren;
        }
    }

    // function to recursively find ids of all children of a dataverse that are
    // of type dataset
    public List<Long> findAllDataverseDatasetChildren(Long dvId) {
        // get list of Dataverse children
        List<Long> dataverseChildren = findIdsByOwnerId(dvId);
        // get list of Dataset children
        List<Long> datasetChildren = datasetRepository.findIdsByOwnerId(dvId);

        for (Long childDvId : dataverseChildren) {
            datasetChildren.addAll(findAllDataverseDatasetChildren(childDvId));
        }
        return datasetChildren;
    }

    public String addRoleAssignmentsToChildren(Dataverse owner, List<String> rolesToInherit,
                                               boolean inheritAllRoles) {
        /*
         * This query recursively finds all Dataverses that are inside/children of the
         * specified one. It recursively finds dvobjects of dtype 'Dataverse' whose
         * owner_id equals an id already in the list and then returns the list of ids
         * found, excluding the id of the original specified Dataverse.
         */
        String qstr = "WITH RECURSIVE path_elements AS ((" + " SELECT id, dtype FROM dvobject WHERE id in ("
                + owner.getId() + "))" + " UNION\n"
                + " SELECT o.id, o.dtype FROM path_elements p, dvobject o WHERE o.owner_id = p.id and o.dtype='Dataverse') "
                + "SELECT id FROM path_elements WHERE id !=" + owner.getId() + ";";

        List<Integer> childIds;
        try {
            childIds = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            childIds = null;
        }

        // Set up to track the set of users/groups that get assigned a role and those
        // that don't
        JsonArrayBuilder usedNames = Json.createArrayBuilder();
        JsonArrayBuilder unusedNames = Json.createArrayBuilder();
        // Set up to track the list of dataverses, by id and alias, that are traversed.
        JsonArrayBuilder dataverseIds = Json.createArrayBuilder();
        JsonArrayBuilder dataverseAliases = Json.createArrayBuilder();
        // Get the Dataverses for the returned ids

        List<Dataverse> children = new ArrayList<>();

        for (Integer childId : childIds) {
            Dataverse child = find(childId.longValue());
            if (child != null) {
                // Add to the list of Dataverses
                children.add(child);
                // Add ids and aliases to the tracking arrays
                dataverseIds.add(childId.longValue());
                dataverseAliases.add(child.getAlias());
            }
        }
        // Find the role assignments on the specified Dataverse
        List<RoleAssignment> allRAsOnOwner = rolesService.directRoleAssignments(owner);

        // Create a list of just the inheritable role assignments on the original
        // dataverse
        List<RoleAssignment> inheritableRAsOnOwner = new ArrayList<>();
        for (RoleAssignment role : allRAsOnOwner) {
            if (inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias())) {
                //Only supporting built-in/non-dataverse-specific custom roles. Custom roles all have an owner.
                if (role.getRole().getOwner() == null) {
                    inheritableRAsOnOwner.add(role);
                }
            }
        }

        String privateUrlToken = null;
        // Create lists of the existing inheritable roles for each child Dataverse
        Map<Long, List<RoleAssignment>> existingRAs = new HashMap<>();
        for (Dataverse childDv : children) {
            List<RoleAssignment> allRAsOnChild = rolesService.directRoleAssignments(childDv);
            List<RoleAssignment> inheritableRoles = new ArrayList<>();
            for (RoleAssignment role : allRAsOnChild) {
                if (inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias())) {
                    inheritableRoles.add(role);
                }
            }
            existingRAs.put(childDv.getId(), inheritableRoles);
        }

        for (RoleAssignment roleAssignment : inheritableRAsOnOwner) {
            DataverseRole inheritableRole = roleAssignment.getRole();
            String identifier = roleAssignment.getAssigneeIdentifier();
            if (identifier.startsWith(AuthenticatedUser.IDENTIFIER_PREFIX)) {
                // The RoleAssignment is for an individual user
                // Add their name to the tracking list
                usedNames.add(identifier);
                // Strip the Identifier prefix so we can retrieve the user
                identifier = identifier.substring(AuthenticatedUser.IDENTIFIER_PREFIX.length());
                AuthenticatedUser roleUser = authService.getAuthenticatedUser(identifier);
                // Now loop over all children and add the roleUser in this role if they don't
                // yet have this role
                for (Dataverse childDv : children) {
                    try {
                        RoleAssignment ra = new RoleAssignment(inheritableRole, roleUser, childDv, privateUrlToken);
                        if (!existingRAs.get(childDv.getId()).contains(ra)) {
                            rolesService.save(ra);
                        }
                    } catch (Exception e) {
                        logger.warning("Unable to assign " + roleAssignment.getAssigneeIdentifier()
                                               + "as an admin for new Dataverse: " + childDv.getName());
                        logger.warning(e.getMessage());
                        throw (e);
                    }
                }
            } else if (identifier.startsWith(Group.IDENTIFIER_PREFIX)) {
                // The role assignment is for a group
                usedNames.add(identifier);
                identifier = identifier.substring(Group.IDENTIFIER_PREFIX.length());
                Group roleGroup = groupService.getGroup(identifier);
                if (roleGroup != null) {
                    for (Dataverse childDv : children) {
                        try {
                            RoleAssignment ra = new RoleAssignment(inheritableRole, roleGroup, childDv,
                                                                   privateUrlToken);
                            if (!existingRAs.get(childDv.getId()).contains(ra)) {
                                rolesService.save(ra);
                            }
                        } catch (Exception e) {
                            logger.warning("Unable to assign " + roleAssignment.getAssigneeIdentifier()
                                                   + "as an admin for new Dataverse: " + childDv.getName());
                            logger.warning(e.getMessage());
                            throw (e);
                        }
                    }
                } else {
                    // Add any groups of types not yet supported
                    unusedNames.add(identifier);
                }
            } else {
                // Add any other types of entity found (not user or group) that aren't supported
                unusedNames.add(identifier);
            }
        }
        /*
         * Report the list of Dataverses affected and the set of users/groups that
         * should now have admin roles on them (they may already have had them) and any
         * entities that had an admin role on the specified dataverse which were not
         * handled. Add this to the log and the API return message.
         */
        String result = Json.createObjectBuilder().add("Dataverses Updated", dataverseIds)
                .add("Updated Dataverse Aliases", dataverseAliases).add("Assignments added for", usedNames)
                .add("Assignments not added for", unusedNames).build().toString();
        logger.info(result);
        return (result);
    }
}
