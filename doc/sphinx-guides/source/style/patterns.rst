Patterns
++++++++

Patterns are what emerge when using the foundation elements together with basic objects like buttons and alerts, more complex Javascript components from `Bootstrap <https://getbootstrap.com/components/>`__ like tooltips and dropdowns, and AJAX components from `PrimeFaces <https://www.primefaces.org/showcase/>`__ like datatables and commandlinks.

.. contents:: |toctitle|
  :local:

Navbar
======

The `Navbar component <https://getbootstrap.com/components/#navbar>`__ from Bootstrap spans the top of the application and contains the logo/branding, aligned to the left, plus search form and links, aligned to the right.

When logged in, the account name is a dropdown menu, linking the user to account-specific content and the log out link.

.. raw:: html

    <div class="panel panel-default code-example">
        <div class="panel-body">
            <nav id="navbarFixed" class="navbar navbar-default"><!-- navbar-fixed-top -->
                <div class="container" style="width:auto !important;">
                    <div class="navbar-header">
                        <a href="#" onclick="return false;">
                            <span class="navbar-brand"><i id="icon-dataverse" class="icon-dataverse"></i> Dataverse</span>
                        </a>
                    </div>
                    <div class="collapse navbar-collapse" id="topNavBar">
                        <ul class="nav navbar-nav navbar-right">
                            <li class="dropdown">
                                <span id="dataverseSupportLink" class="dropdown-toggle" data-toggle="dropdown">
                                    User Name <b class="caret"></b>
                                </span>
                                <ul class="dropdown-menu">
                                    <li><a href="#" onclick="return false;">My Data</a>
                                    </li>
                                    <li><a href="#" onclick="return false;">Notifications</a>
                                    </li>
                                    <li><a href="#" onclick="return false;">Account Information</a>
                                    </li>
                                    <li><a href="#" onclick="return false;">API Token</a>
                                    </li>
                                    <li class="divider"></li>
                                    <li class="logout"><a href="#" onclick="return false;">Log Out</a>
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
        <div class="navbar-header">
          <a href="#" onclick="return false;">
            <span class="navbar-brand"><i id="icon-dataverse" class="icon-dataverse"></i> Dataverse</span>
          </a>
        </div>
        <div class="collapse navbar-collapse" id="topNavBar">
          <ul class="nav navbar-nav navbar-right">
            <li>
              ...
            </li>
          </ul>
        </div>
      </div>
    </nav>


Breadcrumbs
===========

The breadcrumbs are displayed under the header, and provide a trail of links for users to navigate the hierarchy of containing objects, from file to dataset to Dataverse collection. It utilizes a JSF `repeat component <https://docs.oracle.com/javaee/6/javaserverfaces/2.0/docs/pdldocs/facelets/ui/repeat.html>`_ to iterate through the breadcrumbs.

.. raw:: html

    <div class="panel panel-default code-example">
	<div class="panel-body">
            <div id="breadcrumbNavBlock" class="container" style="width:auto !important;">
                <div class="breadcrumbBlock">
                    <a id="breadcrumbLnk0" href="#" onclick="return false;">Name of a Dataverse collection</a>
                </div>
                <span class="breadcrumbCarrot"> &gt; </span>
                <div class="breadcrumbBlock">
                    <a id="breadcrumbLnk1" href="#" onclick="return false;">Name of Another Dataverse collection</a>
                </div>
                <span class="breadcrumbCarrot"> &gt; </span>
            </div>
	</div>
    </div>

.. code-block:: html

    <div id="breadcrumbNavBlock" class="container" jsf:rendered="#{true}">
      <ui:repeat value="#{page.breadcrumbs}" var="breadcrumb" varStatus="status">
        <h:outputText value=" &gt; " styleClass="breadcrumbCarrot" rendered="#{true}"/>
        <div class="breadcrumbBlock">
          ...
        </div>
      </ui:repeat>
    </div>


Tables
======

Most tables use the `DataTable components <https://www.primefaces.org/showcase/ui/data/datatable/basic.xhtml>`__ from PrimeFaces and are styled using the `Tables component <https://getbootstrap.com/css/#tables>`__ from Bootstrap.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
    	<div class="ui-datatable ui-widget">
            <div class="ui-datatable-tablewrapper">
                <table role="grid">
                    <thead>
                        <tr role="row">
                            <th style="width:60px;" class="ui-state-default ui-selection-column col-select-width text-center" role="columnheader"><span class="ui-column-title"></span><div class="ui-chkbox ui-chkbox-all ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div></th><th class="ui-state-default col-sm-2 text-center" role="columnheader"><span class="ui-column-title">Dataset</span></th><th class="ui-state-default" role="columnheader"><span class="ui-column-title">Summary</span></th><th class="ui-state-default col-sm-3" role="columnheader"><span class="ui-column-title">Published</span></th>
                        </tr>
                    </thead>
                    <tbody class="ui-datatable-data ui-widget-content">
                        <tr data-ri="0" class="ui-widget-content ui-datatable-even ui-datatable-selectable" role="row" aria-selected="false">
                            <td role="gridcell" class="ui-selection-column col-select-width text-center">
                                <div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
                            </td>
                            <td role="gridcell" class="text-center">
                                <a href="#" class="ui-commandlink ui-widget" onclick="return false;">3.0</a>
                            </td>
                            <td role="gridcell">
                                <span class="highlightBold">Files (Changed File Metadata: 1); </span><a href="#" class="ui-commandlink ui-widget" onclick="return false;">View Details</a>
                            </td>
                            <td role="gridcell"><span>March 8, 2017</span></td>
                        </tr>
                        <tr data-ri="1" class="ui-widget-content ui-datatable-odd ui-datatable-selectable" role="row" aria-selected="false">
                            <td role="gridcell" class="ui-selection-column col-select-width text-center">
                                <div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
                            </td>
                            <td role="gridcell" class="text-center">
                                <a href="#" class="ui-commandlink ui-widget" onclick="return false;">2.0</a>
                            </td>
                            <td role="gridcell">
                                <span class="highlightBold">Additional Citation Metadata: </span> (1 Added); <a href="#" class="ui-commandlink ui-widget" onclick="return false;">View Details</a>
                            </td>
                            <td role="gridcell"><span>January 25, 2017</span></td>
                        </tr>
                        <tr data-ri="2" class="ui-widget-content ui-datatable-even ui-datatable-selectable" role="row" aria-selected="false">
                            <td role="gridcell" class="ui-selection-column col-select-width text-center">
                                <div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
                            </td>
                            <td role="gridcell" class="text-center">
                                <a href="#" class="ui-commandlink ui-widget" onclick="return false;">1.1</a></td><td role="gridcell"><span class="highlightBold">Additional Citation Metadata: </span> (1 Added); <a href="#" class="ui-commandlink ui-widget" onclick="return false;">View Details</a>
                            </td>
                            <td role="gridcell"><span>October 25, 2016</span></td>
                        </tr>
                        <tr data-ri="3" class="ui-widget-content ui-datatable-odd ui-datatable-selectable" role="row" aria-selected="false">
                            <td role="gridcell" class="ui-selection-column col-select-width text-center">
                                <div class="ui-chkbox ui-widget"><div class="ui-helper-hidden-accessible"><input type="checkbox" name="table_checkbox"></div><div class="ui-chkbox-box ui-widget ui-corner-all ui-state-default"><span class="ui-chkbox-icon ui-icon ui-icon-blank ui-c"></span></div></div>
                            </td>
                            <td role="gridcell" class="text-center">
                                <a href="#" class="ui-commandlink ui-widget" onclick="return false;">1.0</a>
                            </td>
                            <td role="gridcell">
                                This is the first published version.
                            </td>
                            <td role="gridcell"><span>September 19, 2016</span></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
  </div>

.. code-block:: html

   <p:dataTable id="itemTable" styleClass="headerless-table margin-top" value="#{page.item}" var="item" widgetVar="itemTable">
     <p:column>
       ...
     </p:column>
   </p:dataTable>


Forms
=====

Forms fulfill various functions across the site, but we try to style them consistently. We use the ``.form-horizontal`` layout, which uses ``.form-group`` to create a grid of rows for the labels and inputs. The consistent style of forms is maintained using the `Forms component <https://getbootstrap.com/css/#forms>`__ from Bootstrap. Form elements like the `InputText component <https://www.primefaces.org/showcase/ui/input/inputText.xhtml>`__ from PrimeFaces are kept looking clean and consistent across each page.

.. raw:: html

    <div class="panel panel-default code-example">
        <div class="panel-body">
            <div class="form-horizontal">
                <div class="form-group">
                    <label for="userNameEmail" class="col-sm-3 control-label">
                        Username 
                    </label>
                    <div class="col-sm-4">
                        <input name="userName" type="text" value="" tabindex="1" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all ui-state-default form-control" role="textbox" aria-disabled="false" aria-readonly="false">
                    </div>
                </div>
                <div class="form-group">
                    <label for="email" class="col-sm-3 control-label">
                        Email 
                    </label>
                    <div class="col-sm-4">
                        <input name="email" type="text" value="" tabindex="6" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all form-control" role="textbox" aria-disabled="false" aria-readonly="false">
                    </div>
                </div>
            </div>
        </div>
    </div>

.. code-block:: html

    <div class="form-horizontal">
      <div class="form-group">
        <label for="userNameEmail" class="col-sm-3 control-label">
          #{bundle['user.username']} 
        </label>
        <div class="col-sm-4">
          <p:inputText id="userName" styleClass="form-control"></p>
        </div>
      </div>
    </div>

Here are additional form elements that are common across many pages, including required asterisks, icon tooltips, placeholder text, input info message with popover link, and validation error message.

.. raw:: html

    <div class="panel panel-default code-example">
        <div class="panel-body">
            <div class="form-group form-col-container col-sm-9 edit-compound-field">
                <div class="form-col-container col-sm-12">
                    <p class="help-block">
                        This field supports only certain <span class="text-info popoverHTML">HTML tags</span>.
                    </p>
                    <label class="control-label" for="metadata_dsDescriptionValue">
                        Text <span class="glyphicon glyphicon-asterisk text-danger"></span>
                        <span class="glyphicon glyphicon-question-sign tooltip-icon" data-toggle="tooltip" data-placement="auto right" data-original-title="A summary describing the purpose, nature, and scope of the Dataset."></span>
                    </label>
                    <div>
                        <textarea id="datasetForm:description" name="datasetForm:description" cols="60" rows="5" tabindex="1" maxlength="2147483647" class="ui-inputfield ui-inputtextarea ui-widget ui-state-default ui-corner-all form-control ui-inputtextarea-resizable" role="textbox" aria-disabled="false" aria-readonly="false" aria-multiline="true" data-autosize-on="true" placeholder="" style="overflow: hidden; word-wrap: break-word; height: 114px;"></textarea>
                        <div aria-live="polite" class="ui-message ui-message-error ui-widget ui-corner-all">
                            <span class="ui-message-error-detail">Description Text is required.</span>
                        </div>
                    </div>
                </div>
            </div>
            <div class="form-col-container col-sm-6">
                <label class="control-label" for="metadata_dsDescriptionDate">
                   Date
                   <span class="glyphicon glyphicon-question-sign tooltip-icon" data-toggle="tooltip" data-placement="auto right" data-original-title="In cases where a Dataset contains more than one description (for example, one might be supplied by the data producer and another prepared by the data repository where the data are deposited), the date attribute is used to distinguish between the two descriptions. The date attribute follows the ISO convention of YYYY-MM-DD."></span>
                </label>
                <div>
                    <input id="datasetForm:inputText" name="datasetForm:inputText" type="text" tabindex="1" class="ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all form-control " role="textbox" aria-disabled="false" aria-readonly="false" placeholder="YYYY-MM-DD">
                </div>
            </div>
        </div>
    </div>

.. code-block:: html

  <div class="form-group form-col-container col-sm-9 edit-compound-field">
    <div class="form-col-container col-sm-12">
      <p class="help-block">
        <h:outputFormat value="#{bundle.htmlAllowedMsg}" escape="false">
          <f:param value="#{bundle.htmlAllowedTags}"/>
        </h:outputFormat>
      </p>
      <label class="control-label" for="metadata_#{subdsf.datasetFieldType.name}">
        #{subdsf.datasetFieldType.localeTitle}
        <h:outputText styleClass="glyphicon glyphicon-asterisk text-danger" value="" />
        <span class="glyphicon glyphicon-question-sign tooltip-icon" data-toggle="tooltip" data-placement="auto right" data-original-title="#{subdsf.datasetFieldType.localeDescription}"></span>
      </label>
      <div>
        <p:inputTextarea value="#{dsfv.valueForEdit}" id="description" tabindex="#{block.index+1}" rows="5" cols="60" styleClass="form-control" />
        <div class="alert-danger" jsf:rendered="#{!empty subdsf.validationMessage}">
            <strong>#{subdsf.validationMessage}</strong>
        </div>
      </div>
    </div>
  </div>


Buttons
=======

There are various types of buttons for various actions, so we have many components to use, including the `CommandButton component <https://www.primefaces.org/showcase/ui/button/commandButton.xhtml>`__ and `CommandLink component <https://www.primefaces.org/showcase/ui/button/commandLink.xhtml>`__ from PrimeFaces, as well as the basic JSF `Link component <https://docs.oracle.com/javaee/6/javaserverfaces/2.0/docs/pdldocs/facelets/h/link.html>`__ and `OutputLink component <https://docs.oracle.com/javaee/6/javaserverfaces/2.0/docs/pdldocs/facelets/h/outputLink.html>`__. Those are styled using the `Buttons component <https://getbootstrap.com/css/#buttons>`__, `Button Groups component <https://getbootstrap.com/components/#btn-groups>`__ and `Buttons Dropdowns component <https://getbootstrap.com/components/#btn-dropdowns>`__ from Bootstrap.

Action Buttons
--------------

For action buttons on a page, we include an icon and text label.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	    <div class="btn-group">
                <button type="button" id="editDataSet" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="true">
                    <span class="glyphicon glyphicon-pencil"></span> Edit <span class="caret"></span>
                </button>
                <ul class="dropdown-menu text-left">
                    <li>
                        <a href="#" onclick="return false;">Files (Upload)</a>
                    </li>
                    <li>
                        <a href="#" class="ui-commandlink ui-widget" onclick="return false;">Metadata</a>
                    </li>
                    <li>
                        <a href="#" class="ui-commandlink ui-widget" onclick="return false;">Terms</a>
                    </li>
                    <li class="dropdown-submenu pull-left">
                        <a tabindex="-1" href="#">Permissions</a>
                        <ul class="dropdown-menu">
                            <li>
                                <a href="#" onclick="return false;" class="ui-commandlink ui-widget">Dataset</a>
                            </li>
                            <li>
                                <a href="#" onclick="return false;" class="ui-commandlink ui-widget">File</a>
                            </li>
                        </ul>
                    </li>
                    <li>
                        <a href="#" class="ui-commandlink ui-widget" onclick="return false;">Private URL</a>
                    </li>
                    <li>
                        <a href="#" onclick="return false;">Thumbnails + Widgets</a>
                    </li>
                    <li class="divider"></li>
                    <li>
                        <a href="#" class="ui-commandlink ui-widget" onclick="return false;">Deaccession Dataset</a>
                    </li>
                </ul>
            </div>
	  </div>
	</div>

.. code-block:: html

    <div class="btn-group" jsf:rendered="#{true}">
      <button type="button" id="editDataSet" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
        <span class="glyphicon glyphicon-pencil"/> Edit <span class="caret"></span>
      </button>
      <ul class="dropdown-menu text-left">
        <li>
          <h:outputLink> ... </h:outputLink>
        </li>
        <li class="dropdown-submenu pull-left">
          <a tabindex="-1" href="#">Option</a>
          <ul class="dropdown-menu">
            <li>
              <h:link> ... </h:link>
            </li>
            <li>
              <h:link> ... </h:link>
            </li>
          </ul>
        </li>
        ...
      </ul>
    </div>

Action Buttons Block
--------------------

For the main actions on a page, we use a container block to group them together. They use the Bootstrap justified button groups style class ``.btn-group.btn-group-justified`` in order to create a group of buttons that stretch at equal sizes to span the entire width of its parent.

The Bootstrap theme provides a ``.btn-primary`` style class to highlight the primary action for the user. This stronger color provides extra visual weight and identifies the primary action in a set of buttons on the page. In this example button group from the file page, you can see the Download and Explore options are listed together, providing a more scalable solution to configurable options.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
            <div class="col-xs-4">
                <div id="actionButtonBlock">
                    <div class="btn-group btn-group-justified">
                        <div class="btn-group">
                            <button type="button" id="accessFile" class="btn btn-primary btn-access-file dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                Access File <span class="caret"></span>
                            </button>
                            <ul class="dropdown-menu pull-right text-left">
                                <li class="dropdown-header">
                                    Download Options <span class="glyphicon glyphicon-download-alt"></span>
                                </li>
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget">
                                        Original File Format (R Data)
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget">
                                        Tab-Delimited
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget">
                                        RData Format
                                    </a>
                                </li>
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget">
                                        Variable Metadata
                                    </a>
                                </li>
                                <li class="dropdown-submenu pull-left">
                                    <a tabindex="-1" href="javascript:void(0);">Data File Citation</a>
                                    <ul class="dropdown-menu">
                                        <li><a href="#" onclick="return false;">RIS</a>
                                        </li>
                                        <li><a href="#" onclick="return false;">EndNote XML</a>
                                        </li>
                                        <li><a href="#" onclick="return false;">BibTeX</a>
                                        </li>
                                    </ul>
                                </li>
                                <li role="presentation" class="clearfix"></li>
                                <li class="dropdown-header">
                                    Explore Options <span class="glyphicon glyphicon-equalizer"></span>
                                </li>
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget btn-explore">
                                        Data Explorer
                                    </a>
                                </li>                    
                            </ul>
                        </div>
                    </div>
                    <div class="btn-group btn-group-justified">
                        <div class="btn-group">
                            <button type="button" id="editFile" class="btn btn-default btn-access btn-edit dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                Edit File <span class="caret"></span>
                            </button>
                            <ul class="dropdown-menu pull-right text-left">
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget" aria-label="Metadata" title="Metadata">Metadata</a>
                                </li>
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget" aria-label="Restrict" title="Restrict">Restrict</a>
                                </li>                                        
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget" aria-label="Replace" title="Replace">Replace</a>
                                </li> 
                                <li>
                                    <a href="#" onclick="return false;" class="ui-commandlink ui-widget" aria-label="Delete" title="Delete">Delete</a>
                                </li>
                            </ul>
                        </div>
                    </div>
                    <div class="btn-group btn-group-justified">
                        <a href="#" onclick="return false;" class="btn btn-default btn-xs btn-contact" aria-label="Contact Dataset Owner" title="Contact Dataset Owner">
                            Contact Owner
                        </a>
                        <a href="#" onclick="return false;" class="btn btn-default btn-xs btn-share" aria-label="Share Dataset" title="Share Dataset">
                            Share
                        </a>
                    </div>
                </div>
            </div>
	  </div>
	</div>

.. code-block:: html

        <div class="col-xs-4">
          <div id="actionButtonBlock">
            <div class="btn-group btn-group-justified">
              <div class="btn-group">
                <button type="button" id="accessFile" class="btn btn-primary btn-access-file dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Access File
                </button>
                <ul class="dropdown-menu pull-right text-left">
                  <li class="dropdown-header">
                    Download Options <span class="glyphicon glyphicon-download-alt"></span>
                  </li>
                  ...
                </ul>
              </div>
            </div>
            <div class="btn-group btn-group-justified">
              <div class="btn-group">
                <button type="button" id="editFile" class="btn btn-default btn-access btn-edit dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Edit File <span class="caret"></span>
                </button>
                <ul class="dropdown-menu pull-right text-left">
                  <li>
                    ...
                  </li>
                </ul>
              </div>
            </div>
            <div class="btn-group btn-group-justified">
              <a href="#" onclick="return false;" class="btn btn-default btn-xs btn-contact" aria-label="Contact Dataset Owner" title="Contact Dataset Owner">
                Contact Owner
              </a>
              <a href="#" onclick="return false;" class="btn btn-default btn-xs btn-share" aria-label="Share Dataset" title="Share Dataset">
                Share
              </a>
            </div>
          </div>
        </div>

Form Buttons
------------

Form buttons typically appear at the bottom of a form, aligned to the left. They do not have icons, just text labels. The primary button is styled differently.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
            <div class="button-block">
                <button id="datasetForm:save" name="datasetForm:save" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only btn btn-default" onclick="return false;" tabindex="1000" type="submit" role="button" aria-disabled="false">
                    <span class="ui-button-text ui-c">Save Changes</span>
                </button>
                <button id="datasetForm:cancel" name="datasetForm:cancel" class="btn btn-link" onclick="return false;" tabindex="1000" type="submit" role="button" aria-disabled="false">
                    <span class="ui-button-text ui-c">Cancel</span>
                </button>
            </div>
	  </div>
	</div>

.. code-block:: html

    <div class="button-block">
      <p:commandButton id="save" styleClass="btn btn-default" value="#{bundle.saveChanges}" action="#{page.save}" update="@form,:messagePanel" />
      <p:commandButton id="cancel" styleClass="btn btn-link" value="#{bundle.cancel}" action="#{page.cancel}" process="@this" update="@form">
        <p:resetInput target="@form" />
      </p:commandButton>
    </div>

Icon-Only Buttons
-----------------

There are a few places where we use icon-only buttons with no text label. For these buttons, we do utilize tooltips that display on hover, containing a text label.

We use the style class ``.no-text`` with the ``.glyphicon`` class to fix spacing issues from margins and padding applied to buttons with text labels.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
	    <a href="#" class="ui-commandlink ui-widget btn btn-default btn-sm bootstrap-button-tooltip compound-field-btn" aria-label="Add" onclick="return false;" tabindex="1" title="" data-original-title="Add">
                <span class="glyphicon glyphicon-plus no-text"></span>
            </a>
            <a href="#" class="ui-commandlink ui-widget btn btn-default btn-sm bootstrap-button-tooltip compound-field-btn" aria-label="Delete" onclick="return false;" tabindex="1" title="" data-original-title="Delete">
                <span class="glyphicon glyphicon-minus no-text"></span>
            </a>
	  </div>
	</div>

.. code-block:: html

    <p:commandLink styleClass="btn btn-default btn-sm bootstrap-button-tooltip" title="#{bundle.add}" actionListener="#{Page.add(valCount.index + 1)}">
      <h:outputText styleClass="glyphicon glyphicon-plus no-text"/>
    </p:commandLink>
    <p:commandLink styleClass="btn btn-default btn-sm bootstrap-button-tooltip" title="#{bundle.delete}" actionListener="#{Page.remove(valCount.index)}">
      <h:outputText styleClass="glyphicon glyphicon-minus no-text"/>
    </p:commandLink>

Another variation of icon-only buttons uses the ``.btn-link`` style class from Bootstrap, styling it more like a link while maintaining button behavior. The button group provides space for up to three buttons for a file in the table, and if there are more than three action button, they utilize the "kebab" More Options button dropdown with the ``.glyphicon-option-vertical`` icon.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
            <div class="btn-group" role="group" aria-label="File Actions">
                <a href="#" onclick="return false;" class="ui-commandlink ui-widget btn-preview btn btn-link bootstrap-button-tooltip" aria-label="Preview" data-original-title="Preview">
                    <span class="glyphicon glyphicon-eye-open"></span><span class="sr-only">Preview</span>
                </a>
                <a href="#" onclick="return false;" class="ui-commandlink ui-widget btn-download btn btn-link bootstrap-button-tooltip" aria-label="Download" data-original-title="Download">
                    <span class="glyphicon glyphicon-download-alt"></span><span class="sr-only">Download</span>
                </a>
                <div class="btn-group">
                    <a class="btn-explore btn btn-link bootstrap-button-tooltip dropdown-toggle" id="exploreBtn" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" data-original-title="Explore">
                        <span class="glyphicon glyphicon-equalizer"></span><span class="sr-only">Explore</span><span class="caret"></span>
                    </a>
                    <ul class="dropdown-menu multi-level pull-right text-left" aria-labelledby="exploreBtn">
                        <li>
                            <a href="#" rel="noopener" class="ui-commandlink ui-widget" onclick="return false;">
                                Data Explorer
                            </a>
                        </li>
                    </ul>
                </div>
                <div class="btn-group">
                    <a class="btn-options btn btn-link bootstrap-button-tooltip dropdown-toggle" id="optionsBtn" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" data-original-title="More Options">
                        <span class="glyphicon glyphicon-option-vertical"></span><span class="sr-only">More Options</span><span class="caret"></span>
                    </a>
                    <ul class="dropdown-menu multi-level pull-right text-left" aria-labelledby="optionsBtn">
                        <li>
                            <a href="#" class="ui-commandlink ui-widget" onclick="return false;">
                                Misc. Option
                            </a>
                        </li>
                        <li>
                            <a href="#" class="ui-commandlink ui-widget" onclick="return false;">
                                Misc. Option
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
	  </div>
	</div>

.. code-block:: html

    <div class="btn-group" role="group" aria-label="#{bundle['file.actionsBlock']}">

        <ui:fragment rendered="#{true}">
            <a class="btn-preview btn btn-link bootstrap-button-tooltip" title="#{bundle.preview}"
                href="#{widgetWrapper.wrapURL('/file.xhtml?'.concat(...)}">
                <span class="glyphicon glyphicon-eye-open"/><span class="sr-only">#{bundle.preview}</span>
            </a>
        </ui:fragment>

        <p:commandLink rendered="#{true}" styleClass="btn-download btn btn-link bootstrap-button-tooltip" 
                       title="#{bundle.download}"
                       disabled="#{locked ? 'disabled' : ''}" 
                       process="@this" update="@widgetVar(popup)" oncomplete="PF('popup').show();">
          <f:actionListener binding="#{pageBean.function()}" />
          <f:actionListener binding="#{pageBean.function()}" />
          <span class="glyphicon glyphicon-download-alt"/><span class="sr-only">#{bundle.download}</span>
        </p:commandLink>

        <div class="btn-group" jsf:rendered="#{true}">
          <a class="btn-explore btn btn-link bootstrap-button-tooltip" 
             title="#{bundle.explore}" id="exploreBtn" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" data-original-title="#{bundle.explore}">
            <span class="glyphicon glyphicon-equalizer"/><span class="sr-only">#{bundle.explore}</span><span class="caret"></span>
          </a>
          <ul class="dropdown-menu multi-level pull-right text-left" aria-labelledby="exploreBtn">
            <ui:repeat var="tool" value="#{exploreTools}">
              <li>
                <p:commandLink styleClass="#{locked ? 'disabled' : ''}"
                               disabled="#{locked ? 'disabled' : ''}"
                               action="#{pageBean.function()}">
                  #{tool.getDisplayNameLang()}
                </p:commandLink>
              </li>
            </ui:repeat>
          </ul>
        </div>

        <div class="btn-group" jsf:rendered="#{true}">
          <a class="btn-options btn btn-link bootstrap-button-tooltip" 
             id="optionsBtn" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" title="#{bundle.moreOptions}" data-original-title="#{bundle.moreOptions}">
            <span class="glyphicon glyphicon-option-vertical"/><span class="sr-only">#{bundle.moreOptions}</span><span class="caret"></span>
          </a>
          <ul class="dropdown-menu multi-level pull-right text-left" aria-labelledby="optionsBtn">
            <ui:repeat var="options" value="#{fileOptions}">
              <li>
                <p:commandLink styleClass="#{locked ? 'disabled' : ''}"
                               disabled="#{locked ? 'disabled' : ''}"
                               action="#{pageBean.function()}">
                  ...
                </p:commandLink>
              </li>
            </ui:repeat>
          </ul>
        </div>
    </div>

Pagination
==========

We use the `Pagination component <https://getbootstrap.com/components/#pagination>`__ from Bootstrap for paging through search results.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body text-center">
      
        <ul class="pagination">
            <li class="disabled">
                <a href="#" onclick="return false;">«</a>
            </li>
            <li class="disabled">
                <a href="#" onclick="return false;">&lt; Previous</a>
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
            <li>
                <a href="#" onclick="return false;">Next &gt;</a>
            </li>
            <li>
                <a href="#" onclick="return false;">»</a>
            </li>
        </ul>

    </div>
  </div>

.. code-block:: html

  <ul class="pagination">
    <li class="#{include.page == '1' ? 'disabled' : ''}">
      <h:outputLink value="#{page.page}">
        <h:outputText value="&#171;"/>
        ...
      </h:outputLink>
    </li>
    <li class="#{include.page == '1' ? 'disabled' : ''}">
      <h:outputLink value="#{page.page}">
        <h:outputText value="&lt; #{bundle.previous}"/>
        ...
      </h:outputLink>
    </li>
    ...
    <li class="#{include.page == include.totalPages ? 'disabled' : ''}">
      <h:outputLink value="#{page.page}">
        <h:outputText value="#{bundle.next} &gt;"/>
        ...
      </h:outputLink>
    </li>
    <li class="#{include.page == include.totalPages ? 'disabled' : ''}">
      <h:outputLink value="#{page.page}">
        <h:outputText value="&#187;"/>
        ...
      </h:outputLink>
    </li>
  </ul>


Labels
======

The `Labels component <https://getbootstrap.com/components/#labels>`__ from Bootstrap is used for publication status (DRAFT, In Review, Unpublished, Deaccessioned), and Dataset version, as well as Tabular Data Tags (Survey, Time Series, Panel, Event, Genomics, Network, Geospatial).

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

For our help/information, success, warning, and error message blocks we use a custom built UI component based on the `Alerts component <https://getbootstrap.com/components/#alerts>`__ from Bootstrap.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="messagePanel">
        <div class="alert alert-dismissable alert-info">
          <button type="button" class="close" data-dismiss="alert" aria-hidden="true">×</button>
          <span class="glyphicon glyphicon-info-sign"></span>&nbsp;<strong>Edit Dataset Metadata</strong> - Add more metadata about this dataset to help others easily find it.
        </div>
        <div class="alert alert-success">
          <span class="glyphicon glyphicon glyphicon-ok-sign"></span>&nbsp;<strong>Success!</strong> – The metadata for this dataset has been updated.
        </div>
        <div class="alert alert-warning">
          <span class="glyphicon glyphicon glyphicon-warning-sign"></span>&nbsp;<strong>File Upload in Progress</strong> – This dataset is locked while the data files are being transferred and verified.
        </div>
        <div class="alert alert-danger">
          <span class="glyphicon glyphicon-exclamation-sign"></span>&nbsp;<strong>Error</strong> – The username, email address, or password you entered is invalid. Need assistance accessing your account? If you believe this is an error, please contact <a href="#" class="ui-commandlink ui-widget" onclick="return false;">Root Support</a> for assistance.
        </div>
      </div>
    </div>
  </div>

.. code-block:: html

   <div class="alert alert-success" role="alert">...</div>
   <div class="alert alert-info" role="alert">...</div>
   <div class="alert alert-warning" role="alert">...</div>
   <div class="alert alert-danger" role="alert">...</div>


Message Classes
---------------

Style classes can be added to ``p``, ``div``, ``span`` and other elements to add emphasis to inline message blocks.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <p class="help-block">
        <span class="text-muted">Select Dataverse collections to feature on the homepage of this Dataverse collection.</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-ok-sign text-success"></span> <span class="text-success">Search query returned 1,000 datasets!</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-asterisk text-info"></span> <span class="text-info">Permissions with an asterisk icon indicate actions that can be performed by users not logged into the Dataverse installation.</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-warning-sign text-warning"></span> <span class="text-warning">Are you sure you want to remove all roles for user dataverseUser?</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-exclamation-sign text-danger"></span> <span class="text-danger">Please select two versions to view the differences.</span>
      </p>

    </div>
  </div>

.. code-block:: html
    
      <p class="help-block">
        <span class="text-muted">...</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-ok-sign text-success"></span> <span class="text-success">...</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-asterisk text-info"></span> <span class="text-info">...</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-warning-sign text-warning"></span> <span class="text-warning">...</span>
      </p>

      <p class="help-block">
        <span class="glyphicon glyphicon-exclamation-sign text-danger"></span> <span class="text-danger">...</span>
      </p>


Images
======

For images, we use the `GraphicImage  component <https://www.primefaces.org/showcase/ui/multimedia/graphicImage.xhtml>`__ from PrimeFaces, or the basic JSF `GraphicImage component <https://docs.oracle.com/javaee/6/javaserverfaces/2.1/docs/vdldocs/facelets/h/graphicImage.html>`__.

To display images in a responsive way, they are styled with ``.img-responsive``, an `Images CSS class <https://getbootstrap.com/css/#images>`__ from Bootstrap.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <img alt="image-responsive" class="img-responsive" src="../_images/dataverse-project.png">
    </div>
  </div>

.. code-block:: html

  <p:graphicImage styleClass="img-responsive" value="#{Page.imageId}?imageThumb=400" />


Panels
======

The most common of our containers, the `Panels component <https://getbootstrap.com/components/#panels>`__ from Bootstrap is used to add a border and padding around sections of content like metadata blocks. Displayed with a header and/or footer, it can also be used with the  `Collapse plugin <https://getbootstrap.com/javascript/#collapse>`__ from Bootstrap.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

        <div class="panel panel-default">
            <div class="panel-body">
                Basic panel example
            </div>
        </div>

        <div class="panel-group">
            <div class="panel panel-default">
                <div data-toggle="collapse" data-target="#panelCollapse0" class="panel-heading">
                    <span class="text-info">Panel Heading &nbsp;<span class="glyphicon glyphicon-chevron-up"></span></span>
                </div>
                <div id="panelCollapse0" class="collapse in">
                    <div class="panel-body metadata-panel-body">
                        <div class="form-group col-sm-12">
                            <label for="metadata_dsDescription" class="col-sm-3 control-label">
                                Label
                            </label>
                            <div class="col-sm-9">Value</div>
                        </div>
                    </div>
                </div>
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

  <div class="panel panel-default">
    <div data-toggle="collapse" data-target="#panelCollapse0" class="panel-heading">
      <span class="text-info">Panel Heading &#160;<span class="glyphicon glyphicon-chevron-up"/></span>
    </div>
    <div id="panelCollapse0" class="panel-body form-horizontal collapse in">
      <div class="form-group">
        <label class="col-sm-4 control-label">
          Label
        </label>
        <div class="col-sm-6">
          Value
        </div>
      </div>
    </div>
  </div>


Tabs
====

Tabs are used to provide content panes on a page that allow the user to view different sections of content without navigating to a different page.

We use the `TabView component <https://www.primefaces.org/showcase/ui/panel/tabView.xhtml>`__ from PrimeFaces, which is styled using the `Tab component <https://getbootstrap.com/javascript/#tabs>`__ from Bootstrap.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">

      	<div id="datasetForm:tabView" class="ui-tabs ui-widget ui-widget-content ui-corner-all ui-hidden-container ui-tabs-top" data-widget="content" style="border-bottom:0;">
        
	      	<ul class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all" role="tablist">
		      	<li class="ui-state-default ui-tabs-selected ui-state-active ui-corner-top" role="tab" aria-expanded="true" aria-selected="true" tabindex="0">
		      		<a href="#" onclick="return false;" tabindex="-1">Content Tab 1</a>
	      		</li>
		      	<li class="ui-state-default ui-corner-top" role="tab" aria-expanded="false" aria-selected="false" tabindex="-1">
		      		<a href="#" onclick="return false;" tabindex="-1">Content Tab 2</a>
	      		</li>
		      	<li class="ui-state-default ui-corner-top" role="tab" aria-expanded="false" aria-selected="false" tabindex="-1">
		      		<a href="#" onclick="return false;" tabindex="-1">Content Tab 3</a>
	      		</li>
	      	</ul>

      	</div>

      </div>
    </div>
  </div>

.. code-block:: html

  <p:tabView id="tabView" widgetVar="content" activeIndex="#{Page.selectedTabIndex}">
    <p:ajax event="tabChange" listener="#{Page.tabChanged}" update="@this" />
    <p:tab id="dataTab" title="#{bundle.files}">
        ...
    </p:tab>
    ...
  </p:tabView>


Modals
======

Modals are dialog prompts that act as popup overlays, but don't create a new browser window. We use them for confirmation on a delete to make sure the user is aware of the consequences of their actions. We also use them to allow users to execute simple actions on a page without requiring them to navigate to and from a separate page.

Buttons usually provide the UI prompt. A user clicks the button, which then opens a `Dialog component <https://www.primefaces.org/showcase/ui/overlay/dialog/basic.xhtml>`__  or `Confirm Dialog component <https://www.primefaces.org/showcase/ui/overlay/confirmDialog.xhtml>`__  from PrimeFaces that displays the modal with the necessary information and actions to take.

The modal is styled using the `Modal component <https://getbootstrap.com/javascript/#modals>`__ from Bootstrap, for a popup window that prompts a user for information, with overlay and a backdrop, then header, content, and buttons. We can use style classes from Bootstrap for large (``.bs-example-modal-lg``) and small (``.bs-example-modal-sm``) width options.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">

      <button type="button" class="btn btn-default" data-toggle="modal" data-target=".bs-example-modal-lg">Open Modal</button>

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

  <div class="modal bs-example-modal-lg" tabindex="-1" role="dialog" aria-labelledby="myLargeModalLabel">
    <div class="modal-dialog modal-lg" role="document">
      <div class="modal-content">
        ...
      </div>
    </div>
  </div>


.. |image1| image:: ./img/dataverse-project.png
   :class: img-responsive
