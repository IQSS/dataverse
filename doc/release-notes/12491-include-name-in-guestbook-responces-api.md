## BUG ##
Changed "userName" in response in API GET /api/guestbooks/{guestbookId}/responses
"userName" shows either the "Name" entered by the user, or Name of the Auth User, or "Guest". Actual Auth User's username is no longer returned.
