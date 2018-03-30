============
Coding Style
============

Like all development teams, the `Dataverse developers at IQSS <https://dataverse.org/about>`_ have their habits and styles when it comes to writing code. Let's attempt to get on the same page. :)

.. contents:: |toctitle|
	:local:

Java
----

Formatting Code
~~~~~~~~~~~~~~~

Tabs vs. Spaces
^^^^^^^^^^^^^^^

Don't use tabs. Use spaces.

Format Code You Changed with Netbeans
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As you probably gathered from the :doc:`dev-environment` section, IQSS has standardized on Netbeans. It is much appreciated when you format your code (but only the code you touched!) using the out-of-the-box Netbeans configuration. If you have created an entirely new Java class, you can just click Source -> Format. If you are adjusting code in an existing class, highlight the code you changed and then click Source -> Format. Keeping the "diff" in your pull requests small makes them easier to code review.

We would like to someday automate the detection and possibly correction of code that hasn't been formatted using our house style (the default Netbeans style). We've heard that https://maven.apache.org/plugins/maven-checkstyle-plugin/ can do this but we would be happy to see a pull request in this area, especially if it also hooks up to our builds at https://travis-ci.org/IQSS/dataverse .

Logging
~~~~~~~

We have adopted a pattern where the top of every class file has a line like this::

    private static final Logger logger = Logger.getLogger(DatasetUtil.class.getCanonicalName());

Use this ``logger`` field with varying levels such as ``fine`` or ``info`` like this::

    logger.fine("will get thumbnail from dataset logo");

Generally speaking you should use ``fine`` for everything that you don't want to show up in Glassfish's ``server.log`` file by default. If you use a higher level such as ``info`` for common operations, you will probably hear complaints that your code is too "chatty" in the logs. These logging levels can be controlled at runtime both on your development machine and in production as explained in the :doc:`debugging` section.

When adding logging, do not simply add ``System.out.println()`` lines because the logging level cannot be controlled.

Avoid Hard-Coding Strings
~~~~~~~~~~~~~~~~~~~~~~~~~

Special strings should be defined as public constants. For example, ``DatasetFieldConstant.java`` contains a field for "title" and it's used in many places in the code (try "Find Usages" in Netbeans). This is better than writing the string "title" in all those places.

Type Safety
~~~~~~~~~~~

If you just downloaded Netbeans and are using the out-of-the-box settings, you should be in pretty good shape. Unfortunately, the default configuration of Netbeans doesn't warn you about type-safety problems you may be inadvertently introducing into the code. To see these warnings, click Netbeans -> Preferences -> Editor -> Hints and check the following:

- "Raw Types" under "Standard Javac Warnings"

If you know of a way to easily share Netbeans configuration across a team, please get in touch.


Bike Shedding
-------------

What color should the `bike shed <https://en.wiktionary.org/wiki/bikeshedding>`_ be? :)

Come debate with us about coding style in this Google doc that has public comments enabled: https://docs.google.com/document/d/1KTd3FpM1BI3HlBofaZjMmBiQEJtFf11jiiGpQeJzy7A/edit?usp=sharing

----

Previous: :doc:`debugging` | Next: :doc:`containers`
