Geoconnect
==========

.. contents:: |toctitle|
	:local:

Geoconnect works as a middle layer, allowing geospatial data files in Dataverse to be visualized with Harvard WorldMap.

To understand the feature from the user perspective, see the :doc:`/user/data-exploration/worldmap` section of the User Guide.

As of this writing, the README at https://github.com/IQSS/geoconnect recommends not installing Geoconnect at this time due to an ongoing rewrite of the WorldMap code. If you are not deterred by this, read on!

To set up a Geoconnect development environment, you can follow the steps outlined in the `local_setup.md <https://github.com/IQSS/geoconnect/blob/master/local_setup.md>`_ guide. Although those instructions are for a local development setup, they may assist in installing Geoconnect in your production environment. See also "Geoconnect" under the :doc:`/developers/dev-environment` section of the Developer Guide.

Harvard Dataverse runs Geoconnect on Heroku. To make use of Heroku, you will need a Heroku account, as well as a few other prerequisites. Follow the instructions outlined in the `heroku_setup.md <https://github.com/IQSS/geoconnect/blob/master/heroku_setup.md>`_ guide. The `heroku.py <https://github.com/IQSS/geoconnect/blob/master/geoconnect/settings/heroku.py>`_ settings file may also be adapted for other environments. Please note, for the production environment, remember to set ``DEBUG=False``.

See also the :doc:`/admin/geoconnect-worldmap` section of the Admin Guide.
