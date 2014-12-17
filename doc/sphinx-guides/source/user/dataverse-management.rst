Dataverse Management
++++++++++++++++++++++++++++

A dataverse is a container for datasets (research data, code, documentation, and metadata) and other dataverses, 
which can be setup for individual researchers, departments, journals and organizations.

|image1|

Once a user creates a dataverse they, by default, become the
administrator of that dataverse. The dataverse administrator has access
to manage the settings described in this guide.

Create a Dataverse (within the "root" Dataverse)
===================================================

Creating a dataverse is easy but first you must be a registered user (see Create Account).

#. Once you are logged in click on the "Add Data" button and in the dropdown menu select "Create Dataverse".
#. Once on the "New Dataverse" page fill in the following fields:
    * **Name**: Enter the name of your dataverse.
    * **Alias**: This is an abbreviation, usually lower-case, that becomes part of the URL for the new dataverse. Special characters (~,\`, !, @, #, $, %, ^, &, and \*) and spaces are not allowed. **Note**: if you change the Dataverse URL field, the URL for your Dataverse changes (http//.../dv/'url'), which affects links to this page.
    * **E-mail**: This is the email address you will receive notifications for, for this particular dataverse.
    * **Affiliation**: Add any Affiliation that can be associated to this particular dataverse (e.g., project name, institute name, department name, journal name, etc). This is automatically filled out if you have added an affiliation for your user account.
    * **Description**: Provide a description of this dataverse (max. 1000 characters). This will display on the home page of your dataverse and in the search result list.
    * **Choose the sets of Metadata Elements for datasets in this dataverse**: by default the metadata elements will be from the host dataverse that this new dataverse is created in. Dataverse offers metadata standards for multiple domains. To learn more about the metadata standards in Dataverse please check out the appendix (insert link here)
    * **Select facets for this dataverse**: by default the facets that will appear on your dataverse landing page will be from the host dataverse that this new dataverse was created in. The facets are simply metadata fields that can be used to help others easily find dataverses and datasets within this dataverse. You can select as many facets as you would like.
#. Selected metadata elements are also used to pick which metadata fields you would like to use for creating templates for your datasets. Metadata fields can be hidden, or selected as required or optional. Once you have selected all the fields you would like to use, you can create your template(s) after you finish creating your dataverse.
#. Click "Create Dataverse" button and you're done! 

\*Required fields are denoted by a red asterisk.

Edit Dataverse 
=================

To edit your dataverse, navigate to your dataverse homepage and select the "Edit Dataverse" button, 
where you will be presented with the following editing options: 

- :ref:`General Information <general-information>` : edit name, host dataverse, alias, email, 
  description, affilitation, Metadata Elements, and facets for your dataverse.
- :ref:`Theme + Widgets <theme-widgets>` : upload a logo for your dataverse, add a link to your department or personal website, and select colors for your dataverse in order to brand it. Also, you can get code to add to your website to have your dataverse display on it.
- :ref:`Featured Dataverses <featured-dataverses>` : if you have one or more dataverses, you can use this option to show them at the top of your dataverse page to help others easily find interesting or important dataverses
- :ref:`Permissions <dataverse-permissions>` : give Dataverse users permissions to your dataverse, i.e.-can edit datasets, and see which users already have which permissions for your dataverse
- :ref:`Dataset Templates <dataset-templates>` : these are useful when you have several datasets that have the same information in multiple metadata fields that you would prefer not to have to keep manually typing in
- **Delete Dataverse**: you are able to delete your dataverse as long as it is not published and does not have any draft datasets 

.. _general-information:

General Information
=====================================================

The General Information page is the same as the page you filled out while creating your dataverse. If you need to change or add a contact email address, this is the place to do it. Additionally, you can update the metadata elements used for datasets within the dataverse, change which metadafields are hidden, required, or optional, and update the facets you would like displayed for browsing the dataverse. If you plan on using templates, you need to select the metadata fields on the General Information page.

Tip: The metadata fields you select as required, will appear on the Create Dataset form when someone goes to add a dataset to the dataverse. 

.. _theme-widgets:

Theme + Widgets
====================================================

The Theme + Widgets feature provides you with a way to customize the look of your dataverse as well as provide code for you to put on your personal website to have your dataverse appear there. (For adding your dataverse to an OpenScholar site, follow these instructions.)

For Theme, you can decide either to use the customization from the dataverse above yours or upload your own image file. Additionally, you can select the colors for the header of your dataverse and the text that appears in your dataverse. You can also add a link to your personal website, the website for your organization or institution, your department, journal, etc.

There are two options for Widgets, a Dataverse Search box widget and a Dataverse Listing widget. The Dataverse Search Box will add a search box to your website that when someone enters a search term in and clicks Find, will bring them to Dataverse to see the results. The Dataverse Listing widget will provide a listing of all your dataverses and datasets. When someone clicks on a dataverse or dataset in the widget, it will bring them to your dataverse to see the actual data.

.. _featured-dataverses:

Featured Dataverses
======================================================

Featured Dataverses is a way to display sub dataverses in your dataverse that you want to feature for people to easily see when they visit your dataverse. If you have more than one sub dataverse, Featured Dataverses will appear under the Edit Dataverse button. 

Click on Featured Dataverses and a pop up will appear. Check the box that says, "Display "Featured Dataverses" Carousel on Front Page" and then select which dataverses you would like to have appear. 

Note: Featured Dataverses can only be used with published dataverses.

.. _dataverse-permissions:

Permissions 
=======================================================



Publish Your Dataverse
=================================================================

Once your dataverse is ready to go public, go to your dataverse page, click on the "Publish" button on the right 
hand side of the page. A pop-up will appear to confirm that you are ready to actually Publish, since once a dataverse
is made public, it can no longer be unpublished.


.. |image1| image:: ./img/Dataverse-Diagram.png

.. _dataset-templates: 

Dataset Templates
======================
Templates are useful when you have several datasets that have the same information in multiple metadata fields that you would prefer not to have to keep manually typing in. In Dataverse 4.0, templates are created at the dataverse level, can be deleted (so it does not show for future datasets), set to default (not required), and can be copied so you do not have to start over when creating a new template with similiar metadata from another template. When a template is deleted, it does not impact the datasets that have used the template already.

How do you create a template? 

#. Navigate to your dataverse, click on the Edit Dataverse button and select Dataset Templates. 
#. Once you have clicked on Dataset Templates, you will be brought to the Dataset Templates page. On this page, you can 1) decide to use the dataset templates from your parent dataverse 2) create a new dataset template or 3) do both.
#. Click on the Create Dataset Template to get started. You will see that the template is the same as the create dataset page with an additional field at the top of the page to add a name for the template.
#. After adding information into the metadata fields you have information for and clicking save, you will be brought back to the Manage Dataset Templates page and should see your template listed there now with several options. 
#. A dataverse does not have to have a default template and users can select which template they would like to use while on the Create Dataset page. 
#. You can also click on the View button on the Manage Dataset Templates page to see what metadata fields have information filled in.

\* Please note that the ability to choose which metadata fields are hidden, required, or optional is done on the General Information page for the dataverse.







