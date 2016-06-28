/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.web;

import com.google.common.base.Function;
import com.lyncode.builder.ListBuilder;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.model.oaipmh.About;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.model.xoai.Element;
import com.lyncode.xoai.model.xoai.XOAIMetadata;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

/**
 *
 * @author Leonid Andreev
 */
public class XOAIItem implements Item {
    
    /*
    public static XOAIItem item () {
        return new XOAIItem();
    }*/
    
    public XOAIItem(OAIRecord oaiRecord) {
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

    @Override
    public List<About> getAbout() {
        return null;
    }

    @Override
    public xMetadata getMetadata() {
        // Temporary stub for retrieving DDI metadata records: 
        // - the assumption is that they are pre-exported and stored in the Dataset
        // directory, as "export_{format}.xml". 
        // This stub will be replaced by a call to the export subsystem. 
        
        try {
            String filesRootDirectory = System.getProperty("dataverse.files.directory");
            String identifier = getIdentifier().replaceFirst("^[^:]*:", "/");
            InputStream inputStream = new FileInputStream(new File(filesRootDirectory + identifier + "/export_ddi.xml"));
            return new xMetadata(inputStream);
        } catch (Exception e) {
            return new xMetadata("");
        }
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
