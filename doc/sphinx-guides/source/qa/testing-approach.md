# Testing Approach

```{contents} Contents:
:local: 
:depth: 3
```
## Introduction

We use a risk-based, manual testing approach to achieve the most benefit with limited resources. This means we want to catch bugs where they are likely to exist, ensure core functions work, and failures do not have catastrophic results. In practice this means we do a brief positive check of core functions on each build called a smoke test, we test the most likely place for new bugs to exist, the area where things have changed, and attempt to prevent catastrophic failure by asking about the scope and reach of the code and how failures may occur. 

If it seems possible through user error or some other occurrence that such a serious failure will occur, we try to make it happen in the test environment. If the code has a UI component, we also do a limited amount of browser compatibility testing using Chrome, Firefox, and Safari browsers. We do not currently do UX or accessibility testing on a regular basis, though both have been done product-wide by a Design group (in the past) and by the community.

## Examining a Pull Request for Test Cases

### What Problem Does It Solve?

Read the top part of the pull request for a description, notes for reviewers, and usually a "how to test" section. Does it make sense? If not, read the underlying issue it closes and any release notes or documentation. Knowing in general what it does helps you to think about how to approach it.

### How is It Configured?
 
Most pull requests do not have any special configuration and are enabled on deployment, but some do. Configuration is part of testing. A sysadmin or superuser will need to follow these instructions so make sure they are in the release note snippet and try them out. Plus, that is the only way you will get it working to test it! 

Identify test cases by examining the problem report or feature description and any documentation of functionality. Look for statements or assertions about functions, what it does, as well as conditions or conditional behavior. These become your test cases. Think about how someone might make a mistake using it and try it. Does it fail gracefully or in a confusing, or worse, damaging manner? Also, consider whether this pull request may interact with other functionality and try some spot checks there. For instance, if new metadata fields have been added, try the export feature. Of course, try the suggestions under "how to test." Those may be sufficient, but you should always think about the pull request based on what it does.

Try adding, modifying, and deleting any objects involved. This is probably covered by using the feature, but this is a good basic approach to keep in mind.

Make sure any server logging is appropriate. You should tail the server log while running your tests. Watch for unreported errors or stack traces especially chatty logging. If you do find a bug you will need to report the stack trace from the server.log. Err on the side of providing the developer too much of server.log rather than too little.

Exercise the UI if there is one. We tend to use Chrome for most of our basic testing as it's used twice as much as the next most commonly-used browser, according to our site's Google Analytics. First go through all the options in the UI. Then, if all works, spot-check using Firefox and Safari.

Check permissions. Is this feature limited to a specific set of users? Can it be accessed by a guest or by a non-privileged user? How about pasting a privileged page URL into a non-privileged user’s browser?

Think about risk. Is the feature or function part of a critical area such as permissions? Does the functionality modify data? You may do more testing when the risk is higher.

## Smoke Test

1. Go to the homepage on <https://dataverse-internal.iq.harvard.edu>. Scroll to the bottom to ensure the build number is the one you intend to test from Jenkins.
1. Create a new user: It's fine to use a formulaic name with your initials and date and make the username and password the same, eg. kc080622.
1. Create a dataverse: You can use the same username.
1. Create a dataset: You can use the same username; fill in the required fields (do not use a template).
1. Upload 3 different types of files: You can use a tabular file, 50by1000.dta, an image file, and a text file.
1. Publish the dataset.
1. Download a file.


## Alternative Deployment and Testing

This workflow is fine for a single person testing a PR, one at a time. It would be awkward or impossible if there were multiple people wanting to test different PRs at the same time. If a developer is testing, they would likely just deploy to their dev environment. That might be ok, but is the env is fully configured enough to offer a real-world testing scenario? 

An alternative might be to spin an EC2 branch on AWS, potentially using sample data. This can take some time so another option might be to spin up a few, persistent AWS instances with sample data this way, one per tester, and just deploy new builds there when you want to test. You could even configure Jenkins projects for each if desired to maintain consistency in how they’re built.
