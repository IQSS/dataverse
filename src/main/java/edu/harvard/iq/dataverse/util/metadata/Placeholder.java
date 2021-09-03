package edu.harvard.iq.dataverse.util.metadata;

/**
 * This class provides some simple markers, so we con distinguish if we need to replace a placeholder with
 * a real object from the database/... when handing over after parsing
 */
public class Placeholder {
    public static final class Dataverse extends edu.harvard.iq.dataverse.Dataverse {}
    public static final class MetadataBlock extends edu.harvard.iq.dataverse.MetadataBlock {}
    public static final class DatasetFieldType extends edu.harvard.iq.dataverse.DatasetFieldType {}
}
