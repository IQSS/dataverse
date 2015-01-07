Search API
==========

About
-----

The Search API supports the same searching, sorting, and faceting operations as the Dataverse web interface.

The parameters and JSON response are partly inspired by the `GitHub Search API <https://developer.github.com/v3/search/>`_.

Parameters
----------

=====  ===========
Name   Description
=====  ===========
q      The search term or terms. Using "title:data" will search only the "title" field.
fq     A filter query on the search term. Multiple "fq" parameters can be used.
sort   The sort field.
order  The order in which to sort, either be "asc" or "desc".
=====  ===========

Examples
--------

Searching a title
~~~~~~~~~~~~~~~~~

https://apitest.dataverse.org/api/search?q=title:awesome+data
