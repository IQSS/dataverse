In the Container Guide, documentation for developers on how to quickly redeploy code has been added for Netbeans and improved for IntelliJ.

Also in the context of containers, a new option to skip deployment has been added and the war file is now consistently named "dataverse.war" rather than having a version in the filename, such as "dataverse-6.1.war". This predictability makes tooling easier.

Finally, an option to create tabs in the guides using [Sphinx Tabs](https://sphinx-tabs.readthedocs.io) has been added. (You can see the tabs in action in the "dev usage" page of the Container Guide.) To continue building the guides, you will need to install this new dependency by re-running  `pip install -r requirements.txt`.