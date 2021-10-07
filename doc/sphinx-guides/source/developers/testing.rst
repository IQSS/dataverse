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

Locale
------
Do you use a locale different than ``en_US.UTF-8`` on your development machine? Are you in a different timezone
than Harvard (Eastern Time)? You might experience issues while running tests that were written with these settings
in mind. The Maven  ``pom.xml`` tries to handle this for you by setting the locale to ``en_US.UTF-8`` and timezone
``UTC``, but more, not yet discovered building or testing problems might lurk in the shadows.

Unit Tests
----------

Creating unit tests for your code is a helpful way to test what you've built piece by piece.

Unit tests can be executed without runtime dependencies on PostgreSQL, Solr, or any other external system. They are the lowest level of testing and are executed constantly on developers' laptops as part of the build process and via continous integration services in the cloud.

A unit test should execute an operation of your code in a controlled fashion. You must make an assertion of what the expected response gives back. It's important to test optimistic output and assertions (the "happy path"), as well as unexpected input that leads to failure conditions. Know how your program should handle anticipated errors/exceptions and confirm with your test(s) that it does so properly. 

Unit Test Automation Overview
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We use a variety of tools to write, execute, and measure the code coverage of unit tests, including Maven, JUnit, Jacoco, GitHub, Travis, and Coveralls.

As you prepare to make a pull request, as described in the :doc:`version-control` section, you will be working on a new branch you create from the "develop" branch. Let's say your branch is called ``{USERNAME}_1012_private_url``. As you work, you are constantly invoking Maven to build the war file. When you do a "clean and build" in Netbeans, Maven runs all the unit tests (anything ending with ``Test.java``) and the runs the results through a tool called Jacoco that calculates code coverage. When you push your branch to GitHub and make a pull request which is then merged, a web service called Jenkins CI runs Maven and Jacoco on your branch and pushes the results to Coveralls, which is a web service that tracks changes to code coverage over time.

To make this more concrete, observe https://coveralls.io/github/CeON/dataverse you can see the coverage changes as new pull request is merged.

The main takeaway should be that we care about unit testing enough to measure the changes to code coverage over time using automation. Now let's talk about how you can help keep our code coverage up by writing unit tests with JUnit.

Writing Unit Tests with JUnit
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are aware that there are newer testing tools such as TestNG, but we use `JUnit <http://junit.org>`_ because it's tried and true.

If writing tests is new to you, poke around existing unit tests which all end in ``Test.java`` and live under ``src/test``. Each test is annotated with ``@Test`` and should have at least one assertion which specifies the expected result. In Netbeans, you can run all the tests in it by clicking "Run" -> "Test File". From the test file, you should be able to navigate to the code that's being tested by right-clicking on the file and clicking "Navigate" -> "Go to Test/Tested class". Likewise, from the code, you should be able to use the same "Navigate" menu to go to the tests.

We are also using BDD which you can read more about for example here - https://en.wikipedia.org/wiki/Behavior-driven_development

NOTE: Please remember when writing tests checking possibly localized outputs to check against ``en_US.UTF-8`` and ``UTC``
l10n strings!

Parameterized Tests and JUnit Theories
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Often times you will want to test a method multiple times with similar values. In order to avoid test bloat (writing a test for every data combination), JUnit offers Data-driven unit tests with ``Parameterized.class`` and ``Theories.class``. This allows a test to be run for each set of defined data values. For reference, take a look at issue https://github.com/IQSS/dataverse/issues/5619 .

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

You should be aware that some unit tests have been deemed "non-essential" and have been annotated with ``@Category(NonEssentialTests.class)`` and are excluded from the "dev" Maven profile, which is the default profile. All unit tests (that have not been annotated with ``@Ignore``), including these non-essential tests, are run from continuous integration systems such as Jenkins with the following ``mvn`` command that invokes a non-default profile:

``mvn test -P all-unit-tests``

Generally speaking, unit tests have been flagged as non-essential because they are slow or because they require an Internet connection. You should not feel obligated to run these tests continuously but you can use this ``mvn`` command to run them. To iterate on the unit test in Netbeans and execute it with "Run -> Test File", you must temporarily comment out the annotation flagging the test as non-essential.

Integration Tests
-----------------

For integration testing we are using Arquillian, the example test you can look at is called ``ArquillianExampleTest``.

Database
~~~~~~~~

In order to configure the database for tests you need to create user and database that matches the info from ``glassfish.properties`` file.

If you want to edit information contained in the file you need to move it to ~/.dataverse and then you can configure desired values.

Docker
~~~~~~

We are also using "Test containers" so there is no need for Solr manual setup in tests.

In order to make it run you need to have docker installed. Then user must be added to docker group which can be done with following command::

    sudo usermod -aG docker {USERNAME}

First test run downloads docker images so it can take some time for test to be completed.

----

Previous: :doc:`sql-upgrade-scripts` | Next: :doc:`documentation`
