Configuring External Tools
==========================

The Dataverse Team encourages the wider community to contribute to our software. Sometimes these contributions take the form of external tools that can be integrated into Dataverse as modular features. This page introduces some of these tools. For more information or technical support on these tools, it's recommended that you reach out to their creators.

.. contents:: |toctitle|
  :local:

PSI
----
`PSI (Ψ) <http://privacytools.seas.harvard.edu/psi/>`_ is a Private data Sharing Interface created by the `Privacy Tools for Sharing Research Data project <http://privacytools.seas.harvard.edu/>`_. 

When integrated into Dataverse, the PSI tool allows researchers with sensitive tabular data to create safe, non-privacy-leaking summary statistics about their data. The PSI tool protects data using `differential privacy <https://privacytools.seas.harvard.edu/publications/differential-privacy-primer-non-technical-audience-preliminary-version>`_, a framework that provides a mathematical guarantee of privacy protection for any individual represented in the data. The PSI tool allows researchers depositing data to introduce just enough noise into their data's summary statistics to ensure privacy while still allowing a useful (if blurry) window into the contents of the data. 

In this way, Dataverse users who lack the permission to access the raw data can still learn something about that data through its summary statistics, without any sensitive or private information being leaked. The sensitive data remains safe, while interested parties can learn more about the data before they decide to undergo the potentially lengthy and effortful process of seeking approval to view it.


Installation
~~~~~~~~~~~~~

To install PSI for use with Dataverse, follow the steps below:

Download :download:`psi.json <../_static/installation/files/root/external-tools/psi.json>`

``curl -X POST -H 'Content-type: application/json' --upload-file psi.json http://localhost:8080/api/admin/externalTools``
