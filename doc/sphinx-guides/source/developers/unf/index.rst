.. _unf:

=====================================
Universal Numerical Fingerprint (UNF)
=====================================

.. figure:: ./img/unf-diagram.png
    :align: center
    :alt: alternate text
    :figclass: align-center

    Fig.1 UNF: used to uniquely identify and verify data.

Universal Numerical Fingerprint (UNF) is a unique signature of the
**semantic content** of a digital object. It is **not** simply a
checksum of a binary data file. Instead, the UNF algorithm
approximates and normalizes the data stored within. A cryptographic
hash of that normalized (or canonicalized) representation is then
computed. The signature is thus independent of the storage
format. E.g., the same data object stored in, say, SPSS and Stata,
will have the same UNF.

Early versions of the Dataverse Software were using the first released
implementation of the UNF algorithm (v.3, implemented in R).  Starting
with Dataverse Software 2.0 and throughout the 3.* lifecycle, UNF v.5
(implemented in Java) was used. Dataverse Software 4.0 uses the latest release,
UNF v.6. Two parallel implementation, in R and Java, will be
available, for cross-validation.

Learn more: Micah Altman and Gary King. 2007. “A Proposed Standard for the Scholarly Citation of Quantitative Data.” D-Lib Magazine, 13. Publisher’s Version Copy at https://j.mp/2ovSzoT

**Contents:**

.. toctree::
   :maxdepth: 2

   unf-v3
   unf-v5
   unf-v6
