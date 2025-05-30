Foundations
+++++++++++

Foundation elements are the very basic building blocks to create a page in Dataverse. Here we will outline how we've applied Bootstrap CSS to our UI, and how the CSS settings in our stylesheet mesh with it. Each section includes links to relevant parts of the official Bootstrap guides and other useful resources, where you can find more detailed documentation. We will also outline other UI resources like FontCustom and Socicon and how they are utilized.

.. contents:: |toctitle|
  :local:

Grid Layout
===========

`Bootstrap <https://getbootstrap.com/css/#grid>`__ provides a responsive, fluid, 12-column grid system that we use to organize our page layouts.

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
    
    
Typography
==========

The typeface, text size, and line-height are set in the `Bootstrap CSS <https://getbootstrap.com/css/#type>`__. We use Bootstrap's global default ``font-size`` of **14px**, with a ``line-height`` of **1.428**, which is applied to the ``<body>`` and all paragraphs.

.. code-block:: css

    /* bootstrap.css */
    body {
      font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;
      font-size: 14px;
      line-height: 1.42857143;
    }


Color Palette
=============

The default color palette is set in the `Bootstrap CSS <https://getbootstrap.com/css/#less-variables-colors>`__. It provides the background, border, text and link colors used across the application.


Brand Colors
------------

The Dataverse Project uses a particular color palette to help users quickly and easily identify the different types of objects: Dataverse collections, datasets, and files.

We use our brand color, a custom burnt orange ``{color:#C55B28;}``, which is set in our CSS stylesheet, "structure.css". There is also a set of blue "dataset" classes and grey "file" classes, used to help identify those objects when searching and navigating the application.

.. code-block:: css

    /* structure.css */
    .bg-dataverse {
      background:#C55B28;
    }
    .bg-dataset {
      background:#337AB7;
    }
    .bg-file {
      background:#F5F5F5;
    }

    #navbarFixed .navbar-brand {
      color: #C55B28;
    }
    #navbarFixed .icon-dataverse {
      color: #C55B28;
    }

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="color-swatch bg-dataverse"></div>
        <div class="color-swatch bg-dataset"></div>
        <div class="color-swatch bg-file"></div>
      </div>
    </div>
  </div>

.. code-block:: html
  
   <div class="bg-dataverse">...</div>
   <div class="bg-dataset">...</div>
   <div class="bg-file">...</div>

.. code-block:: css

    /* structure.css */
    .text-dataverse {
      color:#C55B28;
    }
    .text-dataset {
      color:#31708F;
    }
    .text-file {
      color:#F5F5F5;
    }

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <p class="text-dataverse">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
      <p class="text-dataset">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
      <p class="text-file">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
    </div>
  </div>

.. code-block:: html
  
   <p class="text-dataverse">...</p>
   <p class="text-dataset">...</p>
   <p class="text-file">...</p>


Text Colors
-----------

Text color is the default setting from `Bootstrap CSS <https://getbootstrap.com/css/#less-variables-scaffolding>`__.

.. code-block:: css

    /* bootstrap.css */
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


Link Colors
-----------

Link color is the default setting from `Bootstrap CSS <https://getbootstrap.com/css/#less-variables-links>`__. The hover state color is set to 15% darker.

**Please note**, there is a CSS override issue with the link color due to the use of both a Bootstrap stylesheet and a PrimeFaces stylesheet in the UI. We've added CSS such as ``.ui-widget-content a {color: #428BCA;}`` to our stylesheet to keep the link color consistent.

.. code-block:: css
    
    /* bootstrap.css */
    a {
      color: #337AB7;
    }
    a:hover {
      color: #23527C;
    }

    /* structure.css */
    .ui-widget-content a {
      color: #337AB7;
    }
    .ui-widget-content a:hover, .ui-widget-content a:focus {
      color: #23527C;
    }

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="color-swatch bg-link" style="background-color:#337AB7;"></div>
        <div class="color-swatch bg-linkhover" style="background-color:#23527C;"></div>
      </div>
    </div>
  </div>

.. code-block:: html

  <a>...</a>


Contextual Classes
------------------

Contextual classes from `Bootstrap CSS <https://getbootstrap.com/css/#helper-classes>`__ can be used to style background and text colors. Semantic colors include various colors assigned to meaningful contextual values. We convey meaning through color with a handful of emphasis utility classes.

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="color-swatch bg-primary"></div>
        <div class="color-swatch bg-success"></div>
        <div class="color-swatch bg-info"></div>
        <div class="color-swatch bg-warning"></div>
        <div class="color-swatch bg-danger"></div>
      </div>
    </div>
  </div>

.. code-block:: html

   <div class="bg-primary">...</div>
   <div class="bg-success">...</div>
   <div class="bg-info">...</div>
   <div class="bg-warning">...</div>
   <div class="bg-danger">...</div>

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <p class="text-muted">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
      <p class="text-primary">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
      <p class="text-success">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
      <p class="text-info">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
      <p class="text-warning">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
      <p class="text-danger">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
    </div>
  </div>

.. code-block:: html

   <p class="text-muted">...</p>
   <p class="text-primary">...</p>
   <p class="text-success">...</p>
   <p class="text-info">...</p>
   <p class="text-warning">...</p>
   <p class="text-danger">...</p>


Icons
=====

We use various icons across the application, which we get from Bootstrap, FontCustom and Socicon. They appear in buttons, in message blocks or as default thumbnails for Dataverse collections, datasets, and files.

Bootstrap Glyphicons
--------------------

There are over 250 glyphs in font format from the Glyphicon Halflings set provided by `Bootstrap <https://getbootstrap.com/components/#glyphicons>`__. We utilize these mainly as icons inside of buttons and in message blocks.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body h1">
        <span class="glyphicon glyphicon-search"></span>
        <span class="glyphicon glyphicon-user"></span>
        <span class="glyphicon glyphicon-lock"></span>
	  </div>
	</div>

.. code-block:: html

   <span class="glyphicon glyphicon-search"></span>
   <span class="glyphicon glyphicon-user"></span>
   <span class="glyphicon glyphicon-lock"></span>

.. _style-guide-fontcustom:

Font Custom Icon Font
---------------------

With the use of `Font Custom <https://github.com/FontCustom/fontcustom>`__ we generate our own custom icon fonts. We use these in the search result cards to help distinguish between Dataverse collection, dataset and file results.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
     <span class="icon-dataverse text-dataverse h1"></span>
     <span class="icon-dataset text-dataset h1"></span>
     <span class="icon-file text-file h1"></span>
	  </div>
	</div>

.. code-block:: html

   <span class="icon-dataverse text-dataverse"></span>
   <span class="icon-dataset text-dataset"></span>
   <span class="icon-file text-file"></span>

The :doc:`/developers/fontcustom` section of the Developer Guide explains how to update these custom icons.

Socicon Icon Font 
-----------------

We use `Socicon <https://www.socicon.com>`__ for our custom social icons. In the footer we use icons for Twitter and Github. In our Share feature, we also use custom social icons to allow users to select from a list of social media channels.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
      <span class="socicon socicon-github h1" title="Dataverse Project On GitHub"></span>
      <span class="socicon socicon-twitter h1" title="Dataverse Project On Twitter"></span>
      <span class="socicon socicon-facebook h1" title="Dataverse Project On Facebook"></span>
	  </div>
	</div>

.. code-block:: html

   <span class="socicon socicon-github" title="Dataverse Project On GitHub"></span>
   <span class="socicon socicon-twitter" title="Dataverse Project On Twitter"></span>
   <span class="socicon socicon-facebook" title="Dataverse Project On Facebook"></span>


Logos
=====

The Dataverse Project logo (below) is displayed in the footer, and was the basis for the creation of the application's icons and favicon.

Create both print and web version of the Dataverse Project logo by downloading this vector-based SVG file: :download:`dataverse_project_logo.svg <../_static/dataverse_project_logo.svg>`

.. raw:: html

  <div class="panel panel-default">
    <div class="panel-body text-center">
      <img alt="Dataverse Project" src="https://dataverse.harvard.edu/resources/images/dataverseproject_logo.jpg" class="img-responsive">
    </div>
  </div>

The brand logo (below) was created as a custom icon to represent the concept of a Dataverse collection. It is used as the logo in the Bootstrap navbar component and across the application.

Create both print and web version of the Dataverse collection logo by downloading this vector-based SVG file: :download:`Dataverse_brand_icon.svg <../_static/Dataverse_brand_icon.svg>`

.. raw:: html

  <div class="panel panel-default">
    <div class="panel-body text-center">
      <img alt="Dataverse Icon" src="../_images/dataverse-icon.jpg" height="175" class="img-responsive">
    </div>
  </div>

.. |image1| image:: ./img/dataverse-icon.jpg
   :class: img-responsive

Here is another vector-based SVG file with three rings: :download:`Dataverse_3ring-brand_icon_EqualSpace.svg <../_static/Dataverse_3ring-brand_icon_EqualSpace.svg>`
