Data Access API
===============

The Data Access API provides programmatic download access to the files stored in a Dataverse installation. 
More advanced features of the Access API include format-specific transformations (thumbnail generation/resizing for images; converting tabular data into alternative file formats) and access to the data-level metadata that describes the contents of the tabular files. 

.. contents:: |toctitle|
   :local:

.. _download-by-dataset-api:

Downloading All Files in a Dataset
----------------------------------

The "download by dataset" API downloads as many files as possible from a dataset as a zipped bundle.

By default, tabular files are downloaded in their "archival" form (tab-separated values). To download the original files (Stata, for example), add ``format=original`` as a query parameter.

There are a number of reasons why not all of the files can be downloaded:

- Some of the files are restricted and your API token doesn't have access (you will still get the unrestricted files).
- The Dataverse installation has limited how large the zip bundle can be.

In the curl example below, the flags ``-O`` and ``J`` are used. When there are no errors, this has the effect of saving the file as "dataverse_files.zip" (just like the web interface). The flags force errors to be downloaded as a file.

Please note that in addition to the files from dataset, an additional file call "MANIFEST.TXT" will be included in the zipped bundle. It has additional information about the files.

There are two forms of the "download by dataset" API, a basic form and one that supports dataset versions.

Basic Download By Dataset
~~~~~~~~~~~~~~~~~~~~~~~~~

The basic form downloads files from the latest accessible version of the dataset. If you are not using an API token, this means the most recently published version. If you are using an API token with full access to the dataset, this means the draft version or the most recently published version if no draft exists.

A curl example using a DOI (no version):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.70122/FK2/N2XGBJ

  curl -L -O -J -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/access/dataset/:persistentId/?persistentId=$PERSISTENT_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -L -O -J -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/access/dataset/:persistentId/?persistentId=doi:10.70122/FK2/N2XGBJ

Download By Dataset By Version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The second form of the "download by dataset" API allows you to specify which version you'd like to download files from. As with the ``datasets`` API endpoints described in the :doc:`native-api` section, the following identifiers can be used.

* ``:draft``  the draft version, if any
* ``:latest`` either a draft (if exists) or the latest published version.
* ``:latest-published`` the latest published version
* ``x.y`` a specific version, where ``x`` is the major version number and ``y`` is the minor version number.
* ``x`` same as ``x.0``

A curl example using a DOI (with version):

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_ID=doi:10.70122/FK2/N2XGBJ
  export VERSION=2.0

  curl -O -J -H "X-Dataverse-key:$API_TOKEN" $SERVER_URL/api/access/dataset/:persistentId/versions/$VERSION?persistentId=$PERSISTENT_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -O -J -H X-Dataverse-key:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx https://demo.dataverse.org/api/access/dataset/:persistentId/versions/2.0?persistentId=doi:10.70122/FK2/N2XGBJ

Basic File Access
-----------------

Basic access URI: 

``/api/access/datafile/$id``

.. note:: Files can be accessed using persistent identifiers. This is done by passing the constant ``:persistentId`` where the numeric id of the file is expected, and then passing the actual persistent id as a query parameter with the name ``persistentId``. However, this file access method is only effective when the FilePIDsEnabled option is enabled, which can be authorized by the admin. For further information, refer to :ref:`:FilePIDsEnabled`. 

  Example: Getting the file whose DOI is *10.5072/FK2/J8SJZB* ::

    GET http://$SERVER/api/access/datafile/:persistentId?persistentId=doi:10.5072/FK2/J8SJZB


Parameters:
~~~~~~~~~~~

``format`` 

the following parameter values are supported (for tabular data files only):

==============  ===========
Value           Description
==============  ===========
original        "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
RData           Tabular data as an R Data frame (generated; unless the "original" file was in R);
prep		"Pre-processed data", in JSON.
subset          Column-wise subsetting. You must also supply a comma separated list of variables in the "variables" query parameter. In this example, 123 and 127 are the database ids of data variables that belong to the data file with the id 6: ``curl 'http://localhost:8080/api/access/datafile/6?format=subset&variables=123,127'``.
==============  ===========

---------------------------

``noVarHeader``

(supported for tabular data files only; ignored for all other file types)

==============  ===========
Value           Description
==============  ===========
true|1          Tab-delimited data file, without the variable name header (added to tab. files by default)
==============  ===========

---------------------------

``imageThumb``

the following parameter values are supported (for image and pdf files only): 

==============  ===========
Value           Description
==============  ===========
true            Generates a thumbnail image by rescaling to the default thumbnail size (64 pixels wide).
``N``           Rescales the image to ``N`` pixels wide. ``imageThumb=true`` and ``imageThumb=64`` are equivalent.
==============  ===========

Headers:
~~~~~~~~

==============  ===========
Header          Description
==============  ===========
Range           Download a specified byte range. Examples:

                - ``bytes=0-9`` gets the first 10 bytes.
                - ``bytes=10-19`` gets 10 bytes from the middle.
                - ``bytes=-10`` gets the last 10 bytes.
                - ``bytes=9-`` gets all bytes except the first 10.

                Only a single range is supported. The "If-Range" header is not supported. For more on the "Range" header, see https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
==============  ===========

Examples
~~~~~~~~

A curl example of using the ``Range`` header to download the first 10 bytes of a file using its file id (database id):

.. code-block:: bash

  export SERVER_URL=https://demo.dataverse.org
  export FILE_ID=42
  export RANGE=0-9

  curl -H "Range:bytes=$RANGE" $SERVER_URL/api/access/datafile/$FILE_ID

The fully expanded example above (without environment variables) looks like this:

.. code-block:: bash

  curl -H "Range:bytes=0-9" https://demo.dataverse.org/api/access/datafile/42

Multiple File ("bundle") download
---------------------------------

``/api/access/datafiles/$id1,$id2,...$idN``

Alternate Form: POST to ``/api/access/datafiles`` with a ``fileIds`` input field containing the same comma separated list of file ids. This is most useful when your list of files surpasses the allowed URL length (varies but can be ~2000 characters).  

Returns the files listed, zipped. 

.. note:: If the request can only be completed partially - if only *some* of the requested files can be served (because of the permissions and/or size restrictions), the file MANIFEST.TXT included in the zipped bundle will have entries specifying the reasons the missing files could not be downloaded. IN THE FUTURE the API will return a 207 status code to indicate that the result was a partial success. (As of writing this - v.4.11 - this hasn't been implemented yet)

.. note:: If any of the datafiles have the ``DirectoryLabel`` attributes in the corresponding ``FileMetadata`` entries, these will be added as folders to the Zip archive, and the files will be placed in them accordingly. 

Parameters: 
~~~~~~~~~~~

``format`` 
the following parameter values are supported (for tabular data files only):

==============  ===========
Value           Description
==============  ===========
original        "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
==============  ===========


"All Formats" bundled download for Tabular Files. 
-------------------------------------------------

``/api/access/datafile/bundle/$id``

This is a convenience packaging method available for tabular data files. 
It returns a zipped bundle that contains the data in the following formats: 

* Tab-delimited;
* "Saved Original", the proprietary (SPSS, Stata, R, etc.) file from which the tabular data was ingested;
* Generated R Data frame (unless the "original" above was in R);
* Data (Variable) metadata record, in DDI XML;
* File citation, in Endnote and RIS formats. 


Parameters: 
~~~~~~~~~~~

``fileMetadataId``

==============  ===========
Value           Description
==============  ===========
ID              Exports file with specific file metadata ``ID``.
==============  ===========

.. _data-variable-metadata-access:

Data Variable Metadata Access
-----------------------------

**These methods are only available for tabular data files. (i.e., data files with associated data table and variable objects).**


``/api/access/datafile/$id/metadata/ddi``

In its basic form the verb above returns a DDI fragment that describes the file and the data variables in it. 

The DDI returned will only have two top-level sections:

* a single ``fileDscr``, with the basic file information plus the numbers of variables and observations and the UNF of the file.  
* a single ``dataDscr`` section, with one ``var`` section for each variable. 

Example: 

``http://localhost:8080/api/access/datafile/6/metadata/ddi``

.. code-block:: xml

   <codeBook version="2.0">
      <fileDscr ID="f6">
         <fileTxt>
            <fileName>_73084.tab</fileName>
            <dimensns>
               <caseQnty>3</caseQnty>
               <varQnty>2</varQnty>
            </dimensns>
            <fileType>text/tab-separated-values</fileType>
         </fileTxt>
         <notes level="file" type="VDC:UNF" subject="Universal Numeric Fingerprint">UNF:6:zChnyI3fjwNP+6qW0VryVQ==</notes>
      </fileDscr>
      <dataDscr>
         <var ID="v1" name="id" intrvl="discrete">
            <location fileid="f6"/>
            <labl level="variable">Personen-ID</labl>
            <sumStat type="mean">2.0</sumStat>
            <sumStat type="mode">.</sumStat>
            <sumStat type="medn">2.0</sumStat>
            <sumStat type="stdev">1.0</sumStat>
            <sumStat type="min">1.0</sumStat>
            <sumStat type="vald">3.0</sumStat>
            <sumStat type="invd">0.0</sumStat>
            <sumStat type="max">3.0</sumStat>
            <varFormat type="numeric"/>
            <notes subject="Universal Numeric Fingerprint" level="variable" type="VDC:UNF">UNF:6:AvELPR5QTaBbnq6S22Msow==</notes>
         </var>
         <var ID="v3" name="sex" intrvl="discrete">
            <location fileid="f6"/>
            <labl level="variable">Geschlecht</labl>
            <sumStat type="mean">1.3333333333333333</sumStat>
            <sumStat type="max">2.0</sumStat>
            <sumStat type="vald">3.0</sumStat>
            <sumStat type="mode">.</sumStat>
            <sumStat type="stdev">0.5773502691896257</sumStat>
            <sumStat type="invd">0.0</sumStat>
            <sumStat type="medn">1.0</sumStat>
            <sumStat type="min">1.0</sumStat>
            <catgry>
               <catValu>1</catValu>
               <labl level="category">Mann</labl>
            </catgry>
            <catgry>
               <catValu>2</catValu>
               <labl level="category">Frau</labl>
            </catgry>
            <varFormat type="numeric"/>
            <notes subject="Universal Numeric Fingerprint" level="variable" type="VDC:UNF">UNF:6:XqQaMwOA63taX1YyBzTZYQ==</notes>
         </var>
      </dataDscr>
   </codeBook>


Parameters: 
~~~~~~~~~~~

``fileMetadataId``

==============  ===========
Value           Description
==============  ===========
ID              Exports file with specific file metadata ``ID``. For example for data file with id 6 and file metadata id 2: ``curl 'http://localhost:8080/api/access/datafile/6/metadata/ddi?fileMetadataId=2'``
==============  ===========


More information on DDI is available in the :doc:`/user/tabulardataingest/ingestprocess` section of the User Guide.

Advanced options/Parameters: 

It is possible to request only specific subsets of, rather than the
full file-level DDI record. This can be a useful optimization, in
cases such as when an application needs to look up a single variable;
especially with data files with large numbers of variables. See
``variables=123,127`` in the example above.

Preprocessed Data
-----------------

``/api/access/datafile/$id?format=prep``

This method provides the "preprocessed data" - a summary record that describes the values of the data vectors in the tabular file, in JSON. These metadata values are used by earlier versions of Data Explorer, an external tool that integrates with a Dataverse installation (see :doc:`/admin/external-tools`). Please note that this format might change in the future.

Authentication and Authorization
-------------------------------- 

Data Access API supports both session- and API key-based authentication. 

If a session is available, and it is already associated with an authenticated user, it will be used for access authorization. If not, or if the user in question is not authorized to access the requested object, an attempt will be made to authorize based on an API key, if supplied. 
All of the API verbs above support the key parameter ``key=...`` as well as the newer ``X-Dataverse-key`` header. For more details, see "Authentication" in the :doc:`intro` section.

Access Requests and Processing
------------------------------

All of the following endpoints take the persistent identifier as a parameter in place of 'id'.

Allow Access Requests:
~~~~~~~~~~~~~~~~~~~~~~

Allow or disallow users from requesting access to restricted files in a dataset where id is the database id of the dataset or pid is the persistent id (DOI or Handle) of the dataset to update. 

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true http://$SERVER/api/access/{id}/allowAccessRequest
    
A curl example using a ``pid``::

   curl -H "X-Dataverse-key:$API_TOKEN" -X PUT -d true http://$SERVER/api/access/:persistentId/allowAccessRequest?persistentId={pid}    
    

Request Access:
~~~~~~~~~~~~~~~
``/api/access/datafile/$id/requestAccess``

This method requests access to the datafile whose id is passed on the behalf of an authenticated user whose key is passed. Note that not all datasets allow access requests to restricted files. 

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT http://$SERVER/api/access/datafile/{id}/requestAccess
    
Grant File Access:
~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/grantAccess/{identifier}``

This method grants access to the datafile whose id is passed on the behalf of an authenticated user whose user identifier is passed with an @ prefix. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT http://$SERVER/api/access/datafile/{id}/grantAccess/{@userIdentifier}
    
Reject File Access:
~~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/rejectAccess/{identifier}``

This method rejects the access request to the datafile whose id is passed on the behalf of an authenticated user whose user identifier is passed with an @ prefix. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X PUT http://$SERVER/api/access/datafile/{id}/rejectAccess/{@userIdentifier}
    
Revoke File Access:
~~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/revokeAccess/{identifier}``

This method revokes previously granted access to the datafile whose id is passed on the behalf of an authenticated user whose user identifier is passed with an @ prefix. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X DELETE http://$SERVER/api/access/datafile/{id}/revokeAccess/{@userIdentifier}    
    
List File Access Requests:
~~~~~~~~~~~~~~~~~~~~~~~~~~ 

``/api/access/datafile/{id}/listRequests``

This method returns a list of Authenticated Users who have requested access to the datafile whose id is passed. The key of a user who can manage permissions of the datafile is required to use this method.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X GET http://$SERVER/api/access/datafile/{id}/listRequests

User Has Requested Access to a File:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``/api/access/datafile/{id}/userFileAccessRequested``

This method returns true or false depending on whether or not the calling user has requested access to a particular file.

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X GET "http://$SERVER/api/access/datafile/{id}/userFileAccessRequested"


Get User Permissions on a File:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``/api/access/datafile/{id}/userPermissions``

This method returns the permissions that the calling user has on a particular file.

In particular, the user permissions that this method checks, returned as booleans, are the following:

* Can download the file
* Can manage the file permissions
* Can edit the file owner dataset

A curl example using an ``id``::

    curl -H "X-Dataverse-key:$API_TOKEN" -X GET "http://$SERVER/api/access/datafile/{id}/userPermissions"
