=======================
Metadata Export Formats
=======================

.. contents:: |toctitle|
    :local:

Introduction
------------

Dataverse ships with a number of metadata export formats available for published datasets. A given metadata export
format may be available for user download (via the UI and API) and/or be available for use in Harvesting between
Dataverse instances.

As of v5.14, Dataverse provides a mechanism for third-party developers to create new metadata Exporters than implement
new metadata formats or that replace existing formats. All the necessary dependencies are packaged in an interface JAR file
available from Maven Central. Developers can distribute their new Exporters as JAR files which can be dynamically loaded
into Dataverse instances - see :ref:`external-exporters`. Developers are encouraged to make their Exporter code available
via https://github.com/gdcc/dataverse-exporters (or minimally, to list their existence in the README there). 

Exporter Basics
---------------

New Exports must implement the ``io.gdcc.spi.export.Exporter`` interface. The interface includes a few methods for the Exporter
to provide Dataverse with the format it produces, a display name, format mimetype, and whether the format is for download 
and/or harvesting use, etc. It also includes a main ``exportDataset(ExportDataProvider dataProvider, OutputStream outputStream)``
method through which the Exporter receives metadata about the given dataset (via the ``ExportDataProvider``, described further 
below) and writes its output (as an OutputStream).

Exporters that create an XML format must implement the ``io.gdcc.spi.export.XMLExporter`` interface (which extends the Exporter
interface). XMLExporter adds a few methods through which the XMLExporter provides information to Dataverse about the XML 
namespace and version being used.

Exporters also need to use the ``@AutoService(Exporter.class)`` which makes the class discoverable as an Exporter implementation.

The ``ExportDataProvider`` interface provides several methods through which your Exporter can receive dataset and file metadata
in various formats. Your exporter would parse the information in one or more of these inputs to retrieve the values needed to
generate the Exporter's output format.

The most important methods/input formats are:

- ``getDatasetJson()`` - metadata in the internal Dataverse JSON format used in the native API and available via the built-in JSON metadata export.
- ``getDatasetORE()`` - metadata in the OAI_ORE format available as a built-in metadata format and as used in Dataverse's BagIT-based Archiving capability. 
- ``getDatasetFileDetails`` - detailed file-level metadata for ingested tabular files.
 
The first two of these provide ~complete metadata about the dataset along with the metadata common to all files. This includes all metadata
entries from all metadata blocks, PIDs, tags, Licenses and custom terms, etc. Almost all built-in exporters today use the JSON input.
The newer OAI_ORE export, which is JSON-LD-based, provides a flatter structure and references metadata terms by their external vocabulary ids
(e.g. http://purl.org/dc/terms/title) which may make it a prefereable starting point in some cases.
 
The last method above provides a new JSON-formatted serialization of the variable-level file metadata Dataverse generates during ingest of tabular files.
This information has only been included in the built-in DDI export, as the content of a ``dataDscr`` element. (Hence inspecting the edu.harvard.iq.dataverse.export.DDIExporter and related classes would be a good way to explore how the JSON is structured.) 

The interface also provides

- ``getDatasetSchemaDotOrg();`` and
- ``getDataCiteXml();``.
  
These provide subsets of metadata in the indicated formats. They may be useful starting points if your exporter will, for example, only add one or two additional fields to the given format.

If an Exporter cannot create a requested metadata format for some reason, it should throw an ``io.gdcc.spi.export.ExportException``.

Building an Exporter
--------------------

The example at https://github.com/gdcc/dataverse-exporters provides a Maven pom.xml file suitable for building an Exporter JAR file and that repository provides additional development guidance.

There are four dependencies needed to build an Exporter:

- ``io.gdcc dataverse-spi`` library containing the interfaces discussed above and the ExportException class
- ``com.google.auto.service auto-service``, which provides the @AutoService annotation
- ``jakarta.json jakarata.json-api`` for JSON classes
- ``jakarta.ws.rs jakarta.ws.rs-api``, which provides a MediaType enumeration for specifying mime types.

Specifying a Prerequisite Export
--------------------------------

An advanced feature of the Exporter mechanism allows a new Exporter to specify that it requires, as input, 
the output of another Exporter. An example of this is the builting HTMLExporter which requires the output 
of the DDI XML Exporter to produce an HTML document with the same DDI content.

This is configured by providing the metadata format name via the ``Exporter.getPrerequisiteFormatName()`` method.
When this method returns a non-empty format name, Dataverse will provide the requested format to the Exporter via
the ``ExportDataProvider.getPrerequisiteInputStream()`` method.

Developers and administrators deploying Exporters using this mechanism should be aware that, since metadata formats
can be changed by other Exporters, the InputStream received may not hold the expected metadata. Developers should clearly
document their compatability with the built-in or third-party Exporters they support as prerequisites.
