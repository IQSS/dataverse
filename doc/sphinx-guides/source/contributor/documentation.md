# Writing Documentation

Thank you for your interest in contributing documentation to Dataverse! Good documentation is absolutely critical to the success of software.

```{contents} Contents:
:local:
:depth: 3
```

## Overview

The Dataverse guides are written using [Sphinx](https://sphinx-doc.org).

The source files are stored under [doc/sphinx-guides](https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides) in the main "dataverse" repo on GitHub.

Historically, guides have been written in the default Sphinx format, [reStructuredText][] (.rst), but newer guides such as the {doc}`/contributor/index` are written in [Markdown][] (.md).

[reStructuredText]: https://en.wikipedia.org/wiki/ReStructuredText
[Markdown]: https://en.wikipedia.org/wiki/Markdown

Below we'll present a technique for making quick edits to the guides using GitHub's web editor ("quick fix"). We'll also describe how to install Sphinx locally for more significant edits.

Finally, we'll provide some guidelines on writing content.

We could use some help on writing this very page and helping newcomers get started. Please don't be shy about suggesting improvements! You can open an issue at <https://github.com/IQSS/dataverse/issues>, post to <https://chat.dataverse.org>, write to the [mailing list](https://groups.google.com/g/dataverse-community), or suggest a change with a pull request.

## Quick Fix

If you find a typo or a small error in the documentation you can fix it using GitHub's online web editor. Generally speaking, we will be following [GitHub's guidance on editing files in another user's repository](https://docs.github.com/en/repositories/working-with-files/managing-files/editing-files#editing-files-in-another-users-repository).

- Navigate to <https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source> where you will see folders for each of the guides: [admin][], [api][], [container][], etc.
- Find the file you want to edit under one of the folders above.
- Click the pencil icon in the upper-right corner. If this is your first contribution to Dataverse, the hover text over the pencil icon will say "Fork this project and edit this file".
- Make changes to the file and preview them.
- In the **Commit changes** box, enter a description of the changes you have made and click **Propose file change**.
- Under the **Write** tab, delete the long welcome message and write a few words about what you fixed.
- Click **Create Pull Request**.

That's it! Thank you for your contribution! Your pull request will be added manually to the main Dataverse project board at <https://github.com/orgs/IQSS/projects/34> and will go through code review and QA before it is merged into the "develop" branch. Along the way, developers might suggest changes or make them on your behalf. Once your pull request has been merged you will be listed as a contributor at <https://github.com/IQSS/dataverse/graphs/contributors>! 🎉

Please see <https://github.com/IQSS/dataverse/pull/5857> for an example of a quick fix that was merged (the "Files changed" tab shows how a typo was fixed).

Preview your documentation changes which will be built automatically as part of your pull request in Github.  It will show up as a check entitled: `docs/readthedocs.org:dataverse-guide — Read the Docs build succeeded!`.  For example, this PR built to <https://dataverse-guide--9249.org.readthedocs.build/en/9249/>.

If you would like to read more about the Dataverse's use of GitHub, please see the {doc}`/developers/version-control` section of the Developer Guide. For bug fixes and features we request that you create an issue before making a pull request but this is not at all necessary for quick fixes to the documentation.

[admin]: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/admin
[api]: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/api
[container]: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source/container

## Building the Guides with Sphinx

While the "quick fix" technique shown above should work fine for minor changes, especially for larger changes, we recommend installing Sphinx on your computer or using a Sphinx Docker container to build the guides locally so you can get an accurate preview of your changes.

In case you decide to use a Sphinx Docker container to build the guides, you can skip the next two installation sections, but you will need to have Docker installed.

### Installing Sphinx

First, make a fork of <https://github.com/IQSS/dataverse> and clone your fork locally. Then change to the ``doc/sphinx-guides`` directory.

``cd doc/sphinx-guides``

Create a Python virtual environment, activate it, then install dependencies:

``python3 -m venv venv``

``source venv/bin/activate``

``pip install -r requirements.txt``

### Installing GraphViz

In some parts of the documentation, graphs are rendered as images using the Sphinx GraphViz extension.

Building the guides requires the ``dot`` executable from GraphViz.

This requires having [GraphViz](https://graphviz.org) installed and either having ``dot`` on the path or
[adding options to the `make` call](https://groups.google.com/forum/#!topic/sphinx-users/yXgNey_0M3I).

On a Mac we recommend installing GraphViz through [Homebrew](<https://brew.sh>). Once you have Homebrew installed and configured to work with your shell, you can type `brew install graphviz`.

### Editing and Building the Guides

To edit the existing documentation:

- Create a branch (see {ref}`how-to-make-a-pull-request`).
- In ``doc/sphinx-guides/source`` you will find the .rst files that correspond to https://guides.dataverse.org.
- Using your preferred text editor, open and edit the necessary files, or create new ones.

Once you are done, you can preview the changes by building the guides locally. As explained, you can build the guides with Sphinx locally installed, or with a Docker container.

#### Building the Guides with Sphinx Installed Locally

Open a terminal, change directories to `doc/sphinx-guides`, activate (or reactivate) your Python virtual environment, and build the guides.

`cd doc/sphinx-guides`

`source venv/bin/activate`

`make clean`

`make html`

#### Building the Guides with a Sphinx Docker Container and a Makefile

We have added a Makefile to simplify the process of building the guides using a Docker container, you can use the following commands from the repository root:

- `make docs-html`
- `make docs-pdf`
- `make docs-epub`
- `make docs-all`

#### Building the Guides with a Sphinx Docker Container and CLI

If you want to build the guides using a Docker container, execute the following command in the repository root:

`docker run -it --rm -v $(pwd):/docs sphinxdoc/sphinx:7.2.6 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make html"`

#### Previewing the Guides

After Sphinx is done processing the files you should notice that the `html` folder in `doc/sphinx-guides/build` directory has been updated. You can click on the files in the `html` folder to preview the changes.

Now you can make a commit with the changes to your own fork in GitHub and submit a pull request. See {ref}`how-to-make-a-pull-request`.

## Writing Guidelines

### Writing Style Guidelines

Please observe the following when writing documentation:

- Use American English spelling.
- Use examples when possible.
- Break up longer paragraphs.
- Use Title Case in Headings.
- Use "double quotes" instead of 'single quotes'.
- Favor "and" (data and code) over slashes (data/code).

### Table of Contents

Every non-index page should use the following code to display a table of contents of internal sub-headings. This code should be placed below any introductory text and directly above the first subheading, much like a Wikipedia page.

If the page is written in reStructuredText (.rst), use this form:

    .. contents:: |toctitle|
        :local:

If the page is written in Markdown (.md), use this form:

    ```{contents} Contents:
    :local:
    :depth: 3
    ```

### Links

Getting links right with .rst files can be tricky.

#### Custom Titles

You can use a custom title when linking to a document like this:

    :doc:`Custom title </api/intro>`

See also <https://docs.readthedocs.io/en/stable/guides/cross-referencing-with-sphinx.html#the-doc-role>

### Images

A good documentation is just like a website enhanced and upgraded by adding high quality and self-explanatory images.  Often images depict a lot of written text in a simple manner. Within our Sphinx docs, you can add them in two ways: a) add a PNG image directly and include or b) use inline description languages like GraphViz (current only option).

While PNGs in the git repo can be linked directly via URL, Sphinx-generated images do not need a manual step and might provide higher visual quality. Especially in terms of quality of content, generated images can be extendend and improved by a textbased and reviewable commit, without needing raw data or source files and no diff around.

### Cross References

When adding ReStructured Text (.rst) [cross references](https://www.sphinx-doc.org/en/master/usage/restructuredtext/roles.html#ref-role), use the hyphen character (`-`) as the word separator for the cross reference label. For example, `my-reference-label` would be the preferred label for a cross reference as opposed to, for example, `my_reference_label`.

## PDF Version of the Guides

The HTML version of the guides is the official one. Any other formats are maintained on a best effort basis.

If you would like to build a PDF version of the guides and have Docker installed, please try the command below from the root of the git repo:

`docker run -it --rm -v $(pwd):/docs sphinxdoc/sphinx-latexpdf:7.2.6 bash -c "cd doc/sphinx-guides && pip3 install -r requirements.txt && make latexpdf LATEXMKOPTS=\"-interaction=nonstopmode\"; cd ../.. && ls -1 doc/sphinx-guides/build/latex/Dataverse.pdf"`

A few notes about the command above:

- Hopefully the PDF was created at `doc/sphinx-guides/build/latex/Dataverse.pdf`.
- For now, we are using "nonstopmode" but this masks some errors.
- See requirements.txt for a note regarding the version of Sphinx we are using.

Also, as of this writing we have enabled PDF builds from the "develop" branch. You download the PDF from <http://preview.guides.gdcc.io/_/downloads/en/develop/pdf/>

If you would like to help improve the PDF version of the guides, please get in touch! Please see {ref}`getting-help-developers` for ways to contact the developer community.


## Hosting Your Own Version of the Guides

Some installations of Dataverse maintain their own versions of the guides and use settings like {ref}`:NavbarGuidesUrl` or {ref}`:GuidesBaseUrl` to point their users to them.

### Having Google Index the Latest Version

As each version of the Dataverse software is released, there is an updated version of the guides released with it. Google and other search engines index all versions, which may confuse users who discover your guides in the search results as to which version they should be looking at. When learning about your installation from the search results, it is best to be viewing the *latest* version.

In order to make it clear to the crawlers that we only want the latest version discoverable in their search results, we suggest adding this to your `robots.txt` file:

    User-agent: *
    Allow: /en/latest/
    Disallow: /en/
