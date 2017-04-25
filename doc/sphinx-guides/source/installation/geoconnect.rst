Geoconnect
==========

Geoconnect works as a middle layer, allowing geospatial data files in Dataverse to be visualized with Harvard WorldMap.

To setup a Geoconnect development environment, you can follow the steps outlined in the `local_setup.md <https://github.com/IQSS/geoconnect/blob/master/local_setup.md>`_ guide. Although those instructions above are for a local development setup, they may assist in installing Geoconnect in your production environment.

Harvard Dataverse runs Geoconnect using Heroku. To set up Heroku, you will need a Heroku account, as well as a few other prerequisites. Follow the instructions outlined in the `heroku_setup.md <https://github.com/IQSS/geoconnect/blob/master/heroku_setup.md>`_ guide. The `heroku.py <https://github.com/IQSS/geoconnect/blob/master/geoconnect/settings/heroku.py>`_ settings file may also be adapted for other environments. Please note, for the production environment, remember to set ``DEGUG=False``.
