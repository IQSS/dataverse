PSI
===

PSI (Ψ) is a private data sharing interface: http://privacytools.seas.harvard.edu/psi

.. contents:: |toctitle|
  :local:
  
Introduction
------------

FIXME: Link to the User Guide once PSI has been added there.

Installation
------------

To install PSI for use with Dataverse, follow the steps below.

Add PSI as an External Tool
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download :download:`psi.json <../_static/installation/files/root/external-tools/psi.json>`

``curl -X POST -H 'Content-type: application/json' --upload-file psi.json http://localhost:8080/api/admin/externalTools``
