package edu.harvard.iq.dataverse.util.json;

public class JsonLDNamespace {

	String prefix;

	
	String url;

	public static JsonLDNamespace dvcore = new JsonLDNamespace("dvcore", "https://dataverse.org/schema/core#");
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
