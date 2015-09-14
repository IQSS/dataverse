Administration
==============

.. contents:: :local:

User Administration
-------------------

Deleting an API token
~~~~~~~~~~~~~~~~~~~~~

If an API token is compromised it should be deleted. Users can generate a new one for themselves, but someone with access to the database can delete tokens as well.

Using the API token 7ae33670-be21-491d-a244-008149856437 as an example:

``delete from apitoken where tokenstring = '7ae33670-be21-491d-a244-008149856437';``

You should expect the output ``DELETE 1`` after issuing the command above.

After the API token has been deleted, users can generate a new one per :doc:`/user/account`.
