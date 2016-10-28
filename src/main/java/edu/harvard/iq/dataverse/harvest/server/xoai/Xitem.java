/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.model.oaipmh.About;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 *
 * @author Leonid Andreev
 * 
 * This is an implemention of an Lyncode XOAI Item; 
 * You can think of it as an XOAI Item wrapper around the
 * Dataverse OAIRecord entity.
 */
public class Xitem implements Item {
    
    public Xitem(OAIRecord oaiRecord) {
        super();
        this.oaiRecord = oaiRecord;  
    }
    
    private OAIRecord oaiRecord;
    
    public OAIRecord getOaiRecord() {
        return oaiRecord;
    }
    
    public void setOaiRecord(OAIRecord oaiRecord) {
        this.oaiRecord = oaiRecord;
    }

    private Dataset dataset; 
    
    public Dataset getDataset() {
        return dataset;
    }
    
    public Xitem withDataset(Dataset dataset) {
        this.dataset = dataset; 
        return this; 
    }
    
    @Override
    public List<About> getAbout() {
        return null;
    }

    @Override
    public Xmetadata getMetadata() {
        return new Xmetadata((String)null);
    }

    @Override
    public String getIdentifier() {
        return oaiRecord.getGlobalId();
    }

    @Override
    public Date getDatestamp() {
        return oaiRecord.getLastUpdateTime();
    }

    @Override
    public List<com.lyncode.xoai.dataprovider.model.Set> getSets() {
        List<Set> sets = new ArrayList<>();
        if (oaiRecord.getSetName() != null) {
            sets.add(new Set(oaiRecord.getSetName()));
        }
        
        return sets;
 
    }

    @Override
    public boolean isDeleted() {
        return oaiRecord.isRemoved();
    }

}
