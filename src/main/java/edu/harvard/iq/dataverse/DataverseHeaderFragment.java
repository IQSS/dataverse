/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class DataverseHeaderFragment implements java.io.Serializable {

    @EJB
    DataverseServiceBean dataverseService;
    
    @Inject
    DataverseSession dataverseSession;

    public List<Dataverse> getDataverses(Dataverse dataverse) {
        List dataverses = new ArrayList();
        if (dataverse != null) {
            dataverses.addAll(dataverse.getOwners());
            dataverses.add(dataverse);
        } else {
            dataverses.add(dataverseService.findRootDataverse());
        }
        return dataverses;
    }

    public TreeNode getDataverseTree(Dataverse dataverse) {
        if (dataverse == null) { // the primefaces component seems to call this with dataverse == null for some reason
            return null;
        }
        return getDataverseNode(dataverse, null, true);
    }

    private TreeNode getDataverseNode(Dataverse dataverse, TreeNode root, boolean expand) {
        TreeNode dataverseNode = new DefaultTreeNode(dataverse, root);
        dataverseNode.setExpanded(expand);
        List<Dataverse> childDataversesOfCurrentDataverse = dataverseService.findByOwnerId(dataverse.getId());
        for (Dataverse child : childDataversesOfCurrentDataverse) {
            getDataverseNode(child, dataverseNode, false);
        }

        return dataverseNode;
    }
    
    public String logout() {
        dataverseSession.setUser(null);
        return "/dataverse.xhtml?faces-redirect=true";
    }

}
