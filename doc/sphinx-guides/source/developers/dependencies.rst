=====================
Dependency Management
=====================

.. contents:: |toctitle|
	:local:

The Dataverse Software is (currently) a Jakarta EE 8 based application, that uses a lot of additional libraries for special purposes.
This includes features like support for SWORD-API, S3 storage and many others.

Besides the code that glues together the single pieces, any developer needs to describe used dependencies for the
Maven-based build system. As is familiar to any Maven user, this happens inside the "Project Object Model" (POM) living in
``pom.xml`` at the root of the project repository. Recursive and convergent dependency resolution makes dependency
management with Maven very easy. But sometimes, in projects with many complex dependencies like the Dataverse Software, you have
to help Maven make the right choices.

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
within the build lifecycle. You have to define a ``<version>``, but ``<scope>`` is optional for ``compile``.
(See `Maven docs: Dep. Scope <https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope>`_)


During fetching, Maven will analyse all transitive dependencies (see graph above) and, if necessary, fetch those, too.
Everything downloaded once is cached locally by default, so nothing needs to be fetched again and again, as long as the
dependency definition does not change.

**Rules to follow:**

1. You should only use direct dependencies for **things you are actually using** in your code.
2. **Clean up** direct dependencies no longer in use. It will bloat the deployment package otherwise!
3. Care about the **scope**. Do not include "testing only" dependencies in the package - it will hurt you in IDEs and bloat things. [#f1]_
4. Avoid using different dependencies for the **same purpose**, e. g. different JSON parsing libraries.
5. Refactor your code to **use Jakarta EE** standards as much as possible.
6. When you rely on big SDKs or similar big cool stuff, try to **include the smallest portion possible**. Complete SDK
   bundles are typically heavyweight and most of the time unnecessary.
7. **Don't include transitive dependencies.** [#f2]_

   * Exception: if you are relying on it in your code (see *Z* in the graph above), you must declare it. See below
     for proper handling in these (rare) cases.


Transitive dependencies
-----------------------

Maven is comfortable for developers; it handles recursive resolution, downloading, and adding "dependencies of dependencies".
However, as life is a box of chocolates, you might find yourself in *version conflict hell* sooner than later without even
knowing, but experiencing unintended side effects.

When you look at the graph above, imagine *B* and *TB* rely on different *versions* of *TC*. How does Maven decide
which version it will include? Easy: the dependent version of the nearest version wins:

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

In this case, version "2.0" will be included. If you know something about semantic versioning, a red alert should ring in your mind right now.
How do we know that *B* is compatible with *Z v2.0* when depending on *Z v1.0*?

Another scenario getting us in trouble: indirect use of transitive dependencies. Imagine the following: we rely on *Z*
in our code, but do not include a direct dependency for it within the POM. Now *B* is updated and removed its dependency
on *Z*. You definitely don't want to head down that road.

**Follow the rules to be safe:**

1. Do **not use transitive deps implicit**: add a direct dependency for transitive deps you re-use in your code.
2. On every build check that no implicit usage was added by accident.
3. **Explicitly declare versions** of transitive dependencies in use by multiple direct dependencies.
4. On every build check that there are no convergence problems hiding in the shadows.
5. **Do special tests** on every build to verify these explicit combinations work.

Managing transitive dependencies in ``pom.xml``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Maven can manage versions of transitive dependencies in four ways:

1. Make a transitive-only dependency not used in your code a direct one and add a ``<version>`` tag.
   Typically a bad idea, don't do that.
2. Use ``<optional>`` or ``<exclusion>`` tags on direct dependencies that request the transitive dependency.
   *Last resort*, you really should avoid this. Not explained or used here.
   `See Maven docs <https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html>`_.
3. Explicitly declare the transitive dependency in ``<dependencyManagement>`` and add a ``<version>`` tag.
4. For more complex transitive dependencies, reuse a "Bill of Materials" (BOM) within ``<dependencyManagement>``
   and add a ``<version>`` tag. Many bigger and standard use projects provide those, making the POM much less bloated
   compared to adding every bit yourself.

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

For *implicit usage detection*, use `mvn dependency:analyze`. Examine the output with great care. Sometimes you will
see implicit usages that do no harm, especially if you are using bigger SDKs having some kind of `core` package.
This will also report on any direct dependency which is not in use and can be removed from the POM. Again, do this with
great caution and double check.

If you want to see the dependencies both direct and transitive in a *dependency tree format*, use `mvn dependency:tree`.

This will however not help you with detecting possible version conflicts. For this you need to use the `Enforcer Plugin
<https://maven.apache.org/enforcer/maven-enforcer-plugin/index.html>`_ with its built in `dependency convergence rule
<https://maven.apache.org/enforcer/enforcer-rules/dependencyConvergence.html>`_. 

Repositories
------------

Maven receives all dependencies from *repositories*. Those can be public like `Maven Central <https://search.maven.org/>`_
and others, but you can also use a private repository on premises or in the cloud. Last but not least, you can use
local repositories, which can live next to your application code (see ``local_lib`` dir within the Dataverse Software codebase).

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
        <repository>
            <id>dvn.private</id>
            <name>Local repository for hosting jars not available from network repositories.</name>
            <url>file://${project.basedir}/local_lib</url>
        </repository>
    </repositories>

You can also add repositories to your local Maven settings, see `docs <https://maven.apache.org/ref/3.6.0/maven-settings/settings.html>`_.

Typically you will skip the addition of the central repository, but adding it to the POM has the benefit that
dependencies are first looked up there (which in theory can speed up downloads). You should keep in mind that repositories
are used in the order they appear.

----

.. rubric:: Footnotes

.. [#f1] Modern IDEs import your Maven POM and offer import autocompletion for classes based on direct dependencies in the model. You might end up using legacy or repackaged classes because of a wrong scope.
.. [#f2] This is going to bite back in modern IDEs when importing classes from transitive dependencies by "autocompletion accident".

----

Previous: :doc:`documentation` | Next: :doc:`debugging`
