package edu.harvard.iq.dataverse.jsf.support;

import java.nio.file.Path;
import java.util.List;

/** Options for {@link JsfFixtures#createDataset(CreateDatasetOptions)}. */
public class CreateDatasetOptions {

    /** Alias of the collection to create the dataset in. Defaults to the root collection. */
    public String collection;
    /** Defaults to a unique "Playwright Dataset …" title. */
    public String title;
    public String description;
    public String subject;
    /** Files to upload — see {@link TestData}. */
    public List<Path> files = List.of();

    public CreateDatasetOptions setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public CreateDatasetOptions setTitle(String title) {
        this.title = title;
        return this;
    }

    public CreateDatasetOptions setDescription(String description) {
        this.description = description;
        return this;
    }

    public CreateDatasetOptions setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public CreateDatasetOptions setFiles(Path... files) {
        this.files = List.of(files);
        return this;
    }

    public CreateDatasetOptions setFiles(List<Path> files) {
        this.files = List.copyOf(files);
        return this;
    }
}
