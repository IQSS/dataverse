Patterns
++++++++

Patters are what emerge when using the foundation elements together with basic objects, like buttons and alerts, as well as more complex Javascript components from `Bootstrap <http://getbootstrap.com/components/>`__ like tooltips and dropdowns and AJAX components from `PrimeFaces <https://www.primefaces.org/showcase/>`__ like datatables and commandlinks.


Navbar
======

The navbar is a component from `Bootstrap <http://getbootstrap.com/components/#navbar>`__, which spans the top of the application, and contains the logo/branding, aligned to the left, plus search form and links, aligned to the right.

When logged in, the account name is a dropdown menu, linking the user to account specific content, as well as the log out link.

*TO-DO...* This UI example isn't working here. Should UI example be a screenshot?

* Logo/Branding
* Search
* Links
* Account

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	  	
  		<nav id="navbarFixed" class="navbar navbar-default"><!-- navbar-fixed-top -->
            <div class="container">
                <div class="navbar-header">
                    <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#topNavBar">
                        <span class="sr-only">Toggle navigation</span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <a href="#" onclick="return false;">
                        <span class="navbar-brand"><i id="icon-dataverse" class="icon-dataverse"></i> Dataverse</span>
                    </a>
                </div>
                <div class="collapse navbar-collapse" id="topNavBar">
                    <ul class="nav navbar-nav navbar-right">
                        <li>
                            <a id="navbar-search-toggle" title="Search all dataverses...">
                                <span class="glyphicon glyphicon-search"></span>
                            </a>
                        </li>
                        <form class="navbar-form navbar-left" role="search" style="display:none;">
                            <div class="form-group">
                                <label class="sr-only" for="navbarsearch">Search</label>
                                <input id="navbarsearch" type="text" class="search-input ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all" size="28" value="" placeholder="Search all dataverses...">
                            </div>
                            <button type="submit" class="btn btn-default" onclick="return false;">
                                <span class="glyphicon glyphicon-search"></span> Find
                            </button>
                        </form>
                        <li><a href="#" onclick="return false;">
                                About
                            </a>
                        </li>
                        <li class="dropdown">
                            <span id="dataverseSupportLink" class="dropdown-toggle" data-toggle="dropdown">
                                Guides <b class="caret"></b>
                            </span>
                            <ul class="dropdown-menu">
                                <li><a href="#" onclick="return false;">
                                        User Guide
                                    </a>
                                </li>
                                <li><a href="#" onclick="return false;">Developer Guide</a>
                                </li>
                                <li><a href="#" onclick="return false;">Installation Guide</a>
                                </li>
                                <li><a href="#" onclick="return false;">API Guide</a>
                                </li><li><a href="#" onclick="return false;">Admin Guide</a></li>
                            </ul>
                        </li>
                        <form name="support" method="post" action="#" class="navbar-form navbar-left navbar-form-link">
							<a href="#" class="ui-commandlink ui-widget" onclick="return false;">Support</a>
						</form>
						<li><a href="#" onclick="return false;">Dashboard</a></li>
                        <li class="dropdown accountName">
                            <span id="lnk_header_account_dropdown" class="dropdown-toggle" data-toggle="dropdown">
                                <span class="glyphicon glyphicon-user text-danger"></span>
                                <span id="userDisplayInfoTitle" class="text-danger">Dataverse Admin</span>
                                <span class="label label-danger">26</span>
                                <b class="caret"></b>
                            </span>
                            <ul class="dropdown-menu">
                                <li><a href="#" onclick="return false;">My Data</a>
                                </li>
                                <li><a href="#" onclick="return false;">Notifications<span class="label label-danger">26</span></a>
                                </li>
                                
                                <li><a href="#" onclick="return false;">Account Information</a>
                                </li>
                                <li><a href="#" onclick="return false;">API Token</a>
                                </li>
                                <li class="divider"></li>
                                <li class="logout">
									<form id="j_idt98" name="j_idt98" method="post" action="/dataverse.xhtml" class="navbar-form navbar-left" role="logout">
										<a href="#" onclick="return false">Log Out</a>
									</form>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
  		
	  </div>
	</div>

.. code-block:: html

    <nav id="navbarFixed" class="navbar navbar-default navbar-fixed-top">
    	<div class="container">
    		...
    	</div>
    </nav>


Header
======

???... Maybe this belongs in the next page, "Templates"...

The header is the top section of the page, immediately under the navbar, which contains the dataverse name, as well as configurable content like the logo, tagline and link.

* Name
* Logo
* Tagline

*TO-DO...* This UI example isn't working here. Are screenshots better than code examples? PrimeFaces CSS? Custom themes? ...??

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	  	<div style="background:#F5F5F5;" id="dataverseHeader" class="container bg-muted">
            <div class="dataverseHeaderBlock">
            	<div class="dataverseHeaderCell dataverseHeaderName">
                    <a href="#" class="dataverseHeaderDataverseName" style="color:#428BCA;">Your Name Dataverse</a>
                </div>
                <div class="dataverseHeaderCell dataverseHeaderLink">
                	<a href="#" style="color:#428BCA;" target="_blank">Here is your tagline.</a>
                </div>
            </div>
        </div>
	  </div>
	</div>

.. code-block:: html

  <span class="name">...</span>
  <span class="name">...</span>

Breadcrumbs
===========

The breadcrumbs are displayed under the header, and provide a trail of links for users to navigate the hierarchy of containing objects from file to dataset to dataverse.

* Links

*TO-DO...* should we change how we do breadcrumbs in order to follow this http://getbootstrap.com/components/#breadcrumbs

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	  	
	  		BREADCRUMBS HERE
  		
	  </div>
	</div>

.. code-block:: html

    <div id="breadcrumbNavBlock" class="container" jsf:rendered="#{true}">
        <ui:repeat value="#{page.breadcrumbs}" var="breadcrumb" varStatus="status">
            <h:outputText value=" > " styleClass="breadcrumbCarrot" rendered="#{true}"/>
            <div class="breadcrumbBlock">
                ...
            </div>
        </ui:repeat>
    </div>


Tables
======

Most tables are DataTable components from `PrimeFaces <https://www.primefaces.org/showcase/ui/data/datatable/basic.xhtml>`__.

* DataTable
* Search Results
* Dataset Files

*TO-DO...* fix the PrimeFaces CSS in example below...

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
    	<div class="ui-datatable ui-widget">
	      	<div class="ui-datatable-tablewrapper">
	      		<table role="grid">
	      			<thead>
	      				<tr role="row">
	      					<th class="ui-state-default ui-selection-column col-select-width text-center" role="columnheader"><span class="ui-column-title"></span><div class="ui-chkbox ui-chkbox-all ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div></th><th class="ui-state-default col-sm-1 text-center" role="columnheader"><span class="ui-column-title">Dataset</span></th><th class="ui-state-default" role="columnheader"><span class="ui-column-title">Summary</span></th><th class="ui-state-default col-sm-3" role="columnheader"><span class="ui-column-title">Contributors</span></th><th class="ui-state-default col-sm-2" role="columnheader"><span class="ui-column-title">Published</span></th>
	  					</tr>
					</thead>
					<tbody class="ui-datatable-data ui-widget-content">
						<tr data-ri="0" class="ui-widget-content ui-datatable-even ui-datatable-selectable" role="row" aria-selected="false">
							<td role="gridcell" class="ui-selection-column col-select-width text-center">
								<div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
							</td>
							<td role="gridcell" class="col-sm-1 text-center">
		                    	<a href="#" class="ui-commandlink ui-widget" onclick="return false;">3.0</a>
			                </td>
			                <td role="gridcell">
			                	<span class="highlightBold">Files (Changed File Metadata: 1); </span><a href="#" class="ui-commandlink ui-widget" onclick="return false;">View Details</a>
			                </td>
			                <td role="gridcell" class="col-sm-3">Dataverse Admin</td>
			                <td role="gridcell" class="col-sm-2"><span>March 8, 2017</span>
		                    </td>
		                </tr>
		                <tr data-ri="1" class="ui-widget-content ui-datatable-odd ui-datatable-selectable" role="row" aria-selected="false">
		                    <td role="gridcell" class="ui-selection-column col-select-width text-center">
		                    	<div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
		                    </td>
		                    <td role="gridcell" class="col-sm-1 text-center">
		                    	<a href="#" class="ui-commandlink ui-widget" onclick="return false;">2.0</a>
		                    </td>
		                    <td role="gridcell">
		                    	<span class="highlightBold">Additional Citation Metadata: </span> (1 Added); <a href="#" class="ui-commandlink ui-widget" onclick="return false;">View Details</a>
		                	</td>
		                	<td role="gridcell" class="col-sm-3">Dataverse Admin</td><td role="gridcell" class="col-sm-2"><span>January 25, 2017</span>
		                    </td>
		                </tr>
		                <tr data-ri="2" class="ui-widget-content ui-datatable-even ui-datatable-selectable" role="row" aria-selected="false">
		                	<td role="gridcell" class="ui-selection-column col-select-width text-center">
		                		<div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
			                </td>
			                <td role="gridcell" class="col-sm-1 text-center">
			                    <a href="#" class="ui-commandlink ui-widget" onclick="return false;">1.1</a></td><td role="gridcell"><span class="highlightBold">Additional Citation Metadata: </span> (1 Added); <a href="#" class="ui-commandlink ui-widget" onclick="return false;">View Details</a>
			                </td>
			                <td role="gridcell" class="col-sm-3">Dataverse Admin</td>
			                <td role="gridcell" class="col-sm-2"><span>October 25, 2016</span></td>
		                </tr>
		                <tr data-ri="3" class="ui-widget-content ui-datatable-odd ui-datatable-selectable" role="row" aria-selected="false">
		                	<td role="gridcell" class="ui-selection-column col-select-width text-center">
		                		<div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
			                </td>
			                <td role="gridcell" class="col-sm-1 text-center">
			                    <a href="#" class="ui-commandlink ui-widget" onclick="return false;">1.0</a></td><td role="gridcell">
			                    This is the first published version.
			                </td>
			                <td role="gridcell" class="col-sm-3">Dataverse Admin</td>
			                <td role="gridcell" class="col-sm-2"><span>September 19, 2016</span></td>
		                </tr>
		            </tbody>
		        </table>
		    </div>
		</div>
    </div>
  </div>

.. code-block:: html

   <p:dataTable id="itemTable" styleClass="headerless-table margin-top"
                value="#{page.item}" var="item" widgetVar="itemTable">
       <p:column>
       	...
       </p:column>
   </p:dataTable>


Forms
=====

Forms can take many forms across the site, but we try to style them consistently. We use the `.form-horizontal` layout, which uses `.form-group` to create a grid of rows for the labels and inputs.

* Horizontal
* Labels
* Tooltips
* Inputs
* Validation

*TO-DO...* Write out a description. Build some examples out.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      FORMS

    </div>
  </div>

.. code-block:: html

  <span class="name">...</span>
  <span class="name">...</span>


Buttons
=======

Action Buttons
--------------

For action buttons on a page, we include an icon and text label, and they are generally aligned to the right side of the page.

* Edit
* Find
* Add Data
* Publish
* Download
* Explore
* ...etc.

*TO-DO...* Add the Primefaces CSS?

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	  	<!-- Edit Button -->
	  	<div class="btn-group pull-right">
            <button type="button" id="editDataSet" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="true">
                <span class="glyphicon glyphicon-pencil"></span> Edit <span class="caret"></span>
            </button>
            <ul class="dropdown-menu pull-right text-left" role="menu">
                <li>
                	<a href="/editdatafiles.xhtml?datasetId=8&amp;mode=UPLOAD">Files (Upload)</a>
                </li>
                <li>
                	<a id="datasetForm:editMetadata" href="#" class="ui-commandlink ui-widget" onclick="PrimeFaces.ab({s:&quot;datasetForm:editMetadata&quot;,u:&quot;datasetForm datasetForm messagePanel&quot;,onco:function(xhr,status,args){javascript:post_edit_metadata();}});return false;">Metadata</a>
                </li>
                <li>
                	<a id="datasetForm:editTerms" href="#" class="ui-commandlink ui-widget" onclick="PrimeFaces.ab({s:&quot;datasetForm:editTerms&quot;,u:&quot;datasetForm datasetForm messagePanel&quot;,onco:function(xhr,status,args){javascript:post_edit_terms();}});return false;">Terms</a>
                </li>
                <li class="dropdown-submenu pull-left">
                    <a tabindex="-1" href="#">Permissions</a>
                    <ul class="dropdown-menu">
                        <li>
                        	<a id="datasetForm:managePermissions" name="datasetForm:managePermissions" href="/permissions-manage.xhtml?id=8" class="ui-commandlink ui-widget">Dataset</a>
                        </li>
                        <li>
                        	<a id="datasetForm:manageFilePermissions" name="datasetForm:manageFilePermissions" href="/permissions-manage-files.xhtml?id=8" class="ui-commandlink ui-widget">File</a>
                        </li>
                    </ul>
                </li>
                <li>
                	<a id="datasetForm:privateUrl" href="#" class="ui-commandlink ui-widget" onclick="PrimeFaces.ab({s:&quot;datasetForm:privateUrl&quot;,u:&quot;datasetForm:privateUrlPanel&quot;,onco:function(xhr,status,args){PF('privateUrlConfirmation').show();}});return false;">Private URL</a>
                </li>
                <li>
                	<a href="/dataset-widgets.xhtml?datasetId=8">Thumbnails + Widgets</a>
                </li>
                <li class="divider"></li>
                <li>
                	<a id="datasetForm:deaccessionDatasetLink" href="#" class="ui-commandlink ui-widget" onclick="PF('deaccessionBlock').show();PrimeFaces.ab({s:&quot;datasetForm:deaccessionDatasetLink&quot;,u:&quot;datasetForm:deaccessionBlock&quot;,onco:function(xhr,status,args){PF('deaccessionBlock').show();bind_bsui_components();;}});return false;">Deaccession Dataset</a>
                </li>
            </ul>
        </div>
	  </div>
	</div>

.. code-block:: html

    <!-- Edit Button -->
    <div class="btn-group" jsf:rendered="#{true}">
        <button type="button" id="editDataSet" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
            <span class="glyphicon glyphicon-pencil"/> Edit <span class="caret"></span>
        </button>
        <ul class="dropdown-menu pull-right text-left" role="menu">
            <li>
                <h:outputLink value="#">
                    <h:outputText value="Files (Upload)"/>
                </h:outputLink>
            </li>
            ...
            <ui:fragment rendered="#{true}">
                <li class="dropdown-submenu pull-left">
                    <a tabindex="-1" href="#">Permissions</a>
                    <ul class="dropdown-menu">
                        <li>
                            <h:link id="managePermissions" styleClass="ui-commandlink ui-widget" outcome="permissions-manage">
                                <h:outputText value="Dataset" />
                                <f:param name="id" value="#{bean}" />
                            </h:link>
                        </li>
                        <li>
                            <h:link id="manageFilePermissions" styleClass="ui-commandlink ui-widget" outcome="permissions-manage-files">
                                <h:outputText value="File" />
                                <f:param name="id" value="#{bean}" />
                            </h:link>
                        </li>
                    </ul>
                </li>
                ...
            </ui:fragment>
            ...
        </ul>
    </div>

Form Buttons
------------

Form buttons are typically at the bottom of a form, aligned to the left, and do not have icons, just text labels.

* Save
* Continue
* Cancel

*TO-DO...* Need more examples?

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	  	<div class="button-block">
	  		<button id="datasetForm:save" name="datasetForm:save" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" onclick="return false;" tabindex="1000" type="submit" role="button" aria-disabled="false">
	  			<span class="ui-button-text ui-c">Save Changes</span>
	  		</button>
	  		<button id="datasetForm:cancel" name="datasetForm:cancel" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" onclick="return false;" tabindex="1000" type="submit" role="button" aria-disabled="false">
	  			<span class="ui-button-text ui-c">Cancel</span>
	  		</button>
  		</div>
	  </div>
	</div>

.. code-block:: html

    <div class="button-block" jsf:rendered="#{true}">
        <p:commandButton tabindex="1000" id="save" value="Save Changes" onclick="checkNewlyRestricted();PF('blockDatasetForm').show();" />
        <p:commandButton tabindex="1000" id="cancel" value="Cancel" action="#{bean}" process="@this" update="@form" rendered="#{true}" oncomplete="javascript:post_cancel_edit_files_or_metadata()">
            <f:setPropertyActionListener target="#{bean}" value="#{DatasetPage.editMode == 'METADATA' ? 1 : DatasetPage.selectedTabIndex}"/>
        </p:commandButton>
        <p:button id="cancelCreate" value="Cancel" outcome="/dataverse.xhtml?alias=#{DatasetPage.dataset.owner.alias}" rendered="#{true}" />
    </div>

Icon-Only Buttons
-----------------

There are a few places we use icon-only buttons with no text label. We do utilize tooltips that display on hover which contain a text label.

* Search
* Add
* Contact
* Link
* Share

*TO-DO...* fix the tooltip + Bootstrap JS in example below...

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	  	<div class="btn-group" id="datasetButtonBar" role="group">
	  		<a href="#" class="ui-commandlink ui-widget btn btn-default bootstrap-button-tooltip" aria-label="Email Dataset Contact" onclick="return false;" title="" data-original-title="Email Dataset Contact">
                <span class="glyphicon glyphicon-envelope no-text"></span>
            </a>
            <a href="#" class="ui-commandlink ui-widget btn btn-default bootstrap-button-tooltip" aria-label="Share Dataset" onclick="return false;" title="" data-original-title="Share Dataset">
                <span class="glyphicon glyphicon-share no-text"></span>
            </a>
        </div>
	  </div>
	</div>

.. code-block:: html

    <div class="button-block" jsf:rendered="#{true}">
        <p:commandButton tabindex="1000" id="save" value="Save Changes" onclick="checkNewlyRestricted();PF('blockDatasetForm').show();" />
        <p:commandButton tabindex="1000" id="cancel" value="Cancel" action="#{bean}" process="@this" update="@form" rendered="#{true}" oncomplete="javascript:post_cancel_edit_files_or_metadata()">
            <f:setPropertyActionListener target="#{bean}" value="#{DatasetPage.editMode == 'METADATA' ? 1 : DatasetPage.selectedTabIndex}"/>
        </p:commandButton>
        <p:button id="cancelCreate" value="Cancel" outcome="/dataverse.xhtml?alias=#{DatasetPage.dataset.owner.alias}" rendered="#{true}" />
    </div>


Pagination
==========

* Search Results
* Manage pg

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <div class="results-sort-pagination results-bottom text-center">
        <ul class="pagination">
            <li class="disabled"><a href="#" onclick="return false;">«</a>
            </li>
            <li class="disabled"><a href="#" onclick="return false;">&lt; Previous</a>
            </li>
                <li class="active"><a href="#" onclick="return false;">1
                	<span class="sr-only">(Current)</span></a>
                </li>
                <li><a href="#" onclick="return false;">2</a>
                </li>
                <li><a href="#" onclick="return false;">3</a>
                </li>
                <li><a href="#" onclick="return false;">4</a>
                </li>
                <li><a href="#" onclick="return false;">5</a>
                </li>
            <li><a href="#" onclick="return false;">Next &gt;</a>
            </li>
            <li><a href="#" onclick="return false;">»</a>
            </li>
        </ul>
      </div>

    </div>
  </div>

.. code-block:: html

  <span class="name">...</span>
  <span class="name">...</span>


Labels
======

* Publication status
* File tags

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <span class="label label-default">Default</span>
      <span class="label label-primary">Primary</span>
      <span class="label label-success">Success</span>
      <span class="label label-info">Info</span>
      <span class="label label-warning">Warning</span>
      <span class="label label-danger">Danger</span>

    </div>
  </div>

.. code-block:: html

  <span class="label label-default">Default</span>
  <span class="label label-primary">Primary</span>
  <span class="label label-success">Success</span>
  <span class="label label-info">Info</span>
  <span class="label label-warning">Warning</span>
  <span class="label label-danger">Danger</span>


Alerts
======

* Help/information, success, warning, error message block
* Inline validation error

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="alert alert-dismissable alert-info">
            <button type="button" class="close" data-dismiss="alert" aria-hidden="true">×</button>
	        <span class="glyphicon glyphicon-info-sign"></span>&nbsp;<strong>Edit Dataset Metadata - Add more metadata about this dataset to help others easily find it.</strong>&nbsp;
	    </div>
	    <div class="alert alert-success">
            <span class="glyphicon glyphicon glyphicon-ok-sign"></span>&nbsp;<strong>Success!</strong> – The metadata for this dataset has been updated.
        </div>
        <div class="alert alert-danger">
			<span class="glyphicon glyphicon-exclamation-sign"></span>&nbsp;<strong>Error</strong> – The username, email address, or password you entered is invalid. Need assistance accessing your account? If you believe this is an error, please contact <a href="#" class="ui-commandlink ui-widget" onclick="return false;">Dataverse Support</a> for assistance.
        </div>
      </div>
    </div>
  </div>

.. code-block:: html

   <div class="alert alert-success" role="alert">...</div>
   <div class="alert alert-info" role="alert">...</div>
   <div class="alert alert-warning" role="alert">...</div>
   <div class="alert alert-danger" role="alert">...</div>


Images
======

* Responsive
* Dataverse Logo
* Dataset Thumbnail
* File Thumbnail + Preview

*TO-DO...* Write a description. Build some examples. Screenshots?

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      IMAGES

    </div>
  </div>

.. code-block:: html

  <span class="name">...</span>
  <span class="name">...</span>


Panels
======

* Citation
* Citation Summary Metadata
* Metrics
* "Why?" Manage Pg Default Text

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <div class="panel panel-default">
		  <div class="panel-body">
		    Basic panel example
		  </div>
	  </div>

    </div>
  </div>

.. code-block:: html

  <div class="panel panel-default">
    <div class="panel-body">
      Basic panel example
    </div>
  </div>


Tabs
====

* Dataset
* Files

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        TABS
      </div>
    </div>
  </div>

.. code-block:: html

  <span class="name">...</span>
  <span class="name">...</span>


Popovers
========

* Small vs Large
* Confirmation vs Manage/Edit/Add

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      POPOVERS

    </div>
  </div>

.. code-block:: html

  <span class="name">...</span>
  <span class="name">...</span>


Footer
======

???... Maybe this belongs in the next page, "Templates"...

* Copyright
* Date + Owner
* Privacy Policy
* Dataverse Project
* Version #

*TO-DO...* Write a description. Build some examples. Screenshot?

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      FOOTER

    </div>
  </div>

.. code-block:: html

  <span class="name">...</span>
  <span class="name">...</span>

