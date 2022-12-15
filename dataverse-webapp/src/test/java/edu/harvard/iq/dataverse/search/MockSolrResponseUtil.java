package edu.harvard.iq.dataverse.search;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MockSolrResponseUtil {

    // -------------------- LOGIC --------------------

    public static QueryResponse createSolrResponse(SolrDocument... documents) {
        return createSolrResponse(documents.length, documents);
    }

    public static QueryResponse createSolrResponse(long numFound, SolrDocument... documents) {
        QueryResponse response = new QueryResponse();
        NamedList<Object> entries = new NamedList<>();
        SolrDocumentList solrDocuments = new SolrDocumentList();
        solrDocuments.addAll(Arrays.asList(documents));
        entries.add("response", solrDocuments);
        solrDocuments.setNumFound(numFound);
        response.setResponse(entries);
        return response;
    }

    @SafeVarargs
    public static SolrDocument document(Tuple2<String, Object>... fields) {
        return new SolrDocument(Arrays.stream(fields)
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2)));
    }

    public static Tuple2<String, Object> field(String name, Object value) {
        return Tuple.of(name, value);
    }
}
