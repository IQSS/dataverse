Foundations
+++++++++++

Foundation elements are the very basic building blocks to create a page in Dataverse. Here we will outline how we've applied Bootstrap CSS to our UI, and how the CSS settings in our stylesheet mesh with it. Each section includes links to relevant parts of the official Bootstrap guides and other useful resources, where you can find more detailed documentation.


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
    
    
Typography
==========

The typeface, text size, and line-height are set in the `Bootstrap CSS <http://getbootstrap.com/css/#type>`__. We use Bootstrap's global default ``font-size`` of **14px**, with a ``line-height`` of **1.428**, which is applied to the ``<body>`` and all paragraphs.

.. code-block:: css

   body {
     font-family: "Helvetica Neue",Helvetica,Arial,sans-serif;
     font-size: 14px;
     line-height: 1.42857143;
   }


Color Palette
=============

The default color palette is set in the `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-colors>`__. It provides the background, border, text and link colors used across the application.


Object/Brand
------------

Dataverse uses a particular color palette to help users identify common types of objects quickly and easily.

The brand color, a "burnt orange" `#C55B28`, is set in our custom CSS stylesheet and applied to the Dataverse logo and brand name in the navbar, as well other Dataverse objects.

The blue "dataset" style is used to help identify dataset related objects in Dataverse. 

The grey "file" style is used to help identify file related objects in Dataverse.

For links, we use the same blue as we use for datasets: `.ui-widget-content a {color: #428BCA;}`

.. code-block:: css

    /* structure.css */
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
      color: #C55B28;
    }
    #navbarFixed .icon-dataverse {
      color: #C55B28;
    }

.. raw:: html

  <div class="panel panel-default code-example">
    <div class="panel-body">
      <div class="color-swatches">
        <div class="color-swatch bg-dataverse" style="background-color:#C55B28;"></div>
        <div class="color-swatch bg-dataset" style="background-color:#428BCA;"></div>
        <div class="color-swatch bg-file" style="background-color:grey;"></div>
        <div class="color-swatch bg-muted" style="background-color:#f5f5f5;"></div>
      </div>
    </div>
  </div>

.. code-block:: html
  
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

Link color is the default setting from `Bootstrap CSS <http://getbootstrap.com/css/#less-variables-links>`__. The hover state is set to 15% darker using Less variables and functions.

.. code-block:: css
    
    /* bootstrap.css */
    a {
      color: #337AB7;
    }
    a:hover {
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

.. raw:: html

  <div class="panel panel-warning">
    <div class="panel-heading">
      <h3 class="panel-title">Warning!</h3>
    </div>
    <div class="panel-body bg-warning">
      <p>There is a CSS override issue with both Bootstrap and PrimeFaces stylesheets over the link color.</p>
    </div>
  </div>

Contextual Classes
------------------

Contextual classes from `Bootstrap CSS <http://getbootstrap.com/css/#helper-classes>`__ can be used to style background and text colors. Semantic colors include various colors assigned to meaningful contextual values. We convey meaning through color with a handful of emphasis utility classes.

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

   <div class="bg-primary"></div>
   <div class="bg-success"></div>
   <div class="bg-info"></div>
   <div class="bg-warning"></div>
   <div class="bg-danger"></div>


Icons
=====

We use various icons across the application. They appear in buttons and as default thumbnails for dataverses, datasets, and files.

Bootstrap Icons
---------------

There are over 250 glyphs in font format from the Glyphicon Halflings set provided by `Bootstrap <http://getbootstrap.com/components/#glyphicons>`__. We utilize these mainly as icons inside of buttons and in message blocks.

.. raw:: html

	<div class="panel panel-default code-example">
	  <div class="panel-body">
        <button type="button" class="btn btn-default">
           <span class="glyphicon glyphicon-star" aria-hidden="true"></span> Star
        </button>
        <button type="button" class="btn btn-default">
           <span class="glyphicon glyphicon-flag" aria-hidden="true"></span> Flag
        </button>
        <button type="button" class="btn btn-default">
           <span class="glyphicon glyphicon-leaf" aria-hidden="true"></span> Leaf
        </button>
	  </div>
	</div>

.. code-block:: html

   <button type="button" class="btn btn-default">
       <span class="glyphicon glyphicon-star" aria-hidden="true"></span> Star
   </button>
   <button type="button" class="btn btn-default">
       <span class="glyphicon glyphicon-flag" aria-hidden="true"></span> Flag
   </button>
   <button type="button" class="btn btn-default">
       <span class="glyphicon glyphicon-leaf" aria-hidden="true"></span> Leaf
   </button>

FontCustom Icons
----------------

With the use of `FontCustom <https://github.com/FontCustom/fontcustom>`__ we were able to generate our own custom icon webfonts. We use these in the search result cards to help distinguish between dataverse, dataset and file results.

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


Socicon Font Icons
------------------

We use `Socicon <http://www.socicon.com>`__ for our custom social icons. In the footer we use icons for Twitter and Github. In our Share feature, we also use custom social icons to allow users to select from a list of social media channels.

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


Logos
=====

The Dataverse Project logo (below) is diplayed in the footer, and was the basis for the creation of the application's icons and favicon.

.. raw:: html

  <div class="panel panel-default">
    <div class="panel-body text-center">
      <img alt="Dataverse Project" src="../_images/dataverse-project.png">
    </div>
  </div>

The brand logo (below) was created as a custom icon to represent the concept of a "dataverse." It is used as the brand logo in the Bootstrap navbar component and across the application.

.. raw:: html

  <div class="panel panel-default">
    <div class="panel-body text-center">
      <img alt="Dataverse Icon" src="../_images/dataverse-icon.jpg" height="175">
    </div>
  </div>
