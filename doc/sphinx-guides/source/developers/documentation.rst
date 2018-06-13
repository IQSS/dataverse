=============
Documentation
=============

.. contents:: |toctitle|
	:local:

Quick Fix
-----------

If you find a typo or a small error in the documentation you can easily fix it using GitHub.

- Fork the repository
- Go to [your GitHub username]/dataverse/doc/sphinx-guides/source and access the file you would like to fix
- Switch to the branch that is currently under development
- Click the Edit button in the upper-right corner and fix the error
- Submit a pull request

Other Changes (Sphinx)
----------------------

The documentation for Dataverse was written using Sphinx (http://sphinx-doc.org/). 
If you are interested in suggesting changes or updates we recommend that you create 
the html files using Sphinx locally and the submit a pull request through GitHub. Here are the instructions on how to proceed:


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

This is all you need. You should now be able to build HTML/pdf documentation from git sources locally.

Using Sphinx
~~~~~~~~~~~~

First, you will need to make a fork of the dataverse repository in GitHub. Then, you will need to make a clone of your fork so you can manipulate the files outside GitHub.

To edit the existing documentation go to ~/dataverse/doc/sphinx-guides/source directory inside your clone. There, you will find the .rst files that correspond to the guides in the dataverse page (http://guides.dataverse.org/en/latest/user/index.html). Now, using your preferred text editor, open and edit these files, or create new .rst files and edit the others accordingly. 

Once you are done, open a terminal and change directories to ~/dataverse/doc/sphinx-guides . Then, run the following commands:

- ``make clean``

- ``make html``

After sphinx is done processing the files you should notice that the html folder in ~/dataverse/doc/sphinx-guides/build directory has been updated.
You can click on the files in the html folder to preview the changes.

Now you can make a commit with the changes to your own fork in GitHub and submit a pull request to the dataverse repository.

Table of Contents
-----------------

Every non-index page should use the following code to display a table of contents of internal sub-headings: ::

	.. contents:: |toctitle|
		:local:

This code should be placed below any introductory text/images and directly above the first subheading, much like a Wikipedia page.

Versions
--------

For installations hosting their own copies of the guides, note that as each version of Dataverse is released, there is an updated version of the guides released with it. Google and other search engines index all versions, which may confuse users who discover your guides in the search results as to which version they should be looking at. When learning about your installation from the search results, it is best to be viewing the *latest* version.

In order to make it clear to the crawlers that we only want the latest version discoverable in their search results, we suggest adding this to your ``robots.txt`` file::

        User-agent: *
        Allow: /en/latest/
        Disallow: /en/

----

Previous: :doc:`testing` | Next: :doc:`debugging`
