/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
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

    
    public TreeNode getDataverseTree(Dataverse dataverse) { 
        if (dataverse == null) {return null;}
        return getDataverseTree(dataverse, new DefaultTreeNode("Root",null));
    }   

    public TreeNode getDataverseTree(Dataverse dataverse, TreeNode root) {
        
        TreeNode dataverseNode = new DefaultTreeNode(dataverse, root);

        List<Dataverse> childDataversesOfCurrentDataverse = dataverseService.findByOwnerId(dataverse.getId());
        for (Dataverse child : childDataversesOfCurrentDataverse) {
            getDataverseTree(child, dataverseNode);
        }   
      
        return root;
    }

}
