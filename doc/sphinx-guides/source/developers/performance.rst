Performance
===========

`Performance is a feature <https://blog.codinghorror.com/performance-is-a-feature/>`_ was a mantra when Stack Overflow was being developed. We endeavor to do the same with Dataverse!

In this section we collect ideas and share practices for improving performance.

.. contents:: |toctitle|
        :local:

Problem Statement
-----------------

Performance has always been important to the Dataverse Project, but results have been uneven. We've seen enough success in the marketplace that performance must be adequate, but internally we sometimes refer to Dataverse as a pig. 🐷

Current Practices
-----------------

We've adopted a number of practices to help us maintain our current level of performance and most should absolutely continue in some form, but challenges mentioned throughout should be addressed to further improve performance.

Cache When You Can
~~~~~~~~~~~~~~~~~~

The Metrics API, for example, caches values for 7 days by default. We took a look at JSR 107 (JCache - Java Temporary Caching API) in `#2100 <https://github.com/IQSS/dataverse/issues/2100>`_. We're aware of the benefits of caching.

Use Async
~~~~~~~~~

We index datasets (and all objects) asynchronously. That is, we let changes persist in the database and afterward copy the data into Solr.

Use a Queue
~~~~~~~~~~~

We use a JMS queue for when ingesting tabular files. We've talked about adding a queue (even `an external queue <https://github.com/IQSS/dataverse/issues/1100%23issuecomment-311341995>`_) for indexing, DOI registration, and other services.

Offload Expensive Operations Outside the App Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When operations are computationally expensive, we have realized performance gains by offloading them to systems outside of the core code. For example, rather than having files pass through our application server when they are downloaded, we use direct download so that client machines download files directly from S3. (We use the same trick with upload.) When a client downloads multiple files, rather than zipping them within the application server as before, we now have a separate "zipper" process that does this work out of band.

Drop to Raw SQL as Necessary
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We aren't shy about writing raw SQL queries when necessary. We've written `querycount <https://github.com/IQSS/dataverse/blob/v6.0/scripts/database/querycount/README.txt>`_  scripts to help identify problematic queries and mention slow query log at :doc:`/admin/monitoring`.

Add Indexes to Database Tables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There was a concerted effort in `#1880 <https://github.com/IQSS/dataverse/issues/1880>`_ to add indexes to a large number of columns, but it's something we're mindful of, generally. Perhaps we could use some better detection of when indexes would be valuable.

Find Bottlenecks with a Profiler
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

VisualVM is popular and bundled with Netbeans. Many options are available including `JProfiler <https://github.com/IQSS/dataverse/pull/9413>`_.

Warn Developers in Code Comments
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For code that has been optimized for performance, warnings are sometimes inserted in the form of comments for future developers to prevent backsliding.

Write Docs for Devs about Perf
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Like this doc. :)

Sometimes perf is written about in other places, such as :ref:`avoid-efficiency-issues-with-render-logic-expressions`.

Horizontal Scaling of App Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We've made it possible to run more than one application server, though it requires some special configuration. This way load can be spread out across multiple servers. For details, see :ref:`multiple-app-servers` in the Installation Guide.

Code Review and QA
~~~~~~~~~~~~~~~~~~

Before code is merged, while it is in review or QA, if a performance problem is detected (usually on an ad hoc basis), the code is returned to the developer for improvement. Developers and reviewers typically do not have many tools at their disposal to test code changes against anything close to production data. QA maintains a machine with a copy of production data but tests against smaller data unless a performance problem is suspected.

A new QA guide is coming in https://github.com/IQSS/dataverse/pull/10103

Locust Testing at Release Time
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As one of the final steps in preparing for a release, QA runs performance tests using a tool called Locust as explained the Developer Guide (see :ref:`locust`). The tests are not comprehensive, testing only a handful of pages with anonymous users, but they increase confidence that the upcoming release is not drastically slower than previous releases.

Issue Tracking and Prioritization
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Performance issues are tracked in our issue tracker under the `Feature: Performance & Stability <https://github.com/IQSS/dataverse/issues?q=is%3Aopen+is%3Aissue+label%3A%22Feature%3A+Performance+%26+Stability%22>`_ label (e.g. `#7788 <https://github.com/IQSS/dataverse/issues/7788>`_). That way, we can track performance problems throughout the application. Unfortunately, the pain is often felt by users in production before we realize there is a problem. As needed, performance issues are prioritized to be included in a sprint, to \ `speed up the collection page <https://github.com/IQSS/dataverse/pull/8143>`_, for example.

Document Performance Tools
~~~~~~~~~~~~~~~~~~~~~~~~~~

In the :doc:`/admin/monitoring` page section of the Admin Guide we describe how to set up Munin for monitoring performance of an operating system. We also explain how to set up Performance Insights to monitor AWS RDS (PostgreSQL as a service, in our case). In the :doc:`/developers/tools` section of the Developer Guide, we have documented how to use Eclipse Memory Analyzer Tool (MAT), SonarQube, jmap, and jstat.

Google Analytics
~~~~~~~~~~~~~~~~

Emails go to a subset of the team monthly with subjects like "Your September Search performance for https://dataverse.harvard.edu" with a link to a report but it's mostly about the number clicks, not how fast the site is. It's unclear if it provides any value with regard to performance.

Abandoned Tools and Practices
-----------------------------

New Relic
~~~~~~~~~

For many years Harvard Dataverse was hooked up to New Relic, a tool that promises all-in-one observability, according to their `website <https://newrelic.com>`_. In practice, we didn't do much with `the data <https://github.com/IQSS/dataverse/issues/3665>`_.

Areas of Particular Concern
---------------------------

Command Engine Execution Rate Metering
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We'd like to rate limit commands (CreateDataset, etc.) so that we can keep them at a reasonable level (`#9356 <https://github.com/IQSS/dataverse/issues/9356>`_). This is similar to how many APIs are rate limited, such as the GitHub API.

Solr
~~~~

While in the past Solr performance hasn't been much of a concern, in recent years we've noticed performance problems when Harvard Dataverse is under load. Improvements were made in `PR #10050 <https://github.com/IQSS/dataverse/pull/10050>`_, for example.

Datasets with Large Numbers of Files or Versions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We'd like to scale Dataverse to better handle large number of files or versions. Progress was made in `PR #9883 <https://github.com/IQSS/dataverse/pull/9883>`_.

Withstanding Bots
~~~~~~~~~~~~~~~~~

Google bot, etc.

Suggested Practices
-------------------

Many of our current practices should remain in place unaltered. Others could use some refinement. Some new practices should be adopted as well. Here are some suggestions.

Implement the Frontend Plan for Performance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The `Dataverse - SPA MVP Definition doc <https://docs.google.com/document/d/1WnJzLeVK5eVP4_10eX6BwPAnmiamO1n2uGzcwrAsucQ/edit?usp%3Dsharing>`_  has some ideas around how to achieve good performance for the new front end in the areas of rendering, monitoring,file upload/download, pagination, and caching. We should create as many issues as necessary in the frontend repo and work on them in time. The doc recommends the use of `React Profiler <https://legacy.reactjs.org/blog/2018/09/10/introducing-the-react-profiler.html>`_ and other tools. Not mentioned is https://pagespeed.web.dev but we can investigate it as well. See also `#183 <https://github.com/IQSS/dataverse-frontend/issues/183>`_, a parent issue about performance. In `#184 <https://github.com/IQSS/dataverse-frontend/issues/184>`_  we plan to compare the performance of the old JSF UI vs. the new React UI. Cypress plugins for load testing could be investigated.

Set up Query Counter in Jenkins
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See countquery script above. See also https://jenkins.dataverse.org/job/IQSS-dataverse-develop/ws/target/query_count.out

Show the plot over time. Make spikes easily apparent. 320,035 queries as of this writing.

Count Database Queries per API Test
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Is it possible? Just a thought.

Teach Developers How to Do Performance Testing Locally
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Do developers know how to use a profiler? Should they use `JMeter <https://github.com/BU-NU-CLOUD-SP18/Dataverse-Scaling%23our-project-video>`_? `statsd-jvm-profiler <https://github.com/etsy/statsd-jvm-profiler>`_? How do you run our :ref:`locust` tests? Should we continue using that tool? Give developers time and space to try out tools and document any tips along the way. For this stage, small data is fine.

Automate Performance Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We are already using two excellent continuous integration (CI) tools, Jenkins and GitHub Actions, to test our code. We should add performance testing into the mix (`#4201 <https://github.com/IQSS/dataverse/issues/4201>`_ is an old issue for this but we can open a fresh one). Currently we test every commit on every PR and we should consider if this model makes sense since performance testing will likely take longer to run than regular tests. Once developers are comfortable with their favorite tools, we can pick which ones to automate.

Make Production Data or Equivalent Available to Developers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If developers are only testing small amounts of data on their laptops, it's hard to detect performance problems. Not every bug fix requires access to data similar to production, but it should be made available. This is not a trivial task! If we are to use actual production data, we need to be very careful to de-identify it. If we start with our `sample-data <https://github.com/IQSS/dataverse-sample-data>`_  repo instead, we'll need to figure out how to make sure we cover cases like many files, many versions, etc.

Automate Performance Testing with Production Data or Equivalent
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Hopefully the environment developers use with production data or equivalent can be made available to our CI tools. Perhaps these tests don't need to be run on every commit to every pull request, but they should be run regularly.

Use Monitoring as Performance Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Monitoring can be seen as a form of testing. How long is a round trip ping to production? What is the Time to First Byte? First Contentful Paint? Largest Contentful Paint? Time to Interactive? We now have a beta server that we could monitor continuously to know if our app is getting faster or slower over time. Should our monitoring of production servers be improved?

Learn from Training and Conferences
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Most likely there is training available that is oriented toward performance. The subject of performance often comes up at conferences as well.

Learn from the Community How They Monitor Performance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Some members of the Dataverse community are likely users of newish tools like the ELK stack (Elasticsearch, Logstash, and Kibana), the TICK stack (Telegraph InfluxDB Chronograph and Kapacitor), GoAccess, Prometheus, Graphite, and more we haven't even heard of. In the :doc:`/admin/monitoring` section of the Admin Guide, we already encourage the community to share findings, but we could dedicate time to this topic at our annual meeting or community calls.

Teach the Community to Do Performance Testing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We have a worldwide community of developers. We should do what we can in the form of documentation and other resources to help them develop performant code.

Conclusion
----------

Given its long history, Dataverse has encountered many performance problems over the years. The core team is conversant in how to make the app more performant, but investment in learning additional tools and best practices would likely yield dividends. We should automate our performance testing, catching more problems before code is merged.
