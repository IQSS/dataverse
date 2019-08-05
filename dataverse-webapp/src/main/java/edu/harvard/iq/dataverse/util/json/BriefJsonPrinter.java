package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;

import javax.json.JsonObjectBuilder;

import static edu.harvard.iq.dataverse.common.NullSafeJsonBuilder.jsonObjectBuilder;

/**
 * A Json printer that prints minimal data on objects. Useful when embedding
 * objects inside others.
 *
 * @author michael
 */
public class BriefJsonPrinter {

    public JsonObjectBuilder json(DatasetVersion dsv) {
        return (dsv == null)
                ? null
                : jsonObjectBuilder().add("id", dsv.getId())
                .add("version", dsv.getVersion())
                .add("versionState", dsv.getVersionState().name())
                .add("title", dsv.getTitle());
    }

    public JsonObjectBuilder json(MetadataBlock blk) {
        return (blk == null)
                ? null
                : jsonObjectBuilder().add("id", blk.getId())
                .add("displayName", blk.getLocaleDisplayName())
                .add("name", blk.getName())
                ;
    }

    public JsonObjectBuilder json(Workflow wf) {
        return jsonObjectBuilder().add("id", wf.getId())
                .add("name", wf.getName());
    }
}
