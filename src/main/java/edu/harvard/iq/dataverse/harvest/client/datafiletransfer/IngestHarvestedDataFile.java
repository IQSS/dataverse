/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client.datafiletransfer;

import edu.harvard.iq.dataverse.DataFile;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author anuj
 */
public class IngestHarvestedDataFile {
    
    private EntityManagerFactory entityManagerFactory;
    
    private List<DataFile> listOfDataFile;
    
    public IngestHarvestedDataFile(){
        entityManagerFactory = Persistence.createEntityManagerFactory( "VDCNet-ejbPU" );
        this.listOfDataFile =  new ArrayList<DataFile>();
    }
    
    public List<DataFile> getHarvestedDatafiles(){
        EntityManager em = entityManagerFactory.createEntityManager();
        listOfDataFile.addAll((em.createQuery("SELECT object(df) "
                    + "FROM DataFile AS df ORDER BY df.id").getResultList()));
        return listOfDataFile;
    }
    
}
