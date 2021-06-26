Structure of the :CVocConf setting
* An array of objects, one per field that specify required parameter

Each object associates a Javascript/external vocabulary service source with a given metadata field, as defined in a Dataverse metadata block.
In the simplest case, the script handles input and display for a single metadata field that is a simple text input (no child fields) and only supports finding terms from a single vocabulary/PID type.
In more complex cases, the script may support choosing from multiple vocabularies.
In more complex cases, the scripts may handle input for compound Dataverse metadata fields, e.g. ones that have child fields to store the termURI/name and vocabularyURI/name


A fairly basic example:

    {
        "field-name": "creator",
        "term-uri-field": "creator",
        "js-url": "/resources/js/people.js",
        "protocol": "orcid",
        "retrieval-uri": "https://pub.orcid.org/v3.0/{0}/person",
        "prefix": "https://orcid.org/",
        "managed-fields": {},
        "retrieval-filtering": {
            "@context": {
                "personName": "https://schema.org/name",
                "scheme": "https://schema.org/identifier"
            },
            "personName": {
                "pattern": "{0}, {1}",
                "params": ["/name/family-name/value", "/name/given-names/value"]
            },
            "@id": {
                "pattern": "{0}",
                "params": ["@id"]
            },
            "scheme": {
                "pattern": "ORCID"
            },
            "@type": "https://schema.org/Person"
        }
    },

* fieldname - the datasetField name of a field defined in a metadata block/tsv file
* term-uri-field - in a single/non-compound field case, this is the same as fieldname
* js-url - the absolute or relative URL of the javascript compatible with the protocol selected. Dataverse will load/reload scripts as needed when fields that require them are shown
* protocol - the API/protocol supported by the selected javascript. Dataverse will mark elements in the page with data-cvoc-protocol attributes with this value, which can be used by the scripts to find the elements they should manage

These four fields are enough to configure a script to manage the input and display for a given metadata field.

* managed-fields - in a single/non-compound field case, this is an empty object

This field is currently required to be non-null but otherwise doesn't affect this example

* retrieval-uri - When a controlled term uri is stored in Dataverse, Dataverse will call this URL to cache a copy of information about this term uri/PID. Currently, Dataverse does a GET requesting application/json from this URL. It first substitutes the term uri/PID for the parameter {0}
* prefix - Specific to ORCID; the PID is of the form `https://orcid.org/<16 character id>` while the retrieval-uri requires just the 16 character id. Dataverse will first strip the specified prefix from the term URI/PID before substituting in the retrieval-uri
* retrieval-filtering - Dataverse uses the cached results to index additional metadata (such as the name of the person in this ORCID example) along with the PID itself and can add these values to exports (currently the json and OAI-ORE exports). Since the services often send significantly more information than Dataverse requires, this filtering object allows selection/formatting of a subset of the response for storage. In the ORCID case, the filtering shown here stores the person's name (lastname, firstname), the PID itself, the fact that the PID scheme is ORCID, and the fact that the type of the identified object is a person. The filtering syntax supports substituting parameters found at specific json paths within the response into a new json object that is stored. (@id is a special token specifying the term URI/PID itself, and patterns such as "ORCID" represent hardcoded text).

These two entries configure how Dataverse will manage to store a cached copy of some information about the term URI/PID that can be used to index for search results and can be included in exported metadata formats. The mechanism shown here - to retrieve information from the authoritative server and to then format the response for Dataverse is intended as a limited internal mechanism sufficient to allow single input/non-compound fields behave like Dataverse's existing manual-entry compound fields. It is expected that, in the future, proxy services could handle reformatting (eliminating the need for the retrieval-filtering parameters) and even minimize/eliminate the need for Dataverse to cache these results (e.g. by handling creation of metadata exports). Until then, this simple mechanism allows Dataverse to record that, for example,  https://orcid.org/0000-0001-8462-650X 
corresponds to:

    {"personName":"Myers, James","@id":"https://orcid.org/0000-0001-8462-650X","scheme":"ORCID"}
    
Here's the equivalent configuration for a skosmos service where the field has been configured to allow choices from either of two vocabularies:

     {
        "field-name": "skosterm",
        "term-uri-field": "skosterm",
        "cvoc-url": "https://skosmos.dev.finto.fi/",
        "js-url": "/resources/js/skosmos.js",
        "protocol": "skosmos",
        "retrieval-uri": "https://skosmos.dev.finto.fi/rest/v1/data?uri={0}",
        "vocabs":{
            "unesco": "http://skos.um.es/unescothes/CS000",
            "agrovoc" : "http://aims.fao.org/vest-registry/kos/agrovoc"
        },
        "managed-fields": {},
        "retrieval-filtering": {
            "@context": {
                "termName": "https://schema.org/name",
                "vocabularyName": "https://dataverse.org/schema/vocabularyName",
                "vocabularyUri": "https://dataverse.org/schema/vocabularyUri",
                "lang": "@language",
                "value": "@value"
            },
            "@id": {
                "pattern": "{0}",
                "params": ["@id"]
            },
            "termName": {
                "pattern": "{0}",
                "params": ["/graph/uri=@id/prefLabel"]
            },
            "vocabularyName": {
                "pattern": "{0}",
                "params": ["/graph/type=skos:ConceptScheme/prefLabel"]
            },
            "vocabularyUri": {
                "pattern": "{0}",
                "params": ["/graph/type=skos:ConceptScheme/uri"]
            }
        }
    }
    
The one addition from the orcid case:
* vocabs contains a list of vocabular names/URIs. The skosmos.js Javascript will display both a vocabular selector and a term selector when there is more than one vocabulary listed

For skosmos, the retrieval-filtering defines four elements to cache for a given term - the termName, the URI itself (as '@id'), the vocabularyName, and vocabularyUri. As skosmos is internationalized, the termName and vocabularyName returned in this case are actually an array of objects with lang/value entries specifying, for example, the termName in multiple languages. In this case, Dataverse will index the term name in all of those languages. Also note that the specified call in retrieval-uri doesn't return a vocabularyName for some skosmos vocabularies and this element may be missing from the cache. 

Present in the retrieval-filtering element in both examples is an @context. This is used to map the cached json to json-ld, which allows it to be included in the OAI-ORE export.

What is cached by Dataverse is service/protocol specific but is not specific to which metadata field is using this service. It is therefore something that is created once, aloing with the Javascript and, at some point, may be in a separate configuration property/file. 

The final example below, shows a skosmos service being associated witha compound Dataverse field. This field is modeled after the keyword field, with the addition of a child field to store the term URI itself (which Dataverse (somewhat oddly) doesn't have an input for in the citation block keyword field).

    {
        "field-name": "cvocDemo",
        "term-uri-field": "cvocDemoTermURI",
        "cvoc-url": "https://skosmos.dev.finto.fi/",
        "js-url": "/resources/js/skosmos.js",
        "protocol": "skosmos",
        "retrieval-uri": "https://skosmos.dev.finto.fi/rest/v1/data?uri={0}",
        "term-parent-uri": "",
        "vocabs": {
            "unesco": "http://skos.um.es/unescothes/CS000"
        },
        "managed-fields": {
            "vocabularyName": "cvocDemoVocabulary",
            "termName": "cvocDemoTerm",
            "vocabularyUri": "cvocDemoVocabularyURI"
        },
        "retrieval-filtering": {
            "@context": {
                "termName": "https://schema.org/name",
                "vocabularyName": "https://dataverse.org/schema/vocabularyName",
                "vocabularyUri": "https://dataverse.org/schema/vocabularyUri",
                "lang": "@language",
                "value": "@value"
            },
            "@id": {
                "pattern": "{0}",
                "params": ["@id"]
            },
            "termName": {
                "pattern": "{0}",
                "params": ["/graph/uri=@id/prefLabel"]
            },
            "vocabularyName": {
                "pattern": "{0}",
                "params": ["/graph/type=skos:ConceptScheme/prefLabel"]
            },
            "vocabularyUri": {
                "pattern": "{0}",
                "params": ["/graph/type=skos:ConceptScheme/uri"]
            }
        }
    }

This example adds the field
* term-parent-uri - skosmos allows limiting queries to a subset of a vocabulary, This parameter allows that (I don't have any description of its format handy though)

To handle multiple fields, this example changes:
* field-name - the name of the parent field
* term-uri-field - the name of the child field in which the termURI will be stored
* managed-fields - a list of child field values that correspond with the information the skosmos script is able to supply, namely the term name, vocabular name and vocabulary uri. 

When the skosmos Javascript runs on this type of field, it hides all child fields and displays and allows input for the term uri as it normally would. However, when a slection is made, the script populates the values for all 4 fields. This simplfies using a controlled vocabulary service with an existing field (although a termURI field is needed and would have to be added to use skosmos with the citation.keyword field.) One limitation of this approach is that the child fields for the term and vocabular name only store the value for the current language (versus the full set of translations). 

#How to make a new Javascript for your Service/Vocabulary

The skosmos.js and people.js scripts in this branch are good examples to start from. people.js is appropriate for single fields (it could be extendded to handle multiple fields but has not) and for services that provide a single vocabulary choice. skosmos.js is an example that handles both multiple vocabularies and managed fields.

Both of these scripts assume that, when the user starts typing, the letters they have types can be sent to your service via its api to get a list of terms that 'match'. What matches is up to your service(e.g. skosmos matches on both the preferred and alternate labels for a term, while ORCID will match on partial names, emails, ORCIDs, and/or information about a person such as institutions where they studied or worked). When the list is returned, the script has to format the response into a set of options for selection, usually putting the term name/uri together in nicely formatted HTML. These customizations can be done within the ajax call portion of the script. What happens when the user selects a choice, clears their choice, etc. should be standard and you probably won't have to make changes to the example script to make it work. Similarly, for metadata display, the scripts again expect to call your API (possibly the same as the retrieval-uri Dataverse will use internally) to get metadata about just that term so you can again provide nicely formatted HTML to replace the plain text display of the termURI/PID that Dataverse generates.

Along with creating a script, you'll need to create an appropriate object to add to the :CVocConf property to let Dataverse know about your service and to assign your service to specific fields.THe most complex part of that is probably defining the retrieval-filtering object - if your service doesn't have an endpoint that produces json that can be used directly. For most vocabularies, the four elements termURI/name and vocabularyURI/name are probably all you could/should provide. For other PIDs, you'll need to decide what Dataverse should know for use in indexing and metadata export. For ORCID, the choices were guided by the existing author/contact fields that record name, identifier type, and value. (Some of these fields include an "Affiliation" child field. For an initial implementation, it wasn't clear that trying to find an affiliation from ORCID made sense (since you may have several active affiliations)).

#Status

The intent with the implementation here is to provide a working mechanism that can be used by Dataverse instances on single metadata fields in existing or custom metadata blocks and can be used on some compound fields, such as keywords, with minor/generic modification (i.e. adding a term uri child field which is probably useful regardless).

However, working with external controlled vocabularies is complex and raises many questions. There are cases such as using vocabularies from multiple services on one metadata field that are potentially valuable but not yet implemented. Handling migration of existing entries (aside from just treating them as plain text), e.g. by matching text to URIs is not supported with the existing code. There are also broader issues regarding how Dataverse should handle change over time (if vocabularies change the meaning of terms over time), how much information about the terms should be added to metadata exports and made avaialable in search, etc. where there may be better options relative to what the code currently does. There are also questions about how long services may live, whether they will move, etc. that add complexity. Etc.

The current design is thus intended to be a useful/usable starting point, and one that has reasonable backward compatibility/fall-back capabilities (I.e. if a service goes away or you decide to no longer use it, the termURI entries, or those in compound fields, remain valid as they were and you can go back to manually entering the same information as is done today.) It is also intended to be a design where innovation/R&D can be done incrementally - scipts can be updated without changing Dataverse internals, proxy services can be plugged in as a way to populate the internal term cache or replace it, additional metadata export formats can access the information being added to the json and OAI_ORE exports now, etc.