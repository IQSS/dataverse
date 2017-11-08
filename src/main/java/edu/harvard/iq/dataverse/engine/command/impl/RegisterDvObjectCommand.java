/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.IdServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author skraffmi
 */
@RequiredPermissions(Permission.EditDataset)
public class RegisterDvObjectCommand extends AbstractVoidCommand {

    private final DvObject target;

    public RegisterDvObjectCommand(DataverseRequest aRequest, DvObject target) {
        super(aRequest, target);
        this.target = target;
        System.out.print("end of constructor: ");
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        String nonNullDefaultIfKeyNotFound = "";
        String protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        String authority = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        String doiSeparator = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);

        System.out.print(" start of execute: ");
        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            System.out.print("!Super user");
            throw new PermissionException("REgister DV Object can only be called by superusers.",
                    this, Collections.singleton(Permission.EditDataset), target);
        }

        IdServiceBean idServiceBean = IdServiceBean.getBean(target.getProtocol(), ctxt);

        try {
            //Test to see if identifier already present
            //if so, leave.
            if (target.getIdentifier() == null || target.getIdentifier().isEmpty()) {
                if (target.isInstanceofDataset()) {
                    target.setIdentifier(ctxt.datasets().generateDatasetIdentifier((Dataset) target, idServiceBean));

                } else {
                    target.setIdentifier(ctxt.files().generateDatasetIdentifier((DataFile) target, idServiceBean));

                }
                if (target.getProtocol() == null) {
                    target.setProtocol(protocol);
                }
                if (target.getAuthority() == null) {
                    target.setAuthority(authority);
                }
                if (target.getDoiSeparator() == null) {
                    target.setDoiSeparator(doiSeparator);
                }

            }

            if (idServiceBean.alreadyExists(target)) {
                System.out.print("already exists");
                return;
            }
            System.out.print("Before create Identifier");
            String doiRetString = idServiceBean.createIdentifier(target);
            System.out.print("After create Identifier");
            if (doiRetString != null && doiRetString.contains(target.getIdentifier())) {
                if (target.isReleased()) {
                    System.out.print("Is released: " + target.getIdentifier());
                    System.out.print("Publicizing : " + target.getId());
                    idServiceBean.publicizeIdentifier(target);
                    System.out.print("after Pub Identifier");
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
                            df.setIdentifier(ctxt.files().generateDatasetIdentifier(df, idServiceBean));
                            if (df.getProtocol() == null) {
                                df.setProtocol(protocol);
                            }
                            if (df.getAuthority() == null) {
                                df.setAuthority(authority);
                            }
                            if (df.getDoiSeparator() == null) {
                                df.setDoiSeparator(doiSeparator);
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
                System.out.print("in 'else'");
                //do nothing - we'll know it failed because the global id create time won't have been updated.
            }
        } catch (Exception e) {
            System.out.print("some kind of exception: " + e.getMessage());
            //do nothing - idem and the problem has been logged
        } catch (Throwable ex) {
            System.out.print("some kind of Throwable: " + ex.getMessage());
            //do nothing - we'll know it failed because the global id create time won't have been updated.
        }
    }
    
    
}
