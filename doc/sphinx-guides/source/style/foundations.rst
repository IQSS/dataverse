Foundations
+++++++++++

Foundation elements are the very basic building blocks to create a page in Dataverse. Here we will outline how we've applied the Bootstrap CSS to our UI, and how the CSS settings in our stylesheet meshes with it. Each section includes links to relevant parts of the official Bootstrap guides and other resources the UI utilize, where you can find more detailed documentation.


Grid Layout
===========

`Bootstrap <http://getbootstrap.com/css/#grid>`__ provides a responsive, fluid, 12-column grid system that we use to organize our page layouts.

We use the fixed-width ``.container`` class which provides responsive widths (i.e. auto, 750px, 970px or 1170px) based on media queries for the page layout, with a series of rows and columns for the content.

The grid layout uses ``.col-sm-*`` classes for horizontal groups of columns, inside a containing element with a ``.row`` class. Content should be placed within columns, and only columns may be immediate children of rows.

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

The default color palette is set in the `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-colors>`__. It provides the background, border, text and link colors used across the application.

Brand
-----

The brand color, a "burnt orange" `#C55B28`, is set in our custom CSS stylesheet and applied to the Dataverse logo and brand name in the navbar, as well other dataverse objects...

There are also the dataset (blue) and file (grey) styles are used to help indentify those object...

The dataset-blue is the same blue we use for the links... `.ui-widget-content a  {color: #428BCA;}`...

.. code-block:: css

    .text-brand {
      color:#C55B28;
      /* dataverse icon, search-include, mydata-cards_min */
    }
    .bg-brand {
      background:#C55B28;
      /* not used anywhere */
    }
    .bg-muted {
      background:#f5f5f5;
      /* header, roles-assign */
    }

    #navbarFixed .navbar-brand {
      color:#C55B28; padding-left:32px;
    }
    #navbarFixed .icon-dataverse {
      color:#C55B28; font-size:28px; margin: -4px 0 0 -27px; position: absolute;
    }
    #navbarFixed .label.status {
      display:block; float:left; margin-top:16px; background:#C55B28; font-size:14px; font-weight:normal;
    }

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="color-swatch" style="background-color:#C55B28;">bg-brand dataverse</div>
        <div class="color-swatch" style="background-color:#428BCA;">dataset</div>
        <div class="color-swatch" style="background-color:grey;">file</div>
        <div class="color-swatch" style="background-color:#f5f5f5;">bg-muted</div>
      </div>
    </div>
  </div>

.. code-block:: html
  
  <!-- code comments -->
   <div class="bg-brand"></div>
   <div class="bg-dataset"></div>
   <div class="bg-file"></div>
   <div class="bg-muted"></div>

Text
----

Text color is the default setting from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-scaffolding>`__.

.. code-block:: css

    body {
      color: #333;
    }

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
    </div>
  </div>

.. code-block:: html

   <p>...</p>


Links
-----

Link color is the default setting from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-links>`__.

Hover state is 15% darker. There is an override in our stylesheet for ``.ui-widget-content a`` which I believe is because of PrimeFaces.

.. code-block:: css

    @brand-primary: darken(#428bca, 6.5%)

    @link-color: @brand-primary;

    @link-hover-color: darken(@link-color, 15%);

    a {
      color: #337AB7;
    }

    a:hover {
      color: #23527C;
    }

    .ui-widget-content a {
      color: #428BCA;
    }

    .ui-widget-content a:hover, .ui-widget-content a:focus {
      color: #2A6496;
    }

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="color-swatch" style="background-color:#337AB7;">a</div>
        <div class="color-swatch" style="background-color:#23527C;">a:hover</div>
        <div class="color-swatch" style="background-color:#428BCA;">.ui-widget-content a</div>
        <div class="color-swatch" style="background-color:#2A6496;">.ui-widget-content a:hover/focus</div>
      </div>
    </div>
  </div>

.. code-block:: html

  <a>...</a>

  <span class="ui-widget-content">
    <a>...</a>
  </span>


Contextual Classes
------------------

Contextual classes can be used to style text and background colors from `Bootstrap CSS <http://getbootstrap.com/css/#helper-classes>`__. Semantic colors include various colors assigned to meaningful contextual values. We convey meaning through color with a handful of emphasis utility classes.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="color-swatch bg-primary">bg-primary</div>
        <div class="color-swatch bg-success">bg-success</div>
        <div class="color-swatch bg-info">bg-info</div>
        <div class="color-swatch bg-warning">bg-warning</div>
        <div class="color-swatch bg-danger">bg-danger</div>
      </div>
    </div>
  </div>

.. code-block:: html

   <div class="bg-primary"></div>
   <div class="bg-success"></div>
   <div class="bg-info"></div>
   <div class="bg-warning"></div>
   <div class="bg-danger"></div>


Typography
==========

The typeface, text size, line-height are set in the `Bootstrap CSS <http://getbootstrap.com/css/#type>`__. Bootstrap's global default ``font-size`` is **14px**, with a ``line-height`` of **1.428**, which is applied to the ``<body>`` and all paragraphs.

.. code-block:: css

   body {
     font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;
     font-size: 14px;
     line-height: 1.42857143;
   }

Logos
=====

The Dataverse Project logo is diplayed in the footer, and was the base for the creation of the favicon for the application as well as the dataverse icons.

.. raw:: html

  <div class="panel panel-default">
    <div class="panel-body text-center">

      <img alt="Dataverse Project" src="../_images/dataverse-project.png">

    </div>
  </div>

The brand logo used in the navbar was created to be a custom icon that represents a dataverse to be used across the application.

.. raw:: html

  <div class="panel panel-default">
    <div class="panel-body text-center">

      <img alt="Dataverse Icon" src="../_images/dataverse-icon.jpg" height="175">

    </div>
  </div>

Icons
=====

We use various icons across the application, in buttons, and as default thumbnails for repositories, dataset and files.

Bootstrap
---------

There are over 250 glyphs in font format from the Glyphicon Halflings set provided by `Bootstrap <http://getbootstrap.com/components/#glyphicons>`__. We utilize these mainly as icons inside of buttons and in message blocks.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
      <div>
         <span class="glyphicon glyphicon-search h1"></span>
         <span class="glyphicon glyphicon-user h1"></span>
         <span class="glyphicon glyphicon-ok h1"></span>
         <span class="glyphicon glyphicon-warning-sign h1"></span>
      </div>
      <button type="button" class="btn btn-default">
         <span class="glyphicon glyphicon-star" aria-hidden="true"></span> Star
      </button>

	  </div>
	</div>

.. code-block:: html

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-ok"></span>
   <span class="glyphicon glyphicon-warning-sign"></span>

Font Custom
-----------

With the use of `FontCustom <https://github.com/FontCustom/fontcustom>`__ we were able to generate our own custom icon webfonts. We use these in the result cards to help distinguish the dataverse, dateset and file results.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">

     <span class="icon-dataverse text-brand h1" style="color:#C55B28;"></span>
     <span class="icon-dataset text-info h1"></span>
     <span class="icon-other text-muted h1"></span>

	  </div>
	</div>

.. code-block:: html

   <span class="icon-dataverse text-brand"></span>
   <span class="icon-dataset text-info"></span>
   <span class="icon-other text-muted"></span>


Socicon Font
------------

We use `Socicon <http://www.socicon.com>`__ for the custom social icons. In the footer we use icons for Twitter, Github as well as icons in the share feature to select social media channels.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">

      <span class="socicon socicon-github h1" title="Dataverse On GitHub"></span>
      <span class="socicon socicon-twitter h1" title="Dataverse On Twitter"></span>
      <span class="socicon socicon-facebook h1" title="Dataverse On Facebook"></span>

	  </div>
	</div>

.. code-block:: html

   <span class="socicon socicon-github" title="Dataverse On GitHub"></span>
   <span class="socicon socicon-twitter" title="Dataverse On Twitter"></span>
   <span class="socicon socicon-facebook" title="Dataverse On Facebook"></span>

