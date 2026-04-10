Locally FAIR
++++++++++++

Locally FAIR describes content that is managed according to FAIR principles
(Findable, Accessible, Interoperable, and Reusable) within a defined local or
organizational community rather than for the public internet as a whole.

Dataverse now has optional, experimental support for managing Locally FAIR collections.

In a typical public Dataverse installation, published dataset metadata is visible
to everyone, even if the dataset's files themselves may be embargoed or restricted. Locally FAIR support
extends this model by allowing some collections, and the published datasets within them to remain
visible only to designated users or groups. This makes it possible for a single
Dataverse installation to support both:

- public, globally discoverable content; and
- organizational content whose existence and metadata are only be visible to
  authorized users.

The rationale for making some content Locally FAIR can vary.
Locally FAIR content can include:

- sensitive research collections;
- institution-only datasets;
- datasets that should not be accessible to bots that may not adhere to the dataset license and terms, and
- projects under contractual or policy restrictions;

Dataverse's Locally FAIR mechanism is appropriate for repositories that will house at least some data
whose metadata should only be visible to organizational members. The decision to make data Locally FAIR
is managed at the collection level and repositories can have both FAIR and Locally FAIR content.

.. contents:: |toctitle|
   :local:

What Locally FAIR Means
=======================

Locally FAIR content is intended to be FAIR within a particular community.

That means:
- **Findable** Data is easy to locate for both humans and machines, when authorized. Locally FAIR datasets (and files if configured) have persistent identifiers, but do not use DOIs which are publicly searchable.

- **Accessible** Data is retrievable through standardized protocols. Authorized users can use Dataverse's standard user interface and API calls to access Locally FAIR content in the same way they do with any published data.

- **Interoperable** Data should be compatible with other datasets and systems. Locally FAIR datasets in Dataverse use the same standard metadata blocks as for public content and files undergo the same ingest process, use the same previewers and tools, etc.

- **Reusable** Data should be well-described and licensed in a way that allows others to use it for future research. The licenses and terms on locally FAIR content make it clear how and when the data can be re-used.

Why Repositories Use It
=======================

Without Locally FAIR support, repositories may need separate Dataverse
installations to separate public and organization-only content.

How It Differs from Restricted Files
====================================

Restricting or embargoing files limits access to the file contents, but in a standard public
repository the dataset's published metadata, including the list of files, would still be visible.
If a dataset allows requests for file access, anyone can request access, even if the dataset's
license or terms limit access to specific groups.

Locally FAIR goes further. Locally FAIR collections and datasets do not appear in content listings or
search results for unauthorized users, nor can the collection/dataset/file page be viewed. API access
is also blocked for unauthorized access.

Who Can See Locally FAIR Content
================================

Visibility is determined by superusers and is managed at the collection level.
Access can be granted to any group(s) or user(s) defined in Dataverse - the same groups/users
available when assigning roles on collections, datasets, and files.

How Can You Tell When Content is Locally FAIR?
==============================================

The Dataverse UI adds a "Locally FAIR" tag to all collections, datasets, and files who's visibility
is limited by the locally FAIR mechanism.


Why is Locally FAIR support "Experimental"
==========================================

The word "experimental" is used when functionality is new, may evolve signifcantly in future releases,
and generally may require more effort to configure and manage and/or more effort to support than more
mature functionality.

With the current Locally FAIR implementation, managers need to be aware that they are responsible for
choosing collection settings compatible with Locally FAIR content, i.e. not using DOIs (whose metadata
is publicly accessible) or publicly visible stores, etc. Users and managers should also be aware that
some functionality that might expose Locally FAIR content, e.g. linking, may not be prohibited programmatically
but should still be avoided. Similarly, users should be aware that functionality such as metrics and quotas
may expose the existence of Locally FAIR content. If your Dataverse instance supports Locally FAIR data,
you are encouraged to be an active participant in reporting any issues and suggesting further improvements.

Things to Keep in Mind
======================

If your repository supports Locally FAIR content:

- published does not always mean public;
- search and browse results may vary depending on who is logged in;
- colleagues outside your authorized group may not be able to see the same
  datasets you can see;
- you should not share Locally FAIR content with others who don't have access themselves; and
- this functionality is experimental.
