package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.util.SystemConfig;

public class JsonLDNamespace {

	String prefix;

	
	String url;

	//FixMe - use a universal Dataverse URL rather than an instance one for Core terms
	public static JsonLDNamespace dvcore = new JsonLDNamespace("dvcore", SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#");
	public static JsonLDNamespace dcterms = new JsonLDNamespace("dcterms","http://purl.org/dc/terms/");
	public static JsonLDNamespace ore = new JsonLDNamespace("ore","http://www.openarchives.org/ore/terms/");
	public static JsonLDNamespace schema = new JsonLDNamespace("schema","http://schema.org/");

	public JsonLDNamespace(String prefix, String url) {
		this.prefix = prefix;
		this.url = url;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getUrl() {
		return url;
	}

}
