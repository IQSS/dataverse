package edu.harvard.iq.dataverse.harvest.server.xoai;


import com.lyncode.builder.Builder;
import com.lyncode.xoai.dataprovider.exceptions.*;
import com.lyncode.xoai.dataprovider.handlers.*;
import com.lyncode.xoai.exceptions.InvalidResumptionTokenException;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.model.oaipmh.OAIPMH;
import com.lyncode.xoai.model.oaipmh.Request;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import com.lyncode.xoai.dataprovider.parameters.OAIRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.services.api.DateProvider;
import com.lyncode.xoai.services.impl.UTCDateProvider;
import org.apache.log4j.Logger;

import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.*;

/**
 *
 * @author Leonid Andreev
 */
public class XdataProvider {
    private static Logger log = Logger.getLogger(XdataProvider.class);

    public static XdataProvider dataProvider (Context context, Repository repository) {
        return new XdataProvider(context, repository);
    }

    private Repository repository;
    private DateProvider dateProvider;

    private final IdentifyHandler identifyHandler;
    private final XgetRecordHandler getRecordHandler;
    private final ListSetsHandler listSetsHandler;
    private final XlistRecordsHandler listRecordsHandler;
    private final ListIdentifiersHandler listIdentifiersHandler;
    private final ListMetadataFormatsHandler listMetadataFormatsHandler;
    private final ErrorHandler errorsHandler;

    public XdataProvider (Context context, Repository repository) {
        this.repository = repository;
        this.dateProvider = new UTCDateProvider();

        this.identifyHandler = new IdentifyHandler(context, repository);
        this.listSetsHandler = new ListSetsHandler(context, repository);
        this.listMetadataFormatsHandler = new ListMetadataFormatsHandler(context, repository);
        this.listRecordsHandler = new XlistRecordsHandler(context, repository);
        this.listIdentifiersHandler = new ListIdentifiersHandler(context, repository);
        //this.getRecordHandler = new GetRecordHandler(context, repository);
        this.getRecordHandler = new XgetRecordHandler(context, repository);
        this.errorsHandler = new ErrorHandler();
    }

    public OAIPMH handle (Builder<OAIRequest> builder) throws OAIException {
        return handle(builder.build());
    }

    public OAIPMH handle (OAIRequest requestParameters) throws OAIException {
        log.debug("Handling OAI request");
        Request request = new Request(repository.getConfiguration().getBaseUrl())
                .withVerbType(requestParameters.get(Verb))
                .withResumptionToken(requestParameters.get(ResumptionToken))
                .withIdentifier(requestParameters.get(Identifier))
                .withMetadataPrefix(requestParameters.get(MetadataPrefix))
                .withSet(requestParameters.get(Set))
                .withFrom(requestParameters.get(From))
                .withUntil(requestParameters.get(Until));

        OAIPMH response = new OAIPMH()
                .withRequest(request)
                .withResponseDate(dateProvider.now());
        try {
            OAICompiledRequest parameters = compileParameters(requestParameters);

            switch (request.getVerbType()) {
                case Identify:
                    response.withVerb(identifyHandler.handle(parameters));
                    break;
                case ListSets:
                    response.withVerb(listSetsHandler.handle(parameters));
                    break;
                case ListMetadataFormats:
                    response.withVerb(listMetadataFormatsHandler.handle(parameters));
                    break;
                case GetRecord:
                    response.withVerb(getRecordHandler.handle(parameters));
                    break;
                case ListIdentifiers:
                    response.withVerb(listIdentifiersHandler.handle(parameters));
                    break;
                case ListRecords:
                    response.withVerb(listRecordsHandler.handle(parameters));
                    break;
            }
        } catch (HandlerException e) {
            log.debug(e.getMessage(), e);
            response.withError(errorsHandler.handle(e));
        }

        return response;
    }

    private OAICompiledRequest compileParameters(OAIRequest requestParameters) throws IllegalVerbException, UnknownParameterException, BadArgumentException, DuplicateDefinitionException, BadResumptionToken {
        try {
            return requestParameters.compile();
        } catch (InvalidResumptionTokenException e) {
            throw new BadResumptionToken("The resumption token is invalid");
        }
    }    
    
}
