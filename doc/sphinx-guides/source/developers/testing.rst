=======
Testing
=======

In order to keep our codebase healthy, the Dataverse Project encourages developers to write automated tests in the form of unit tests and integration tests. We also welcome ideas for how to improve our automated testing.

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

Unit tests can be executed without runtime dependencies on PostgreSQL, Solr, or any other external system. They are the lowest level of testing and are executed constantly on developers' laptops as part of the build process and via continuous integration services in the cloud.

A unit test should execute an operation of your code in a controlled fashion. You must make an assertion of what the expected response gives back. It's important to test optimistic output and assertions (the "happy path"), as well as unexpected input that leads to failure conditions. Know how your program should handle anticipated errors/exceptions and confirm with your test(s) that it does so properly.

Unit Test Automation Overview
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We use a variety of tools to write, execute, and measure the code coverage of unit tests, including Maven, JUnit, Jacoco, GitHub, and Coveralls. We'll explain the role of each tool below, but here's an overview of what you can expect from the automation we've set up.

As you prepare to make a pull request, as described in the :doc:`version-control` section, you will be working on a new branch you create from the "develop" branch. Let's say your branch is called ``1012-private-url``. As you work, you are constantly invoking Maven to build the war file. When you do a "clean and build" in Netbeans, Maven runs all the unit tests (anything ending with ``Test.java``) and then runs the results through a tool called Jacoco that calculates code coverage. When you push your branch to GitHub and make a pull request, GitHub Actions runs Maven and Jacoco on your branch and pushes the results to Coveralls, which is a web service that tracks changes to code coverage over time. Note that we have configured Coveralls to not mark small decreases in code coverage as a failure. You can find the Coveralls reports at https://coveralls.io/github/IQSS/dataverse

The main takeaway should be that we care about unit testing enough to measure the changes to code coverage over time using automation. Now let's talk about how you can help keep our code coverage up by writing unit tests with JUnit.

Writing Unit Tests with JUnit
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are aware that there are newer testing tools such as TestNG, but we use `JUnit <http://junit.org>`_ because it's tried and true.
We support JUnit 5 based testing and require new tests written with it.
(Since Dataverse 6.0, we migrated all of our tests formerly based on JUnit 4.)

If writing tests is new to you, poke around existing unit tests which all end in ``Test.java`` and live under ``src/test``.
Each test is annotated with ``@Test`` and should have at least one assertion which specifies the expected result.
In Netbeans, you can run all the tests in it by clicking "Run" -> "Test File".
From the test file, you should be able to navigate to the code that's being tested by right-clicking on the file and clicking "Navigate" -> "Go to Test/Tested class".
Likewise, from the code, you should be able to use the same "Navigate" menu to go to the tests.

NOTE: Please remember when writing tests checking possibly localized outputs to check against ``en_US.UTF-8`` and ``UTC``
l10n strings!

Refactoring Code to Make It Unit-Testable
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Existing code is not necessarily written in a way that lends itself to easy testing. Generally speaking, it is difficult to write unit tests for both JSF "backing" beans (which end in ``Page.java``) and "service" beans (which end in ``Service.java``) because they require the database to be running in order to test them. If service beans can be exercised via API they can be tested with integration tests (described below) but a good technique for making the logic testable it to move code to "util beans" (which end in ``Util.java``) that operate on Plain Old Java Objects (POJOs). ``PrivateUrlUtil.java`` is a good example of moving logic from ``PrivateUrlServiceBean.java`` to a "util" bean to make the code testable.

Parameterized Tests
^^^^^^^^^^^^^^^^^^^

Often times you will want to test a method multiple times with similar values.
In order to avoid test bloat (writing a test for every data combination),
JUnit offers Data-driven unit tests. This allows a test to be run for each set
of defined data values.

JUnit 5 offers great parameterized testing. Some guidance how to write those:

- https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests
- https://www.baeldung.com/parameterized-tests-junit-5
- https://blog.codefx.org/libraries/junit-5-parameterized-tests/
- See also many examples in our codebase.

Note that JUnit 5 also offers support for custom test parameter resolvers. This enables keeping tests cleaner,
as preparation might happen within some extension and the test code is more focused on the actual testing.
See https://junit.org/junit5/docs/current/user-guide/#extensions-parameter-resolution for more information.

JUnit 5 Test Helper Extensions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Our codebase provides little helpers to ease dealing with state during tests.
Some tests might need to change something which should be restored after the test ran.

For unit tests, the most interesting part is to set a JVM setting just for the current test.
Please use the ``@JvmSetting(key = JvmSettings.XXX, value = "")`` annotation on a test method or
a test class to set and clear the property automatically.

To set arbitrary system properties for the current test, a similar extension
``@SystemProperty(key = "", value = "")`` has been added.

Both extensions will ensure the global state of system properties is non-interfering for
test executions. Tests using these extensions will be executed in serial.

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

You should be aware that some unit tests have been deemed "non-essential" and have been annotated with ``@Tag(Tags.NOT_ESSENTIAL_UNITTESTS)`` and are excluded from the "dev" Maven profile, which is the default profile.
All unit tests (that have not been annotated with ``@Disable``), including these non-essential tests, are run from continuous integration systems such as Jenkins and GitHub Actions with the following ``mvn`` command that invokes a non-default profile:

``mvn test -P all-unit-tests``

Generally speaking, unit tests have been flagged as non-essential because they are slow or because they require an Internet connection.
You should not feel obligated to run these tests continuously but you can use the ``mvn`` command above to run them.
To iterate on the unit test in Netbeans and execute it with "Run -> Test File", you must temporarily comment out the annotation flagging the test as non-essential.

Integration Tests
-----------------

Unit tests are fantastic for low level testing of logic but aren't especially real-world-applicable because they do not exercise the Dataverse Software as it runs in production with a database and other runtime dependencies. We test in-depth by also writing integration tests to exercise a running system.

Unfortunately, the term "integration tests" can mean different things to
different people. For our purposes, an integration test can have two flavors:

1. Be an API Test:

   - Exercise the Dataverse Software APIs.
   - Running not automatically on developers' laptops.
   - Operate on a Dataverse installation that is running and able to talk to both PostgreSQL and Solr.
   - Written using REST Assured.

2. Be a `Testcontainers <https://testcontainers.org>`__ Test:

   - Operates any dependencies via the Testcontainers API, using containers.
   - Written as a JUnit test, using all things necessary to test.
   - Makes use of the Testcontainers framework.
   - Able to run anywhere having Docker around (podman support under construction).

Running the Full API Test Suite Using EC2
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Prerequisite:** To run the API test suite in an EC2 instance you should first follow the steps in the :doc:`deployment` section to get set up with the AWS binary to launch EC2 instances. If you're here because you just want to spin up a branch, you'll still want to follow the AWS deployment setup steps, but may find the `ec2-create README.md <https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible/blob/master/ec2/README.md>`_ Quick Start section helpful.

You may always retrieve a current copy of the ec2-create-instance.sh script and accompanying group_var.yml file from the `dataverse-ansible repo <https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible/>`_. Since we want to run the test suite, let's grab the group_vars used by Jenkins:

- `ec2-create-instance.sh <https://raw.githubusercontent.com/GlobalDataverseCommunityConsortium/dataverse-ansible/master/ec2/ec2-create-instance.sh>`_
- `jenkins.yml <https://raw.githubusercontent.com/GlobalDataverseCommunityConsortium/dataverse-ansible/master/tests/group_vars/jenkins.yml>`_

Edit ``jenkins.yml`` to set the desired GitHub repo and branch, and to adjust any other options to meet your needs:

- ``dataverse_repo: https://github.com/IQSS/dataverse.git``
- ``dataverse_branch: develop``
- ``dataverse.api.test_suite: true``
- ``dataverse.unittests.enabled: true``
- ``dataverse.sampledata.enabled: true``

If you wish, you may pass the script a ``-l`` flag with a local relative path in which the script will `copy various logs <https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible/blob/master/ec2/ec2-create-instance.sh#L185>`_ at the end of the test suite for your review.

Finally, run the script:

.. code-block:: bash

  $ ./ec2-create-instance.sh -g jenkins.yml -l log_dir

Running the full API test suite using Docker
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To run the full suite of integration tests on your laptop, running Dataverse and its dependencies in Docker, as explained in the :doc:`/container/dev-usage` section of the Container Guide.

Alternatively, you can run tests against the app server running on your laptop by following the "getting set up" steps below.

Getting Set Up to Run REST Assured Tests
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Unit tests are run automatically on every build, but dev environments and servers require special setup to run REST Assured tests. In short, the Dataverse Software needs to be placed into an insecure mode that allows arbitrary users and datasets to be created and destroyed. This differs greatly from the out-of-the-box behavior of the Dataverse Software, which we strive to keep secure for sysadmins installing the software for their institutions in a production environment.

The :doc:`dev-environment` section currently refers developers here for advice on getting set up to run REST Assured tests, but we'd like to add some sort of "dev" flag to the installer to put the Dataverse Software in "insecure" mode, with lots of scary warnings that this dev mode should not be used in production.

The instructions below assume a relatively static dev environment on a Mac. There is a newer "all in one" Docker-based approach documented in the :doc:`/developers/containers` section under "Docker" that you may like to play with as well.

The Burrito Key
^^^^^^^^^^^^^^^

For reasons that have been lost to the mists of time, the Dataverse Software really wants you to to have a burrito. Specifically, if you're trying to run REST Assured tests and see the error "Dataverse config issue: No API key defined for built in user management", you must run the following curl command (or make an equivalent change to your database):

``curl -X PUT -d 'burrito' http://localhost:8080/api/admin/settings/BuiltinUsers.KEY``

Without this "burrito" key in place, REST Assured will not be able to create users. We create users to create objects we want to test, such as Dataverse collections, datasets, and files.

Root Dataverse Collection Permissions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In your browser, log in as dataverseAdmin (password: admin) and click the "Edit" button for your root Dataverse collection. Navigate to Permissions, then the Edit Access button. Under "Who can add to this Dataverse collection?" choose "Anyone with a Dataverse installation account can add sub Dataverse collections and datasets" if it isn't set to this already.

Alternatively, this same step can be done with this script: ``scripts/search/tests/grant-authusers-add-on-root``

Publish Root Dataverse Collection
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The root Dataverse collection must be published for some of the REST Assured tests to run.

dataverse.siteUrl
^^^^^^^^^^^^^^^^^

When run locally (as opposed to a remote server), some of the REST Assured tests require the ``dataverse.siteUrl`` JVM option to be set to ``http://localhost:8080``. See :ref:`jvm-options` section in the Installation Guide for advice changing JVM options. First you should check to check your JVM options with:

``./asadmin list-jvm-options | egrep 'dataverse|doi'``

If ``dataverse.siteUrl`` is absent, you can add it with:

``./asadmin create-jvm-options "-Ddataverse.siteUrl=http\://localhost\:8080"``

Identifier Generation
^^^^^^^^^^^^^^^^^^^^^

``DatasetsIT.java`` exercises the feature where the "identifier" of a DOI can be a digit and requires a sequence to be added to your database.  See ``:IdentifierGenerationStyle`` under the :doc:`/installation/config` section for adding this sequence to your installation of PostgreSQL.


Writing API Tests with REST Assured
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

To execute existing integration tests on your local Dataverse installation, a helpful command line tool to use is `Maven <http://maven.apache.org/ref/3.1.0/maven-embedder/cli.html>`_. You should have Maven installed as per the `Development Environment <http://guides.dataverse.org/en/latest/developers/dev-environment.html>`_ guide, but if not it’s easily done via Homebrew: ``brew install maven``.

Once installed, you may run commands with ``mvn [options] [<goal(s)>] [<phase(s)>]``.

+ If you want to run just one particular API test, it’s as easy as you think:

  ``mvn test -Dtest=FileRecordJobIT``

+ To run more than one test at a time, separate by commas:

  ``mvn test -Dtest=FileRecordJobIT,ConfirmEmailIT``

+ To run any test(s) on a particular domain, replace localhost:8080 with desired domain name:

  ``mvn test -Dtest=FileMetadataIT -Ddataverse.test.baseurl='http://localhost:8080'``

If you are adding a new test class, be sure to add it to :download:`tests/integration-tests.txt <../../../../tests/integration-tests.txt>` so that our automated testing knows about it.


Writing and Using a Testcontainers Test
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Most scenarios of integration testing involve having dependent services running.
This is where `Testcontainers <https://www.testcontainers.org>`__ kicks in by
providing a JUnit interface to drive them before and after executing your tests.

Test scenarios are endless. Some examples are migration scripts, persistance,
storage adapters etc.

To run a test with Testcontainers, you will need to write a JUnit 5 test.
`The upstream project provides some documentation about this. <https://www.testcontainers.org/test_framework_integration/junit_5>`_

Please make sure to:

1. End your test class with ``IT``
2. Provide a ``@Tag("testcontainers")`` to be picked up during testing.

.. code:: java

   /** A very minimal example for a Testcontainers integration test class. */
   @Testcontainers
   @Tag("testcontainers")
   class MyExampleIT { /* ... */ }

If using upstream Modules, e.g. for PostgreSQL or similar, you will need to add
a dependency to ``pom.xml`` if not present. `See the PostgreSQL module example. <https://www.testcontainers.org/modules/databases/postgres/>`_

To run these tests, simply call out to Maven:

.. code::

	 mvn -P tc verify

.. note::

	 1. Remember to have Docker ready to serve or tests will fail.
	 2. This will not run any unit tests or API tests.

Measuring Coverage of Integration Tests
---------------------------------------

Measuring the code coverage of integration tests with Jacoco requires several steps. In order to make these steps clear we'll use "/usr/local/payara6" as the Payara directory and "dataverse" as the Payara Unix user.

Please note that this was tested under Glassfish 4 but it is hoped that the same steps will work with Payara.

Add jacocoagent.jar to Payara
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to get code coverage reports out of Payara we'll be adding jacocoagent.jar to the Payara "lib" directory.

First, we need to download Jacoco. Look in pom.xml to determine which version of Jacoco we are using. As of this writing we are using 0.8.1 so in the example below we download the Jacoco zip from https://github.com/jacoco/jacoco/releases/tag/v0.8.1

Note that we are running the following commands as the user "dataverse". In short, we stop Payara, add the Jacoco jar file, and start up Payara again.

.. code-block:: bash

  su - dataverse
  cd /home/dataverse
  mkdir -p local/jacoco-0.8.1
  cd local/jacoco-0.8.1
  wget https://github.com/jacoco/jacoco/releases/download/v0.8.1/jacoco-0.8.1.zip
  unzip jacoco-0.8.1.zip
  /usr/local/payara6/bin/asadmin stop-domain
  cp /home/dataverse/local/jacoco-0.8.1/lib/jacocoagent.jar /usr/local/payara6/glassfish/lib
  /usr/local/payara6/bin/asadmin start-domain

Add jacococli.jar to the WAR File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As the "dataverse" user download :download:`instrument_war_jacoco.bash <../_static/util/instrument_war_jacoco.bash>` (or skip ahead to the "git clone" step to get the script that way) and give it two arguments:

- path to your pristine WAR file
- path to the new WAR file the script will create with jacococli.jar in it

.. code-block:: bash

  ./instrument_war_jacoco.bash dataverse.war dataverse-jacoco.war

Deploy the Instrumented WAR File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please note that you'll want to undeploy the old WAR file first, if necessary.

Run this as the "dataverse" user.

.. code-block:: bash

  /usr/local/payara6/bin/asadmin deploy dataverse-jacoco.war

Note that after deployment the file "/usr/local/payara6/glassfish/domains/domain1/config/jacoco.exec" exists and is empty.

Run Integration Tests
~~~~~~~~~~~~~~~~~~~~~

Note that even though you see "docker-aio" in the command below, we assume you are not necessarily running the test suite within Docker. (Some day we'll probably move this script to another directory.) For this reason, we pass the URL with the normal port (8080) that app servers run on to the ``run-test-suite.sh`` script.

Note that "/usr/local/payara6/glassfish/domains/domain1/config/jacoco.exec" will become non-empty after you stop and start Payara. You must stop and start Payara before every run of the integration test suite.

.. code-block:: bash

  /usr/local/payara6/bin/asadmin stop-domain
  /usr/local/payara6/bin/asadmin start-domain
  git clone https://github.com/IQSS/dataverse.git
  cd dataverse
  conf/docker-aio/run-test-suite.sh http://localhost:8080

(As an aside, you are not limited to API tests for the purposes of learning which code paths are being executed. You could click around the GUI, for example. Jacoco doesn't know or care how you exercise the application.)

Create Code Coverage Report
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Run these commands as the "dataverse" user. The ``cd dataverse`` means that you should change to the directory where you cloned the "dataverse" git repo.

.. code-block:: bash

  cd dataverse
  java -jar /home/dataverse/local/jacoco-0.8.1/lib/jacococli.jar report --classfiles target/classes --sourcefiles src/main/java --html target/coverage-it/ /usr/local/payara6/glassfish/domains/domain1/config/jacoco.exec

Read Code Coverage Report
~~~~~~~~~~~~~~~~~~~~~~~~~

target/coverage-it/index.html is the place to start reading the code coverage report you just created.

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

Continuous Integration
----------------------

The Dataverse Project currently makes use of two Continuous Integration platforms, Jenkins and GitHub Actions.

Our Jenkins config is a work in progress and may be viewed at https://github.com/IQSS/dataverse-jenkins/ A corresponding GitHub webhook is required. Build output is viewable at https://jenkins.dataverse.org/

GitHub Actions jobs can be found in ``.github/workflows``.

As always, pull requests to improve our continuous integration configurations are welcome.

Enhance build time by caching dependencies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In the future, CI builds in ephemeral build environments and Docker builds can benefit from caching all dependencies and plugins.
As the Dataverse Project is a huge project, build times can be enhanced by avoiding re-downloading everything when the Maven POM is unchanged.
To seed the cache, use the following Maven goal before using Maven in (optional) offline mode in your scripts:

.. code:: shell

  mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies``
  mvn -o package -DskipTests

The example above builds the WAR file without running any tests. For other scenarios: not using offline mode allows
Maven to download more dynamic dependencies, which are not easy to track, like Surefire Plugins. Overall downloads will
reduced anyway.

You will obviously have to utilize caching functionality of your CI service or do proper Docker layering.

The Phoenix Server
~~~~~~~~~~~~~~~~~~

How the Phoenix Tests Work
^^^^^^^^^^^^^^^^^^^^^^^^^^

A server at http://phoenix.dataverse.org has been set up to test the latest code from the develop branch. Testing is done using chained builds of Jenkins jobs:

- A war file is built from the latest code in develop: https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-build-develop/
- The resulting war file is depoyed to the Phoenix server: https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-deploy-develop/
- REST Assured Tests are run across the wire from the Jenkins server to the Phoenix server:  https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-apitest-develop/

How to Run the Phoenix Tests
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Take a quick look at http://phoenix.dataverse.org to make sure the server is up and running Dataverse. If it's down, fix it.
- Log into Jenkins and click "Build Now" at https://build.hmdc.harvard.edu:8443/job/phoenix.dataverse.org-build-develop/
- Wait for all three chained Jenkins jobs to complete and note if they passed or failed. If you see a failure, open a GitHub issue or at least get the attention of some developers.

Accessibility Testing
---------------------

Accessibility Policy
~~~~~~~~~~~~~~~~~~~~

The Dataverse Project aims to improve the user experience for those with disabilities, and are in the process of following the recommendations of the `Harvard University Digital Accessibility Policy <https://accessibility.huit.harvard.edu/digital-accessibility-policy>`__,  which use the Worldwide Web Consortium’s Web Content Accessibility Guidelines version 2.1, Level AA Conformance (WCAG 2.1 Level AA) as the standard.

To report an accessibility issue with the Dataverse Software, you can create a new issue in our GitHub repo at: https://github.com/IQSS/dataverse/issues/

Accessibility Tools
~~~~~~~~~~~~~~~~~~~

Our development process will incorporate automated testing provided by tools like `SiteImprove <https://siteimprove.com/en-us/accessibility/>`__ and `Accessibility Management Platform (AMP) <https://www.levelaccess.com/solutions/software/amp/>`__ from Level Access, to run accessibility reports for the application.

Developers who contribute front-end UI code are responsible for understanding the requirements of this standard and the tools and methods for securing conformance with it.

There are browser developer tools such as the `Wave toolbar <https://wave.webaim.org/extension/>`__ by WebAIM (available for Chrome, Firefox) and the `Siteimprove Accessibility Checker <https://siteimprove.com/en-us/core-platform/integrations/browser-extensions/>`__  (available for Chrome, Firefox) that will generate reports for a single page. It is required that developers utilize these tools to catch any accessibility issues with pages or features that are being added to the application UI.

Future Work
-----------

We'd like to make improvements to our automated testing. See also 'this thread from our mailing list <https://groups.google.com/forum/#!topic/dataverse-community/X8OrRWbPimA>'_ asking for ideas from the community, and discussion at 'this GitHub issue. <https://github.com/IQSS/dataverse/issues/2746>'_

Future Work on Unit Tests
~~~~~~~~~~~~~~~~~~~~~~~~~

- Review pull requests from @bencomp for ideas for approaches to testing: https://github.com/IQSS/dataverse/pulls?q=is%3Apr+author%3Abencomp
- Come up with a way to test commands: http://irclog.iq.harvard.edu/dataverse/2015-11-04#i_26750
- Test EJBs using Arquillian, embedded app servers, or similar. @bmckinney kicked the tires on Arquillian at https://github.com/bmckinney/bio-dataverse/commit/2f243b1db1ca704a42cd0a5de329083763b7c37a

Future Work on Integration Tests
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Automate testing of dataverse-client-python: https://github.com/IQSS/dataverse-client-python/issues/10
- Work with @leeper on testing the R client: https://github.com/IQSS/dataverse-client-r
- Review and attempt to implement "API Test Checklist" from @kcondon at https://docs.google.com/document/d/199Oq1YwQ4pYCguaeW48bIN28QAitSk63NbPYxJHCCAE/edit?usp=sharing
- Generate code coverage reports for **integration** tests: https://github.com/pkainulainen/maven-examples/issues/3 and http://www.petrikainulainen.net/programming/maven/creating-code-coverage-reports-for-unit-and-integration-tests-with-the-jacoco-maven-plugin/
- Consistent logging of API Tests. Show test name at the beginning and end and status codes returned.
- expected passing and known/expected failing integration tests: https://github.com/IQSS/dataverse/issues/4438

Browser-Based Testing
~~~~~~~~~~~~~~~~~~~~~

- Revisit Selenium/Open Sauce: https://github.com/IQSS/dataverse/commit/8a26404 and https://saucelabs.com/u/esodvn and https://saucelabs.com/u/wdjs and http://sauceio.com/index.php/2013/05/a-browser-matrix-widget-for-the-open-source-community/

Installation Testing
~~~~~~~~~~~~~~~~~~~~

- Work with @donsizemore to automate testing of https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible

Future Work on Load/Performance Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Clean up and copy stress tests code, config, and docs into main repo from https://github.com/IQSS/dataverse-helper-scripts/tree/master/src/stress_tests
- Marcel Duran created a command-line wrapper for the WebPagetest API that can be used to test performance in your continuous integration pipeline (TAP, Jenkins, etc.): https://github.com/marcelduran/webpagetest-api/wiki/Test-Specs#jenkins-integration
- Create top-down checklist, building off the "API Test Coverage" spreadsheet at https://github.com/IQSS/dataverse/issues/3358#issuecomment-256400776

Future Work on Accessibility Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

- Using https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible and hooks available from accessibility testing tools, automate the running of accessibility tools on PRs so that developers will receive quicker feedback on proposed code changes that reduce the accessibility of the application.

----

Previous: :doc:`sql-upgrade-scripts` | Next: :doc:`documentation`
