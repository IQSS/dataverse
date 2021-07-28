=====================
Writing Documentation
=====================

.. contents:: |toctitle|
	:local:

Quick Fix
-----------

If you find a typo or a small error in the documentation you can fix it using GitHub's online web editor. Generally speaking, we will be following https://help.github.com/en/articles/editing-files-in-another-users-repository

- Navigate to https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source where you will see folders for each of the guides:

  - `admin`_
  - `api`_
  - `developers`_
  - `installation`_
  - `user`_

- Find the file you want to edit under one of the folders above.
- Click the pencil icon in the upper-right corner. If this is your first contribution to the Dataverse Project, the hover text over the pencil icon will say "Fork this project and edit this file".
- Make changes to the file and preview them.
- In the **Commit changes** box, enter a description of the changes you have made and click **Propose file change**.
- Under the **Write** tab, delete the long welcome message and write a few words about what you fixed.
- Click **Create Pull Request**.

That's it! Thank you for your contribution! Your pull request will be added manually to the main Dataverse Project board at https://github.com/orgs/IQSS/projects/2 and will go through code review and QA before it is merged into the "develop" branch. Along the way, developers might suggest changes or make them on your behalf. Once your pull request has been merged you will be listed as a contributor at https://github.com/IQSS/dataverse/graphs/contributors

Please see https://github.com/IQSS/dataverse/pull/5857 for an example of a quick fix that was merged (the "Files changed" tab shows how a typo was fixed).

If you would like to read more about the Dataverse Project's use of GitHub, please see the :doc:`version-control` section. For bug fixes and features we request that you create an issue before making a pull request but this is not at all necessary for quick fixes to the documentation.

.. _admin: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/admin
.. _api: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/api
.. _developers: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/developers
.. _installation: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/installation
.. _user: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/user

Other Changes (Sphinx)
----------------------

The documentation for the Dataverse Project was written using Sphinx (http://sphinx-doc.org/). 
If you are interested in suggesting changes or updates we recommend that you create 
the html files using Sphinx locally and then submit a pull request through GitHub. Here are the instructions on how to proceed:


Installing Sphinx
~~~~~~~~~~~~~~~~~

On a Mac: 

Download the sphinx zip file from http://sphinx-doc.org/install.html

Unzip it somewhere. In the unzipped directory, do the following as
root, (sudo -i):

python setup.py build
python setup.py install

Alternative option (Mac/Unix/Windows):

Unless you already have it, install pip (https://pip.pypa.io/en/latest/installing.html)

run ``pip install sphinx`` in a terminal

Building the guides requires the ``dot`` executable from GraphViz and you can find documentation on GraphViz below.

This is all you need. You should now be able to build HTML/pdf documentation from git sources locally.

Using Sphinx
~~~~~~~~~~~~

First, you will need to make a fork of the Dataverse Software repository in GitHub. Then, you will need to make a clone of your fork so you can manipulate the files outside GitHub.

To edit the existing documentation:

- Create a branch (refer to http://guides.dataverse.org/en/latest/developers/version-control.html > *Create a New Branch off the develop Branch*) to record the changes you are about to perform.
- Go to ~/dataverse/doc/sphinx-guides/source directory inside your clone. There, you will find the .rst files that correspond to the guides in the Dataverse Software Guides page (http://guides.dataverse.org/en/latest/).
- Using your preferred text editor, open and edit the necessary files, or create new ones.

**NOTE:** When adding ReStructured Text (RST) `cross references <https://www.sphinx-doc.org/en/master/usage/restructuredtext/roles.html#ref-role>`_, use the hyphen character (``-``) as the word separator for the cross reference label. For example, ``my-reference-label`` would be the preferred label for a cross reference as opposed to, for example, ``my_reference_label``.

Once you are done, open a terminal and change directories to ~/dataverse/doc/sphinx-guides . Then, run the following commands:

- ``make clean``

- ``make html``

After sphinx is done processing the files you should notice that the html folder in ~/dataverse/doc/sphinx-guides/build directory has been updated.
You can click on the files in the html folder to preview the changes.

Now you can make a commit with the changes to your own fork in GitHub and submit a pull request to the original (upstream) Dataverse Software repository.

Table of Contents
-----------------

Every non-index page should use the following code to display a table of contents of internal sub-headings: ::

	.. contents:: |toctitle|
		:local:

This code should be placed below any introductory text/images and directly above the first subheading, much like a Wikipedia page.

Images
------

A good documentation is just like a website enhanced and upgraded by adding high quality and self-explanatory images.
Often images depict a lot of written text in a simple manner. Within our Sphinx docs, you can add them in two ways: a) add a
PNG image directly and include or b) use inline description languages like GraphViz (current only option).

While PNGs in the git repo can be linked directly via URL, Sphinx-generated images do not need a manual step and might
provide higher visual quality. Especially in terms of quality of content, generated images can be extendend and improved
by a textbased and reviewable commit, without needing raw data or source files and no diff around.

GraphViz based images
~~~~~~~~~~~~~~~~~~~~~

In some parts of the documentation, graphs are rendered as images via Sphinx GraphViz extension.

This requires `GraphViz <http://graphviz.org/>`_ installed and either ``dot`` on the path or
`adding options to the make call <https://groups.google.com/forum/#!topic/sphinx-users/yXgNey_0M3I>`_.

This has been tested and works on Mac, Linux, and Windows.

Versions
--------

For installations hosting their own copies of the guides, note that as each version of the Dataverse Software is released, there is an updated version of the guides released with it. Google and other search engines index all versions, which may confuse users who discover your guides in the search results as to which version they should be looking at. When learning about your installation from the search results, it is best to be viewing the *latest* version.

In order to make it clear to the crawlers that we only want the latest version discoverable in their search results, we suggest adding this to your ``robots.txt`` file::

        User-agent: *
        Allow: /en/latest/
        Disallow: /en/

----

Previous: :doc:`testing` | Next: :doc:`dependencies`
