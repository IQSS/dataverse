Standard
+++++++++

The citation standard defined here offers proper recognition to authors as well as permanent identification through the use of global, persistent identifiers in place of URLs, which can change frequently. Use of universal numerical fingerprints (UNFs) guarantees to the scholarly community that future researchers will be able to verify that data retrieved is identical to that used in a publication decades earlier, even if it has changed storage media, operating systems, hardware, and statistical program format.

**Following are two authentic examples of replication data citations:**

From International Studies Quarterly, King and Zeng, 2006, p. 209:

    Gary King; Langche Zeng, 2006, "Replication data for: When Can
    History be Our Guide? The Pitfalls of Counterfactual Inference",
    Harvard Dataverse, V2, http://hdl.handle.net/1902.1/DXRXCFAWPK UNF:3:DaYlT6QSX9r0D50ye+tXpA==

From Political Analysis, Hanmer, Banks, and White, 2013:

    Hanmer, Michael J.; Banks, Antoine J., White, Ismail K., 2013,
    "Replication data for: Experiments to Reduce the Over-reporting of
    Voting: A Pipeline to the Truth", Harvard Dataverse, V1, http://dx.doi.org/10.7910/DVN/22893 UNF:5:eJOVAjDU0E0jzSQ2bRCg9g==

This citation has seven components. Five are human readable: the author(s), title, year, data repository (or distributor), and version number. Two components are machine-readable:

#. Of the machine-readable components to these citations, the unique global identifier begins with either "hdl" (this refers to the international `HANDLE.NET <http://www.handle.net/>`_ system) or "doi" (this refers to a `Digital Object Identifier (DOI) <http://www.doi.org/>`_ system). This identifier is designed to persist even if URLs--or the web itself--are replaced with something else. When the citation appears online, the identifier is hot-linked to the URL that references the identifier, which works in browsers available today. In print, the URL is also included in the citation.

#. The universal numerical fingerprint begins with "UNF". Four features make the UNF especially useful: The UNF algorithm's cryptographic technology ensures that the alphanumeric identifier will change when any portion of the data set changes. Not only does this assure future researchers that they can use the same data set referenced in a years-old journal article, it enables the data set's owner to track each iteration of the owner's research. When an original data set is updated or incorporated into a new, related data set, the algorithm generates a unique UNF each time. The UNF is determined by the content of the data, not the format in which it is stored. For example, you create a data set in SPSS, Stata or R, and five years later, you need to look at your data set again, but the data was converted to the next big thing (NBT). You can use NBT, recompute the UNF, and verify for certain that the data set you're downloading is the same one you created originally. That is, the UNF will not change. Knowing only the UNF, journal editors can be confident that they are referencing a specific data set that never can be changed, even if they do not have permission to see the data. In a sense, the UNF is the ultimate summary statistic. The UNF's noninvertible, cryptographic properties guarantee that acquiring the UNF of a data set conveys no information about the content of the data. Authors can take advantage of this property to distribute the full citation of a data set--including the UNF--even if the data is proprietary or highly confidential, all without the risk of disclosure.

For information on how to implement the Universal Numerical Fingerprint (UNF), see `"A Fingerprint Method for the Verification of Scientific Data" <http://datascience.iq.harvard.edu/publications/fingerprint-method-verification-scientific-data>`_.

**Learn more:**

#. Micah Altman and Gary King. (2007). "A Proposed Standard for the Scholarly Citation of Quantitative Data," D-Lib Magazine, Vol. 13, No. 3/4 (March). `Link <http://datascience.iq.harvard.edu/publications/proposed-standard-scholarly-citation-quantitative-data>`_.
#. Paul E. Uhlir, R., Board on Research Data, Information, Policy, Global Affairs, & National Research Council. (2012). For attribution -- developing data attribution and citation practices and standards: Summary of an international workshop. The National Academies Press. `Link <http://www.nap.edu/openbook.php?record_id=13564>`_
