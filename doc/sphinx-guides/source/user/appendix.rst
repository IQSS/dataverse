.. _user-appendix:

Appendix
+++++++++

Additional documentation complementary to the User Guide.

Metadata References
======================

Dataverse is committed to using standard-compliant metadata to ensure that Dataverse
metadata can be mapped easily to standard metadata schemas and be exported into JSON
format (XML for tabular file metadata) for preservation and interoperability.

Detailed below are what metadata schemas we support for Citation and Domain Specific Metadata in Dataverse:

- `Citation Metadata <https://docs.google.com/spreadsheet/ccc?key=0AjeLxEN77UZodHFEWGpoa19ia3pldEFyVFR0aFVGa0E#gid=0>`__: compliant with `DDI Lite <http://www.ddialliance.org/specification/ddi2.1/lite/index.html>`_, `DDI 2.5 Codebook <http://www.ddialliance.org/>`__, `DataCite 3.1 <http://schema.datacite.org/meta/kernel-3.1/doc/DataCite-MetadataKernel_v3.1.pdf>`__, and Dublin Core's `DCMI Metadata Terms <http://dublincore.org/documents/dcmi-terms/>`__ (`see .tsv version <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/citation.tsv>`__). Language field uses `ISO 639-2 <http://www.loc.gov/standards/iso639-2/php/code_list.php>`__ controlled vocabulary.
- `Geospatial Metadata <https://docs.google.com/spreadsheet/ccc?key=0AjeLxEN77UZodHFEWGpoa19ia3pldEFyVFR0aFVGa0E#gid=4>`__: compliant with DDI Lite, DDI 2.5 Codebook, DataCite, and Dublin Core (`see .tsv version <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/geospatial.tsv>`__). Country / Nation field uses `ISO 3166-1 <http://en.wikipedia.org/wiki/ISO_3166-1>`_ controlled vocabulary.
- `Social Science & Humanities Metadata <https://docs.google.com/spreadsheet/ccc?key=0AjeLxEN77UZodHFEWGpoa19ia3pldEFyVFR0aFVGa0E#gid=1>`__: compliant with DDI Lite, DDI 2.5 Codebook, and Dublin Core (`see .tsv version <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/social_science.tsv>`__).
- `Astronomy and Astrophysics Metadata <https://docs.google.com/spreadsheet/ccc?key=0AjeLxEN77UZodHFEWGpoa19ia3pldEFyVFR0aFVGa0E#gid=3>`__
  : These metadata elements can be mapped/exported to the International Virtual Observatory Alliance’s (IVOA) 
  `VOResource Schema format <http://www.ivoa.net/documents/latest/RM.html>`__ and is based on 
  `Virtual Observatory (VO) Discovery and Provenance Metadata <http://perma.cc/H5ZJ-4KKY>`__ (`see .tsv version <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/astrophysics.tsv>`__).
- `Life Sciences Metadata <https://docs.google.com/spreadsheet/ccc?key=0AjeLxEN77UZodHFEWGpoa19ia3pldEFyVFR0aFVGa0E#gid=2>`__: based on `ISA-Tab Specification <http://isatab.sourceforge.net/format.html>`__, along with controlled vocabulary from subsets of the `OBI Ontology <http://bioportal.bioontology.org/ontologies/OBI>`__ and the `NCBI Taxonomy for Organisms <http://www.ncbi.nlm.nih.gov/Taxonomy/taxonomyhome.html/>`__ (`see .tsv version <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/biomedical.tsv>`__).

See also the `Dataverse 4.0 Metadata Crosswalk: DDI, DataCite, DC, DCTerms, VO, ISA-Tab <https://docs.google.com/spreadsheets/d/10Luzti7svVTVKTA-px27oq3RxCUM-QbiTkm8iMd5C54/edit?usp=sharing>`__ document.
