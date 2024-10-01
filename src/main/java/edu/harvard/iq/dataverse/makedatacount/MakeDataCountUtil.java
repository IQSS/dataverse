package edu.harvard.iq.dataverse.makedatacount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.persistence.Transient;

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
 * https://github.com/gdcc/Make-Data-Count/blob/master/getting-started.md
 * which specifically recommends reading the "COUNTER Code of Practice for
 * Research Data" mentioned in the user facing docs.
 *
 * Make Data Count was first implemented in DASH. Here's an example dataset:
 * https://dash.ucmerced.edu/stash/dataset/doi:10.6071/M3RP49
 *
 * For processing logs we could try DASH's
 * https://github.com/gdcc/counter-processor
 *
 * Next, DataOne implemented it, and you can see an example dataset here:
 * https://search.dataone.org/view/doi:10.5063/F1Z899CZ
 *
 * Parts of DataOne are written in Java so perhaps there is some code that can
 * be reused?
 */
public class MakeDataCountUtil {

    public static final String LOG_HEADER = "#Fields: event_time	client_ip	session_cookie_id	user_cookie_id	user_id	request_url	identifier	filename	size	user-agent	title	publisher	publisher_id	authors	publication_date	version	other_id	target_url	publication_year\n";

    //ISO 3166 Country codes as used by Geomind Geolite2 database in counter-processor
    @Transient private static Set<String> validCountryCodes = Stream
            .of("a1", "a2", "o1", "ad", "ae", "af", "ag", "ai", "al", "am", "ao", "ap", "aq", "ar", "as", "at", "au", "aw", "ax", "az", "ba", "bb", "bd", "be", "bf", "bg", "bh", "bi", "bj", "bl", "bm", "bn", "bo", "bq", "br", "bs", "bt", "bv", "bw", "by", "bz", "ca", "cc", "cd", "cf", "cg", "ch", "ci", "ck", "cl", "cm", "cn", "co", "cr", "cu", "cv", "cw", "cx", "cy", "cz", "de", "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "eh", "er", "es", "et", "eu", "fi", "fj", "fk",
                    "fm", "fo", "fr", "ga", "gb", "gd", "ge", "gf", "gg", "gh", "gi", "gl", "gm", "gn", "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il", "im", "in", "io", "iq", "ir", "is", "it", "je", "jm", "jo", "jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls", "lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mf", "mg", "mh", "mk", "ml", "mm", "mn", "mo",
                    "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz", "na", "nc", "ne", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt", "pw", "py", "qa", "re", "ro", "rs", "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sj", "sk", "sl", "sm", "sn", "so", "sr", "ss", "st", "sv", "sx", "sy", "sz", "tc", "td", "tf", "tg", "th", "tj", "tk", "tl", "tm", "tn", "to",
                    "tr", "tt", "tv", "tw", "tz", "ua", "ug", "um", "us", "uy", "uz", "va", "vc", "ve", "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "za", "zm", "zw")
            .collect(Collectors.toCollection(HashSet<String>::new));;

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
            throw new IllegalArgumentException("MetricType must be one of these values: " + getMetricNameList() + ".");
        }
        
        private static List<String> getMetricNameList() {
           ArrayList<String> names = new ArrayList<String>();
           for(MetricType mt: MetricType.values()) {
               names.add(mt.text);
           }
            return names;
        }

        @Override
        public String toString() {
            switch (this) {
            case DOWNLOADS_TOTAL:
                return (MetricType.DOWNLOADS_TOTAL_MACHINE + " + " + MetricType.DOWNLOADS_TOTAL_REGULAR);
            case VIEWS_TOTAL:
                return (MetricType.VIEWS_TOTAL_MACHINE + " + " + MetricType.VIEWS_TOTAL_REGULAR);
            case DOWNLOADS_UNIQUE:
                return (MetricType.DOWNLOADS_UNIQUE_MACHINE + " + " + MetricType.DOWNLOADS_UNIQUE_REGULAR);
            case VIEWS_UNIQUE:
                return (MetricType.VIEWS_UNIQUE_MACHINE + " + " + MetricType.VIEWS_UNIQUE_REGULAR);
            default:
                return text;
            }
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
    
    
    //counter-processor sushi report uses lower case, so standardizing on that for storage
    public static boolean isValidCountryCode(String code) {
        return validCountryCodes.contains(code.toLowerCase());
    }


}
