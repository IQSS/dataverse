# Other Approaches to Deploying and Testing

```{contents} Contents:
:local: 
:depth: 3
```

This workflow is fine for a single person testing a PR, one at a time. It would be awkward or impossible if there were multiple people wanting to test different PRs at the same time. If a developer is testing, they would likely just deploy to their dev environment. That might be ok, but is the env is fully configured enough to offer a real-world testing scenario? An alternative might be to spin an EC2 branch on AWS, potentially using sample data. This can take some time so another option might be to spin up a few, persistent AWS instances with sample data this way, one per tester, and just deploy new builds there when you want to test. You could even configure Jenkins projects for each if desired to maintain consistency in how they’re built.

## Tips and Tricks

- Start testing simply, with the most obvious test. You don’t need to know all your tests upfront. As you gain comfort and understanding of how it works, try more tests until you are done. If it is a complex feature, jot down your tests in an outline format, some beforehand as a guide, and some after as things occur to you. Save the doc in a testing folder (on Google Drive). This potentially will help with future testing.
- When in doubt, ask someone. If you are confused about how something is working, it may be something you have missed, or it could be a documentation issue, or it could be a bug! Talk to the code reviewer and the contributor/developer for their opinion and advice.
- Always tail the server.log file while testing. Open a terminal window to the test instance and `tail -F server.log`. This helps you get a real-time sense of what the server is doing when you act and makes it easier to identify any stack trace on failure.
- When overloaded, do the simple pull requests first to reduce the queue. It gives you a mental boost to complete something and reduces the perception of the amount of work still to be done.
- When testing a bug fix, try reproducing the bug on the demo before testing the fix, that way you know you are taking the correct steps to verify that the fix worked.
- When testing an optional feature that requires configuration, do a smoke test without the feature configured and then with it configured. That way you know that folks using the standard config are unaffected by the option if they choose not to configure it.
- Back up your DB before applying an irreversible DB update and you are using a persistent/reusable platform. Just in case it fails, and you need to carry on testing something else you can use the backup.

## Workflow for Completing QA on a PR

1. Assign the PR you are working on to yourself.

1. What does it do?

    Read the description at the top of the PR, any release notes, documentation, and the original issue.

1. Does it address the issue it closes? 

    The PR should address the issue entirely unless otherwise noted.

1. How do you test it?
    
    Look at the “how to test" section at the top of the pull request. Does it make sense? This likely won’t be the only testing you perform. You can develop further tests from the original issue or problem description, from the description of functionality, the documentation, configuration, and release notes. Also consider trying to reveal bugs by trying to break it: try bad or missing data, very large values or volume of data, exceed any place that may have a limit or boundary.

1. Does it have or need documentation?

    Small changes or fixes usually don’t have docs but new features or extensions of a feature or new configuration options should have documentation.

1. Does it have or need release notes?

    Same as for doc, just a heads up to an admin for something of note or especially upgrade instructions as needed.

1. Does it use a DB, Flyway script?
    
    Good to know since it may collide with another existing one by version or it could be a one way transform of your DB so back up your test DB before. Also, happens during deployment so be on the lookout for any issues.

1. Validate the documentation.

    Build the doc using Jenkins, does it build without errors?
    Read it through for sense.
    Use it for test cases and to understand the feature.

1. Build and deploy the pull request.

    Normally this is done using Jenkins and automatically deployed to the QA test machine.

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
    
    Click merge to include this PR into the common develop branch.

1. Delete merged branch
    
    Just a housekeeping move if the PR is from IQSS. Click the delete branch button where the merge button had been. There is no deletion for outside contributions.


## Checklist for Completing QA on a PR

1. Build the docs 
1. Smoke test the pr 
1. Test the new functionality
1. Regression test 
1. Test any upgrade instructions

## Checklist for QA on Release

1. Review Consolidated Release Notes, in particular upgrade instructions.
1. Conduct performance testing and compare with the previous release.
1. Perform clean install and smoke test.
1. Potentially follow upgrade instructions. Though they have been performed incrementally for each PR, the sequence may need checking
