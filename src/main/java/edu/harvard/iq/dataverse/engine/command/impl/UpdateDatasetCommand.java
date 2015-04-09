/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 *
 * @author skraffmiller
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetCommand extends AbstractCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(UpdateDatasetCommand.class.getCanonicalName());
    private final Dataset theDataset;

    public UpdateDatasetCommand(Dataset theDataset, User user) {
        super(user, theDataset);
        this.theDataset = theDataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        // first validate
        // @todo for now we run through an initFields method that creates empty fields for anything without a value
        // that way they can be checked for required
        theDataset.getEditVersion().setDatasetFields(theDataset.getEditVersion().initDatasetFields());
        Set<ConstraintViolation> constraintViolations = theDataset.getEditVersion().validate();        
        if (!constraintViolations.isEmpty()) {
            String validationFailedString = "Validation failed:";
            for (ConstraintViolation constraintViolation : constraintViolations) {
                validationFailedString += " " + constraintViolation.getMessage();
            }
            throw new IllegalCommandException(validationFailedString, this);
        }
        
        if ( ! (getUser() instanceof AuthenticatedUser) ) {
            throw new IllegalCommandException("Only authenticated users can update datasets", this);
        }
        
        return save(ctxt);
    }

    public void saveDatasetAPI(CommandContext ctxt) {
        save(ctxt);
    }

    public Dataset save(CommandContext ctxt) {
        
        Iterator<DatasetField> dsfIt = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (dsfIt.next().removeBlankDatasetFieldValues()) {
                dsfIt.remove();
            }
        }
        Iterator<DatasetField> dsfItSort = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfItSort.hasNext()) {
            dsfItSort.next().setValueDisplayOrder();
        }
        Timestamp updateTime = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setLastUpdateTime(updateTime);
        theDataset.setModificationTime(updateTime);
        for (DataFile dataFile : theDataset.getFiles()) {
            if (dataFile.getCreateDate() == null) {
                dataFile.setCreateDate(updateTime);
                dataFile.setCreator((AuthenticatedUser) getUser());
            }
        }

        String nonNullDefaultIfKeyNotFound = "";
        String doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);

        if (theDataset.getProtocol().equals("doi")
                && doiProvider.equals("EZID") && theDataset.getGlobalIdCreateTime() == null) {
            String doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
            if (doiRetString.contains(theDataset.getIdentifier())) {
                theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
            } else {
                //try again if identifier exists
                if (doiRetString.contains("identifier already exists")) {
                    theDataset.setIdentifier(ctxt.datasets().generateIdentifierSequence(theDataset.getProtocol(), theDataset.getAuthority(), theDataset.getDoiSeparator()));
                    doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
                    if (!doiRetString.contains(theDataset.getIdentifier())) {
                        // didn't register new identifier
                    } else {
                        theDataset.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                    }
                } else {
                    //some reason other that duplicate identifier so don't try again
                    //EZID down possibly
                }
            }
        }

        Dataset savedDataset = ctxt.em().merge(theDataset);
        ctxt.em().flush();

        /**
         * @todo What should we do with the indexing result? Print it to the
         * log?
         */
        boolean doNormalSolrDocCleanUp = true;
        Future<String> indexingResult = ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        //String indexingResult = "(Indexing Skipped)";
//        logger.log(Level.INFO, "during dataset save, indexing result was: {0}", indexingResult);
        DatasetVersionUser ddu = ctxt.datasets().getDatasetVersionUser(theDataset.getLatestVersion(), this.getUser());

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
        return savedDataset;
    }

}
