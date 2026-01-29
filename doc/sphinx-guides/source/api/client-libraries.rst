Client Libraries
================

Listed below are a variety of clienty libraries to help you interact with Dataverse APIs from Python, R, Javascript, etc.

To get support for any of these client libraries, please consult each project's README.

.. contents:: |toctitle|
	:local:

C/C++
-----

https://github.com/aeonSolutions/OpenScience-Dataverse-API-C-library is the official C/C++ library for Dataverse APIs.

This C/C++ library was created and is currently maintained by `Miguel T. <https://www.linkedin.com/in/migueltomas/>`_ To learn how to install and use it, see the project's `wiki page <https://github.com/aeonSolutions/OpenScience-Dataverse-API-C-library/wiki>`_.

Go
--
https://github.com/libis/rdm-dataverse-go-api is Go API library that can be used in your project by simply adding ``github.com/libis/rdm-dataverse-go-api`` as a dependency in your ``go.mod`` file. See the GitHub page for more details and usage examples.

Java
----

https://github.com/IQSS/dataverse-client-java is the official Java library for Dataverse APIs.

`Richard Adams <https://www.researchspace.com/electronic-lab-notebook/about_us_team.html>`_ from `ResearchSpace <https://www.researchspace.com>`_ created and maintains this library.

Javascript
----------

https://github.com/IQSS/dataverse-client-javascript is the official Javascript package for Dataverse APIs. It can be found on npm at https://www.npmjs.com/package/js-dataverse

It was created and is maintained by `The Agile Monkeys <https://www.theagilemonkeys.com>`_.

Julia
-----

https://github.com/gaelforget/Dataverse.jl is the official Julia package for Dataverse APIs. It can be found on JuliaHub (https://juliahub.com/ui/Packages/Dataverse/xWAqY/) and leverages pyDataverse to provide an interface to Dataverse's data access API and native API. Dataverse.jl provides a few additional functionalities with documentation (https://gaelforget.github.io/Dataverse.jl/dev/) and a demo notebook (https://gaelforget.github.io/Dataverse.jl/dev/notebook.html).

It was created and is maintained by `Gael Forget <https://github.com/gaelforget>`_.

PHP
---

There is no official PHP library for Dataverse APIs (please :ref:`get in touch <getting-help-developers>` if you'd like to create one!) but there is a SWORD library written in PHP listed under :ref:`client-libraries` in the :doc:`/api/sword` documentation.

Python
------

There are multiple Python modules for interacting with Dataverse APIs.

`EasyDataverse <https://github.com/gdcc/easyDataverse>`_ is a Python library designed to simplify the management of Dataverse datasets in an object-oriented way, giving users the ability to upload, download, and update datasets with ease. By utilizing metadata block configurations, EasyDataverse automatically generates Python objects that contain all the necessary details required to create the native Dataverse JSON format used to create or edit datasets. Adding files and directories is also possible with EasyDataverse and requires no additional API calls. This library is particularly well-suited for client applications such as workflows and scripts as it minimizes technical complexities and facilitates swift development.

`python-dvuploader <https://github.com/gdcc/python-dvuploader>`_ implements Jim Myers' excellent `dv-uploader <https://github.com/GlobalDataverseCommunityConsortium/dataverse-uploader>`_ as a Python module. It offers parallel direct uploads to Dataverse backend storage, streams files directly instead of buffering them in memory, and supports multi-part uploads, chunking data accordingly.

`pyDataverse <https://github.com/gdcc/pyDataverse>`_ primarily allows developers to manage Dataverse collections, datasets and datafiles. Its intention is to help with data migrations and DevOps activities such as testing and configuration management. The module is developed by `Stefan Kasberger <https://stefankasberger.at>`_ from `AUSSDA - The Austrian Social Science Data Archive <https://aussda.at>`_.  

`UBC's Dataverse Utilities <https://ubc-library-rc.github.io/dataverse_utils/>`_ are a set of Python console utilities which allow one to upload datasets from a tab-separated-value spreadsheet, bulk release multiple datasets, bulk delete unpublished datasets, quickly duplicate records. replace licenses, and more. For additional information see their `PyPi page <https://pypi.org/project/dataverse-utils/>`_.

`dataverse-client-python <https://github.com/IQSS/dataverse-client-python>`_ had its initial release in 2015. `Robert Liebowitz <https://github.com/rliebz>`_ created this library while at the `Center for Open Science (COS) <https://centerforopenscience.org>`_ and the COS uses it to integrate the `Open Science Framework (OSF) <https://osf.io>`_ with Dataverse installations via an add-on which itself is open source and listed on the :doc:`/api/apps` page.

`Pooch <https://github.com/fatiando/pooch>`_ is a Python library that allows library and application developers to download data. Among other features, it takes care of various protocols, caching in OS-specific locations, checksum verification and adds optional features like progress bars or log messages. Among other popular repositories, Pooch supports Dataverse in the sense that you can reference Dataverse-hosted datasets by just a DOI and Pooch will determine the data repository type, query the Dataverse API for contained files and checksums, giving you an easy interface to download them.

`idsc.dataverse <https://github.com/iza-institute-of-labor-economics/idsc.dataverse>`_ reads metadata and files of datasets from a dataverse dataverse.example1.com and writes them into ~/.idsc/dataverse/api/dataverse.example1.com organized in directories PID_type/prefix/suffix, where PID_type is one of: hdl, doi or ark. It can then ''export'' the local copy of the dataverse from ~/.idsc/dataverse/api/dataverse.example1.com to ~/.idsc/.cache/dataverse.example2.com so that one can upload them to dataverse.example2.com.

R
-

https://github.com/IQSS/dataverse-client-r is the official R package for Dataverse APIs. The latest release can be installed from `CRAN <https://cran.r-project.org/package=dataverse>`_. 
The R client can search and download datasets. It is useful when automatically (instead of manually) downloading data files as part of a script. For bulk edit and upload operations, we currently recommend pyDataverse.

The package is currently maintained by  `Shiro Kuriwaki <https://github.com/kuriwaki>`_. It was originally created by `Thomas Leeper <https://thomasleeper.com>`_ and then formerly maintained by `Will Beasley <https://github.com/wibeasley>`_.

Ruby
----

https://github.com/libis/dataverse_api is a Ruby gem for Dataverse APIs. It is registered as a library on Rubygems (https://rubygems.org/search?query=dataverse).

The gem is created and maintained by the LIBIS team (https://www.libis.be) at the University of Leuven (https://www.kuleuven.be).

Rust
----

https://github.com/gdcc/rust-dataverse

The Rust Dataverse client is a comprehensive crate designed for seamless interaction with the Dataverse API. It facilitates essential operations such as collection, dataset, and file management. Additionally, the crate includes a user-friendly command-line interface (CLI) that brings the full functionality of the library to the command line. This project is actively maintained by `Jan Range <https://github.com/jr-1991>`_.
