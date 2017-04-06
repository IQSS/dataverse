Patterns
++++++++

Patters are what emerge when using the foundation elements together with basic objects, like buttons and tables. Here we will outline how the Bootstrap CSS and our custom CSS are used to style `Bootstrap <http://getbootstrap.com/components/>`_ and `PrimeFaces <https://www.primefaces.org/showcase/>`_ UI components.


Buttons
=======

Action Buttons
--------------

For action buttons on a page, we include an icon and text label, and they are generally aligned to the right side of the page.

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

Textless-Icon-Only Buttons
--------------------------

There are a few places we use textless icon-only buttons.

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


Labels
======

Used for publication status and file tags...

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

Used at the top of the page, for help/information, success, warning, error messages.

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

