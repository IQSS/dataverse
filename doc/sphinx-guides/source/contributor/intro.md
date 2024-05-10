# Introduction

Please note: this is a copy/paste from <https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md> but we intend to split the content on this intro page into sub-pages.

Thank you for your interest in contributing to Dataverse!  We are open to contributions from everyone. You don't need permission to participate. Just jump in. If you have questions, please reach out using one or more of the channels described below.

We aren't just looking for developers. There are many ways to contribute to Dataverse.  We welcome contributions of ideas, bug reports, usability research/feedback, documentation, code, and more!

```{contents} Contents:
:local:
:depth: 3
```

## Ideas/Feature Requests

1. Please check if your idea or feature request is already captured in our [issue tracker][] or [roadmap][].
1. Bring your idea to the community by posting on our [Google Group][] or [chat.dataverse.org][].
1. To discuss privately, email us at support@dataverse.org.

[issue tracker]: https://github.com/IQSS/dataverse/issues
[roadmap]: https://www.iq.harvard.edu/roadmap-dataverse-project
[chat.dataverse.org]: http://chat.dataverse.org
[Google Group]: https://groups.google.com/group/dataverse-community

## Usability Testing

Please email us at <support@dataverse.org> if you are interested in participating in usability testing.

## Bug Reports/Issues

An issue is a bug (a feature is no longer behaving the way it should) or a feature (something new to Dataverse that helps users complete tasks). You can browse the Dataverse [issue tracker] on GitHub by open or closed issues or by milestones.

Before submitting an issue, please search the existing issues by using the search bar at the top of the page. If there is an existing open issue that matches the issue you want to report, please add a comment to it.

If there is no pre-existing issue or it has been closed, please click on the "New Issue" button, log in, and write in what the issue is (unless it is a security issue which should be reported privately to security@dataverse.org).

If you do not receive a reply to your new issue or comment in a timely manner, please email <support@dataverse.org> with a link to the issue.

### Writing an Issue

For the subject of an issue, please start it by writing the feature or functionality it relates to, i.e. "Create Account:..." or "Dataset Page:...". In the body of the issue, please outline the issue you are reporting with as much detail as possible. In order for the Dataverse development team to best respond to the issue, we need as much information about the issue as you can provide. Include steps to reproduce bugs. Indicate which version you're using, which is shown at the bottom of the page. We love screenshots!

### Issue Attachments

You can attach certain files (images, screenshots, logs, etc.) by dragging and dropping, selecting them, or pasting from the clipboard. Files must be one of GitHub's [supported attachment formats] such as png, gif, jpg, txt, pdf, zip, etc. (Pro tip: A file ending in .log can be renamed to .txt so you can upload it.) If there's no easy way to attach your file, please include a URL that points to the file in question.

[supported attachment formats]: https://help.github.com/articles/file-attachments-on-issues-and-pull-requests/

## Documentation

The source for the documentation is in the GitHub repo under the "[doc][]" folder. If you find a typo or inaccuracy or something to clarify, please send us a pull request! For more on the tools used to write docs, please see the {doc}`/developers/documentation` section of the Developer Guide.

[doc]: https://github.com/IQSS/dataverse/tree/develop/doc/sphinx-guides/source

## Code/Pull Requests

We love code contributions. Developers are not limited to the main Dataverse code in this git repo. You can help with API client libraries in your favorite language that are mentioned in the {doc}`/api/index` or create a new library. You can help work on configuration management code that's mentioned in the {doc}`/installation/index` . The Installation Guide also covers a relatively new concept called "external tools" that allows developers to create their own tools that are available from within an installation of Dataverse.

If you are interested in working on the main Dataverse code, great! Before you start coding, please reach out to us either on the [dataverse-community Google Group][], the [dataverse-dev Google Group][], [chat.dataverse.org][], or via <support@dataverse.org> to make sure the effort is well coordinated and we avoid merge conflicts. We maintain a list of [community contributors][] and [dev efforts][] the community is working on so please let us know if you'd like to be added or removed from either list.

Please read {doc}`/developers/version-control` in the Developer Guide to understand how we use the "git flow" model of development and how we will encourage you to create a GitHub issue (if it doesn't exist already) to associate with your pull request. That page also includes tips on making a pull request.

After making your pull request, your goal should be to help it advance through our kanban board at <https://github.com/orgs/IQSS/projects/34> . If no one has moved your pull request to the code review column in a timely manner, please reach out. Note that once a pull request is created for an issue, we'll remove the issue from the board so that we only track one card (the pull request).

Thanks for your contribution!

[dataverse-community Google Group]: https://groups.google.com/group/dataverse-community
[Community Call]: https://dataverse.org/community-calls
[dataverse-dev Google Group]: https://groups.google.com/group/dataverse-dev
[community contributors]: https://docs.google.com/spreadsheets/d/1o9DD-MQ0WkrYaEFTD5rF_NtyL8aUISgURsAXSL7Budk/edit?usp=sharing
[dev efforts]: https://github.com/orgs/IQSS/projects/34/views/6
