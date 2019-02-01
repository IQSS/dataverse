Finding and Using Data
+++++++++++++++++++++++

.. contents:: |toctitle|
    :local:

Finding Data
============

Without logging in to Dataverse, users can browse Dataverse, search for dataverses, datasets, and files, view dataset descriptions and files for
published datasets, and subset, analyze, and visualize data for published (restricted & not restricted) data files. To view an unpublished dataverse, dataset, or file, a user will need to be given permission from that dataverse's administrator to access it.

A user can search within a specific dataverse for the dataverses, datasets, and files it contains by using the search bar and facets displayed on that dataverse's page.

Basic Search
------------
You can search the entire contents of the Dataverse installation, including dataverses, datasets, and files. You can access the search through the search bar on the homepage, or by clicking the magnifying glass icon in the header of every page. The search bar accepts search terms, queries, or exact phrases (in quotations).

Sorting and Viewing Search Results
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Facets: to the left of the search results, there are several facets a user can click on to narrow the number of results displayed.
    - Choosing a facet: to choose a facet to narrow your results by, click on that facet.
    - Removing a facet: A chosen facet can be removed by clicking on the X on it, either in the facets panel to the left, or above the results.
    - Viewing more or fewer facets: Each category in the facets panel lists the top 5 most common facets from that category. To view more, click on "More..." in the bottom right of that category. Once you've chosen to see more, an option to view less will appear in the bottom left of the facet.
   
Result cards: after entering a search term or query, result cards that match your term or query appear underneath the search bar and to the right of the facets.
    - Relevancy of results: each result card shows which metadata fields match the search query or term you entered into the search bar, with the matching term or query bolded. If the search term or query was found in the title or name of the dataverse, dataset, or file, the search term or query will be bolded within it.

Other basic search features: 
    - Sorting results: search results can be sorted by name (A-Z or Z-A), by date (newest or oldest), or by relevancy of results. The sort button can be found above the search results, in the top right.
    - Bookmarkable URLs: search URLs can be copied and sent to a fellow researcher, or can be bookmarked for future sessions.

Advanced Search 
---------------

To perform an advanced search, click the "Advanced Search" link next to the search bar. There you will have the ability to 
enter search terms for dataverses, dataset metadata (citation and domain-specific), and file-level 
metadata. If you are searching for tabular data files you can also search at the variable level for name and label. To find 
out more about what each field searches, hover over the field name for a detailed description of the field.

Browsing Dataverse
------------------

In Dataverse, browsing is the default view when a user hasn't begun a search on the homepage or on a specific dataverse's page.  When browsing, only dataverses and datasets appear in the results list and the results can be sorted by Name (A-Z or Z-A) and by Newest or Oldest.

Saved Search
------------

Saved Search is currently an experimental feature only available to superusers. Please see the :doc:`/api/native-api` section of the API Guide for more information.

Using Data
==========

View Dataverses + Datasets
--------------------------

After performing a search and finding the dataverse or dataset you are looking for, click on the name of the dataverse or dataset or on the thumbnail image to be taken to the page for that dataverse or dataset. Once on a dataverse page, you can view the dataverses, datasets, and files within that dataverse.

Once on a dataset page, you will see the title, citation, description, and several other fields, as well as a button to email the dataset contact and a button to share the dataset on social media. Below that information, the files, metadata, terms of use, and version information for the dataset are available. 

View Files
----------

Files in Dataverse each have their own page that can be reached through the search results or through the Files table on their parent dataset's page. The dataset page and file page offer much the same functionality in terms of viewing and editing files, with a few small exceptions. The file page includes the file's persistent identifier (DOI or handle), which can be found under the Metadata tab. Also, the file page's Versions tab gives you a version history that is more focused on the individual file rather than the dataset as a whole. 

Cite Data
---------

You can find the citation for the dataset at the top of the dataset page in a blue box. Additionally, there is a Cite Data button that offers the option to download the citation as EndNote XML, RIS Format, or BibTeX Format.

.. _download_files:

Download Files
--------------

Within the Files tab on a dataset page, you can download the files in that dataset. To download more than one file at a time, select the files you would like to download and then click the Download button above the files. The selected files will download in zip format.

You may also download a file from its file page by clicking the Download button in the upper right corner of the page, or by :ref:`url_download` under the Metadata tab on the lower half of the page.

Tabular data files offer additional options: You can explore using any data exploration or visualization :doc:`/installation/external-tools` (if they have been enabled) by clicking the Explore button, or choose from a number of tabular-data-specific download options available as a dropdown under the Download button.

Tabular Data
^^^^^^^^^^^^

Ingested files can be downloaded in several different ways. 

- The default option is to download a tab-separated-value file which is an easy and free standard to use.

- The original file, which may be in a proprietary format which requires special software

- Rdata format if the instalation has configured this

- The variable metadata for the file in DDI format

- A subset of the columns of the data


.. _url_download:

Downloading via URL
^^^^^^^^^^^^^^^^^^^^

Dataverse displays a plaintext URL for the location of the file under the Metadata tab on the dataset page. This Download URL can be used to directly access the file via API (or in a web browser, if needed). When downloading larger files, in order to ensure a reliable, resumable download, we recommend using `GNU Wget <https://www.gnu.org/software/wget/>`_ in a command line terminal or using a download manager software of your choice.
 
Certain files do not provide Download URLs for technical reasons: those that are restricted, have terms of use associated with them, or are part of a dataverse with a guestbook enabled. 

.. _package_download_url:

Downloading a Dataverse Package via URL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Dataverse Packages are typically used to represent extremely large files or bundles containing a large number of files. Dataverse Packages are often too large to be reliably downloaded using a web browser. When you click to download a Dataverse Package, instead of automatically initiating the download in your web browser, Dataverse displays a plaintext URL for the location of the file. To ensure a reliable, resumable download, we recommend using `GNU Wget <https://www.gnu.org/software/wget/>`_ in a command line terminal or using a download manager software of your choice. If you try to simply paste the URL into your web browser then the download may overwhelm your browser, resulting in an interrupted, timed out, or otherwise failed download.

.. _rsync_download:

Downloading a Dataverse Package via rsync
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

rsync is typically used for synchronizing files and directories between two different systems. Some Dataverse installations allow downloads using rsync, to facilitate large file transfers in a reliable and secure manner.

rsync-enabled Dataverse installations offer a new file download process that differs from traditional browser-based downloading. Instead of multiple files, each dataset uploaded via rsync contains a single "Dataverse Package". When you download this package you will receive a folder that contains all files from the dataset, arranged in the exact folder structure in which they were originally uploaded.

In a dataset containing a Dataverse Package, at the bottom of the dataset page, under the **Data Access** tab, instead of a download button you will find the information you need in order to download the Dataverse Package using rsync. If the data is locally available to you (on a shared drive, for example) then you can find it at the folder path under **Local Access**. Otherwise, to download the Dataverse Package you will have to use one of the rsync commands under **Download Access**. There may be multiple commands listed, each corresponding to a different mirror that hosts the Dataverse Package. Go outside your browser and open a terminal (AKA command line) window on your computer. Use the terminal to run the command that corresponds with the mirror of your choice. It's usually best to choose the mirror that is geographically closest to you. Running this command will initiate the download process.

After you've downloaded the Dataverse Package, you may want to double-check that your download went perfectly. Under **Verify Data**, you'll find a command that you can run in your terminal that will initiate a checksum to ensure that the data you downloaded matches the data in Dataverse precisely. This way, you can ensure the integrity of the data you're working with. 

Explore Data
------------

Please see the :doc:`/user/data-exploration/index`.
