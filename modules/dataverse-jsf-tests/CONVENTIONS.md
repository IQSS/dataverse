# JSF test suite conventions

> [!IMPORTANT]
> **Test classes must end in `JT`** (for "JSF test"), for example `CreateCollectionJT.java`. A class with any other name, including `*Test.java` and `*IT.java`, is never run, and the build still passes.

## Why

CI runs the suite in the JSF jobs of [`dataverse_tests.yml`](../../.github/workflows/dataverse_tests.yml), through the [`run-jsf-tests`](../../.github/actions/run-jsf-tests/action.yml) action, which runs `mvn verify` in this directory. In [`pom.xml`](pom.xml), the Failsafe plugin only includes `**/*JT.java`, in place of its default `*IT` patterns, and the Surefire plugin (`*Test`) is switched off. The file name is the only thing that decides whether a test runs.

## Use `@JsfTest`, not `@Test`

`@JsfTest` gives a test method its logged-in browser and its `Page`, `JsfFixtures` and `Cleanup` parameters. A `@Test` method that asks for them fails.

## Check that a new test runs

```
mvn -f modules/dataverse-jsf-tests verify -Dit.test=<ClassName> -Ddataverse.test.jsf.browser=chromium
```

Failsafe fails if `<ClassName>` matches no test, so a misnamed class shows up here instead of being skipped silently.

Everything else (packages, method names, how a test is written) is up to you. The existing tests under `src/test/java/edu/harvard/iq/dataverse/jsf/` are there as examples.
