package edu.harvard.iq.dataverse.makedatacount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * See doc/sphinx-guides/source/admin/make-data-count.rst for user facing docs
 * about Make Data Count. Go read that first.
 *
 * The main issue for initial backend work is
 * https://github.com/IQSS/dataverse/issues/4821
 *
 * The following is a brain dump of additional details from participating in a
 * 2018-10-18 kickoff meeting (notes at
 * https://docs.google.com/document/d/1eM4rAuhmR4ZQxJC_PTE0rq2x7N3aNEjMN7QVvpkY1os/edit?usp=sharing
 * ) and from watching two webinars at https://makedatacount.org/presentations/
 * (MDC Webinar: COUNTER Code of Practice September 13th, 2017 and MDC Webinar:
 * How to Make Your Data Count July 10th, 2018).
 *
 * The recommended starting point to implement Make Data Count is
 * https://github.com/CDLUC3/Make-Data-Count/blob/master/getting-started.md
 * which specifically recommends reading the "COUNTER Code of Practice for
 * Research Data" mentioned in the user facing docs.
 *
 * Make Data Count was first implemented in DASH. Here's an example dataset:
 * https://dash.ucmerced.edu/stash/dataset/doi:10.6071/M3RP49
 *
 * For processing logs we could try DASH's
 * https://github.com/CDLUC3/counter-processor
 *
 * Next, DataOne implemented it, and you can see an example dataset here:
 * https://search.dataone.org/view/doi:10.5063/F1Z899CZ
 *
 * Parts of DataOne are written in Java so perhaps there is some code that can
 * be reused?
 * 
 * On 2019-02-26 we made some decisions at tech hours that we wanted to document
 * in a code comment:
 *
 * 1. If a country cannot be determined, don't persist the metric.
 *
 * 2. We know that an external tool might come along in the future that could
 * come from localhost or a private IP address and we knowingly are not counting
 * these views and downloads because a country cannot be determined.
 */
public class MakeDataCountUtil {

    public static final String LOG_HEADER = "#Fields: event_time	client_ip	session_cookie_id	user_cookie_id	user_id	request_url	identifier	filename	size	user-agent	title	publisher	publisher_id	authors	publication_date	version	other_id	target_url	publication_year\n";

    public enum MetricType {

        VIEWS_TOTAL("viewsTotal"),
        VIEWS_TOTAL_REGULAR("viewsTotalRegular"),
        VIEWS_TOTAL_MACHINE("viewsTotalMachine"),
        VIEWS_UNIQUE("viewsUnique"),
        VIEWS_UNIQUE_REGULAR("viewsUniqueRegular"),
        VIEWS_UNIQUE_MACHINE("viewsUniqueMachine"),
        DOWNLOADS_TOTAL("downloadsTotal"),
        DOWNLOADS_TOTAL_REGULAR("downloadsTotalRegular"),
        DOWNLOADS_TOTAL_MACHINE("downloadsTotalMachine"),
        DOWNLOADS_UNIQUE("downloadsUnique"),
        DOWNLOADS_UNIQUE_REGULAR("downloadsUniqueRegular"),
        DOWNLOADS_UNIQUE_MACHINE("downloadsUniqueMachine"),
        // Technically, "citations" goes to a different API endpoint and database table.
        CITATIONS("citations");

        private final String text;

        private MetricType(final String text) {
            this.text = text;
        }

        public static MetricType fromString(String text) {
            if (text != null) {
                for (MetricType metricType : MetricType.values()) {
                    if (text.equals(metricType.text)) {
                        return metricType;
                    }
                }
            }
            throw new IllegalArgumentException("MetricType must be one of these values: " + Arrays.asList(MetricType.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
    }

    static List<DatasetMetrics> parseSushiReport(JsonObject report) {
        List<DatasetMetrics> datasetMetrics = new ArrayList<>();
        JsonArray reportDatasets = report.getJsonArray("report_datasets");
        for (JsonValue reportDataset : reportDatasets) {
            // TODO: Populate each DatasetMetrics object properly once that entity has settled down.
            //Done in MakeDataCountServiceBean because access to the Dataset Service Bean is required
            datasetMetrics.add(new DatasetMetrics());
        }
        return datasetMetrics;
    }

    public static List<DatasetExternalCitations> parseCitations(JsonObject report) {
        List<DatasetExternalCitations> datasetExternalCitations = new ArrayList<>();
        JsonArray citations = report.getJsonArray("data");
        for (JsonValue citationValue : citations) {
            JsonObject citation = (JsonObject) citationValue;
            String citedByDoi = citation.getJsonObject("attributes").getString("subj-id");
            String occurredAtDate = citation.getJsonObject("attributes").getString("occurred-at");
            System.out.println("cited by " + citedByDoi + " at " + occurredAtDate);
            datasetExternalCitations.add(new DatasetExternalCitations());
        }
        return datasetExternalCitations;
    }

}
