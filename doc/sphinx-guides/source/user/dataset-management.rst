Dataset & File Management
+++++++++++++++++++++++++++++

A dataset in Dataverse is a container for your data, documentation, code, and the metadata describing this Dataset.

|image1|

New Dataset
====================

#. Navigate to the dataverse in which you want to add a dataset (or in the "root" dataverse). 
#. Click on the "Add Data" button and select "New Dataset" in the dropdown menu.
#. To quickly get started, enter at minimum all the required fields with an asterisk to get a Data Citation with a DOI (e.g., the Dataset Title, Author, 
   Description, etc)
#. Scroll down to the "Files" section and click on "Select Files to Add" to add all the relevant files to your Dataset. 
   You can also upload your files directly from your Dropbox. **Tip:** You can drag and drop or select multiple files at a time from your desktop,
   directly into the upload widget. Your files will appear below the "Select Files to Add" button where you can add a
   description for each file. Additionally, an MD5 checksum will be added for each file. If you upload a tabular file a :ref:`Universal Numerical Fingerprint (UNF) <unf>` will be added to this file.
#. Click the "Add Dataset" button when you are done. Your unpublished dataset is now created. 

Note: You can add additional metadata once you have completed the initial dataset creation by going to Edit Dataset. 


Edit Dataset
==================

Go to your dataset page and click on the "Edit Dataset" button. There you will have the following options where you can either edit:

- Files (Upload or Edit Data): to add or edit files in this dataset.
- Metadata: to add/edit metadata including additional metadata than was not previously available during Dataset Creation.
- Roles + Permissions
- Delete Dataset (only available before your Dataset is published)
- Deaccession Dataset (only when your Dataset is published, see below)

You can also directly select either the Metadata or File tabs found below the dataset summary information to specifically edit either of those parts of your dataset.

Publish Dataset
====================

When you publish a dataset (available to an admin, curator, or any custom role which has this level of permission assigned), you make it available to the public so that other users can browse or search for it. Once your dataset is ready to go public, go to your dataset page and click on the "Publish" button on the right hand side of the page. A pop-up will appear to confirm that you are ready to actually Publish since once a dataset is made public it can no longer be unpublished. 

Whenever you edit your dataset, you are able to publish a new version of the dataset. The publish dataset button will reappear whenever you edit the metadata of the dataset or add a file.

Note: Prior to publishing your dataset the Data Citation will indicate that this is a draft but the "DRAFT VERSION" text
will be removed as soon as you Publish.

Dataset Versioning
======================

Versioning is important for long term-research data management where metadata and/or files are updated over time.

Once you have published a dataset, any metadata or file changes (e.g, by uploading a new file, changing file metadata, adding 
or editing metadata) will be tracked in our versioning feature. For example if you were at version 1 of your dataset, and you
edit your dataset a new draft version of this dataset will be created. To get to the already published version 1 of your dataset,
click on the blue "View Published Version" button on the top right of your dataset. To go back to the unpublished version click on the orange "View Unpublished Version" button. Once you are ready to publish this new version of your dataset, select the "Publish Dataset" button on the top right side of the page. If you were at version 1 of your dataset, and depending on the types of changes you have made, you will be asked to select to publish your draft as either version 1.1 or version 2.0 (**important note**: if you add a file, your dataset will automatically be bumped up to version 2.0). 

|image2|

**Dataset Versions Tab**

To view what has exactly changed starting from the originally published version to any subsequent published versions: click on the Versions tab on the dataset page to see all versions and changes made for that particular dataset. Once you have more than one version (can be version 1 and a draft), you can click the Show Details link in the Versions tab to learn more about the metadata fields and files that were either added or edited. 

If you have more than two versions of a dataset, you can select any two versions to compare the differences between them. After selecting two versions, click on the "Show Differences" button to see the version differences details.

Deaccession Your Dataset [not recommended]
===============================================

Deaccessioning a dataset or a version of a dataset is a very serious action that should only occur if there is a legal or valid reason for the dataset to no longer be accessible to the public. If you absolutely must deaccession, you can deaccession a version of a dataset or an entire dataset. To deaccession, go to a dataset you’ve already published (or add a new one and publish it), click on Edit Dataset, then Deaccession Dataset. If you have multiple versions of a dataset, you can select here which versions you want to deaccession or choose to deaccession the entire dataset. You must also include a reason as to why this dataset was deaccessioned from a dropdown list of options. There is also a free-text box to add more details as to why this was deaccessioned. If the dataset has moved to a different repository or site you are encouraged to include a URL (preferably persistent) for users to continue to be able to access this dataset in the future.

**Important Note**: A tombstone landing page with the basic citation metadata will always be accessible to the public if they use the persistent URL (Handle or DOI) provided in the citation for that dataset.  Users will not be able to see any of the files or additional metadata that were previously available prior to deaccession.


.. |image1| image:: ./img/DatasetDiagram.png
.. |image2| image:: http://static.projects.iq.harvard.edu/files/styles/os_files_xxlarge/public/datascience/files/data_publishing_version_workflow.png?itok=8Z0PM-QC
