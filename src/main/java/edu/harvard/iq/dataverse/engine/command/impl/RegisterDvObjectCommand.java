package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.PersistentIdentifierServiceBean;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions({})
public class RegisterDvObjectCommand extends AbstractVoidCommand {

    private final DvObject target;

    public RegisterDvObjectCommand(DataverseRequest aRequest, DvObject target) {
        super(aRequest, target);
        this.target = target;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        String nonNullDefaultIfKeyNotFound = "";
        String protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        String authority = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        PersistentIdentifierServiceBean idServiceBean = PersistentIdentifierServiceBean.getBean(target.getProtocol(), ctxt);
        try {
            //Test to see if identifier already present
            //if so, leave.
            if (target.getIdentifier() == null || target.getIdentifier().isEmpty()) {
                if (target.isInstanceofDataset()) {
                    target.setIdentifier(ctxt.datasets().generateDatasetIdentifier((Dataset) target, idServiceBean));

                } else {
                    target.setIdentifier(ctxt.files().generateDataFileIdentifier((DataFile) target, idServiceBean));
                }
                if (target.getProtocol() == null) {
                    target.setProtocol(protocol);
                }
                if (target.getAuthority() == null) {
                    target.setAuthority(authority);
                }
            }

            if (idServiceBean.alreadyExists(target)) {
                return;
            }
            String doiRetString = idServiceBean.createIdentifier(target);
            if (doiRetString != null && doiRetString.contains(target.getIdentifier())) {
                if (target.isReleased()) {
                    idServiceBean.publicizeIdentifier(target);
                }

                if (!idServiceBean.registerWhenPublished() || target.isReleased()) {
                    target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                    target.setIdentifierRegistered(true);
                }

                ctxt.em().merge(target);
                ctxt.em().flush();
                if (target.isInstanceofDataset()) {
                    Dataset dataset = (Dataset) target;
                    for (DataFile df : dataset.getFiles()) {
                        if (df.getIdentifier() == null || df.getIdentifier().isEmpty()) {
                            df.setIdentifier(ctxt.files().generateDataFileIdentifier(df, idServiceBean));
                            if (df.getProtocol() == null) {
                                df.setProtocol(protocol);
                            }
                            if (df.getAuthority() == null) {
                                df.setAuthority(authority);
                            }
                        }
                        doiRetString = idServiceBean.createIdentifier(df);
                        if (doiRetString != null && doiRetString.contains(df.getIdentifier())) {
                            if (df.isReleased()) {
                                idServiceBean.publicizeIdentifier(df);
                            }
                            if (!idServiceBean.registerWhenPublished() || df.isReleased()) {
                                df.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                                df.setIdentifierRegistered(true);
                            }
                            ctxt.em().merge(df);
                            ctxt.em().flush();
                        }
                    }
                }

            } else {
                //do nothing - we'll know it failed because the global id create time won't have been updated.
            }
        } catch (Exception e) {
            //do nothing - idem and the problem has been logged
        } catch (Throwable ex) {
            //do nothing - we'll know it failed because the global id create time won't have been updated.
        }
    }
        
}
