package edu.harvard.iq.dataverse.harvest.server.xoai;

import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import io.gdcc.xoai.model.oaipmh.results.record.About;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Leonid Andreev
 * 
 * This is an implemention of a Lyncode/DSpace/gdcc XOAI Item.
 * You can think of it as an XOAI Item wrapper around the
 * Dataverse OAIRecord entity.
 */
public final class DataverseXoaiItem implements Item {
    
    public DataverseXoaiItem(OAIRecord oaiRecord) {
        super();
        this.oaiRecord = oaiRecord;
        oaisets = new ArrayList<>();
        if (!StringUtil.isEmpty(oaiRecord.getSetName())) {
            oaisets.add(new Set(oaiRecord.getSetName()));
        }
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
    
    public DataverseXoaiItem withDataset(Dataset dataset) {
        this.dataset = dataset; 
        return this; 
    }
    
    private Metadata metadata;
    
    @Override
    public Metadata getMetadata() {
        return metadata;
    }
    
    public DataverseXoaiItem withMetadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public String getIdentifier() {
        return oaiRecord.getGlobalId();
    }

    @Override
    public Instant getDatestamp() {
        return oaiRecord.getLastUpdateTime().toInstant();
    }
    
    private  List<Set> oaisets;

    @Override
    public List<Set> getSets() {
        
        return oaisets;
    }

    @Override
    public boolean isDeleted() {
        return oaiRecord.isRemoved();
    }
}
