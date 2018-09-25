=======
Testing
=======

In order to keep our codebase healthy, the Dataverse project encourages developers to write automated tests in the form of unit tests and integration tests. We also welcome ideas for how to improve our automated testing.

.. contents:: |toctitle|
	:local:

The Health of a Codebase
------------------------

Before we dive into the nut and bolts of testing, let's back up for a moment and think about why we write automated tests in the first place. Writing automated tests is an investment and leads to better quality software. Counterintuitively, writing tests and executing them regularly allows a project to move faster. Martin Fowler explains this well while talking about the health of a codebase:

    "This is an economic judgment. Several times, many times, I run into teams that say something like, 'Oh well. Management isn't allowing us to do a quality job here because it will slow us down. And we've appealed to management and said we need to put more quality in the code, but they've said no, we need to go faster instead.' And my comment to that is well, as soon as you’re framing it in terms of code quality versus speed, you've lost. Because the whole point of refactoring is to go faster.

    "And this is why I quite like playing a bit more with the metaphor as the health of a codebase. If you keep yourself healthy then you'll be able to run faster. But if you just say, 'Well, I want to run a lot so I'm therefore going to run a whole load all the time and not eat properly and not pay attention about this shooting pain going up my leg,' then you’re not going to be able to run quickly very long. **You have to pay attention to your health. And same with the codebase. You have to continuously say, 'How do we keep it in a healthy state? Then we can go fast,' because we’re running marathons here with codebases. And if we neglect that internal quality of the codebase, it hits you surprisingly fast.**"

    --Martin Fowler at https://devchat.tv/ruby-rogues/178-rr-book-club-refactoring-ruby-with-martin-fowler

Testing in Depth
----------------

`Security in depth <https://en.wikipedia.org/wiki/Defense_in_depth_(computing)>`_ might mean that your castle has a moat as well as high walls. Likewise, when testing, you should consider testing a various layers of the stack using both unit tests and integration tests.

When writing tests, you may find it helpful to first map out which functions of your code you want to test, and then write a functional unit test for each which can later comprise a larger integration test. 

Unit Tests
----------

Creating unit tests for your code is a helpful way to test what you've built piece by piece.

Unit tests can be executed without runtime dependencies on PostgreSQL, Solr, or any other external system. They are the lowest level of testing and are executed constantly on developers' laptops as part of the build process and via continous integration services in the cloud.

A unit test should execute an operation of your code in a controlled fashion. You must make an assertion of what the expected response gives back. It's important to test optimistic output and assertions (the "happy path"), as well as unexpected input that leads to failure conditions. Know how your program should handle anticipated errors/exceptions and confirm with your test(s) that it does so properly. 

Unit Test Automation Overview
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We use a variety of tools to write, execute, and measure the code coverage of unit tests, including Maven, JUnit, Jacoco, GitHub, Travis, and Coveralls. We'll explain the role of each tool below, but here's an overview of what you can expect from the automation we've set up.

As you prepare to make a pull request, as described in the :doc:`version-control` section, you will be working on a new branch you create from the "develop" branch. Let's say your branch is called ``1012-private-url``. As you work, you are constantly invoking Maven to build the war file. When you do a "clean and build" in Netbeans, Maven runs all the unit tests (anything ending with ``Test.java``) and the runs the results through a tool called Jacoco that calculates code coverage. When you push your branch to GitHub and make a pull request, a web service called Travis CI runs Maven and Jacoco on your branch and pushes the results to Coveralls, which is a web service that tracks changes to code coverage over time.

To make this more concrete, observe that https://github.com/IQSS/dataverse/pull/3111 has comments from a GitHub user called ``coveralls`` saying things like "Coverage increased (+0.5%) to 5.547% when pulling dd6ceb1 on 1012-private-url into be5b26e on develop." Clicking on the comment should lead you to a URL such as https://coveralls.io/builds/7013870 which shows how code coverage has gone up or down. That page links to a page such as https://travis-ci.org/IQSS/dataverse/builds/144840165 which shows the build on the Travis side that pushed the results ton Coveralls.

The main takeaway should be that we care about unit testing enough to measure the changes to code coverage over time using automation. Now let's talk about how you can help keep our code coverage up by writing unit tests with JUnit.

Writing Unit Tests with JUnit
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are aware that there are newer testing tools such as TestNG, but we use `JUnit <http://junit.org>`_ because it's tried and true.

If writing tests is new to you, poke around existing unit tests which all end in ``Test.java`` and live under ``src/test``. Each test is annotated with ``@Test`` and should have at least one assertion which specifies the expected result. In Netbeans, you can run all the tests in it by clicking "Run" -> "Test File". From the test file, you should be able to navigate to the code that's being tested by right-clicking on the file and clicking "Navigate" -> "Go to Test/Tested class". Likewise, from the code, you should be able to use the same "Navigate" menu to go to the tests.

NOTE: Please remember when writing tests checking possibly localized outputs to check against ``en_US.UTF-8`` and ``UTC``
l10n strings!

Refactoring Code to Make It Unit-Testable
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Existing code is not necessarily written in a way that lends itself to easy testing. Generally speaking, it is difficult to write unit tests for both JSF "backing" beans (which end in ``Page.java``) and "service" beans (which end in ``Service.java``) because they require the database to be running in order to test them. If service beans can be exercised via API they can be tested with integration tests (described below) but a good technique for making the logic testable it to move code to "util beans" (which end in ``Util.java``) that operate on Plain Old Java Objects (POJOs). ``PrivateUrlUtil.java`` is a good example of moving logic from ``PrivateUrlServiceBean.java`` to a "util" bean to make the code testable.

Observing Changes to Code Coverage
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Once you've written some tests, you're probably wondering how much you've helped to increase the code coverage. In Netbeans, do a "clean and build." Then, under the "Projects" tab, right-click "dataverse" and click "Code Coverage" -> "Show Report". For each Java file you have open, you should be able to see the percentage of code that is covered by tests and every line in the file should be either green or red. Green indicates that the line is being exercised by a unit test and red indicates that it is not.

In addition to seeing code coverage in Netbeans, you can also see code coverage reports by opening ``target/site/jacoco/index.html`` in your browser.

Testing Commands
^^^^^^^^^^^^^^^^

You might find studying the following test classes helpful in writing tests for commands:

- CreatePrivateUrlCommandTest.java
- DeletePrivateUrlCommandTest.java
- GetPrivateUrlCommandTest.java

In addition, there is a writeup on "The Testable Command" at https://github.com/IQSS/dataverse/blob/develop/doc/theTestableCommand/TheTestableCommand.md .

Running Non-Essential (Excluded) Unit Tests
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You should be aware that some unit tests have been deemed "non-essential" and have been annotated with ``@Category(NonEssentialTests.class)`` and are excluded from the "dev" Maven profile, which is the default profile. All unit tests (that have not been annotated with ``@Ignore``), including these non-essential tests, are run from continuous integration systems such as Jenkins and Travis CI with the following ``mvn`` command that invokes a non-default profile:

``mvn test -P all-unit-tests``

Typically https://travis-ci.org/IQSS/dataverse will show a higher number of unit tests executed because it uses the profile above.

Generally speaking, unit tests have been flagged as non-essential because they are slow or because they require an Internet connection. You should not feel obligated to run these tests continuously but you can use the ``mvn`` command above to run them. To iterate on the unit test in Netbeans and execute it with "Run -> Test File", you must temporarily comment out the annotation flagging the test as non-essential.

Integration Tests
-----------------

Unit tests are fantastic for low level testing of logic but aren't especially real-world-applicable because they do not exercise Dataverse as it runs in production with a database and other runtime dependencies. We test in-depth by also writing integration tests to exercise a running system.

Unfortunately, the term "integration tests" can mean different things to different people. For our purposes, an integration test has the following qualities:

- Integration tests exercise Dataverse APIs.
- Integration tests are not automatically on developers' laptops.
- Integration tests operate on an installation of Dataverse that is running and able to talk to both PostgreSQL and Solr.
- Integration tests are written using REST Assured.

Running the full API test suite using Docker
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To run the full suite of integration tests on your laptop, we recommend using the "all in one" Docker configuration described in ``conf/docker-aio/readme.txt`` in the root of the repo.

Alternatively, you can run tests against Glassfish running on your laptop by following the "getting set up" steps below.

Getting Set Up to Run REST Assured Tests
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Unit tests are run automatically on every build, but dev environments and servers require special setup to run REST Assured tests. In short, Dataverse needs to be placed into an insecure mode that allows arbitrary users and datasets to be created and destroyed. This differs greatly from the out-of-the-box behavior of Dataverse, which we strive to keep secure for sysadmins installing the software for their institutions in a production environment.

The :doc:`dev-environment` section currently refers developers here for advice on getting set up to run REST Assured tests, but we'd like to add some sort of "dev" flag to the installer to put Dataverse in "insecure" mode, with lots of scary warnings that this dev mode should not be used in production.

The instructions below assume a relatively static dev environment on a Mac. There is a newer "all in one" Docker-based approach documented in the :doc:`/developers/containers` section under "Docker" that you may like to play with as well.

The Burrito Key
^^^^^^^^^^^^^^^

For reasons that have been lost to the mists of time, Dataverse really wants you to to have a burrito. Specifically, if you're trying to run REST Assured tests and see the error "Dataverse config issue: No API key defined for built in user management", you must run the following curl command (or make an equivalent change to your database):

``curl -X PUT -d 'burrito' http://localhost:8080/api/admin/settings/BuiltinUsers.KEY``

Without this "burrito" key in place, REST Assured will not be able to create users. We create users to create objects we want to test, such as dataverses, datasets, and files.

Root Dataverse Permissions
^^^^^^^^^^^^^^^^^^^^^^^^^^

In your browser, log in as dataverseAdmin (password: admin) and click the "Edit" button for your root dataverse. Navigate to Permissions, then the Edit Access button. Under "Who can add to this dataverse?" choose "Anyone with a dataverse account can add sub dataverses" if it isn't set to this already. 

Alternatively, this same step can be done with this script: ``scripts/search/tests/grant-authusers-add-on-root``

Publish Root Dataverse
^^^^^^^^^^^^^^^^^^^^^^

The root dataverse must be published for some of the REST Assured tests to run.

dataverse.siteUrl
^^^^^^^^^^^^^^^^^

When run locally (as opposed to a remote server), some of the REST Assured tests require the ``dataverse.siteUrl`` JVM option to be set to ``http://localhost:8080``. See "JVM Options" under the :doc:`/installation/config` section of the Installation Guide for advice changing JVM options. First you should check to check your JVM options with:

``./asadmin list-jvm-options | egrep 'dataverse|doi'``

If ``dataverse.siteUrl`` is absent, you can add it with:

``./asadmin create-jvm-options "-Ddataverse.siteUrl=http\://localhost\:8080"`` 

Identifier Generation 
^^^^^^^^^^^^^^^^^^^^^

``DatasetsIT.java`` exercises the feature where the "identifier" of a DOI can be a digit and requires a sequence to be added to your database.  See ``:IdentifierGenerationStyle`` under the :doc:`/installation/config` section for adding this sequence to your installation of PostgreSQL.


Writing Integration Tests with REST Assured
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Before writing any new REST Assured tests, you should get the tests to pass in an existing REST Assured test file. ``BuiltinUsersIT.java`` is relatively small and requires less setup than other test files. 

You do not have to reinvent the wheel. There are many useful methods you can call in your own tests -- especially within UtilIT.java -- when you need your test to create and/or interact with generated accounts, files, datasets, etc. Similar methods can subsequently delete them to get them out of your way as desired before the test has concluded.

For example, if you’re testing your code’s operations with user accounts, the method ``UtilIT.createRandomUser();`` can generate an account for your test to work with. The same account can then be deleted by your program by calling the ``UtilIT.deleteUser();`` method on the imaginary friend your test generated.

Remember, it’s only a test (and it's not graded)! Some guidelines to bear in mind: 

- Map out which logical functions you want to test
- Understand what’s being tested and ensure it’s repeatable
- Assert the conditions of success / return values for each operation
  * A useful resource would be `HTTP status codes <http://www.restapitutorial.com/httpstatuscodes.html>`_
- Let the code do the labor; automate everything that happens when you run your test file.
- Just as with any development, if you’re stuck: ask for help!

To execute existing integration tests on your local Dataverse, a helpful command line tool to use is `Maven <http://maven.apache.org/ref/3.1.0/maven-embedder/cli.html>`_. You should have Maven installed as per the `Development Environment <http://guides.dataverse.org/en/latest/developers/dev-environment.html>`_ guide, but if not it’s easily done via Homebrew: ``brew install maven``. 

Once installed, you may run commands with ``mvn [options] [<goal(s)>] [<phase(s)>]``. 

+ If you want to run just one particular API test, it’s as easy as you think:

  ``mvn test -Dtest=FileRecordJobIT``

+ To run more than one test at a time, separate by commas:

  ``mvn test -Dtest=FileRecordJobIT,ConfirmEmailIT``

+ To run any test(s) on a particular domain, replace localhost:8080 with desired domain name:

  ``mvn test -Dtest=FileMetadataIT -Ddataverse.test.baseurl='http://localhost:8080'``

To see the full list of tests used by the Docker option mentioned above, see :download:`run-test-suite.sh <../../../../conf/docker-aio/run-test-suite.sh>`.

Measuring Coverage of Integration Tests
---------------------------------------
Measuring the code coverage of integration tests with jacoco requires several steps:

- Instrument the WAR file. Using an approach similar to :download:`this script <../_static/util/instrument_war_jacoco.bash>` is probably preferable to instrumenting the WAR directly (at least until the ``nu.xom.UnicodeUtil.decompose`` method too large exceptions get sorted).
- Deploy the WAR file to a glassfish server with ``jacocoagent.jar`` in ``glassfish4/glassfish/lib/``
- Run integration tests as usual
- Use ``glassfish4/glassfish/domains/domain1/config/jacoco.exec`` to generate a report: ``java -jar ${JACOCO_HOME}/jacococli.jar report --classfiles ${DV_REPO}/target/classes --sourcefiles ${DV_REPO}/src/main/java --html ${DV_REPO}/target/coverage-it/ jacoco.exec``

The same approach could be used to measure code paths exercised in normal use (by substituting the "run integration tests" step).
There is obvious potential to improve automation of this process.

Load/Performance Testing
------------------------

Locust
~~~~~~

Load and performance testing is conducted on an as-needed basis but we're open to automating it. As of this writing Locust ( https://locust.io ) scripts at https://github.com/IQSS/dataverse-helper-scripts/tree/master/src/stress_tests have been used.

download-files.sh script
~~~~~~~~~~~~~~~~~~~~~~~~

One way of generating load is by downloading many files. You can download :download:`download-files.sh <../../../../tests/performance/download-files/download-files.sh>`, make it executable (``chmod 755``), and run it with ``--help``. You can use ``-b`` to specify the base URL of the Dataverse installation and ``-s`` to specify the number of seconds to wait between requests like this:

``./download-files.sh -b https://dev1.dataverse.org -s 2``

The script requires a file called ``files.txt`` to operate and database IDs for the files you want to download should each be on their own line.

The Phoenix Server
------------------

How the Phoenix Tests Work
~~~~~~~~~~~~~~~~~~~~~~~~~~

A server at http://phoenix.dataverse.org has been set up to test the latest code from the develop branch. Testing is done using chained builds of Jenkins jobs:

- A war file is built from the latest code in develop: https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-build-develop/
- The resulting war file is depoyed to the Phoenix server: https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-deploy-develop/
- REST Assured Tests are run across the wire from the Jenkins server to the Phoenix server:  https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-apitest-develop/

How to Run the Phoenix Tests
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Take a quick look at http://phoenix.dataverse.org to make sure the server is up and running Dataverse. If it's down, fix it.
- Log into Jenkins and click "Build Now" at https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-build-develop/
- Wait for all three chained Jenkins jobs to complete and note if they passed or failed. If you see a failure, open a GitHub issue or at least get the attention of some developers.

List of Tests Run Against the Phoenix Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We haven't thought much about a good way to publicly list the "IT" classes that are executed against the phoenix server. (Currently your best bet is to look at the ``Executing Maven`` line at the top of the "Full Log" of "Console Output" of ``phoenix.dataverse.org-apitest-develop`` Jenkins job mentioned above.) We endeavor to keep the list of tests in the "all-in-one" Docker environment described above in sync with the list of tests configured in Jenkins. That is to say, refer to :download:`run-test-suite.sh <../../../../conf/docker-aio/run-test-suite.sh>` mentioned in ``conf/docker-aio/readme.txt`` for the current list of IT tests that are expected to pass. Here's a dump of that file:

.. literalinclude:: ../../../../conf/docker-aio/run-test-suite.sh

Future Work
-----------

We'd like to make improvements to our automated testing. See also 'this thread from our mailing list <https://groups.google.com/forum/#!topic/dataverse-community/X8OrRWbPimA>'_ asking for ideas from the community, and discussion at 'this GitHub issue. <https://github.com/IQSS/dataverse/issues/2746>'_ 

Future Work on Unit Tests
~~~~~~~~~~~~~~~~~~~~~~~~~

- Review pull requests from @bencomp for ideas for approaches to testing: https://github.com/IQSS/dataverse/pulls?q=is%3Apr+author%3Abencomp
- Come up with a way to test commands: http://irclog.iq.harvard.edu/dataverse/2015-11-04#i_26750
- Test EJBs using Arquillian, embedded Glassfish, or similar. @bmckinney kicked the tires on Arquillian at https://github.com/bmckinney/bio-dataverse/commit/2f243b1db1ca704a42cd0a5de329083763b7c37a

Future Work on Integration Tests
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Automate testing of the Python client: https://github.com/IQSS/dataverse-client-python/issues/10
- Work with @leeper on testing the R client: https://github.com/IQSS/dataverse-client-r
- Review and attempt to implement "API Test Checklist" from @kcondon at https://docs.google.com/document/d/199Oq1YwQ4pYCguaeW48bIN28QAitSk63NbPYxJHCCAE/edit?usp=sharing
- Attempt to use @openscholar approach for running integration tests using Travis https://github.com/openscholar/openscholar/blob/SCHOLAR-3.x/.travis.yml (probably requires using Ubuntu rather than CentOS)
- Generate code coverage reports for **integration** tests: https://github.com/pkainulainen/maven-examples/issues/3 and http://www.petrikainulainen.net/programming/maven/creating-code-coverage-reports-for-unit-and-integration-tests-with-the-jacoco-maven-plugin/
- Consistent logging of API Tests. Show test name at the beginning and end and status codes returned.
- expected passing and known/expected failing integration tests: https://github.com/IQSS/dataverse/issues/4438

Browser-Based Testing
~~~~~~~~~~~~~~~~~~~~~

- Revisit Selenium/Open Sauce: https://github.com/IQSS/dataverse/commit/8a26404 and https://saucelabs.com/u/esodvn and https://saucelabs.com/u/wdjs and http://sauceio.com/index.php/2013/05/a-browser-matrix-widget-for-the-open-source-community/

Installation Testing
~~~~~~~~~~~~~~~~~~~~

- Run `vagrant up` on a server to test the installer: http://guides.dataverse.org/en/latest/developers/tools.html#vagrant . We haven't been able to get this working in Travis: https://travis-ci.org/IQSS/dataverse/builds/96292683 . Perhaps it would be possible to use AWS as a provider from Vagrant judging from https://circleci.com/gh/critical-alert/circleci-vagrant/6 .
- Work with @lwo to automate testing of https://github.com/IQSS/dataverse-puppet . Consider using Travis: https://github.com/IQSS/dataverse-puppet/issues/10
- Work with @donsizemore to automate testing of https://github.com/IQSS/dataverse-ansible with Travis or similar.

Future Work on Load/Performance Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Clean up and copy stress tests code, config, and docs into main repo from https://github.com/IQSS/dataverse-helper-scripts/tree/master/src/stress_tests
- Marcel Duran created a command-line wrapper for the WebPagetest API that can be used to test performance in your continuous integration pipeline (TAP, Jenkins, Travis-CI, etc): https://github.com/marcelduran/webpagetest-api/wiki/Test-Specs#jenkins-integration
- Create top-down checklist, building off the "API Test Coverage" spreadsheet at https://github.com/IQSS/dataverse/issues/3358#issuecomment-256400776

----

Previous: :doc:`version-control` | Next: :doc:`documentation`
