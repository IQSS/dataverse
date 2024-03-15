=====================
Dependency Management
=====================

.. contents:: |toctitle|
    :local:

Introduction
------------

As explained under :ref:`core-technologies`, the Dataverse Software is a Jakarta EE 8 based application that uses a lot of additional libraries for
special purposes. This includes support for the SWORD API, S3 storage, and many other features.

Besides the code that glues together individual pieces, any developer needs to describe dependencies used within the
Maven-based build system. As is familiar to any Maven user, this happens inside the "Project Object Model" (POM) file, ``pom.xml``.

Recursive and convergent dependency resolution makes dependency management with Maven quite easy, but sometimes, in
projects with many complex dependencies like the Dataverse Software, you have to help Maven make the right choices.

Maven can foster good development practices by enabling modulithic (modular monolithic) architecture: splitting
functionalities into different Maven submodules while expressing dependencies between them. But there's more: the
parent-child model allows you to create consistent dependency versioning (see below) within children.


Terms
-----

As a developer, you should familiarize yourself with the following terms:

- **Direct dependencies**: things *you use* yourself in your own code for the Dataverse Software.
- **Transitive dependencies**: things *others use* for things you use, pulled in recursively.
  See also: `Maven docs <https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Transitive_Dependencies>`_.

  .. graphviz::

    digraph {
        rankdir="LR";
        node [fontsize=10]

        yc [label="Your Code"]
        da [label="Direct Dependency A"]
        db [label="Direct Dependency B"]
        ta [label="Transitive Dependency TA"]
        tb [label="Transitive Dependency TB"]
        tc [label="Transitive Dependency TC"]
        dtz [label="Direct/Transitive Dependency Z"]

        yc -> da -> ta;
        yc -> db -> tc;
        da -> tb -> tc;
        db -> dtz;
        yc -> dtz;
    }

- **Project Object Model** (POM): the basic XML file unit to describe a Maven-based project.
- **Bill Of Materials** (BOM): larger projects like Payara, Amazon SDK etc. provide lists of their direct dependencies.
  This comes in handy when adding these dependencies (transitive for us) as direct dependencies, see below.

  .. graphviz::

    digraph {
        rankdir="TD";
        node [fontsize=10]
        edge [fontsize=8]

        msp [label="Maven Super POM"]
        sp  [label="Your POM"]
        bom [label="Some BOM"]
        td  [label="Direct & Transitive\nDependency"]

        msp -> sp [label="inherit", dir="back"];
        bom -> sp [label="import", dir="back"];
        bom -> td [label="depend on"];
        sp -> td [label="depend on\n(same version)", constraint=false];
    }

- **Parent POM**, **Super POM**: any project may be a child of a parent.

  Project silently inherit from a "super POM", which is the global Maven standard parent POM.
  Children may also be aggregated by a parent (without them knowing) for convenient builds of larger projects.

  .. graphviz::

    digraph {
        rankdir="TD";
        node [fontsize=10]
        edge [fontsize=8]

        msp [label="Maven Super POM"]
        ap  [label="Any POM"]
        msp -> ap [label="inherit", dir="back"];

        pp [label="Parent 1 POM"]
        cp1 [label="Submodule 1 POM"]
        cp2 [label="Submodule 2 POM"]

        msp -> pp [label="inherit", dir="back", constraint=false];
        pp -> cp1 [label="aggregate"];
        pp -> cp2 [label="aggregate"];
    }

  Children may inherit dependencies, properties, settings, plugins etc. from the parent (making it possible to share
  common ground). Both approaches may be combined. Children may import as many BOMs as they want, but can have only a
  single parent to inherit from at a time.

  .. graphviz::

    digraph {
        rankdir="TD";
        node [fontsize=10]
        edge [fontsize=8]

        msp [label="Maven Super POM"]
        pp  [label="Parent POM"]
        cp1 [label="Submodule 1 POM"]
        cp2 [label="Submodule 2 POM"]

        msp -> pp [label="inherit", dir="back", constraint=false];
        pp -> cp1 [label="aggregate"];
        pp -> cp2 [label="aggregate"];
        cp1 -> pp [label="inherit"];
        cp2 -> pp [label="inherit"];

        d [label="Dependency"]
        pp -> d [label="depends on"]
        cp1 -> d [label="inherit:\ndepends on", style=dashed];
        cp2 -> d [label="inherit:\ndepends on", style=dashed];
    }

- **Modules**: when using parents and children, these are called "modules" officially, each having their own POM.

  Using modules allows bundling different aspects of (Dataverse) software in their own domains, with their own
  behavior, dependencies etc. Parent modules allow for sharing of common settings, properties, dependencies and more.
  Submodules may also be used as parent modules for a lower level of submodules.

  Maven modules within the same software project may also depend on each other, allowing to create complex structures
  of packages and projects. Each module may be released on their own (e. g. on Maven Central) and other projects may
  rely on and reuse them. This is especially useful for parent POMs: they may be reused as BOMs or to share a standard
  between independent software projects.

  Maven modules should not be confused with the `Java Platform Module System (JPMS) <https://en.wikipedia.org/wiki/Java_Platform_Module_System>`_ introduced in Java 9 under Project Jigsaw.

Direct dependencies
-------------------

Within the POM, any direct dependencies reside within the ``<dependencies>`` tag:

.. code:: xml

    <dependencies>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>example</artifactId>
            <version>1.1.0</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>


Anytime you add a ``<dependency>``, Maven will try to fetch it from defined/configured repositories and use it
within the build lifecycle. You have to define a ``<version>`` (note exception below), but ``<scope>`` is optional for
``compile``. (See `Maven docs: Dep. Scope <https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope>`_)

During fetching, Maven will analyze all transitive dependencies (see graph above) and, if necessary, fetch those too.
Everything downloaded once is cached locally by default, so nothing needs to be fetched again and again, as long as the
dependency definition does not change.

**Rules to follow:**

1. You should only use direct dependencies for **things you are actually using** in your code.
2. When declaring a direct dependency with its **version** managed by ``<dependencyManagement>``, a BOM or parent POM, you
   may not provide one unless you want to explicitly override!
3. **Clean up** direct dependencies no longer in use. It will bloat the deployment package otherwise!
4. Care about the **scope** [#f1]_:

   * Do not include "testing only" dependencies in the final package - it will hurt you in IDEs and bloat things.
     There is scope ``test`` for this!
   * Make sure to use the ``runtime`` scope when you need to ensure a library is present on our classpath at runtime.
     An example is the SLF4J JUL bridge: we want to route logs from SLF4J into ``java.util.logging``, so it needs to be
     present on the classpath, although we aren't using SLF4J unlike, some of our dependencies.
   * Some dependencies might be ``provided`` by the runtime environment. Good example: everything from Jakarta EE!
     We use the Payara BOM to ensure using the same version during development and runtime.

5. Avoid using different dependencies for the **same purpose**, e. g. different JSON parsing libraries.
6. Refactor your code to **use Jakarta EE** standards as much as possible.
7. When you rely on big SDKs or similar big cool stuff, try to **include the smallest portion possible**. Complete SDK
   bundles are typically heavyweight and most of the time unnecessary.
8. **Don't include transitive dependencies.** [#f2]_

   * Exception: if you are relying on it in your code (see *Z* in the graph above), you must declare it. See below
     for proper handling in these (rare) cases.


Transitive dependencies
-----------------------

Maven is comfortable for developers; it handles recursive resolution, downloading, and adding "dependencies of dependencies".
However, as life is a box of chocolates, you might find yourself in *version conflict hell* sooner than later without even
knowing, but experiencing unintended side effects.

When you look at the topmost graph above, imagine *B* and *TB* rely on different *versions* of *TC*. How does Maven
decide which version it will include? Easy: the version of the dependency nearest to our project ("Your Code)" wins. The following graph gives an example:

.. graphviz::

    digraph {
        rankdir="LR";
        node [fontsize=10]

        yc [label="Your Code"]
        db [label="Direct Dependency B"]
        dtz1 [label="Z v1.0"]
        dtz2 [label="Z v2.0"]

        yc -> db -> dtz1;
        yc -> dtz2;
    }

In this case, version "2.0" will be included. If you know something about semantic versioning, a red alert should ring
in your mind right now. How do we know that *B* is compatible with *Z v2.0* when depending on *Z v1.0*?

Another scenario getting us in trouble: indirect use of transitive dependencies. Imagine the following: we rely on *Z*
in our code, but do not include a direct dependency for it within the POM. Now assume *B* is updated and removed its 
dependency on *Z*. You definitely don't want to head down that road.

**Follow the rules to be safe:**

1. Do **not use transitive deps implicitly**: add a direct dependency for transitive deps you re-use in your code.
2. On every build, check that no implicit usage was added by accident.
3. **Explicitly declare versions** of transitive dependencies in use by multiple direct dependencies.
4. On every build, check that there are no convergence problems hiding in the shadows.
5. **Do special tests** on every build to verify these explicit combinations work.

Managing transitive dependencies in ``pom.xml``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Maven can manage versions of transitive dependencies in four ways:

.. list-table::
    :align: left
    :stub-columns: 1
    :widths: 12 40 40

    * - Safe Good Practice
      - (1) Explicitly declare the transitive dependency in ``<dependencyManagement>`` with a ``<version>`` tag.
      - (2) For more complex transitive dependencies, reuse a "Bill of Materials" (BOM) within ``<dependencyManagement>``.
            Many bigger projects provide them, making the POM much less bloated compared to adding every bit yourself.
    * - Better Avoid or Don't
      - (3) Use ``<optional>`` or ``<exclusion>`` tags on direct dependencies that request the transitive dependency.
            *Last resort*, you really should avoid this. Not explained or used here, but sometimes unavoidable.
            `See Maven docs <https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html>`_.
      - (4) Make a transitive-only dependency not used in your code a direct one and add a ``<version>`` tag.
            Typically a bad idea; don't do that.

**Note:** when the same transitive dependency is used in multiple Maven modules of a software project, it might be added
to a common ``<dependencyManagement>`` section of an inherited parent POM instead. (Overrides are still possible.)

A reduced example, only showing bits relevant to the above cases and usage of an explicit transitive dep directly:

.. code-block:: xml
    :linenos:

    <properties>
        <aws.version>1.11.172</aws.version>
        <!-- We need to ensure that our choosen version is compatible with every dependency relying on it.
             This is manual work and needs testing, but a good investment in stability and up-to-date dependencies. -->
        <jackson.version>2.9.6</jackson.version>
        <joda.version>2.10.1</joda.version>
    </properties>

    <!-- Transitive dependencies, bigger library "bill of materials" (BOM) and
         versions of dependencies used both directly and transitive are managed here. -->
    <dependencyManagement>
        <dependencies>
            <!-- First example for case 4. Only one part of the SDK (S3) is used and transitive deps
                 of that are again managed by the upstream BOM. -->
            <dependency>
                <groupId>com.amazonaws</groupId>
                <artifactId>aws-java-sdk-bom</artifactId>
                <version>${aws.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Second example for case 4 and an example for explicit direct usage of a transitive dependency.
                 Jackson is used by AWS SDK and others, but we also use it in the Dataverse Software. -->
            <dependency>
                <groupId>com.fasterxml.jackson</groupId>
                <artifactId>jackson-bom</artifactId>
                <version>${jackson.version}</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
            <!-- Example for case 3. Joda is not used in the Dataverse Software (as of writing this). -->
            <dependency>
                <groupId>joda-time</groupId>
                <artifactId>joda-time</artifactId>
                <version>${joda.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- Declare any DIRECT dependencies here.
         In case the depency is both transitive and direct (e. g. some common lib for logging),
         manage the version above and add the direct dependency here WITHOUT version tag, too.
    -->
    <dependencies>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-java-sdk-s3</artifactId>
            <!-- no version here as managed by BOM above! -->
        </dependency>
        <!-- Should be refactored and removed now that we are on Jakarta EE 8 -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <!-- no version here as managed above! -->
        </dependency>
        <!-- Should be refactored and removed now that we are on Jakarta EE 8 -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <!-- no version here as managed above! -->
        </dependency>
    </dependencies>


Helpful tools
--------------

Maven provides some plugins that are of great help to detect possible conflicts and implicit usage.

For *implicit usage detection*, use ``mvn dependency:analyze``. Examine the output with great care. Sometimes you will
see implicit usages that do no harm, especially if you are using bigger SDKs having some kind of `core` package.
This will also report on any direct dependency which is not in use and can be removed from the POM. Again, do this with
great caution and double check.

If you want to see the dependencies both direct and transitive in a *dependency tree format*, use ``mvn dependency:tree``.

This will however not help you with detecting possible version conflicts. For this you need to use the `Enforcer Plugin
<https://maven.apache.org/enforcer/maven-enforcer-plugin/index.html>`_ with its built in `dependency convergence rule
<https://maven.apache.org/enforcer/enforcer-rules/dependencyConvergence.html>`_. 

Repositories
------------

Maven receives all dependencies from *repositories*. These can be public like `Maven Central <https://search.maven.org/>`_
and others, but you can also use a private repository on premises or in the cloud.

Repositories are defined within the Dataverse Software POM like this:

.. code:: xml

    <repositories>
        <repository>
            <id>central-repo</id>
            <name>Central Repository</name>
            <url>http://repo1.maven.org/maven2</url>
            <layout>default</layout>
        </repository>
        <repository>
            <id>prime-repo</id>
            <name>PrimeFaces Maven Repository</name>
            <url>http://repository.primefaces.org</url>
            <layout>default</layout>
        </repository>
    </repositories>

You can also add repositories to your local Maven settings, see `docs <https://maven.apache.org/ref/3.6.0/maven-settings/settings.html>`_.

Typically you will skip the addition of the central repository, but adding it to the POM has the benefit that
dependencies are first looked up there (which in theory can speed up downloads). You should keep in mind that repositories
are used in the order they appear.


Dataverse Parent POM
--------------------

Within ``modules/dataverse-parent`` you will find the parent POM for the Dataverse codebase. It serves different
purposes:

1. Provide the common version number for a Dataverse release (may be overriden where necessary).
2. Provide common metadata necessary for releasing modules to repositories like Maven Central.
3. Declare aggregated submodules via ``<modules>``.
4. Collate common BOMs and transitive dependencies within ``<dependencyManagement>``.
   (Remember: a direct dependency declaration may omit the version element when defined in that area!)
5. Collect common ``<properties>`` regarding the Maven project (encoding, ...), dependency versions, target Java version, etc.
6. Gather common ``<repositories>`` and ``<pluginRepositories>`` - no need to repeat those in submodules.
7. Make submodules use current Maven plugin release versions via ``<pluginManagement>``.

As of this writing (2022-02-10), our parent module looks like this:

.. graphviz::

  digraph {
    rankdir="TD";
    node [fontsize=10]
    edge [fontsize=8]

    dvp [label="Dataverse Parent"]
    dvw [label="Submodule:\nDataverse WAR"]
    zip [label="Submodule:\nZipdownloader JAR"]

    dvw -> dvp [label="inherit"];
    dvp -> dvw [label="aggregate"];
    zip -> dvp [label="inherit"];
    dvp -> zip [label="aggregate"];

    pay [label="Payara BOM"]
    aws [label="AWS SDK BOM"]
    ggl [label="Googe Cloud BOM"]
    tc  [label="Testcontainers BOM"]
    td  [label="Multiple (transitive) dependencies\n(PSQL, Logging, Apache Commons, ...)"]

    dvp -> td [label="manage"];

    pay -> dvp [label="import", dir="back"];
    aws -> dvp [label="import", dir="back"];
    ggl -> dvp [label="import", dir="back"];
    tc -> dvp  [label="import", dir="back"];

  }

The codebase is structured like this:

.. code-block::

    <project root>              # Dataverse WAR Module
    ├── pom.xml                 # (POM file of WAR module)
    ├── modules                 #
    │   └── dataverse-parent    # Dataverse Parent Module
    │       └── pom.xml         # (POM file of Parent Module)
    └── scripts                 #
        └── zipdownload         # Zipdownloader JAR Module
            └── pom.xml         # (POM file of Zipdownloader Module)

- Any developer cloning the project and running ``mvn`` within the project root will interact with the Dataverse WAR
  module, which is the same behavior since Dataverse 4.0 has been released.
- Running ``mvn`` targets within the parent module will execute all aggregated submodules in one go.


----

.. rubric:: Footnotes

.. [#f1] Modern IDEs import your Maven POM and offer import autocompletion for classes based on direct dependencies in the model. You might end up using legacy or repackaged classes because of a wrong scope.
.. [#f2] This is going to bite back in modern IDEs when importing classes from transitive dependencies by "autocompletion accident".

----

Previous: :doc:`documentation` | Next: :doc:`debugging`
