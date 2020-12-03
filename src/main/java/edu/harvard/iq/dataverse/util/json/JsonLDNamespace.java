package edu.harvard.iq.dataverse.util.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JsonLDNamespace {

	String prefix;

	
	String url;

	public static JsonLDNamespace dvcore = new JsonLDNamespace("dvcore", "https://dataverse.org/schema/core#");
	public static JsonLDNamespace dcterms = new JsonLDNamespace("dcterms","http://purl.org/dc/terms/");
	public static JsonLDNamespace ore = new JsonLDNamespace("ore","http://www.openarchives.org/ore/terms/");
	public static JsonLDNamespace schema = new JsonLDNamespace("schema","http://schema.org/");

	private static List<JsonLDNamespace> namespaces = new ArrayList<JsonLDNamespace>(Arrays.asList(dvcore, dcterms, ore, schema));
	
	public static JsonLDNamespace defineNamespace(String prefix, String url) {
	    JsonLDNamespace ns = new JsonLDNamespace(prefix, url);
	    namespaces.add(ns);
	    return ns;
	}
	
	public static void deleteNamespace(JsonLDNamespace ns) {
        namespaces.remove(ns);
    }
    
	public static boolean isInNamespace(String url) {
	  for(JsonLDNamespace ns: namespaces) {
	      if(url.startsWith(ns.getUrl())) {
	          return true;
	      }
	  }
	  return false;
	}
	
	public static void addNamespacesToContext(Map<String, String> context) {
	    for(JsonLDNamespace ns: namespaces) {
	        context.putIfAbsent(ns.getPrefix(), ns.getUrl());
	    };
	}
	
	private JsonLDNamespace(String prefix, String url) {
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
