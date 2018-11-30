package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.MediaType;

/**
 * Schema.org JSON-LD is used by Google Dataset Search and other services to
 * make datasets more discoverable. It is embedded in the HTML of dataset pages
 * and available as an export format.
 * <p>
 * Do not make any backward incompatible changes unless it's absolutely
 * necessary and list them in the API Guide. The existing list is in the
 * "Native API" section.
 * <p>
 * {@link SchemaDotOrgExporterTest} has most of the tests but
 * {@link DatasetVersionTest} has some as well. See
 * https://schema.org/docs/gs.html#schemaorg_expected for some discussion on
 * what a flexible format Schema.org JSON-LD. Use of tools such as
 * https://search.google.com/structured-data/testing-tool and
 * https://webmaster.yandex.com/tools/microtest/ and
 * http://linter.structured-data.org to make sure Dataverse continues to emit
 * valid output is encouraged but you will find that these tools (and the
 * underlying spec) can be extremely accommodating to fairly radical
 * restructuring of the JSON output. Strings can become objects or arrays, for
 * example, and Honey Badger don't care. Because we expect API users will make
 * use of the JSON output, you should not change it or you will break their
 * code.
 * <p>
 * Copying and pasting output into
 * https://search.google.com/structured-data/testing-tool to make sure it's
 * still valid can get tedious but we are not aware of a better way. We looked
 * at https://github.com/jessedc/ajv-cli (doesn't support JSON-LD, always
 * reports "valid"), https://github.com/jsonld-java/jsonld-java and
 * https://github.com/jsonld-java/jsonld-java-tools (unclear if they support
 * validation), https://github.com/structured-data/linter (couldn't get it
 * installed), https://github.com/json-ld/json-ld.org (couldn't get the test
 * suite to detect changes) , https://tech.yandex.com/validator/ (requires API
 * key),
 * https://packagist.org/packages/padosoft/laravel-google-structured-data-testing-tool
 * (may be promising). We use https://github.com/everit-org/json-schema in our
 * app already to validate JSON Schema but JSON-LD is a different animal.
 * https://schema.org/Dataset.jsonld appears to be the way to download just the
 * "Dataset" definition ( https://schema.org/Dataset ) from schema.org but the
 * official way to download these definitions is from
 * https://schema.org/docs/developers.html#defs . Despite all this
 * experimentation (some of these tools were found at
 * https://medium.com/@vilcins/structured-data-markup-validation-and-testing-tools-1968bd5dea37
 * ), the accepted answer at
 * https://webmasters.stackexchange.com/questions/56577/any-way-to-validate-schema-org-json-ld-before-publishing
 * is to just copy and paste your output into one of the online tools so for
 * now, just do that.
 * <p>
 * Google provides a Schema.org JSON-LD example at
 * https://developers.google.com/search/docs/data-types/dataset but we've also
 * looked at examples from
 * https://zenodo.org/record/1419226/export/schemaorg_jsonld#.W9NJjicpDUI ,
 * https://www.icpsr.umich.edu/icpsrweb/ICPSR/studies/23980/export , and
 * https://doi.pangaea.de/10.1594/PANGAEA.884619
 */
@AutoService(Exporter.class)
public class SchemaDotOrgExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(SchemaDotOrgExporter.class.getCanonicalName());

    public static final String NAME = "schema.org";

    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        String jsonLdAsString = version.getJsonLd();
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonLdAsString));) {
            JsonObject jsonLdJsonObject = jsonReader.readObject();
            try {
                outputStream.write(jsonLdJsonObject.toString().getBytes("UTF8"));
            } catch (IOException ex) {
                logger.info("IOException calling outputStream.write: " + ex);
            }
            try {
                outputStream.flush();
            } catch (IOException ex) {
                logger.info("IOException calling outputStream.flush: " + ex);
            }
        }
    }

    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.schemaDotOrg");
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }

    @Override
    public Boolean isHarvestable() {
        // Defer harvesting because the current effort was estimated as a "2": https://github.com/IQSS/dataverse/issues/3700
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getXMLNameSpace() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaLocation() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaVersion() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public void setParam(String name, Object value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }
    

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
