PSI
===

`PSI (Ψ) <http://privacytools.seas.harvard.edu/psi/>`_ is a Private data Sharing Interface. 
.. contents:: |toctitle|
  :local:
  
Introduction
------------

The PSI tool can be integrated into Dataverse to allow researchers with sensitive or confidential datasets to make differentially private summary statistics about their data available. The PSI tool is used to introduce just enough noise to the summary statistics to ensure privacy while still allowing a useful (if blurry) window into the contents of the data.  This way, Dataverse users who lack the permission to view the raw data can still learn something about that data without any sensitive or private information being leaked. The sensitive data remains safe, while interested parties can learn more about it before they decide to undergo the potentially difficult process of seeking approval to view it.

Installation
------------

To install PSI for use with Dataverse, follow the steps below.

Add PSI as an External Tool
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download :download:`psi.json <../_static/installation/files/root/external-tools/psi.json>`

``curl -X POST -H 'Content-type: application/json' --upload-file psi.json http://localhost:8080/api/admin/externalTools``
