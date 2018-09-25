/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.ItemIdentifier;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.dataprovider.repository.ItemRepository;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 * Implements an XOAI "Item Repository". Retrieves Dataverse "OAIRecords" 
 * representing harvestable local datasets and translates them into 
 * XOAI "items". 
 */

public class XitemRepository implements ItemRepository {
    private static Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.XitemRepository");
    
    private OAIRecordServiceBean recordService;
    private DatasetServiceBean datasetService;

    public XitemRepository (OAIRecordServiceBean recordService, DatasetServiceBean datasetService) {
        super();
        this.recordService = recordService;
        this.datasetService = datasetService;
    }
    
    private List<Xitem> list = new ArrayList<Xitem>();


    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException, OAIException {
        logger.fine("getItem; calling findOaiRecordsByGlobalId, identifier " + identifier);
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsByGlobalId(identifier);
        if (oaiRecords != null && !oaiRecords.isEmpty()) {
            Xitem xoaiItem = null; 
            for (OAIRecord record : oaiRecords) {
                if (xoaiItem == null) {
                    Dataset dataset = datasetService.findByGlobalId(record.getGlobalId());
                    if (dataset != null) {
                        xoaiItem = new Xitem(record).withDataset(dataset);
                    }
                } else {
                    // Adding extra set specs to the XOAI Item, if this record
                    // is part of multiple sets:
                    if (!StringUtil.isEmpty(record.getSetName())) {
                        xoaiItem.getSets().add(new Set(record.getSetName()));
                    }
                }
            }
            if (xoaiItem != null) {
                return xoaiItem;
            }
        }

        throw new IdDoesNotExistException();
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length) throws OAIException {
        return getItemIdentifiers(filters, offset, length, null, null, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return getItemIdentifiers(filters, offset, length, null, from, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return getItemIdentifiers(filters, offset, length, null, null, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return getItemIdentifiers(filters, offset, length, null, from, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItemIdentifiers(filters, offset, length, setSpec, null, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItemIdentifiers(filters, offset, length, setSpec, from, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItemIdentifiers(filters, offset, length, setSpec, null, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        logger.fine("calling getItemIdentifiers; offset=" + offset
                + ", length=" + length
                + ", setSpec=" + setSpec
                + ", from=" + from
                + ", until=" + until);

        List<OAIRecord> oaiRecords = recordService.findOaiRecordsBySetName(setSpec, from, until);

        logger.fine("total " + oaiRecords.size() + " returned");

        List<ItemIdentifier> xoaiItems = new ArrayList<>();
        if (oaiRecords != null && !oaiRecords.isEmpty()) {

            for (int i = offset; i < offset + length && i < oaiRecords.size(); i++) {
                OAIRecord record = oaiRecords.get(i);
                xoaiItems.add(new Xitem(record));
            }
            
            // Run a second pass, looking for records in this set that occur
            // in *other* sets. Then we'll add these multiple sets to the 
            // formatted output in the header:
            addExtraSets(xoaiItems, setSpec, from, until);
            
            boolean hasMore = offset + length < oaiRecords.size();
            ListItemIdentifiersResult result = new ListItemIdentifiersResult(hasMore, xoaiItems);
            logger.fine("returning result with " + xoaiItems.size() + " items.");
            return result;
        }

        return new ListItemIdentifiersResult(false, xoaiItems);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length) throws OAIException {
        return getItems(filters, offset, length, null, null, null);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return getItems(filters, offset, length, null, from, null);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return getItems(filters, offset, length, null, null, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return getItems(filters, offset, length, null, from, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItems(filters, offset, length, setSpec, null, null);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItems(filters, offset, length, setSpec, from, null);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItems(filters, offset, length, setSpec, null, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        logger.fine("calling getItems; offset=" + offset
                + ", length=" + length
                + ", setSpec=" + setSpec
                + ", from=" + from
                + ", until=" + until);

        List<OAIRecord> oaiRecords = recordService.findOaiRecordsBySetName(setSpec, from, until);

        logger.fine("total " + oaiRecords.size() + " returned");

        List<Item> xoaiItems = new ArrayList<>();
        if (oaiRecords != null && !oaiRecords.isEmpty()) {

            for (int i = offset; i < offset + length && i < oaiRecords.size(); i++) {
                OAIRecord oaiRecord = oaiRecords.get(i);
                Dataset dataset = datasetService.findByGlobalId(oaiRecord.getGlobalId());
                if (dataset != null) {
                    Xitem xItem = new Xitem(oaiRecord).withDataset(dataset);
                    xoaiItems.add(xItem);
                }
            }
            
            addExtraSets(xoaiItems, setSpec, from, until);
            
            boolean hasMore = offset + length < oaiRecords.size();
            ListItemsResults result = new ListItemsResults(hasMore, xoaiItems);
            logger.fine("returning result with " + xoaiItems.size() + " items.");
            return result;
        }

        return new ListItemsResults(false, xoaiItems);
    }
    
    private void addExtraSets(Object xoaiItemsList, String setSpec, Date from, Date until) {
        
        List<Xitem> xoaiItems = (List<Xitem>)xoaiItemsList;
        
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsNotInThisSet(setSpec, from, until);
        
        if (oaiRecords == null || oaiRecords.isEmpty()) {
            return;
        }
                
        // Make a second pass through the list of xoaiItems already found for this set, 
        // and add any other sets in which this item occurs:
        
        int j = 0;
        for (int i = 0; i < xoaiItems.size(); i++) {
            // fast-forward the second list, until we find a record with this identifier, 
            // or until we are past this record (both lists are sorted alphabetically by
            // the identifier:
            Xitem xitem = xoaiItems.get(i);
            
            while (j < oaiRecords.size() && xitem.getIdentifier().compareTo(oaiRecords.get(j).getGlobalId()) > 0) {
                j++;
            }
            
            while (j < oaiRecords.size() && xitem.getIdentifier().equals(oaiRecords.get(j).getGlobalId())) {
                xoaiItems.get(i).getSets().add(new Set(oaiRecords.get(j).getSetName()));
                j++;
            }
        }
                
    }
}
