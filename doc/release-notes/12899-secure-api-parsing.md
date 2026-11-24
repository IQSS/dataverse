
A bug in the API to edit Guestbooks (introduced in v6.11) that would delete existing custom question answers, has been fixed. See #12899 .

Native and SWORD APIs related to adding/editing datasets and editing Guestbooks now respond with a 400/BAD_REQUEST error if you're input includes system-managed fields (such as `id`, `releaseTime`, or `UNF`) and the values for these fields differs from the current state of the object.
