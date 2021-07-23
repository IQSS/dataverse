/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.filter.ScopedFilter;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.handlers.results.ListItemsResults;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.ItemIdentifier;
import org.dspace.xoai.dataprovider.repository.ItemRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Leonid Andreev
 * Implements an XOAI "Item Repository". Retrieves Dataverse "OAIRecords"
 * representing harvestable local datasets and translates them into
 * XOAI "items".
 */

public class XitemRepository implements ItemRepository {
    private static Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.XitemRepository");

    private OAIRecordServiceBean recordService;
    private DatasetDao datasetDao;

    public XitemRepository(OAIRecordServiceBean recordService, DatasetDao datasetDao) {
        super();
        this.recordService = recordService;
        this.datasetDao = datasetDao;
    }


    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException, OAIException {
        logger.fine("getItem; calling findOaiRecordsByGlobalId, identifier " + identifier);
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsByGlobalId(identifier);
        if (oaiRecords.isEmpty()) {
            throw new IdDoesNotExistException();
        }
        
        Dataset dataset = datasetDao.findByGlobalId(identifier);
        if (dataset == null) {
            throw new IdDoesNotExistException();
        }
        
        boolean removed = oaiRecords.stream().allMatch(oaiRecord -> oaiRecord.isRemoved());
        Date lastUpdateTimestamp = oaiRecords.get(0).getLastUpdateTime();
        
        Xitem xoaiItem = new Xitem(identifier, lastUpdateTimestamp, removed)
                .withDataset(dataset);

        oaiRecords.forEach(record -> xoaiItem.addSet(record.getSetName()));

        return xoaiItem;
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
        if (!oaiRecords.isEmpty()) {

            for (int i = offset; i < offset + length && i < oaiRecords.size(); i++) {
                OAIRecord record = oaiRecords.get(i);
                Xitem xItem = new Xitem(record.getGlobalId(), record.getLastUpdateTime(), record.isRemoved());

                xoaiItems.add(xItem);
            }

            // Run a second pass, looking for records in this set that occur
            // in *other* sets. Then we'll add these multiple sets to the
            // formatted output in the header:
            addExtraSets(xoaiItems);

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
        if (!oaiRecords.isEmpty()) {

            for (int i = offset; i < offset + length && i < oaiRecords.size(); i++) {
                OAIRecord oaiRecord = oaiRecords.get(i);
                Dataset dataset = datasetDao.findByGlobalId(oaiRecord.getGlobalId());
                if (dataset != null) {
                    Xitem xItem = new Xitem(oaiRecord.getGlobalId(), oaiRecord.getLastUpdateTime(), oaiRecord.isRemoved())
                            .withDataset(dataset);
                    xoaiItems.add(xItem);
                }
            }

            addExtraSets(xoaiItems);

            boolean hasMore = offset + length < oaiRecords.size();
            ListItemsResults result = new ListItemsResults(hasMore, xoaiItems);
            logger.fine("returning result with " + xoaiItems.size() + " items.");
            return result;
        }

        return new ListItemsResults(false, xoaiItems);
    }

    private void addExtraSets(List<? extends ItemIdentifier> xoaiItemsList) {

        Map<String, Xitem> xoaiItemsMap = new HashMap<>();
        xoaiItemsList.forEach(item -> xoaiItemsMap.put(item.getIdentifier(), (Xitem)item));

        List<OAIRecord> oaiRecords = recordService.findOaiRecordsByGlobalIds(new ArrayList<>(xoaiItemsMap.keySet()));

        oaiRecords.forEach(oaiRecord -> xoaiItemsMap.get(oaiRecord.getGlobalId()).addSet(oaiRecord.getSetName()));

    }
}
