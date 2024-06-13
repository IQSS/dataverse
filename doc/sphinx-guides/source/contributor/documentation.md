# Writing Documentation

Thank you for your interest in contributing documentation to Dataverse! Good documentation is absolutely critical to the success of a software project.

```{contents} Contents:
:local:
:depth: 3
```

## Where the Source Files Live

The source for the documentation is in the main "dataverse" GitHub repo under the "[doc][]" folder.

[doc]: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source

## Tools

The {doc}`/developers/documentation` section of the Developer Guide has, for a long time, been the primary place to find information about the tools we use to write docs. We may move some of that content here, but until then, please visit that page.

## Writing Style Guidelines

Please observe the following when writing documentation:

- Use American English spelling.
- Use examples when possible.
- Break up longer paragraphs.
- Use "double quotes" instead of 'single quotes'.
- Favor "and" (data and code) over slashes (data/code).

## Table of Contents

Every non-index page should use the following code to display a table of contents of internal sub-headings. This code should be placed below any introductory text and directly above the first subheading, much like a Wikipedia page.

If the page is written in reStructuredText (.rst), use this form:

    .. contents:: |toctitle|
        :local:

If the page is written in Markdown (.md), use this form:

    ```{contents} Contents:
    :local:
    :depth: 3
    ```

## Images

A good documentation is just like a website enhanced and upgraded by adding high quality and self-explanatory images.  Often images depict a lot of written text in a simple manner. Within our Sphinx docs, you can add them in two ways: a) add a PNG image directly and include or b) use inline description languages like GraphViz (current only option).

While PNGs in the git repo can be linked directly via URL, Sphinx-generated images do not need a manual step and might provide higher visual quality. Especially in terms of quality of content, generated images can be extendend and improved by a textbased and reviewable commit, without needing raw data or source files and no diff around.

TODO: The above covers "how" but what about "when"?

## Cross References

When adding ReStructured Text (.rst) [cross references](https://www.sphinx-doc.org/en/master/usage/restructuredtext/roles.html#ref-role), use the hyphen character (`-`) as the word separator for the cross reference label. For example, `my-reference-label` would be the preferred label for a cross reference as opposed to, for example, `my_reference_label`.
