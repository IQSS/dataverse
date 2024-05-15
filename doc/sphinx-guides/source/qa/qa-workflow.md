# QA Workflow for Pull Requests

```{contents} Contents:
:local: 
:depth: 3
```
## Checklist

1. Assign the PR you are working on to yourself.

1. What does it do?

    Read the description at the top of the PR, any release notes, documentation, and the original issue.

1. Does it address the issue it closes? 

    The PR should address the issue entirely unless otherwise noted.

1. How do you test it?
    
    Look at the “how to test" section at the top of the pull request. Does it make sense? This likely won’t be the only testing you perform. You can develop further tests from the original issue or problem description, from the description of functionality, the documentation, configuration, and release notes. Also consider trying to reveal bugs by trying to break it: try bad or missing data, very large values or volume of data, exceed any place that may have a limit or boundary.

1. Does it have or need documentation?

    Small changes or fixes usually don’t have docs but new features or extensions of a feature or new configuration options should have documentation.

1. Does it have or need a release note snippet?

    Same as for doc, just a heads up to an admin for something of note or especially upgrade instructions as needed. See also {ref}`writing-release-note-snippets` for what to expect in a release note snippet.

1. Does it include a database migration script (Flyway)?
    
    First, check the numbering in the filename of the script. It must be in line with the rules defined at {ref}`create-sql-script`. If the number is out of date (very common for older pull requests), do not merge and ask the developer to rename the script. Otherwise, deployment will fail.

    Once you're sure the numbering is ok (the next available number, basically), back up your database and proceeed with testing.

1. Validate the documentation.

    Build the doc using Jenkins or read the automated Read the Docs preview. Does it build without errors?
    Read it through for sense.
    Use it for test cases and to understand the feature.

1. Build and deploy the pull request.

    Normally this is done using Jenkins and automatically deployed to the QA test machine. See {ref}`deploy-to-internal`.

1. Configure if required

    If needed to operate and everyone installing or upgrading will use this, configure now as all testing will use it.

1. Smoke test the branch.
    
    Standard, minimal test of core functionality.

1. Regression test-related or potentially affected features

    If config is optional and testing without config turned on, do some spot checks/ regression tests of related or potentially affected areas. 

1. Configure if optional

    What is the default, enabled or disabled? Is that clearly indicated? Test both.
    By config here we mean enabling the functionality versus choosing a particular config option. Some complex features have config options in addition to enabling. Those will also need to be tested.

1. Test all the new or changed functionality.

    The heart of the PR, what is this PR adding or fixing? Is it all there and working?

1. Regression test related or potentially affected features.
    
    Sometimes new stuff modifies and extends other functionality or functionality that is shared with other aspects of the system, e.g. export, import. Check the underlying functionality that was also modified but in a spot check or briefer manner.

1. Report any issues found within the PR

    It can be easy to lose track of what you’ve found, steps to reproduce, and any errors or stack traces from the server log. Add these in a numbered list to a comment in the pr. Easier to check off when fixed and to work on. Add large amounts of text as in the server log as attached, meaningfully named files.

1. Retest all fixes, spot check feature functionality, smoke test
    
    Similar to your initial testing, it is only narrower.

1. Test upgrade instructions, if required

    Some features build upon the existing architecture but require modifications, such as adding a new column to the DB or changing or adding data. It is crucial that this works properly for our 100+ installations. This testing should be performed at the least on the prior version with basic data objects (collection, dataset, files) and any other data that will be updated by this feature. Using the sample data from the prior version would be good or deploying to dataverse-internal and upgrading there would be a good test. Remember to back up your DB before doing a transformative upgrade so that you can repeat it later if you find a bug.

1. Make sure the API tests in the PR have been completed and passed.
   
    They are run with each commit to the PR and take approximately 42 minutes to run.

1. Merge PR
    
    Click the "Merge pull request" button and be sure to use the "Create a merge commit" option to include this PR into the common develop branch.

    Some of the reasons why we encourage using this option over Rebase or Squash are:

    - Preservation of commit history
    - Clearer context and treaceability
    - Easier collaboration, bug tracking and reverting

1. Delete merged branch
    
    Just a housekeeping move if the PR is from IQSS. Click the delete branch button where the merge button had been. There is no deletion for outside contributions.

1. Ensure that deployment to beta.dataverse.org succeeded.

    Go to <https://github.com/IQSS/dataverse/actions/workflows/deploy_beta_testing.yml> to keep any eye on the deployment to <https://beta.dataverse.org> to make sure it succeeded. The latest commit will appear at the bottom right and <https://beta.dataverse.org/api/info/version>.  
