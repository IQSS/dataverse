package edu.harvard.iq.dataverse.harvest.server.xoai;

import com.lyncode.xml.exceptions.XmlWriteException;
import org.dspace.xoai.dataprovider.exceptions.BadArgumentException;
import org.dspace.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import org.dspace.xoai.dataprovider.exceptions.HandlerException;
import org.dspace.xoai.dataprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.handlers.VerbHandler;
import org.dspace.xoai.dataprovider.handlers.helpers.ItemHelper;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.MetadataFormat;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.dataprovider.parameters.OAICompiledRequest;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.model.oaipmh.GetRecord;
import org.dspace.xoai.model.oaipmh.Header;
import org.dspace.xoai.model.oaipmh.Metadata;
import org.dspace.xoai.xml.XSLPipeline;
import org.dspace.xoai.xml.XmlWriter;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

/*
 * @author Leonid Andreev
 */
public class XgetRecordHandler extends VerbHandler<GetRecord> {
    private static Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.xoai.XgetRecordHandler");

    public XgetRecordHandler(Context context, Repository repository) {
        super(context, repository);
    }

    @Override
    public GetRecord handle(OAICompiledRequest parameters) throws OAIException, HandlerException {

        MetadataFormat format = getContext().formatForPrefix(parameters.getMetadataPrefix());
        Item item = getRepository().getItemRepository().getItem(parameters.getIdentifier());

        if (getContext().hasCondition() &&
                !getContext().getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(item)) {
            throw new IdDoesNotExistException("This context does not include this item");
        }

        if (format.hasCondition() &&
                !format.getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(item)) {
            throw new OAIException("Format not applicable to this item");
        }


        Xrecord record = this.createRecord(parameters, item);
        GetRecord result = new XgetRecord(record);

        return result;
    }

    private Xrecord createRecord(OAICompiledRequest parameters, Item item)
            throws BadArgumentException,
            OAIException, NoMetadataFormatsException, CannotDisseminateFormatException {
        MetadataFormat format = getContext().formatForPrefix(parameters.getMetadataPrefix());
        Header header = new Header();

        Dataset dataset = ((Xitem) item).getDataset();
        Xrecord xrecord = new Xrecord().withFormatName(parameters.getMetadataPrefix()).withDataset(dataset);
        header.withIdentifier(item.getIdentifier());

        ItemHelper itemHelperWrap = new ItemHelper(item);
        header.withDatestamp(item.getDatestamp());
        for (Set set : itemHelperWrap.getSets(getContext(), getRepository().getFilterResolver())) {
            header.withSetSpec(set.getSpec());
        }
        if (item.isDeleted()) {
            header.withStatus(Header.Status.DELETED);
        }

        xrecord.withHeader(header);
        xrecord.withMetadata(item.getMetadata());

        return xrecord;
    }

    private XSLPipeline toPipeline(Item item) throws XmlWriteException, XMLStreamException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(output);
        Metadata metadata = item.getMetadata();
        metadata.write(writer);
        writer.close();
        return new XSLPipeline(new ByteArrayInputStream(output.toByteArray()), true);
    }
}
