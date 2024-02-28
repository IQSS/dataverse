===========
Font Custom
===========

As mentioned under :ref:`style-guide-fontcustom` in the Style Guide, Dataverse uses `Font Custom`_ to create custom icon fonts.

.. _Font Custom: https://github.com/FontCustom/fontcustom

.. contents:: |toctitle|
	:local:

Previewing Icons
----------------

The process below updates a `preview page`_ that you can use to see how the icons look.

.. _preview page: ../_static/fontcustom-preview.html

In `scripts/icons/svg`_ in the source tree, you can see the SVG files that the icons are built from. 

.. _scripts/icons/svg: https://github.com/IQSS/dataverse/tree/develop/scripts/icons

Install Font Custom
-------------------

You'll need Font Custom and its dependencies installed if you want to update the icons.

Ruby Version
~~~~~~~~~~~~

Font Custom is written in Ruby. Ruby 3.0 didn't "just work" with FontAwesome but Ruby 2.7 was fine.

RVM is a good way to install a specific version of Ruby: https://rvm.io

Install Dependencies and Font Custom Gem
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The brew commands below assume you are on a Mac.

.. code-block:: bash

        brew tap bramstein/webfonttools
        brew update
        brew install woff2
        brew install sfnt2woff
        brew install fontforge
        brew install eot-utils
        gem install fontcustom


(``brew install sfnt2woff`` isn't currently listed in the FontCustom README but it's in mentioned in https://github.com/FontCustom/fontcustom/pull/385)

If ``fontcustom --help`` works now, you have it installed.

Updating Icons
--------------

Navigate to ``scripts/icons`` in the source tree (or `online`_) and you will find:

- An ``svg`` directory containing the "source" for the icons.
- Scripts to update the icons.

.. _online: https://github.com/IQSS/dataverse/tree/develop/scripts/icons

There is a copy of these icons in both the app and the guides. We'll update the guides first because it's much quicker to iterate and notice any problems with the icons.

Updating the Guides Icons
~~~~~~~~~~~~~~~~~~~~~~~~~

Run ``docs.sh`` and then open ``../../doc/sphinx-guides/source/_static/fontcustom-preview.html`` in a browser to look at the icons. (This is the `preview page`_ mentioned above that gets incorporated in the next Sphinx build.)

Update any files in the ``svg`` directory and run the script again to see any differences.

Note that Font Custom creates font files with unique names. For this reason, we should remove the old files from git as we add the new ones. The script deletes the old files for you but in a step below we'll do a ``git add`` to stage this change.

Updating the App Icons
~~~~~~~~~~~~~~~~~~~~~~

Assuming you're happy with how the icons look in the preview page in the guides, you can move on to updating the icons in the Dataverse app itself.

This time the script is called ``app.sh`` and it works the same way with the addition of tweaking some URLs. Go ahead and run this script and do a full "clean and build" before making sure the changes are visible in the application.

Committing Changes to Git
~~~~~~~~~~~~~~~~~~~~~~~~~

As mentioned above, icons are in both the app and the docs. Again, because the filenames change, we should make sure the old files are removed from git.

From the root of the repo, run the following:

.. code-block:: bash

        git add doc/sphinx-guides/source/_static
        git add src/main/webapp/resources

That should be enough to make sure old files are replaced by new ones. At this point, you can commit and make a pull request.

Caveats About Font Custom
-------------------------

Font Custom is a useful tool and has an order of magnitude more stars on GitHub than its competitors. However, an `issue`_ suggests that the tool is somewhat abandoned. Its domain has expired but you can still get at what used to be its website at https://fontcustom.github.io/fontcustom/

.. _issue: https://github.com/FontCustom/fontcustom/issues/321
