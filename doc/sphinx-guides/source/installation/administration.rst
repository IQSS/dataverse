Administration
==============

.. contents:: :local:

User Administration
-------------------

Deleting an API token
~~~~~~~~~~~~~~~~~~~~~

If an API token is compromised it should be deleted. Users will be able to do this themselves once https://github.com/IQSS/dataverse/issues/1098 is complete but until then someone with access to the database must do it.

Using the API token 7ae33670-be21-491d-a244-008149856437 as an example:

``delete from apitoken where tokenstring = '7ae33670-be21-491d-a244-008149856437';``

You should expect the output ``DELETE 1`` after issuing the command above.

After the API token has been deleted, users can generate a new one per :doc:`/user/account`.
