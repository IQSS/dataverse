/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import cn.edu.pku.lib.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author luopc
 */
@Stateless
public class UsageSearchServiceBean {
    
    private static final Logger logger = Logger.getLogger(UsageSearchServiceBean
            .class.getCanonicalName());
    
    @EJB
    SystemConfig systemConfig;
    private JestClient client;
    
    @PostConstruct
    public void init(){
        JestClientFactory factory = new JestClientFactory();
//        String elasticSearchUrl = "http://" + systemConfig.getElasticSearchHostColonPort();
        String elasticSearchUrl = "http://localhost:9200";
        factory.setHttpClientConfig(new HttpClientConfig.Builder(elasticSearchUrl)
                .multiThreaded(true)
                .build());
        client = factory.getObject();
    }
    
    @PreDestroy
    public void close(){
        if(client != null){
            client.shutdownClient();
            client = null;
        }
    }
    
    public UsageSearchResult search(UsageSearchQuery searchQuery) {
        String query = getQuery(searchQuery);
        Search search = new Search.Builder(query)
                .addIndex(UsageConstant.INDEX_NAME)
                .addType(UsageConstant.INDEX_TYPE)
                .build();

        SearchResult result;
        UsageSearchResult usageResult = new UsageSearchResult();
        try {
            result = client.execute(search);
            if (result != null) {
                List<SearchResult.Hit<Event, Void>> hits = result.getHits(Event.class);
                usageResult.setTotal(result.getTotal());
                List<Event> events = new ArrayList<>(hits.size());
                for (SearchResult.Hit<Event, Void> hit : hits) {
                    events.add(hit.source);
                }
                usageResult.setEvents(events);
                usageResult.setTotalPages((int) ((result.getTotal() - 1) / searchQuery.getSize() + 1));
                usageResult.setCurrentPage((int) (searchQuery.getFrom() / searchQuery.getSize() + 1));
            }
            
            MetricAggregation aggregation = result.getAggregations();
            DateHistogramAggregation dateHistogram = aggregation.getDateHistogramAggregation("event_over_time");
            List<String> histogramX = new ArrayList<>();
            List<Long> histogramY = new ArrayList<>();
            if (dateHistogram != null && dateHistogram.getBuckets().size() > 0) {
                for (DateHistogramAggregation.DateHistogram unit : dateHistogram.getBuckets()) {
                    histogramX.add(unit.getTimeAsString()+ " GMT" +getTimeZone());
                    histogramY.add(unit.getCount());
                }
            }
            usageResult.setHistogramX(histogramX);
            usageResult.setHistogramY(histogramY);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return usageResult;
    }
    
    private String getQuery(UsageSearchQuery query) {
        List<String> subQuerys = new ArrayList<>();
        //add from and size for query
        if (query.getFrom() != null && query.getFrom() >= 0) {
            subQuerys.add("\"from\":" + query.getFrom());
        }
        if (query.getSize() != null && query.getSize() >= 0) {
            subQuerys.add("\"size\":" + query.getSize());
        }
        //add data histogram aggregation for query
        String queryHistogram = getDateHistogramAggregation(query.getDateHistogramInterval());
        if(queryHistogram != null)subQuerys.add(queryHistogram);
        //add filters for query
        subQuerys.add(getFilteredQuery(query));
        //add sort for query
        subQuerys.add(getSortByDate());

        return "{"+StringUtil.listToString(subQuerys, ",")+"}";
    }
    
    private String getFilteredQuery(UsageSearchQuery query){
        if ((query.getEventTypes()== null || query.getEventTypes().isEmpty()) &&
                query.getIp() == null && query.getUserId() == null &&
                query.getUserName() == null && query.getAffiliation() == null &&
                query.getPosition() == null && 
                query.getStartTime() == null && query.getEndTime() == null &&
                (query.getDataverseIds() == null || query.getDataverseIds().isEmpty()) &&
                (query.getDatasetIds() == null || query.getDatasetIds().isEmpty()) &&
                (query.getDatafileIds() == null || query.getDatafileIds().isEmpty()) &&
                (query.getGroupIds() == null || query.getGroupIds().isEmpty())) {
            return null;
        }
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("\"query\":{\"filtered\":{\"filter\": {\"bool\":{");
        
        List<String> clauses = new ArrayList<>();
        //If the eventTypes is specified, the return event must has one of the
        //EventType specified in the eventTypes. 
        if(query.getEventTypes() != null && query.getEventTypes().size() > 0){
            clauses.add(getSubClauseForMustBoolQuery(
                    getShouldBoolQuery("eventType", query.getEventTypes())));
        }
        //If the ip is specified, the return event must have the specified ip.
        if(query.getIp() != null){
            clauses.add(getSubClauseForMustBoolQuery(getTermQuery("ip",
                    StringUtil.jsonEscape(query.getIp()))));
        }
        //must have is specified
        if(query.getUserId() != null){
            clauses.add(getSubClauseForMustBoolQuery(getTermQuery("userId",
                    StringUtil.jsonEscape(query.getUserId()))));
        }
        if(query.getUserName() != null){
            clauses.add(getSubClauseForMustBoolQuery(getTermQuery("userName",
                    StringUtil.jsonEscape(query.getUserName()))));
        }
        if(query.getAffiliation() != null){
            clauses.add(getSubClauseForMustBoolQuery(getTermQuery("affiliation",
                    StringUtil.jsonEscape(query.getAffiliation()))));
        }
        if(query.getPosition() != null){
            clauses.add(getSubClauseForMustBoolQuery(getTermQuery("position",
                    StringUtil.jsonEscape(query.getPosition()))));
        }
        String rangeQuery = getRangeQuery(query.getStartTime(), query.getEndTime());
        if(rangeQuery != null){
            clauses.add(getSubClauseForMustBoolQuery(rangeQuery));
        }
        //Must have one if specified
        if(query.getDataverseIds() != null && query.getDataverseIds().size() > 0){
            clauses.add(getSubClauseForMustBoolQuery(
                    getShouldBoolQuery("dataverseId", query.getDataverseIds())));
        }
        if(query.getDatasetIds() != null && query.getDatasetIds().size() > 0){
            clauses.add(getSubClauseForMustBoolQuery(
                    getShouldBoolQuery("datasetId", query.getDatasetIds())));
        }
        if(query.getDatafileIds()!= null && query.getDatafileIds().size() > 0){
            clauses.add(getSubClauseForMustBoolQuery(
                    getShouldBoolQuery("datafileId", query.getDatafileIds())));
        }
        if(query.getGroupIds()!=null && query.getGroupIds().size() > 0){
            clauses.add(getSubClauseForMustBoolQuery(
                    getShouldBoolQuery("groupId", query.getGroupIds())));
        }
        queryBuilder.append(StringUtil.listToString(clauses, ","));
        queryBuilder.append("}}}}");
        return queryBuilder.toString();
    }
    
    private String getShouldBoolQuery(String field, List list){
        List<String> subClauses = new ArrayList<>(list.size());
        for (Object obj : list) {
            subClauses.add(getSubClauseForShouldBoolQuery(
                    getTermQuery(field, obj.toString())));
        }
        return getBoolQuery(subClauses);
    }
    
    private String getTermQuery(String field, String value){
        return "{\"term\":{\""+field+"\":\"" + value + "\"}}";
    }
    
    private String getBoolQuery(List<String> subClausesForBoolQuery){
        return "{\"bool\":{" + StringUtil.listToString(subClausesForBoolQuery,",")
                + "}}";
    }
    
    private String getSubClauseForMustBoolQuery(String query){
        return "\"must\":" + query;
    }
    
    private String getSubClauseForShouldBoolQuery(String query){
        return "\"should\":"+query;
    }
    
    private String getRangeQuery(Date start, Date end){
        if(start == null && end == null)return null;
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<String> arrays = new ArrayList<>();
        if(start != null)
            arrays.add("\"gte\":\""+format.format(start)+"\"");
        if(end != null)
            arrays.add("\"lte\":\""+format.format(end)+"\"");
        arrays.add("\"format\":\"yyyy-MM-dd HH:mm:ss\"");
        arrays.add("\"time_zone\": \""+getTimeZone()+"\"");
        String range = "{\"range\":{\"date\":{"+StringUtil.listToString(arrays, ",")+"}}}";
        return range;
    }
    
    private String getTimeZone(){
        long hours = TimeUnit.MILLISECONDS.toHours(TimeZone.getDefault().getRawOffset());
        if(hours > 0)
            return String.format("+%02d00", hours);
        else
            return String.format("-%02d00", -hours);
    }

    private String getDateHistogramAggregation(UsageSearchQuery.DateHistogramInterval interval) {
        if (interval == null) {
            return null;
        }
        String str = "\"aggs\":{\"event_over_time\":{\"date_histogram\":{\"field\":\"date\",\"interval\":\"%s\",\"format\" : \"%s\",\"time_zone\":\"%s\"}}}";
        switch (interval) {
            case YEAR:
                return String.format(str, "year","yyyy",getTimeZone());
            case MONTH:
                return String.format(str, "month","MM-dd-yyyy",getTimeZone());
            case DAY:
                return String.format(str, "day","MM-dd-yyyy",getTimeZone());
            case HOUR:
                return String.format(str, "hour","MM-dd-yyyy hh",getTimeZone());
            default:
                return String.format(str, "day","MM-dd-yyyy",getTimeZone());
        }
    }
    
    private String getSortByDate(){
        return "\"sort\" : [{\"date\" : {\"order\" : \"desc\"}}]";
    }
    
}
