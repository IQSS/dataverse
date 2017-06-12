=======
Testing
=======

For testing in Dataverse, JUnit passes along test information to utilities like `Coveralls <https://coveralls.io/github/IQSS/dataverse>`_ which builds a quantifiable report of how much code coverage (% of code with verifiable tests) the project’s codebase has. We encourage test-driven development to help ensure new code has functional backend logic that can be repeated and help diagnose issues within the app.

.. contents:: |toctitle|
	:local:

Unit Tests
----------

Write them. `JUnit <http://junit.org/junit4>`_ is your friend. Creating unit tests for your code is a helpful way to test what you’ve built piece by piece.
  
You may notice when code is pushed, an automated Coveralls bot will let you know if it has increased or decreased code coverage.

- If code coverage is increasing: good! You made some test(s). 
- If code coverage is decreasing: write some unit tests to show the world just how good your code is!
 
A unit test should execute an operation of your code in a controlled fashion. You must make an assertion of what the expected response gives back. It's important to test optimistic output and assertions, but also test controlled failures such as 400 errors or other issues your API may encounter (to simulate human or machine error) with assertions stating such.

Know how your program should handle anticipated errors/exceptions and confirm with your test(s) that it does so properly. 

Integration Tests
-----------------

Write them. `REST-assured <https://github.com/jayway/rest-assured>`_ and `Selenium <http://seleniumhq.org>`_ are recommended. A Jenkins environment is available for automated testing: https://build.hmdc.harvard.edu:8443
 
Integration tests help ensure that your code gets along well with the rest of Dataverse. It’s good to test the different functions of your code with the different parts of your Dataverse which the feature may interact with.

These tests already live in the code base under ``/dataverse/src/test/java/edu/harvard/iq/dataverse/api/`` in your respective Dataverse directory. When you write an integration test of your own, it should be saved here.
 
To execute existing integration tests on your local Dataverse, a helpful command line tool to use is `Maven <http://maven.apache.org/ref/3.1.0/maven-embedder/cli.html>`_. You should have Maven installed as per the `Development Environment <http://guides.dataverse.org/en/latest/developers/dev-environment.html>`_ guide, but if not it’s easily done via Homebrew: ``brew install maven``. 

Once installed, you may run commands with ``mvn [options] [<goal(s)>] [<phase(s)>]``. 

+ If you want to run just one particular API test, it’s as easy as you think:

  ``mvn test -Dtest=FileRecordJobIT``

+ To run more than one test at a time, separate by commas:

  ``mvn test -Dtest=FileRecordJobIT,ConfirmEmailIT``

+ To run any test(s) on a particular domain, replace localhost:8080 with desired domain name:

  ``mvn test -Dtest=FileMetadataIT -Ddataverse.test.baseurl='http://localhost:8080'``

**Note:** if you’re trying to run the test suite and receive the error "Dataverse config issue: No API key defined for built in user management" just run the curl command below.

``curl -X PUT -d 'burrito' http://localhost:8080/api/admin/settings/BuiltinUsers.KEY``

Writing Tests
-------------

When writing tests, you may find it helpful to first map out which functions of your code you want to test, and then write a functional unit test for each which can later comprise a larger integration test. 

Recall that integration tests should live in the ``/dataverse/src/test/java/edu/harvard/iq/dataverse/api`` directory, or if you’re on Netbeans then navigate to the project’s Test Packages → edu.harvard.iq.dataverse.api and place your testnameIT.java file there!

If writing tests is new to you: in NetBeans, poke around existing tests. If you read through any you may notice each test within a file (bearing the @Test prefix) has an assertion which specifies the expected result (expected by the test program). 

To run a test in Netbeans: from the Run dropdown menu, select Test File. You can view the glassfish logs and the test output back to back.

The output in the file’s test window (beside the console) corresponds to what each line of each @Test block is doing. Many existing tests provide readable Json output, and how verbose the output is up to you as a developer.

You do not have to reinvent the wheel. There are many useful methods you can call in your own tests -- especially within UtilIT.java -- when you need your test to create and/or interact with generated accounts, files, datasets, etc. Similar methods can subsequently delete them to get them out of your way as desired before the test has concluded.

For example, if you’re testing your code’s operations with user accounts, the method ``UtilIT.createRandomUser();`` can generate an account for your test to work with. The same account can then be deleted by your program by calling the ``UtilIT.deleteUser();`` method on the imaginary friend your test generated.

Remember, it’s only a test (and it's not graded)! Some guidelines to bear in mind: 

- Map out which logical functions you want to test
- Understand what’s being tested and ensure it’s repeatable
- Assert the conditions of success / return values for each operation
  * A useful resource would be `HTTP status codes <http://www.restapitutorial.com/httpstatuscodes.html>`_
- Let the code do the labor; automate everything that happens when you run your test file.
- Just as with any development, if you’re stuck: ask for help!


Troubleshooting
---------------

In your local development environment, it canbe tricky to make *all* tests pass. This is especially true for some isolated features and tests that aren't necessarily indicative of a working/non-working Dataverse app. 

One such example as of 4.6.2 is in ``DatasetsIT.java``. Three of the thirteen tests in this file depend on a numeric identifier sequence for datasets (to test a certain non-default feature), but this sequence may not be preconfigured in your database. To remedy this, there is a SQL script you can run found `here <http://guides.dataverse.org/en/latest/installation/config.html#identifiergenerationstyle>`_ which will enable the test conditions to pass. Read the :IdentifierGenerationStyle section for more information.

If the ``DatasetsIT.java`` test is still failing, and your logs are complaining about privateurl or "unable to find valid certification path to requested target" then you may need to configure your siteUrl to specify localhost:8080 via glassfish. This can be done with the following:

``asadmin create-jvm-options "-Ddataverse.siteUrl=http\://localhost\:8080"`` 

Defining the siteUrl as localhost:8080 stops the app from referring to your domain name and will also clean up referential email links sent from your app such as email confirmation, password reset, etc.

It's also helpful to publish your root dataverse before running all REST-assured tests. Some discrepancies can stop certain tests from working, such as ``FilesIT.java``. If this integration test is mostly failing, it is likely due to the permissions of your root dataverse. The solution to this:

+ From the localhost Dataverse UI, log in as dataverseAdmin (password: admin) and click the Edit button for your root dataverse.
+ Navigate to Permissions, then the Edit Access button.
+ Under "Who can add to this dataverse?" choose "Anyone with a dataverse account can add sub dataverses"
+ This should allow the 10 tests dependent on adding new dataverses to now pass. Save changes and run tests for ``FilesIT.java`` again.

Alternatively, this same step can be done with a script, found here:
``scripts/search/tests/grant-authusers-add-on-root``

