package edu.harvard.iq.dataverse.export;

import java.io.OutputStream;

import javax.json.JsonObject;
import javax.xml.stream.XMLStreamException;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.openaire.OpenAireExportUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.BundleUtil;

@AutoService(Exporter.class)
public class OpenAireExporter implements Exporter {
    
	public OpenAireExporter() {
	}

	@Override
	public String getProviderName() {
		return "oai_datacite";
	}
	

	@Override
	public String getDisplayName() {
		return  BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dataciteOpenAIRE") != null ? BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.dataciteOpenAIRE") : "DataCite OpenAIRE";
	}

	@Override
	public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream)
			throws ExportException {
		try {
            OpenAireExportUtil.datasetJson2openaire(json, outputStream);
		} catch (XMLStreamException xse) {
            throw new ExportException("Caught XMLStreamException performing DataCite OpenAIRE export", xse);
        }
	}

	@Override
	public Boolean isXMLFormat() {
		return true;
	}

	@Override
	public Boolean isHarvestable() {
		return true;
	}

	@Override
	public Boolean isAvailableToUsers() {
		return false;
	}

	@Override
	public String getXMLNameSpace() throws ExportException {
		return OpenAireExportUtil.RESOURCE_NAMESPACE;
	}

	@Override
	public String getXMLSchemaLocation() throws ExportException {
		return OpenAireExportUtil.RESOURCE_SCHEMA_LOCATION;
	}

	@Override
	public String getXMLSchemaVersion() throws ExportException {
		return OpenAireExportUtil.SCHEMA_VERSION;
	}

	@Override
	public void setParam(String name, Object value) {
		// not used
	}

}
