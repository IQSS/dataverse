### Extend Restrict API to include new attributes

Original /restrict API only allowed for a boolean to update the restricted attribute of a file
The extended API still allows for the single boolean for backward compatibility.
This change also allows for a Json object to be passed which allows for the required ``restrict`` flag as well as optional attributes: ``enableAccessRequest`` and ``termsOfAccess``.
If ``enableAccessRequest`` is false then the ``termsOfAccess`` text must also be included.
