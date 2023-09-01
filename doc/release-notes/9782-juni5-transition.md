# Migrating all test to JUnit 5
With this release, we transition all of our test cases (see `src/test/`) to use JUnit 5 only.
Moving forward from JUnit 4 will allow writing tests in more concise and easier ways.
The tests themselves have not been altered, but updated to match JUnit 5 ways.
They have not been extended or dropped coverage; this is mostly a preparation of things to come in the future. 
If you are writing tests in JUnit 4 in your feature branches, you need to migrate.
The development guides section of testing has been updated as well.
