Data Privacy Management
++++++++++++++++++++++++

Dataverse offers a set of tools to help you protect your sensitive data from privacy leaks.

.. contents:: |toctitle|
  :local:


Restricting Access to Files
===========================
(Insert info about how to restrict access and unrestrict access to a file)

Granting Access to Specific Users
---------------------------------
(Insert info about how to give specific users access to a restricted file)

DataTags
========

(Insert DataTags guide here)

PSI
======

`PSI (Ψ) <http://privacytools.seas.harvard.edu/psi/>`_ is a Private data Sharing Interface. If the administrators of the Dataverse have enabled the PSI tool, it can be used to create a privacy-preserving data preview of any restricted tabular data file.

Introduction
------------

When integrated into Dataverse, the PSI tool allows researchers with sensitive tabular data to create safe, non-privacy-leaking summary statistics about their data. The PSI tool protects data using `differential privacy <https://privacytools.seas.harvard.edu/publications/differential-privacy-primer-non-technical-audience-preliminary-version>`_, a framework that provides a mathematical guarantee of privacy protection for any individual represented in the data. The PSI tool allows researchers depositing data to introduce just enough noise into their data's summary statistics to ensure privacy while still allowing a useful (if blurry) window into the contents of the data. 

In this way, Dataverse users who lack the permission to access the raw data can still learn something about that data through its summary statistics, without any sensitive or private information being leaked. The sensitive data remains safe, while interested parties can learn more about it before they decide to undergo the potentially difficult process of seeking approval to view it.

Creating a Privacy-Preserving Data Preview using PSI
----------------------------------------------------
TO BE ADDED: describe workflow -- Click Configure --> Privacy Preview, see modal, click Configure button, it takes you to the PSI Budgeter tool. Follow the instructions provided by the budgeter tool. Return to Dataverse when finished.



Exploring a Data File using PSI
-------------------------------
TO BE ADDED: Explain how to get to the TwoRavens page for a file that's been through PSI, explain that it's essentially TwoRavens, link to TwoRavens documentation.
