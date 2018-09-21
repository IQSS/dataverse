==========
Deployment
==========

Developers often only deploy Dataverse to their :doc:`dev-environment` but it can be useful to deploy Dataverse to cloud services such as Amazon Web Services (AWS).

.. contents:: |toctitle|
	:local:

Deploying Dataverse to Amazon Web Services (AWS)
------------------------------------------------

We have written scripts to deploy Dataverse to Amazon Web Services (AWS) but they require some setup.

Install AWS CLI
~~~~~~~~~~~~~~~

First, you need to have AWS Command Line Interface (AWS CLI) installed, which is called ``aws`` in your terminal. Launching your terminal and running the following command to print out the version of AWS CLI will tell you if it is installed or not:

``aws --version``

If you have not yet installed AWS CLI you should install it by following the instructions at https://docs.aws.amazon.com/cli/latest/userguide/installing.html . Afterwards, you should re-run the "version" command above to verify that AWS CLI has been properly installed.

Configure AWS CLI
~~~~~~~~~~~~~~~~~

Next you need to configure AWS CLI.

Create a ``.aws`` directory in your home directory (which is called ``~``) like this:

``mkdir ~/.aws``

Create a plain text file at ``~/.aws/config`` with the following content::

        [default]
        region = us-east-1

Please note that at this time the region must be set to "us-east-1" but in the future we could improve our scripts to support other regions.

Create a plain text file at ``~/.aws/credentials`` with the following content::

        [default]
        aws_access_key_id = XXXXXXXXXXXXXXXXXXXX
        aws_secret_access_key = XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

Then update the file and replace the values for "aws_access_key_id" and "aws_secret_access_key" with your actual credentials by following the instructions at https://aws.amazon.com/blogs/security/wheres-my-secret-access-key/

Download and Run the "Create Instance" Script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once you have done the configuration above, you are ready to try running the "create instance" script to spin up Dataverse in AWS.

Download :download:`ec2-create-instance.sh <../../../../scripts/installer/ec2-create-instance.sh>` and put it somewhere reasonable. For the purpose of these instructions we'll assume it's in the "Downloads" directory in your home directory.

You need to decide which branch you'd like to deploy to AWS. Select a branch from https://github.com/IQSS/dataverse/branches/all such as "develop" and pass it to the script with ``-b`` as in the following example. (Branches such as "master" and "develop" are described in the :doc:`version-control` section.)

``bash ~/Downloads/ec2-create-instance.sh -b develop``

You must specify the branch with ``-b`` but you can also specify a non-IQSS git repo URL with ``-r`` as in the following example.

``bash ~/Downloads/ec2-create-instance.sh -b develop -r https://github.com/scholarsportal/dataverse.git``

Now you will need to wait around 15 minutes until the deployment is finished. Eventually, the output should tell you how to access the installation of Dataverse in a web browser or via ssh. It will also provide instructions on how to delete the instance when you are finished with it. Please be aware that AWS charges per minute for a running instance. You can also delete your instance from https://console.aws.amazon.com/console/home?region=us-east-1 .

Caveats
~~~~~~~

Please note that while the script should work fine on newish branches, older branches that have different dependencies such as an older version of Solr are now expected to yield a working Dataverse installation. Your mileage may vary.

----

Previous: :doc:`coding-style` | Next: :doc:`containers`
