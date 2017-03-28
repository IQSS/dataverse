/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server.xoai;

import edu.harvard.iq.dataverse.harvest.server.xoai.Xitem;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.ItemIdentifier;
import com.lyncode.xoai.dataprovider.repository.ItemRepository;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import static java.lang.Math.min;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import static java.lang.Math.min;
import static java.lang.Math.min;
import static java.lang.Math.min;

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
        logger.fine("getItem; calling findOAIRecordBySetNameandGlobalId, identifier " + identifier);
        OAIRecord oaiRecord = recordService.findOAIRecordBySetNameandGlobalId(null, identifier);
        if (oaiRecord != null) {
            Dataset dataset = datasetService.findByGlobalId(oaiRecord.getGlobalId());
            if (dataset != null) {
                return new Xitem(oaiRecord).withDataset(dataset);
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
            boolean hasMore = offset + length < oaiRecords.size();
            ListItemsResults result = new ListItemsResults(hasMore, xoaiItems);
            logger.fine("returning result with " + xoaiItems.size() + " items.");
            return result;
        }

        return new ListItemsResults(false, xoaiItems);
    }
}
