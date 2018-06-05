package edu.harvard.iq.dataverse.util.json;

import javax.json.JsonObject;
import javax.json.JsonValue;

import edu.harvard.iq.dataverse.util.SystemConfig;

public class JsonLDTerm {

	String prefix;

	String term;

	String url;
/*
	public static JsonLDTerm termsOfUse = new JsonLDTerm("termsOfUse",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#termsOfUse");
	public static JsonLDTerm confidentialityDeclaration = new JsonLDTerm("confidentialityDeclaration",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#confidentialityDeclaration");
	public static JsonLDTerm specialPermissions = new JsonLDTerm("specialPermissions",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#specialPermissions");
	public static JsonLDTerm restrictions = new JsonLDTerm("restrictions",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#restrictions");
	public static JsonLDTerm citationRequirements = new JsonLDTerm("citationRequirements",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#citationRequirements");
	public static JsonLDTerm depositorRequirements = new JsonLDTerm("depositorRequirements",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#depositorRequirements");
	public static JsonLDTerm conditions = new JsonLDTerm("conditions",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#conditions");
	public static JsonLDTerm disclaimer = new JsonLDTerm("disclaimer",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#disclaimer");

	public static JsonLDTerm fileTermsOfAccess = new JsonLDTerm("fileTermsOfAccess",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#fileTermsOfAccess");

	public static JsonLDTerm termsOfAccess = new JsonLDTerm("termsOfAccess",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#termsOfAccess");
	public static JsonLDTerm fileRequestAccess = new JsonLDTerm("fileRequestAccess",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#fileRequestAccess");
	public static JsonLDTerm dataAccessPlace = new JsonLDTerm("dataAccessPlace",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#dataAccessPlace");
	public static JsonLDTerm originalArchive = new JsonLDTerm("originalArchive",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#originalArchive");
	public static JsonLDTerm availabilityStatus = new JsonLDTerm("availabilityStatus",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#availabilityStatus");
	public static JsonLDTerm contactForAccess = new JsonLDTerm("contactForAccess",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#contactForAccess");
	public static JsonLDTerm sizeOfCollection = new JsonLDTerm("sizeOfCollection",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#sizeOfCollection");
	public static JsonLDTerm studyCompletion = new JsonLDTerm("studyCompletion",
			SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#studyCompletion");
*/
	public JsonLDTerm(String prefix, String term, String url) {
		this.prefix = prefix;
		this.term = term;
		this.url = url;
	}

	public JsonLDTerm(String term, String url) {
		this.prefix = null;
		this.term = term;
		this.url = url;
	}

	public static JsonLDTerm DVCore(String term) {
		return new JsonLDTerm("dvcore", term, SystemConfig.getDataverseSiteUrlStatic() + "/schema/core#");
	}
	
	public static JsonLDTerm ore(String term) {
		return new JsonLDTerm("ore", term, "http://www.openarchives.org/ore/terms/");
	}
	
	public static JsonLDTerm schemaOrg(String term) {
		return new JsonLDTerm("schema", term, "http://schema.org/");
	}
	public static JsonLDTerm dcTerms(String term) {
		return new JsonLDTerm("dcterms", term, "http://purl.org/dc/terms/");
	}
	

	
	public String getContextLabel() {
		if(prefix!=null) {
		return prefix;
		} else {
			return term;
		}
	}

	public String getLabel() {
		if (prefix == null) {
			return term;
		} else {
			return prefix + ":" + term;
		}
	}

	public String getUrl() {
		return url;
	}

}
