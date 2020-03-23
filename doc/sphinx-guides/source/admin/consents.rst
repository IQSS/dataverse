Consents
=========
Consents are used for user registration where you can define what user has to(or can) accept in order to successfully register account.

Dataverse offers a possibility of adding/editing consents that are for superusers only.
If you are a logged-in superuser, you can access it by REST api.

.. contents:: Contents:
    :local:

Listing consents
----------------
If you already have added consents you can list all of them by using::

        curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/consents

If you just want to see single consent you need to know the name of this consent and use::

        curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/consents/{name}


Creating consents
-----------------
In order to create consent you need to follow the following json structure:

.. code-block:: json

    {
        "name": "newConsent",
        "displayOrder": 0,
        "required": false,
        "hidden": false,
        "consentDetails": [
            {
                "language": "en",
                "text": "New consent"
            },
            {
                "language": "pl",
                "text": "neeeee"
            }
        ],
        "consentActions": [
        {
            "consentActionType":"SEND_NEWSLETTER_EMAIL",
            "actionOptions": "{\"email\":\"laro@op.pl\"}"
        }]
    }
..

The curl command below assumes you have kept the name “consent.json” and that this file is in your current working directory.

::

    curl -H "X-Dataverse-key: $API_TOKEN" -X POST http://localhost:8080/api/admin/consents --upload-file consent.json

Consent needs to have english version since it is the default one
and it will be shown when no consent in user language will be available.
Consent actions are optional and if you don't need them just remove the key and value.

- name - unique name for consent.
- displayOrder - order of consent.
- required - is the consent required to be accepted.
- hidden - is the consent hidden from all users. (Used mainly to get rid of unused consent since they are by definition non-removable).

* consentDetails - list that holds the details about consent.
* language - language that is associated with the displayed text.
* text - text that is shown to the user in given language.

- consentActions - generic structure that allows to execute certain actions after user accepts the consent.
- consentActionType - type of action.
- actionOptions - configuration specific to action type.

Action Types
~~~~~~~~~~~~
.. list-table:: Action types
   :widths: 25 50

   * - Action type
     - Description
   * - SEND_NEWSLETTER_EMAIL
     - Sends a request for newsletter subscription at provided email address. Email will contain information about the user that accepted subscription consent.

.. list-table:: Available options for SEND_NEWSLETTER_EMAIL action type
   :widths: 25 50

   * - KEY
     - VALUE
   * - email
     - {Email address to the person that manages subscriptions for the newsletter}

Editing consents
-----------------
Easiest way to edit the consent is to first retrieve it::

    curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/consents/{name}

Copy the result, then edit the desired fields, there are few restrictions though what are you allowed to edit:

- displayOrder
- required
- hidden
- consent actions
- consent details are "append only", you can only add consent in new language but you cannot modify them.

Then you can send edited consent::

    curl -H "X-Dataverse-key: $API_TOKEN" -X PUT http://localhost:8080/api/admin/consents/{name} --upload-file consent.json

