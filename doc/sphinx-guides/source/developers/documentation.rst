=====================
Writing Documentation
=====================

.. contents:: |toctitle|
	:local:

Quick Fix
-----------

If you find a typo or a small error in the documentation you can fix it using GitHub's online web editor. Generally speaking, we will be following https://help.github.com/en/articles/editing-files-in-another-users-repository

- Navigate to https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source where you will see folders for each of the guides: `admin`_, `api`_, `developers`_, `installation`_, `style`_, `user`_.
- Find the file you want to edit under one of the folders above.
- Click the pencil icon in the upper-right corner. If this is your first contribution to the Dataverse Project, the hover text over the pencil icon will say "Fork this project and edit this file".
- Make changes to the file and preview them.
- In the **Commit changes** box, enter a description of the changes you have made and click **Propose file change**.
- Under the **Write** tab, delete the long welcome message and write a few words about what you fixed.
- Click **Create Pull Request**.

That's it! Thank you for your contribution! Your pull request will be added manually to the main Dataverse Project board at https://github.com/orgs/IQSS/projects/2 and will go through code review and QA before it is merged into the "develop" branch. Along the way, developers might suggest changes or make them on your behalf. Once your pull request has been merged you will be listed as a contributor at https://github.com/IQSS/dataverse/graphs/contributors

Please see https://github.com/IQSS/dataverse/pull/5857 for an example of a quick fix that was merged (the "Files changed" tab shows how a typo was fixed).

Preview your documentation changes which will be built automatically as part of your pull request in Github.  It will show up as a check entitled: `docs/readthedocs.org:dataverse-guide — Read the Docs build succeeded!`.  For example, this PR built to https://dataverse-guide--9249.org.readthedocs.build/en/9249/.

If you would like to read more about the Dataverse Project's use of GitHub, please see the :doc:`version-control` section. For bug fixes and features we request that you create an issue before making a pull request but this is not at all necessary for quick fixes to the documentation.

.. _admin: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/admin
.. _api: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/api
.. _developers: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/developers
.. _installation: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/installation
.. _style: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/style
.. _user: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/user

Building the Guides with Sphinx
-------------------------------

The Dataverse guides are written using Sphinx (http://sphinx-doc.org). We recommend installing Sphinx on your localhost or using a Sphinx Docker container to build the guides locally so you can get an accurate preview of your changes.

In case you decide to use a Sphinx Docker container to build the guides, you can skip the next two installation sections, but you will need to have Docker installed.

Installing Sphinx
~~~~~~~~~~~~~~~~~

First, make a fork of https://github.com/IQSS/dataverse and clone your fork locally. Then change to the ``doc/sphinx-guides`` directory.

``cd doc/sphinx-guides``

Create a Python virtual environment, activate it, then install dependencies:

``python3 -m venv venv``

``source venv/bin/activate``

``pip install -r requirements.txt``

Installing GraphViz
~~~~~~~~~~~~~~~~~~~

In some parts of the documentation, graphs are rendered as images using the Sphinx GraphViz extension.

Building the guides requires the ``dot`` executable from GraphViz.

This requires having `GraphViz <http://graphviz.org>`_ installed and either having ``dot`` on the path or
`adding options to the make call <https://groups.google.com/forum/#!topic/sphinx-users/yXgNey_0M3I>`_.

Editing and Building the Guides
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To edit the existing documentation:

- Create a branch (see :ref:`how-to-make-a-pull-request`).
- In ``doc/sphinx-guides/source`` you will find the .rst files that correspond to http://guides.dataverse.org.
- Using your preferred text editor, open and edit the necessary files, or create new ones.

Once you are done, you can preview the changes by building the guides locally. As explained, you can build the guides with Sphinx locally installed, or with a Docker container.

Building the Guides with Sphinx Locally Installed
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Open a terminal, change directories to ``doc/sphinx-guides``, activate (or reactivate) your Python virtual environment, and build the guides.

``cd doc/sphinx-guides``

``source venv/bin/activate``

``make clean``

``make html``

Building the Guides with a Sphinx Docker Container
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you want to build the guides using a Docker container, execute the following command in the repository root:

``docker run -it --rm -v $(pwd):/docs sphinxdoc/sphinx:3.5.4 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make html"``

Previewing the Guides
^^^^^^^^^^^^^^^^^^^^^

After Sphinx is done processing the files you should notice that the ``html`` folder in ``doc/sphinx-guides/build`` directory has been updated.
You can click on the files in the ``html`` folder to preview the changes.

Now you can make a commit with the changes to your own fork in GitHub and submit a pull request. See :ref:`how-to-make-a-pull-request`.

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

Cross References
----------------

**NOTE:** When adding ReStructured Text (RST) `cross references <https://www.sphinx-doc.org/en/master/usage/restructuredtext/roles.html#ref-role>`_, use the hyphen character (``-``) as the word separator for the cross reference label. For example, ``my-reference-label`` would be the preferred label for a cross reference as opposed to, for example, ``my_reference_label``.

Versions
--------

For installations hosting their own copies of the guides, note that as each version of the Dataverse Software is released, there is an updated version of the guides released with it. Google and other search engines index all versions, which may confuse users who discover your guides in the search results as to which version they should be looking at. When learning about your installation from the search results, it is best to be viewing the *latest* version.

In order to make it clear to the crawlers that we only want the latest version discoverable in their search results, we suggest adding this to your ``robots.txt`` file::

        User-agent: *
        Allow: /en/latest/
        Disallow: /en/

PDF Version of the Guides
-------------------------

The HTML version of the guides is the official one. Any other formats are maintained on a best effort basis.

If you would like to build a PDF version of the guides and have Docker installed, please try the command below from the root of the git repo:

``docker run -it --rm -v $(pwd):/docs sphinxdoc/sphinx-latexpdf:3.5.4 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make latexpdf LATEXMKOPTS=\"-interaction=nonstopmode\"; cd ../.. && ls -1 doc/sphinx-guides/build/latex/Dataverse.pdf"``

A few notes about the command above:

- Hopefully the PDF was created at ``doc/sphinx-guides/build/latex/Dataverse.pdf``.
- For now, we are using "nonstopmode" but this masks some errors.
- See requirements.txt for a note regarding the version of Sphinx we are using.

Also, as of this writing we have enabled PDF builds from the "develop" branch. You download the PDF from http://preview.guides.gdcc.io/_/downloads/en/develop/pdf/

If you would like to help improve the PDF version of the guides, please get in touch! Please see :ref:`getting-help-developers` for ways to contact the developer community.

----

Previous: :doc:`testing` | Next: :doc:`dependencies`
