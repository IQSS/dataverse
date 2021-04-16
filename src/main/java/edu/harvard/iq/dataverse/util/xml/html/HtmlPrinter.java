package edu.harvard.iq.dataverse.util.xml.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HtmlPrinter {

    public static String prettyPrint(String ugly) {
        Document doc = Jsoup.parseBodyFragment(ugly);
        doc.outputSettings().indentAmount(2);
        return doc.body().html();
    }

}
