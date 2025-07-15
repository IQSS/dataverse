.. _user-appendix:

Appendix
+++++++++

Additional documentation complementary to the User Guide.

.. contents:: |toctitle|
	:local:

.. _metadata-references:

Metadata References
======================

The Dataverse Project is committed to using standard-compliant metadata to ensure that a Dataverse installation's
metadata can be mapped easily to standard metadata schemas and be exported into JSON
format (XML for tabular file metadata) for preservation and interoperability.

Supported Metadata
~~~~~~~~~~~~~~~~~~

Detailed below are what metadata schemas we support for Citation and Domain Specific Metadata in the Dataverse Project:

- Citation Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/citation.tsv>`__): compliant with `DDI Lite <https://www.ddialliance.org/specification/ddi2.1/lite/index.html>`_, `DDI 2.5 Codebook <https://www.ddialliance.org/>`__, `DataCite 4.5 <https://schema.datacite.org/meta/kernel-4.5/>`__, and Dublin Core's `DCMI Metadata Terms <https://dublincore.org/documents/dcmi-terms/>`__ . Language field uses `ISO 639-1 <https://www.loc.gov/standards/iso639-2/php/English_list.php>`__ controlled vocabulary.
- Geospatial Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/geospatial.tsv>`__, NEW DRAFT: `see 05-2025 .tsv <https://github.com/user-attachments/files/20354535/geospatial_new.txt>`__): compliant with `DDI Lite <https://www.ddialliance.org/specification/ddi2.1/lite/index.html>`_, `DDI 2.5 Codebook <https://www.ddialliance.org/>`__, `DataCite 4.5 <https://schema.datacite.org/meta/kernel-4.5/>`__, and Dublin Core. Country / Nation field uses `ISO 3166-1 <https://en.wikipedia.org/wiki/ISO_3166-1>`_ controlled vocabulary.
- Social Science & Humanities Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/social_science.tsv>`__): compliant with `DDI Lite <https://www.ddialliance.org/specification/ddi2.1/lite/index.html>`_, `DDI 2.5 Codebook <https://www.ddialliance.org/>`__, and Dublin Core.
- Astronomy and Astrophysics Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/astrophysics.tsv>`__): These metadata elements can be mapped/exported to the International Virtual Observatory Alliance’s (IVOA) 
  `VOResource Schema format <https://www.ivoa.net/documents/latest/RM.html>`__ and is based on 
  `Virtual Observatory (VO) Discovery and Provenance Metadata <https://perma.cc/H5ZJ-4KKY>`__.
- Life Sciences Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/biomedical.tsv>`__): based on `ISA-Tab Specification <https://isa-specs.readthedocs.io/en/latest/isamodel.html>`__, along with controlled vocabulary from subsets of the `OBI Ontology <https://bioportal.bioontology.org/ontologies/OBI>`__ and the `NCBI Taxonomy for Organisms <https://www.ncbi.nlm.nih.gov/Taxonomy/taxonomyhome.html/>`__.
- Journal Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/journals.tsv>`__): based on the `Journal Archiving and Interchange Tag Set, version 1.2 <https://jats.nlm.nih.gov/archiving/tag-library/1.2/chapter/how-to-read.html>`__.
- 3D Objects Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/3d_objects.tsv>`__).

Experimental Metadata
~~~~~~~~~~~~~~~~~~~~~

Unlike supported metadata, experimental metadata is not enabled by default in a new Dataverse installation. Feedback via any `channel <https://dataverse.org/contact>`_ is welcome!

- `CodeMeta Software Metadata <https://docs.google.com/spreadsheets/d/e/2PACX-1vTE-aSW0J7UQ0prYq8rP_P_AWVtqhyv46aJu9uPszpa9_UuOWRsyFjbWFDnCd7us7PSIpW7Qg2KwZ8v/pub>`__: based on the `CodeMeta Software Metadata Schema, version 2.0 <https://codemeta.github.io/terms/>`__ (`see .tsv version <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/codemeta.tsv>`__)
- Computational Workflow Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/computational_workflow.tsv>`__): adapted from `Bioschemas Computational Workflow Profile, version 1.0 <https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE>`__ and `Codemeta <https://codemeta.github.io/terms/>`__.
- Archival Metadata (`see .tsv <https://github.com/IQSS/dataverse/blob/master/scripts/api/data/metadatablocks/archival.tsv>`__): Enables repositories to register metadata relating to the potential archiving of the dataset at a depositor archive, whether that be your own institutional archive or an external archive, i.e. a historical archive.
- Local Contexts Metadata (`see .tsv <https://github.com/gdcc/dataverse-external-vocab-support/blob/main/packages/local_contexts/cvocLocalContexts.tsv>`__): Supports integration with the `Local Contexts <https://localcontexts.org/>`__ platform, enabling the use of Traditional Knowledge and Biocultural Labels, and Notices. For more information on setup and configuration, see :doc:`../installation/localcontexts`.

Please note: these custom metadata schemas are not included in the Solr schema for indexing by default, you will need
to add them as necessary for your custom metadata blocks. See "Update the Solr Schema" in :doc:`../admin/metadatacustomization`.

See Also
~~~~~~~~

See also the `Dataverse Software 4.0 Metadata Crosswalk: DDI, DataCite, DC, DCTerms, VO, ISA-Tab <https://docs.google.com/spreadsheets/d/10Luzti7svVTVKTA-px27oq3RxCUM-QbiTkm8iMd5C54/edit?usp=sharing>`__ document and the :doc:`/admin/metadatacustomization` section of the Admin Guide.
