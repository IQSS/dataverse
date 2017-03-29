Foundations
+++++++++++

Foundational elements are the very basic building blocks to create a page in Dataverse. This page will go over some of these basic elements. Dataverse uses both Bootstrap and PrimeFaces, and conflicts between the two occasionally emerge. It takes some tweaking and overriding in order to get everything working as consistently as possible. 

For this guide, we will focus in particular on areas where our implementation differs from the default settings of Bootstrap and PrimeFaces. Each section includes links to relevant parts of the official Bootstrap guides, where you can find more details.


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

The color palette is set in the `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-colors>`__. There were some additions and some changes here and there.

Semantic colors include various colors assigned to meaningful contextual values. We convey meaning through color with a handful of emphasis utility classes. These may also be applied to links and will darken on hover just like our default link styles.

Primary/Brand
-------------

Colors from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-colors>`__. For the primary/brand color used in the Bootstrap stylesheet, we override (?) their blue with our orange `#C55B28` which comes from the Dataverse Project logo.

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
   <span class="text-brand"></span>
   <span class="text-info"></span>
   <span class="text-muted"></span>

Text
----

Text color from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-scaffolding>`__.

MAKE EVEN MORE NEW WERDS

.. code-block:: css

    body {
      color: #333;
    }
    
    @text-color: @black-50;

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <p style="color:#333;">Nullam id dolor id nibh ultricies vehicula ut id elit.</p>
      <p style="color:#7f7f7f;">Duis mollis, est non commodo luctus, nisi erat porttitor ligula.</p>
      <p style="color:#777;">Maecenas sed diam eget risus varius blandit sit amet non magna.</p>
    </div>
  </div>

.. code-block:: html

   <span class="text-brand"></span>
   <span class="text-info"></span>
   <span class="text-muted"></span>


Links
-----

Link color from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-links>`__.

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

Contextual classes can be used to style text and background colors from `Bootstrap CSS <http://getbootstrap.com/css/#helper-classes>`__.

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

      <img alt="image1" src="../_images/dataverse-project.png">

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

.. |image1| image:: ./img/dataverse-project.png
   :class: img-responsive

.. |image2| image:: ./img/dataverse-icon.jpg
   :class: img-responsive
