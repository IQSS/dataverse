package edu.harvard.iq.dataverse.harvest.server.xoai;

import io.gdcc.xoai.dataprovider.exceptions.handler.IdDoesNotExistException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.export.ExportService;
import io.gdcc.spi.export.ExportException;
import edu.harvard.iq.dataverse.harvest.server.OAIRecord;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.util.StringUtil;
import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMetadataFormatsException;
import io.gdcc.xoai.dataprovider.repository.ResultsPage;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import io.gdcc.xoai.xml.EchoElement;
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
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.DataverseXoaiItemRepository");
    
    private final OAIRecordServiceBean recordService;
    private final DatasetServiceBean datasetService;
    private final String serverUrl; 

    public DataverseXoaiItemRepository (OAIRecordServiceBean recordService, DatasetServiceBean datasetService, String serverUrl) {
        this.recordService = recordService;
        this.datasetService = datasetService;
        this.serverUrl = serverUrl; 
    }
    
    @Override
    public ItemIdentifier getItemIdentifier(String identifier) throws IdDoesNotExistException {
        // This method is called when ListMetadataFormats request specifies 
        // the identifier, requesting the formats available for this specific record.
        // In our case, under the current implementation, we need to simply look 
        // up if the record exists; if it does, all the OAI formats that we serve
        // should be available for this record. 
        
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsByGlobalId(identifier);
        if (oaiRecords != null && !oaiRecords.isEmpty()) {
            for (OAIRecord oaiRecord : oaiRecords) {
                // We can return the first *active* record we find for this identifier. 
                if (!oaiRecord.isRemoved()) {
                    return new DataverseXoaiItem(oaiRecord);
                }
            }
        }
        
        throw new IdDoesNotExistException();
    }
    
    @Override
    public Item getItem(String identifier, MetadataFormat metadataFormat) throws HandlerException {
        logger.fine("getItem; calling findOaiRecordsByGlobalId, identifier " + identifier);
        
        if (metadataFormat == null) {
            throw new NoMetadataFormatsException("Metadata Format is Required");
        }
        
        List<OAIRecord> oaiRecords = recordService.findOaiRecordsByGlobalId(identifier);
        if (oaiRecords != null && !oaiRecords.isEmpty()) {
            DataverseXoaiItem xoaiItem = null; 
            for (OAIRecord oaiRecord : oaiRecords) {
                if (xoaiItem == null) {
                    xoaiItem = new DataverseXoaiItem(oaiRecord); 
                    xoaiItem = addMetadata(xoaiItem, metadataFormat);
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
    public ResultsPage<ItemIdentifier> getItemIdentifiers(List<ScopedFilter> filters, MetadataFormat metadataFormat, int maxResponseLength, ResumptionToken.Value resumptionToken) throws HandlerException {
        
        return (ResultsPage<ItemIdentifier>)getRepositoryRecords(metadataFormat, maxResponseLength, resumptionToken, false);

    }
 
    @Override
    public ResultsPage<Item> getItems(List<ScopedFilter> filters, MetadataFormat metadataFormat, int maxResponseLength, ResumptionToken.Value resumptionToken) throws HandlerException {
        
        return (ResultsPage<Item>)getRepositoryRecords(metadataFormat, maxResponseLength, resumptionToken, true);
    }
    
    private ResultsPage<? extends ItemIdentifier> getRepositoryRecords (
            MetadataFormat metadataFormat, 
            int maxResponseLength, 
            ResumptionToken.Value resumptionToken,
            boolean fullItems) throws HandlerException {
                
        int offset = Long.valueOf(resumptionToken.getOffset()).intValue();
        String setSpec = resumptionToken.getSetSpec();
        Instant from = resumptionToken.getFrom();
        Instant until = resumptionToken.getUntil();
        
        boolean hasMore = false; 
        
        logger.fine("calling " + (fullItems ? "getItems" : "getItemIdentifiers")
                + "; offset=" + offset
                + ", length=" + maxResponseLength
                + ", setSpec=" + setSpec
                + ", from=" + from
                + ", until=" + until);

        List<OAIRecord> oaiRecords = recordService.findOaiRecordsBySetName(setSpec, from, until);
        
        List<DataverseXoaiItem> xoaiItems = new ArrayList<>();

        if (oaiRecords != null && !oaiRecords.isEmpty()) {
            logger.fine("total " + oaiRecords.size() + " records returned");
            
            for (int i = offset; i < offset + maxResponseLength && i < oaiRecords.size(); i++) {
                OAIRecord record = oaiRecords.get(i);
                DataverseXoaiItem xoaiItem = new DataverseXoaiItem(record);
                
                if (fullItems) {
                    // If we are cooking "full" Items (for the ListRecords verb),
                    // add the metadata to the item object (if not a deleted
                    // record, if available, etc.):
                    xoaiItem = addMetadata(xoaiItem, metadataFormat);
                }
                
                xoaiItems.add(xoaiItem);
            }
            
            // Run a second pass, looking for records in this set that occur
            // in *other* sets. Then we'll add these multiple sets to the 
            // formatted output in the header:
            addExtraSets(xoaiItems, setSpec, from, until);
            
            hasMore = offset + maxResponseLength < oaiRecords.size();
            
            ResultsPage<DataverseXoaiItem> result = new ResultsPage(resumptionToken, hasMore, xoaiItems, oaiRecords.size());
            logger.fine("returning result with " + xoaiItems.size() + " items.");
            return result;
        }

        return new ResultsPage(resumptionToken, false, xoaiItems, 0);
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
    
    private DataverseXoaiItem addMetadata(DataverseXoaiItem xoaiItem, MetadataFormat metadataFormat) {
        // This may be a "deleted" record - i.e., a oaiRecord kept in 
        // the OAI set for a dataset that's no longer in this Dataverse. 
        // (it serves to tell the remote client to delete it from their 
        // holdings too). 
        // If this is the case here, there's nothing we need to do for this item.
        // If not, if it's a live record, let's try to look up the dataset and 
        // open the pre-generated metadata stream.

        if (!xoaiItem.isDeleted()) {
            Dataset dataset = datasetService.findByGlobalId(xoaiItem.getIdentifier());
            if (dataset != null) {
                try {
                    Metadata metadata = getDatasetMetadata(dataset, metadataFormat.getPrefix());
                    xoaiItem.withDataset(dataset).withMetadata(metadata);
                } catch (IOException ex) {
                    // This is not supposed to happen in normal operations; 
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
        return xoaiItem;
    }
    
    private Metadata getDatasetMetadata(Dataset dataset, String metadataPrefix) throws ExportException, IOException {
        Metadata metadata;

        if ("dataverse_json".equals(metadataPrefix)) {
            // Solely for backward compatibility, for older Dataverse harvesting clients
            // that may still be relying on harvesting "dataverse_json";
            // we will want to eventually get rid of this hack! 
            // @Deprecated(since = "5.0")
            metadata = new Metadata(
                    new EchoElement("<dataverse_json>custom metadata</dataverse_json>"))
                    .withAttribute("directApiCall", customDataverseJsonApiUri(dataset.getGlobalId().asString()));
            
        } else {
            InputStream pregeneratedMetadataStream;
            pregeneratedMetadataStream = ExportService.getInstance().getExport(dataset, metadataPrefix);

            metadata = Metadata.copyFromStream(pregeneratedMetadataStream);
        }
        return metadata;
    }
    
    private String customDataverseJsonApiUri(String identifier) {
        String ret = serverUrl  
                + "/api/datasets/export?exporter=dataverse_json&amp;persistentId="
                + identifier;
        
        return ret;
    }
}
