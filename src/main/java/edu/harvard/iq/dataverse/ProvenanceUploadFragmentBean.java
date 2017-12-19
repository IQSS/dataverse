/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * This bean exists to ease the reuse of provenance upload code across pages
 * 
 * @author madunlap
 */
public class ProvenanceUploadFragmentBean implements java.io.Serializable{
    
    /**
     * Handle native file replace
     * @param event 
     * @throws java.io.IOException 
     */
    public void handleFileUpload(FileUploadEvent event) throws IOException {
        //refer to EditDatafilesPage or somewhere else...
    }

}
