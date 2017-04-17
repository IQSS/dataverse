Patterns
++++++++

Patterns are what emerge when using the foundation elements together with basic objects like buttons and alerts, as well as more complex Javascript components from `Bootstrap <http://getbootstrap.com/components/>`__ like tooltips and dropdowns, and AJAX components from `PrimeFaces <https://www.primefaces.org/showcase/>`__ like datatables and commandlinks.


Navbar
======

The navbar is a component from `Bootstrap <http://getbootstrap.com/components/#navbar>`__, which spans the top of the application and contains the logo/branding, aligned to the left, plus search form and links, aligned to the right.

When logged in, the account name is a dropdown menu, linking the user to account-specific content, as well as the log out link.

*TO-DO...* Removed extra navbar links to make it look a little cleaner. Might add some back. Should UI example be a screenshot?

* Logo/Branding
* Search
* Links
* Account

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	  	
  		<nav id="navbarFixed" class="navbar navbar-default"><!-- navbar-fixed-top -->
            <div class="container" style="width:auto !important;">
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


Breadcrumbs
===========

The breadcrumbs are displayed under the header, and provide a trail of links for users to navigate the hierarchy of containing objects, from file to dataset to dataverse.

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

Tables component from `Bootstrap <http://getbootstrap.com/css/#tables>`__.

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

Forms component from `Bootstrap <http://getbootstrap.com/css/#forms>`__.

InputText component from `PrimeFaces <https://www.primefaces.org/showcase/ui/input/inputText.xhtml>`__.

Forms fulfill various different functions across the site, but we try to style them consistently. We use the `.form-horizontal` layout, which uses `.form-group` to create a grid of rows for the labels and inputs.

* Horizontal
* Labels
* Tooltips
* Inputs
* Validation

*TO-DO...* Fix tooltips. Add real XHTML not the generated HTML. Should more sections talk about the PrimeFaces components?

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

		<div class="form-horizontal">
			<div class="form-group">
                <label for="userNameEmail" class="col-sm-3 control-label">
                    <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="Between 2-60 characters, and can use &quot;a-z&quot;, &quot;0-9&quot;, &quot;_&quot; for your username.">
                        Username <span class="glyphicon glyphicon-asterisk text-danger"></span>
                    </span>
                </label>
                <div class="col-sm-4">
                	<input name="userName" type="text" value="" tabindex="1" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all ui-state-disabled form-control" role="textbox" aria-disabled="true" aria-readonly="false">
                </div>
            </div>
            <div class="form-group">
                <label for="firstName" class="col-sm-3 control-label">
                    <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="The first name or name you would like to use for this account.">
                        Given Name <span class="glyphicon glyphicon-asterisk text-danger"></span>
                    </span>
                </label>
                <div class="col-sm-4">
                	<input name="firstName" type="text" value="" tabindex="4" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all form-control" role="textbox" aria-disabled="false" aria-readonly="false">
                </div>
            </div>
            <div class="form-group">
                <label for="lastName" class="col-sm-3 control-label">
                    <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="The last name you would like to use for this account.">
                        Family Name <span class="glyphicon glyphicon-asterisk text-danger"></span>
                    </span>
                </label>
                <div class="col-sm-4">
                	<input name="lastName" type="text" value="" tabindex="5" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all form-control" role="textbox" aria-disabled="false" aria-readonly="false">
                </div>
            </div>
            <div class="form-group">
                <label for="email" class="col-sm-3 control-label">
                    <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="" title="">
                        Email <span class="glyphicon glyphicon-asterisk text-danger"></span>
                    </span>
                </label>
                <div class="col-sm-4">
                	<input name="email" type="text" value="" tabindex="6" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all form-control" role="textbox" aria-disabled="false" aria-readonly="false">
                </div>
            </div>
            <div class="form-group">
                <label for="affiliation" class="col-sm-3 control-label">
                    <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="The organization with which you are affiliated.">
                        Affiliation
                    </span>
                </label>
                <div class="col-sm-4">
                	<input name="affiliation" type="text" value="" tabindex="7" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all form-control" role="textbox" aria-disabled="false" aria-readonly="false">
                </div>
            </div>
            <div class="form-group">
                <label for="position" class="col-sm-3 control-label">
                    <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="Your role or title at the organization you are affiliated with; such as staff, faculty, student, etc.">
                        Position
                    </span>
                </label>
                <div class="col-sm-4">
                	<input name="position" type="text" value="" tabindex="8" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all form-control" role="textbox" aria-disabled="false" aria-readonly="false">
                </div>
            </div>
        </div>

    </div>
  </div>

.. code-block:: html

  <div class="form-horizontal">
	<div class="form-group">
        <label for="userNameEmail" class="col-sm-3 control-label">
            <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="Between 2-60 characters, and can use &quot;a-z&quot;, &quot;0-9&quot;, &quot;_&quot; for your username.">
                Username <span class="glyphicon glyphicon-asterisk text-danger"></span>
            </span>
        </label>
        <div class="col-sm-4">
        	<input name="userName" type="text" value="" tabindex="1" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all ui-state-disabled form-control" role="textbox" aria-disabled="true" aria-readonly="false">
        </div>
    </div>
  </div>


Buttons
=======

Buttons component from `Bootstrap <http://getbootstrap.com/css/#buttons>`__.

Button Groups component from `Bootstrap <http://getbootstrap.com/components/#btn-groups>`__.

Buttons Dropdowns component from `Bootstrap <http://getbootstrap.com/components/#btn-dropdowns>`__.

CommandButton component from `PrimeFaces <https://www.primefaces.org/showcase/ui/button/commandButton.xhtml>`__.

CommandLink component from `PrimeFaces <https://www.primefaces.org/showcase/ui/button/commandLink.xhtml>`__.

Link component from `JSF <http://docs.oracle.com/javaee/6/javaserverfaces/2.0/docs/pdldocs/facelets/h/link.html>`__.

OutputLink component from `JSF <http://docs.oracle.com/javaee/6/javaserverfaces/2.0/docs/pdldocs/facelets/h/outputLink.html>`__.


Action Buttons
--------------

For action buttons on a page, we include an icon and text label. Action buttons are generally aligned to the right side of the page.

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

Form buttons typically appear at the bottom of a form, aligned to the left. They do not have icons, just text labels.

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

There are a few places where we use icon-only buttons with no text label. For these buttons, we do utilize tooltips that display on hover, containing a text label.

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

Pagination component from `Bootstrap <http://getbootstrap.com/components/#pagination>`__.

* Search Results
* Manage pg (...no ``class="pagination"`` ...??)

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

  <ul class="pagination">
    <li class="#{SearchIncludeFragment.page == '1' ? 'disabled' : ''}">
        <h:outputLink value="#{widgetWrapper.wrapURL(page)}">
            <h:outputText value="&#171;"/>
            <f:param name="q" value="#{SearchIncludeFragment.query}"/>
            <c:forEach items="#{SearchIncludeFragment.filterQueries}" var="clickedFilterQuery" varStatus="status">
                <f:param name="fq#{status.index}" value='#{clickedFilterQuery}'/>
            </c:forEach>
            <f:param name="types" value="#{SearchIncludeFragment.selectedTypesString}"/>
            <f:param name="sort" value="#{SearchIncludeFragment.sortField}"/>
            <f:param name="order" value="#{SearchIncludeFragment.sortOrder}"/>
            <f:param name="page" value="1"/>
        </h:outputLink>
    </li>
    <li class="#{SearchIncludeFragment.page == '1' ? 'disabled' : ''}">
        <h:outputLink value="#{widgetWrapper.wrapURL(page)}">
            <h:outputText value="&lt; #{bundle.previous}"/>
            <f:param name="q" value="#{SearchIncludeFragment.query}"/>
            <c:forEach items="#{SearchIncludeFragment.filterQueries}" var="clickedFilterQuery" varStatus="status">
                <f:param name="fq#{status.index}" value='#{clickedFilterQuery}'/>
            </c:forEach>
            <f:param name="types" value="#{SearchIncludeFragment.selectedTypesString}"/>
            <f:param name="sort" value="#{SearchIncludeFragment.sortField}"/>
            <f:param name="order" value="#{SearchIncludeFragment.sortOrder}"/>
            <f:param name="page" value="#{Math:max(1,SearchIncludeFragment.page-1).intValue()}"/>
        </h:outputLink>
    </li>
    <c:forEach begin="#{Math:max(1,SearchIncludeFragment.page-Math:max(2,SearchIncludeFragment.page-SearchIncludeFragment.totalPages+4))}"
               end="#{Math:min(SearchIncludeFragment.totalPages,SearchIncludeFragment.page+Math:max(2,5-SearchIncludeFragment.page))}"
               varStatus="pageStatus">
        <li class="#{SearchIncludeFragment.page == pageStatus.index ? 'active' : ''}">
            <h:outputLink value="#{widgetWrapper.wrapURL(page)}">
                <h:outputText value="#{pageStatus.index}">
                    <f:convertNumber pattern="###,###" />
                </h:outputText>
                <span class="#{SearchIncludeFragment.page == pageStatus.index ? 'sr-only' : ''}">
                    <h:outputText value="#{SearchIncludeFragment.page == pageStatus.index ? bundle['dataverse.results.paginator.current'] : ''}"/>
                </span>
                <f:param name="q" value="#{SearchIncludeFragment.query}"/>
                <c:forEach items="#{SearchIncludeFragment.filterQueries}" var="clickedFilterQuery" varStatus="status">
                    <f:param name="fq#{status.index}" value='#{clickedFilterQuery}'/>
                </c:forEach>
                <f:param name="types" value="#{SearchIncludeFragment.selectedTypesString}"/>
                <f:param name="sort" value="#{SearchIncludeFragment.sortField}"/>
                <f:param name="order" value="#{SearchIncludeFragment.sortOrder}"/>
                <f:param name="page" value="#{pageStatus.index}"/>
            </h:outputLink>
        </li>
    </c:forEach>
    <li class="#{SearchIncludeFragment.page == SearchIncludeFragment.totalPages ? 'disabled' : ''}">
        <h:outputLink value="#{widgetWrapper.wrapURL(page)}">
            <h:outputText value="#{bundle.next} &gt;"/>
            <f:param name="q" value="#{SearchIncludeFragment.query}"/>
            <c:forEach items="#{SearchIncludeFragment.filterQueries}" var="clickedFilterQuery" varStatus="status">
                <f:param name="fq#{status.index}" value='#{clickedFilterQuery}'/>
            </c:forEach>
            <f:param name="types" value="#{SearchIncludeFragment.selectedTypesString}"/>
            <f:param name="sort" value="#{SearchIncludeFragment.sortField}"/>
            <f:param name="order" value="#{SearchIncludeFragment.sortOrder}"/>
            <f:param name="page" value="#{Math:min(SearchIncludeFragment.page+1,SearchIncludeFragment.totalPages).intValue()}"/>
        </h:outputLink>
    </li>
    <li class="#{SearchIncludeFragment.page == SearchIncludeFragment.totalPages ? 'disabled' : ''}">
        <h:outputLink value="#{widgetWrapper.wrapURL(page)}">
            <h:outputText value="&#187;"/>
            <f:param name="q" value="#{SearchIncludeFragment.query}"/>
            <c:forEach items="#{SearchIncludeFragment.filterQueries}" var="clickedFilterQuery" varStatus="status">
                <f:param name="fq#{status.index}" value='#{clickedFilterQuery}'/>
            </c:forEach>
            <f:param name="sort" value="#{SearchIncludeFragment.sortField}"/>
            <f:param name="order" value="#{SearchIncludeFragment.sortOrder}"/>
            <f:param name="page" value="#{SearchIncludeFragment.totalPages}"/>
            <f:param name="types" value="#{SearchIncludeFragment.selectedTypesString}"/>
        </h:outputLink>
    </li>
  </ul>


Labels
======

Labels component from `Bootstrap <http://getbootstrap.com/components/#labels>`__.

* Publication status (DRAFT, In Review, Unpublished, Deaccessioned)
* Dataset version (Version 2.0, ``4.6.2``)
* Tabular Data Tags (Survey, Time Series, Panel, Event, Genomics, Network, Geospatial)

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <span class="label label-default">Version 2.0</span>
      <span class="label label-primary">DRAFT</span>
      <span class="label label-success">In Review</span>
      <span class="label label-info">Geospatial</span>
      <span class="label label-warning">Unpublished</span>
      <span class="label label-danger">Deaccessioned</span>

    </div>
  </div>

.. code-block:: html

  <span class="label label-default">Version 2.0</span>
  <span class="label label-primary">DRAFT</span>
  <span class="label label-success">In Review</span>
  <span class="label label-info">Geospatial</span>
  <span class="label label-warning">Unpublished</span>
  <span class="label label-danger">Deaccessioned</span>


Alerts
======

Alerts component from `Bootstrap <http://getbootstrap.com/components/#alerts>`__.

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

GraphicImage  component from `PrimeFaces <https://www.primefaces.org/showcase/ui/multimedia/graphicImage.xhtml>`__.

GraphicImage component from `JSF <http://docs.oracle.com/javaee/6/javaserverfaces/2.1/docs/vdldocs/facelets/h/graphicImage.html>`__.

Images CSS from `Bootstrap <http://getbootstrap.com/css/#images>`__.

* Responsive ``class="img-responsive"``
* Dataverse Logo
* Dataset Thumbnail
* File Thumbnail + Preview

*TO-DO...* Write a description. Build some examples. Screenshots?

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <img alt="image-responsive" class="img-responsive" src="../_images/dataverse-page.png">

    </div>
  </div>

.. code-block:: html

  <p:graphicImage styleClass="img-responsive" value="/api/access/datafile/#{FilePage.fileId}?imageThumb=400" rendered="#{true}"/>


Panels
======

Panels component from `Bootstrap <http://getbootstrap.com/components/#panels>`__.

Collapse javascript from `Bootstrap <http://getbootstrap.com/javascript/#collapse>`__.

These components are basically containers with rounded borders. Good for descriptions or blocks of metadata or forms or whatever you need to have a border and some padding around.

* Advanced Search collapse
* Citation
* Citation Summary Metadata
* Metadata blocks
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
    <div data-toggle="collapse" data-target="#panelCollapseDataversesFieldList" class="panel-heading">
        <h:outputText value="#{bundle['advanced.search.header.dataverses']}"/> &#160;<span class="glyphicon glyphicon-chevron-up"/>
    </div>
    <div id="panelCollapseDataversesFieldList" class="panel-body form-horizontal collapse in">
        <div class="form-group">
            <label class="col-sm-4 control-label">
                <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="#{bundle['advanced.search.dataverses.name.tip']}">
                    #{bundle.name}
                </span>
            </label>
            <div class="col-sm-6">
                <p:inputText id="dvFieldName" styleClass="form-control" value="#{AdvancedSearchPage.dvFieldName}"/>
            </div>
        </div>
        <div class="form-group">
            <label class="col-sm-4 control-label">
                <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="#{bundle['advanced.search.dataverses.affiliation.tip']}">
                    #{bundle.affiliation}
                </span>
            </label>
            <div class="col-sm-6">
                <p:inputText id="dvFieldAffiliation" styleClass="form-control" value="#{AdvancedSearchPage.dvFieldAffiliation}"/>
            </div>
        </div>
        <div class="form-group">
            <label class="col-sm-4 control-label">
                <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="#{bundle['advanced.search.dataverses.description.tip']}">
                    #{bundle.description}
                </span>
            </label>
            <div class="col-sm-6">
                <p:inputText id="dvFieldDescription" styleClass="form-control" value="#{AdvancedSearchPage.dvFieldDescription}"/>
            </div>
        </div>
        <div class="form-group">
            <label class="col-sm-4 control-label">
                <span data-toggle="tooltip" data-placement="auto right" class="tooltiplabel text-info" data-original-title="#{bundle['advanced.search.dataverses.subject.tip']}">
                    #{bundle.subject}
                </span>
            </label>
            <div class="col-sm-6">
                <div class="ui-inputfield form-control select-scroll-block">
                    <p:selectManyCheckbox value="#{AdvancedSearchPage.dvFieldSubject}" layout="pageDirection">
                        <f:selectItems value="#{AdvancedSearchPage.dvFieldSubjectValues}" var="cvv"
                                       itemLabel="#{cvv.strValue}" itemValue="#{cvv.strValue}"/>
                    </p:selectManyCheckbox>
                </div>
            </div>
        </div>
    </div>
  </div>


Tabs
====

Tab component from `Bootstrap <http://getbootstrap.com/javascript/#tabs>`__.

TabView component from `PrimeFaces <https://www.primefaces.org/showcase/ui/panel/tabView.xhtml>`__.

These are a mix of the Bootstrap CSS and PrimeFaces. More PrimeFaces though... That is why the example below doesn't look like tabs...

* Dataset
* Files
* Account

*TO-DO...* Write a description. This needs PrimeFaces CSS to look like tabs.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">

      	<div id="datasetForm:tabView" class="ui-tabs ui-widget ui-widget-content ui-corner-all ui-hidden-container ui-tabs-top" data-widget="content">
        
	      	<ul class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all" role="tablist">
		      	<li class="ui-state-default ui-tabs-selected ui-state-active ui-corner-top" role="tab" aria-expanded="true" aria-selected="true" tabindex="0">
		      		<a href="#" onclick="return false;" tabindex="-1">Files</a>
	      		</li>
		      	<li class="ui-state-default ui-corner-top" role="tab" aria-expanded="false" aria-selected="false" tabindex="-1">
		      		<a href="#" onclick="return false;" tabindex="-1">Metadata</a>
	      		</li>
		      	<li class="ui-state-default ui-corner-top" role="tab" aria-expanded="false" aria-selected="false" tabindex="-1">
		      		<a href="#" onclick="return false;" tabindex="-1">Terms</a>
	      		</li>
		      	<li class="ui-state-default ui-corner-top" role="tab" aria-expanded="false" aria-selected="false" tabindex="-1">
		      		<a href="#" onclick="return false;" tabindex="-1">Versions</a>
	      		</li>
	      	</ul>

      	</div>

      </div>
    </div>
  </div>

.. code-block:: html

  <p:tabView id="tabView" widgetVar="content" activeIndex="#{DatasetPage.selectedTabIndex}"
           rendered="#{true}">
    <p:ajax event="tabChange" listener="#{DatasetPage.tabChanged}" oncomplete="bind_bsui_components();" update="@this" />
    <p:tab id="dataFilesTab" title="#{bundle.files}" rendered="#{true}">
        <ui:include src="filesFragment.xhtml">
            <ui:param name="fileDownloadHelper" value="#{DatasetPage.fileDownloadHelper}"/>
        </ui:include>
    </p:tab>
    ...
  </p:tabView>


Modals
======

Modal component from `Bootstrap <http://getbootstrap.com/javascript/#modals>`__.

Dialog component from `PrimeFaces <https://www.primefaces.org/showcase/ui/overlay/dialog/basic.xhtml>`__.

Confirm Dialog component from `PrimeFaces <https://www.primefaces.org/showcase/ui/overlay/confirmDialog.xhtml>`__.

This is the Bootstrap component for a popup window that prompts a user for information, with overlay an a backdrop, then header, content and buttons.

There is are large ``.bs-example-modal-lg`` and small ``.bs-example-modal-sm`` options for the width.

Note: we don't use the ``.fade`` class.

* Small vs Large
* Confirmation vs Manage/Edit/Add

*TO-DO...* Write a description. Build some examples.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <button type="button" class="btn btn-primary" data-toggle="modal" data-target=".bs-example-modal-lg">Large modal</button>

      <div class="modal bs-example-modal-lg" tabindex="-1" role="dialog" aria-labelledby="myLargeModalLabel">
		<div class="modal-dialog modal-lg" role="document">
		  <div class="modal-content">
		  	<div class="modal-header">
		      <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
		      <h4 class="modal-title" id="myModalLabel">Modal title</h4>
		    </div>
		    <div class="modal-body">
		      ...
		    </div>
		  </div>
		</div>
	  </div>

    </div>
  </div>

.. code-block:: html

  <!-- Large modal -->
  <button type="button" class="btn btn-primary" data-toggle="modal" data-target=".bs-example-modal-lg">Large modal</button>

  <div class="modal fade bs-example-modal-lg" tabindex="-1" role="dialog" aria-labelledby="myLargeModalLabel">
	<div class="modal-dialog modal-lg" role="document">
	  <div class="modal-content">
	    ...
	  </div>
	</div>
  </div>

