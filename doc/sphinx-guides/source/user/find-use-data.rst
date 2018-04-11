Finding and Using Data
+++++++++++++++++++++++

.. contents:: |toctitle|
	:local:

Finding Data
============

Without logging in to Dataverse, users can browse Dataverse, search for dataverses, datasets, and files, view dataset descriptions and files for
published datasets, and subset, analyze, and visualize data for published (restricted & not restricted) data files. To view an unpublished dataverse, dataset, or file, a user will need to be given permission from that dataverse's administrator to access it.

A user can search the dataverses, datasets, and files within a particular dataverse by using the search bar found on a dataverse page. For example, if you are on the Murray Research Archive Dataverse page, you can search that specific dataverse's contents by using the search bar and/or facets displayed on the page.

Basic Search
------------
You can search the entire contents of the Dataverse installation, including dataverses, datasets, and files. You can access the search through the search bar on the homepage, or by clicking the magnifying glass icon in the header of every page. The search bar accepts search terms, queries, or exact phrases (in quotations).

Sorting and Viewing Search Results
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

Cite Data
---------

You can find the citation for the dataset at the top of the dataset page in a blue box. Additionally, there is a Cite Data button that offers the option to download the citation as EndNote XML, RIS Format, or BibTeX Format.

.. _download_files:

Download Files
--------------

Within the Files tab on a dataset page, you can download the files in that dataset. To download more than one file at a time, select the files you would like to download and then click the Download button above the files. The selected files will download in zip format.

Tabular data files offer additional options: You can explore using the TwoRavens data visualization tool (or other :doc:`/installation/external-tools` if they have been enabled) by clicking the Explore button, or choose from a number of tabular-data-specific download options available as a dropdown under the Download button.


.. _rsync_download:

Downloading a Dataverse Package via rsync
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

rsync is typically used for synchronizing files and directories between two different systems, using SSH to connect rather than HTTP. Some Dataverse installations allow downloads using rsync, to facilitate large file transfers in a reliable and secure manner.

rsync-enabled Dataverse installations have a new file download process that differs from traditional browser-based downloading. Instead of multiple files, each dataset contains a single "Dataverse Package". When you download this package you will receive a folder that contains all files from the dataset, arranged in the exact folder structure in which they were originally uploaded.

At the bottom of the dataset page, under the **Data Access** tab, instead of a download button you will find the information you need in order to download a Dataverse Package using rsync. If the data is locally available to you (on a shared drive, for example) then you can find it at the folder path under **Local Access**. Otherwise, to download the Dataverse Package you will have to use one of the rsync commands under **Download Access**. There may be multiple commands listed, each corresponding to a different mirror that hosts the Dataverse Package. Go outside your browser and open a terminal (AKA command line) window on your computer. Use the terminal to run the command that corresponds with the mirror of your choice. It's usually best to choose the mirror that is geographically closest to you. Running this command will initiate the download process.

After you've downloaded the Dataverse Package, you may want to double-check that your download went perfectly. Under **Verify Data**, you'll find a command that you can run in your terminal that will initiate a checksum to ensure that the data you downloaded matches the data in Dataverse precisely. This way, you can ensure the integrity of the data you're working with. 

Explore Data
------------

Please see the :doc:`/user/data-exploration/index`.
