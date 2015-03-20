/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 *
 * @author skraffmiller
 */

@RequiredPermissions(Permission.PublishDataset)
public class PublishDatasetCommand extends AbstractCommand<Dataset> {

    boolean minorRelease = false;
    Dataset theDataset;

    /**
     * @todo Is there any use case where this command should allow the
     * publication of a "V0" version? Shouldn't the first published version of a
     * dataset be "V1"? Before a fix/workaround was introduced, it was possible
     * to use this command to create a published "V0" version. For details, see
     * https://github.com/IQSS/dataverse/issues/1392
     */
    public PublishDatasetCommand(Dataset datasetIn, AuthenticatedUser user, boolean minor) {
        super(user, datasetIn);
        minorRelease = minor;
        theDataset = datasetIn;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {

        if (!theDataset.getOwner().isReleased()) {
            throw new IllegalCommandException("This dataset may not be published because its host dataverse (" + theDataset.getOwner().getAlias() + ") has not been published.", this);
        }
        /* make an attempt to register if not registered */
        String nonNullDefaultIfKeyNotFound = "";
        String    protocol = theDataset.getProtocol();
        String    doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        String    authority = theDataset.getAuthority();        
        if (theDataset.getGlobalIdCreateTime() == null) {
            if (protocol.equals("doi")
                    && doiProvider.equals("EZID")) {
                String doiRetString = ctxt.doiEZId().createIdentifier(theDataset);               
                if (doiRetString.contains(theDataset.getIdentifier())) {
                    theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                } else {
                    if (doiRetString.contains("identifier already exists")){
                        theDataset.setIdentifier(ctxt.datasets().generateIdentifierSequence(protocol, authority, theDataset.getDoiSeparator()));
                        doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
                        if(!doiRetString.contains(theDataset.getIdentifier())){
                            throw new IllegalCommandException("This dataset may not be published because it has not been registered. Please contact thedata.org for assistance.", this);
                        } else{
                            theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                        }
                    } else {
                          throw new IllegalCommandException("This dataset may not be published because it has not been registered. Please contact thedata.org for assistance.", this);
                    }
                }
            } else {
                 throw new IllegalCommandException("This dataset may not be published because it has not been registered. Please contact thedata.org for assistance.", this);
            }
        }

        if (theDataset.getLatestVersion().isReleased()) {
            throw new IllegalCommandException("Latest version of dataset " + theDataset.getIdentifier() + " is already released. Only draft versions can be released.", this);
        }

        if (minorRelease && !theDataset.getLatestVersion().isMinorUpdate()) {
            throw new IllegalCommandException("Cannot release as minor version. Re-try as major release.", this);
        }

        if (theDataset.getPublicationDate() == null) {
            theDataset.setPublicationDate(new Timestamp(new Date().getTime()));
            theDataset.setReleaseUser((AuthenticatedUser) getUser());
            if (!minorRelease) {
                theDataset.getEditVersion().setVersionNumber(new Long(1));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            } else {
                theDataset.getEditVersion().setVersionNumber(new Long(0));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(1));
            }
        } else {
            if (!minorRelease) {
                theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber() + 1));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(0));
            } else {
                theDataset.getEditVersion().setVersionNumber(new Long(theDataset.getVersionNumber()));
                theDataset.getEditVersion().setMinorVersionNumber(new Long(theDataset.getMinorVersionNumber() + 1));
            }
        }
        
        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setReleaseTime(updateTime);
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        theDataset.setModificationTime(updateTime);
        theDataset.getEditVersion().setVersionState(DatasetVersion.VersionState.RELEASED);

        for (DataFile dataFile : theDataset.getFiles()) {
            if (dataFile.getPublicationDate() == null) {
                // this is a new, previously unpublished file, so publish by setting date
                dataFile.setPublicationDate(updateTime);
                
                // check if any prexisting roleassignments have file download and send notifications
                List<RoleAssignment> ras = ctxt.roles().directRoleAssignments(dataFile);
                for (RoleAssignment ra : ras) {
                    if (ra.getRole().permissions().contains(Permission.DownloadFile)) {
                        for (AuthenticatedUser au : ctxt.roleAssignees().getExplicitUsers(ctxt.roleAssignees().getRoleAssignee(ra.getAssigneeIdentifier()))) {
                            ctxt.notifications().sendNotification(au, new Timestamp(new Date().getTime()), UserNotification.Type.GRANTFILEACCESS, theDataset.getId());                                       
                        }
                    }
                }                                
            }
            
            // set the files restriction flag to the same as the latest version's
            if (dataFile.getFileMetadata() != null && dataFile.getFileMetadata().getDatasetVersion().equals(theDataset.getLatestVersion())) {
                dataFile.setRestricted(dataFile.getFileMetadata().isRestricted());
            }
        }
        
        theDataset.setFileAccessRequest(theDataset.getLatestVersion().isFileAccessRequest());
        Dataset savedDataset = ctxt.em().merge(theDataset);

        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        /**
         * @todo consider also ctxt.solrIndex().indexPermissionsOnSelfAndChildren(theDataset);
         */
        /**
         * @todo what should we do with the indexRespose?
         */
        IndexResponse indexResponse = ctxt.solrIndex().indexPermissionsForOneDvObject(savedDataset.getId());

        // set the subject of the parent (all the way up) Dataverses
        DatasetField subject = null;
        for (DatasetField dsf : savedDataset.getLatestVersion().getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject)) {
                subject = dsf;
                break;
            }   
        }
        if (subject != null) {
            Dataverse dv = savedDataset.getOwner();
            while (dv != null) {
                if (dv.getDataverseSubjects().addAll(subject.getControlledVocabularyValues())) {
                    ctxt.index().indexDataverse(dv); // need to reindex to capture the new subjects
                }
                dv = dv.getOwner();
            }
        }        
        
        
        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(savedDataset.getLatestVersion(), this.getUser());

        if (ddu != null) {
            ddu.setLastUpdateDate(updateTime);
            ctxt.em().merge(ddu);
        } else {
            DatasetVersionUser datasetDataverseUser = new DatasetVersionUser();
            datasetDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());
            datasetDataverseUser.setLastUpdateDate((Timestamp) updateTime);
            String id = getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            datasetDataverseUser.setAuthenticatedUser(au);
            ctxt.em().merge(datasetDataverseUser);
        }

        ctxt.doiEZId().publicizeIdentifier(savedDataset);
        return savedDataset;
    }

}
