============
Coding Style
============

Java
----

Formatting Code
~~~~~~~~~~~~~~~

NetBeans
^^^^^^^^

We highly recommend using Netbeans as a text editor. This makes it significantly easier to commit code that is well formatted and type safe. By default, NetBeans will warn you about some things, but if you want better warnings and code formatting, download this##################TODO ADD FILE######################### zip file, and import it to your nNtBeans settings by going to options, import, and selecting the file. These settings will cause NetBeans to automatically format code that you've worked on whenever you save, point out many common problems in code, and make sure that all indentation uses spaces.

Alternatively, you can set the warnings yourself. To do so, go to tools-> options-> editor -> hints, and turn on the following
    - Braces
    - Error Fixes
    - JDK Migration Support
    - Probable Bugs
    - Standard Javac Warnings


Not NetBeans
^^^^^^^^^^^^

If you are not going to use NetBeans, here are code style tips in order to be consistent with the rest of the project.
- Use 4 spaces for indentation (not tabs)
- Make sure your code is type safe.
- Make sure not to use raw classes
- Use diamond interface when applicable
- Use javadoc and comments to help explain what your code is doing
- Try to make your code look roughly like the following sample

For an example code that follows our styleguide, see this
############TODO INSERT IMAGE################################

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

Bike Shedding
-------------

What color should the `bike shed <https://en.wiktionary.org/wiki/bikeshedding>`_ be? :)

Come debate with us about coding style in this Google doc that has public comments enabled: https://docs.google.com/document/d/1KTd3FpM1BI3HlBofaZjMmBiQEJtFf11jiiGpQeJzy7A/edit?usp=sharing

----

Previous: :doc:`debugging` | Next: :doc:`making-releases`
