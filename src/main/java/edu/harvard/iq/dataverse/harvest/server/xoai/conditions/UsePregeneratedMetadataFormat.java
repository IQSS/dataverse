package edu.harvard.iq.dataverse.harvest.server.xoai.conditions;

import io.gdcc.xoai.dataprovider.filter.Filter;
import io.gdcc.xoai.dataprovider.filter.FilterResolver;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.model.conditions.Condition;

/**
 * The purpose of this Condition is to pass the MetadataFormat to the 
 * getItems() methods in the Dataverse ItemRepository, as part of a 
 * ScopedFilter. 
 * 
 * @author Leonid Andreev
 */
public class UsePregeneratedMetadataFormat implements Condition {

    public UsePregeneratedMetadataFormat() {
        alwaysTrueFilter = new Filter() {
            @Override
            public boolean isItemShown(ItemIdentifier item) {
                return true;
            }
        };
    }
    
    private final Filter alwaysTrueFilter;
    
    @Override
    public Filter getFilter(FilterResolver filterResolver) {
        return alwaysTrueFilter;
    }
    
    private MetadataFormat metadataFormat;
    
    public void withMetadataFormat(MetadataFormat metadataFormat) {
        this.metadataFormat = metadataFormat;
    }
    
    public MetadataFormat getMetadataFormat() {
        return metadataFormat; 
    }
}
