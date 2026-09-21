package edu.harvard.iq.dataverse.util;

import org.jsoup.Jsoup;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarkupCheckerUtil {

    /** Test HTML equivalence and ignore differences in the order of attributes
     *   This does (in normalizeHtml()) use Jsoup to help test Jsoup though.
     *
     * @param expected - the HTML we expect to be returned
     * @param actual - the HTML we actually got
     */
    public static void assertHtmlEqual(String expected, String actual) {
        if (expected == null || actual == null) {
            assertEquals(expected, actual);
            return;
        }

        String normalizedExpected = normalizeHtml(expected);
        String normalizedActual = normalizeHtml(actual);
        assertEquals(normalizedExpected, normalizedActual);
    }

    /**
     * Creates a Hamcrest matcher that compares HTML after normalization, ignoring
     * differences such as attribute ordering and whitespace.
     *
     * @param expected the expected HTML
     * @return a matcher for HTML-equivalent strings
     */
    public static org.hamcrest.Matcher<String> htmlEqualTo(String expected) {
        return new org.hamcrest.BaseMatcher<String>() {

            @Override
            public boolean matches(Object actual) {
                if (expected == null || actual == null) {
                    return expected == actual;
                }

                if (!(actual instanceof String actualHtml)) {
                    return false;
                }

                return normalizeHtml(expected).equals(normalizeHtml(actualHtml));
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("HTML equal to ")
                        .appendValue(expected);

                if (expected != null) {
                    description.appendText(" after normalization as ")
                            .appendValue(normalizeHtml(expected));
                }
            }

            @Override
            public void describeMismatch(Object actual, org.hamcrest.Description description) {
                if (actual == null) {
                    description.appendText("was null");
                    return;
                }

                if (!(actual instanceof String actualHtml)) {
                    description.appendText("was ")
                            .appendValue(actual);
                    return;
                }

                description.appendText("was ")
                        .appendValue(actualHtml)
                        .appendText(" after normalization as ")
                        .appendValue(normalizeHtml(actualHtml));
            }
        };
    }

    private static String normalizeHtml(String html) {
        org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(html);
        for (org.jsoup.nodes.Element el : doc.getAllElements()) {
            org.jsoup.nodes.Attributes attrs = el.attributes();
            java.util.List<org.jsoup.nodes.Attribute> list = new java.util.ArrayList<>(attrs.asList());
            list.sort(java.util.Map.Entry.comparingByKey());
            for (org.jsoup.nodes.Attribute a : list) {
                attrs.remove(a.getKey());
            }
            for (org.jsoup.nodes.Attribute a : list) {
                attrs.put(a);
            }
        }
        return doc.body().html().replaceAll("\\s+", "");
    }
}
