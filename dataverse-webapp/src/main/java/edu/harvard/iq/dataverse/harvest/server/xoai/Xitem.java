/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.model.oaipmh.About;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import edu.harvard.iq.dataverse.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static java.util.stream.Collectors.toList;


/**
 * @author Leonid Andreev
 * <p>
 * This is an implemention of an Lyncode XOAI Item;
 * You can think of it as an XOAI Item wrapper around the
 * Dataverse OAIRecord entity.
 */
public class Xitem implements Item {

    private String identifier;

    private Date lastUpdateTimestamp;

    private boolean deleted;

    private java.util.Set<String> oaisets = new HashSet<>();

    private Dataset dataset;

    // -------------------- CONSTRUCTORS --------------------

    public Xitem(String identifier, Date lastUpdateTimestamp, boolean deleted) {
        this.identifier = identifier;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
        this.deleted = deleted;
    }

    // -------------------- GETTERS --------------------

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
        return new Xmetadata(null);
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Date getDatestamp() {
        return lastUpdateTimestamp;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public List<Set> getSets() {
        return oaisets.stream().map(s -> new Set(s)).collect(toList());
    }

    public void addSet(String setName) {
        if (!StringUtil.isEmpty(setName)) {
            oaisets.add(setName);
        }
    }

}
