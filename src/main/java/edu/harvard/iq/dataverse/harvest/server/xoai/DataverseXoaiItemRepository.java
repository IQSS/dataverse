package edu.harvard.iq.dataverse.harvest.server.xoai;

import io.gdcc.xoai.dataprovider.exceptions.IdDoesNotExistException;
import io.gdcc.xoai.dataprovider.exceptions.OAIException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import io.gdcc.xoai.dataprovider.handlers.results.ListItemsResults;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.harvest.server.xoai.conditions.UsePregeneratedMetadataFormat;
import edu.harvard.iq.dataverse.util.StringUtil;
import io.gdcc.xoai.dataprovider.filter.Scope;
import io.gdcc.xoai.dataprovider.model.conditions.Condition;
import io.gdcc.xoai.model.oaipmh.Metadata;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 * Implements an XOAI "Item Repository". Retrieves Dataverse "OAIRecords" 
 * representing harvestable local datasets and translates them into 
 * XOAI "items". 
 */

public class DataverseXoaiItemRepository implements ItemRepository {
    private static Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.XitemRepository");
    
    private OAIRecordServiceBean recordService;
    private DatasetServiceBean datasetService;

    public DataverseXoaiItemRepository (OAIRecordServiceBean recordService, DatasetServiceBean datasetService) {
        super();
        this.recordService = recordService;
        this.datasetService = datasetService;
    }
    
    private List<DataverseXoaiItem> list = new ArrayList<DataverseXoaiItem>();


    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException, OAIException {
        // I'm assuming we don't want to use this version of getItem 
        // that does not specify the requested metadata format - ? 
        throw new OAIException("Metadata Format is Required");
    }
    
    @Override
    public Item getItem(String identifier, MetadataFormat metadataFormat) throws IdDoesNotExistException, OAIException {
        logger.fine("getItem; calling findOaiRecordsByGlobalId, identifier " + identifier);
        
        if (metadataFormat == null) {
            throw new OAIException("Metadata Format is Required");
        }
        
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsByGlobalId(identifier);
        if (oaiRecords != null && !oaiRecords.isEmpty()) {
            DataverseXoaiItem xoaiItem = null; 
            for (OAIRecord oaiRecord : oaiRecords) {
                if (xoaiItem == null) {
                    xoaiItem = new DataverseXoaiItem(oaiRecord); 
                    
                    // If this is a "deleted" OAI oaiRecord - i.e., if someone
                    // has called GetRecord on a deleted oaiRecord (??), our 
                    // job here is done. If it's a live oaiRecord, let's try to 
                    // look up the dataset and open the pre-generated metadata 
                    // stream. 
                    
                    if (!oaiRecord.isRemoved()) {
                        Dataset dataset = datasetService.findByGlobalId(oaiRecord.getGlobalId());
                        if (dataset == null) {
                            // This should not happen - but if there are no longer datasets 
                            // associated with this persistent identifier, we should simply 
                            // bail out. 
                            // TODO: Consider an alternative - instead of throwing 
                            // an IdDoesNotExist exception, mark the oaiRecord as 
                            // "deleted" and serve it to the client (?). For all practical
                            // purposes, this is what this oaiRecord represents - it's 
                            // still in the database as part of an OAI set; but the 
                            // corresponding dataset no longer exists, because it 
                            // must have been deleted. 
                            // i.e.
                            // xoaiItem.getOaiRecord().setRemoved(true);
                            break;
                        }

                        InputStream pregeneratedMetadataStream;
                        try {
                            pregeneratedMetadataStream = ExportService.getInstance().getExport(dataset, metadataFormat.getPrefix());
                        } catch (ExportException | IOException ex) {
                            // Again, this is not supposed to happen in normal operations; 
                            // since by design only the datasets for which the metadata
                            // records have been pre-generated ("exported") should be 
                            // served as "OAI Record". But, things happen. If for one
                            // reason or another that cached metadata file is no longer there, 
                            // we are not going to serve this oaiRecord. 
                            // TODO: see the comment above; and consider
                            // xoaiItem.getOaiRecord().setRemoved(true);
                            // instead. 
                            break;
                        }

                        Metadata metadata = Metadata.copyFromStream(pregeneratedMetadataStream);
                        xoaiItem.withDataset(dataset).withMetadata(metadata);
                    }
                } else {
                    // Adding extra set specs to the XOAI Item, if this oaiRecord
                    // is part of multiple sets:
                    if (!StringUtil.isEmpty(oaiRecord.getSetName())) {
                        xoaiItem.getSets().add(new Set(oaiRecord.getSetName()));
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
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Instant from) throws OAIException {
        return getItemIdentifiers(filters, offset, length, null, from, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, Instant until) throws OAIException {
        return getItemIdentifiers(filters, offset, length, null, null, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Instant from, Instant until) throws OAIException {
        return getItemIdentifiers(filters, offset, length, null, from, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItemIdentifiers(filters, offset, length, setSpec, null, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Instant from) throws OAIException {
        return getItemIdentifiers(filters, offset, length, setSpec, from, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Instant until) throws OAIException {
        return getItemIdentifiers(filters, offset, length, setSpec, null, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Instant from, Instant until) throws OAIException {
        logger.fine("calling getItemIdentifiers; offset=" + offset
                + ", length=" + length
                + ", setSpec=" + setSpec
                + ", from=" + from
                + ", until=" + until);

        List<OAIRecord> oaiRecords = recordService.findOaiRecordsBySetName(setSpec, from, until);

        //logger.fine("total " + oaiRecords.size() + " returned");

        List<ItemIdentifier> xoaiItems = new ArrayList<>();
        if (oaiRecords != null && !oaiRecords.isEmpty()) {

            for (int i = offset; i < offset + length && i < oaiRecords.size(); i++) {
                OAIRecord record = oaiRecords.get(i);
                xoaiItems.add(new DataverseXoaiItem(record));
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
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Instant from) throws OAIException {
        return getItems(filters, offset, length, null, from, null);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, Instant until) throws OAIException {
        return getItems(filters, offset, length, null, null, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Instant from, Instant until) throws OAIException {
        return getItems(filters, offset, length, null, from, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItems(filters, offset, length, setSpec, null, null);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Instant from) throws OAIException {
        return getItems(filters, offset, length, setSpec, from, null);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Instant until) throws OAIException {
        return getItems(filters, offset, length, setSpec, null, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Instant from, Instant until) throws OAIException {
        logger.info("calling getItems; offset=" + offset
                + ", length=" + length
                + ", setSpec=" + setSpec
                + ", from=" + from
                + ", until=" + until);

        // TODO:?/WORKINPROGRESS:
        // we need to know the MetadataFormat requested, in 
        // order to look up the pre-generated metadata stream
        // and create a CopyElement Metadata object out of it.
        // In the current implementation this is solved by encoding the 
        // MetadataFormat in a custom Condition, which results in it being 
        // passed to the getItems() method as a ScopedFilter. 
        // (or should we simply offer versions of all these methods,
        // with the extra MetadataFormat argument, on the gdcc.xoai side, 
        // like it was done with getItem() above?
        
        MetadataFormat metadataFormat = null; 
        
        for (ScopedFilter f : filters) {
            
            if (f.getScope().equals(Scope.MetadataFormat)) {
                logger.fine("found metadata-scoped filter");
                Condition condition = f.getCondition();
                if (condition instanceof UsePregeneratedMetadataFormat) {
                    logger.fine("found pregenerated metadata condition");
                    metadataFormat = ((UsePregeneratedMetadataFormat) condition).getMetadataFormat();
                    break;
                }
            }
        }
        
        if (metadataFormat == null) {
            // we should throw a "cannot dissiminate format" (?) exception here; 
            // but let's do this for now: 
            metadataFormat =  MetadataFormat.metadataFormat("oai_dc");
        }
        
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsBySetName(setSpec, from, until);

        logger.info("total " + oaiRecords.size() + " returned");

        List<Item> xoaiItems = new ArrayList<>();
        if (oaiRecords != null && !oaiRecords.isEmpty()) {

            for (int i = offset; i < offset + length && i < oaiRecords.size(); i++) {
                OAIRecord oaiRecord = oaiRecords.get(i);
                
                DataverseXoaiItem xoaiItem = new DataverseXoaiItem(oaiRecord); 
                
                // This may be a "deleted" OAI oaiRecord - i.e., a oaiRecord kept in 
                // the OAI set for a dataset that's no longer in this Dataverse. 
                // (it serves to tell the remote client to delete it from their 
                // holdings too). 
                // If this is the case here, our job is done with this oaiRecord.
                // If not, if it's a live oaiRecord, let's try to 
                // look up the dataset and open the pre-generated metadata 
                // stream.
                
                if (!oaiRecord.isRemoved()) {
                    Dataset dataset = datasetService.findByGlobalId(oaiRecord.getGlobalId());
                    if (dataset != null) {
                    
                        InputStream pregeneratedMetadataStream; 
                        try {
                            pregeneratedMetadataStream = ExportService.getInstance().getExport(dataset, metadataFormat.getPrefix());
                            
                            Metadata metadata = Metadata.copyFromStream(pregeneratedMetadataStream);
                            xoaiItem.withDataset(dataset).withMetadata(metadata);
                        } catch (ExportException|IOException ex) {
                            // Again, this is not supposed to happen in normal operations; 
                            // since by design only the datasets for which the metadata
                            // records have been pre-generated ("exported") should be 
                            // served as "OAI Record". But, things happen. If for one
                            // reason or another that cached metadata file is no longer there, 
                            // we are not going to serve any metadata for this oaiRecord, 
                            // BUT we are going to include it marked as "deleted"
                            // (because skipping it could potentially mess up the
                            // counts and offsets, in a resumption token scenario.
                            xoaiItem.getOaiRecord().setRemoved(true);
                        }
                    } else {
                        // If dataset (somehow) no longer exists (again, this is 
                        // not supposed to happen), we will serve the oaiRecord, 
                        // marked as "deleted" and without any metadata. 
                        // We can't just skip it, because that could mess up the
                        // counts and offsets, in a resumption token scenario.
                        xoaiItem.getOaiRecord().setRemoved(true);
                    }
                }
                xoaiItems.add(xoaiItem);
            }
            
            addExtraSets(xoaiItems, setSpec, from, until);
            
            boolean hasMore = offset + length < oaiRecords.size();
            ListItemsResults result = new ListItemsResults(hasMore, xoaiItems);
            logger.fine("returning result with " + xoaiItems.size() + " items.");
            return result;
        }

        return new ListItemsResults(false, xoaiItems);
    }
    
    private void addExtraSets(Object xoaiItemsList, String setSpec, Instant from, Instant until) {
        
        List<DataverseXoaiItem> xoaiItems = (List<DataverseXoaiItem>)xoaiItemsList;
        
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsNotInThisSet(setSpec, from, until);
        
        if (oaiRecords == null || oaiRecords.isEmpty()) {
            return;
        }
                
        // Make a second pass through the list of xoaiItems already found for this set, 
        // and add any other sets in which this item occurs:
        
        int j = 0;
        for (int i = 0; i < xoaiItems.size(); i++) {
            // fast-forward the second list, until we find a oaiRecord with this identifier, 
            // or until we are past this oaiRecord (both lists are sorted alphabetically by
            // the identifier:
            DataverseXoaiItem xitem = xoaiItems.get(i);
            
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
