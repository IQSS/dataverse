============
Coding Style
============

Like all development teams, the `Dataverse  Project developers at IQSS <https://dataverse.org/about>`_ have their habits and styles when it comes to writing code. Let's attempt to get on the same page. :)

.. contents:: |toctitle|
	:local:

Java
----

Formatting Code
~~~~~~~~~~~~~~~

Tabs vs. Spaces
^^^^^^^^^^^^^^^

Don't use tabs. Use 4 spaces.

Braces Placement
^^^^^^^^^^^^^^^^

Place curly braces according to the style below, which is an example you can see from Netbeans.

.. code-block:: java

    public class ClassA {

        private String letters[] = new String[]{"A", "B"};

        public int meth(String text, int number) {
            BinaryOperator plus = (a, b) -> {
                return a + b;
            };
            if (text != null) {
                try {
                    meth("Some text", text.length());
                } catch (Throwable t) {
                } finally {
                }
            } else if (number >= 0) {
                text = number == 0 ? "empty" : "nonempty";
            }
            do {
                number = number + 1;
            } while (number < 2);
            for (int i = 1; i < 100; i++) {
                number = number + i;
            }
            while (number > 0) {
                number--;
            }
        }
    }

Format Code You Changed with Netbeans
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

IQSS has standardized on Netbeans. It is much appreciated when you format your code (but only the code you touched!) using the out-of-the-box Netbeans configuration. If you have created an entirely new Java class, you can just click Source -> Format. If you are adjusting code in an existing class, highlight the code you changed and then click Source -> Format. Keeping the "diff" in your pull requests small makes them easier to code review.

Checking Your Formatting With Checkstyle
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The easiest way to adopt the Dataverse Project coding style is to use Netbeans as your IDE, avoid change the default Netbeans formatting settings, and only reformat code you've changed, as described above.

If you do not use Netbeans, you are encouraged to check the formatting of your code using Checkstyle.

To check the entire project:

``mvn checkstyle:checkstyle``

To check a single file:

``mvn checkstyle:checkstyle -Dcheckstyle.includes=**\/SystemConfig*.java``

Logging
~~~~~~~

We have adopted a pattern where the top of every class file has a line like this::

    private static final Logger logger = Logger.getLogger(DatasetUtil.class.getCanonicalName());

Use this ``logger`` field with varying levels such as ``fine`` or ``info`` like this::

    logger.fine("will get thumbnail from dataset logo");

Generally speaking you should use ``fine`` for everything that you don't want to show up by default in the app server's log file. If you use a higher level such as ``info`` for common operations, you will probably hear complaints that your code is too "chatty" in the logs. These logging levels can be controlled at runtime both on your development machine and in production as explained in the :doc:`debugging` section.

When adding logging, do not simply add ``System.out.println()`` lines because the logging level cannot be controlled.

Avoid Hard-Coding Strings (Use Constants)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Special strings should be defined as public constants. For example, ``DatasetFieldConstant.java`` contains a field for "title" and it's used in many places in the code (try "Find Usages" in Netbeans). This is better than writing the string "title" in all those places.

Avoid Hard-Coding User-Facing Messaging in English
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There is an ongoing effort to translate the Dataverse Software into various languages. Look for "lang" or "languages" in the :doc:`/installation/config` section of the Installation Guide for details if you'd like to help or play around with this feature.

The translation effort is hampered if you hard code user-facing messages in English in the Java code. Put English strings in ``Bundle.properties`` and use ``BundleUtil`` to pull them out. This is especially important for messages that appear in the UI. We are aware that the API has many, many hard coded English strings in it. If you touch a method in the API and notice English strings, you are strongly encouraged to use that opportunity to move the English to ``Bundle.properties``.

Type Safety
~~~~~~~~~~~

If you just downloaded Netbeans and are using the out-of-the-box settings, you should be in pretty good shape. Unfortunately, the default configuration of Netbeans doesn't warn you about type-safety problems you may be inadvertently introducing into the code. To see these warnings, click Netbeans -> Preferences -> Editor -> Hints and check the following:

- "Raw Types" under "Standard Javac Warnings"

If you know of a way to easily share Netbeans configuration across a team, please get in touch.

Bash
----

Generally, Google's Shell Style Guide at https://google.github.io/styleguide/shell.xml seems to have good advice.

Formatting Code
~~~~~~~~~~~~~~~

Tabs vs. Spaces
^^^^^^^^^^^^^^^

Don't use tabs. Use 2 spaces.

shfmt from https://github.com/mvdan/sh seems like a decent way to enforce indentation of two spaces (i.e. ``shfmt -i 2 -w path/to/script.sh``) but be aware that it makes other changes.

Bike Shedding
-------------

What color should the `bike shed <https://en.wiktionary.org/wiki/bikeshedding>`_ be? :)

Come debate with us about coding style in this Google doc that has public comments enabled: https://docs.google.com/document/d/1KTd3FpM1BI3HlBofaZjMmBiQEJtFf11jiiGpQeJzy7A/edit?usp=sharing
