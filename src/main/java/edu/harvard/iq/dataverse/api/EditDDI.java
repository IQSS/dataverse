package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataset;

import edu.harvard.iq.dataverse.engine.command.Command;

import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataUtil;
import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
import edu.harvard.iq.dataverse.datavariable.VarGroup;
import edu.harvard.iq.dataverse.datavariable.CategoryMetadata;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.datavariable.VariableMetadataDDIParser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import org.apache.solr.client.solrj.SolrServerException;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Date;
import java.sql.Timestamp;


import javax.validation.ConstraintViolationException;

@Stateless
@Path("edit")
public class EditDDI  extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Access.class.getCanonicalName());

    //private static final String API_KEY_HEADER = "X-Dataverse-key";

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    PermissionServiceBean permissionService;

    @EJB
    VariableServiceBean variableService;

    @EJB
    EjbDataverseEngine commandEngine;

    @Inject
    DataverseRequestServiceBean dvRequestService;

    @EJB
    IndexServiceBean indexService;

    @Inject
    DataverseSession session;

    private List<FileMetadata> filesToBeDeleted = new ArrayList<>();

    private VariableMetadataUtil variableMetadataUtil;


    @PUT
    @Consumes("application/xml")
    @Path("{fileId}")
    public Response edit (InputStream body, @PathParam("fileId") String fileId) {
        DataFile dataFile = null;
        try {
            dataFile = findDataFileOrDie(fileId);

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        User apiTokenUser = checkAuth(dataFile);

        if (apiTokenUser == null) {
            return unauthorized("Cannot edit metadata, access denied" );
        }

        Map<Long, VariableMetadata> mapVarToVarMet = new HashMap<Long, VariableMetadata>();
        Map<Long,VarGroup> varGroupMap = new HashMap<Long, VarGroup>();
        try {
            readXML(body, mapVarToVarMet,varGroupMap);
        } catch (XMLStreamException e) {
            logger.warning(e.getMessage());
            return error(Response.Status.NOT_ACCEPTABLE, "bad xml file" );
        }

        DatasetVersion latestVersion = dataFile.getOwner().getLatestVersion();
        Dataset dataset = latestVersion.getDataset();

        ArrayList<VariableMetadata> neededToUpdateVM = new ArrayList<VariableMetadata>();

        if (!latestVersion.isWorkingCopy()) {
            //for new draft version

            FileMetadata latestFml = dataFile.getLatestPublishedFileMetadata();

            boolean groupUpdate = newGroups(varGroupMap, latestFml);
            boolean varUpdate = varUpdates(mapVarToVarMet, latestFml, neededToUpdateVM, true);
            if (varUpdate || groupUpdate) {
                if (!createNewDraftVersion(neededToUpdateVM,  varGroupMap, dataset, dataFile, apiTokenUser)) {
                    return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to create new draft version" );
                }
            } else {
                return ok("Nothing to update");
            }
        } else {

            FileMetadata fml = dataFile.getFileMetadata();
            boolean groupUpdate = newGroups(varGroupMap, fml);
            boolean varUpdate = varUpdates(mapVarToVarMet, fml, neededToUpdateVM, false);
            if (varUpdate || groupUpdate) {

                if (!updateDraftVersion(neededToUpdateVM, varGroupMap, dataset, apiTokenUser, groupUpdate, fml)) {
                    return error(Response.Status.INTERNAL_SERVER_ERROR, "Failed to update draft version" );
                }
            } else {
                return ok("Nothing to update");
            }

        }
        return ok("Updated");
    }

    private boolean varUpdates( Map<Long, VariableMetadata> mapVarToVarMet , FileMetadata fm, ArrayList<VariableMetadata> neededToUpdateVM, boolean newVersion) {
        boolean updates = false;

        for ( Long varId : mapVarToVarMet.keySet()) {
            VariableMetadata varMet = mapVarToVarMet.get(varId);
            List<VariableMetadata> vml = variableService.findByDataVarIdAndFileMetaId(varMet.getDataVariable().getId(), fm.getId());
            if (vml.size() > 0) {
                if (!variableMetadataUtil.compareVarMetadata(vml.get(0), varMet )) {
                    updates = true;
                    neededToUpdateVM.add(varMet);
                } else if (newVersion) {
                    neededToUpdateVM.add(varMet);
                }
            } else {
                if (!AreDefaultValues(varMet)) {
                    neededToUpdateVM.add(varMet);
                    updates = true;
                }
            }
        }

        return updates;
    }

    private boolean createNewDraftVersion(ArrayList<VariableMetadata> neededToUpdateVM, Map<Long,VarGroup> varGroupMap, Dataset dataset, DataFile dataFile, User apiTokenUser ) {


        FileMetadata fm = dataFile.getFileMetadata();

        Command<Dataset> cmd;
        try {

            DataverseRequest dr = createDataverseRequest(apiTokenUser);
            cmd = new UpdateDatasetVersionCommand(dataset, dr, fm);
            ((UpdateDatasetVersionCommand) cmd).setValidateLenient(true);
            dataset = commandEngine.submit(cmd);

        } catch (EJBException ex) {
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            logger.log(Level.SEVERE, "Couldn''t save dataset: {0}", error.toString());

            return false;
        } catch (CommandException ex) { ;
            logger.log(Level.SEVERE, "Couldn''t save dataset: {0}", ex.getMessage());
            return false;
        }

       List<FileMetadata> fmlList = dataset.getLatestVersion().getFileMetadatas();
        FileMetadata fml = null;
        for (FileMetadata fmlCurr : fmlList) {
            if (fmlCurr.getDataFile().getId() == dataFile.getId()) {
                fml = fmlCurr;
            }
        }

        for (int i = 0; i < neededToUpdateVM.size(); i++) {
            neededToUpdateVM.get(i).setFileMetadata(fml);
            updateCategories(neededToUpdateVM.get(i));
            try {
                em.persist(neededToUpdateVM.get(i));
            } catch (EJBException ex) {
                logger.log(Level.SEVERE, "Couldn''t save dataset: " + ex.getMessage());
                return false;
            }
        }

        //add New groups
        for (VarGroup varGroup : varGroupMap.values()) {
            varGroup.setFileMetadata(fml);
            varGroup.setId(null);
            try {
                em.persist(varGroup);
            } catch (EJBException ex) {
                logger.log(Level.SEVERE, "Couldn''t save dataset: " + ex.getMessage());
                return false;
            }
        }

        boolean doNormalSolrDocCleanUp = true;
        try {
            Future<String> indexDatasetFuture = indexService.indexDataset(dataset, doNormalSolrDocCleanUp);
        } catch (IOException | SolrServerException ex) {
            logger.log(Level.SEVERE, "Couldn''t index dataset: " + ex.getMessage());
        }

        return true;
    }

    private void updateCategories(VariableMetadata varMet) {

        Collection<CategoryMetadata> cms = varMet.getCategoriesMetadata();
        DataVariable dv = em.find(DataVariable.class, varMet.getDataVariable().getId());
        Collection<VariableCategory> vcl =  dv.getCategories();
        for (CategoryMetadata cm : cms) {
            String catValue = cm.getCategory().getValue();
            Long varId = varMet.getDataVariable().getId();
            for (VariableCategory vc : vcl) {
                if ((catValue != null && catValue.equals(vc.getValue())) || (catValue == null && vc.getValue()==null )) {
                    cm.getCategory().setId(vc.getId());
                    break;
                }
            }
        }

    }

    private void updateCategoryMetadata(VariableMetadata vmNew, VariableMetadata vmOld) {
        for (CategoryMetadata cm : vmOld.getCategoriesMetadata()) { // update categories
            for (CategoryMetadata cmNew : vmNew.getCategoriesMetadata()) {
                if (cm.getCategory().getValue().equals(cmNew.getCategory().getValue())) {
                    cmNew.setId(cm.getId());
                    break;
                }
            }
        }

    }

    private boolean updateDraftVersion(ArrayList<VariableMetadata> neededToUpdateVM, Map<Long,VarGroup> varGroupMap, Dataset dataset, User apiTokenUser, boolean groupUpdate, FileMetadata fml ) {


        for (int i = 0; i < neededToUpdateVM.size(); i++)  {
            VariableMetadata vm = neededToUpdateVM.get(i);
            DataVariable dv = em.find(DataVariable.class, vm.getDataVariable().getId() );
            if (dv==null) { //Variable does not exist in database
                return false;
            }
            vm.setDataVariable(dv);
            updateCategories(vm);
            List<VariableMetadata> vmOld = variableService.findByDataVarIdAndFileMetaId(vm.getDataVariable().getId(), fml.getId());
            if (vmOld.size() > 0) {
                vm.setId(vmOld.get(0).getId());
                if (!vm.isWeighted() && vmOld.get(0).isWeighted()) { //unweight the variable
                    for (CategoryMetadata cm : vmOld.get(0).getCategoriesMetadata()) {
                        CategoryMetadata oldCm = em.find(CategoryMetadata.class, cm.getId());
                        em.remove(oldCm);
                    }
                } else {
                    updateCategoryMetadata(vm, vmOld.get(0));
                }
            }
            try {
                vm.setFileMetadata(fml);
                em.merge(vm);
            } catch (ConstraintViolationException e) {
                logger.log(Level.SEVERE,"Exception: ");
                e.getConstraintViolations().forEach(err->logger.log(Level.SEVERE,err.toString()));
            }

        }
        if (groupUpdate) {
            //remove old groups
            List<VarGroup> varGroups = variableService.findAllGroupsByFileMetadata(fml.getId());
            for (int i = 0; i < varGroups.size(); i++) {
                em.remove(varGroups.get(i));
            }

            //add new groups
            for (VarGroup varGroup : varGroupMap.values()) {
                varGroup.setFileMetadata(fml);
                varGroup.setId(null);
                em.merge(varGroup);
            }
        }
        Command<Dataset> cmd;
        try {
            DataverseRequest dr = createDataverseRequest(apiTokenUser);
            cmd = new UpdateDatasetVersionCommand(dataset, dr);
            ((UpdateDatasetVersionCommand) cmd).setValidateLenient(true);
            commandEngine.submit(cmd);

        } catch (EJBException ex) {
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            logger.log(Level.SEVERE, "Couldn''t save dataset: {0}", error.toString());

            return false;
        } catch (CommandException ex) { ;
            logger.log(Level.SEVERE, "Couldn''t save dataset: {0}", ex.getMessage());
            return false;
        }

        return true;
    }

    private  void readXML(InputStream body, Map<Long,VariableMetadata> mapVarToVarMet, Map<Long,VarGroup> varGroupMap) throws XMLStreamException {
        XMLInputFactory factory=XMLInputFactory.newInstance();
        XMLStreamReader xmlr=factory.createXMLStreamReader(body);
        VariableMetadataDDIParser vmdp = new VariableMetadataDDIParser();

        vmdp.processDataDscr(xmlr,mapVarToVarMet, varGroupMap);

    }

    private boolean newGroups(Map<Long,VarGroup> varGroupMap, FileMetadata fm) {
        boolean areNewGroups = false;

        List<VarGroup> varGroups = variableService.findAllGroupsByFileMetadata(fm.getId());
        if (varGroups.size() != varGroupMap.size()) {
            return true;
        }

        for (Long id : varGroupMap.keySet()) {
            VarGroup dbVarGroup = em.find(VarGroup.class, id);
            if (dbVarGroup != null) {
                if (variableMetadataUtil.checkDiff(dbVarGroup.getLabel(), varGroupMap.get(id).getLabel())) {
                    areNewGroups = true;
                    break;
                } else if (!dbVarGroup.getVarsInGroup().equals(varGroupMap.get(id).getVarsInGroup())) {
                    areNewGroups = true;
                    break;
                }
            } else {
                areNewGroups = true;
                break;
            }

        }

        return areNewGroups;
    }



    private boolean AreDefaultValues(VariableMetadata varMet) {
        boolean thedefault = true;

        if (varMet.getNotes() != null && !varMet.getNotes().trim().equals("")) {
            thedefault = false;
        } else if (varMet.getUniverse() != null && !varMet.getUniverse().trim().equals("") ) {
            thedefault = false;
        } else if (varMet.getInterviewinstruction() != null && !varMet.getInterviewinstruction().trim().equals("")) {
            thedefault = false;
        } else if (varMet.getLiteralquestion() != null && !varMet.getLiteralquestion().trim().equals("")) {
            thedefault = false;
        } else if (varMet.getPostquestion() != null && !varMet.getPostquestion().trim().equals("")) {
            thedefault = false;
        } else if (varMet.isIsweightvar() != false ) {
            thedefault = false;
        } else if (varMet.isWeighted() != false) {
            thedefault = false;
        } else {
            DataVariable dv = em.find(DataVariable.class, varMet.getDataVariable().getId());
            if (dv.getLabel() == null && varMet.getLabel() != null && !varMet.getLabel().trim().equals("")) {
                thedefault = false;
            } else if (dv.getLabel() != null && !dv.getLabel().equals(varMet.getLabel())) {
                thedefault = false;
            }
        }

        return thedefault;
    }


    private User checkAuth(DataFile dataFile) {

        User apiTokenUser = null;

        try {
            apiTokenUser = findUserOrDie();
        } catch (WrappedResponse wr) {
            apiTokenUser = null;
            logger.log(Level.FINE, "Message from findUserOrDie(): {0}", wr.getMessage());
        }

        if (apiTokenUser != null) {
            // used in an API context
            if (!permissionService.requestOn(createDataverseRequest(apiTokenUser), dataFile.getOwner()).has(Permission.EditDataset)) {
                apiTokenUser = null;
            }
        }

        return apiTokenUser;

    }
}


