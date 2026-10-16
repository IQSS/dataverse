## Updates for Developers

The JSF frontend tests have moved into this repository, as `modules/dataverse-jsf-tests`, with their history. Previously they lived in [gdcc/dataverse-jsf-tests](https://github.com/gdcc/dataverse-jsf-tests). The suite is now written in Java (Playwright for Java and JUnit 5) and runs with Maven, without Node.js:

```bash
mvn -f modules/dataverse-jsf-tests verify -Ddataverse.test.jsf.browser=firefox
```

The main build doesn't include the module, so its tests only run when you ask for them. They can test any installation from a machine Playwright supports, which also covers servers on platforms it doesn't support, such as RHEL. Don't point them at a production installation: they create and publish test data. See [the guides](https://guides.dataverse.org/en/latest/developers/testing.html#browser-based-testing-of-jsf) and #12797.
