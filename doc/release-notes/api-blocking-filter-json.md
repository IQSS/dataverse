* When any `ApiBlockingFilter` policy applies to a request, the JSON in the body of the error response is now valid JSON.
  In case an API client did any special processing to allow it to parse the body, that is no longer necessary.
  The status code of such responses has not changed.
