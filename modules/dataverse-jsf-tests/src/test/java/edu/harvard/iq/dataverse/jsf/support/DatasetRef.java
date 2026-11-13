package edu.harvard.iq.dataverse.jsf.support;

/**
 * A dataset a test created.
 *
 * @param title the dataset's title
 * @param url full URL of the dataset page, as Dataverse redirected to after saving
 */
public record DatasetRef(String title, String url) {
}
