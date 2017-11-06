Data Privacy Management
++++++++++++++++++++++++

Dataverse offers a set of tools to help you protect your sensitive data from privacy leaks.

.. contents:: |toctitle|
  :local:


Restricting Access to Files
===========================
(Insert info about how to restrict access and unrestrict access to a file)

Granting Access to Specific Users
=================================
(Insert info about how to give specific users access to a restricted file)

DataTags
========

(Insert DataTags guide here)

PSI
======

`PSI (Ψ) <http://privacytools.seas.harvard.edu/psi/>`_ is a Private data Sharing Interface. 

Introduction
------------

The PSI tool can be integrated into Dataverse to allow researchers with sensitive or confidential datasets to make differentially private summary statistics about their data available. The PSI tool is used to introduce just enough noise to the summary statistics to ensure privacy while still allowing a useful (if blurry) window into the contents of the data. This way, Dataverse users who lack the permission to view the raw data can still learn something about that data without any sensitive or private information being leaked. The sensitive data remains safe, while interested parties can learn more about it before they decide to undergo the potentially difficult process of seeking approval to view it.

