Foundations
+++++++++++

Foundational elements are the very basic elements to create a page in Dataverse. These basic elements have been broken down to Grid Layout, Color pPalette, Typography, Logos, Icons.


Grid Layout
===========

`Bootstrap <http://getbootstrap.com/css/#grid>`_ provides a responsive, fluid, 12-column grid system.

We use the fixed-width ``.container`` class which provides repsonive widths (i.e. auto, 750px, 970px or 1170px) based on media queries for the page layout, with a series of rows and columns for the content.

The grid layout uses ``.col-sm-*`` classes for horizontal groups of columns, inside a containing element with a ``.row`` class. Content should be placed within columns, and only columns may be immediate children of rows.

Code Sample:

.. code-block:: html

    <div class="container">
        <div class="row">
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        	<div class="col-sm-1">.col-sm-1</div>
        </div>
        <div class="row">
        	<div class="col-sm-8">.col-sm-8</div>
        	<div class="col-sm-4">.col-sm-4</div>
        </div>
    </div>

Color Palette
=============

The color palette is set in the `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-colors>`_.

Primary/Brand
-------------

Colors from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-colors>`_.

``.text-brand {color:#C55B28;}``

``.bg-brand {background:#C55B28;}``

.. raw:: html

	<div class="row">
		<div style="height:100px" class="col-sm-2 bg-primary">bg-primary</div>
		<div style="height:100px" class="col-sm-2 bg-success">bg-success</div>
		<div style="height:100px" class="col-sm-2 bg-info">bg-info</div>
		<div style="height:100px" class="col-sm-2 bg-warning">bg-warning</div>
		<div style="height:100px" class="col-sm-2 bg-danger">bg-danger</div>
	</div>

Text
----

Text color from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-scaffolding>`_.

``{color: #333;}``
``@text-color: @black-50;``

Links
-----

Link color from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-links>`_.

``@link-color: @brand-primary;``

``@link-hover-color: darken(@link-color, 15%);``

``a {color: #337AB7;}``

``a:hover {color: #23527C;}``

``.ui-widget-content a {color: #428BCA;}``

``.ui-widget-content a:hover, .ui-widget-content a:focus {color: #2A6496;}``


Typography
==========

The typeface, text size, line-height are set in the `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-typography>`_.

``font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;``

``font-size: 14px;``

``line-height: 1.42857143;``


Logos
=====

There are two logos that we use in the application. There is the Dataverse logo and the Dataverse Project logo.

* Dataverse
* Dataverse Project


Icons
=====

We use various icons across the application, in buttons, and as default thumbnails for repositories, dataset and files.

Bootstrap
---------

There are over 250 glyphs in font format from the Glyphicon Halflings set provided by `Bootstrap <http://getbootstrap.com/components/#glyphicons>`_.

* Search
* Buttons
* Account
* Info
* Message block Help/Error/Success

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-ok"></span>
   <span class="glyphicon glyphicon-warning-sign"></span>

	  </div>
	</div>

.. code-block:: html

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-ok"></span>
   <span class="glyphicon glyphicon-warning-sign"></span>

Font Custom
-----------

With the use of `FontCustom <https://github.com/FontCustom/fontcustom>`_ we were able to generate our own custom icon webfonts.

* Default dataverse, dataset
* File type

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-ok"></span>
   <span class="glyphicon glyphicon-warning-sign"></span>

	  </div>
	</div>

.. code-block:: html

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-ok"></span>
   <span class="glyphicon glyphicon-warning-sign"></span>


Socicon Font
------------

We use `Socicon <http://www.socicon.com>`_ for the custom social icons. 

* Footer icons Twitter, Github
* Sharrre icons Facebook, Twitter, Google Plus

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-ok"></span>
   <span class="glyphicon glyphicon-warning-sign"></span>

	  </div>
	</div>

.. code-block:: html

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-ok"></span>
   <span class="glyphicon glyphicon-warning-sign"></span>

